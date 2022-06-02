package be.vlaanderen.informatievlaanderen.ldes.client.services;

import be.vlaanderen.informatievlaanderen.ldes.client.valueobjects.LdesFragment;
import org.apache.jena.graph.TripleBoundary;
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

    private final ModelExtract modelExtract;

    public LdesServiceImpl(String initialPageUrl) {
        stateManager = new StateManager(initialPageUrl);
        modelExtract = new ModelExtract(new StatementTripleBoundary(TripleBoundary.stopNowhere));
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
        List<String[]> ldesMembers = processLdesMembers(ldesFragment.getModel(), ldesFragment.getFragmentId());

        // Queuing next pages
        processRelations(ldesFragment.getModel());

        stateManager.processFragment(fragmentToProcess, ldesFragment.getMaxAge());

        return ldesMembers;
    }

    @Override
    public boolean hasFragmentsToProcess() {
        return stateManager.hasFragmentsToProcess();
    }

    protected List<String[]> processLdesMembers(Model model, String fragmentId) {

        Resource subjectId = model.listStatements(ANY, W3ID_TREE_VIEW, model.createResource(fragmentId))
                .toList()
                .stream()
                .findFirst()
                .map(Statement::getSubject)
                .orElse(null);

        List<String[]> ldesMembers = new LinkedList<>();
        StmtIterator iter = model.listStatements(subjectId, W3ID_TREE_MEMBER, ANY);

        iter.forEach(statement -> {
            if (stateManager.processMember(statement.getObject().toString())) {
                ldesMembers.add(processMember(modelExtract.extract(statement.getObject().asResource(), model)));
            }
        });

        return ldesMembers;
    }

    protected String[] processMember(Model model) {
        StringWriter outputStream = new StringWriter();

        RDFDataMgr.write(outputStream, model, RDFFormat.NQUADS);

        return outputStream.toString().split("\n");
    }

    protected void processRelations(Model model) {
        model.listStatements(ANY, W3ID_TREE_RELATION, ANY)
                .forEach(relation -> stateManager.addNewFragmentToProcess(relation.getResource()
                        .getProperty(W3ID_TREE_NODE)
                        .getResource()
                        .toString()));
    }

}
