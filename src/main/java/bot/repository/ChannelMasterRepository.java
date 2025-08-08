package bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import bot.entity.ChannelMaster;

@Repository
public interface  ChannelMasterRepository extends JpaRepository<ChannelMaster, Integer>{
	public ChannelMaster findByChannelId(String channelId);
}
