package bot.model.init;

import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import bot.model.discord.DiscordModel;
import bot.service.ChatService;
import bot.service.MemberService;
import bot.util.discord.DiscordBot;

@Component
public class StartupRunner implements CommandLineRunner {
	private static final Logger log = LoggerFactory.getLogger(StartupRunner.class);
	@Autowired
	private ChatService chatService;
	@Autowired
	private DiscordBot discordBot;
	@Autowired
	public DiscordModel discordModel;
	@Autowired
	private MemberService memberService;

	@Override
	public void run(String... args) throws Exception {
		try {
			discordModel.addDiscordEventListener(chatService);
			discordModel.addDiscordEventListener(memberService);
			discordBot.init(discordModel);
			discordModel.initDiscordMember();
			chatService.init();
			Thread.sleep(5000);
			discordModel.getHistory(100);

		} catch (Exception e) {
			log.error("初期起動に失敗しました。終了します。", e);
			System.exit(-1);
		}
		log.info("初期起動完了");
	}
	@PreDestroy
    public void cleanup() {
		discordModel.removeDiscordEventListener(chatService);
		discordModel.removeDiscordEventListener(memberService);
		discordBot.shutDown();
		log.info("終了完了");
    }
}
