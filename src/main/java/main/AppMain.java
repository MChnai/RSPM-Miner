package main;

import core.model.Pattern;
import pipeline.MiningPipeline;
import pipeline.MiningPipelineBuilder;
import strategy.counting.TwoPointerNonOverlappingCounter;
import strategy.pruning.NoPruningStrategy;
import strategy.pruning.RPBPruningStrategy;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class AppMain {

    public static void main(String[] args) {
    	while(true) {
    		System.out.println("=================================================================");
            System.out.println("  RSPM-FRAMEWORK: REPETITIVE SEQUENTIAL PATTERN MINING SYSTEM  ");
            System.out.println("=================================================================");

            Scanner scanner = new Scanner(System.in);

            // VỊ TRÍ 1: In thêm lựa chọn 4 ra menu
            System.out.println("\nSelect Execution Mode:");
            System.out.println("1. Static ORN-Miner (Data File)");
            System.out.println("2. Streaming SRN-Miner (Sliding Window)");
            System.out.println("3. Run Demo Benchmark with Sample Data");
            System.out.println("4. Run Large-Scale Benchmark & Scalability Test (RQ1 & RQ2)");
            System.out.print("Enter choice (1-4): ");

            int choice = 1;
            if (scanner.hasNextInt()) {
                choice = scanner.nextInt();
            }
            scanner.nextLine();

            try {
                // VỊ TRÍ 2: Thêm case 4 vào switch-case
                switch (choice) {
                    case 1 -> runStaticMode(scanner);
                    case 2 -> runStreamingMode(scanner);
                    case 3 -> runDemoBenchmark();
                    case 4 -> runLargeScaleBenchmark(scanner); // <--- Đã gán hàm vào đây
                    default -> {
                        System.out.println("Invalid choice. Running default Demo Benchmark.");
                        runDemoBenchmark();
                    }
                }
            } catch (Exception e) {
                System.err.println("Execution Error: " + e.getMessage());
                e.printStackTrace();
            }
    	}
    }

    private static void runStaticMode(Scanner scanner) throws IOException {
        System.out.print("Enter dataset file path (e.g., data/Bibles.txt): ");
        String path = scanner.next();

        System.out.print("Enter minimum support threshold (minSup, e.g., 2.0): ");
        double minSup = scanner.nextDouble();

        MiningPipeline pipeline = new MiningPipelineBuilder()
                .setDataSourcePath(path)
                .setMinSup(minSup)
                .setExecutionMode(MiningPipeline.ExecutionMode.STATIC_ORN)
                .setCountingStrategy(new TwoPointerNonOverlappingCounter())
                .setPruningStrategy(new RPBPruningStrategy())
                .build();

        executeAndMeasure(pipeline);
    }

    private static void runStreamingMode(Scanner scanner) throws IOException {
        System.out.print("Enter dataset file path (e.g., data/FIFA.txt): ");
        String path = scanner.next();

        System.out.print("Enter minimum support threshold (minSup, e.g., 2.0): ");
        double minSup = scanner.nextDouble();

        System.out.print("Enter Sliding Window Size (e.g., 100): ");
        int windowSize = scanner.nextInt();

        MiningPipeline pipeline = new MiningPipelineBuilder()
                .setDataSourcePath(path)
                .setMinSup(minSup)
                .setWindowSize(windowSize)
                .setExecutionMode(MiningPipeline.ExecutionMode.STREAMING_SRN)
                .setCountingStrategy(new TwoPointerNonOverlappingCounter())
                .setPruningStrategy(new RPBPruningStrategy())
                .build();

        executeAndMeasure(pipeline);
    }

    private static void runDemoBenchmark() throws IOException {
        System.out.println("\n--- RUNNING DEMO BENCHMARK ---");

        File tempFile = File.createTempFile("demo_spmf", ".txt");
        tempFile.deleteOnExit();

        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("1 -1 2 -1 1 -1 2 -1 -2\n");
            writer.write("1 -1 2 -1 -2\n");
            writer.write("2 -1 1 -1 2 -1 -2\n");
        }

        System.out.println("\n1. Benchmarking ORN-Miner WITH RPB Pruning Strategy:");
        MiningPipeline rpbPipeline = new MiningPipelineBuilder()
                .setDataSourcePath(tempFile.getAbsolutePath())
                .setMinSup(2.0)
                .setExecutionMode(MiningPipeline.ExecutionMode.STATIC_ORN)
                .setCountingStrategy(new TwoPointerNonOverlappingCounter())
                .setPruningStrategy(new RPBPruningStrategy())
                .build();
        executeAndMeasure(rpbPipeline);

        System.out.println("\n2. Benchmarking ORN-Miner WITHOUT Pruning (Ablation Study):");
        MiningPipeline noPrunePipeline = new MiningPipelineBuilder()
                .setDataSourcePath(tempFile.getAbsolutePath())
                .setMinSup(2.0)
                .setExecutionMode(MiningPipeline.ExecutionMode.STATIC_ORN)
                .setCountingStrategy(new TwoPointerNonOverlappingCounter())
                .setPruningStrategy(new NoPruningStrategy())
                .build();
        executeAndMeasure(noPrunePipeline);
    }

    private static void runLargeScaleBenchmark(Scanner scanner) throws IOException {
        System.out.println("\n=================================================================");
        System.out.println("  LARGE-SCALE BENCHMARK & SCALABILITY TEST (RQ1 & RQ2)           ");
        System.out.println("=================================================================");

        System.out.print("Nhập số lượng chuỗi muốn sinh thử nghiệm (ví dụ: 10000 hoặc 50000): ");
        int numSeq = 10000;
        if (scanner.hasNextInt()) {
            numSeq = scanner.nextInt();
        }

        String largeFilePath = "data/benchmark_large_" + numSeq + ".txt";
        System.out.println("-> Đang tự động sinh file dữ liệu SPMF: " + largeFilePath + "...");
        
        util.SPMFDataGenerator.generateLargeDataset(largeFilePath, numSeq, 40, 30, 0.65);
        System.out.println("-> Sinh dữ liệu hoàn tất! Tiến hành đo đạc hiệu năng...");

        System.out.print("Nhập ngưỡng minSup tuyệt đối (ví dụ: 50 hoặc 100): ");
        double minSup = 50.0;
        if (scanner.hasNextDouble()) {
            minSup = scanner.nextDouble();
        }

        // 1. Benchmark có bật RPB Pruning Strategy
        System.out.println("\n[1] CHẠY THỬ NGHIỆM VỚI ORN-MINER (CÓ RPB PRUNING):");
        MiningPipeline rpbPipeline = new MiningPipelineBuilder()
                .setDataSourcePath(largeFilePath)
                .setMinSup(minSup)
                .setExecutionMode(MiningPipeline.ExecutionMode.STATIC_ORN)
                .setCountingStrategy(new TwoPointerNonOverlappingCounter())
                .setPruningStrategy(new RPBPruningStrategy())
                .build();
        executeAndMeasure(rpbPipeline);

        // 2. Benchmark Ablation Study (Không có RPB Pruning)
        System.out.println("\n[2] CHẠY THỬ NGHIỆM ĐỐI CHỨNG (ABLATION - KHÔNG RPB PRUNING):");
        MiningPipeline noPrunePipeline = new MiningPipelineBuilder()
                .setDataSourcePath(largeFilePath)
                .setMinSup(minSup)
                .setExecutionMode(MiningPipeline.ExecutionMode.STATIC_ORN)
                .setCountingStrategy(new TwoPointerNonOverlappingCounter())
                .setPruningStrategy(new NoPruningStrategy())
                .build();
        executeAndMeasure(noPrunePipeline);
    }

    private static void executeAndMeasure(MiningPipeline pipeline) throws IOException {
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();

        long startMemory = runtime.totalMemory() - runtime.freeMemory();
        long startTime = System.currentTimeMillis();

        List<Pattern> patterns = pipeline.execute();

        long endTime = System.currentTimeMillis();
        long endMemory = runtime.totalMemory() - runtime.freeMemory();

        long elapsedTime = endTime - startTime;
        double memoryUsedMB = (endMemory - startMemory) / (1024.0 * 1024.0);
        if (memoryUsedMB < 0) {
            memoryUsedMB = 0.01;
        }

        System.out.println("-----------------------------------------------------------------");
        System.out.println("Execution Mode      : " + pipeline.getMode());
        System.out.println("Frequent Patterns   : " + patterns.size());
        System.out.println("Execution Time (ms) : " + elapsedTime + " ms");
        System.out.println("Peak Memory (MB)    : " + String.format("%.2f", memoryUsedMB) + " MB");
        System.out.println("-----------------------------------------------------------------");

        System.out.println("Mined Frequent Patterns:");
        for (Pattern p : patterns) {
            System.out.println("  -> Pattern: " + p.getSequence() + " | Non-Overlapping Support: " + p.getSupport());
        }
    }
}