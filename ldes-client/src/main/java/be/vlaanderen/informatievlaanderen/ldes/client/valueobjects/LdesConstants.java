package be.vlaanderen.informatievlaanderen.ldes.client.valueobjects;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;

public class LdesConstants {
    public static final Model model = ModelFactory.createDefaultModel();

    public static final String W3C_TREE = "https://w3id.org/tree#";

    public static final Property W3ID_TREE_MEMBER = model.createProperty(W3C_TREE, "member");
    public static final Property W3ID_TREE_NODE = model.createProperty(W3C_TREE, "node");
    public static final Property W3ID_TREE_RELATION = model.createProperty(W3C_TREE, "relation");

    public static final Property RDF_TYPE = model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "type");
}
