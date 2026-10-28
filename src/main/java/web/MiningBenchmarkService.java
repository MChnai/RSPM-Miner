package web;

import core.model.Pattern;
import engine.StaticORNMinerEngine;
import ingestion.SPMFDataReader;
import strategy.counting.TwoPointerNonOverlappingCounter;
import strategy.pruning.RPBPruningStrategy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/** One job at a time: CPU/RAM measurements do not overlap between benchmark requests. */
public final class MiningBenchmarkService {
    private static final String CSV_HEADER = "RunId,Dataset,MinSup,RuntimeMs,AvgProcessCpuPercent,PeakHeapMB,Patterns,Status\n";
    private final Path dataDirectory;
    private final Path historyCsv;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final AtomicLong nextId = new AtomicLong();
    private final Map<Long, RunRecord> records = new ConcurrentHashMap<>();

    public MiningBenchmarkService(Path projectRoot) throws IOException {
        this.dataDirectory = projectRoot.resolve("data").toRealPath();
        if (!Files.isDirectory(dataDirectory)) throw new IOException("Missing data/ folder");
        Path results = projectRoot.resolve("results");
        Files.createDirectories(results);
        this.historyCsv = results.resolve("web_benchmark_results.csv");
        if (!Files.exists(historyCsv)) Files.writeString(historyCsv, CSV_HEADER, StandardCharsets.UTF_8);
    }

    public List<String> datasets() throws IOException {
        try (Stream<Path> paths = Files.list(dataDirectory)) {
            return paths.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.toLowerCase().endsWith(".txt"))
                    .sorted(String.CASE_INSENSITIVE_ORDER).toList();
        }
    }

    public RunRecord submit(String dataset, double minSup) throws IOException {
        if (dataset == null || dataset.isBlank() || !datasets().contains(dataset))
            throw new IllegalArgumentException("Chỉ được chọn file .txt trong data/");
        if (!Double.isFinite(minSup) || minSup <= 0)
            throw new IllegalArgumentException("minSup phải là một số dương (ngưỡng tuyệt đối)");
        Path input = dataDirectory.resolve(dataset).normalize();
        if (!input.getParent().equals(dataDirectory) || !Files.isRegularFile(input, LinkOption.NOFOLLOW_LINKS))
            throw new IllegalArgumentException("Đường dẫn dataset không hợp lệ");
        RunRecord run = new RunRecord(nextId.incrementAndGet(), dataset, minSup);
        records.put(run.id, run);
        worker.submit(() -> execute(run, input));
        return run;
    }

    private void execute(RunRecord run, Path input) {
        run.started();
        ProcessSampler sampler = new ProcessSampler(run);
        long finishedNs;
        try {
            StaticORNMinerEngine engine = new StaticORNMinerEngine(
                    new SPMFDataReader(input.toString()),
                    new TwoPointerNonOverlappingCounter(),
                    new RPBPruningStrategy(),
                    run.minSup
            );
            engine.setVerbose(false); // Logging each candidate would distort the benchmark.
            engine.setStageListener(run::stage);

            sampler.start();
            long miningStartedNs = System.nanoTime();
            List<Pattern> patterns = engine.mine(); // Includes data loading; no duplicate loadData().
            finishedNs = System.nanoTime();
            Double cpu = sampler.averageCpuPercent(finishedNs);
            sampler.close();
            run.completed((finishedNs - miningStartedNs) / 1_000_000.0, patterns.size(), cpu);
        } catch (Exception ex) {
            sampler.close();
            run.failed(ex.getClass().getSimpleName() + ": " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            // The CSV is an audit summary. Live chart series remain in memory until restart.
            synchronized (this) {
                try {
                    Files.writeString(historyCsv, run.csv(), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
                } catch (IOException ex) {
                    System.err.println("Cannot write web_benchmark_results.csv: " + ex.getMessage());
                }
            }
        }
    }

    public RunRecord find(long id) { return records.get(id); }

    public List<RunRecord> history() {
        List<RunRecord> result = new ArrayList<>(records.values());
        result.sort(Comparator.comparingLong(r -> r.id));
        return result;
    }

    public Path csvFile() { return historyCsv; }

    public void shutdown() { worker.shutdownNow(); }
}
