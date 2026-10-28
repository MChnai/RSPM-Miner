package engine;

import core.db.PositionList;
import core.model.Interval;
import core.model.Pattern;
import ingestion.IDataReader;
import memory.BitmapFlyweightFactory;
import strategy.counting.ICountingStrategy;
import strategy.pruning.IPruningStrategy;
import util.LogExporter;

import java.util.*;

public class StaticORNMinerEngine extends AbstractMiningEngine {

    private final BitmapFlyweightFactory flyweightFactory;
    private final List<Pattern> frequentIPatterns = new ArrayList<>();
    private static final int MAX_PATTERN_LENGTH = 10;
    private int totalSIDs = 0;
    
    // Khai báo LogExporter để điều hướng dữ liệu log
    private LogExporter logger;
    private boolean verbose = true;

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public List<Pattern> getMinedPatterns() {
        return minedPatterns;
    }

    public void setLogger(LogExporter logger) {
        this.logger = logger;
    }

    public StaticORNMinerEngine(IDataReader dataReader, 
                                ICountingStrategy countingStrategy, 
                                IPruningStrategy pruningStrategy, 
                                double minSup) {
        super(dataReader, countingStrategy, pruningStrategy, minSup);
        this.flyweightFactory = BitmapFlyweightFactory.getInstance();
    }

    // Constructor mở rộng nhận trực tiếp LogExporter
    public StaticORNMinerEngine(IDataReader dataReader, 
                                ICountingStrategy countingStrategy, 
                                IPruningStrategy pruningStrategy, 
                                double minSup,
                                LogExporter logger) {
        super(dataReader, countingStrategy, pruningStrategy, minSup);
        this.flyweightFactory = BitmapFlyweightFactory.getInstance();
        this.logger = logger;
    }

    // Helper method nội bộ hỗ trợ ghi log an toàn (nếu logger chưa khởi tạo thì fallback về System.out)
    private void log(String message) {
        if (!verbose) return;
        if (logger != null) {
            logger.logMessage(message);
        } else {
            System.out.println(message);
        }
    }

    @Override
    protected void findL1() {
        frequent1Itemsets.clear();
        frequentIPatterns.clear();

        // Xác định tổng số SID trong database
        for (Map<Integer, PositionList> sidMap : verticalDB.getRawMatrix().values()) {
            for (int sid : sidMap.keySet()) {
                if (sid >= totalSIDs) totalSIDs = sid + 1;
            }
        }

        // ==========================================
        // PHASE 0: PREPROCESSING & BITMAP CONSTRUCTION
        // ==========================================
        log("\n=================================================================");
        log("  PHASE 0: DATA PREPROCESSING & BITMAP CONSTRUCTION              ");
        log("=================================================================");
        log("[Step 0.1] Sparse Vertical Database (Raw Occurrence Lists):");
        for (Map.Entry<Integer, Map<Integer, PositionList>> entry : verticalDB.getRawMatrix().entrySet()) {
            int itemId = entry.getKey();
            List<Integer> sids = new ArrayList<>(entry.getValue().keySet());
            Collections.sort(sids);
            log("  -> Item [" + itemId + "] present in SIDs: " + sids);
        }

        // ==========================================
        // PHASE 1: MINING FREQUENT 1-ITEMSETS
        // ==========================================
        log("\n=================================================================");
        log("  PHASE 1: MINING FREQUENT 1-ITEMSETS (minSup = " + minSup + ")    ");
        log("=================================================================");
        log("[Step 1.1] Generating Filtered Bitmaps & Calculating Support:");

        for (Map.Entry<Integer, Map<Integer, PositionList>> entry : verticalDB.getRawMatrix().entrySet()) {
            int itemId = entry.getKey();
            Pattern p = new Pattern(String.valueOf(itemId));
            
            for (Map.Entry<Integer, PositionList> sidEntry : entry.getValue().entrySet()) {
                int sid = sidEntry.getKey();
                PositionList posList = sidEntry.getValue();
                for (int pos : posList.getRawList()) {
                    p.addOccurrence(sid, new Interval(pos, pos));
                }
            }

            int[] bitmap = buildBitmapArray(itemId);
            log("  -> Item [" + itemId + "] Bitmap: " + Arrays.toString(bitmap) + " | Support = " + p.getSupport() + 
                (p.getSupport() >= minSup ? " >= " + minSup + " [FREQUENT]" : " < " + minSup + " [PRUNED]"));

            if (p.getSupport() >= minSup) {
                frequent1Itemsets.add(p);
                minedPatterns.add(p);
            }
        }
    }

    @Override
    protected void runMiningCore() {
        // ==========================================
        // PHASE 2: I-EXTENSION EXPANSION
        // ==========================================
        log("\n=================================================================");
        log("  PHASE 2: I-EXTENSION EXPANSION (BITWISE AND KERNEL)            ");
        log("=================================================================");
        generateFrequentIPatterns();

        // ==========================================
        // PHASE 3: S-EXTENSION EXPANSION
        // ==========================================
        log("\n=================================================================");
        log("  PHASE 3: S-EXTENSION EXPANSION (TWO-POINTER POSITIONAL SCAN)   ");
        log("=================================================================");
        List<Pattern> basePatterns = new ArrayList<>(minedPatterns);
        for (Pattern basePattern : basePatterns) {
            runDFS(basePattern, 1);
        }
    }

    private void generateFrequentIPatterns() {
        frequentIPatterns.addAll(frequent1Itemsets);
        int size = frequent1Itemsets.size();

        for (int i = 0; i < size; i++) {
            for (int j = i + 1; j < size; j++) {
                Pattern itemA = frequent1Itemsets.get(i);
                Pattern itemB = frequent1Itemsets.get(j);

                int itemIdA = Integer.parseInt(itemA.getSequence());
                int itemIdB = Integer.parseInt(itemB.getSequence());

                int[] mapA = buildBitmapArray(itemIdA);
                int[] mapB = buildBitmapArray(itemIdB);
                int[] resultBit = new int[totalSIDs];
                int support = 0;

                for (int k = 0; k < totalSIDs; k++) {
                    resultBit[k] = mapA[k] & mapB[k];
                    if (resultBit[k] == 1) support++;
                }

                log("  -> Bitwise AND (" + itemIdA + ", " + itemIdB + "): " + 
                    Arrays.toString(mapA) + " AND " + Arrays.toString(mapB) + 
                    " = " + Arrays.toString(resultBit) + " | Support = " + support +
                    (support >= minSup ? " >= " + minSup + " [FREQUENT I-PATTERN]" : " < " + minSup + " [PRUNED]"));

                Pattern iCandidate = generateIExtension(itemA, itemB);
                if (iCandidate != null && iCandidate.getSupport() >= minSup) {
                    if (!containsPattern(minedPatterns, iCandidate.getSequence())) {
                        frequentIPatterns.add(iCandidate);
                        minedPatterns.add(iCandidate);
                    }
                }
            }
        }
    }

    private void runDFS(Pattern parentPattern, int currentDepth) {
        if (currentDepth >= MAX_PATTERN_LENGTH) return;
        if (pruningStrategy.shouldPrune(parentPattern, verticalDB, minSup)) return;

        for (Pattern extensionPattern : frequentIPatterns) {
            Pattern sCandidate = generateSExtension(parentPattern, extensionPattern);
            if (sCandidate != null && sCandidate.getSupport() >= minSup) {
                if (!containsPattern(minedPatterns, sCandidate.getSequence())) {
                    log("  -> [Two-Pointer Scan Match] Parent: " + parentPattern.getFormattedSequence() + 
                        " + Extension: " + extensionPattern.getFormattedSequence() + 
                        " ==> Mined Candidate: " + sCandidate.getFormattedSequence() + 
                        " | Non-Overlapping Support = " + sCandidate.getSupport());
                    minedPatterns.add(sCandidate);
                    runDFS(sCandidate, currentDepth + 1);
                }
            }
        }
    }

    private int[] buildBitmapArray(int itemId) {
        int[] bitmap = new int[totalSIDs];
        Map<Integer, PositionList> sidMap = verticalDB.getItemVerticalData(itemId);
        for (int sid : sidMap.keySet()) {
            if (sid < totalSIDs) bitmap[sid] = 1;
        }
        return bitmap;
    }

    private Pattern generateSExtension(Pattern parent, Pattern extension) {
        String newSeq = parent.getSequence() + "-" + extension.getSequence();
        Pattern candidate = new Pattern(newSeq);

        for (Map.Entry<Integer, List<Interval>> entry : parent.getOccurrences().entrySet()) {
            int sid = entry.getKey();
            List<Interval> parentIntervals = entry.getValue();

            PositionList posListA = new PositionList();
            for (Interval inter : parentIntervals) {
                posListA.addPosition(inter.getEnd());
            }

            PositionList posListB = getExtensionPositionList(extension, sid);
            if (posListB == null || posListB.isEmpty()) continue;

            List<Interval> matches = countingStrategy.sJoin(posListA, posListB);
            for (Interval match : matches) {
                candidate.addOccurrence(sid, match);
            }
        }
        return candidate;
    }

    private PositionList getExtensionPositionList(Pattern pattern, int sid) {
        String seq = pattern.getSequence();
        if (!seq.contains(",")) {
            try {
                int itemId = Integer.parseInt(seq);
                return verticalDB.getPositionList(itemId, sid);
            } catch (NumberFormatException e) {
                return null;
            }
        } else {
            PositionList posList = new PositionList();
            List<Interval> intervals = pattern.getOccurrences().get(sid);
            if (intervals != null) {
                for (Interval inter : intervals) {
                    posList.addPosition(inter.getStart());
                }
            }
            return posList;
        }
    }

    private Pattern generateIExtension(Pattern parent, Pattern item) {
        if (parent.getSequence().contains(item.getSequence())) return null;

        String newSeq = parent.getSequence() + "," + item.getSequence();
        Pattern candidate = new Pattern(newSeq);

        BitSet bitSetA = flyweightFactory.getBitSet();
        BitSet bitSetB = flyweightFactory.getBitSet();

        try {
            int itemIdA = Integer.parseInt(parent.getSequence());
            int itemIdB = Integer.parseInt(item.getSequence());

            Map<Integer, PositionList> sidMapA = verticalDB.getItemVerticalData(itemIdA);

            for (Map.Entry<Integer, PositionList> entry : sidMapA.entrySet()) {
                int sid = entry.getKey();
                PositionList posA = entry.getValue();
                PositionList posB = verticalDB.getPositionList(itemIdB, sid);

                if (posB == null || posB.isEmpty()) continue;

                bitSetA.clear();
                bitSetB.clear();

                for (int p : posA.getRawList()) bitSetA.set(p);
                for (int p : posB.getRawList()) bitSetB.set(p);

                bitSetA.and(bitSetB);

                for (int pos = bitSetA.nextSetBit(0); pos >= 0; pos = bitSetA.nextSetBit(pos + 1)) {
                    candidate.addOccurrence(sid, new Interval(pos, pos));
                }
            }
        } catch (NumberFormatException ignored) {
        } finally {
            flyweightFactory.release(bitSetA);
            flyweightFactory.release(bitSetB);
        }

        return candidate;
    }

    private boolean containsPattern(List<Pattern> list, String seq) {
        for (Pattern p : list) {
            if (p.getSequence().equals(seq)) return true;
        }
        return false;
    }
}