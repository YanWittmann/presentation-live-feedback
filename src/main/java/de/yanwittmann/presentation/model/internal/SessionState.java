package de.yanwittmann.presentation.model.internal;

import lombok.Data;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

@Data
public class SessionState {

    // 2023-11-11T00:04:28.396+00:00
    private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    private Emotion emotion = Emotion.SMILE;
    private Date emotionChangedTime = new Date(); // emotion changed
    private boolean isSpectator = false;
    private Date raisedHandTime;

    public void setEmotion(Emotion emotion) {
        this.emotion = emotion;
        this.emotionChangedTime = new Date();
    }

    public JSONObject toJson() {
        return new JSONObject()
                .put("emotionChangedTime", DATE_FORMAT.format(emotionChangedTime))
                .put("raisedHandTime", raisedHandTime == null ? null : DATE_FORMAT.format(raisedHandTime))
                .put("emotion", emotion.toJson())
                .put("isSpectator", isSpectator);
    }
}
