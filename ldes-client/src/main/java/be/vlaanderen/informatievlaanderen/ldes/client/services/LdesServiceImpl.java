package be.vlaanderen.informatievlaanderen.ldes.client.services;

import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFParser;

import java.io.IOException;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

import static be.vlaanderen.informatievlaanderen.ldes.client.valueobjects.LdesConstants.*;
import static java.util.Arrays.stream;

public class LdesServiceImpl implements LdesService {
    protected static final Resource ANY = null;

    protected final StateManager stateManager;

    public LdesServiceImpl(String initialPageUrl) {
        stateManager = new StateManager(initialPageUrl);
    }

    @Override
    public void populateFragmentQueue() {
        stateManager.populateFragmentQueue();
    }

    @Override
    public List<String[]> processNextFragment() {
        try {
            Model model = ModelFactory.createDefaultModel();

            String fragmentToProcess = stateManager.getNextFragmentToProcess();

            Long fragmentMaxAge = retrieveFragment(fragmentToProcess, model);

            // Sending members
            List<String[]> ldesMembers = processLdesMembers(model);

            // Queuing next pages
            processRelations(model);

            stateManager.processFragment(fragmentToProcess, fragmentMaxAge);

            return ldesMembers;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean hasFragmentsToProcess() {
        return stateManager.hasFragmentsToProcess();
    }

    protected List<String[]> processLdesMembers(Model model) {
        List<String[]> ldesMembers = new LinkedList<>();
        StmtIterator iter = model.listStatements(ANY, W3ID_TREE_MEMBER, ANY);

        iter.forEach(statement -> {
            if (stateManager.processMember(statement.getObject().toString())) {
                ldesMembers.add(processMember(statement));
            }
        });

        return ldesMembers;
    }

    protected String[] processMember(Statement statement) {
        Model outputModel = ModelFactory.createDefaultModel();
        outputModel.add(statement);
        populateRdfModel(outputModel, statement.getResource());

        StringWriter outputStream = new StringWriter();

        RDFDataMgr.write(outputStream, outputModel, RDFFormat.NQUADS);

        return outputStream.toString().split("\n");
    }

    protected void processRelations(Model model) {
        model.listStatements(ANY, W3ID_TREE_RELATION, ANY)
                .forEach(relation -> stateManager.addNewFragmentToProcess(relation.getResource()
                        .getProperty(W3ID_TREE_NODE)
                        .getResource()
                        .toString()));
    }

    private void populateRdfModel(Model model, Resource resource) {
        resource.listProperties().forEach(statement -> {
            model.add(statement);
            if (!statement.getObject().isLiteral()) {
                populateRdfModel(model, statement.getResource());
            }
        });
    }

    /**
     * Retrieves fragment from URL and stores it into the Model whilst passing returning the max age (in seconds) of the fragment
     *
     * @param url URL to retrieve fragment from
     * @param model Jena Model to populate based on the data from the provided url
     * @return the max age (in seconds) of the fragment
     */
    private Long retrieveFragment(String url, Model model) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        //Executing the Get request
        HttpResponse httpresponse = httpclient.execute(new HttpGet(url));

        RDFParser.source(httpresponse.getEntity().getContent())
                .forceLang(Lang.JSONLD11)
                .parse(model);

        return stream(httpresponse.getHeaders("Cache-Control"))
                .findFirst()
                .map(header -> {
                    if (stream(header.getElements())
                            .anyMatch(headerElement -> "immutable".equals(header.getName()))) {
                        return null;
                    } else {
                        return stream(header.getElements())
                                .filter(headerElement -> "max-age".equals(headerElement.getName()))
                                .findFirst()
                                .map(HeaderElement::getValue)
                                .map(Long::parseLong)
                                .orElse(null);
                    }
                })
                .orElse(null);
    }

}
