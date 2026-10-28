package core.db;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SparseVerticalDB {
    private final Map<Integer, Map<Integer, PositionList>> verticalMatrix;

    public SparseVerticalDB() {
        this.verticalMatrix = new HashMap<>();
    }

    public void addOccurrence(int itemId, int sid, int pos) {
        this.verticalMatrix
            .computeIfAbsent(itemId, k -> new HashMap<>())
            .computeIfAbsent(sid, k -> new PositionList())
            .addPosition(pos);
    }

    public PositionList getPositionList(int itemId, int sid) {
        Map<Integer, PositionList> sidMap = verticalMatrix.get(itemId);
        if (sidMap != null) {
            return sidMap.get(sid);
        }
        return null;
    }

    public Map<Integer, PositionList> getItemVerticalData(int itemId) {
        return verticalMatrix.getOrDefault(itemId, Collections.emptyMap());
    }

    public Map<Integer, Map<Integer, PositionList>> getRawMatrix() {
        return verticalMatrix;
    }

    public void printDatabase() {
        System.out.println("=== SPARSE VERTICAL DATABASE ===");
        verticalMatrix.forEach((itemId, sidMap) -> {
            System.out.println("Item [" + itemId + "]:");
            sidMap.forEach((sid, posList) -> {
                System.out.println("   -> SID " + sid + ": " + posList);
            });
        });
    }
}