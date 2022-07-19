package be.vlaanderen.informatievlaanderen.ldes.processors.config;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.util.StandardValidators;

import be.vlaanderen.informatievlaanderen.ldes.processors.validators.RDFLanguageValidator;

public final class LdesProcessorProperties {

    private LdesProcessorProperties() {}

    public static final PropertyDescriptor DATA_SOURCE_URL =
            new PropertyDescriptor
                    .Builder()
                    .name("DATA_SOURCE_URL")
                    .displayName("Data source url")
                    .description("Url to data source")
                    .required(true)
                    .addValidator(StandardValidators.URL_VALIDATOR)
                    .build();

    public static final PropertyDescriptor DATA_SOURCE_FORMAT =
            new PropertyDescriptor
                    .Builder()
                    .name("DATA_SOURCE_FORMAT")
                    .displayName("Data source format")
                    .description("RDF format identifier of the data source")
                    .required(false)
                    .addValidator(new RDFLanguageValidator())
                    .defaultValue("JSONLD11")
                    .build();

    public static final PropertyDescriptor DATA_DESTINATION_FORMAT =
            new PropertyDescriptor
                    .Builder()
                    .name("DATA_DESTINATION_FORMAT")
                    .displayName("Data destination format")
                    .description("RDF format identifier of the data destination")
                    .required(false)
                    .addValidator(new RDFLanguageValidator())
                    .defaultValue("n-quads")
                    .build();
    
    public static String getDataSourceUrl(final ProcessContext context) {
    	return context.getProperty(DATA_SOURCE_URL).getValue();
    }
    
    public static Lang getDataSourceFormat(final ProcessContext context) {
    	return RDFLanguages.nameToLang(context.getProperty(DATA_SOURCE_FORMAT).getValue());
    }
    
    public static Lang getDataDestinationFormat(final ProcessContext context) {
    	return RDFLanguages.nameToLang(context.getProperty(DATA_DESTINATION_FORMAT).getValue());
    }
}
