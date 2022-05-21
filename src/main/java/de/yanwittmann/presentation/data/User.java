package de.yanwittmann.presentation.data;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class User {

    private final static Logger LOG = LoggerFactory.getLogger(User.class);

    private final String name;
    private Reaction reaction = Reaction.REACTIONS.get(0);
    private long reactionLastChanged = System.currentTimeMillis();
    private int handRaisedIndex = -1;

    private Set<ReactionChangeListener> reactionChangeListeners = new HashSet<>();
    private Set<HandRaisedListener> handRaisedListeners = new HashSet<>();

    public User(String name) {
        this.name = name;
        LOG.info("Created user [{}]", name);
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
        json.put("name", name);
        json.put("reaction", reaction.toJson());
        json.put("reactionLastChanged", reactionLastChanged);
        json.put("handRaisedIndex", handRaisedIndex);
        return json;
    }

    public interface ReactionChangeListener {
        void onReactionChanged(User user, Reaction reaction);
    }

    public interface HandRaisedListener {
        void onHandRaised(User user, int handRaisedIndex);
    }
}
