package bot.model.discord;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import bot.ArkApplication;
import bot.dto.AllianceMemberDto;
import bot.dto.ChatAttachmentDto;
import bot.dto.ChatMessageDto;
import bot.dto.MemberAlliance;
import bot.dto.MemberRole;
import bot.entity.ChatMessage;
import bot.repository.ChannelMasterRepository;
import bot.repository.ChatMessageRepository;
import bot.service.ChatService;
import bot.service.MemberService;
import bot.util.discord.DiscordBot;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

@Component
public class DiscordModel extends ListenerAdapter {
	private static final Logger log = LoggerFactory.getLogger(DiscordModel.class);
	@Autowired
	private DiscordBot discordBot;
	@Autowired
	private MemberService memberService;
	@Autowired
	private ChatService chatService;
	@Autowired
	private ChatMessageRepository chatMessageRepository;
	@Autowired
	private ChannelMasterRepository channelRepository;
	private List<DIscordEventListener> dIscordEventListenerList = new ArrayList<DIscordEventListener>();

	private List<Message> messageList;

	public void initDiscordMember() {
		discordBot.getGuild().loadMembers().onSuccess(members -> {
			if (members.isEmpty()) {
				log.error("このギルドにはメンバーがいません（または取得できませんでした）。");
				return;
			}

			try {
				for (Member member : members) {
					memberService.init(getName(member), member.getId(), member.getUser().isBot());
				}
			} catch (Exception e) {
				log.error("メンバー取得でエラー", e);
				throw e;
			}
		}).onError(throwable -> {
			log.error("メンバー取得でエラー", throwable);
		});
		log.info("Discordメンバー取得完了");
	}

	public void getHistory(int limit) {
		List<bot.entity.ChannelMaster> channelList = channelRepository.findAll();
		channelList.forEach(channel -> {
			String messageId;
			ChatMessageDto chatMessageDto = new ChatMessageDto();
			chatMessageDto.setChannelId(channel.getChannelId());
			chatMessageDto.setChannelName(channel.getChannelName());
			chatMessageDto.setName("ボット");
			chatMessageDto.setMessage("ボット起動");
			sendMessage(chatMessageDto);
			// TODO 気に食わない
			try {
				Thread.sleep(5000L);
			} catch (InterruptedException e) {
			}
		    PageRequest pageable = PageRequest.of(0, 1000);
		    Page<ChatMessage> page = chatMessageRepository.findByChannelMasterId(channel.getId(), pageable);
		    List<ChatMessage> list = page.getContent();
		    
			if (!list.isEmpty()) {
				messageId = list.getFirst().getDiscordMessageId();
				discordBot.getChannel(channel.getChannelId()).getHistoryBefore(messageId, limit).queue(
						history -> {
							messageList = history.getRetrievedHistory();
							log.info("基準メッセージ (" + messageId + ") より前のメッセージ " + messageList.size()
									+ " 件を取得しました。");
							chatService.saveChatHistory(messageList, channel);

						});
			}
		});
	}

	public void addDiscordEventListener(DIscordEventListener dIscordEventListener) {
		dIscordEventListenerList.add(dIscordEventListener);
	}

	public void removeDiscordEventListener(DIscordEventListener dIscordEventListener) {
		dIscordEventListenerList.remove(dIscordEventListener);
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		try {
			ChatMessageDto chatMessageDto = createChatMessageDto(event, event.getMember(), event.getMessage());
			for (DIscordEventListener dIscordEventListener : dIscordEventListenerList) {
				dIscordEventListener
						.onMessageReceived(chatMessageDto);

			}
		} catch (Exception e) {
			log.error("error.", e);
			throw e;
		}
	}

	private ChatMessageDto createChatMessageDto(GenericMessageEvent event, Member member, Message discoMessage) {
		List<Attachment> attachmentList = discoMessage.getAttachments();

		List<ChatAttachmentDto> attachmentDtoList = new ArrayList<ChatAttachmentDto>();
		if (attachmentList != null && !attachmentList.isEmpty()) {
			attachmentList.forEach((attachment) -> {
				ChatAttachmentDto chatAttachmentDto = new ChatAttachmentDto();
				chatAttachmentDto.setAttachmentFileInputStream(null);
				chatAttachmentDto.setAttachmentFileName(attachment.getFileName());
				chatAttachmentDto.setAttachmentUrl(attachment.getUrl());
				attachmentDtoList.add(chatAttachmentDto);
			});
		}
		AllianceMemberDto allianceMemberDto = memberService.getAllianceMemberDto(member.getId());
		if (allianceMemberDto == null) {
			allianceMemberDto = new AllianceMemberDto();
			log.warn("メンバーにいない人からメッセージ message=" + discoMessage);
		}
		String message = discoMessage.getContentDisplay();
		Message referencedMessage = discoMessage.getReferencedMessage();
		String referencedMessageId = null;
		if (referencedMessage != null)
			referencedMessageId = referencedMessage.getId();
		MessageChannelUnion messageChannelUnion = event.getChannel();

		ChatMessageDto chatMessageDto = new ChatMessageDto();
		chatMessageDto.setChatAttachmentDtoList(attachmentDtoList);
		ZoneId zone = ZoneId.of("Etc/GMT+1");
		ZonedDateTime zonedDateTime = discoMessage.getTimeCreated().atZoneSameInstant(zone);
		Instant instant = zonedDateTime.toInstant();
		Date date = Date.from(instant);
		chatMessageDto.setCreateDate(ArkApplication.sdf.format(date));
		chatMessageDto.setDiscordMessageId(discoMessage.getId());
		chatMessageDto.setMessage(message);
		chatMessageDto.setName(getName(member));
		chatMessageDto.setQuoteDiscordId(referencedMessageId);
		chatMessageDto.setChannelId(messageChannelUnion.getId());
		chatMessageDto.setChannelName(messageChannelUnion.getName());

		ChatMessage chatMessage = chatMessageRepository.findByDiscordMessageId(referencedMessageId);
		if (chatMessage != null)
			chatMessageDto.setQuoteId(chatMessage.getId().toString());

		return chatMessageDto;
	}

	private String getName(Member member) {
		String nickname = member.getNickname();
		String effectiveName = member.getEffectiveName();
		if (nickname == null) {
			nickname = effectiveName;
		}
		return nickname;
	}

	@Override
	public void onMessageUpdate(MessageUpdateEvent event) {
		try {
			ChatMessageDto chatMessageDto = createChatMessageDto(event, event.getMember(), event.getMessage());
			for (DIscordEventListener dIscordEventListener : dIscordEventListenerList) {
				dIscordEventListener.onMessageUpdate(chatMessageDto);

			}
		} catch (Exception e) {
			log.error("error.", e);
			throw e;
		}
	}

	@Override
	public void onMessageDelete(MessageDeleteEvent event) {
		try {
			for (DIscordEventListener dIscordEventListener : dIscordEventListenerList) {
				dIscordEventListener
						.onMessageDelete(event.getMessageId());
			}
		} catch (Exception e) {
			log.error("error.", e);
			throw e;
		}
	}

	@Override
	public void onGuildMemberJoin(GuildMemberJoinEvent event) {
		try {
			Member member = event.getMember();
			AllianceMemberDto allianceMemberDto = new AllianceMemberDto();
			allianceMemberDto.setAlliance(MemberAlliance.NONE);
			allianceMemberDto.setAyarabuId("mitsu");
			allianceMemberDto.setAyarabuName(getName(member));
			allianceMemberDto.setBot(false);
			allianceMemberDto.setCreateDate(ArkApplication.sdf.format(new Date()));
			allianceMemberDto.setDiscordName(getName(member));
			allianceMemberDto.setMemberRole(MemberRole.MEMBER);
			allianceMemberDto.setStatementCount(0);
			for (DIscordEventListener dIscordEventListener : dIscordEventListenerList) {
				dIscordEventListener
						.onGuildMemberJoin(allianceMemberDto);
			}
		} catch (Exception e) {
			log.error("error.", e);
			throw e;
		}
	}

	@Override
	public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
		try {
			Member member = event.getMember();
			AllianceMemberDto allianceMemberDto = memberService.getAllianceMemberDto(member.getId());
			allianceMemberDto.setBot(false);
			for (DIscordEventListener dIscordEventListener : dIscordEventListenerList) {
				dIscordEventListener
						.onGuildMemberRemove(allianceMemberDto);
			}
		} catch (Exception e) {
			log.error("error.", e);
			throw e;
		}
	}

	@Override
	public void onStringSelectInteraction(StringSelectInteractionEvent event) {
		String customId = event.getComponentId();
		if (customId.equals("selectId1")) {
			String selectedValue = event.getValues().get(0);
			event.reply(selectedValue + "が選択されました。").setEphemeral(true).queue();
		}
		event.deferReply(true).queue();
	}

	public void sendMessage(ChatMessageDto chatMessageDto) {
		discordBot.sendMessage(chatMessageDto, memberService.getAllianceMemberDtoList());
	}

	@Override
	public void onButtonInteraction(ButtonInteractionEvent event) {
		if (event.getComponentId().equals("open-form-button")) {
			String message = event.getMessage().getContentRaw();
			TextInput subject = TextInput.create("subject-field", "件名", TextInputStyle.SHORT)
					.setPlaceholder("件名を入力してください")
					.setMaxLength(100)
					.setValue("hogehoge")
					.setRequired(true)
					.build();

			TextInput body = TextInput.create("body-field", "本文", TextInputStyle.PARAGRAPH)
					.setPlaceholder("詳細を入力してください")
					.setMaxLength(100)
					.setRequired(true)
					.build();

			Modal modal = Modal.create("my-form-modal", "フィードバックを送信")
					.addComponents(ActionRow.of(subject), ActionRow.of(body))
					.build();

			event.replyModal(modal).queue(); // モーダルを表示
		}
	}

	@Override
	public void onModalInteraction(net.dv8tion.jda.api.events.interaction.ModalInteractionEvent event) {
		if (event.getModalId().equals("my-form-modal")) {
			String subject = event.getValue("subject-field").getAsString();
			String body = event.getValue("body-field").getAsString();

			event.reply("フィードバックを受け付けました！\n件名: " + subject + "\n本文: " + body).setEphemeral(true).queue();
		}
	}
}
