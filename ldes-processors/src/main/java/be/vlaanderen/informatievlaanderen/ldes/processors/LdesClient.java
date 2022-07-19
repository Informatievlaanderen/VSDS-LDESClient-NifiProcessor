package be.vlaanderen.informatievlaanderen.ldes.processors;

import be.vlaanderen.informatievlaanderen.ldes.client.services.LdesService;
import be.vlaanderen.informatievlaanderen.ldes.client.services.LdesServiceImpl;
import be.vlaanderen.informatievlaanderen.ldes.client.services.LdesStateManager;
import be.vlaanderen.informatievlaanderen.ldes.client.valueobjects.LdesFragment;
import be.vlaanderen.informatievlaanderen.ldes.processors.config.LdesProcessorProperties;
import be.vlaanderen.informatievlaanderen.ldes.processors.services.FlowManager;

import org.apache.jena.riot.Lang;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.state.Scope;
import org.apache.nifi.components.state.StateManager;
import org.apache.nifi.components.state.StateMap;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static be.vlaanderen.informatievlaanderen.ldes.processors.config.LdesProcessorProperties.DATA_SOURCE_URL;
import static be.vlaanderen.informatievlaanderen.ldes.processors.config.LdesProcessorProperties.DATA_DESTINATION_FORMAT;
import static be.vlaanderen.informatievlaanderen.ldes.processors.config.LdesProcessorProperties.DATA_SOURCE_FORMAT;
import static be.vlaanderen.informatievlaanderen.ldes.processors.config.LdesProcessorRelationships.DATA_RELATIONSHIP;

@Tags({ "ldes-client, vsds" })
@CapabilityDescription("Takes in an LDES source and passes its individual LDES members")
public class LdesClient extends AbstractProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(LdesClient.class);

	private LdesService ldesService;
	private StateManager stateManager;

	private String dataSourceUrl;
	private Lang dataSourceFormat;
	private Lang dataDestinationFormat;

	@Override
	public Set<Relationship> getRelationships() {
		return Set.of(DATA_RELATIONSHIP);
	}

	@Override
	public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
		return List.of(DATA_SOURCE_URL, DATA_SOURCE_FORMAT, DATA_DESTINATION_FORMAT);
	}

	@OnScheduled
	public void onScheduled(final ProcessContext context) {
		dataSourceUrl = LdesProcessorProperties.getDataSourceUrl(context);
		dataSourceFormat = LdesProcessorProperties.getDataSourceFormat(context);
		dataDestinationFormat = LdesProcessorProperties.getDataDestinationFormat(context);
		
		LOGGER.info("Starting process {} with base url {}", context.getName(), dataSourceUrl);
		
		ldesService = new LdesServiceImpl(dataSourceFormat);
		
		stateManager = context.getStateManager();
		try {
			final StateMap stateMap = stateManager.getState(Scope.CLUSTER);
			
			// There will always be at least one mutable fragment in an LDES stream.
			// If the processor has run before, it is stored here.
			// If the LDES stream is started for the first time, the StateManager map
			// will be empty and we can schedule the data source URL.
			Map<String, String> currentState = stateMap.toMap();
			// REPLICATION
			if (currentState.isEmpty()) {
				ldesService.queueFragment(dataSourceUrl);
			}
			// SYNCHRONISATION
			else {
				for (String key : currentState.keySet()) {
					ldesService.queueFragment(key, LocalDateTime.parse(currentState.get(key)));
				}
			}
		} catch (IOException e) {
			LOGGER.error("An error occurred while retrieving the StateMap", e);
		}
	}

	@Override
	public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {
		while (ldesService.hasFragmentsToProcess()) {
			LdesFragment fragment = ldesService.processNextFragment();
			
			List<String[]> members = fragment.getMembers();
			members.forEach(ldesMember -> FlowManager.sendTriplesToRelation(session,
					dataDestinationFormat, ldesMember, DATA_RELATIONSHIP));
			
			if (!fragment.isImmutable()) {
				storeMutableFragment(fragment);
			}
		}
	}
	
	protected void storeMutableFragment(LdesFragment fragment) {
		try {
			final StateMap stateMap = stateManager.getState(Scope.CLUSTER);
			final Map<String, String> newMap = stateMap.toMap();
			
			newMap.put(fragment.getFragmentId(), fragment.getExpirationDate().toString());
			
			stateManager.replace(stateMap, newMap, Scope.CLUSTER);
		} catch (IOException e) {
			LOGGER.error("An error occured while storing mutable fragment {} in the StateManager", fragment.getFragmentId(), e);
		}
	}
}
