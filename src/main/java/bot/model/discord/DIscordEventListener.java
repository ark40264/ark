package bot.model.discord;

import bot.dto.AllianceMemberDto;
import bot.dto.ChatMessageDto;

public interface DIscordEventListener {
	public void onMessageReceived(ChatMessageDto chatMessageDto) ;
	public void onMessageUpdate(ChatMessageDto chatMessageDto) ;
	public void onMessageDelete(String messageId) ;
	public void onGuildMemberJoin(AllianceMemberDto allianceMemberDto) ;
	public void onGuildMemberRemove(AllianceMemberDto allianceMemberDto) ;

}
