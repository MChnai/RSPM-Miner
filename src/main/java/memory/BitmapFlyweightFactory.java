package memory;

import java.util.BitSet;

public class BitmapFlyweightFactory {
    private static BitmapFlyweightFactory instance;
    private final BitSet[] pool;
    private final boolean[] used;
    private final int poolSize;
    private final int bitSetCapacity;

    private BitmapFlyweightFactory(int poolSize, int bitSetCapacity) {
        this.poolSize = poolSize;
        this.bitSetCapacity = bitSetCapacity;
        this.pool = new BitSet[poolSize];
        this.used = new boolean[poolSize];
        for (int i = 0; i < poolSize; i++) {
            this.pool[i] = new BitSet(bitSetCapacity);
            this.used[i] = false;
        }
    }

    public static synchronized BitmapFlyweightFactory getInstance(int poolSize, int bitSetCapacity) {
        if (instance == null) {
            instance = new BitmapFlyweightFactory(poolSize, bitSetCapacity);
        }
        return instance;
    }

    public static synchronized BitmapFlyweightFactory getInstance() {
        if (instance == null) {
            instance = new BitmapFlyweightFactory(1000, 10000);
        }
        return instance;
    }

    public synchronized BitSet getBitSet() {
        for (int i = 0; i < poolSize; i++) {
            if (!used[i]) {
                used[i] = true;
                pool[i].clear();
                return pool[i];
            }
        }
        return new BitSet(bitSetCapacity);
    }

    public synchronized void release(BitSet bitSet) {
        if (bitSet == null) {
            return;
        }
        for (int i = 0; i < poolSize; i++) {
            if (pool[i] == bitSet) {
                used[i] = false;
                pool[i].clear();
                return;
            }
        }
    }
}