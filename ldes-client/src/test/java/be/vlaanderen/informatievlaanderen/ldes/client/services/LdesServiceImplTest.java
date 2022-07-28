package be.vlaanderen.informatievlaanderen.ldes.client.services;

import org.apache.jena.riot.Lang;
import org.junit.jupiter.api.BeforeEach;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;

@WireMockTest(httpPort = 10101)
class LdesServiceImplTest {

	private final String initialFragmentUrl = "http://localhost:10101/exampleData?generatedAtTime=2022-05-03T00:00:00.000Z";
	private final String oneMemberFragmentUrl = "http://localhost:10101/exampleData?generatedAtTime=2022-05-05T00:00:00.000Z";
	private final String oneMemberUrl = "http://localhost:10101/member?generatedAtTime=2022-05-05T00:00:00.000Z";

	private LdesServiceImpl ldesService;

	@BeforeEach
	void setup() {
		ldesService = new LdesServiceImpl(Lang.JSONLD11);

		ldesService.queueFragment(initialFragmentUrl);
	}

//	@Test
//	void when_processRelations_expectFragmentQueueToBeUpdated() {
//		assertEquals(1, ldesService.stateManager.fragmentsToProcess.size());
//
//		ldesService.extractRelations(getInputModelFromUrl(initialFragmentUrl))
//				.forEach(relationStatement -> ldesService.stateManager.queueFragment(
//						relationStatement.getResource().getProperty(W3ID_TREE_NODE).getResource().toString()));
//
//		assertEquals(2, ldesService.stateManager.fragmentsToProcess.size());
//	}
//
//	@Test
//	void when_ProcessNextFragmentWith2Fragments_expect2MembersPerFragment() {
//		LdesFragment fragment;
//
//		fragment = ldesService.processNextFragment();
//		assertEquals(2, fragment.getMembers().size());
//
//		fragment = ldesService.processNextFragment();
//		assertEquals(2, fragment.getMembers().size());
//	}
//
//	@Test
//	void when_ProcessNextFragment_expectValidLdesMember() {
//		ldesService = new LdesServiceImpl(Lang.JSONLD11, Lang.NQUADS);
//		ldesService.queueFragment(oneMemberFragmentUrl);
//
//		LdesFragment fragment = ldesService.processNextFragment();
//
//		assertEquals(1, fragment.getMembers().size());
//
//		String output = fragment.getMembers().get(0).getMemberData();
//
//		Model outputModel = RDFParserBuilder.create().fromString(output).lang(Lang.NQUADS).toModel();
//		Model validateModel = getInputModelFromUrl(oneMemberUrl);
//
//		assertTrue(outputModel.isIsomorphicWith(validateModel));
//	}
//
//	private Model getInputModelFromUrl(String fragmentUrl) {
//		Model inputModel = ModelFactory.createDefaultModel();
//
//		RDFParser.source(fragmentUrl).forceLang(Lang.JSONLD11).parse(inputModel);
//
//		return inputModel;
//	}
}
