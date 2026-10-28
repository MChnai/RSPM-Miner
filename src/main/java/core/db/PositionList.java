package core.db;

import core.iterator.IPositionIterator;
import core.iterator.PositionArrayIterator;
import java.util.ArrayList;
import java.util.List;

public class PositionList {
    private final List<Integer> positionList;

    public PositionList() {
        this.positionList = new ArrayList<>();
    }

    public void addPosition(int pos) {
        this.positionList.add(pos);
    }

    public int size() {
        return positionList.size();
    }

    public boolean isEmpty() {
        return positionList.isEmpty();
    }

    public List<Integer> getRawList() {
        return positionList;
    }

    public int[] toArray() {
        int size = positionList.size();
        int[] arr = new int[size];
        for (int i = 0; i < size; i++) {
            arr[i] = positionList.get(i);
        }
        return arr;
    }

    public IPositionIterator createIterator() {
        int size = positionList.size();
        int[] arr = new int[size];
        for (int i = 0; i < size; i++) {
            arr[i] = positionList.get(i);
        }
        return new PositionArrayIterator(arr);
    }

    @Override
    public String toString() {
        return positionList.toString();
    }
}