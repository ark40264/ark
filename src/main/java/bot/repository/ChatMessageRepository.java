package bot.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import bot.entity.ChatMessage;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Integer>{
	public ChatMessage findByDiscordMessageId(String discordMessageId);
	public List<ChatMessage> findAllByOrderByIdDesc();
	public Page<ChatMessage> findByChannelMasterId(Integer channelId, Pageable pageable);
}
