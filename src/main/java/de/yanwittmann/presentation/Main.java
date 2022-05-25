package de.yanwittmann.presentation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Properties;

public class Main {

    private final static Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        logVersion();

        // get the -ws and -hs parameters
        int webSocketPort = 8081;
        int httpServerPort = 8080;
        String password = null;
        String httpServerContext = "/";
        for (String arg : args) {
            if (arg.contains("=") && arg.length() > 2) {
                String key = arg.substring(1, arg.indexOf('='));
                String value = arg.substring(arg.indexOf('=') + 1);
                switch (key) {
                    case "ws":
                    case "webSocketPort":
                        webSocketPort = Integer.parseInt(value);
                        break;
                    case "hs":
                    case "httpPort":
                        httpServerPort = Integer.parseInt(value);
                        break;
                    case "pw":
                    case "password":
                        password = value;
                        break;
                    case "ctx":
                    case "path":
                    case "context":
                        httpServerContext = value;
                        if (!httpServerContext.startsWith("/")) {
                            httpServerContext = "/" + httpServerContext;
                        }
                        break;
                    default:
                        LOG.warn("Unknown parameter: {}", arg);
                }
            }
        }


        final Manager manager;
        if (password != null) {
            manager = new Manager(webSocketPort, httpServerPort, password);
        } else {
            manager = new Manager(webSocketPort, httpServerPort);
        }
        manager.startupServer(httpServerContext);

        Thread.sleep(2000);
        LOG.info("Commands available: [exit, password, ports]");

        // read user input
        while (true) {
            BufferedInputStream in = new BufferedInputStream(System.in);
            byte[] buffer = new byte[1024];
            int bytesRead = in.read(buffer);
            if (bytesRead == -1) {
                break;
            }
            String input = new String(buffer, 0, bytesRead).replace("|", "").trim();

            switch (input) {
                case "exit":
                case "quit":
                    manager.shutdownServer();
                    return;
                case "password":
                case "passwords":
                    LOG.info("Admin password is [{}]", manager.getAdminPassword());
                    break;
                case "ports":
                case "port":
                    LOG.info("WebSocket port is [{}]", webSocketPort);
                    LOG.info("HTTP port is [{}]", httpServerPort);
                    break;
                default:
                    LOG.info("Unknown command [{}]", input);
                    break;
            }
        }

    }

    private static void logVersion() {
        try {
            Properties props = new Properties();
            props.load(Main.class.getClassLoader().getResourceAsStream("project.properties"));
            LOG.info("Presentation Live Feedback version is [{}]", props.getProperty("application.version"));
        } catch (IOException e) {
            LOG.error("Could not load project.properties", e);
        }
    }
}
