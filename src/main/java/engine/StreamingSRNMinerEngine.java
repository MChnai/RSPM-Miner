package engine;

import core.db.PositionList;
import core.model.Interval;
import core.model.Pattern;
import ingestion.IDataReader;
import memory.BitmapFlyweightFactory;
import strategy.counting.ICountingStrategy;
import strategy.pruning.IPruningStrategy;
import stream.OccurrenceIndex;
import stream.SlidingWindow;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

public class StreamingSRNMinerEngine extends AbstractMiningEngine {
    private final SlidingWindow window;
    private final OccurrenceIndex occurrenceIndex;
    private final BitmapFlyweightFactory flyweightFactory;
    private static final int MAX_PATTERN_LENGTH = 10;

    public StreamingSRNMinerEngine(IDataReader dataReader, 
                                  ICountingStrategy countingStrategy, 
                                  IPruningStrategy pruningStrategy, 
                                  double minSup, 
                                  int windowSize) {
        super(dataReader, countingStrategy, pruningStrategy, minSup);
        this.window = new SlidingWindow(windowSize);
        this.occurrenceIndex = new OccurrenceIndex();
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
                    occurrenceIndex.addOccurrence(p.getSequence(), sid, new Interval(pos, pos));
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

        try {
            int itemId = Integer.parseInt(item.getSequence());
            
            for (Map.Entry<Integer, List<Interval>> entry : parent.getOccurrences().entrySet()) {
                int sid = entry.getKey();
                PositionList posListB = verticalDB.getPositionList(itemId, sid);
                if (posListB == null || posListB.isEmpty()) {
                    continue;
                }

                PositionList posListA = new PositionList();
                List<Interval> parentIntervals = entry.getValue();
                for (int i = 0; i < parentIntervals.size(); i++) {
                    posListA.addPosition(parentIntervals.get(i).getEnd());
                }

                List<Interval> matches = countingStrategy.sJoin(posListA, posListB);
                for (int i = 0; i < matches.size(); i++) {
                    Interval match = matches.get(i);
                    candidate.addOccurrence(sid, match);
                    occurrenceIndex.addOccurrence(newSeq, sid, match);
                }
            }
        } catch (NumberFormatException ignored) {
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
            int itemId = Integer.parseInt(item.getSequence());

            for (Map.Entry<Integer, List<Interval>> entry : parent.getOccurrences().entrySet()) {
                int sid = entry.getKey();
                PositionList posListB = verticalDB.getPositionList(itemId, sid);
                if (posListB == null || posListB.isEmpty()) {
                    continue;
                }

                bitSetA.clear();
                bitSetB.clear();

                List<Interval> parentIntervals = entry.getValue();
                for (int i = 0; i < parentIntervals.size(); i++) {
                    bitSetA.set(parentIntervals.get(i).getEnd());
                }

                int[] positionsB = posListB.toArray();
                for (int pos : positionsB) {
                    bitSetB.set(pos);
                }

                bitSetA.and(bitSetB);

                for (int pos = bitSetA.nextSetBit(0); pos >= 0; pos = bitSetA.nextSetBit(pos + 1)) {
                    Interval interval = new Interval(pos, pos);
                    candidate.addOccurrence(sid, interval);
                    occurrenceIndex.addOccurrence(newSeq, sid, interval);
                }
            }
        } catch (NumberFormatException ignored) {
        } finally {
            flyweightFactory.release(bitSetA);
            flyweightFactory.release(bitSetB);
        }

        return candidate;
    }

    public void processSnapshot(int expiredSid) {
        window.addSequence(expiredSid);
        Integer toRemove = window.evictExpired();
        if (toRemove != null) {
            occurrenceIndex.removeExpiredSid(toRemove);
        }
    }
}