package de.yanwittmann.presentation.controller;

import de.yanwittmann.presentation.model.internal.Emotion;
import de.yanwittmann.presentation.model.internal.Session;
import de.yanwittmann.presentation.service.SessionService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping()
public class PageController {

    private final static Logger LOG = LogManager.getLogger(PageController.class);

    @Autowired
    private SessionService sessionService;

    @Value("${application.version}")
    private String appVersion;

    @GetMapping("/session/{sessionId}")
    public String showPageWithVariable(@PathVariable String sessionId, Model model) {
        try {
            final Session session = sessionService.findSessionByName(sessionId);
            if (session == null) {
                LOG.error("Session with id [{}] not found", sessionId);
                return "login";
            }
            model.addAttribute("sessionId", sessionId);
            model.addAttribute("availableEmoticons", Emotion.ALL_EMOTIONS.toString());
            model.addAttribute("initialTimerValue", session.getTimerTargetDate());
            model.addAttribute("appVersion", appVersion);
        } catch (Exception e) {
            LOG.error("Invalid session id [{}]", sessionId, e);
            return "login";
        }
        return "session";
    }

    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    @GetMapping("/")
    public String showLoginPageFallback() {
        return "login";
    }

    @GetMapping("/manager")
    public String showManagerPage(Model model) {
        model.addAttribute("appVersion", appVersion);
        return "manager";
    }
}
