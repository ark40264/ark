package bot.dto;

import lombok.Data;

@Data
public class AllianceMemberDto {
	private Integer id;
	private MemberRole memberRole = MemberRole.MEMBER;
	private String discordMemberId;
	private String discordName;
	private String ayarabuId;
	private String ayarabuName;
	private MemberAlliance alliance = MemberAlliance.NONE;
	private Integer statementCount;
	private String createDate;
	private boolean isBot = false;
}
