package ru.dimajokes.featuretoggle;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeatureToggleService {

    private final RedisTemplate redisTemplate;

    private static final String FT_PREFIX = "feature-toggle";

    public boolean isEnabled(Feature feature, Long chatId) {
        String s = (String) redisTemplate.opsForValue().get(buildKey(feature, chatId));
        if (s == null) {
            return true;
        }
        return Boolean.parseBoolean(s);
    }

    public boolean toggleFeature(Feature feature, Long chatId) {
        String s = (String) redisTemplate.opsForValue().get(buildKey(feature, chatId));
        Boolean newValue;
        if (s == null) {
            newValue = false;
        } else {
            newValue = !Boolean.parseBoolean(s);
        }
        redisTemplate.opsForValue().set(buildKey(feature, chatId), newValue.toString());
        return newValue;
    }

    private String buildKey(Feature feature, Long chatId) {
        return String.join(":", FT_PREFIX, chatId.toString(), feature.toString());
    }


}
