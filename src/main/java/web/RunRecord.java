package web;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** An individual benchmark: one mining request = one chart line. */
public final class RunRecord {
    public final long id;
    public final String dataset;
    public final double minSup;
    public final String createdAt;

    private String status = "QUEUED";
    private String error = null;
    private double durationMs = 0.0;
    private int patterns = 0;
    private Double avgCpuPercent = null;
    private double peakHeapMB = 0.0;
    private final List<StagePoint> stages = new ArrayList<>();
    private final List<SamplePoint> samples = new ArrayList<>();

    public record StagePoint(String name, double elapsedMs) {
        String json() { return "{\"name\":" + Json.q(name) + ",\"elapsedMs\":" + Json.n(elapsedMs) + "}"; }
    }
    public record SamplePoint(double elapsedMs, Double cpuPercent, double heapMB) {
        String json() { return "{\"elapsedMs\":" + Json.n(elapsedMs) + ",\"cpuPercent\":" + Json.n(cpuPercent)
                + ",\"heapMB\":" + Json.n(heapMB) + "}"; }
    }

    public RunRecord(long id, String dataset, double minSup) {
        this.id = id;
        this.dataset = dataset;
        this.minSup = minSup;
        this.createdAt = java.time.OffsetDateTime.now().toString();
    }

    public synchronized void started() { status = "RUNNING"; }
    public synchronized void stage(String name, double elapsedMs) { stages.add(new StagePoint(name, elapsedMs)); }
    public synchronized void sample(double elapsedMs, Double cpuPercent, double heapMB) {
        samples.add(new SamplePoint(elapsedMs, cpuPercent, heapMB));
        peakHeapMB = Math.max(peakHeapMB, heapMB);
    }
    public synchronized void completed(double durationMs, int patterns, Double avgCpuPercent) {
        this.durationMs = durationMs;
        this.patterns = patterns;
        this.avgCpuPercent = avgCpuPercent;
        this.status = "DONE";
    }
    public synchronized void failed(String message) {
        this.status = "ERROR";
        this.error = message;
    }
    public synchronized String status() { return status; }
    public synchronized String csv() {
        return id + "," + dataset + "," + String.format(Locale.ROOT, "%.2f", minSup) + "," +
                String.format(Locale.ROOT, "%.3f", durationMs) + "," +
                (avgCpuPercent == null ? "" : String.format(Locale.ROOT, "%.3f", avgCpuPercent)) + "," +
                String.format(Locale.ROOT, "%.3f", peakHeapMB) + "," + patterns + "," + status + "\n";
    }
    public synchronized String json() {
        List<String> stageJson = new ArrayList<>();
        for (StagePoint s : stages) stageJson.add(s.json());
        List<String> sampleJson = new ArrayList<>();
        for (SamplePoint s : samples) sampleJson.add(s.json());
        return "{\"id\":" + id + ",\"dataset\":" + Json.q(dataset)
                + ",\"minSup\":" + Json.n(minSup)
                + ",\"createdAt\":" + Json.q(createdAt)
                + ",\"status\":" + Json.q(status)
                + ",\"error\":" + Json.q(error)
                + ",\"durationMs\":" + Json.n(durationMs)
                + ",\"patterns\":" + patterns
                + ",\"avgCpuPercent\":" + Json.n(avgCpuPercent)
                + ",\"peakHeapMB\":" + Json.n(peakHeapMB)
                + ",\"stages\":" + Json.array(stageJson)
                + ",\"samples\":" + Json.array(sampleJson) + "}";
    }
}
