package strategy.pruning;

import core.db.SparseVerticalDB;
import core.model.Pattern;

public class NoPruningStrategy implements IPruningStrategy {

    @Override
    public double evaluateRPB(Pattern pattern, SparseVerticalDB db) {
        return Double.MAX_VALUE;
    }

    @Override
    public boolean shouldPrune(Pattern pattern, SparseVerticalDB db, double minSup) {
        return false;
    }
}