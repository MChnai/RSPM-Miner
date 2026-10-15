package ingestion;

import core.db.SparseVerticalDB;
import java.io.IOException;

public interface IDataReader {
    SparseVerticalDB readData() throws IOException;
}