package be.vlaanderen.informatievlaanderen.ldes.client.valueobjects;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MutableFragmentQueue {

	private final Map<String, LocalDateTime> mutableFragments;
	protected final Map<String, Set<String>> processedMembers;

	public MutableFragmentQueue() {
		mutableFragments = new HashMap<>();
		processedMembers = new HashMap<>();
	}

	public boolean hasNext() {
		return next() != null;
	}

	/**
	 * Return the next expired mutable fragment (or the next mutable fragment if no expiration date has been set).
	 * 
	 * If the next mutable fragment has no expiration date set, return it.
	 * If it has an expiration date, return it only if the fragment has expired.
	 * 
	 * @return the fragment id (URL) of the next mutable fragment
	 */
	public String next() {
		for (String fragmentId : mutableFragments.keySet()) {
			// If an expiration date is set, only return this mutable fragment if it is expired
			LocalDateTime expirationDate = mutableFragments.get(fragmentId);
			
			if (expirationDate == null) {
				return fragmentId;
			}
			
			if (expirationDate.isBefore(LocalDateTime.now())) {
				return fragmentId;
			}
		}

		return null;
	}

	public boolean hasMutableFragment(String fragmentId) {
		return mutableFragments.containsKey(fragmentId);
	}

	public void addMutableFragment(LdesFragment fragment) {
		mutableFragments.put(fragment.getFragmentId(), fragment.getExpirationDate());
		processedMembers.put(fragment.getFragmentId(), new HashSet<>());
	}

	/**
	 * Removes the mutable fragment and it's processed members.
	 * @param fragment the fragment to remove
	 */
	public void removeMutableFragment(LdesFragment fragment) {
		mutableFragments.remove(fragment.getFragmentId());
		processedMembers.remove(fragment.getFragmentId());
	}
	
	public void addMembers(LdesFragment fragment, Set<String> members) {
		processedMembers.get(fragment.getFragmentId()).addAll(members);
	}
}
