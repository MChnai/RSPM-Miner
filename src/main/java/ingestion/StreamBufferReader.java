package ingestion;

import core.db.SparseVerticalDB;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class StreamBufferReader implements IDataReader {
    private final String filePath;

    public StreamBufferReader(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public SparseVerticalDB readData() throws IOException {
        SparseVerticalDB db = new SparseVerticalDB();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
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