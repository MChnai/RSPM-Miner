package ingestion;

import core.db.SparseVerticalDB;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class SPMFDataReader implements IDataReader {
    private final String filePath;

    public SPMFDataReader(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public SparseVerticalDB readData() throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("Cannot find file at Absolute Path: " + file.getAbsolutePath());
        }

        SparseVerticalDB db = new SparseVerticalDB();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            int sid = 0;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("@")) {
                    continue;
                }
                String[] tokens = line.split("\\s+");
                int itemsetIndex = 0;
                for (String token : tokens) {
                    try {
                        int itemId = Integer.parseInt(token);
                        db.addOccurrence(itemId, sid, itemsetIndex);
                        itemsetIndex++;
                    } catch (NumberFormatException ignored) {
                    }
                }
                sid++;
            }
        }
        return db;
    }
}