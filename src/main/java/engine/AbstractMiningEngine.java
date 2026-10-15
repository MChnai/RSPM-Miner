package engine;

import core.db.SparseVerticalDB;
import core.model.Pattern;
import ingestion.IDataReader;
import strategy.counting.ICountingStrategy;
import strategy.pruning.IPruningStrategy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractMiningEngine {
    protected final IDataReader dataReader;
    protected final ICountingStrategy countingStrategy;
    protected final IPruningStrategy pruningStrategy;
    protected final double minSup;

    protected SparseVerticalDB verticalDB;
    protected List<Pattern> frequent1Itemsets;
    protected List<Pattern> minedPatterns;

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
        loadData();
        buildVerticalDB();
        findL1();
        runMiningCore();
        return exportResults();
    }

    protected void loadData() throws IOException {
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