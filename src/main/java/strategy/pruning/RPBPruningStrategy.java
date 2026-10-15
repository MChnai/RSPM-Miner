package strategy.pruning;

import core.db.PositionList;
import core.db.SparseVerticalDB;
import core.iterator.IPositionIterator;
import core.model.Interval;
import core.model.Pattern;
import java.util.List;
import java.util.Map;

public class RPBPruningStrategy implements IPruningStrategy {

    @Override
    public double evaluateRPB(Pattern pattern, SparseVerticalDB db) {
        if (pattern == null || db == null) {
            return 0.0;
        }

        double totalRPB = 0.0;
        Map<Integer, List<Interval>> occurrences = pattern.getOccurrences();

        for (Map.Entry<Integer, List<Interval>> entry : occurrences.entrySet()) {
            int sid = entry.getKey();
            List<Interval> intervals = entry.getValue();

            int currentMatchCount = intervals.size();
            int potentialRemains = 0;

            if (!intervals.isEmpty()) {
                Interval lastInterval = intervals.get(intervals.size() - 1);
                int lastPos = lastInterval.getEnd();

                for (Map.Entry<Integer, Map<Integer, PositionList>> itemEntry : db.getRawMatrix().entrySet()) {
                    PositionList posList = itemEntry.getValue().get(sid);
                    if (posList != null) {
                        IPositionIterator iter = posList.createIterator();
                        iter.advanceTo(lastPos);
                        if (iter.hasNext()) {
                            potentialRemains++;
                            break;
                        }
                    }
                }
            }

            totalRPB += (currentMatchCount + potentialRemains);
        }

        return totalRPB;
    }

    @Override
    public boolean shouldPrune(Pattern pattern, SparseVerticalDB db, double minSup) {
        double rpbValue = evaluateRPB(pattern, db);
        return rpbValue < minSup;
    }
}