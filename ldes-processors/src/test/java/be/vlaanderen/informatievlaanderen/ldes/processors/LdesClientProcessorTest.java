package be.vlaanderen.informatievlaanderen.ldes.processors;

import org.apache.nifi.util.TestRunner;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;

@WireMockTest(httpPort = 10101)
class LdesClientProcessorTest {

    private TestRunner testRunner;

//    @BeforeEach
//    public void init() {
//        testRunner = TestRunners.newTestRunner(LdesClientProcessor.class);
//    }
//
//    @Test
//    void when_runningLdesClientWithConnectedFragments_expectsAllLdesMembers() {
//        testRunner.setProperty("DATA_SOURCE_URL",
//                "http://localhost:10101/exampleData?generatedAtTime=2022-05-04T00:00:00.000Z");
//
//        testRunner.run(10);
//
//        List<MockFlowFile> dataFlowfiles = testRunner.getFlowFilesForRelationship(DATA_RELATIONSHIP);
//
//        assertEquals(6, dataFlowfiles.size());
//    }
//
//    @Test
//    void when_runningLdesClientWithFragmentContaining2DifferentLDES_expectsLdesMembersOnlyFromFragmentView() {
//        testRunner.setProperty("DATA_SOURCE_URL", "http://localhost:10101/exampleData?scenario=differentLdes");
//
//        testRunner.run(10);
//
//        List<MockFlowFile> dataFlowfiles = testRunner.getFlowFilesForRelationship(DATA_RELATIONSHIP);
//
//        assertEquals(1, dataFlowfiles.size());
//    }
}
