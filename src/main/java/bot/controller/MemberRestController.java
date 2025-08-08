package bot.controller;

import java.util.Date;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import bot.ArkApplication;
import bot.dto.AllianceMemberDto;
import bot.form.AllianceMemberForm;
import bot.service.MemberService;

@RestController
@RequestMapping(value = "/member", produces = MediaType.APPLICATION_JSON_VALUE)
public class MemberRestController {
	Logger log = LoggerFactory.getLogger(MemberRestController.class);
	@Autowired
	private MemberService memberService;

	@GetMapping
	public List<AllianceMemberDto> getAllMember() {
		return memberService.getAllianceMemberDtoList();
	}

	@PostMapping
	public void postMember(@RequestBody AllianceMemberForm allianceMemberForm) {
		ModelMapper modelMapper = new ModelMapper();
		AllianceMemberDto allianceMemberDto = modelMapper.map(allianceMemberForm, AllianceMemberDto.class);
		allianceMemberDto.setId(null);
		allianceMemberDto.setCreateDate(ArkApplication.sdf.format(new Date()));
		memberService.addAllianceMemberDto(allianceMemberDto);
	}
	
	@PutMapping
	public void putMember(@RequestBody AllianceMemberForm allianceMemberForm) {
		ModelMapper modelMapper = new ModelMapper();
		memberService.updateAllianceMemberDto(modelMapper.map(allianceMemberForm, AllianceMemberDto.class));
	}

	@DeleteMapping("/{id}")
	public void deleteMember(@PathVariable Integer id) {
		memberService.removeAllianceMemberDto(id);
	}

}
