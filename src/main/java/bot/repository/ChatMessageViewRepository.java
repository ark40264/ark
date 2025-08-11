package bot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import bot.entity.ChatMessageView;

@Repository
public interface ChatMessageViewRepository extends JpaRepository<ChatMessageView, Integer>{
	public List<ChatMessageView> findAllByMemberId(int memberId);
}
