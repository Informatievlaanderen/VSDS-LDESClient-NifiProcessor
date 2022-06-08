package be.vlaanderen.informatievlaanderen.ldes.client.services;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@WireMockTest(httpPort = 8089)
public class LdesServiceImplTest {

    private final String initialFragmentUrl = "http://localhost:8089/exampleData?generatedAtTime=2022-05-03T00:00:00.000Z";
    private final String oneMemberFragmentUrl = "http://localhost:8089/exampleData?generatedAtTime=2022-05-05T00:00:00.000Z";
    private final String oneMemberUrl = "http://localhost:8089/member?generatedAtTime=2022-05-05T00:00:00.000Z";

    private LdesServiceImpl ldesService = new LdesServiceImpl(initialFragmentUrl);

    @Test
    void when_processRelations_expectFragmentQueueToBeUpdated() {
        assertEquals(1, ldesService.stateManager.fragmentsToProcessQueue.size());

        ldesService.processRelations(getInputModelFromUrl(initialFragmentUrl));

        assertEquals(2, ldesService.stateManager.fragmentsToProcessQueue.size());
    }

    @Test
    void when_ProcessNextFragmentWith2Fragments_expect2MembersPerFragment() {
        List<String[]> ldesMembers = ldesService.processNextFragment();

        assertEquals(2, ldesMembers.size());

        ldesMembers = ldesService.processNextFragment();

        assertEquals(2, ldesMembers.size());
    }

    @Test
    void when_ProcessNextFragment_expectValidLdesMember() {
        ldesService = new LdesServiceImpl(oneMemberFragmentUrl);
        List<String[]> ldesMembers = ldesService.processNextFragment();

        assertEquals(1, ldesMembers.size());

        String output = String.join("\n", ldesMembers.get(0));

        Model outputModel = RDFParserBuilder.create()
                .fromString(output)
                .lang(Lang.NQUADS)
                .toModel();
        Model validateModel = getInputModelFromUrl(oneMemberUrl);

        assertTrue(outputModel.isIsomorphicWith(validateModel));
    }

    private Model getInputModelFromUrl(String fragmentUrl) {
        Model inputModel = ModelFactory.createDefaultModel();

        RDFParser.source(fragmentUrl)
                .forceLang(Lang.JSONLD11)
                .parse(inputModel);

        return inputModel;
    }
}
