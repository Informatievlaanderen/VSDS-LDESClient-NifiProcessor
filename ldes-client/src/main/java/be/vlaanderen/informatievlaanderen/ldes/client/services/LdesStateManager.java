package be.vlaanderen.informatievlaanderen.ldes.client.services;

import be.vlaanderen.informatievlaanderen.ldes.client.exceptions.LdesException;
import be.vlaanderen.informatievlaanderen.ldes.client.valueobjects.LdesFragment;
import be.vlaanderen.informatievlaanderen.ldes.client.valueobjects.MutableFragmentQueue;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LdesStateManager {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(LdesStateManager.class);
	
	protected final MutableFragmentQueue mutableFragments;
    protected final Queue<String> fragmentsToProcess;
	protected final Map<String, Set<String>> processedMembers;

    protected LdesStateManager() {
    	mutableFragments = new MutableFragmentQueue();
        fragmentsToProcess = new ArrayDeque<>();
		processedMembers = new HashMap<>();
    }

    public boolean hasFragmentsToProcess() {
        return fragmentsToProcess.iterator().hasNext() || mutableFragments.hasNext();
    }

    public String getNextFragment() {
        if (!hasFragmentsToProcess()) {
            throw new LdesException("No more fragments to process");
        }

        if (fragmentsToProcess.iterator().hasNext()) {
        	return fragmentsToProcess.poll();
        }
        
        if (mutableFragments.hasNext()) {
        	return mutableFragments.next();
        }
        
        throw new LdesException("An error occurred while getting next fragment to process.");
    }
    
    public void queueFragment(String fragmentId) {
    	queueFragment(fragmentId, null);
    }

    public void queueFragment(String fragmentId, LocalDateTime expirationDate) {
    	if (!mutableFragments.hasMutableFragment(fragmentId)) {
    		fragmentsToProcess.add(fragmentId);

    		if (!processedMembers.containsKey(fragmentId)) {
    			processedMembers.put(fragmentId, new HashSet<>());
    		}
    	}
    }
    
    public void processedFragment(LdesFragment fragment) {
    	if (fragment.isImmutable()) {
    		mutableFragments.removeMutableFragment(fragment);
    	}
    	else {
    		mutableFragments.addMutableFragment(fragment);
    		mutableFragments.addMembers(fragment, processedMembers.get(fragment.getFragmentId()));
    	}
    	
		fragmentsToProcess.remove(fragment.getFragmentId());
		processedMembers.remove(fragment.getFragmentId());
    }
	
	public void addMember(LdesFragment fragment, String memberId) {
		processedMembers.get(fragment.getFragmentId()).add(memberId);
	}

	public boolean shouldProcessMember(LdesFragment fragment, String memberId) {
		return !(processedMembers.containsKey(fragment.getFragmentId())
				&& processedMembers.get(fragment.getFragmentId()).contains(memberId));
	}
}
