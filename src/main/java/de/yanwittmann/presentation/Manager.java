package de.yanwittmann.presentation;

import com.sun.net.httpserver.HttpServer;
import de.yanwittmann.presentation.data.User;
import de.yanwittmann.presentation.websocket.EventServer;
import de.yanwittmann.presentation.websocket.EventSocket;
import de.yanwittmann.presentation.websocket.WebsiteHttpHandler;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Manager {

    private final static Logger LOG = LoggerFactory.getLogger(Manager.class);

    private final EventServer webSocketServer = new EventServer();
    private HttpServer server;

    private final List<User> users = new ArrayList<>();

    public void startupServer(int webSocketPort, int httpPort) {
        EventSocket.addUserMessageListener(this::userMessage);

        new Thread(() -> {
            try {
                webSocketServer.setPort(webSocketPort);
                webSocketServer.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();

        try {
            server = HttpServer.create(new InetSocketAddress(httpPort), 0);
            server.createContext("/presentation", new WebsiteHttpHandler(this));
            server.setExecutor(null);
            server.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void userMessage(Session session, String message) {
        LOG.info("User message {} from [{}]", message, session.getRemoteAddress());
    }


}
