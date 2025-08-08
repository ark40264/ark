package bot.dto;

import java.util.List;

import lombok.Data;

@Data
public class ChatMessageDto {
	private Integer id;
	private String discordMessageId;
	private String quoteId;
	private String quoteDiscordId;
	private String channelId;
	private String channelName;
	private String name;
	private String message;
	private List<ChatAttachmentDto> chatAttachmentDtoList;
	private String createDate;
}
