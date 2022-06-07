package be.vlaanderen.informatievlaanderen.ldes.processors.config;

import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.processor.util.StandardValidators;

public final class LdesProcessorProperties {

    private LdesProcessorProperties() {}

    public static final PropertyDescriptor DATASOURCE_URL =
            new PropertyDescriptor
                    .Builder()
                    .name("DATASOURCE_URL")
                    .displayName("Datasource url")
                    .description("Url to data source")
                    .required(true)
                    .addValidator(StandardValidators.URL_VALIDATOR)
                    .build();
}
