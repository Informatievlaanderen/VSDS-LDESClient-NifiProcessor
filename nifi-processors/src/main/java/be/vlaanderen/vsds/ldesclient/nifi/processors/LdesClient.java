package be.vlaanderen.vsds.ldesclient.nifi.processors;

import be.vlaanderen.vsds.ldesclient.nifi.processors.services.FlowManager;
import be.vlaanderen.vsds.ldesclient.nifi.processors.services.StateManager;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFParser;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;

import java.io.StringWriter;
import java.util.List;
import java.util.Set;

import static be.vlaanderen.vsds.ldesclient.nifi.processors.config.LdesProcessorProperties.DATASOURCE_URL;
import static be.vlaanderen.vsds.ldesclient.nifi.processors.config.LdesProcessorProperties.TREE_DIRECTION;
import static be.vlaanderen.vsds.ldesclient.nifi.processors.config.LdesProcessorRelationships.DATA_RELATIONSHIP;

@Tags({"ldes-client, vsds"})
@CapabilityDescription("Takes in an LDES source and passes its individual LDES members")
public class LdesClient extends AbstractProcessor {
    private StateManager stateManager;
    private FlowManager flowManager;

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
        stateManager = new StateManager(context.getProperty(TREE_DIRECTION).getValue(), context.getProperty(DATASOURCE_URL).getValue());
    }

    @Override
    public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {
        flowManager = new FlowManager(session);
        if (stateManager.hasPagesToProcess()) {
            processPage(stateManager.getNextPageToProcess());
        }
    }

    private void populateModel(Model model, Resource resource) {
        resource.listProperties().forEach(statement -> {
            if (statement.getObject().isLiteral()) {
                model.add(statement);
            } else {
                populateModel(model, statement.getResource());
            }
        });
    }

    private void processPage(String pageUrl) {
        getLogger().info("Processing LDES Page %s".formatted(pageUrl));
        Model model = ModelFactory.createDefaultModel();

        RDFParser.source(pageUrl)
                .forceLang(Lang.JSONLD11)
                .parse(model);

        // Data
        StmtIterator iter = model.listStatements(null, model.createProperty("https://w3id.org/tree#", "member"), (String) null);

        iter.forEach(statement -> {
            if (stateManager.processMember(statement.getObject().toString())) {
                Model outputModel = ModelFactory.createDefaultModel();
                outputModel.add(statement);
                populateModel(outputModel, statement.getResource());

                StringWriter outputStream = new StringWriter();

                RDFDataMgr.write(outputStream, outputModel, Lang.NTRIPLES);

                flowManager.sendTriplesToRelation(outputStream.toString().split("\n"), DATA_RELATIONSHIP);
            }
        });

        // Next Pages
        model.listStatements(null, model.createProperty("https://w3id.org/tree#", "node"), (String) null)
                .forEach(statement -> stateManager.addNewPageToProcess(statement.getObject().toString()));

        if (!stateManager.hasPagesToProcess()) {
            stateManager.setFullyReplayed(true);
        }
    }
}