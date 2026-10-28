package util;

import core.model.Pattern;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.List;

public class BenchmarkProfiler {

    private long startTime;
    private long endTime;
    private long startMemory;
    private long peakMemory;
    private int totalCandidatesGenerated = 0;
    private int totalCandidatesPruned = 0;

    public void start() {
        System.gc(); // Gợi ý dọn dẹp RAM trước khi đo
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        this.startMemory = memoryBean.getHeapMemoryUsage().getUsed();
        this.startTime = System.currentTimeMillis();
    }

    public void stop() {
        this.endTime = System.currentTimeMillis();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long currentMemory = memoryBean.getHeapMemoryUsage().getUsed();
        this.peakMemory = Math.max(0, currentMemory - startMemory);
    }

    public void incrementCandidateGenerated() {
        totalCandidatesGenerated++;
    }

    public void incrementCandidatePruned() {
        totalCandidatesPruned++;
    }

    public long getExecutionTimeMs() {
        return endTime - startTime;
    }

    public double getPeakMemoryMB() {
        return peakMemory / (1024.0 * 1024.0);
    }

    public int getTotalCandidatesGenerated() {
        return totalCandidatesGenerated;
    }

    public int getTotalCandidatesPruned() {
        return totalCandidatesPruned;
    }

    /**
     * Xuất kết quả thống kê ra file CSV phục vụ vẽ biểu đồ báo cáo
     */
    public void exportToCSV(String filePath, String datasetName, String algorithm, double minSup, List<Pattern> patterns) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath, true))) {
            // Header format: Dataset, Algorithm, MinSup, PatternsCount, Runtime_ms, PeakMemory_MB, CandidatesGen, CandidatesPruned
            writer.printf("%s,%s,%.2f,%d,%d,%.2f,%d,%d\n",
                    datasetName,
                    algorithm,
                    minSup,
                    patterns.size(),
                    getExecutionTimeMs(),
                    getPeakMemoryMB(),
                    totalCandidatesGenerated,
                    totalCandidatesPruned
            );
        } catch (IOException e) {
            System.err.println("Error writing benchmark CSV: " + e.getMessage());
        }
    }

    /**
     * Tính toán chỉ số Jaccard Similarity giữa 2 tập Pattern (RQ6)
     */
    public static double calculateJaccardSimilarity(List<Pattern> listA, List<Pattern> listB) {
        if (listA.isEmpty() && listB.isEmpty()) return 1.0;
        
        long intersectionCount = listA.stream()
                .filter(p1 -> listB.stream().anyMatch(p2 -> p2.getSequence().equals(p1.getSequence())))
                .count();

        long unionCount = listA.size() + listB.size() - intersectionCount;
        if (unionCount == 0) return 1.0;

        return (double) intersectionCount / unionCount;
    }
}