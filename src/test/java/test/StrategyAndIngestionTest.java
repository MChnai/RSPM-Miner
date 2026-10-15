package test;

import core.db.PositionList;
import core.db.SparseVerticalDB;
import core.model.Interval;
import core.model.Pattern;
import ingestion.IDataReader;
import ingestion.factory.DataReaderFactory;
import ingestion.factory.SPMFReaderFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import strategy.counting.ICountingStrategy;
import strategy.counting.TwoPointerNonOverlappingCounter;
import strategy.pruning.IPruningStrategy;
import strategy.pruning.RPBPruningStrategy;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class StrategyAndIngestionTest {

    @Test
    @DisplayName("Test SPMFDataReader via Factory Method Pattern")
    public void testSPMFDataReaderFactory() throws IOException {
        File tempFile = File.createTempFile("test_spmf", ".txt");
        tempFile.deleteOnExit();

        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("1 -1 2 -1 3 -1 -2\n");
            writer.write("1 -1 3 -1 -2\n");
        }

        DataReaderFactory factory = new SPMFReaderFactory();
        IDataReader reader = factory.createReader(tempFile.getAbsolutePath());
        SparseVerticalDB db = reader.readData();

        assertNotNull(db);
        assertNotNull(db.getPositionList(1, 0));
        assertEquals(1, db.getPositionList(1, 0).size());
        assertEquals(1, db.getPositionList(1, 1).size());
    }

    @Test
    @DisplayName("Test TwoPointerSJoin Greedy Matching")
    public void testTwoPointerSJoin() {
        PositionList posListA = new PositionList();
        posListA.addPosition(0);
        posListA.addPosition(2);

        PositionList posListB = new PositionList();
        posListB.addPosition(1);
        posListB.addPosition(2);

        ICountingStrategy counter = new TwoPointerNonOverlappingCounter();
        List<Interval> matches = counter.sJoin(posListA, posListB);

        assertEquals(1, matches.size());
        assertEquals(0, matches.get(0).getStart());
        assertEquals(1, matches.get(0).getEnd());
    }

    @Test
    @DisplayName("Test RPBPruningStrategy Evaluation")
    public void testRPBPruning() {
        SparseVerticalDB db = new SparseVerticalDB();
        db.addOccurrence(1, 0, 0);
        db.addOccurrence(2, 0, 1);

        Pattern pattern = new Pattern("1-2");
        pattern.addOccurrence(0, new Interval(0, 1));

        IPruningStrategy pruner = new RPBPruningStrategy();
        double rpb = pruner.evaluateRPB(pattern, db);

        assertEquals(1.0, rpb);
        assertTrue(pruner.shouldPrune(pattern, db, 2.0));
        assertFalse(pruner.shouldPrune(pattern, db, 1.0));
    }
}