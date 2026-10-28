package strategy.pruning;

import core.db.SparseVerticalDB;
import core.model.Pattern;

public interface IPruningStrategy {
    double evaluateRPB(Pattern pattern, SparseVerticalDB db);
    boolean shouldPrune(Pattern pattern, SparseVerticalDB db, double minSup);
}