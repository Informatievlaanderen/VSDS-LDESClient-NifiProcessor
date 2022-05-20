import be.vlaanderen.informatievlaanderen.ldes.client.services.LdesService;
import be.vlaanderen.informatievlaanderen.ldes.client.services.LdesServiceImpl;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.System.lineSeparator;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

@WireMockTest(httpPort = 8089)
public class LdesServiceImplTest {

    private final String initialPageUrl = "http://localhost:8089/exampleData?generatedAtTime=2022-05-03T00:00:00.000Z";

    private final LdesService ldesService = new LdesServiceImpl(initialPageUrl);

    @Test
    void when_processLdesMember_onlyExpectsOneRootNode() {
        List<String[]> members = ldesService.processNextPage();

        assertEquals(members.size(), 1, "Only processed one Ldes member");

        Model model = ModelFactory.createDefaultModel();

        InputStream inputStream = new ByteArrayInputStream(String.join(lineSeparator(), asList(members.get(0)))
                .getBytes(StandardCharsets.UTF_8));

        RDFDataMgr.read(model, inputStream, Lang.NQUADS);

        Set<RDFNode> objectURIResources = model.listObjects()
                .toList()
                .stream()
                .filter(RDFNode::isURIResource)
                .collect(Collectors.toSet());

        long rootNodeCount = model.listSubjects().toSet()
                .stream()
                .filter(RDFNode::isResource)
                .collect(Collectors.toSet())
                .stream()
                .filter(resource -> !objectURIResources.contains(resource))
                .filter(resource -> !resource.isAnon())
                .count();

        assertEquals(rootNodeCount, 1, "Ldes member only contains one root node");
    }
}
