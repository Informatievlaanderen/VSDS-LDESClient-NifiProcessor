package be.vlaanderen.informatievlaanderen.ldes.client.valueobjects;

public class LdesMember {
	
	private final String memberId;
	private final String memberData;
	
	public LdesMember(final String memberId, final String memberData) {
		this.memberId = memberId;
		this.memberData = memberData;
	}
	
	public String getMemberId() {
		return memberId;
	}
	
	public String getMemberData() {
		return memberData;
	}
}
