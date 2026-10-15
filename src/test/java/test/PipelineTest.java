package test;

import core.model.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pipeline.MiningPipeline;
import pipeline.MiningPipelineBuilder;
import strategy.counting.TwoPointerNonOverlappingCounter;
import strategy.pruning.RPBPruningStrategy;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PipelineTest {

    @Test
    @DisplayName("Test MiningPipelineBuilder Construction and Static Execution")
    public void testPipelineBuilderStaticExecution() throws IOException {
        File tempFile = File.createTempFile("pipeline_dataset", ".txt");
        tempFile.deleteOnExit();

        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("1 -1 2 -1 1 -1 2 -1 -2\n");
            writer.write("1 -1 2 -1 -2\n");
        }

        MiningPipeline pipeline = new MiningPipelineBuilder()
                .setDataSourcePath(tempFile.getAbsolutePath())
                .setMinSup(2.0)
                .setExecutionMode(MiningPipeline.ExecutionMode.STATIC_ORN)
                .setCountingStrategy(new TwoPointerNonOverlappingCounter())
                .setPruningStrategy(new RPBPruningStrategy())
                .build();

        assertNotNull(pipeline);
        assertEquals(MiningPipeline.ExecutionMode.STATIC_ORN, pipeline.getMode());

        List<Pattern> results = pipeline.execute();
        assertNotNull(results);
        assertFalse(results.isEmpty());
    }

    @Test
    @DisplayName("Test MiningPipelineBuilder Exception Handling for Invalid Parameters")
    public void testPipelineBuilderValidation() {
        assertThrows(IllegalArgumentException.class, () -> {
            new MiningPipelineBuilder()
                    .setDataSourcePath("")
                    .build();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new MiningPipelineBuilder()
                    .setDataSourcePath("data/test.txt")
                    .setMinSup(-1.0)
                    .build();
        });
    }
}