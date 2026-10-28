package ingestion.factory;

import ingestion.IDataReader;
import ingestion.StreamBufferReader;

public class StreamBufferReaderFactory extends DataReaderFactory {
    @Override
    public IDataReader createReader(String sourcePath) {
        return new StreamBufferReader(sourcePath);
    }
}