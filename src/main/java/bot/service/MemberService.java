package bot.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.transaction.Transactional;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import bot.ArkApplication;
import bot.dto.AllianceMemberDto;
import bot.dto.ChatMessageDto;
import bot.dto.MemberAlliance;
import bot.dto.MemberRole;
import bot.entity.AllianceMember;
import bot.entity.ChannelMaster;
import bot.entity.ChatMessageView;
import bot.model.discord.DIscordEventListener;
import bot.repository.AllianceMemberRepository;
import bot.repository.ChannelMasterRepository;
import bot.repository.ChatMessageViewRepository;

@Service
public class MemberService implements DIscordEventListener {
	Logger log = LoggerFactory.getLogger(MemberService.class);
	@Autowired
	private AllianceMemberRepository allianceMemberRepository;
	@Autowired
	private ChatMessageViewRepository chatMessageViewRepository;
	@Autowired
	private ChannelMasterRepository channelMasterRepository;
	private ModelMapper modelMapper;

	@Transactional
	public void removeAllianceMemberDto(Integer id) {
		allianceMemberRepository.deleteById(id);
		log.info("メンバー削除:" + id);
	}

	/**
     * 指定されたユーザー情報（名前・ID・Botフラグ）に基づいて、
     * ユーザー情報の初期化処理を行う。
     * 
     * DB上に該当ユーザー（discordId）が存在しない場合:
     * デフォルトの {@link AllianceMemberDto} を新規作成し、DBおよびメモリ上のリストに追加する。
     * このとき、isBotがtrueであれば管理者（LEADER）権限を自動付与する。
     *
     * DB上に既に存在する場合:
     * DBの情報をDTOへ変換し、メモリ上のリストに反映（更新）する。
     * この処理により、最新のDB情報が同期される。
     *
     * 本メソッドは、Bot起動時や初回参加時などに呼ばれ、
     * ユーザーの存在確認および初期登録・更新を担う。
     * 
	 * @param name			Discord上の表示名
	 * @param discordId	DiscordユーザーID
	 * @param isBot		Botかどうかを示すフラグ（true の場合、リーダー権限を付与）
	 */
	@Async
	@Transactional
	public void init(String name, String discordId, boolean isBot) {
		AllianceMemberDto allianceMemberDto = new AllianceMemberDto();
		AllianceMember allianceMember = allianceMemberRepository.findByDiscordMemberId(discordId);
		if (allianceMember == null) {
			allianceMemberDto = getDefault(name, discordId, isBot);
			addOrChangeAllianceMemberDto(allianceMemberDto);
		} else {
			allianceMemberDto = toDtoFromEntity(allianceMember);
			addOrChangeAllianceMemberDto(allianceMemberDto);
		}
	}
	private AllianceMemberDto getDefault(String name, String discordId, boolean isBot) {
		AllianceMemberDto allianceMemberDto = new AllianceMemberDto();
		if (isBot) {
			allianceMemberDto.setMemberRole(MemberRole.LEADER);
			allianceMemberDto.setAyarabuId("mitsu");
			allianceMemberDto.setAyarabuName(name);
		} else {
			allianceMemberDto.setMemberRole(MemberRole.MEMBER);
			allianceMemberDto.setAyarabuId("mitsu");
			allianceMemberDto.setAyarabuName(name);
		}
		allianceMemberDto.setBot(false);
		allianceMemberDto.setCreateDate(ArkApplication.sdf.format(new Date()));
		allianceMemberDto.setStatementCount(0);
		allianceMemberDto.setAlliance(MemberAlliance.NONE);
		allianceMemberDto.setDiscordMemberId(discordId);
		allianceMemberDto.setDiscordName(name);
		allianceMemberDto.setBot(isBot);
		return allianceMemberDto;
	}

	public List<AllianceMemberDto> getAllianceMemberDtoList() {
		List<AllianceMemberDto> allianceMemberDtoList = new ArrayList<AllianceMemberDto>();
		List<AllianceMember> allianceMemberList = allianceMemberRepository.findAll();
		allianceMemberList.forEach(allianceMember->{
			allianceMemberDtoList.add(toDtoFromEntity(allianceMember));
		});
		return allianceMemberDtoList;
	}

	private AllianceMember toEntityFromDto(AllianceMemberDto allianceMemberDto) {
		ModelMapper modelMapper = new ModelMapper();
		//		modelMapper.addConverter(new StringToMemberRoleConverter());
		return modelMapper.map(allianceMemberDto, AllianceMember.class);
	}

	private AllianceMemberDto toDtoFromEntity(AllianceMember allianceMember) {
		if (allianceMember == null) {
			return null;
		}
		ModelMapper modelMapper = new ModelMapper();
		return modelMapper.map(allianceMember, AllianceMemberDto.class);
	}

	@Transactional
	public synchronized void addOrChangeAllianceMemberDto(AllianceMemberDto allianceMemberDto) {
		// TODO 起動時DBにいてdiscoにいない場合は消す？

		AllianceMember allianceMember = allianceMemberRepository.findByDiscordMemberId(allianceMemberDto.getDiscordMemberId());
		if (allianceMember == null) {
			allianceMember = allianceMemberRepository.findByAyarabuName(allianceMemberDto.getAyarabuName());
			if (allianceMember == null) {
				allianceMember = toEntityFromDto(allianceMemberDto);
				allianceMember = allianceMemberRepository.save(allianceMember);
				log.info("メンバー追加:" + allianceMember);

				List<ChannelMaster> channelList = channelMasterRepository.findAll();
				for (ChannelMaster channelMaster : channelList) {
					ChatMessageView chatMessageView = new ChatMessageView();
					chatMessageView.setChannelId(channelMaster.getChannelId());
					chatMessageView.setChatMessageId(0);
					chatMessageView.setMemberId(allianceMember.getId());
					chatMessageViewRepository.save(chatMessageView);
				}

			} else {
				int id = allianceMember.getId();
				allianceMember = toEntityFromDto(allianceMemberDto);
				allianceMember.setId(id);
				allianceMember = allianceMemberRepository.save(allianceMember);
				log.info("メンバー更新:" + allianceMemberDto);
			}
		} else {
			int id = allianceMember.getId();
			allianceMember = toEntityFromDto(allianceMemberDto);
			allianceMember.setId(id);
			allianceMember = allianceMemberRepository.save(allianceMember);
			log.info("メンバー更新:" + allianceMemberDto);
		}
	}

	@Transactional
	public void removeAllianceMemberDtoByDiscordId(AllianceMemberDto allianceMemberDto) {
		AllianceMemberDto removeDto = getAllianceMemberDto(allianceMemberDto.getDiscordMemberId());
		AllianceMember allianceMember = allianceMemberRepository.findByDiscordMemberId(
				allianceMemberDto.getDiscordMemberId());

		allianceMemberRepository.deleteById(allianceMember.getId());
		log.info("メンバー削除:" + allianceMemberDto);
	}

	public AllianceMemberDto getAllianceMemberDto(String discordMemberId) {
		return toDtoFromEntity(allianceMemberRepository.findByDiscordMemberId(discordMemberId));
	}

	public MemberService() {
		modelMapper = new ModelMapper();
	}

	public void addAllianceMemberDto(AllianceMemberDto AllianceMemberDto) {
		addOrChangeAllianceMemberDto(AllianceMemberDto);
	}

	@Transactional
	public void updateAllianceMemberDto(AllianceMemberDto AllianceMemberDto) {
		addOrChangeAllianceMemberDto(AllianceMemberDto);
	}

	public AllianceMemberDto getAllianceMemberDtoByAyarabuName(String ayarabuName) {
		AllianceMember allianceMember = allianceMemberRepository.findByAyarabuName(ayarabuName);
		AllianceMemberDto allianceMemberDto = modelMapper.map(allianceMember, AllianceMemberDto.class);
		List<ChatMessageView> chatMessageViewList = chatMessageViewRepository.findAllByMemberId(allianceMember.getId());
		allianceMemberDto.setChatMessageViewList(chatMessageViewList);
		return allianceMemberDto;
	}

	public AllianceMemberDto getAllianceMemberDtoByMemberId(Integer memberId) {
		AllianceMember allianceMember = allianceMemberRepository.findById(memberId).get();
		AllianceMemberDto allianceMemberDto = modelMapper.map(allianceMember, AllianceMemberDto.class);
		List<ChatMessageView> chatMessageViewList = chatMessageViewRepository.findAllByMemberId(allianceMember.getId());
		allianceMemberDto.setChatMessageViewList(chatMessageViewList);
		return allianceMemberDto;
	}

	@Override
	public void onGuildMemberJoin(AllianceMemberDto allianceMemberDto) {
		if (allianceMemberDto == null)
			return;
		addOrChangeAllianceMemberDto(allianceMemberDto);
	}

	@Override
	public void onGuildMemberRemove(AllianceMemberDto allianceMemberDto) {
		// TODO discord抜けたら脱退扱いでいいか？
		removeAllianceMemberDtoByDiscordId(allianceMemberDto);
	}

	@Override
	public void onMessageReceived(ChatMessageDto chatMessageDto) {
	}

	@Override
	public void onMessageUpdate(ChatMessageDto chatMessageDto) {
	}

	@Override
	public void onMessageDelete(String messageId) {
	}

}
