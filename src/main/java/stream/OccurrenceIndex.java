package stream;

import core.model.Interval;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OccurrenceIndex {
    private final Map<String, Map<Integer, List<Interval>>> index;

    public OccurrenceIndex() {
        this.index = new HashMap<>();
    }

    public void addOccurrence(String patternSeq, int sid, Interval interval) {
        index.computeIfAbsent(patternSeq, k -> new HashMap<>())
             .computeIfAbsent(sid, k -> new ArrayList<>())
             .add(interval);
    }

    public void removeExpiredSid(int expiredSid) {
        for (Map<Integer, List<Interval>> sidMap : index.values()) {
            sidMap.remove(expiredSid);
        }
    }

    public Map<Integer, List<Interval>> getOccurrences(String patternSeq) {
        return index.getOrDefault(patternSeq, new HashMap<>());
    }

    public void clear() {
        index.clear();
    }
}