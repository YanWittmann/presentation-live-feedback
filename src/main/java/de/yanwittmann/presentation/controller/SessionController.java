package de.yanwittmann.presentation.controller;

import de.yanwittmann.presentation.model.in.*;
import de.yanwittmann.presentation.model.internal.Session;
import de.yanwittmann.presentation.model.internal.SessionParticipant;
import de.yanwittmann.presentation.model.out.OutFullSession;
import de.yanwittmann.presentation.model.out.OutOtherSessionParticipant;
import de.yanwittmann.presentation.model.out.OutSelfSessionParticipant;
import de.yanwittmann.presentation.service.SessionService;
import de.yanwittmann.presentation.service.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/session")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class SessionController {

    private final static Logger LOG = LogManager.getLogger(SessionController.class);

    @Autowired
    private SessionService sessionService;

    @Autowired
    private UserService userService;

    @PostMapping("/join")
    public OutSelfSessionParticipant userJoinSession(@RequestBody InJoinSession requestUser) {
        LOG.info("User [{}] with computer id [{}] wants to join session [{}] with password [{}]",
                requestUser.getUsername(), requestUser.getUniqueComputerId(), requestUser.getSessionName(), requestUser.getSessionPassword() == null);
        final Session session = sessionService.findSessionByName(requestUser.getSessionName());
        if (session == null) {
            throw new RuntimeException("Session not found");
        }

        session.assertPasswordCorrect(requestUser.getSessionPassword());

        final SessionParticipant foundUser = userService.findUserByNameAndComputerId(requestUser.getUsername(), requestUser.getUniqueComputerId());
        final SessionParticipant effectiveUser;
        if (foundUser == null) {
            final SessionParticipant newUser = new SessionParticipant(requestUser.getUsername(), requestUser.getUniqueComputerId());
            userService.registerParticipant(newUser);
            effectiveUser = newUser;
        } else {
            effectiveUser = foundUser;
        }

        effectiveUser.getSessionState(session).setSpectator(requestUser.isSpectator());
        session.addParticipant(effectiveUser);
        notifySessionChanged(session);

        LOG.info("User {} [aka {}] with computer id [{}] joined session {}",
                effectiveUser, effectiveUser.getReferenceId(), effectiveUser.getUniqueComputerId(), session);

        return effectiveUser.toOutSelfSessionParticipant();
    }

    @PostMapping("/join/existing")
    public OutSelfSessionParticipant userJoinSessionExistingUser(@RequestBody InJoinSessionExistingUser requestUser) {
        LOG.info("User [{}] wants to join session [{}] with password [{}]",
                requestUser.getUserId(), requestUser.getSessionName(), requestUser.getSessionPassword());

        final Session session = sessionService.findSessionByName(requestUser.getSessionName());
        if (session == null) {
            throw new RuntimeException("Session not found");
        }

        final SessionParticipant foundUser = userService.findUserById(UUID.fromString(requestUser.getUserId()));
        if (foundUser == null) {
            throw new RuntimeException("User not found");
        }

        if (!session.getParticipants().contains(foundUser)) {
            session.assertPasswordCorrect(requestUser.getSessionPassword());
        }

        foundUser.getSessionState(session).setSpectator(requestUser.isSpectator());
        session.addParticipant(foundUser);
        notifySessionChanged(session);

        return foundUser.toOutSelfSessionParticipant();
    }

    @PostMapping("/manager/validate/login")
    public OutSelfSessionParticipant assertUserSuperuserLogin(@RequestBody InManagerLogin requestManagerLogin) {
        LOG.info("User [{}] with computer id [{}] wants to login as manager", requestManagerLogin.getUsername(), requestManagerLogin.getUniqueComputerId());

        final SessionParticipant foundUser = userService.findUserByNameAndComputerId(requestManagerLogin.getUsername(), requestManagerLogin.getUniqueComputerId());

        if (foundUser != null && foundUser.isSuperuser()) {
            return foundUser.toOutSelfSessionParticipant();
        }

        userService.assertSuperuser(requestManagerLogin.getAuthToken());

        final SessionParticipant effectiveUser;
        if (foundUser == null) {
            final SessionParticipant newUser = new SessionParticipant(requestManagerLogin.getUsername(), requestManagerLogin.getUniqueComputerId());
            userService.registerParticipant(newUser);
            effectiveUser = newUser;
        } else {
            effectiveUser = foundUser;
        }

        effectiveUser.setSuperuser(true);

        LOG.info("User {} [aka {}] with computer id [{}] validated as superuser",
                effectiveUser, effectiveUser.getReferenceId(), effectiveUser.getUniqueComputerId());

        return effectiveUser.toOutSelfSessionParticipant();
    }

    @PostMapping("/participants")
    public List<OutOtherSessionParticipant> getSessionParticipants(@RequestBody InSessionParticipantCheck sessionData) {
        LOG.info("User [{}] wants to get participants of session [{}]",
                sessionData.getUserId(), sessionData.getSessionName());

        final SessionParticipant foundUser = userService.findUserById(UUID.fromString(sessionData.getUserId()));
        if (foundUser == null) {
            LOG.error("User [{}] wanted to get participants of session [{}], but did not exist",
                    sessionData.getUserId(), sessionData.getSessionName());
            return List.of(new OutOtherSessionParticipant("unknown user"));
        }

        final Session foundSession = sessionService.findSessionByName(sessionData.getSessionName());
        if (foundSession == null) {
            LOG.error("User [{}] wanted to get participants of session [{}], but session did not exist",
                    sessionData.getUserId(), sessionData.getSessionName());
            return List.of(new OutOtherSessionParticipant("unknown session"));
        }
        if (!foundSession.getParticipants().contains(foundUser)) {
            LOG.error("User [{}] wanted to get participants of session [{}], but was not part of session",
                    sessionData.getUserId(), sessionData.getSessionName());
            return List.of(new OutOtherSessionParticipant("not part of session"));
        }

        LOG.info("Allowing user {} to get participants of session [{}]", foundUser, foundSession);

        return foundSession.toOutOtherParticipants();
    }

    @PostMapping("/manager/validate/id")
    public OutSelfSessionParticipant checkUserSuperuser(@RequestBody InUserId userId) {
        final SessionParticipant foundUser = getSuperuser(userId, "check superuser status");
        return foundUser.toOutSelfSessionParticipant();
    }

    @PostMapping("/create")
    public List<OutFullSession> createSession(@RequestBody InSessionCreation requestSessionName) {
        LOG.info("User [{}] wants to create session [{}] with password [{}]",
                requestSessionName.getUserId(), requestSessionName.getSessionName(), requestSessionName.getPassword() == null);
        getSuperuser(requestSessionName, "create session");
        sessionService.createSession(requestSessionName.getSessionName(), requestSessionName.getPassword());
        return sessionService.getOutSessions();
    }

    @PostMapping("/delete")
    public List<OutFullSession> deleteSession(@RequestBody InSessionDeletion requestSessionName) {
        LOG.info("User [{}] wants to delete session [{}]",
                requestSessionName.getUserId(), requestSessionName.getSessionId());
        final SessionParticipant superuser = getSuperuser(requestSessionName, "delete session");
        final Session session = sessionService.findSessionById(UUID.fromString(requestSessionName.getSessionId()));
        if (session == null) {
            throw new RuntimeException("User " + superuser + " wanted to delete session [" + requestSessionName.getSessionId() + "], but session did not exist");
        }

        sessionService.close(session);

        return sessionService.getOutSessions();
    }

    @PostMapping("/list")
    public List<OutFullSession> listSessions(@RequestBody InUserId userIf) {
        getSuperuser(userIf, "list sessions");
        return sessionService.getOutSessions();
    }

    private void notifySessionChanged(Session session) {
        session.notifySessionChanged();
        userService.notifyAllSessionManagersOfSessionChanged(session);
    }

    private SessionParticipant getSuperuser(InUserId userId, String reason) {
        return getSuperuser(userId.getUserId(), reason);
    }

    private SessionParticipant getSuperuser(String userId, String reason) {
        final UUID uuid;
        try {
            uuid = UUID.fromString(userId);
        } catch (Exception e) {
            throw new RuntimeException("User [" + userId + "] wanted to perform superuser action [" + reason + "], but provided invalid user id (" + e.getMessage() + ")");
        }
        final SessionParticipant foundUser = userService.findUserById(uuid);
        if (foundUser == null) {
            throw new RuntimeException("User [" + userId + "] wanted to perform superuser action [" + reason + "], but did not exist");
        }
        if (!foundUser.isSuperuser()) {
            throw new RuntimeException("User " + foundUser + " wanted to perform superuser action [" + reason + "], but was not superuser");
        }
        LOG.info("Allowing user {} to perform superuser action [{}]", foundUser, reason);
        return foundUser;
    }
}
