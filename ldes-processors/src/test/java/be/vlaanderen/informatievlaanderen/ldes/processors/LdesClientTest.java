package be.vlaanderen.informatievlaanderen.ldes.processors;/*
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

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static be.vlaanderen.informatievlaanderen.ldes.processors.config.LdesProcessorRelationships.DATA_RELATIONSHIP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@WireMockTest(httpPort = 8089)
public class LdesClientTest {

    private TestRunner testRunner;

    @BeforeEach
    public void init() {
        testRunner = TestRunners.newTestRunner(LdesClient.class);
    }

    @Test
    void when_runningLdesClientWithConnectedFragments_expectsAllLdesMembers() {
        testRunner.setProperty("DATASOURCE_URL", "http://localhost:8089/exampleData?generatedAtTime=2022-05-04T00:00:00.000Z");

        testRunner.run(10);

        List<MockFlowFile> dataFlowfiles = testRunner.getFlowFilesForRelationship(DATA_RELATIONSHIP);

        assertEquals(dataFlowfiles.size(), 6);
        assertTrue(dataFlowfiles.stream().allMatch(x -> new String(x.getData()).contains("https://w3id.org/tree#member")));
    }


    @Test
    void when_runningLdesClientWithFragmentContaining2DifferentLDES_expectsLdesMembersOnlyFromFragmentView() {
        testRunner.setProperty("DATASOURCE_URL", "http://localhost:8089/exampleData?scenario=differentLdes");

        testRunner.run(10);

        List<MockFlowFile> dataFlowfiles = testRunner.getFlowFilesForRelationship(DATA_RELATIONSHIP);

        assertEquals(dataFlowfiles.size(), 1);
        assertTrue(dataFlowfiles.stream().allMatch(x -> new String(x.getData()).contains("https://w3id.org/tree#member")));
    }


}
