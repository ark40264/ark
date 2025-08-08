package bot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import bot.dto.MemberRole;
import bot.service.LoginUserDetailsService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
	// ★修正点1: LoginUserDetailsService の @Autowired フィールドを削除します
	@Autowired
	private LoginUserDetailsService loginUserDetailsService;

	@Autowired
	private ArkAuthenticationFailureHandler botAuthenticationFailureHandler;
	@Autowired
	private ArkAccessDeniedHandler botAccessDeniedHandler;

	private final AuthenticationConfiguration authenticationConfiguration;

	public SecurityConfig(AuthenticationConfiguration authenticationConfiguration) {
		this.authenticationConfiguration = authenticationConfiguration;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		String LEADER = MemberRole.LEADER.toString();
		String SUB_LEADER = MemberRole.SUB_LEADER.toString();
		http
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/","/h2-console/**", "/css/**", "/js/**", "/img/**").permitAll()
						.requestMatchers("/memberHtml/**")
						.hasAnyAuthority(LEADER, SUB_LEADER)
						.anyRequest().authenticated())
				.exceptionHandling(exceptions -> exceptions
						.accessDeniedHandler(botAccessDeniedHandler))
				.formLogin(form -> form
						.loginProcessingUrl("/postLogin")
						.loginPage("/loginForm")
						.failureHandler(botAuthenticationFailureHandler)
						.usernameParameter("ayarabuName").passwordParameter("ayarabuId"))
				.csrf(csrf -> csrf
		                // H2コンソールへのパスに対してCSRF保護を無効にする
		                .ignoringRequestMatchers("/h2-console/**")
		            )
				.rememberMe(rememberMe -> rememberMe
						.key("secret_key")
						.rememberMeServices(
								new TokenBasedRememberMeServices("secret_key", loginUserDetailsService))
						.rememberMeParameter("remember-me")
						.tokenValiditySeconds(60 * 60 * 24 * 700))
				.logout(logout -> logout
						.logoutRequestMatcher(PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/logout"))
		                .logoutSuccessUrl("/loginForm") // ログアウト成功後のリダイレクト先
		                .deleteCookies("JSESSIONID")
		                .permitAll()
		            );
		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new NoEncodingPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager() throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}
}