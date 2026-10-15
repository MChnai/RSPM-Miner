package pipeline;

import engine.AbstractMiningEngine;
import engine.StaticORNMinerEngine;
import engine.StreamingSRNMinerEngine;
import ingestion.IDataReader;
import ingestion.factory.DataReaderFactory;
import ingestion.factory.SPMFReaderFactory;
import ingestion.factory.StreamBufferReaderFactory;
import strategy.counting.ICountingStrategy;
import strategy.counting.TwoPointerNonOverlappingCounter;
import strategy.pruning.IPruningStrategy;
import strategy.pruning.RPBPruningStrategy;

import core.model.Pattern;
import java.io.IOException;
import java.util.List;

public class MiningPipeline {
    public enum ExecutionMode {
        STATIC_ORN,
        STREAMING_SRN
    }

    private final String dataSourcePath;
    private final double minSup;
    private final ExecutionMode mode;
    private final int windowSize;
    private final ICountingStrategy countingStrategy;
    private final IPruningStrategy pruningStrategy;

    MiningPipeline(String dataSourcePath, double minSup, ExecutionMode mode, int windowSize, 
                   ICountingStrategy countingStrategy, IPruningStrategy pruningStrategy) {
        this.dataSourcePath = dataSourcePath;
        this.minSup = minSup;
        this.mode = mode;
        this.windowSize = windowSize;
        this.countingStrategy = countingStrategy != null ? countingStrategy : new TwoPointerNonOverlappingCounter();
        this.pruningStrategy = pruningStrategy != null ? pruningStrategy : new RPBPruningStrategy();
    }

    public List<Pattern> execute() throws IOException {
        DataReaderFactory readerFactory;
        AbstractMiningEngine engine;

        if (mode == ExecutionMode.STREAMING_SRN) {
            readerFactory = new StreamBufferReaderFactory();
            IDataReader reader = readerFactory.createReader(dataSourcePath);
            engine = new StreamingSRNMinerEngine(reader, countingStrategy, pruningStrategy, minSup, windowSize);
        } else {
            readerFactory = new SPMFReaderFactory();
            IDataReader reader = readerFactory.createReader(dataSourcePath);
            engine = new StaticORNMinerEngine(reader, countingStrategy, pruningStrategy, minSup);
        }

        return engine.mine();
    }

    public String getDataSourcePath() {
        return dataSourcePath;
    }

    public double getMinSup() {
        return minSup;
    }

    public ExecutionMode getMode() {
        return mode;
    }

    public int getWindowSize() {
        return windowSize;
    }
}