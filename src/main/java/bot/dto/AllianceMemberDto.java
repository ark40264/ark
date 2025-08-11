package bot.dto;

import java.util.ArrayList;
import java.util.List;

import bot.entity.ChatMessageView;
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
	private boolean bot = false;
	private List<ChatMessageView> chatMessageViewList = new ArrayList<ChatMessageView>();
}
