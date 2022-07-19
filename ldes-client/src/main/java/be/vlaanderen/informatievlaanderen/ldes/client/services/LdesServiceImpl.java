package be.vlaanderen.informatievlaanderen.ldes.client.services;

import be.vlaanderen.informatievlaanderen.ldes.client.valueobjects.LdesFragment;

import org.apache.jena.graph.TripleBoundary;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static be.vlaanderen.informatievlaanderen.ldes.client.valueobjects.LdesConstants.W3ID_TREE_MEMBER;
import static be.vlaanderen.informatievlaanderen.ldes.client.valueobjects.LdesConstants.W3ID_TREE_NODE;
import static be.vlaanderen.informatievlaanderen.ldes.client.valueobjects.LdesConstants.W3ID_TREE_RELATION;
import static be.vlaanderen.informatievlaanderen.ldes.client.valueobjects.LdesConstants.W3ID_TREE_VIEW;

import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class LdesServiceImpl implements LdesService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(LdesServiceImpl.class);
	
    protected static final Resource ANY_RESOURCE = null;
    protected static final Property ANY_PROPERTY = null;

    protected final LdesStateManager stateManager;
    private final ModelExtract modelExtract;
    private final LdesFragmentFetcher fragmentFetcher;

    /**
     * Replicates and synchronizes an LDES data set.
     * @param dataSourceUrl the base url of the data set 
     * @param lang the data format the data set is returned in (e.g. JSONLD11, N-QUADS)
     */
    public LdesServiceImpl(Lang lang) {
        stateManager = new LdesStateManager();
        modelExtract = new ModelExtract(new StatementTripleBoundary(TripleBoundary.stopNowhere));
        fragmentFetcher = new LdesFragmentFetcherImpl(lang);
    }
    
    @Override
    public void queueFragment(String fragmentId) {
    	queueFragment(fragmentId, LocalDateTime.now());
    }
    
    @Override
    public void queueFragment(String fragmentId, LocalDateTime expirationDate) {
    	stateManager.queueFragment(fragmentId, expirationDate);
    }
    
    @Override
    public boolean hasFragmentsToProcess() {
    	return stateManager.hasFragmentsToProcess();
    }

    @Override
    public LdesFragment processNextFragment() {
        LdesFragment fragment = fragmentFetcher.fetchFragment(stateManager.getNextFragment());
        
        fragment.setMembers(extractMembers(fragment.getModel(), fragment.getFragmentId()));
        // Queuing next pages
        extractRelations(fragment);

        stateManager.processFragment(fragment.getFragmentId(), fragment.isImmutable(), fragment.getExpirationDate());
        
        LOGGER.info("Fetched fragment {} ({}MUTABLE) has {} member(s)", fragment.getFragmentId(), fragment.isImmutable() ? "IM" : "", fragment.getMembers().size());
        
        if (!fragment.isImmutable()) {
        	
        }

        return fragment;
    }
    
    public Map<String, String> processStream() {
    	while (stateManager.hasFragmentsToProcess()) {
    		LdesFragment fragment = processNextFragment();
    	}
    }

    protected List<String[]> extractMembers(Model fragmentModel, String fragmentId) {
        Resource subjectId = fragmentModel.listStatements(ANY_RESOURCE, W3ID_TREE_VIEW, fragmentModel.createResource(fragmentId))
                .toList()
                .stream()
                .findFirst()
                .map(Statement::getSubject)
                .orElse(null);

        List<String[]> ldesMembers = new LinkedList<>();

        StmtIterator memberIterator = fragmentModel.listStatements(subjectId, W3ID_TREE_MEMBER, ANY_RESOURCE);

        memberIterator.forEach(statement -> {
            String memberId = statement.getObject().toString();
            if (stateManager.shouldProcessMember(memberId)) {
                ldesMembers.add(processMember(statement, fragmentModel));
            }
        });

        return ldesMembers;
    }

    protected String[] processMember(Statement memberStatement, Model fragmentModel) {
        Model memberModel = modelExtract.extract(memberStatement.getObject().asResource(), fragmentModel);

        memberModel.add(memberStatement);

        // Add reverse properties
        Set<Statement> otherLdesMembers = fragmentModel.listStatements(memberStatement.getSubject(), W3ID_TREE_MEMBER, ANY_RESOURCE)
                .toSet()
                .stream()
                .filter(statement -> !memberStatement.equals(statement))
                .collect(Collectors.toSet());

        fragmentModel.listStatements(ANY_RESOURCE, ANY_PROPERTY, memberStatement.getResource())
                .filterKeep(statement -> statement.getSubject().isURIResource())
                .filterDrop(memberStatement::equals)
                .forEach(statement -> {
                    Model reversePropertyModel = modelExtract.extract(statement.getSubject(), fragmentModel);
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

    protected List<String> extractRelations(LdesFragment fragment) {
    	List<String> relations = new ArrayList<>();
    	fragment.getModel().listStatements(ANY_RESOURCE, W3ID_TREE_RELATION, ANY_RESOURCE)
                .forEach(relation -> fragment.addRelation(relation.getResource()
                        .getProperty(W3ID_TREE_NODE)
                        .getResource()
                        .toString()));
    }
}
