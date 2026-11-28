package io.github.randybauer.cuckoosnest.web;

import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** The login page and the signed-in home page. */
@Controller
public class PageController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String home(Principal principal, Model model) {
        model.addAttribute("username", principal.getName());
        return "home";
    }
}
