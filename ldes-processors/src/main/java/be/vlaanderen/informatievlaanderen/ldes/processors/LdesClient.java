package be.vlaanderen.informatievlaanderen.ldes.processors;

import be.vlaanderen.informatievlaanderen.ldes.client.services.LdesService;
import be.vlaanderen.informatievlaanderen.ldes.client.services.LdesServiceImpl;
import be.vlaanderen.informatievlaanderen.ldes.processors.services.FlowManager;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;

import java.util.List;
import java.util.Set;

import static be.vlaanderen.informatievlaanderen.ldes.processors.config.LdesProcessorProperties.DATASOURCE_URL;
import static be.vlaanderen.informatievlaanderen.ldes.processors.config.LdesProcessorRelationships.DATA_RELATIONSHIP;

@Tags({ "ldes-client, vsds" })
@CapabilityDescription("Takes in an LDES source and passes its individual LDES members")
public class LdesClient extends AbstractProcessor {

    private LdesService ldesService;

    @Override
    public Set<Relationship> getRelationships() {
        return Set.of(DATA_RELATIONSHIP);
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return List.of(DATASOURCE_URL);
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) {
        ldesService = new LdesServiceImpl(context.getProperty(DATASOURCE_URL).getValue());
    }

    @Override
    public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {
        FlowManager flowManager = new FlowManager(session);

        if (ldesService.hasFragmentsToProcess()) {
            List<String[]> ldesMembers = ldesService.processNextFragment();
            ldesMembers.forEach(ldesMember -> flowManager.sendTriplesToRelation(ldesMember, DATA_RELATIONSHIP));
        } else {
            ldesService.populateFragmentQueue();
        }
    }

}