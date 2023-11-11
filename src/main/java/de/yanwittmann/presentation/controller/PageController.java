package de.yanwittmann.presentation.controller;

import de.yanwittmann.presentation.model.internal.Emotion;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping()
public class PageController {

    @GetMapping("/session/{sessionId}")
    public String showPageWithVariable(@PathVariable String sessionId, Model model) {
        model.addAttribute("sessionId", sessionId);
        model.addAttribute("availableEmoticons", Emotion.ALL_EMOTIONS.toString());
        return "session";
    }

    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    @GetMapping("/manager")
    public String showManagerPage() {
        return "manager";
    }
}
