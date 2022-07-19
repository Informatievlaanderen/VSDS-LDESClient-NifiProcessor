package be.vlaanderen.informatievlaanderen.ldes.client.valueobjects;

import java.time.LocalDateTime;

public class MutableFragment {

	private final String fragmentId;
	private final LocalDateTime expirationDate;
	
	public MutableFragment(final String fragmentId, final LocalDateTime expirationDate) {
		this.fragmentId = fragmentId;
		this.expirationDate = expirationDate;
	}
	
	public String getFragmentId() {
		return fragmentId;
	}
	
	public LocalDateTime getExpirationDate() {
		return expirationDate;
	}
}
