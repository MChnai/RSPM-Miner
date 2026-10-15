package strategy.counting;

import core.db.PositionList;
import core.iterator.IPositionIterator;
import core.model.Interval;
import core.model.Pattern;
import java.util.ArrayList;
import java.util.List;

public class TwoPointerNonOverlappingCounter implements ICountingStrategy {

    @Override
    public List<Interval> sJoin(PositionList posListA, PositionList posListB) {
        List<Interval> matches = new ArrayList<>();
        if (posListA == null || posListB == null || posListA.isEmpty() || posListB.isEmpty()) {
            return matches;
        }

        IPositionIterator iterA = posListA.createIterator();
        IPositionIterator iterB = posListB.createIterator();

        while (iterA.hasNext() && iterB.hasNext()) {
            int valA = iterA.peek();
            iterB.advanceTo(valA);

            if (iterB.hasNext()) {
                int valB = iterB.next();
                matches.add(new Interval(valA, valB));
                iterA.next();
            }
        }

        return matches;
    }

    @Override
    public int countSupport(Pattern pattern) {
        if (pattern == null) {
            return 0;
        }
        return pattern.getSupport();
    }
}