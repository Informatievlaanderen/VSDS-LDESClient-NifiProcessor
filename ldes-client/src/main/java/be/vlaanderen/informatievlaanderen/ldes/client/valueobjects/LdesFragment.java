package be.vlaanderen.informatievlaanderen.ldes.client.valueobjects;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import be.vlaanderen.informatievlaanderen.ldes.client.exceptions.LdesMemberNotFoundException;

public class LdesFragment {

    private final Model model = ModelFactory.createDefaultModel();
    
    private String fragmentId;
    private LocalDateTime expirationDate;
    
    private boolean immutable = false;
    private List<LdesMember> members = new ArrayList<>();
    private List<String> relations = new ArrayList<>();
    
    public LdesFragment() {
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
    
    public List<LdesMember> getMembers() {
    	return members;
    }
    
    public void setMembers(List<LdesMember> members) {
    	this.members = members;
    }
    
    public void addMember(LdesMember member) {
    	members.add(member);
    }
    
    public LdesMember getMember(String memberId) {
    	for (LdesMember member : members) {
    		if (member.getMemberId().equalsIgnoreCase(memberId)) {
    			return member;
    		}
    	}
    	throw new LdesMemberNotFoundException(memberId);
    }
    
    public List<String> getRelations() {
    	return relations;
    }
    
    public void addRelation(String relation) {
    	relations.add(relation);
    }
}
