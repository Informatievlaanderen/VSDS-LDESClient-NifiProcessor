package be.vlaanderen.informatievlaanderen.ldes.processors;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static be.vlaanderen.informatievlaanderen.ldes.processors.config.LdesProcessorRelationships.DATA_RELATIONSHIP;
import static org.junit.jupiter.api.Assertions.assertEquals;

@WireMockTest(httpPort = 9090)
class LdesClientProcessorTest {

    private TestRunner testRunner;

    @BeforeEach
    public void init() {
        testRunner = TestRunners.newTestRunner(LdesClientProcessor.class);
    }

//    @Test
//    void when_runningLdesClientWithConnectedFragments_expectsAllLdesMembers() {
//        testRunner.setProperty("DATA_SOURCE_URL",
//                "http://localhost:8089/exampleData?generatedAtTime=2022-05-04T00:00:00.000Z");
//
//        testRunner.run(10);
//
//        List<MockFlowFile> dataFlowfiles = testRunner.getFlowFilesForRelationship(DATA_RELATIONSHIP);
//
//        assertEquals(6, dataFlowfiles.size());
//    }

//    @Test
//    void when_runningLdesClientWithFragmentContaining2DifferentLDES_expectsLdesMembersOnlyFromFragmentView() {
//        testRunner.setProperty("DATA_SOURCE_URL", "http://localhost:8089/exampleData?scenario=differentLdes");
//
//        testRunner.run(10);
//
//        List<MockFlowFile> dataFlowfiles = testRunner.getFlowFilesForRelationship(DATA_RELATIONSHIP);
//
//        assertEquals(1, dataFlowfiles.size());
//    }
}
