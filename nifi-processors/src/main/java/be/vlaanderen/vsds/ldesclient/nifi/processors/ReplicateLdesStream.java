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
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.flowfile.attributes.CoreAttributes;
import org.apache.nifi.processor.*;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.StreamSupport;

import static be.vlaanderen.vsds.ldesclient.nifi.processors.config.LdesProcessorProperties.DATASOURCE_URL;
import static be.vlaanderen.vsds.ldesclient.nifi.processors.config.LdesProcessorProperties.TREE_DIRECTION;
import static be.vlaanderen.vsds.ldesclient.nifi.processors.config.LdesProcessorRelationships.SUCCESS_RELATIONSHIP;

@Tags({"ldes-client, vsds"})
@CapabilityDescription("Takes in an LDES source and passes it through")
@SeeAlso({LdesClient.class})
public class ReplicateLdesStream extends AbstractProcessor {

    private String nextUrl;
    private String lastProcessedId;
    private String treeDirection;

    private ObjectMapper objectMapper;
    Gson gson;

    @Override
    public Set<Relationship> getRelationships() {
        return Set.of(SUCCESS_RELATIONSHIP);
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return List.of(DATASOURCE_URL, TREE_DIRECTION);
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) {
        nextUrl = context.getProperty(DATASOURCE_URL).getValue();
        treeDirection = "tree:" + context.getProperty(TREE_DIRECTION).getValue();
        lastProcessedId = "";

        objectMapper = new ObjectMapper();
        gson = new Gson();
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) {
        Map<String, String> metricAttributes = new HashMap<>();
        String jsonResponse = retrieveLDESFromSource(nextUrl);
        updateVariables(jsonResponse, metricAttributes, treeDirection);

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

    private void updateVariables(String jsonResponse, Map<String, String> metricAttributes, String treeDirection) {
        JsonObject map = gson.fromJson(jsonResponse, JsonObject.class);
        metricAttributes.put("filename", map.get("@id").toString());

        JsonArray treeRelations = gson.fromJson(map.get("tree:relation").toString(), JsonArray.class);
        StreamSupport.stream(treeRelations.spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .filter(x -> Objects.equals(x.get("@type").getAsString(), treeDirection))
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
