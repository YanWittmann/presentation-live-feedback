package de.yanwittmann.presentation.controller;

import de.yanwittmann.presentation.model.internal.Emotion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/data")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class DataController {

    private final static Logger LOG = LogManager.getLogger(DataController.class);

    @GetMapping("/emotions")
    public List<Emotion> getEmotions() {
        return Arrays.asList(Emotion.values());
    }
}
