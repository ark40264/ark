package ark;

import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class ArkAccessDeniedHandler implements AccessDeniedHandler {
	Logger log = LoggerFactory.getLogger(ArkAccessDeniedHandler.class);

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException, ServletException {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String username = "unknown";
		String authorities = "none";

		if (authentication != null) {
			username = authentication.getName(); // ユーザー名を取得

			// 2. ユーザーが持っている権限（ロール）を取得
			Collection<? extends GrantedAuthority> grantedAuthorities = authentication.getAuthorities();

			// 権限を文字列に変換して表示用に整形
			authorities = grantedAuthorities.stream()
					.map(GrantedAuthority::getAuthority)
					.collect(Collectors.joining(", "));
		}

		log.error("権限エラー ユーザ=" + username +
				" 権限=" + authorities +
				", Attempted URL: " + request.getRequestURI(), accessDeniedException);

		if (!response.isCommitted()) {
			if (isAjaxRequest(request)) {
				// AJAXリクエストの場合
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				response.setContentType("application/json;charset=UTF-8");
				response.getWriter().write("{\"error\":\"Access Denied\",\"message\":\"このリソースへのアクセス権限がありません。\"}");
			} else {
				// 通常のWebリクエストの場合
				response.sendRedirect(request.getContextPath() + "/access-denied");
			}
		}
	}

	// AJAXリクエストかどうかの簡易判定
	private boolean isAjaxRequest(HttpServletRequest request) {
		String xRequestedWith = request.getHeader("X-Requested-With");
		return "XMLHttpRequest".equals(xRequestedWith);
	}
}
