package be.vlaanderen.informatievlaanderen.ldes.client.services;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFParser;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static be.vlaanderen.informatievlaanderen.ldes.client.services.LdesServiceImpl.ANY;
import static be.vlaanderen.informatievlaanderen.ldes.client.valueobjects.LdesConstants.W3ID_TREE_MEMBER;
import static java.lang.System.lineSeparator;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

@WireMockTest(httpPort = 8089)
public class LdesServiceImplTest {

    private final String initialFragmentUrl = "http://localhost:8089/exampleData?generatedAtTime=2022-05-03T00:00:00.000Z";

    private final LdesServiceImpl ldesService = new LdesServiceImpl(initialFragmentUrl);

    @Test
    void when_processMember_onlyExpectsOneRootNode() {
        Model outputModel = ModelFactory.createDefaultModel();
        RDFDataMgr.read(outputModel, getInputStreamOfFirstLdesMemberFromUrl(initialFragmentUrl), Lang.NQUADS);

        Set<RDFNode> objectURIResources = outputModel.listObjects()
                .toList()
                .stream()
                .filter(RDFNode::isURIResource)
                .collect(Collectors.toSet());

        long rootNodeCount = outputModel.listSubjects().toSet()
                .stream()
                .filter(RDFNode::isResource)
                .collect(Collectors.toSet())
                .stream()
                .filter(resource -> !objectURIResources.contains(resource))
                .filter(resource -> !resource.isAnon())
                .count();

        assertEquals(rootNodeCount, 1, "Ldes member only contains one root node");
    }

    @Test
    void when_processRelations_expectFragmentQueueToBeUpdated() {
        assertEquals(ldesService.stateManager.fragmentsToProcessQueue.size(), 1);

        ldesService.processRelations(getInputModelFromUrl(initialFragmentUrl));

        assertEquals(ldesService.stateManager.fragmentsToProcessQueue.size(), 2);
    }

    @Test
    void when_ProcessNextFragmentWith2Fragments_expect2MembersPerFragment() {
        List<String[]> ldesMembers = ldesService.processNextFragment();

        assertEquals(ldesMembers.size(), 2);

        ldesMembers = ldesService.processNextFragment();

        assertEquals(ldesMembers.size(), 2);
    }

    private Model getInputModelFromUrl(String fragmentUrl) {
        Model inputModel = ModelFactory.createDefaultModel();

        RDFParser.source(fragmentUrl)
                .forceLang(Lang.JSONLD11)
                .parse(inputModel);

        return inputModel;
    }

    private InputStream getInputStreamOfFirstLdesMemberFromUrl(String fragmentUrl) {
        Model inputModel = getInputModelFromUrl(fragmentUrl);
        StmtIterator iter = inputModel.listStatements(ANY, W3ID_TREE_MEMBER, ANY);

        return new ByteArrayInputStream(String.join(lineSeparator(), asList(ldesService.processMember(iter.nextStatement())))
                .getBytes(StandardCharsets.UTF_8));
    }
}
