package de.yanwittmann.presentation.data;

import java.util.Arrays;
import java.util.List;

public class Reaction {

    public final static List<Reaction> REACTIONS = Arrays.asList(
            new Reaction("smile", "\uD83D\uDE00"),
            new Reaction("love", "\uD83D\uDE0D"),
            new Reaction("think", "\uD83E\uDD14"),
            new Reaction("turtle", "\uD83D\uDC22"),
            new Reaction("bunny", "\uD83D\uDC07"),
            new Reaction("number1", "1️⃣"),
            new Reaction("number2", "2️⃣"),
            new Reaction("number3", "3️⃣"),
            new Reaction("number4", "4️⃣"),
            new Reaction("number5", "5️⃣")
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
}
