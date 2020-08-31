package ru.dimajokes;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "telegram")
public class BotConfig {

    private String botToken;
    private Map<Long, ConfigEntry> jokers;

    @Data
    public static class ConfigEntry {
        List<String> names;
        Boolean canBeDisliked = true;
    }
}
