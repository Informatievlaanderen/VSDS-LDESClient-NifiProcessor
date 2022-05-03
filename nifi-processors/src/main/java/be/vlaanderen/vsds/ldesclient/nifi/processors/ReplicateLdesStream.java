/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package be.vlaanderen.vsds.ldesclient.nifi.processors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jsonldjava.utils.JsonUtils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.flowfile.attributes.CoreAttributes;
import org.apache.nifi.processor.*;
import org.apache.nifi.processor.util.StandardValidators;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.StreamSupport;

@Tags({"ldes-client, vsds"})
@CapabilityDescription("Provide a description")
@SeeAlso({})
@ReadsAttributes({@ReadsAttribute(attribute = "", description = "")})
@WritesAttributes({@WritesAttribute(attribute = "", description = "")})
public class ReplicateLdesStream extends AbstractProcessor {

    public static final PropertyDescriptor API_URL = new PropertyDescriptor
            .Builder().name("API_URL")
            .displayName("Url to data source api")
            .description("Example Property")
            .required(true)
            .addValidator(StandardValidators.URL_VALIDATOR)
            .build();
    public static final PropertyDescriptor POLLING_INTERVAL = new PropertyDescriptor
            .Builder().name("POLLING_INTERVAL")
            .displayName("Polling interval")
            .description("Interval how often the API will be consulted. To be defined in milliseconds")
            .required(true)
            .addValidator(StandardValidators.POSITIVE_INTEGER_VALIDATOR)
            .build();

    public static final Relationship SUCCESS_RELATIONSHIP = new Relationship.Builder()
            .name("success")
            .description("Successfully retrieved items")
            .build();

    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;

    private String nextUrl;
    private String lastProcessedId;

    private ObjectMapper objectMapper;
    Gson gson;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        descriptors = new ArrayList<>();
        descriptors.add(API_URL);
        descriptors.add(POLLING_INTERVAL);
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
        nextUrl = context.getProperty(API_URL).getValue();
        lastProcessedId = "";

        objectMapper = new ObjectMapper();
        gson = new Gson();
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) {
        Map<String, String> metricAttributes = new HashMap<>();
        String jsonResponse = retrieveLDESFromSource(nextUrl);
        updateVariables(jsonResponse, metricAttributes);


        if ((!lastProcessedId.equals(metricAttributes.get("filename"))) && jsonResponse != null) {
            FlowFile flowFile = session.create();
            flowFile = session.write(flowFile, (rawIn, rawOut) -> {
                try (OutputStream out = new BufferedOutputStream(rawOut)) {
                    Object jsonObject = JsonUtils.fromString(jsonResponse);

                    out.write(objectMapper.writeValueAsBytes(jsonObject));
                }
            });

            flowFile = session.putAttribute(flowFile, CoreAttributes.MIME_TYPE.key(), "application/json");
            session.putAllAttributes(flowFile, metricAttributes);
            session.transfer(flowFile, SUCCESS_RELATIONSHIP);
            lastProcessedId = metricAttributes.get("filename");
        }
    }

    private void updateVariables(String jsonResponse, Map<String, String> metricAttributes) {
        JsonObject map = gson.fromJson(jsonResponse, JsonObject.class);
        metricAttributes.put("filename", map.get("@id").toString());

        JsonArray treeRelations = gson.fromJson(map.get("tree:relation").toString(), JsonArray.class);
        StreamSupport.stream(treeRelations.spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .filter(x -> Objects.equals(x.get("@type").getAsString(), "tree:GreaterThanRelation"))
                .findFirst()
                .ifPresent(jsonObject -> nextUrl = jsonObject.get("tree:node").getAsString());
    }

    private String retrieveLDESFromSource(String url) {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpGet httpGet = new HttpGet(url);
            getLogger().trace("url {}", url);

            HttpResponse response = httpClient.execute(httpGet);
            HttpEntity httpEntity = response.getEntity();

            return EntityUtils.toString(httpEntity);
        } catch (IOException e) {
            throw new RuntimeException();
        }

    }
}
