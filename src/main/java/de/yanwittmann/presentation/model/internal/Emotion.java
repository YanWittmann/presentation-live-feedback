package de.yanwittmann.presentation.model.internal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum Emotion {
    SMILE("\uD83D\uDE42"), PARTY("\uD83E\uDD73"),
    SLOWER("\uD83D\uDC22"), FASTER("\uD83D\uDC07"),
    QUESTION_MARK("\u2753"),
    BREAK("\u2615"),
    CHECKMARK("\u2714"), CROSS("\u274C"),
    ONE("\u0031\u20E3"), TWO("\u0032\u20E3"), THREE("\u0033\u20E3"), FOUR("\u0034\u20E3"), FIVE("\u0035\u20E3"),
    ;

    private final String emoji;

    public String getWellFormedName() {
        final String lowercaseAndUnderscoreReplaced = name().toLowerCase().replace("_", " ");
        return lowercaseAndUnderscoreReplaced.substring(0, 1).toUpperCase() + lowercaseAndUnderscoreReplaced.substring(1);
    }

    public JSONObject toJson() {
        return new JSONObject()
                .put("emoji", emoji)
                .put("wellFormedName", getWellFormedName())
                .put("name", name());
    }

    public final static JSONArray ALL_EMOTIONS = new JSONArray(Arrays.stream(Emotion.values()).map(Emotion::toJson).collect(Collectors.toList()));
}
