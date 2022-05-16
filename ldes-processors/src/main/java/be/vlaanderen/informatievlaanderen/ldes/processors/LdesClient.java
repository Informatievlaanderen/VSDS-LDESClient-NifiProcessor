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

import static be.vlaanderen.informatievlaanderen.ldes.client.config.LdesProcessorProperties.DATASOURCE_URL;
import static be.vlaanderen.informatievlaanderen.ldes.client.config.LdesProcessorProperties.TREE_DIRECTION;
import static be.vlaanderen.informatievlaanderen.ldes.client.config.LdesProcessorRelationships.DATA_RELATIONSHIP;

@Tags({"ldes-client, vsds"})
@CapabilityDescription("Takes in an LDES source and passes its individual LDES members")
public class LdesClient extends AbstractProcessor {

    private LdesService ldesService;

    @Override
    public Set<Relationship> getRelationships() {
        return Set.of(DATA_RELATIONSHIP);
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return List.of(DATASOURCE_URL, TREE_DIRECTION);
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) {
        if (context.getProperty(TREE_DIRECTION).isSet()) {
            ldesService = new LdesServiceImpl(context.getProperty(TREE_DIRECTION).getValue(), context.getProperty(DATASOURCE_URL).getValue());
        } else {
            ldesService = new LdesServiceImpl(context.getProperty(DATASOURCE_URL).getValue());
        }

    }

    @Override
    public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {
        FlowManager flowManager = new FlowManager(session);
        if (ldesService.hasPagesToProcess()) {
            List<String[]> ldesMembers = ldesService.processNextPage();
            ldesMembers.forEach(ldesMember -> flowManager.sendTriplesToRelation(ldesMember, DATA_RELATIONSHIP));
        }
    }


}