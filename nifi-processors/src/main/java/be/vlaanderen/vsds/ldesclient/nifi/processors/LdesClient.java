package be.vlaanderen.vsds.ldesclient.nifi.processors;

import be.vlaanderen.vsds.ldesclient.nifi.processors.util.LdesPage;
import be.vlaanderen.vsds.ldesclient.nifi.processors.util.StateManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jsonldjava.utils.JsonUtils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.flowfile.attributes.CoreAttributes;
import org.apache.nifi.processor.*;
import org.apache.nifi.processor.exception.ProcessException;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

import static be.vlaanderen.vsds.ldesclient.nifi.processors.config.LdesProcessorProperties.DATASOURCE_URL;
import static be.vlaanderen.vsds.ldesclient.nifi.processors.config.LdesProcessorProperties.TREE_DIRECTION;
import static be.vlaanderen.vsds.ldesclient.nifi.processors.config.LdesProcessorRelationships.*;

@Tags({"ldes-client, vsds"})
@CapabilityDescription("Takes in an LDES source and passes its data and metadata through")
@SeeAlso({ReplicateLdesStream.class})
public class LdesClient extends AbstractProcessor {
    private StateManager stateManager;

    private final Gson gson = new Gson();
    private final ObjectMapper objectMapper = new ObjectMapper();

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
    }

    @Override
    public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {
        JsonObject jsonResponse = retrieveLDESFromSource(stateManager.getNextPageToProcess());
        if(jsonResponse == null) {
            return;
        }

        LdesPage ldesPage = new LdesPage(jsonResponse, gson);

        processMetaData(ldesPage.getLdesMetadata(), session);
        processLdesItems(ldesPage.getLdesItems(), session);

        stateManager.lookupNextPageToProcess(ldesPage);
    }

    private void processLdesItems(JsonArray ldesItems, ProcessSession session) {
        Map<String, String> flowFileAttributes = new HashMap<>();

        ldesItems.forEach(jsonElement -> {
            String itemId = jsonElement.getAsJsonObject().get("@id").getAsString();
            if (!stateManager.processItem(itemId)) {
                return;
            }

            flowFileAttributes.put("filename", jsonElement.getAsJsonObject().get("@id").toString());
            // Send to queue
            sendFlowFile(gson.toJson(jsonElement.getAsJsonObject()), session, flowFileAttributes);

        });
    }

    private void sendFlowFile(String ldesItem, ProcessSession session, Map<String, String> flowFileAttributes) {
        FlowFile flowFile = writeJsonToFlowFile(ldesItem, session);

        session.putAllAttributes(flowFile, flowFileAttributes);
        session.transfer(flowFile, DATA_RELATIONSHIP);
    }

    private void processMetaData(String ldesMetadata, ProcessSession session) {
        FlowFile flowFile = writeJsonToFlowFile(ldesMetadata, session);

        session.transfer(flowFile, METADATA_RELATIONSHIP);
    }

    private FlowFile writeJsonToFlowFile(String ldesMetadata, ProcessSession session) {
        FlowFile flowFile = session.create();
        flowFile = session.write(flowFile, (rawIn, rawOut) -> {
            try (OutputStream out = new BufferedOutputStream(rawOut)) {
                out.write(objectMapper.writeValueAsBytes(JsonUtils.fromString(ldesMetadata)));
            }
        });

        return session.putAttribute(flowFile, CoreAttributes.MIME_TYPE.key(), "application/json");
    }

    private JsonObject retrieveLDESFromSource(String url) {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpGet httpGet = new HttpGet(url);
            getLogger().info("url {}", url);

            HttpResponse response = httpClient.execute(httpGet);
            HttpEntity httpEntity = response.getEntity();

            if (response.getStatusLine().getStatusCode() == 200) {
                return gson.fromJson(EntityUtils.toString(httpEntity), JsonObject.class);
            }
            else {
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }
}