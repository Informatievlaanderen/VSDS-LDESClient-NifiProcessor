package be.vlaanderen.informatievlaanderen.ldes.processors.services;

import org.apache.jena.riot.Lang;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.flowfile.attributes.CoreAttributes;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.util.Arrays;

public class FlowManager {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(FlowManager.class);
	
	private static int counter = 0;
    
	public static void sendQuadsToRelation(ProcessSession session, Lang lang, String[] tripples, Relationship relationship) {
        FlowFile flowFile = session.create();
        flowFile = session.write(flowFile, (rawIn, rawOut) -> {
            try (PrintWriter out = new PrintWriter(rawOut)) {
                Arrays.stream(tripples).forEach(out::println);
            }
        });

        flowFile = session.putAttribute(flowFile, CoreAttributes.MIME_TYPE.key(), lang.getContentType().toHeaderString());
        session.transfer(flowFile, relationship);
        
        counter++;
        LOGGER.info("TRANSFER: sent quad #{} to processor {}", counter, relationship.getName());
    }
}
