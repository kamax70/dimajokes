package ru.dimajokes;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisConnectionUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import redis.embedded.RedisServer;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.glassfish.jersey.internal.guava.Sets.newHashSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.*;

@RunWith(JUnit4.class)
@Slf4j
public class BotTest {

    private static Bot bot;
    private static JokesCache cache;
    private static RedisTemplate template;
    private static RedisServer server;

    @BeforeClass
    @SneakyThrows
    public static void init() {
        ((Logger) LoggerFactory.getLogger(RedisConnectionUtils.class)).setLevel(Level.OFF); // заебал флудить блять в лог, пиздец
        server = new RedisServer(6379);
        server.start();
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName("localhost");
        configuration.setPort(6379);
        template = new RedisSpringConfiguration(null).redisTemplate(new JedisConnectionFactory(configuration));
        cache = new JokesCache(template);
        cache.saveChatId(1);
        bot = new Bot(cache, null);
    }

    @Test
    @SneakyThrows
    public void testPositive() {
        template.opsForValue().set(cache.getKey(), 0);
        Bot spy = prepareBot();
        Message message = prepareMessage();
        Update update = prepareUpdate(message);
        Set<Integer> messageIds = newHashSet();
        asList("хахах", "кек", "смешно", "смишно", "бля ахаха", "пиздатая шутейка", "смешная шутка", "плохая шутка").forEach(s -> {
            log.info("trying {}", s);
            when(message.getText()).thenReturn(s);
            int messageId = ThreadLocalRandom.current().nextInt(100_000);
            messageIds.add(messageId);
            when(message.getMessageId()).thenReturn(messageId);
            spy.onUpdateReceived(update);
        });
        verify(spy, times(8)).execute(isA(SendMessage.class));
        assertEquals(6, template.opsForValue().get(cache.getKey()));
        Map<String, Integer> map = getJokeTypesMap(messageIds);
        assertSame(7, map.get(Boolean.TRUE.toString()));
        assertSame(1, map.get(Boolean.FALSE.toString()));
    }

    @Test
    @SneakyThrows
    public void testNegative() {
        template.opsForValue().set(cache.getKey(), 0);
        Bot spy = prepareBot();
        Message message = prepareMessage();
        Update update = prepareUpdate(message);
        Set<Integer> messageIds = newHashSet();
        asList("нихуя не смешно", "не смишно", "не, ниоч", "не, ниоч шутейка", "плохая шутка", "ниоч шутка", "хуйня", "хуйня шутка", "хуевая шутка").forEach(s -> {
            log.info("trying {}", s);
            when(message.getText()).thenReturn(s);
            int messageId = ThreadLocalRandom.current().nextInt(100_000);
            messageIds.add(messageId);
            when(message.getMessageId()).thenReturn(messageId);
            spy.onUpdateReceived(update);
        });
        verify(spy, times(7)).execute(isA(SendMessage.class));
        assertEquals(-7, template.opsForValue().get(cache.getKey()));
        Map<String, Integer> jokeTypesMap = getJokeTypesMap(messageIds);
        assertSame(7, jokeTypesMap.get(Boolean.FALSE.toString()));
    }

    private Map<String, Integer> getJokeTypesMap(Set<Integer> messageIds) {
        List<String> list = template.opsForHash().multiGet("jokesTypes." + DateTimeFormatter.ofPattern("dd.MM.yyyy").format(LocalDate.now()), messageIds.stream().map(Object::toString).collect(Collectors.toList()));
        return list.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(s -> s))
                .entrySet()
                .stream()
                .map(e -> Pair.of(e.getKey(), e.getValue().size()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Message prepareMessage() {
        Message message = mock(Message.class);
        when(message.getReplyToMessage()).thenReturn(message);
        User user = mock(User.class);
        when(message.getFrom()).thenReturn(user);
        when(message.getMessageId()).thenReturn(ThreadLocalRandom.current().nextInt(1000));
        when(user.getId()).thenReturn(1);
        return message;
    }

    private Update prepareUpdate(Message message) {
        Update update = mock(Update.class);
        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        return update;
    }

    @SneakyThrows
    private Bot prepareBot() {
        Bot spy = spy(bot);
        doReturn(null).when(spy).execute(isA(SendMessage.class));
        return spy;
    }

    @AfterClass
    public static void cleanup() {
        server.stop();
    }

}
