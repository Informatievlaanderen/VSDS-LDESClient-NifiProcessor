package be.vlaanderen.informatievlaanderen.ldes.processors;

import be.vlaanderen.informatievlaanderen.ldes.client.services.LdesService;
import be.vlaanderen.informatievlaanderen.ldes.client.services.LdesServiceImpl;
import be.vlaanderen.informatievlaanderen.ldes.client.valueobjects.LdesFragment;
import be.vlaanderen.informatievlaanderen.ldes.processors.config.LdesProcessorProperties;
import be.vlaanderen.informatievlaanderen.ldes.processors.services.FlowManager;

import org.apache.jena.riot.Lang;
import org.apache.nifi.annotation.behavior.Stateful;
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
@CapabilityDescription("Extracts members from an LDES source and sends to them the next processor")
@Stateful(description="Stores mutable fragments to allow processor restart", scopes=Scope.LOCAL)
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

	private StateMap getState() throws IOException {
		return stateManager.getState(Scope.LOCAL);
	}

	private Map<String, String> getStateMap() throws IOException {
		return getState().toMap();
	}

	@OnScheduled
	public void onScheduled(final ProcessContext context) {
		dataSourceUrl = LdesProcessorProperties.getDataSourceUrl(context);
		dataSourceFormat = LdesProcessorProperties.getDataSourceFormat(context);
		dataDestinationFormat = LdesProcessorProperties.getDataDestinationFormat(context);

		ldesService = new LdesServiceImpl(dataSourceFormat);

		stateManager = context.getStateManager();
		try {
			// There will always be at least one mutable fragment in an LDES stream.
			// If the processor has run before, it is stored here.
			// If the LDES stream is started for the first time, the StateManager map
			// will be empty and we can schedule the data source URL.
			Map<String, String> currentState = getStateMap();
			// FIRST SCHEDULE
			if (currentState.isEmpty()) {
				LOGGER.info("START: LDES extraction processor {} with base url {} (expected LDES source format: {})", context.getName(), dataSourceUrl, dataSourceFormat.toString());
				ldesService.queueFragment(dataSourceUrl);
			}
			// PROCESSOR RESTARTED
			else {
				Set<String> keys = currentState.keySet();
				LOGGER.info("RESTART: LDES extraction processor {} with base url {} (expected LDES source format: {}) -> queueing {} mutable fragment(s) from state", context.getName(), dataSourceUrl, dataSourceFormat.toString(), keys.size());
				for (String key : keys) {
					ldesService.queueFragment(key, LocalDateTime.parse(currentState.get(key)));
				}
			}
		} catch (IOException e) {
			LOGGER.error("An error occurred while retrieving the StateMap", e);
		}
	}

	@Override
	public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {
		if (ldesService.hasFragmentsToProcess()) {
			LdesFragment fragment = ldesService.processNextFragment();

			// Send the processed members to the next Nifi processor 
			fragment.getMembers().forEach(ldesMember -> FlowManager.sendQuadsToRelation(session,
					dataDestinationFormat, ldesMember.getStatements(), DATA_RELATIONSHIP));

			if (!fragment.isImmutable()) {
				storeMutableFragment(fragment);
			}
		}
	}

	protected void storeMutableFragment(LdesFragment fragment) {
		try {
			final Map<String, String> newMap = getStateMap();

			newMap.put(fragment.getFragmentId(), fragment.getExpirationDate().toString());

			stateManager.replace(getState(), newMap, Scope.CLUSTER);
		} catch (IOException e) {
			LOGGER.error("An error occured while storing mutable fragment {} in the StateManager", fragment.getFragmentId(), e);
		}
	}
}
