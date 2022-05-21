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

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class EventSocket extends WebSocketAdapter {

    private final static Logger LOG = LoggerFactory.getLogger(EventSocket.class);

    public final static Map<SocketAddress, Session> SESSIONS = new HashMap<>();
    private final CountDownLatch closureLatch = new CountDownLatch(1);

    private final static Set<UserConnectionListener> userConnectionListeners = new HashSet<>();
    private final static Set<UserMessageListener> userMessageListeners = new HashSet<>();

    @Override
    public void onWebSocketConnect(Session sess) {
        super.onWebSocketConnect(sess);
        LOG.info("Client connected: {}", sess.getRemoteAddress());
        SESSIONS.put(sess.getRemoteAddress(), sess);
        try {
            sess.getRemote().sendString("{\"to\":\"ALL_USERS\",\"content\":\"Connected to " + sess.getLocalAddress() + "\"}");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onWebSocketText(String message) {
        super.onWebSocketText(message);
        LOG.info("Received TEXT message: {}", message);
        userMessageListeners.forEach(listener -> listener.onUserMessage(message));
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        super.onWebSocketClose(statusCode, reason);
        LOG.info("Socket Closed: [{}] {}", statusCode, reason);
        closureLatch.countDown();
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        super.onWebSocketError(cause);
        cause.printStackTrace(System.err);
    }

    public void awaitClosure() throws InterruptedException {
        LOG.info("Awaiting socket closure");
        closureLatch.await();
    }

    public static boolean addUserConnectionListener(UserConnectionListener listener) {
        return userConnectionListeners.add(listener);
    }

    public static boolean removeUserConnectionListener(UserConnectionListener listener) {
        return userConnectionListeners.remove(listener);
    }

    public static boolean addUserMessageListener(UserMessageListener listener) {
        return userMessageListeners.add(listener);
    }

    public static boolean removeUserMessageListener(UserMessageListener listener) {
        return userMessageListeners.remove(listener);
    }

    public interface UserConnectionListener {
        void onUserConnected(Session session);
        void onUserDisconnected(Session session);
    }

    public interface UserMessageListener {
        void onUserMessage(String message);
    }
}
