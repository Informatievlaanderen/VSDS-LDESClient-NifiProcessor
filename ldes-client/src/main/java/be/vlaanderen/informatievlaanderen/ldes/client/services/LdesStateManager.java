package be.vlaanderen.informatievlaanderen.ldes.client.services;

import be.vlaanderen.informatievlaanderen.ldes.client.exceptions.LdesException;
import be.vlaanderen.informatievlaanderen.ldes.client.valueobjects.LdesFragment;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LdesStateManager {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(LdesStateManager.class);
	
    protected final Queue<String> fragmentsToProcess;
    
    protected final Map<String, LocalDateTime> processedMutableFragments;
    protected final List<String> processedImmutableFragments;
	protected final Map<String, Set<String>> processedMembers;

    protected LdesStateManager() {
        fragmentsToProcess = new ArrayDeque<>();
        
        processedMutableFragments = new HashMap<>();
        processedImmutableFragments = new ArrayList<>();
		processedMembers = new HashMap<>();
    }

	public boolean hasNext() {
		return hasNextQueuedFragment() || nextExpiredFragment() != null;
	}
	
	public boolean hasNextQueuedFragment() {
		return !fragmentsToProcess.isEmpty();
	}

	/**
	 * Return the next queued fragment or the next expired or expiration-date-less mutable fragment).
	 * 
	 * If there are more fragments queued, return the next one.
	 * If the next mutable fragment has no expiration date set, return it.
	 * If it has an expiration date, return it only if the fragment has expired.
	 * 
	 * @return the fragment id (URL) of the next fragment
	 */
	public String next() {
		if (hasNextQueuedFragment()) {
			return fragmentsToProcess.poll();
		}
		
		String fragment = nextExpiredFragment();
		if (fragment != null) {
			return fragment;
		}

        throw new LdesException("No fragments to process");
	}
	
	public String nextExpiredFragment() {
		for (String fragmentId : processedMutableFragments.keySet()) {
			// If an expiration date is set, only return this mutable fragment if it is expired
			LocalDateTime expirationDate = processedMutableFragments.get(fragmentId);

			if (expirationDate == null) {
				return fragmentId;
			}

			if (expirationDate.isBefore(LocalDateTime.now())) {
				return fragmentId;
			}
		}
		
		return null;
	}

	/**
	 * Returns true if the fragment was queued, false otherwise.
	 * 
	 * @param fragmentId the id of the fragment to queue
	 * @return true if the fragment was queued, false otherwise
	 * @throws LdesException if the fragment is not in the queue, not in the processed immutable fragments list and present in the processed mutable fragments list.
	 */
    public boolean queueFragment(String fragmentId) {
    	return queueFragment(fragmentId, null);
    }

    /**
     * Returns true if the fragment was queued, false otherwise.
     * 
     * 1) If the fragments queue already contains the fragment id, don't queue it and return false.
     * 2) If the fragment is immutable and was already processed, don't queue it and return false.
     * 3) If the fragment is not in the processed mutable fragments queue, queue it and return true.
     * 4) Throw an {@link LdesException}
     * 
     * @param fragmentId the id of the fragment to queue
     * @param expirationDate the expiration date of the mutable fragment
     * @return true if the fragment was queued, false otherwise
     * @throws LdesException if the fragment is not in the queue, not in the processed immutable fragments list and present in the processed mutable fragments list.
     */
    public boolean queueFragment(String fragmentId, LocalDateTime expirationDate) {
    	if (fragmentsToProcess.contains(fragmentId)) {
    		LOGGER.info("QUEUE: Not queueing already queued fragment {}", fragmentId);
    		return false;
    	}

    	if (processedImmutableFragments.contains(fragmentId)) {
    		LOGGER.info("QUEUE: Not queueing processed immutable fragment {}", fragmentId);
    		return false;
    	}

    	if (!processedMutableFragments.containsKey(fragmentId)) {	
    		fragmentsToProcess.add(fragmentId);
    		processedMembers.put(fragmentId, new HashSet<>());
        	LOGGER.info("QUEUE: Queued fragment {}", fragmentId);
        	return true;
    	}

    	throw new LdesException("Unable to decide if the fragment needs to be queued ! (" + fragmentId + ")");
    }

    public void processedFragment(LdesFragment fragment) {
    	if (fragment.isImmutable()) {
    		processedImmutableFragments.add(fragment.getFragmentId());
    		
    		fragmentsToProcess.remove(fragment.getFragmentId());
    		processedMembers.remove(fragment.getFragmentId());
    	}
    }
	
	public void addMember(LdesFragment fragment, String memberId) {
		processedMembers.get(fragment.getFragmentId()).add(memberId);
	}

	public boolean shouldProcessMember(LdesFragment fragment, String memberId) {
		return !(processedMembers.containsKey(fragment.getFragmentId())
				&& processedMembers.get(fragment.getFragmentId()).contains(memberId));
	}
}
