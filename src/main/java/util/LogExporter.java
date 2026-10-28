package util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogExporter {

    private final String logFilePath;
    private final String csvFilePath;

    public LogExporter(String datasetName) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        this.logFilePath = "logs/execution_" + datasetName + "_" + timestamp + ".log";
        this.csvFilePath = "results/benchmark_summary.csv";
        
        // Tạo thư mục nếu chưa tồn tại
        new java.io.File("logs").mkdirs();
        new java.io.File("results").mkdirs();
        
        initCSVHeader();
    }

    private void initCSVHeader() {
        java.io.File csvFile = new java.io.File(csvFilePath);
        if (!csvFile.exists()) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile, true))) {
                writer.println("Dataset,Algorithm,Variant,MinSup,PatternsCount,CandidatesCount,RuntimeMs,PeakMemoryMB,JaccardExactness");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void logMessage(String message) {
        System.out.println(message); // In ra Console
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFilePath, true))) {
            writer.println(message); // In vào file Log
        } catch (IOException e) {
            System.err.println("Error writing to log file: " + e.getMessage());
        }
    }

    public void exportMetricsToCSV(String dataset, String algorithm, String variant, double minSup, 
                                   int patternCount, int candidateCount, long runtimeMs, double memoryMB, double jaccard) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFilePath, true))) {
            writer.printf("%s,%s,%s,%.2f,%d,%d,%d,%.2f,%.3f\n",
                    dataset, algorithm, variant, minSup, patternCount, candidateCount, runtimeMs, memoryMB, jaccard);
        } catch (IOException e) {
            System.err.println("Error writing to CSV: " + e.getMessage());
        }
    }

    public String getLogFilePath() {
        return logFilePath;
    }
}