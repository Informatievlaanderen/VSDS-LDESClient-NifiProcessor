package be.vlaanderen.informatievlaanderen.ldes.processors.config;

import org.apache.nifi.processor.Relationship;

public class LdesProcessorRelationships {
    public static final Relationship DATA_RELATIONSHIP = new Relationship.Builder().name("data")
            .description("LDES item updates").build();
}
