//
// ========================================================================
// Copyright (c) Mort Bay Consulting Pty Ltd and others.
//
// This program and the accompanying materials are made available under the
// terms of the Eclipse Public License v. 2.0 which is available at
// https://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
// which is available at https://www.apache.org/licenses/LICENSE-2.0.
//
// SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
// ========================================================================
//

package de.yanwittmann.presentation.websocket;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EventServer {

    private final static Logger LOG = LoggerFactory.getLogger(EventSocket.class);

    private final Server server;
    private final ServerConnector connector;

    public EventServer() {
        server = new Server();
        connector = new ServerConnector(server);
        server.addConnector(connector);

        // Setup the basic application "context" for this application at "/"
        // This is also known as the handler tree (in jetty speak)
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // Configure specific websocket behavior
        JettyWebSocketServletContainerInitializer.configure(context, (servletContext, wsContainer) ->
        {
            // Configure default max size
            wsContainer.setMaxTextMessageSize(65535);
            // set timeout
            wsContainer.setIdleTimeout(Duration.ofHours(3));

            // Add websockets
            wsContainer.addMapping("/events/*", EventSocket.class);
        });
    }

    public void setPort(int port) {
        connector.setPort(port);
    }

    public void start() throws Exception {
        server.start();
    }

    public URI getURI() {
        return server.getURI();
    }

    public void stop() throws Exception {
        server.stop();
    }

    public void join() throws InterruptedException {
        System.out.println("Use Ctrl+C to stop server");
        server.join();
    }

    public void broadcast(String message) {
        LOG.info("Broadcasting message: {}", message);
        List<SocketAddress> toBeRemoved = new ArrayList<>(0);

        for (Map.Entry<SocketAddress, Session> entry : EventSocket.SESSIONS.entrySet()) {
            try {
                entry.getValue().getRemote().sendString(message);
            } catch (IOException e) {
                LOG.warn("Error sending message, removing session from socket list; {}", e.getMessage());
                toBeRemoved.add(entry.getKey());
            }
        }

        toBeRemoved.forEach(EventSocket.SESSIONS::remove);
    }
}
