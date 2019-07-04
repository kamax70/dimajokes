package ru.dimajokes;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class JokesCache {

    private static final String PREFIX = "jokes";
    private static final String PREFIX_IDS = "jokesIds";
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyy");

    @Autowired
    private RedisTemplate redisTemplate;

    public long getCount() {
        String key = getKey();
        redisTemplate.opsForValue().setIfAbsent(key, 0);
        return redisTemplate.opsForValue().increment(key);
    }

    public String getKey() {
        return PREFIX + "."+sdf.format(new Date());
    }

    public boolean save(Integer messageId) {
        if (!redisTemplate.opsForSet().isMember(PREFIX_IDS, messageId)) {
            redisTemplate.opsForSet().add(PREFIX_IDS, messageId);
            return true;
        }
        return false;
    }
}
