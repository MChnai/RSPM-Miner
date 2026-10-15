package test;

import core.model.Pattern;
import engine.AbstractMiningEngine;
import engine.StaticORNMinerEngine;
import ingestion.IDataReader;
import ingestion.factory.DataReaderFactory;
import ingestion.factory.SPMFReaderFactory;
import memory.BitmapFlyweightFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import strategy.counting.TwoPointerNonOverlappingCounter;
import strategy.pruning.RPBPruningStrategy;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.BitSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EngineAndMemoryTest {

    @Test
    @DisplayName("Test BitmapFlyweightFactory Reuse Mechanism")
    public void testFlyweightFactoryReuse() {
        BitmapFlyweightFactory factory = BitmapFlyweightFactory.getInstance(2, 100);

        BitSet bs1 = factory.getBitSet();
        BitSet bs2 = factory.getBitSet();

        assertNotNull(bs1);
        assertNotNull(bs2);
        assertNotSame(bs1, bs2);

        factory.release(bs1);
        BitSet bs3 = factory.getBitSet();

        assertSame(bs1, bs3);
    }

    @Test
    @DisplayName("Test StaticORNMinerEngine End-to-End Mining Pipeline")
    public void testStaticORNMinerEnginePipeline() throws IOException {
        File tempFile = File.createTempFile("test_orn_dataset", ".txt");
        tempFile.deleteOnExit();

        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("1 -1 2 -1 1 -1 2 -1 -2\n");
            writer.write("1 -1 2 -1 -2\n");
        }

        DataReaderFactory factory = new SPMFReaderFactory();
        IDataReader reader = factory.createReader(tempFile.getAbsolutePath());

        AbstractMiningEngine engine = new StaticORNMinerEngine(
                reader,
                new TwoPointerNonOverlappingCounter(),
                new RPBPruningStrategy(),
                2.0
        );

        List<Pattern> results = engine.mine();

        assertNotNull(results);
        assertFalse(results.isEmpty());

        boolean found1_2 = false;
        for (Pattern p : results) {
            if (p.getSequence().equals("1-2")) {
                found1_2 = true;
                assertEquals(3, p.getSupport());
            }
        }

        assertTrue(found1_2);
    }
}