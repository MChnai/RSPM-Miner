package stream;

import java.util.LinkedList;
import java.util.Queue;

public class SlidingWindow {
    private final int windowSize;
    private final Queue<Integer> activeSids;

    public SlidingWindow(int windowSize) {
        this.windowSize = windowSize;
        this.activeSids = new LinkedList<>();
    }

    public void addSequence(int sid) {
        activeSids.add(sid);
    }

    public Integer evictExpired() {
        if (activeSids.size() > windowSize) {
            return activeSids.poll();
        }
        return null;
    }

    public Queue<Integer> getActiveSids() {
        return new LinkedList<>(activeSids);
    }

    public int getCurrentSize() {
        return activeSids.size();
    }
}