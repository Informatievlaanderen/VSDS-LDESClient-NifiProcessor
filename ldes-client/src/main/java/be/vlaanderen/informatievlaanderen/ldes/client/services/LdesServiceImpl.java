package be.vlaanderen.informatievlaanderen.ldes.client.services;

import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFParser;

import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

import static be.vlaanderen.informatievlaanderen.ldes.client.valueobjects.LdesConstants.*;

public class LdesServiceImpl implements LdesService {
    private static final Resource ANY = null;

    private final StateManager stateManager;

    public LdesServiceImpl(String initialPageUrl) {
        stateManager = new StateManager(initialPageUrl);
    }

    @Override
    public List<String[]> processNextPage() {
        Model model = ModelFactory.createDefaultModel();

        // Parsing the data
        RDFParser.source(stateManager.getNextPageToProcess())
                .forceLang(Lang.JSONLD11)
                .parse(model);

        // Sending members
        List<String[]> ldesMembers = processMembers(model);

        // Queuing next pages
        processRelations(model);

        if (!stateManager.hasPagesToProcess()) {
            stateManager.setFullyReplayed(true);
        }

        return ldesMembers;
    }

    @Override
    public boolean hasPagesToProcess() {
        return stateManager.hasPagesToProcess();
    }

    private List<String[]> processMembers(Model model) {
        List<String[]> ldesMembers = new LinkedList<>();
        StmtIterator iter = model.listStatements(ANY, W3ID_TREE_MEMBER, ANY);

        iter.forEach(statement -> {
            if (stateManager.processMember(statement.getObject().toString())) {
                Model outputModel = ModelFactory.createDefaultModel();
                outputModel.add(statement);
                populateModel(outputModel, statement.getResource());

                StringWriter outputStream = new StringWriter();

                RDFDataMgr.write(outputStream, outputModel, RDFFormat.NQUADS);

                ldesMembers.add(outputStream.toString().split("\n"));
            }
        });

        return ldesMembers;
    }

    private void processRelations(Model model) {
        List<Statement> relations = model.listStatements(ANY, W3ID_TREE_RELATION, ANY).toList();

        relations.forEach(relation -> stateManager.addNewPageToProcess(relation.getResource()
                .getProperty(W3ID_TREE_NODE)
                .getResource()
                .toString()));
    }

    private void populateModel(Model model, Resource resource) {
        resource.listProperties().forEach(statement -> {
            model.add(statement);
            if (!statement.getObject().isLiteral()) {
                populateModel(model, statement.getResource());
            }
        });
    }

}
