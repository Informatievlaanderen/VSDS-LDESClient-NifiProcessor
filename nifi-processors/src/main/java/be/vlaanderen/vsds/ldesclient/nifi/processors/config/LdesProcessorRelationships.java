package be.vlaanderen.vsds.ldesclient.nifi.processors.config;

import org.apache.nifi.processor.Relationship;

public class LdesProcessorRelationships {
    public static final Relationship SUCCESS_RELATIONSHIP = new Relationship.Builder()
            .name("success")
            .description("Successfully retrieved items")
            .build();

    public static final Relationship DATA_RELATIONSHIP = new Relationship.Builder()
            .name("data")
            .description("Successfully retrieved items")
            .build();

    public static final Relationship METADATA_RELATIONSHIP = new Relationship.Builder()
            .name("metadata")
            .description("Successfully retrieved items")
            .build();
}
