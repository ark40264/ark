package bot.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import bot.ArkApplication;
import bot.dto.ChatAttachmentDto;
import bot.dto.ChatMessageDto;
import bot.dto.MessageSizeDto;
import bot.form.MessageForm;
import bot.service.ChatService;

@RestController
@RequestMapping(value = "/chat", produces = MediaType.APPLICATION_JSON_VALUE)
public class ChatRestController {
	private static final Logger log = LoggerFactory.getLogger(ChatRestController.class);
	@Autowired
	private ChatService chatService;

	@GetMapping("/pageable")
	public List<ChatMessageDto> getAllChatsPageable(@RequestParam String channelId,
			@PageableDefault(size = 20) Pageable pageable) {
		return chatService.getChatMessageDtoList(channelId, pageable);
	}

	@GetMapping("/size")
	public MessageSizeDto getSize(@RequestParam String channelId) {
		MessageSizeDto messageSizeDto = new MessageSizeDto();
		messageSizeDto.setSize(chatService.getChatMessageDtoList(channelId).size());
		return messageSizeDto;
	}

	@PostMapping
	public void postMessage(MessageForm messageForm) {
		try {
			log.info("messageForm=" + messageForm);
			chatService.sendMessage(toChatMessageDtoFromForm(messageForm));
		} catch (Exception e) {
			log.error("メッセージの送信で失敗しました。messageForm=" + messageForm, e);
		}
	}

	private ChatMessageDto toChatMessageDtoFromForm(MessageForm messageForm) {
		ChatMessageDto chatMessageDto = new ChatMessageDto();
		chatMessageDto.setCreateDate(ArkApplication.sdf.format(new Date()));
		chatMessageDto.setDiscordMessageId(null);
		chatMessageDto.setChannelId(messageForm.getChannelId());
		chatMessageDto.setMessage(messageForm.getMessage());
		chatMessageDto.setName(messageForm.getName());
		if (messageForm.getReferencedMessageId() != null && !messageForm.getReferencedMessageId().isEmpty()) {
			ChatMessageDto refChatMessageDto = chatService
					.getChatMessageDto(Integer.parseInt(messageForm.getReferencedMessageId().trim()));
			chatMessageDto.setQuoteDiscordId(refChatMessageDto.getDiscordMessageId());
			chatMessageDto.setQuoteId(refChatMessageDto.getId().toString());
		}

		List<ChatAttachmentDto> chatAttachmentDtoList = new ArrayList<ChatAttachmentDto>();
		if (messageForm.getMultipartFiles() != null) {
			for (int i = 0; i < messageForm.getMultipartFiles().length; i++) {
				MultipartFile multipartFile = messageForm.getMultipartFiles()[i];
				ChatAttachmentDto chatAttachmentDto = new ChatAttachmentDto();
				try {
					chatAttachmentDto.setAttachmentFileInputStream(multipartFile.getInputStream());
					chatAttachmentDto.setAttachmentFileName(multipartFile.getOriginalFilename());
					chatAttachmentDto.setAttachmentUrl(null);
					chatAttachmentDtoList.add(chatAttachmentDto);
				} catch (IOException e) {
					log.error("添付ファイルの処理に失敗しました。", e);
				}

			}
		}
		chatMessageDto.setChatAttachmentDtoList(chatAttachmentDtoList);

		return chatMessageDto;
	}
}
