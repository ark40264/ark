package bot;


import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class ArkAuthenticationFailureHandler implements AuthenticationFailureHandler {
	Logger log = LoggerFactory.getLogger(ArkAuthenticationFailureHandler.class);

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {

        String errorMessage;
        log.error("ログインエラー", exception);
        if (exception instanceof BadCredentialsException) {
            errorMessage = "ユーザー名またはパスワードが間違っています。";
        } else if (exception instanceof DisabledException) {
            errorMessage = "このアカウントは無効化されています。";
        } else if (exception instanceof LockedException) {
            errorMessage = "このアカウントはロックされています。";
        } else if (exception instanceof AccountStatusException) {
            errorMessage = "アカウントの状態に問題があります。";
        } else {
            errorMessage = "予期せぬ認証エラーが発生しました。";
        }
        request.getSession().setAttribute("errorMessage", errorMessage);
        response.sendRedirect(request.getContextPath() + "/loginForm?error");
    }
}