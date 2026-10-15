package test;

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

public class IntegrationStage1And2Test {

    @Test
    @DisplayName("Integration Test Stage 1 & 2: Reading SPMF Data -> SparseVerticalDB -> TwoPointer SJoin -> RPB Pruning")
    public void testIntegrationStage1And2() throws IOException {
        File tempFile = File.createTempFile("spmf_integration_dataset", ".txt");
        tempFile.deleteOnExit();

        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("1 -1 2 -1 1 -1 2 -1 -2\n");
            writer.write("1 -1 2 -1 -2\n");
        }

        DataReaderFactory factory = new SPMFReaderFactory();
        IDataReader reader = factory.createReader(tempFile.getAbsolutePath());
        SparseVerticalDB db = reader.readData();

        assertNotNull(db);
        assertNotNull(db.getPositionList(1, 0));
        assertNotNull(db.getPositionList(2, 0));

        ICountingStrategy counter = new TwoPointerNonOverlappingCounter();
        List<Interval> matchesS0 = counter.sJoin(db.getPositionList(1, 0), db.getPositionList(2, 0));
        
        assertEquals(2, matchesS0.size());
        assertEquals(0, matchesS0.get(0).getStart());
        assertEquals(1, matchesS0.get(0).getEnd());
        assertEquals(2, matchesS0.get(1).getStart());
        assertEquals(3, matchesS0.get(1).getEnd());

        Pattern pattern1_2 = new Pattern("1-2");
        pattern1_2.addOccurrence(0, matchesS0.get(0));
        pattern1_2.addOccurrence(0, matchesS0.get(1));

        List<Interval> matchesS1 = counter.sJoin(db.getPositionList(1, 1), db.getPositionList(2, 1));
        assertEquals(1, matchesS1.size());
        pattern1_2.addOccurrence(1, matchesS1.get(0));

        assertEquals(3, pattern1_2.getSupport());

        IPruningStrategy pruner = new RPBPruningStrategy();
        double rpbValue = pruner.evaluateRPB(pattern1_2, db);

        assertTrue(rpbValue >= 3.0);
        assertFalse(pruner.shouldPrune(pattern1_2, db, 2.0));
        assertTrue(pruner.shouldPrune(pattern1_2, db, 10.0));
    }
}