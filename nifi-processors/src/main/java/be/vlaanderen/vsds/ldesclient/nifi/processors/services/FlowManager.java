package be.vlaanderen.vsds.ldesclient.nifi.processors.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jsonldjava.utils.JsonUtils;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.flowfile.attributes.CoreAttributes;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.util.Map;

public class FlowManager {
    ProcessSession session;
    private final ObjectMapper objectMapper;

    public FlowManager(ProcessSession session) {
        this.session = session;
        objectMapper = new ObjectMapper();
    }

    public FlowFile writeJsonToFlowFile(String ldesMetadata) {
        FlowFile flowFile = session.create();
        flowFile = session.write(flowFile, (rawIn, rawOut) -> {
            try (OutputStream out = new BufferedOutputStream(rawOut)) {
                out.write(objectMapper.writeValueAsBytes(JsonUtils.fromString(ldesMetadata)));
            }
        });

        return session.putAttribute(flowFile, CoreAttributes.MIME_TYPE.key(), "application/json");
    }

    public void sendJsonStringToRelationship(String jsonString, Relationship relationship) {
        sendJsonStringToRelationship(jsonString, relationship, Map.of());
    }

    public void sendJsonStringToRelationship(String jsonString, Relationship relationship, Map<String, String> attributes) {
        FlowFile flowFile = writeJsonToFlowFile(jsonString);
        flowFile = session.putAllAttributes(flowFile, attributes);

        session.transfer(flowFile, relationship);
    }
}
