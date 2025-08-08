package bot.form;

import lombok.Data;

@Data
public class AllianceMemberForm {
	private Integer id;
	private String memberRole;
	private String discordMemberId;
	private String discordName;
	private String ayarabuId;
	private String ayarabuName;
	private String alliance;
	private Integer statementCount;
	private String createDate;
	private boolean isBot = false;
}
