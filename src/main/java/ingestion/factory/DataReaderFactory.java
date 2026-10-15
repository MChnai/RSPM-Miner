package ingestion.factory;

import ingestion.IDataReader;

public abstract class DataReaderFactory {
    public abstract IDataReader createReader(String sourcePath);
}