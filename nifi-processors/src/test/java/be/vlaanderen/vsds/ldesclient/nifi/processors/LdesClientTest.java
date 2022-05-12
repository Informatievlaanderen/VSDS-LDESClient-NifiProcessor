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

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static be.vlaanderen.vsds.ldesclient.nifi.processors.config.LdesProcessorRelationships.DATA_RELATIONSHIP;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class LdesClientTest {

    private TestRunner testRunner;

    @Before
    public void init() {
        testRunner = TestRunners.newTestRunner(LdesClient.class);
    }

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);

    @Test
    public void when_LdesClientRetrievesData_expectsRightAmountOfMembersInQueue() {
        stubFor(get("http://localhost:8089/exampleData?generatedAtTime=2022-05-04T00:00:00.000Z")
                .willReturn(aResponse().withStatus(200)));
        stubFor(get("http://localhost:8089/exampleData?generatedAtTime=2022-05-05T00:00:00.000Z")
                .willReturn(aResponse().withStatus(200)));

        testRunner.setProperty("DATASOURCE_URL", "http://localhost:8089/exampleData?generatedAtTime=2022-05-04T00:00:00.000Z");
        testRunner.setProperty("TREE_DIRECTION", "GreaterThanRelation");

        testRunner.run(4);

        List<MockFlowFile> dataFlowfiles = testRunner.getFlowFilesForRelationship(DATA_RELATIONSHIP);

        assertEquals(dataFlowfiles.size(), 4);
        assertTrue(dataFlowfiles.stream().allMatch(x-> new String(x.getData()).contains("https://w3id.org/tree#member")));
    }


}
