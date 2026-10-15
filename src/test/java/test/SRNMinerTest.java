package test;

import core.model.Pattern;
import engine.StreamingSRNMinerEngine;
import ingestion.IDataReader;
import ingestion.factory.DataReaderFactory;
import ingestion.factory.StreamBufferReaderFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import strategy.counting.TwoPointerNonOverlappingCounter;
import strategy.pruning.RPBPruningStrategy;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SRNMinerTest {

    @Test
    @DisplayName("Test StreamingSRNMinerEngine Snapshot Re-Mining Pipeline")
    public void testStreamingSRNMinerPipeline() throws IOException {
        File tempFile = File.createTempFile("test_srn_dataset", ".txt");
        tempFile.deleteOnExit();

        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("1 -1 2 -1 1 -1 2 -1 -2\n");
            writer.write("1 -1 2 -1 -2\n");
        }

        DataReaderFactory factory = new StreamBufferReaderFactory();
        IDataReader reader = factory.createReader(tempFile.getAbsolutePath());

        StreamingSRNMinerEngine engine = new StreamingSRNMinerEngine(
                reader,
                new TwoPointerNonOverlappingCounter(),
                new RPBPruningStrategy(),
                2.0,
                2
        );

        List<Pattern> results = engine.mine();

        assertNotNull(results);
        assertFalse(results.isEmpty());

        engine.processSnapshot(0);
        assertTrue(results.size() > 0);
    }
}