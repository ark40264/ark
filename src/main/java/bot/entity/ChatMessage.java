package bot.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import lombok.Data;

@Data
@Entity
public class ChatMessage {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	private String discordMessageId;
	private String quoteId;
	private String quoteDiscordId;
	private String name;
	private String message;
	@OneToMany(mappedBy = "chatMessage", fetch = FetchType.EAGER)
	private List<ChatAttachment> chatAttachmentList = new ArrayList<>();
	private Integer channelMasterId;
	private String createDate;
}
