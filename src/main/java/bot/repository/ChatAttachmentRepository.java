package bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import bot.entity.ChatAttachment;

@Repository
public interface ChatAttachmentRepository extends JpaRepository<ChatAttachment, Integer>{
}
