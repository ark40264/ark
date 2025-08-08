package bot.controller;

import jakarta.servlet.http.HttpSession; // HttpSession をインポート

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model; // Model をインポート
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import bot.form.LoginForm;

@Controller
public class LoginController {
	Logger log = LoggerFactory.getLogger(LoginController.class);

	@GetMapping(path = "loginForm")
	public String loginForm(Model model, HttpSession session) {
		if (session.getAttribute("errorMessage") != null) {
			model.addAttribute("errorMessage", session.getAttribute("errorMessage"));
			session.removeAttribute("errorMessage");
		}
		return "login";
	}

	@GetMapping(path = "access-denied")
	public String accessDenied() {
		return "access-denied";
	}

	@PostMapping(path = "postLogin")
	public String login(@Validated @ModelAttribute("loginForm") LoginForm loginForm) {
		return "";
	}
}