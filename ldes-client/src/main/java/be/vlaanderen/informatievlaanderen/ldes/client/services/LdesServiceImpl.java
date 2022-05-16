package be.vlaanderen.informatievlaanderen.ldes.client.services;

import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFParser;

import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class LdesServiceImpl implements LdesService {

    private final StateManager stateManager;

    public LdesServiceImpl(String treeDirection, String initialPageUrl) {
        stateManager = new StateManager(treeDirection, initialPageUrl);
    }

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

    @Override
    public String getNextPageUrl() {
        return stateManager.getNextPageToProcess();
    }

    private List<String[]> processMembers(Model model) {
        List<String[]> ldesMembers = new LinkedList<>();
        StmtIterator iter = model.listStatements(null, model.createProperty("https://w3id.org/tree#", "member"), (String) null);

        iter.forEach(statement -> {
            if (stateManager.processMember(statement.getObject().toString())) {
                Model outputModel = ModelFactory.createDefaultModel();
                outputModel.add(statement);
                populateModel(outputModel, statement.getResource());

                StringWriter outputStream = new StringWriter();

                RDFDataMgr.write(outputStream, outputModel, Lang.NQUADS);

                ldesMembers.add(outputStream.toString().split("\n"));
            }
        });

        return ldesMembers;
    }

    private void processRelations(Model model) {
        List<Statement> relations = model.listStatements(null, model.createProperty("https://w3id.org/tree#", "relation"), (String) null).toList();
        if (stateManager.getTreeDirection() != null) {
            relations = relations.stream()
                    .filter(relation -> Objects.equals(relation.getResource()
                            .getProperty(model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "type"))
                            .getObject().toString(), "https://w3id.org/tree#" + stateManager.getTreeDirection()))
                    .toList();
        }

        relations.forEach(relation -> stateManager.addNewPageToProcess(relation.getResource()
                .getProperty(model.createProperty("https://w3id.org/tree#", "node"))
                .getResource()
                .toString()));
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

}
