package web;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Measures JVM heap and process CPU approximately every 100 ms. */
public final class ProcessSampler implements AutoCloseable {
    private final RunRecord record;
    private final long startedNs;
    private final MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
    private final com.sun.management.OperatingSystemMXBean os;
    private final int processors = Math.max(1, Runtime.getRuntime().availableProcessors());
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "rspm-metrics-sampler");
        t.setDaemon(true);
        return t;
    });
    private long lastWallNs;
    private long lastCpuNs;
    private final long initialCpuNs;

    public ProcessSampler(RunRecord record) {
        this.record = record;
        this.startedNs = System.nanoTime();
        var bean = ManagementFactory.getOperatingSystemMXBean();
        this.os = bean instanceof com.sun.management.OperatingSystemMXBean
                ? (com.sun.management.OperatingSystemMXBean) bean : null;
        this.initialCpuNs = os == null ? -1L : os.getProcessCpuTime();
        this.lastWallNs = startedNs;
        this.lastCpuNs = initialCpuNs;
    }

    public long startNanos() { return startedNs; }

    public void start() {
        collect();
        scheduler.scheduleAtFixedRate(this::collectSafely, 100, 100, TimeUnit.MILLISECONDS);
    }

    private void collectSafely() {
        try { collect(); } catch (RuntimeException e) { System.err.println("Sampling: " + e.getMessage()); }
    }

    public synchronized void collect() {
        long now = System.nanoTime();
        double elapsedMs = (now - startedNs) / 1_000_000.0;
        double heapMB = memory.getHeapMemoryUsage().getUsed() / 1_048_576.0;
        Double cpuPercent = null;
        if (os != null) {
            long current = os.getProcessCpuTime();
            long wallDelta = now - lastWallNs;
            if (current >= 0 && lastCpuNs >= 0 && wallDelta > 0) {
                cpuPercent = Math.max(0.0, Math.min(100.0,
                        (current - lastCpuNs) * 100.0 / (wallDelta * processors)));
            }
            lastCpuNs = current;
        }
        lastWallNs = now;
        record.sample(elapsedMs, cpuPercent, heapMB);
    }

    public Double averageCpuPercent(long finishedNs) {
        if (os == null || initialCpuNs < 0) return null;
        long totalCpuNs = os.getProcessCpuTime() - initialCpuNs;
        long wall = finishedNs - startedNs;
        if (totalCpuNs < 0 || wall <= 0) return null;
        return Math.max(0.0, Math.min(100.0, totalCpuNs * 100.0 / (wall * processors)));
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
        collect();
    }
}
