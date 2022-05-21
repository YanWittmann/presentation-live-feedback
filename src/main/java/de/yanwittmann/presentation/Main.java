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
        int ws = 8080;
        int hs = 8000;
        for (String arg : args) {
            if (arg.startsWith("-ws=")) {
                ws = Integer.parseInt(arg.substring(4));
            } else if (arg.startsWith("-hs=")) {
                hs = Integer.parseInt(arg.substring(4));
            }
        }


        Manager manager = new Manager(ws, hs);
        manager.startupServer();

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
                    LOG.info("WebSocket port is [{}]", ws);
                    LOG.info("HTTP port is [{}]", hs);
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
