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
import bot.model.discord.DIscordEventListener;
import bot.repository.AllianceMemberRepository;

@Service
public class MemberService implements DIscordEventListener {
	Logger log = LoggerFactory.getLogger(MemberService.class);
	@Autowired
	private AllianceMemberRepository allianceMemberRepository;
	private ModelMapper modelMapper;

	@Transactional
	public void removeAllianceMemberDto(Integer id) {
		allianceMemberRepository.deleteById(id);
		log.info("メンバー削除:" + id);
	}

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
		if (allianceMember == null)
			return null;
		ModelMapper modelMapper = new ModelMapper();
		return modelMapper.map(allianceMember, AllianceMemberDto.class);
	}

	@Transactional
	public void addOrChangeAllianceMemberDto(AllianceMemberDto allianceMemberDto) {
		AllianceMember allianceMember;

		allianceMember = allianceMemberRepository.findByAyarabuName(allianceMemberDto.getAyarabuName());
		if (allianceMember == null) {
			String discordMemberId = allianceMemberDto.getDiscordMemberId();
			if (discordMemberId==null || discordMemberId.equals("")) {
				allianceMember = null;
			} else {
				allianceMember = allianceMemberRepository.findByDiscordMemberId(allianceMemberDto.getDiscordMemberId());
			}
			
			if (allianceMember == null) {
				allianceMember = toEntityFromDto(allianceMemberDto);
				allianceMember = allianceMemberRepository.save(allianceMember);
				log.info("メンバー追加:" + allianceMemberDto);
			} else {
				int id = allianceMember.getId();
				allianceMember = toEntityFromDto(allianceMemberDto);
				allianceMember.setId(id);
				allianceMember = allianceMemberRepository.save(allianceMember);
				log.info("メンバー更新1:" + allianceMemberDto);
			}
		} else {
			int id = allianceMember.getId();
			allianceMember = toEntityFromDto(allianceMemberDto);
			allianceMember.setId(id);
			allianceMember = allianceMemberRepository.save(allianceMember);
			log.info("メンバー更新2:" + allianceMemberDto);
		}
	}

	@Transactional
	public void removeAllianceMemberDtoByDiscordId(AllianceMemberDto allianceMemberDto) {
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
		return modelMapper.map(allianceMember, AllianceMemberDto.class);
	}

	public AllianceMemberDto getAllianceMemberDtoByMemberId(Integer memberId) {
		AllianceMember allianceMember = allianceMemberRepository.findById(memberId).get();
		return modelMapper.map(allianceMember, AllianceMemberDto.class);
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
