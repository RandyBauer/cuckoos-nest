package io.github.randybauer.cuckoosnest.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** The login page. The signed-in pages are in {@link DashboardController}. */
@Controller
public class PageController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
