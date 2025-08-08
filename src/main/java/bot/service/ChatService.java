package bot.service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
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

import bot.dto.AllianceMemberDto;
import bot.dto.ChatAttachmentDto;
import bot.dto.ChatMessageDto;
import bot.entity.ChannelMaster;
import bot.entity.ChatAttachment;
import bot.entity.ChatMessage;
import bot.model.discord.DIscordEventListener;
import bot.repository.ChannelMasterRepository;
import bot.repository.ChatAttachmentRepository;
import bot.repository.ChatMessageRepository;
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

	@Transactional
	public void init() {
		ModelMapper modelMapper = new ModelMapper();
		List<ChannelMaster> channelList = channelRepository.findAll();
		channelList.forEach(channel -> {
			processChannel(channel, modelMapper);
		});
	}
	@Async
	@Transactional // ここでトランザクションを確保
	private void processChannel(ChannelMaster channel, ModelMapper modelMapper) {
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
			chatMessage
					.setCreateDate(message.getTimeCreated().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")));
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
	@Async
	@Transactional
	public void onMessageReceived(ChatMessageDto chatMessageDto) {
		ChannelMaster channel = channelRepository.findByChannelId(chatMessageDto.getChannelId());
		String message = chatMessageDto.getMessage().replace("\n", "<br>");
		chatMessageDto.setMessage(message);
		log.info("ChatService:メッセージ:" + chatMessageDto);

		// DB保存
		ModelMapper modelMapper = new ModelMapper();
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

	public ChatMessageDto getChatMessageDto(int id) {
		Optional<ChatMessage> optional = chatMessageRepository.findById(id);
		if (optional.isEmpty())
		return null;
		ModelMapper modelMapper = new ModelMapper();
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
	public void onMessageUpdate(ChatMessageDto chatMessageDto) {
	}

	public List<ChatMessageDto> getChatMessageDtoList(String channelId) {
		PageRequest pageable = PageRequest.of(0, 200);
		return getChatMessageDtoList(channelId, pageable);
	}

	@Transactional
	public List<ChatMessageDto> getChatMessageDtoList(String channelId, Pageable pageable) {
		ChannelMaster channel = channelRepository.findByChannelId(channelId);
		List<ChatMessageDto> chatMessageDtoList = new ArrayList<>();
		Page<ChatMessage> chatMessagePage = chatMessageRepository.findByChannelMasterId(channel.getId(), pageable);
		ModelMapper modelMapper = new ModelMapper();
		for (ChatMessage chatMessage : chatMessagePage) {
//			Hibernate.initialize(chatMessage.getChatAttachmentList());
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

		return chatMessageDtoList;
	}

	@Override
	public void onMessageDelete(String messageId) {
	}

	@Override
	public void onGuildMemberJoin(AllianceMemberDto allianceMemberDto) {
	}

	@Override
	public void onGuildMemberRemove(AllianceMemberDto allianceMemberDto) {
	}

}