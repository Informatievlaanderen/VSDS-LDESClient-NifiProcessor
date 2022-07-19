package be.vlaanderen.informatievlaanderen.ldes.client.valueobjects;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

public class LdesFragment {

    private final Model model = ModelFactory.createDefaultModel();
    
    private String fragmentId;
    private LocalDateTime expirationDate;
    
    private boolean immutable = false;
    private List<String[]> members;
    private List<String> relations = new ArrayList<>();
    
    public LdesFragment() {
    	this(null, null);
    }
    
    public LdesFragment(LocalDateTime expirationDate) {
    	this(null, null);
    }

    public LdesFragment(String fragmentId, LocalDateTime expirationDate) {
    	this.fragmentId = fragmentId;
        this.expirationDate = expirationDate;
    }

    public Model getModel() {
        return model;
    }

    public String getFragmentId() {
        return fragmentId;
    }

    public void setFragmentId(String fragmentId) {
    	this.fragmentId = fragmentId;
    }

    public LocalDateTime getExpirationDate() {
        return expirationDate;
    }
    
    public void setExpirationDate(LocalDateTime expirationDate) {
    	this.expirationDate = expirationDate;
    }
    
    public boolean isImmutable() {
    	return immutable;
    }
    
    public void setImmutable(boolean immutable) {
    	this.immutable = immutable;
    }
    
    public List<String[]> getMembers() {
    	return members;
    }
    
    public void setMembers(List<String[]> members) {
    	this.members = members;
    }
    
    public List<String> getRelations() {
    	return relations;
    }
    
    public void addRelation(String relation) {
    	relations.add(relation);
    }
}
