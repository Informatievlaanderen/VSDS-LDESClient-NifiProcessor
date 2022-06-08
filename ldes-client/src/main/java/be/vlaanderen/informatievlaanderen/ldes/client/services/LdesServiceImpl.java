package be.vlaanderen.informatievlaanderen.ldes.client.services;

import be.vlaanderen.informatievlaanderen.ldes.client.valueobjects.LdesFragment;
import org.apache.jena.graph.TripleBoundary;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static be.vlaanderen.informatievlaanderen.ldes.client.valueobjects.LdesConstants.*;

public class LdesServiceImpl implements LdesService {
    protected static final Resource ANY_RESOURCE = null;
    protected static final Property ANY_PROPERTY = null;

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

    protected List<String[]> processLdesMembers(Model fragmentModel, String fragmentId) {

        Resource subjectId = fragmentModel.listStatements(ANY_RESOURCE, W3ID_TREE_VIEW, fragmentModel.createResource(fragmentId))
                .toList()
                .stream()
                .findFirst()
                .map(Statement::getSubject)
                .orElse(null);

        List<String[]> ldesMembers = new LinkedList<>();

        Set<Statement> statements = fragmentModel.listStatements(subjectId, W3ID_TREE_MEMBER, ANY_RESOURCE).toSet();
        StmtIterator memberIterator = fragmentModel.listStatements(subjectId, W3ID_TREE_MEMBER, ANY_RESOURCE);

        memberIterator.forEach(statement -> {
            String memberId = statement.getObject().toString();
            Set<Statement> otherMembers = statements.stream()
                    .filter(statement1 -> !statement.equals(statement1))
                    .collect(Collectors.toSet());
            if (stateManager.shouldProcessMember(memberId)) {
                ldesMembers.add(extractMember(statement, fragmentModel, otherMembers));
            }
        });

        return ldesMembers;
    }

    protected String[] extractMember(Statement memberStatement, Model model, Set<Statement> otherLdesMembers) {
        Model memberModel = modelExtract.extract(memberStatement.getObject().asResource(), model);

        memberModel.add(memberStatement);

        // Add reverse properties

        model.listStatements(ANY_RESOURCE, ANY_PROPERTY, memberStatement.getResource())
                .filterKeep(statement -> statement.getSubject().isURIResource())
                .filterDrop(memberStatement::equals)
                .forEach(statement -> {
                    Model reversePropertyModel = modelExtract.extract(statement.getSubject(), model);
                    List<Statement> otherMembers = reversePropertyModel.listStatements(statement.getSubject(), statement.getPredicate(), ANY_RESOURCE).toList();
                    otherLdesMembers.forEach(otherLdesMember -> {
                        reversePropertyModel.listStatements(ANY_RESOURCE, ANY_PROPERTY, otherLdesMember.getResource()).toList();
                    });
                    otherMembers.forEach(otherMember -> {
                        reversePropertyModel.remove(modelExtract.extract(otherMember.getResource(), reversePropertyModel));
                    });

                    memberModel.add(reversePropertyModel);
                });

        StringWriter outputStream = new StringWriter();

        RDFDataMgr.write(outputStream, memberModel, RDFFormat.NQUADS);

        return outputStream.toString().split("\n");
    }

    protected void processRelations(Model model) {
        model.listStatements(ANY_RESOURCE, W3ID_TREE_RELATION, ANY_RESOURCE)
                .forEach(relation -> stateManager.addNewFragmentToProcess(relation.getResource()
                        .getProperty(W3ID_TREE_NODE)
                        .getResource()
                        .toString()));
    }

}
