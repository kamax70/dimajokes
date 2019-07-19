package ru.dimajokes;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

import static java.lang.String.valueOf;
import static java.util.Optional.ofNullable;

@Component
@RequiredArgsConstructor
public class JokesCache {
    private final RedisTemplate redisTemplate;

    private final static String SEPARATOR = ".";

    public static final String CHAT_ID = "CHAT_ID";
    private static final String PREFIX = "jokes";
    private static final String PREFIX_IDS = "jokesIds";
    private static final String PREFIX_TYPES = "jokesTypes";
    private static final String JOKE = "joke";

    private final DateTimeFormatter sdf = DateTimeFormatter.ofPattern("dd.MM.yyy");


    @SuppressWarnings("unchecked")
    public long getCount(boolean good) {
        String key = getKey();
        redisTemplate.opsForValue().setIfAbsent(key, 0);
        Function<String, Long> operation = good
                ? s -> redisTemplate.opsForValue().increment(s)
                : s -> redisTemplate.opsForValue().decrement(s);
        return operation.apply(key);
    }

    public String getKey() {
        return PREFIX + SEPARATOR + sdf.format(LocalDate.now());
    }

    @SuppressWarnings("unchecked")
    public boolean save(Integer messageId, String text, boolean good) {
        if (!redisTemplate.opsForSet().isMember(PREFIX_IDS, messageId)) {
            redisTemplate.opsForSet().add(PREFIX_IDS, messageId);
            redisTemplate.opsForValue().set(JOKE + SEPARATOR + messageId, text);
            redisTemplate.opsForHash().put(PREFIX_TYPES + SEPARATOR + sdf.format(LocalDate.now()), messageId.toString(), valueOf(good));
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public void saveChatId(long chatId) {
        redisTemplate.opsForValue().set(CHAT_ID, chatId);
    }

    public Long getChatId() {
        return ofNullable(((Number)redisTemplate.opsForValue().get(CHAT_ID)))
                .map(Number::longValue)
                .orElse(null);
    }
}
