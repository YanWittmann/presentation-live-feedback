package de.yanwittmann.presentation.data;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class User {

    private final static Logger LOG = LoggerFactory.getLogger(User.class);

    private final String uuid;
    private final String name;
    private Reaction reaction = Reaction.REACTIONS.get(0);
    private long reactionLastChanged = System.currentTimeMillis();
    private int handRaisedIndex = -1;
    private boolean spectator = false;

    private Set<ReactionChangeListener> reactionChangeListeners = new HashSet<>();
    private Set<HandRaisedListener> handRaisedListeners = new HashSet<>();

    public User(String uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        LOG.info("Created user [{}]", name);
    }

    public static boolean isValidUsername(String from) {
        if (from == null) {
            return false;
        } else if (from.length() == 0) {
            return false;
        } else if (from.length() > 25) {
            return false;
        }
        return from.toLowerCase().matches("^[a-zA-Z\\d_ !?()äöüâàçéèêëîïôùûœ\n]+$");
    }

    public void setReaction(Reaction reaction) {
        if (reaction == null) {
            throw new IllegalArgumentException("reaction must not be null");
        }
        this.reaction = reaction;
        this.reactionLastChanged = System.currentTimeMillis();
        LOG.info("Changed reaction of user [{}] to [{}]", name, reaction.toJson());
        reactionChangeListeners.forEach(listener -> listener.onReactionChanged(this, reaction));
    }

    public String getName() {
        return name;
    }

    public String getUuid() {
        return uuid;
    }

    public Reaction getReaction() {
        return reaction;
    }

    public long getReactionLastChanged() {
        return reactionLastChanged;
    }

    public boolean addReactionChangeListener(ReactionChangeListener listener) {
        return reactionChangeListeners.add(listener);
    }

    public boolean removeReactionChangeListener(ReactionChangeListener listener) {
        return reactionChangeListeners.remove(listener);
    }

    public void addHandRaisedListener(HandRaisedListener listener) {
        handRaisedListeners.add(listener);
    }

    public void removeHandRaisedListener(HandRaisedListener listener) {
        handRaisedListeners.remove(listener);
    }

    public void setHandRaisedIndex(int handRaisedIndex) {
        this.handRaisedIndex = handRaisedIndex;
        handRaisedListeners.forEach(listener -> listener.onHandRaised(this, handRaisedIndex));
    }

    public int getHandRaisedIndex() {
        return handRaisedIndex;
    }

    public boolean isHandRaised() {
        return handRaisedIndex >= 1;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("uuid", uuid);
        json.put("name", name);
        json.put("reaction", reaction.toJson());
        json.put("reactionLastChanged", reactionLastChanged);
        json.put("handRaisedIndex", handRaisedIndex);
        json.put("spectator", spectator);
        return json;
    }

    public boolean isUser(String uuid, String name) {
        if (uuid == null || name == null) {
            return false;
        }
        return String.valueOf(this.uuid).equals(uuid) && String.valueOf(this.name).equals(name);
    }

    public void setSpectator(boolean spectator) {
        this.spectator = spectator;
    }

    public boolean isSpectator() {
        return spectator;
    }

    public interface ReactionChangeListener {
        void onReactionChanged(User user, Reaction reaction);
    }

    public interface HandRaisedListener {
        void onHandRaised(User user, int handRaisedIndex);
    }
}
