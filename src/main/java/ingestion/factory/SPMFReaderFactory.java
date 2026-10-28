package ingestion.factory;

import ingestion.IDataReader;
import ingestion.SPMFDataReader;

public class SPMFReaderFactory extends DataReaderFactory {
    @Override
    public IDataReader createReader(String sourcePath) {
        return new SPMFDataReader(sourcePath);
    }
}