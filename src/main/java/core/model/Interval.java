package core.model;

import java.util.Objects;


public final class Interval {
    private final int start;
    private final int end;   

    public Interval(int start, int end) {
        if (start > end) {
            throw new IllegalArgumentException("Vị trí start (" + start + ") không được lớn hơn end (" + end + ")");
        }
        this.start = start;
        this.end = end;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public boolean isOverlapping(Interval other) {
        return this.start <= other.end && this.end >= other.start;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Interval interval = (Interval) o;
        return start == interval.start && end == interval.end;
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }

    @Override
    public String toString() {
        return "[" + start + ", " + end + "]";
    }
}