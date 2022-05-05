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

import be.vlaanderen.vsds.ldesclient.nifi.processors.rest.clients.LdesInputRestClient;
import be.vlaanderen.vsds.ldesclient.nifi.processors.services.FlowManager;
import be.vlaanderen.vsds.ldesclient.nifi.processors.models.LdesPage;
import be.vlaanderen.vsds.ldesclient.nifi.processors.services.StateManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static be.vlaanderen.vsds.ldesclient.nifi.processors.config.LdesProcessorProperties.DATASOURCE_URL;
import static be.vlaanderen.vsds.ldesclient.nifi.processors.config.LdesProcessorProperties.TREE_DIRECTION;
import static be.vlaanderen.vsds.ldesclient.nifi.processors.config.LdesProcessorRelationships.SUCCESS_RELATIONSHIP;

@Tags({"ldes-client, vsds"})
@CapabilityDescription("Takes in an LDES source and passes it through")
@SeeAlso({LdesClient.class})
public class ReplicateLdesStream extends AbstractProcessor {

    private String lastProcessedId;

    private StateManager stateManager;
    private LdesInputRestClient ldesInputRestClient;

    private final Gson gson = new Gson();

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
        stateManager = new StateManager(context.getProperty(TREE_DIRECTION).getValue(), context.getProperty(DATASOURCE_URL).getValue());
        ldesInputRestClient = new LdesInputRestClient(gson);
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) {
        if (Objects.equals(lastProcessedId, stateManager.getNextPageToProcess())) {
            return;
        }
        FlowManager flowManager = new FlowManager(session);

        JsonObject ldesInput = ldesInputRestClient.retrieveLdesInput(stateManager.getNextPageToProcess());
        if (ldesInput == null) {
            return;
        }

        LdesPage ldesPage = new LdesPage(ldesInput, gson);

        flowManager.sendJsonStringToRelationship(ldesPage.getPage(), SUCCESS_RELATIONSHIP);

        lastProcessedId = ldesPage.getPageId();
        stateManager.lookupNextPageToProcess(ldesPage);
    }
}
