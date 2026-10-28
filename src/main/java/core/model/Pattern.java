package core.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Pattern {
    private String sequence;
    private int support;
    private final Map<Integer, List<Interval>> occurrences;

    public Pattern(String sequence) {
        this.sequence = sequence;
        this.support = 0;
        this.occurrences = new HashMap<>();
    }

    public String getSequence() {
        return sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    public int getSupport() {
        // Occurrence-support counts occurrences, not only distinct sequence IDs.
        // Correctness of overlap handling still depends on the matching kernel.
        return occurrences.values().stream().mapToInt(List::size).sum();
    }

    public void setSupport(int support) {
        this.support = support;
    }

    public Map<Integer, List<Interval>> getOccurrences() {
        return occurrences;
    }

    public void addOccurrence(int sid, Interval interval) {
        this.occurrences.computeIfAbsent(sid, k -> new ArrayList<>()).add(interval);
    }

    public String getFormattedSequence() {
        if (sequence == null || sequence.isEmpty()) return "<>";
        
        String[] sParts = sequence.split("-");
        StringBuilder sb = new StringBuilder("<");

        for (int i = 0; i < sParts.length; i++) {
            String part = sParts[i];
            if (part.contains(",")) {
                sb.append("(").append(part.replace(",", ", ")).append(")");
            } else {
                sb.append(part);
            }
            
            if (i < sParts.length - 1) {
                sb.append(", ");
            }
        }
        sb.append(">");
        return sb.toString();
    }

    @Override
    public String toString() {
        return getFormattedSequence() + " | Non-Overlapping Support: " + getSupport();
    }
}