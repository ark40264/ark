package bot.util.discord;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import bot.dto.AllianceMemberDto;
import bot.dto.ChatMessageDto;
import bot.util.prop.AppriCationProperties;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

@Component
public class DiscordBot {
	Logger log = LoggerFactory.getLogger(DiscordBot.class);
	private JDA jda;
	private Guild guild;
	@Autowired
	private AppriCationProperties appriCationProperties;

	public void init(ListenerAdapter listenerAdapter) {
		try {
			jda = JDABuilder.createDefault(System.getenv("DISCORD_BOT_TOKEN"))
					.setRawEventsEnabled(true)
					.setMemberCachePolicy(MemberCachePolicy.ALL)
					.enableIntents(GatewayIntent.MESSAGE_CONTENT)
					.enableIntents(GatewayIntent.GUILD_MESSAGES)
					.enableIntents(GatewayIntent.GUILD_MEMBERS)
					.setActivity(Activity.playing("ステータス"))
					.build();
			jda.addEventListener(listenerAdapter);

			jda.updateCommands().queue();
			jda.awaitReady();

			guild = jda.getGuildById(appriCationProperties.getGuildId());
		} catch (Exception e) {
			throw new RuntimeException("DiscordBot初期化エラー.", e);
		}

	}

	public void shutDown() {
		jda.shutdown();
	}
	private String getMention(String discordUserId) {
		User user = jda.retrieveUserById(discordUserId).complete();
		String mention = null;
		if (user != null) {
			mention = user.getAsMention();
		}
		return mention;
	}

	public Guild getGuild() {
		return guild;
	}

	public TextChannel getChannel(String channelId) {
		return guild.getTextChannelById(channelId);
	}

	public void sendMessage(ChatMessageDto chatMessageDto, List<AllianceMemberDto> allianceMemberDtoList) {
		String message = chatMessageDto.getMessage();
		if (message.trim().isEmpty())
			return;
		if (!chatMessageDto.getName().trim().isEmpty()) {
			message = chatMessageDto.getName().trim() + "さんの発言：\n" + message;
		}
		String replaceMessage = addMention(allianceMemberDtoList, message);

		MessageCreateAction messageCreateAction = addAttachment(chatMessageDto, replaceMessage);
		
		Button myButton = Button.primary("open-form-button", "OK");
		messageCreateAction.addActionRow(myButton);
		
		messageCreateAction.queue();
	}

	private MessageCreateAction addAttachment(ChatMessageDto chatMessageDto, String replaceMessage) {
		MessageCreateAction messageCreateAction = getChannel(chatMessageDto.getChannelId()).sendMessage(replaceMessage);
		if (chatMessageDto.getQuoteDiscordId() != null && !chatMessageDto.getQuoteDiscordId().isEmpty())
			messageCreateAction.setMessageReference(chatMessageDto.getQuoteDiscordId());
		if (chatMessageDto.getChatAttachmentDtoList() != null) {
			List<FileUpload> fileUploadList = new ArrayList<FileUpload>();
			chatMessageDto.getChatAttachmentDtoList().forEach(chatAttachmentDto -> {
				FileUpload fileUpload = FileUpload.fromData(chatAttachmentDto.getAttachmentFileInputStream(),
						chatAttachmentDto.getAttachmentFileName());
				fileUploadList.add(fileUpload);
			});
			messageCreateAction.setFiles(fileUploadList);
		}
		return messageCreateAction;
	}

	private String addMention(List<AllianceMemberDto> allianceMemberDtoList, String message) {
		String replaceMessage = "";
		for (AllianceMemberDto allianceMemberDto : allianceMemberDtoList) {
			String mentionName = "@" + allianceMemberDto.getDiscordName();
			Pattern pattern = Pattern.compile("(" + mentionName + ")");
			String[] arr = pattern.split(message);
			if (arr.length == 1)
				continue;
			replaceMessage = "";
			for (int i = 0; i < arr.length; i++) {
				String str = arr[i];
				if (message.startsWith(mentionName)) {
					replaceMessage += getMention(allianceMemberDto.getDiscordMemberId()) + str;
				} else if (i < arr.length - 1) {
					replaceMessage += str + getMention(allianceMemberDto.getDiscordMemberId());
				} else {
					replaceMessage += str;
				}
			}
			message = replaceMessage;
		}
		if (replaceMessage.isEmpty())
			replaceMessage = message;
		return replaceMessage;
	}

}
