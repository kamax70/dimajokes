package ru.dimajokes;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class JokesCache {

    public static final String CHAT_ID = "CHAT_ID";
    private static final String PREFIX = "jokes";
    private static final String PREFIX_IDS = "jokesIds";
    private static final String JOKE = "joke";
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

    public boolean save(Integer messageId, String text) {
        if (!redisTemplate.opsForSet().isMember(PREFIX_IDS, messageId)) {
            redisTemplate.opsForSet().add(PREFIX_IDS, messageId);
            redisTemplate.opsForValue().set(JOKE + "." + messageId, text);
            return true;
        }
        return false;
    }

    public void saveChatId(long chatId) {
        redisTemplate.opsForValue().set(CHAT_ID, chatId);
    }

    public long getChatId() {
        return ((Number)redisTemplate.opsForValue().get(CHAT_ID)).longValue();
    }
}
