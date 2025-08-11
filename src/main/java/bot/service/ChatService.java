package bot.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bot.ArkApplication;
import bot.dto.AllianceMemberDto;
import bot.dto.ChatAttachmentDto;
import bot.dto.ChatMessageDto;
import bot.entity.ChannelMaster;
import bot.entity.ChatAttachment;
import bot.entity.ChatMessage;
import bot.entity.ChatMessageView;
import bot.model.discord.DIscordEventListener;
import bot.repository.ChannelMasterRepository;
import bot.repository.ChatAttachmentRepository;
import bot.repository.ChatMessageRepository;
import bot.repository.ChatMessageViewRepository;
import bot.util.discord.DiscordBot;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;

@Service
public class ChatService implements DIscordEventListener {

	private static final Logger log = LoggerFactory.getLogger(ChatService.class);
	@Autowired
	private MemberService memberService;
	@Autowired
	private DiscordBot discordBot;
	@Autowired
	private ChannelMasterRepository channelRepository;
	@Autowired
	private ChatAttachmentRepository chatAttachmentRepository;
	@Autowired
	private ChatMessageRepository chatMessageRepository;
	@Autowired
	private ChatMessageViewRepository chatMessageViewRepository;
	private ModelMapper modelMapper;

	public ChatService() {
		modelMapper = new ModelMapper();
	}

	@Transactional
	public void init() {
		List<ChannelMaster> channelList = channelRepository.findAll();
		channelList.forEach(channel -> {
			processChannel(channel);
		});
	}

	public boolean existNewMessage(String channelId, int chatMessageid) {
		PageRequest pageable = PageRequest.of(0, 200);
		List<ChatMessageDto> chatMessageDtoList = getChatMessageDtoList(channelId, pageable, null);
		boolean result = false;
		for (ChatMessageDto chatMessageDto : chatMessageDtoList) {
			if (chatMessageDto.getId() > chatMessageid) {
				result = true;
				break;
			}
		}
		return result;
	}

	@Async
	@Transactional // ここでトランザクションを確保
	private void processChannel(ChannelMaster channel) {
		PageRequest pageable = PageRequest.of(0, 200);
		Page<ChatMessage> page = chatMessageRepository.findByChannelMasterId(channel.getId(), pageable);
		List<ChatMessageDto> chatMessageDtoList = modelMapper.map(page.getContent(),
				new TypeToken<List<ChatMessageDto>>() {
				}.getType());
		chatMessageDtoList.forEach(chatMessageDto -> {
			chatMessageDto.setChannelId(channel.getChannelId());
			chatMessageDto.setChannelName(channel.getChannelName());
		});
	}

	@Transactional // このメソッド全体を単一のトランザクションで実行
	public void saveChatHistory(List<Message> messageList, ChannelMaster channel) {
		List<Message> sortMessageList = new ArrayList<>(messageList);
		sortMessageList.sort(Comparator.comparing(Message::getIdLong));

		for (Message message : sortMessageList) {
			if (chatMessageRepository.findByDiscordMessageId(message.getId()) != null)
				continue;

			// TODO ChannelとChatMessageのリレーションシップを適切に設定する
			// 現在は channel_master_id が使われている

			ChatMessage chatMessage = new ChatMessage();
			ZoneId zone = ZoneId.of("Etc/GMT+9");
			ZonedDateTime zonedDateTime = message.getTimeCreated().atZoneSameInstant(zone);
			Instant instant = zonedDateTime.toInstant();
			Date date = Date.from(instant);
			chatMessage.setCreateDate(ArkApplication.sdf.format(date));
			chatMessage.setDiscordMessageId(message.getId());
			chatMessage.setMessage(message.getContentDisplay().replace("\n", "<br>"));
			chatMessage.setName(getName(message.getMember()));
			chatMessage.setChannelMasterId(channel.getId()); // この部分はあなたのエンティティ構造に合わせる

			if (message.getReferencedMessage() != null)
				chatMessage.setQuoteDiscordId(message.getReferencedMessage().getId());

			ChatMessage savedChatMessage = chatMessageRepository.save(chatMessage);

			// 添付ファイルの保存
			message.getAttachments().forEach((attachment) -> {
				ChatAttachment chatAttachment = new ChatAttachment();
				chatAttachment.setAttachmentUrl(attachment.getUrl());
				chatAttachment.setChatMessage(savedChatMessage);
				chatAttachment.setAttachmentFileName(attachment.getFileName());
				chatAttachmentRepository.save(chatAttachment);
			});

			// quoteIdの更新
			ChatMessage quoteChatMessage = chatMessageRepository
					.findByDiscordMessageId(savedChatMessage.getQuoteDiscordId());
			if (quoteChatMessage != null) {
				savedChatMessage.setQuoteId(quoteChatMessage.getId().toString());
				chatMessageRepository.save(savedChatMessage);
			}
		}
		log.info("履歴のデータベース保存が完了しました。");
	}

	private String getName(Member member) {
		if (member == null) {
			return "不明";
		}
		String nickname = member.getNickname();
		String effectiveName = member.getEffectiveName();
		if (nickname == null) {
			nickname = effectiveName;
		}
		return nickname;
	}

	@Override
	@Transactional
	public synchronized void onMessageReceived(ChatMessageDto chatMessageDto) {
		ChannelMaster channel = channelRepository.findByChannelId(chatMessageDto.getChannelId());
		String message = chatMessageDto.getMessage().replace("\n", "<br>");
		chatMessageDto.setMessage(message);
		log.info("ChatService:メッセージ:" + chatMessageDto);

		// DB保存
		ChatMessage chatMessage = modelMapper.map(chatMessageDto, ChatMessage.class);
		chatMessage.setChannelMasterId(channel.getId());
		// TODO ChatMessageを保存 配下のChatAttachmentも同時に保存できるはず。うまくいかなかったので暫定
		ChatMessage savedChatMessage = chatMessageRepository.save(chatMessage);
		chatMessageDto.setId(savedChatMessage.getId());

		if (chatMessageDto.getChatAttachmentDtoList().size() != 0) {
			chatMessageDto.getChatAttachmentDtoList().forEach((chatAttachmentDto) -> {
				ChatAttachment chatAttachment = new ChatAttachment();
				chatAttachment.setAttachmentUrl(chatAttachmentDto.getAttachmentUrl());
				chatAttachment.setChatMessage(chatMessage);
				chatAttachment.setAttachmentFileName(chatAttachmentDto.getAttachmentFileName());
				savedChatMessage.getChatAttachmentList().add(chatAttachment);
				chatAttachmentRepository.save(chatAttachment);
			});
		}
	}

	@Transactional
	public ChatMessageDto getChatMessageDto(int id) {
		Optional<ChatMessage> optional = chatMessageRepository.findById(id);
		if (optional.isEmpty())
			return null;
		ChatMessage chatMessage = optional.get();
		ChannelMaster channelMaster = channelRepository.findById(chatMessage.getChannelMasterId()).get();
		List<ChatAttachmentDto> chatAttachmentDtoList = new ArrayList<ChatAttachmentDto>();
		for (ChatAttachment chatAttachment : chatMessage.getChatAttachmentList()) {
			ChatAttachmentDto chatAttachmentDto = new ChatAttachmentDto();
			chatAttachmentDto.setAttachmentFileName(chatAttachment.getAttachmentFileName());
			chatAttachmentDto.setAttachmentUrl(chatAttachment.getAttachmentUrl());
			chatAttachmentDtoList.add(chatAttachmentDto);
		}
		ChatMessageDto chatMessageDto = modelMapper.map(chatMessage, ChatMessageDto.class);
		chatMessageDto.setChannelId(channelMaster.getChannelId());
		chatMessageDto.setChannelName(channelMaster.getChannelName());
		chatMessageDto.setChatAttachmentDtoList(chatAttachmentDtoList);

		return chatMessageDto;
	}

	public void sendMessage(ChatMessageDto chatMessageDto) {
		discordBot.sendMessage(chatMessageDto, memberService.getAllianceMemberDtoList());
	}

	@Override
	@Transactional
	public void onMessageUpdate(ChatMessageDto chatMessageDto) {
		Optional<ChatMessage> optional = chatMessageRepository.findById(chatMessageDto.getId());
		if (optional.isEmpty())
			return;
		ChatMessage chatMessage = modelMapper.map(chatMessageDto, ChatMessage.class);
		chatMessageRepository.save(chatMessage);
		chatMessageDto.setId(chatMessage.getId());

		if (chatMessageDto.getChatAttachmentDtoList().size() != 0) {
			chatMessageDto.getChatAttachmentDtoList().forEach((chatAttachmentDto) -> {
				ChatAttachment chatAttachment = new ChatAttachment();
				chatAttachment.setAttachmentUrl(chatAttachmentDto.getAttachmentUrl());
				chatAttachment.setChatMessage(chatMessage);
				chatAttachment.setAttachmentFileName(chatAttachmentDto.getAttachmentFileName());
				chatMessage.getChatAttachmentList().add(chatAttachment);
				chatAttachmentRepository.save(chatAttachment);
			});
		}
	}

	//	public List<ChatMessageDto> getChatMessageDtoList(String channelId) {
	//		PageRequest pageable = PageRequest.of(0, 200);
	//		return getChatMessageDtoList(channelId, pageable);
	//	}


	@Transactional
	public List<ChatMessageDto> getChatMessageDtoList(String channelId, Pageable pageable, String ayarabuName) {
		ChannelMaster channel = channelRepository.findByChannelId(channelId);
		List<ChatMessageDto> chatMessageDtoList = new ArrayList<>();
		Page<ChatMessage> chatMessagePage = chatMessageRepository.findByChannelMasterId(channel.getId(), pageable);
		for (ChatMessage chatMessage : chatMessagePage) {
			List<ChatAttachmentDto> chatAttachmentDtoList = new ArrayList<ChatAttachmentDto>();
			for (ChatAttachment chatAttachment : chatMessage.getChatAttachmentList()) {
				ChatAttachmentDto chatAttachmentDto = new ChatAttachmentDto();
				chatAttachmentDto.setAttachmentFileName(chatAttachment.getAttachmentFileName());
				chatAttachmentDto.setAttachmentUrl(chatAttachment.getAttachmentUrl());
				chatAttachmentDtoList.add(chatAttachmentDto);
			}
			ChatMessageDto chatMessageDto = modelMapper.map(chatMessage, ChatMessageDto.class);
			chatMessageDto.setChannelId(channelId);
			chatMessageDto.setChannelName(channel.getChannelName());
			chatMessageDto.setChatAttachmentDtoList(chatAttachmentDtoList);
			chatMessageDtoList.add(chatMessageDto);
		}
		chatMessageDtoList.sort(Comparator.comparing(ChatMessageDto::getId).reversed());
		if (ayarabuName != null && !ayarabuName.isEmpty()) {
			AllianceMemberDto allianceMemberDto = memberService.getAllianceMemberDtoByAyarabuName(ayarabuName);
			List<ChatMessageView> chatMessageViewList = chatMessageViewRepository
					.findAllByMemberId(allianceMemberDto.getId());
			chatMessageViewList.forEach(chatMessageView -> {
				if (chatMessageView.getChannelId().equals(channelId)) {
					ChatMessageDto firstChatMessageDto = chatMessageDtoList.getFirst();
					chatMessageView.setChatMessageId(firstChatMessageDto.getId());
					chatMessageViewRepository.save(chatMessageView);
				}
			});
		}
		return chatMessageDtoList;
	}

	@Override
	public void onGuildMemberJoin(AllianceMemberDto allianceMemberDto) {
	}

	@Override
	public void onGuildMemberRemove(AllianceMemberDto allianceMemberDto) {
	}

	@Override
	public void onMessageDelete(String messageId) {
		// TODO 自動生成されたメソッド・スタブ

	}

}