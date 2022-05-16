package be.vlaanderen.informatievlaanderen.ldes.client.config;

import be.vlaanderen.informatievlaanderen.ldes.client.models.TreeDirection;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.processor.util.StandardValidators;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class LdesProcessorProperties {
    public static final PropertyDescriptor DATASOURCE_URL = new PropertyDescriptor
            .Builder().name("DATASOURCE_URL")
            .displayName("Datasource url")
            .description("Url to data source")
            .required(true)
            .addValidator(StandardValidators.URL_VALIDATOR)
            .build();

    public static final Set<String> SUPPORTED_TREE_DIRECTIONS = Stream.of(TreeDirection.values())
            .map(TreeDirection::name)
            .collect(Collectors.toSet());

    public static final PropertyDescriptor TREE_DIRECTION = new PropertyDescriptor
            .Builder().name("TREE_DIRECTION")
            .displayName("Ldes tree direction")
            .description("Defines in which direction the LDES Client will follow the stream")
            .required(false)
            .allowableValues(SUPPORTED_TREE_DIRECTIONS)
            .build();
}
