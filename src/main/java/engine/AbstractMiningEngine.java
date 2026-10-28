package engine;

import core.db.SparseVerticalDB;
import core.model.Pattern;
import ingestion.IDataReader;
import strategy.counting.ICountingStrategy;
import strategy.pruning.IPruningStrategy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public abstract class AbstractMiningEngine {
    protected final IDataReader dataReader;
    protected final ICountingStrategy countingStrategy;
    protected final IPruningStrategy pruningStrategy;
    protected final double minSup;

    protected SparseVerticalDB verticalDB;
    protected List<Pattern> frequent1Itemsets;
    protected List<Pattern> minedPatterns;
    private BiConsumer<String, Double> stageListener = (stage, elapsedMs) -> {};

    public void setStageListener(BiConsumer<String, Double> listener) {
        this.stageListener = listener == null ? (stage, elapsedMs) -> {} : listener;
    }

    private void markStage(String stage, long started) {
        stageListener.accept(stage, (System.nanoTime() - started) / 1_000_000.0);
    }

    public AbstractMiningEngine(IDataReader dataReader, 
                                ICountingStrategy countingStrategy, 
                                IPruningStrategy pruningStrategy, 
                                double minSup) {
        this.dataReader = dataReader;
        this.countingStrategy = countingStrategy;
        this.pruningStrategy = pruningStrategy;
        this.minSup = minSup;
        this.frequent1Itemsets = new ArrayList<>();
        this.minedPatterns = new ArrayList<>();
    }

    public final List<Pattern> mine() throws IOException {
        long started = System.nanoTime();
        frequent1Itemsets.clear();
        minedPatterns.clear();

        loadData();
        markStage("Nạp dữ liệu", started);
        buildVerticalDB();
        markStage("Dựng Vertical DB", started);
        findL1();
        markStage("Tìm L1", started);
        runMiningCore();
        markStage("DFS / khai phá", started);
        List<Pattern> result = exportResults();
        markStage("Xuất kết quả", started);
        return result;
    }

    public void loadData() throws IOException {
        this.verticalDB = dataReader.readData();
    }

    protected void buildVerticalDB() {
    }

    protected abstract void findL1();

    protected abstract void runMiningCore();

    protected List<Pattern> exportResults() {
        return new ArrayList<>(minedPatterns);
    }

    public SparseVerticalDB getVerticalDB() {
        return verticalDB;
    }
}