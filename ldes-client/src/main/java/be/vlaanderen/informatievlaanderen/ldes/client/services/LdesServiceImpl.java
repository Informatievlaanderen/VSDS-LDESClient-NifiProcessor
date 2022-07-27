package be.vlaanderen.informatievlaanderen.ldes.client.services;

import be.vlaanderen.informatievlaanderen.ldes.client.valueobjects.LdesFragment;
import be.vlaanderen.informatievlaanderen.ldes.client.valueobjects.LdesMember;

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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LdesServiceImpl implements LdesService {

	private static final Logger LOGGER = LoggerFactory.getLogger(LdesServiceImpl.class);

	protected static final Resource ANY_RESOURCE = null;
	protected static final Property ANY_PROPERTY = null;

	protected final LdesStateManager stateManager;
	private final ModelExtract modelExtract;
	private final LdesFragmentFetcher fragmentFetcher;

	/**
	 * Replicates and synchronizes an LDES data set.
	 * 
	 * @param dataSourceUrl the base url of the data set
	 * @param lang          the data format the data set is returned in (e.g.
	 *                      JSONLD11, N-QUADS)
	 */
	public LdesServiceImpl(Lang lang) {
		stateManager = new LdesStateManager();
		modelExtract = new ModelExtract(new StatementTripleBoundary(TripleBoundary.stopNowhere));
		fragmentFetcher = new LdesFragmentFetcherImpl(lang);
	}

	@Override
	public void queueFragment(String fragmentId) {
		queueFragment(fragmentId, null);
	}

	@Override
	public void queueFragment(String fragmentId, LocalDateTime expirationDate) {
		stateManager.queueFragment(fragmentId, expirationDate);
	}

	@Override
	public boolean hasFragmentsToProcess() {
		return stateManager.hasNext();
	}

	@Override
	public LdesFragment processNextFragment() {
		LdesFragment fragment = fragmentFetcher.fetchFragment(stateManager.next());

		// Extract and process the members and add them to the fragment
		extractMembers(fragment.getModel(), fragment.getFragmentId())
				.filter(memberStatement -> stateManager.shouldProcessMember(fragment,
						memberStatement.getObject().toString()))
				.forEach(memberStatement -> fragment.addMember(processMember(fragment, memberStatement)));

		// Extract relations and add them to the fragment
		extractRelations(fragment.getModel()).forEach(relationStatement -> fragment
				.addRelation(relationStatement.getResource().getProperty(W3ID_TREE_NODE).getResource().toString()));
		// Queue the related fragments
		fragment.getRelations().forEach(fragmentId -> stateManager.queueFragment(fragmentId));

		// Inform the StateManager that a fragment has been processed
		stateManager.processedFragment(fragment);

		LOGGER.info("PROCESSED fragment {} ({}MUTABLE) has {} member(s) and {} tree:relation(s)", fragment.getFragmentId(),
				fragment.isImmutable() ? "IM" : "", fragment.getMembers().size(), fragment.getRelations().size());

		return fragment;
	}

	@Override
	public Map<String, LocalDateTime> processStream() {
		Map<String, LocalDateTime> fragments = new HashMap<>();
		while (stateManager.hasNext()) {
			LdesFragment fragment = processNextFragment();

			fragments.put(fragment.getFragmentId(), fragment.getExpirationDate());
		}

		return fragments;
	}

	protected Stream<Statement> extractMembers(Model fragmentModel, String fragmentId) {
		Resource subjectId = fragmentModel
				.listStatements(ANY_RESOURCE, W3ID_TREE_VIEW, fragmentModel.createResource(fragmentId)).toList()
				.stream().findFirst().map(Statement::getSubject).orElse(null);
		StmtIterator memberIterator = fragmentModel.listStatements(subjectId, W3ID_TREE_MEMBER, ANY_RESOURCE);
		
		return Stream.iterate(memberIterator, Iterator<Statement>::hasNext, UnaryOperator.identity())
				.map(Iterator::next);
	}

	protected LdesMember processMember(LdesFragment fragment, Statement memberStatement) {
		Model fragmentModel = fragment.getModel();
		Model memberModel = modelExtract.extract(memberStatement.getObject().asResource(), fragmentModel);
		String memberId = memberStatement.getObject().toString();
		
		memberModel.add(memberStatement);
		
		// @TODO: this should not be here -> check if jena is using the titanium parser for json-ld
		// Add reverse properties
		Set<Statement> otherLdesMembers = fragmentModel
				.listStatements(memberStatement.getSubject(), W3ID_TREE_MEMBER, ANY_RESOURCE).toSet().stream()
				.filter(statement -> !memberStatement.equals(statement)).collect(Collectors.toSet());

		fragmentModel.listStatements(ANY_RESOURCE, ANY_PROPERTY, memberStatement.getResource())
				.filterKeep(statement -> statement.getSubject().isURIResource()).filterDrop(memberStatement::equals)
				.forEach(statement -> {
					Model reversePropertyModel = modelExtract.extract(statement.getSubject(), fragmentModel);
					List<Statement> otherMembers = reversePropertyModel
							.listStatements(statement.getSubject(), statement.getPredicate(), ANY_RESOURCE).toList();
					otherLdesMembers.forEach(otherLdesMember -> {
						reversePropertyModel.listStatements(ANY_RESOURCE, ANY_PROPERTY, otherLdesMember.getResource())
								.toList();
					});
					otherMembers.forEach(otherMember -> {
						reversePropertyModel
								.remove(modelExtract.extract(otherMember.getResource(), reversePropertyModel));
					});

					memberModel.add(reversePropertyModel);
				});

		RDFDataMgr.write(new StringWriter(), memberModel, RDFFormat.NQUADS);
		
		LOGGER.info("PROCESSED LDES member ({}) on fragment {}", memberId, fragment.getFragmentId());

		return new LdesMember(memberId, memberModel.toString().split("\n"));
	}

	protected Stream<Statement> extractRelations(Model fragmentModel) {
		return Stream.iterate(fragmentModel.listStatements(ANY_RESOURCE, W3ID_TREE_RELATION, ANY_RESOURCE),
				Iterator<Statement>::hasNext, UnaryOperator.identity()).map(Iterator::next);
	}
}
