package core.iterator;

import java.util.NoSuchElementException;

public class PositionArrayIterator implements IPositionIterator {
    private final int[] positions;
    private int cursor;           

    public PositionArrayIterator(int[] positions) {
        this.positions = positions != null ? positions : new int[0];
        this.cursor = 0;
    }

    @Override
    public boolean hasNext() {
        return cursor < positions.length;
    }

    @Override
    public int next() {
        if (!hasNext()) {
            throw new NoSuchElementException("Đã duyệt hết danh sách vị trí.");
        }
        return positions[cursor++];
    }

    @Override
    public int peek() {
        if (!hasNext()) {
            throw new NoSuchElementException("Danh sách vị trí rỗng hoặc đã duyệt xong.");
        }
        return positions[cursor];
    }

    @Override
    public void advanceTo(int targetPos) {
        // Tiến con trỏ lên cho đến khi gặp mốc thời gian lớn hơn targetPos
        while (hasNext() && positions[cursor] <= targetPos) {
            cursor++;
        }
    }
}