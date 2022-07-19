package be.vlaanderen.informatievlaanderen.ldes.client.valueobjects;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class MutableFragmentQueue {

	private final Map<String, LocalDateTime> mutableFragments = new HashMap<>();
	
	public boolean hasNext() {
		return next() != null;
	}
	
	public String next() {
		for (String fragmentId : mutableFragments.keySet()) {
			if (mutableFragments.get(fragmentId) != null && mutableFragments.get(fragmentId).isBefore(LocalDateTime.now())) {
				return fragmentId;
			}
		}
		
		return null;
	}
}
