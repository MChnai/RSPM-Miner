package strategy.counting;

import core.db.PositionList;
import core.model.Interval;
import core.model.Pattern;
import java.util.List;

public interface ICountingStrategy {
    List<Interval> sJoin(PositionList posListA, PositionList posListB);
    int countSupport(Pattern pattern);
}