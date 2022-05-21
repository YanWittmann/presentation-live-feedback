package de.yanwittmann.presentation.data;

public class User {

    private final String username;
    private Reaction reaction;
    private long reactionLastChanged;

    public User(String username) {
        this.username = username;
    }

    public void setReaction(Reaction reaction) {
        this.reaction = reaction;
        this.reactionLastChanged = System.currentTimeMillis();
    }

    public String getUsername() {
        return username;
    }

    public Reaction getReaction() {
        return reaction;
    }

    public long getReactionLastChanged() {
        return reactionLastChanged;
    }

}
