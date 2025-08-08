package bot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import bot.entity.AllianceMember;
import bot.repository.AllianceMemberRepository;

@Service
public class LoginUserDetailsService implements UserDetailsService {
	Logger log = LoggerFactory.getLogger(LoginUserDetailsService.class);
	@Autowired
	private AllianceMemberRepository allianceMemberRepository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		log.error("✩✩ username=" + username);
		if (username == null || username.isEmpty()) {
			throw new UsernameNotFoundException("ユーザー名が空です。");
		}

		AllianceMember allianceMember = allianceMemberRepository.findByAyarabuName(username);
		if (allianceMember == null) {
			throw new UsernameNotFoundException("アカウントが見つかりませんでした。");
		}
		return new User(allianceMember.getAyarabuName(), allianceMember.getAyarabuId(),
				AuthorityUtils.createAuthorityList(allianceMember.getMemberRole()));
	}

}
