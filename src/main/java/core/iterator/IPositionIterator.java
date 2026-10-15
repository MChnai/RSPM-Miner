package core.iterator;

public interface IPositionIterator {
    boolean hasNext();

    int next();
    int peek();

    void advanceTo(int targetPos);
}