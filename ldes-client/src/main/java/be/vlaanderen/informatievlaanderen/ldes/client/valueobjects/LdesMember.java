package be.vlaanderen.informatievlaanderen.ldes.client.valueobjects;

public class LdesMember {
	
	private final String memberId;
	private final String[] statements;
	
	public LdesMember(final String memberId, final String[] statements) {
		this.memberId = memberId;
		this.statements = statements;
	}
	
	public String getMemberId() {
		return memberId;
	}
	
	public String[] getStatements() {
		return statements;
	}
}
