package pipeline;

import strategy.counting.ICountingStrategy;
import strategy.pruning.IPruningStrategy;

public class MiningPipelineBuilder {
    private String dataSourcePath;
    private double minSup = 2.0;
    private MiningPipeline.ExecutionMode mode = MiningPipeline.ExecutionMode.STATIC_ORN;
    private int windowSize = 1000;
    private ICountingStrategy countingStrategy;
    private IPruningStrategy pruningStrategy;

    public MiningPipelineBuilder setDataSourcePath(String dataSourcePath) {
        this.dataSourcePath = dataSourcePath;
        return this;
    }

    public MiningPipelineBuilder setMinSup(double minSup) {
        this.minSup = minSup;
        return this;
    }

    public MiningPipelineBuilder setExecutionMode(MiningPipeline.ExecutionMode mode) {
        this.mode = mode;
        return this;
    }

    public MiningPipelineBuilder setWindowSize(int windowSize) {
        this.windowSize = windowSize;
        return this;
    }

    public MiningPipelineBuilder setCountingStrategy(ICountingStrategy countingStrategy) {
        this.countingStrategy = countingStrategy;
        return this;
    }

    public MiningPipelineBuilder setPruningStrategy(IPruningStrategy pruningStrategy) {
        this.pruningStrategy = pruningStrategy;
        return this;
    }

    public MiningPipeline build() {
        if (dataSourcePath == null || dataSourcePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Data source path must be specified.");
        }
        if (minSup <= 0) {
            throw new IllegalArgumentException("minSup must be greater than 0.");
        }
        return new MiningPipeline(dataSourcePath, minSup, mode, windowSize, countingStrategy, pruningStrategy);
    }
}