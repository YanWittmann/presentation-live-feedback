package de.yanwittmann.presentation.data;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

public class Reaction {

    public final static List<Reaction> REACTIONS = Arrays.asList(
            new Reaction("Smile", "\uD83D\uDE42"),
            new Reaction("Party", "\uD83E\uDD73"),
            new Reaction("Question", "\u2753"),
            new Reaction("Slower", "\uD83D\uDC22"),
            new Reaction("Faster", "\uD83D\uDC07"),
            new Reaction("Coffee Break", "\u2615"),
            new Reaction("Checkmark", "\u2714"),
            new Reaction("Cross", "\u274C"),
            new Reaction("Answer 1", "\u0031\uFE0F\u20E3"),
            new Reaction("Answer 2", "\u0032\uFE0F\u20E3"),
            new Reaction("Answer 3", "\u0033\uFE0F\u20E3"),
            new Reaction("Answer 4", "\u0034\uFE0F\u20E3"),
            new Reaction("Answer 5", "\u0035\uFE0F\u20E3"),
            new Reaction("Answer 6", "\u0036\uFE0F\u20E3")
    );

    private final String name;
    private final String emote;

    private Reaction(String name, String emote) {
        this.name = name;
        this.emote = emote;
    }

    public String getName() {
        return name;
    }

    public String getEmote() {
        return emote;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("name", name);
        json.put("emote", emote);
        return json;
    }

    @Override
    public String toString() {
        return "Reaction [name=" + name + ", emote=" + emote + "]";
    }

    public static Reaction findReaction(String reactionName, String emote) {
        for (Reaction reaction : REACTIONS) {
            if (reaction.getName().equals(reactionName) || reaction.getEmote().equals(emote)) {
                return reaction;
            }
        }
        throw new IllegalArgumentException("Reaction not found: " + reactionName + " " + emote);
    }
}
