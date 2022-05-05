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

/*


    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;

    private String nextUrl;
    private String lastProcessedId  = "";

    private boolean emitItemsOnce;

    private ObjectMapper objectMapper;

    private final BookKeeper bookKeeper = new BookKeeper();
    Queue<String> urlsToProcess = new ArrayDeque<>();

    private final Set<String> sentItems = new HashSet<>();

    private boolean initiated = false;
    Gson gson;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        descriptors = new ArrayList<>();
        descriptors.add(API_URL);
        descriptors.add(POLLING_INTERVAL);
        descriptors.add(EMIT_ONCE);
        descriptors = Collections.unmodifiableList(descriptors);

        relationships = new HashSet<>();
        relationships.add(SUCCESS_RELATIONSHIP);
        relationships = Collections.unmodifiableSet(relationships);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return descriptors;
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) {
        emitItemsOnce = context.getProperty(EMIT_ONCE).asBoolean();
        urlsToProcess.add(context.getProperty(API_URL).getValue());

        objectMapper = new ObjectMapper();
        gson = new Gson();
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) {
        while (!urlsToProcess.isEmpty()) {
            Map<String, String> metricAttributes = new HashMap<>();
            // 1. + 2. (retrieve info, save it in bookKeeper)
            String url = urlsToProcess.poll();
            JsonObject jsonResponse = retrieveLDESFromSource(url);

            if(jsonResponse == null) {
                return;
            }

            JsonArray items = jsonResponse.getAsJsonArray("items");

            items.forEach(jsonElement -> {
                String itemId = jsonElement.getAsJsonObject().get("@id").getAsString();
                if (emitItemsOnce && sentItems.contains(itemId)) {
                    return;
                }

                metricAttributes.put("filename", jsonElement.getAsJsonObject().get("@id").toString());
                // Send to queue
                sendFlowFile(gson.toJson(jsonElement.getAsJsonObject()), session, metricAttributes);

                if (emitItemsOnce) {
                    sentItems.add(itemId);
                }

            });

            if(!initiated) {
                // prepare next iteration
                JsonArray treeRelations = gson.fromJson(jsonResponse.get("tree:relation").toString(), JsonArray.class);
                StreamSupport.stream(treeRelations.spliterator(), false)
                        .map(JsonElement::getAsJsonObject)
                        .filter(x -> Objects.equals(x.get("@type").getAsString(), "tree:GreaterThanRelation"))
                        .findFirst()
                        .ifPresentOrElse(jsonObject -> urlsToProcess.add(jsonObject.get("tree:node").getAsString()),
                                () -> initiated = true);
            }

        }
    }

    private JsonObject retrieveLDESFromSource(String url) {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpGet httpGet = new HttpGet(url);
            getLogger().info("url {}", url);

            HttpResponse response = httpClient.execute(httpGet);
            HttpEntity httpEntity = response.getEntity();

            if (response.getStatusLine().getStatusCode() == 200) {
                JsonObject jsonResponse = gson.fromJson(EntityUtils.toString(httpEntity), JsonObject.class);

                addItemToBookkeeper(response.getFirstHeader("cache-control"), jsonResponse);

                return jsonResponse;
            }
            else {
                return null;
            }



        } catch (IOException e) {
            throw new RuntimeException();
        }

    }

    private void sendFlowFile(final String jsonResponse, final ProcessSession session, Map<String, String> metricAttributes) {
        FlowFile flowFile = session.create();
        flowFile = session.write(flowFile, (rawIn, rawOut) -> {
            try (OutputStream out = new BufferedOutputStream(rawOut)) {
                out.write(objectMapper.writeValueAsBytes(JsonUtils.fromString(jsonResponse)));
            }
        });

        flowFile = session.putAttribute(flowFile, CoreAttributes.MIME_TYPE.key(), "application/json");
        session.putAllAttributes(flowFile, metricAttributes);
        session.transfer(flowFile, SUCCESS_RELATIONSHIP);
        lastProcessedId = metricAttributes.get("filename");
    }

    private void addItemToBookkeeper(Header cacheHeader, JsonObject jsonResponse) {
        JsonElement id = jsonResponse.get("@id");

        if (!id.isJsonNull()) {
            final String regex = "max-age=(\\d+)";

            final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
            final Matcher matcher = pattern.matcher(cacheHeader.getValue());
            matcher.find();


            BookKeeperItem item = BookKeeperItem.newBuilder()
                    .withRefreshDate(LocalDateTime.now().plusSeconds(Integer.parseInt(matcher.group(1))))
                    .withUrl(id.getAsString())
                    .build();

            bookKeeper.addItem(item);
        }
    }
}

 */