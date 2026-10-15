package engine;

import core.db.PositionList;
import core.model.Interval;
import core.model.Pattern;
import ingestion.IDataReader;
import memory.BitmapFlyweightFactory;
import strategy.counting.ICountingStrategy;
import strategy.pruning.IPruningStrategy;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

public class StaticORNMinerEngine extends AbstractMiningEngine {
    private final BitmapFlyweightFactory flyweightFactory;
    private static final int MAX_PATTERN_LENGTH = 10;

    public StaticORNMinerEngine(IDataReader dataReader, 
                                ICountingStrategy countingStrategy, 
                                IPruningStrategy pruningStrategy, 
                                double minSup) {
        super(dataReader, countingStrategy, pruningStrategy, minSup);
        this.flyweightFactory = BitmapFlyweightFactory.getInstance();
    }

    @Override
    protected void findL1() {
        frequent1Itemsets.clear();
        for (Map.Entry<Integer, Map<Integer, PositionList>> entry : verticalDB.getRawMatrix().entrySet()) {
            int itemId = entry.getKey();
            Map<Integer, PositionList> sidMap = entry.getValue();

            Pattern p = new Pattern(String.valueOf(itemId));
            for (Map.Entry<Integer, PositionList> sidEntry : sidMap.entrySet()) {
                int sid = sidEntry.getKey();
                PositionList posList = sidEntry.getValue();
                int[] positions = posList.toArray();
                for (int pos : positions) {
                    p.addOccurrence(sid, new Interval(pos, pos));
                }
            }

            if (p.getSupport() >= minSup) {
                frequent1Itemsets.add(p);
                minedPatterns.add(p);
            }
        }
    }

    @Override
    protected void runMiningCore() {
        for (Pattern p : frequent1Itemsets) {
            runDFS(p, 1);
        }
    }

    private void runDFS(Pattern parentPattern, int currentDepth) {
        if (currentDepth >= MAX_PATTERN_LENGTH) {
            return;
        }

        if (pruningStrategy.shouldPrune(parentPattern, verticalDB, minSup)) {
            return;
        }

        List<Pattern> extensions = generateExtensions(parentPattern);
        for (Pattern candidate : extensions) {
            if (candidate.getSupport() >= minSup) {
                minedPatterns.add(candidate);
                runDFS(candidate, currentDepth + 1);
            }
        }
    }

    private List<Pattern> generateExtensions(Pattern parentPattern) {
        List<Pattern> candidates = new ArrayList<>();
        
        for (Pattern itemPattern : frequent1Itemsets) {
            Pattern sCandidate = generateSExtension(parentPattern, itemPattern);
            if (sCandidate != null && sCandidate.getSupport() >= minSup) {
                candidates.add(sCandidate);
            }

            Pattern iCandidate = generateIExtension(parentPattern, itemPattern);
            if (iCandidate != null && iCandidate.getSupport() >= minSup) {
                candidates.add(iCandidate);
            }
        }
        return candidates;
    }

    private Pattern generateSExtension(Pattern parent, Pattern item) {
        String newSeq = parent.getSequence() + "-" + item.getSequence();
        Pattern candidate = new Pattern(newSeq);

        for (Map.Entry<Integer, List<Interval>> entry : parent.getOccurrences().entrySet()) {
            int sid = entry.getKey();
            try {
                int itemId = Integer.parseInt(item.getSequence());
                PositionList posListB = verticalDB.getPositionList(itemId, sid);
                if (posListB == null || posListB.isEmpty()) {
                    continue;
                }

                PositionList posListA = new PositionList();
                for (Interval interval : entry.getValue()) {
                    posListA.addPosition(interval.getEnd());
                }

                List<Interval> matches = countingStrategy.sJoin(posListA, posListB);
                for (Interval match : matches) {
                    candidate.addOccurrence(sid, match);
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return candidate;
    }

    private Pattern generateIExtension(Pattern parent, Pattern item) {
        if (parent.getSequence().endsWith(item.getSequence())) {
            return null;
        }

        String newSeq = parent.getSequence() + "," + item.getSequence();
        Pattern candidate = new Pattern(newSeq);

        BitSet bitSetA = flyweightFactory.getBitSet();
        BitSet bitSetB = flyweightFactory.getBitSet();

        try {
            for (Map.Entry<Integer, List<Interval>> entry : parent.getOccurrences().entrySet()) {
                int sid = entry.getKey();
                try {
                    int itemId = Integer.parseInt(item.getSequence());
                    PositionList posListB = verticalDB.getPositionList(itemId, sid);
                    if (posListB == null || posListB.isEmpty()) {
                        continue;
                    }

                    bitSetA.clear();
                    bitSetB.clear();

                    for (Interval interval : entry.getValue()) {
                        bitSetA.set(interval.getEnd());
                    }

                    int[] positionsB = posListB.toArray();
                    for (int pos : positionsB) {
                        bitSetB.set(pos);
                    }

                    bitSetA.and(bitSetB);

                    for (int pos = bitSetA.nextSetBit(0); pos >= 0; pos = bitSetA.nextSetBit(pos + 1)) {
                        candidate.addOccurrence(sid, new Interval(pos, pos));
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        } finally {
            flyweightFactory.release(bitSetA);
            flyweightFactory.release(bitSetB);
        }

        return candidate;
    }
}