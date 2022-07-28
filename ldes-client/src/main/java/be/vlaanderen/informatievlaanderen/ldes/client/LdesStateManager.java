package be.vlaanderen.informatievlaanderen.ldes.client;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.vlaanderen.informatievlaanderen.ldes.client.exceptions.LdesException;
import be.vlaanderen.informatievlaanderen.ldes.client.valueobjects.LdesFragment;

public class LdesStateManager {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(LdesStateManager.class);
	
    protected final Queue<String> fragmentsToProcess;

    protected final List<String> processedImmutableFragments;
    protected final Map<String, LocalDateTime> processedMutableFragments;
    /**
     * A map of key-value pairs with the fragment id as key.
     */
	protected final Map<String, Set<String>> processedMutableFragmentMembers;

    public LdesStateManager() {
        fragmentsToProcess = new ArrayDeque<>();
        
        processedImmutableFragments = new ArrayList<>();
        processedMutableFragments = new HashMap<>();
		processedMutableFragmentMembers = new HashMap<>();
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
    		processedMutableFragmentMembers.put(fragmentId, new HashSet<>());
        	LOGGER.info("QUEUE: Queued fragment {}", fragmentId);
        	return true;
    	}

    	throw new LdesException("Unable to decide if the fragment needs to be queued ! (" + fragmentId + ")");
    }

    public void processedFragment(LdesFragment fragment) {
    	if (fragment.isImmutable()) {
    		processedImmutableFragments.add(fragment.getFragmentId());
    		processedMutableFragments.remove(fragment.getFragmentId());
    		processedMutableFragmentMembers.remove(fragment.getFragmentId());
    	}
    	else {
    		processedMutableFragments.put(fragment.getFragmentId(), fragment.getExpirationDate());
    		processedMutableFragmentMembers.put(fragment.getFragmentId(), fragment.getMembers().stream().map(member -> member.getMemberId()).collect(Collectors.toSet()));
    	}
		fragmentsToProcess.remove(fragment.getFragmentId());
    }

	public boolean shouldProcessMember(LdesFragment fragment, String memberId) {
		return !(processedMutableFragmentMembers.containsKey(fragment.getFragmentId())
				&& processedMutableFragmentMembers.get(fragment.getFragmentId()).contains(memberId));
	}
}
