package de.yanwittmann.presentation.websocket;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.yanwittmann.presentation.Main;
import de.yanwittmann.presentation.Manager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class WebsiteHttpHandler implements HttpHandler {

    private final static Logger LOG = LoggerFactory.getLogger(WebsiteHttpHandler.class);

    private final Manager manager;

    public WebsiteHttpHandler(Manager manager) {
        this.manager = manager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        LOG.info("Handling HTTP exchange for [{}]", exchange.getRemoteAddress());

        exchange.getResponseHeaders().set("Content-Type", "text/html");
        exchange.getResponseHeaders().set("WebSocket-Address", manager.getWebSocketPort() + "/events"); // e.g. ws://localhost:8080/events

        byte[] responseBytes = readResource("index.html").getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(200, responseBytes.length);
        OutputStream out = exchange.getResponseBody();

        try (ByteArrayInputStream bis = new ByteArrayInputStream(responseBytes)) {
            byte[] buffer = new byte[1024];
            int count;
            while ((count = bis.read(buffer)) != -1) {
                out.write(buffer, 0, count);
            }
            out.close();
        } catch (IOException e) {
            LOG.error("Could not write response", e);
            exchange.sendResponseHeaders(500, 0);
        }

        out.close();
    }

    private static String readResource(String resource) {
        InputStream inputStream = Main.class.getClassLoader().getResourceAsStream(resource);
        StringBuilder result = new StringBuilder();
        try {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.append(new String(buffer, 0, length));
            }
        } catch (IOException e) {
            LOG.error("Could not read resource {}", resource, e);
        }
        return result.toString();
    }
}
