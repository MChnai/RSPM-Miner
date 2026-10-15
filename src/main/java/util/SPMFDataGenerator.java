package util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class SPMFDataGenerator {

    public static void generateLargeDataset(String outputPath, 
                                           int numSequences, 
                                           int maxSeqLength, 
                                           int numDistinctItems, 
                                           double repetitionDensity) throws IOException {
        
        File file = new File(outputPath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        Random random = new Random(42);
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file), 32768)) {
            for (int i = 0; i < numSequences; i++) {
                int seqLength = random.nextInt(maxSeqLength - 5) + 5;
                StringBuilder sb = new StringBuilder();

                for (int pos = 0; pos < seqLength; pos++) {
                    int itemId;
                    
                    if (random.nextDouble() < repetitionDensity) {
                        itemId = random.nextInt(Math.max(1, numDistinctItems / 4)) + 1;
                    } else {
                        itemId = random.nextInt(numDistinctItems) + 1;
                    }

                    sb.append(itemId).append(" -1 ");
                }
                
                sb.append("-2\n");
                writer.write(sb.toString());
            }
            writer.flush();
        }
    }

    public static void main(String[] args) {
        try {
            String targetPath = "data/benchmark_large_50000.txt";
            System.out.println("Đang tiến hành sinh file dữ liệu lớn quy mô 50.000 chuỗi...");
            long startTime = System.currentTimeMillis();

            generateLargeDataset(
                targetPath, 
                50000, 
                40,    
                30,    
                0.65   
            );

            long endTime = System.currentTimeMillis();
            File generatedFile = new File(targetPath);
            
            System.out.println("=================================================================");
            System.out.println("  HOÀN TẤT TẠO FILE BENCHMARK DỮ LIỆU LỚN                        ");
            System.out.println("=================================================================");
            System.out.println("Tên tệp xuất      : " + generatedFile.getName());
            System.out.println("Đường dẫn tuyệt đối: " + generatedFile.getAbsolutePath());
            System.out.println("Dung lượng tệp     : " + String.format("%.2f", generatedFile.length() / (1024.0 * 1024.0)) + " MB");
            System.out.println("Thời gian khởi tạo : " + (endTime - startTime) + " ms");
            System.out.println("=================================================================");

        } catch (IOException e) {
            System.err.println("Lỗi khi tạo dữ liệu: " + e.getMessage());
            e.printStackTrace();
        }
    }
}