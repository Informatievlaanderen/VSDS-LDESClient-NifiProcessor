package be.vlaanderen.vsds.ldesclient.nifi.processors.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jsonldjava.utils.JsonUtils;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.flowfile.attributes.CoreAttributes;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;

import java.io.*;
import java.util.Arrays;
import java.util.Map;

public class FlowManager {
    ProcessSession session;

    public FlowManager(ProcessSession session) {
        this.session = session;
    }

    public void sendTriplesToRelation(String[] tripples, Relationship relationship) {
        sendTriplesToRelation(tripples, relationship, Map.of());
    }

    public void sendTriplesToRelation(String[] tripples, Relationship relationship, Map<String, String> attributes) {
        FlowFile flowFile = session.create();
        flowFile = session.write(flowFile, (rawIn, rawOut) -> {
            try (PrintWriter out = new PrintWriter(rawOut)) {
                Arrays.stream(tripples).forEach(out::println);
            }
        });

        flowFile = session.putAttribute(flowFile, CoreAttributes.MIME_TYPE.key(), "application/n-quads");
        session.transfer(flowFile, relationship);
    }
}
