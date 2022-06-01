package be.vlaanderen.informatievlaanderen.ldes.client.services;

import be.vlaanderen.informatievlaanderen.ldes.client.valueobjects.LdesFragment;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

import static be.vlaanderen.informatievlaanderen.ldes.client.valueobjects.LdesConstants.*;

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
        String fragmentToProcess = stateManager.getNextFragmentToProcess();

        LdesFragment ldesFragment = LdesFragment.fromURL(fragmentToProcess);

        // Sending members
        List<String[]> ldesMembers = processLdesMembers(ldesFragment.getModel());

        // Queuing next pages
        processRelations(ldesFragment.getModel());

        stateManager.processFragment(fragmentToProcess, ldesFragment.getMaxAge());

        return ldesMembers;
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

}
