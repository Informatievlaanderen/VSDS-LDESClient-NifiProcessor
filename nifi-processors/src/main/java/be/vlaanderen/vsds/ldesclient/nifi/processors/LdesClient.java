package be.vlaanderen.vsds.ldesclient.nifi.processors;

import be.vlaanderen.vsds.ldesclient.nifi.processors.rest.clients.LdesInputRestClient;
import be.vlaanderen.vsds.ldesclient.nifi.processors.services.FlowManager;
import be.vlaanderen.vsds.ldesclient.nifi.processors.models.LdesPage;
import be.vlaanderen.vsds.ldesclient.nifi.processors.services.StateManager;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.processor.*;
import org.apache.nifi.processor.exception.ProcessException;

import java.util.*;

import static be.vlaanderen.vsds.ldesclient.nifi.processors.config.LdesProcessorProperties.DATASOURCE_URL;
import static be.vlaanderen.vsds.ldesclient.nifi.processors.config.LdesProcessorProperties.TREE_DIRECTION;
import static be.vlaanderen.vsds.ldesclient.nifi.processors.config.LdesProcessorRelationships.*;

@Tags({"ldes-client, vsds"})
@CapabilityDescription("Takes in an LDES source and passes its data and metadata through")
@SeeAlso({ReplicateLdesStream.class})
public class LdesClient extends AbstractProcessor {
    private StateManager stateManager;
    private FlowManager flowManager;
    private LdesInputRestClient ldesInputRestClient;

    private final Gson gson = new Gson();

    @Override
    public Set<Relationship> getRelationships() {
        return Set.of(DATA_RELATIONSHIP, METADATA_RELATIONSHIP);
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return List.of(DATASOURCE_URL, TREE_DIRECTION);
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) {
        stateManager = new StateManager(context.getProperty(TREE_DIRECTION).getValue(), context.getProperty(DATASOURCE_URL).getValue());
        ldesInputRestClient = new LdesInputRestClient(gson);
    }

    @Override
    public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {
        flowManager = new FlowManager(session);

        JsonObject ldesInput = ldesInputRestClient.retrieveLdesInput(stateManager.getNextPageToProcess());
        if(ldesInput == null) {
            return;
        }

        LdesPage ldesPage = new LdesPage(ldesInput, gson);

        flowManager.sendJsonStringToRelationship(ldesPage.getLdesMetadata(), METADATA_RELATIONSHIP);

        processLdesItems(ldesPage.getLdesItems());

        stateManager.lookupNextPageToProcess(ldesPage);
    }

    private void processLdesItems(JsonArray ldesItems) {
        ldesItems.forEach(jsonElement -> {
            String itemId = jsonElement.getAsJsonObject().get("@id").getAsString();
            if (!stateManager.processItem(itemId)) {
                return;
            }

            Map<String, String> flowFileAttributes = Map.of("filename", itemId);
            // Send to queue
            flowManager.sendJsonStringToRelationship(gson.toJson(jsonElement.getAsJsonObject()), DATA_RELATIONSHIP, flowFileAttributes);
        });
    }
}