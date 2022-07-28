package be.vlaanderen.informatievlaanderen.ldes.client.valueobjects;

public class LdesMember {
	
	private final String memberId;
	private final String statement;
	
	public LdesMember(final String memberId, final String statement) {
		this.memberId = memberId;
		this.statement = statement;
	}
	
	public String getMemberId() {
		return memberId;
	}
	
	public String getStatement() {
		return statement;
	}
}
