package de.yanwittmann.presentation.websocket;

import de.yanwittmann.presentation.service.SessionService;
import de.yanwittmann.presentation.service.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final static Logger LOG = LogManager.getLogger(WebSocketConfig.class);

    private final UserService userService;
    private final SessionService sessionService;

    public WebSocketConfig(UserService userService, SessionService sessionService) {
        this.userService = userService;
        this.sessionService = sessionService;
        LOG.info("WebSocketConfig initialized with user service {} and session service {}", userService, sessionService);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new WebSocketHandler(userService, sessionService), "/ws");
    }
}
