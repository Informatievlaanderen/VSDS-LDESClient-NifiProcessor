package be.vlaanderen.informatievlaanderen.ldes.client.services;

import be.vlaanderen.informatievlaanderen.ldes.client.exceptions.LdesException;
import be.vlaanderen.informatievlaanderen.ldes.client.valueobjects.MutableFragmentQueue;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

public class LdesStateManager {
	
	protected final MutableFragmentQueue mutableFragments;
    protected final Queue<String> fragmentsToProcess;
    /**
     * 
     */
    protected final Map<String, Set<String>> processedMembers;
    protected final List<String> processedFragments;
    private final Clock clock;

    public LdesStateManager() {
    	this(Clock.systemDefaultZone());
    }

    protected LdesStateManager(Clock clock) {
    	this.mutableFragments = new MutableFragmentQueue();
        this.fragmentsToProcess = new ArrayDeque<>();
        this.processedMembers = new HashMap<>();
        this.processedFragments = new ArrayList<>();
        this.clock = clock;
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
        
        throw new LdesException("An error occurred while determining if there are more fragments to process.");
    }

    public boolean shouldProcessMember(String fragmentId, String memberId) {
    	if (!processedMembers.containsKey(fragmentId)) {
    		processedMembers.put(fragmentId, new HashSet<>());
    	}
    	
    	return processedMembers.get(fragmentId).add(memberId);
    }

    public void queueFragment(String fragmentUrl, LocalDateTime expirationDate) {
        if (!processedFragments.containsKey(fragmentUrl)) {
            fragmentsToProcess.add(fragmentUrl);
        }
    }
    
    public void processFragment(String fragmentUrl) {
    	processFragment(fragmentUrl, false, null);
    }
    
    public void processFragment(String fragmentUrl, LocalDateTime expirationDate) {
    	processFragment(fragmentUrl, false, expirationDate);
    }

    public void processFragment(String fragmentUrl, boolean immutable, LocalDateTime expirationDate) {
        if (maxAge == null) {
            this.processedFragments.put(fragmentUrl, new FragmentSettings(IMMUTABLE));
        } else {
            this.processedFragments.put(fragmentUrl, new FragmentSettings(LocalDateTime.now(clock).plusSeconds(maxAge)));
        }
    }

    public void populateFragmentQueue() {
        processedFragments.forEach((fragmentURL, fragmentSettings) -> {
            if (fragmentSettings.getExpireDate() != null && fragmentSettings.getExpireDate().isBefore(LocalDateTime.now(clock))) {
                fragmentsToProcess.add(fragmentURL);
            }
        });
    }
}
