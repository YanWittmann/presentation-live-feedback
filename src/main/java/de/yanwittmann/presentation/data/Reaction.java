package de.yanwittmann.presentation.data;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

public class Reaction {

    public final static List<Reaction> REACTIONS = Arrays.asList(
            new Reaction("Smile", "\uD83D\uDE00"),
            new Reaction("Love", "\uD83D\uDE0D"),
            new Reaction("Confusion", "\uD83E\uDD14"),
            new Reaction("Slower", "\uD83D\uDC22"),
            new Reaction("Faster", "\uD83D\uDC07"),
            new Reaction("Number 1", "1️⃣"),
            new Reaction("Number 2", "2️⃣"),
            new Reaction("Number 3", "3️⃣"),
            new Reaction("Number 4", "4️⃣"),
            new Reaction("Number 5", "5️⃣")
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
