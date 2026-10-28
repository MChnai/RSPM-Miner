package test;

import core.db.PositionList;
import core.db.SparseVerticalDB;
import core.iterator.IPositionIterator;
import core.model.Interval;
import core.model.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PatternGrowthTest {

    @Test
    @DisplayName("Kiểm thử khởi tạo Sparse Vertical Database dựa trên Ví dụ Bảng 2.1")
    public void testSparseVerticalDBInitialization() {
        SparseVerticalDB db = new SparseVerticalDB();

        // Nạp dữ liệu giả lập giống Ví dụ Chương 2: S0 = A C D, B C E, A B C D (pos: 0, 1, 2)
        // Item A xuất hiện ở S0 tại pos 0 và pos 2
        db.addOccurrence(1, 0, 0); // Item 1 (A), SID 0, Pos 0
        db.addOccurrence(1, 0, 2); // Item 1 (A), SID 0, Pos 2
        
        // Item B (2) xuất hiện ở S0 tại pos 1 và pos 2
        db.addOccurrence(2, 0, 1);
        db.addOccurrence(2, 0, 2);

        // Kiểm tra nạp thành công
        PositionList posListA_S0 = db.getPositionList(1, 0);
        assertNotNull(posListA_S0, "PosList của Item A tại SID 0 không được null");
        assertEquals(2, posListA_S0.size(), "Item A tại SID 0 phải xuất hiện 2 lần");

        // Kiểm thử Iterator Pattern
        IPositionIterator iterator = posListA_S0.createIterator();
        assertTrue(iterator.hasNext());
        assertEquals(0, iterator.next());
        assertEquals(2, iterator.peek());
    }

    @Test
    @DisplayName("Kiểm thử tính đúng đắn của Pattern và kiểm tra Chồng Lấp Interval")
    public void testPatternAndIntervalOverlap() {
        Pattern pattern = new Pattern("A-B");
        Interval int1 = new Interval(0, 1);
        Interval int2 = new Interval(3, 4);
        Interval intOverlap = new Interval(1, 3); // Chồng lấp với int1 tại điểm 1

        pattern.addOccurrence(0, int1);
        pattern.addOccurrence(0, int2);

        // Tổng Support phải bằng 2
        assertEquals(2, pattern.getSupport());
        
        // Kiểm tra tính chất Overlap
        assertTrue(int1.isOverlapping(intOverlap), "int1 [0,1] và intOverlap [1,3] phải tính là đè lặp");
        assertFalse(int1.isOverlapping(int2), "int1 [0,1] và int2 [3,4] không được đè lặp nhau");
    }
}