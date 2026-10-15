package core.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Pattern {
    private final String sequence; 
    private final Map<Integer, List<Interval>> occurrences;

    public Pattern(String sequence) {
        this.sequence = sequence;
        this.occurrences = new HashMap<>();
    }

    public String getSequence() {
        return sequence;
    }

    public Map<Integer, List<Interval>> getOccurrences() {
        return occurrences;
    }

    public void addOccurrence(int sid, Interval interval) {
        this.occurrences.computeIfAbsent(sid, k -> new ArrayList<>()).add(interval);
    }


    public List<Interval> getOccurrencesBySid(int sid) {
        return this.occurrences.getOrDefault(sid, new ArrayList<>());
    }

    public int getSupport() {
        int totalSupport = 0;
        for (List<Interval> list : occurrences.values()) {
            totalSupport += list.size(); 
        }
        return totalSupport;
    }

    @Override
    public String toString() {
        return "Pattern{'" + sequence + "', Support=" + getSupport() + ", Occurrences=" + occurrences + "}";
    }
}