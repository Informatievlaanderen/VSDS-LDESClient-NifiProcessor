package be.vlaanderen.informatievlaanderen.ldes.client.services;

import java.time.LocalDateTime;
import java.util.Map;

import be.vlaanderen.informatievlaanderen.ldes.client.valueobjects.LdesFragment;

public interface LdesService {
	
	/**
	 * Queues a fragment for immediate fetching.
	 * 
	 * Internally calls {@link #queueFragment(String, LocalDateTime)} with an expirationDate of LocalDateTime.now().
	 * 
	 * @param fragmentId the fragment id (i.e. URL) to process.
	 */
	void queueFragment(String fragmentId);
	
	/**
	 * Queues a fragment for processing after the fragments expirationDate.
	 * 
	 * @param fragmentId the fragment id (i.e. URL) to process.
	 * @param expirationDate the fragment's expiration date.
	 */
	void queueFragment(String fragmentId, LocalDateTime expirationDate);
	
	/**
	 * Processes all queued LDES fragments.
	 * @return a Map that contains the mutable fragment id's as keys and their expiration dates as values.
	 */
	Map<String, String> processStream();

	/**
	 * Checks if there are unprocessed fragments
	 * @return true if there are unprocessed fragments, false otherwise.
	 */
	boolean hasFragmentsToProcess();
	
	/**
	 * Processes the next available LDES fragment.
	 * @return the processed LDES fragment containing the members and an expiration date.
	 */
	LdesFragment processNextFragment();
}
