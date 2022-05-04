package be.vlaanderen.vsds.ldesclient.nifi.processors.config;

import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.processor.util.StandardValidators;

public final class LdesProcessorProperties {
    public static final PropertyDescriptor DATASOURCE_URL = new PropertyDescriptor
            .Builder().name("DATASOURCE_URL")
            .displayName("Datasource url")
            .description("Url to data source")
            .required(true)
            .addValidator(StandardValidators.URL_VALIDATOR)
            .build();

    public static final PropertyDescriptor TREE_DIRECTION = new PropertyDescriptor
            .Builder().name("TREE_DIRECTION")
            .displayName("Ldes tree direction")
            .description("Defines in which direction the LDES Client will follow the stream")
            .required(true)
            .allowableValues("tree:GreaterThanRelation", "tree:LessThanRelation")
            .build();
}
