package ru.dimajokes;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;
import org.mockito.invocation.Invocation;
import org.mockito.verification.VerificationMode;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisConnectionUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendSticker;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import redis.embedded.RedisServer;

import java.net.ServerSocket;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
        Integer port;
        try(ServerSocket s = new ServerSocket(0)) {
            port = s.getLocalPort();
        }
        server = new RedisServer(port);
        server.start();
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName("localhost");
        configuration.setPort(port);
        template = new RedisSpringConfiguration(null).redisTemplate(new JedisConnectionFactory(configuration));
        cache = new JokesCache(template);
        bot = new Bot(cache, prepareConfig(), false);
    }

    @Test
    @SneakyThrows
    public void testPositive() {
        template.opsForValue().set(cache.getKey(0L), 0);
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
        assertEquals(6, template.opsForValue().get(cache.getKey(0L)));
        Map<String, Integer> map = getJokeTypesMap(messageIds);
        assertSame(7, map.get(Boolean.TRUE.toString()));
        assertSame(1, map.get(Boolean.FALSE.toString()));
    }

    @Test
    @SneakyThrows
    public void testNegative() {
        template.opsForValue().set(cache.getKey(0L), 0);
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
        assertEquals(-7, template.opsForValue().get(cache.getKey(0L)));
        Map<String, Integer> jokeTypesMap = getJokeTypesMap(messageIds);
        assertSame(7, jokeTypesMap.get(Boolean.FALSE.toString()));
    }


    @Test
    @SneakyThrows
    public void testVoiceMessage() {
        template.opsForValue().set(cache.getKey(0L), 0);
        Bot spy = prepareBot();
        Message message = prepareMessage();
        Update update = prepareUpdate(message);
        Set<Integer> messageIds = newHashSet();
        log.info("trying {}", "");

        int messageId = ThreadLocalRandom.current().nextInt(100_000);
        messageIds.add(messageId);
        when(message.getMessageId()).thenReturn(messageId);
        when(message.getFrom().getUserName()).thenReturn(String.valueOf(messageId));
        when(message.hasVoice()).thenReturn(true);
        spy.onUpdateReceived(update);
        verify(spy, times(1)).execute(isA(SendMessage.class));
    }


    @Test
    @SneakyThrows
    public void testVideo() {
        template.opsForValue().set(cache.getKey(0L), 0);
        Bot spy = prepareBot();
        Message message = prepareMessage();
        Update update = prepareUpdate(message);
        Set<Integer> messageIds = newHashSet();
        asList("тут текст", "", "еще какойто текст", "", "теекст", "", "привет", "здраствуйте").forEach(s -> {
            log.info("trying {}", s);

            when(message.getText()).thenReturn(s);

            int messageId = ThreadLocalRandom.current().nextInt(100_000);
            messageIds.add(messageId);
            when(message.getMessageId()).thenReturn(messageId);
            when(message.getFrom().getUserName()).thenReturn(String.valueOf(messageId));
            when(message.hasVideoNote()).thenReturn(s.isEmpty());
            spy.onUpdateReceived(update);
        });
        verify(spy, times(3)).execute(isA(SendMessage.class));
    }

    @Test
    @SneakyThrows
    public void testUkrainianSupport() {
        template.opsForValue().set(cache.getKey(0L), 0);
        Bot spy = prepareBot();
        Message message = prepareMessage();
        Update update = prepareUpdate(message);
        Set<Integer> messageIds = newHashSet();

        List<String> positive = asList("Слава УкРаИне и героям", "слава УКРАИНЕ", "украине слава", "слава Украине!", "Слава украине.");
        List<String> negative = asList("дуд лох", "еще какойто текст", "шо", " ");

        ListUtils.union(positive, negative).forEach(s -> {
            log.info("trying {}", s);

            when(message.getText()).thenReturn(s);

            int messageId = ThreadLocalRandom.current().nextInt(100_000);
            messageIds.add(messageId);
            when(message.getMessageId()).thenReturn(messageId);
            when(message.hasText()).thenReturn(true);
            when(message.getFrom().getUserName()).thenReturn(String.valueOf(messageId));
            when(message.hasVideo()).thenReturn(s.isEmpty());
            spy.onUpdateReceived(update);
        });
        verify(spy, Mockito.atMost(positive.size())).execute(isA(SendMessage.class)); // atMost here is just to set isVerified marker
        verify(spy, Mockito.atMost(positive.size())).execute(isA(SendSticker.class)); // atMost here is just to set isVerified marker
        long count = Mockito.mockingDetails(spy).getInvocations().stream()
                .filter(Invocation::isVerified)
                .count();
        assertEquals(positive.size(), count);
    }

    @Test
    @SneakyThrows
    public void testBelarusSupport() {
        Bot spy = prepareBot();
        Message message = prepareMessage();
        Update update = prepareUpdate(message);

        asList("беларуссия", "беларусии", "еще какойто текст", "беларусия", "беларусь", " ", "беларуссия", "белоруссия").forEach(s -> {
            log.info("trying {}", s);

            when(message.getText()).thenReturn(s);

            when(message.hasText()).thenReturn(true);
            when(message.getText()).thenReturn(s);
            spy.onUpdateReceived(update);
        });
        verify(spy, times(5)).execute(isA(SendMessage.class));
    }

    @Test
    @SneakyThrows
    public void testDaStickerSupport() {
        Bot spy = prepareBot();
        Message message = prepareMessage();
        Update update = prepareUpdate(message);

        List<String> matching = asList("да", "ДА", "Дааа", "Да?", "ДАААА!", "Да.", "да))))", "дa", "да_");
        List<String> nonMatching = asList("пизда", "когда", "елда", "вода", "погода", "да уж", "всегда", "ну типа да", ",да", "...да");

        ListUtils.union(matching, nonMatching).forEach(s -> {
            log.info("trying {}", s);

            when(message.getText()).thenReturn(s);
            when(message.hasText()).thenReturn(true);
            spy.onUpdateReceived(update);
        });
        verify(spy, times(matching.size())).execute(isA(SendSticker.class));
    }

    @Test
    @SneakyThrows
    public void testNetMessageSupport() {
        Bot spy = prepareBot();
        Message message = prepareMessage();
        Update update = prepareUpdate(message);

        List<String> matching = asList("нет", "НЕТ", "неееет", "нет?", "НЕЕЕЕЕТ!", "Нет.", "нет))))", "нeт", "нет_");
        List<String> nonMatching = asList("минет","нет 10", "кабинет", "кларнет", "ронет", "нетраннер", "рунет", "ыфванетфыва");

        ListUtils.union(matching, nonMatching).forEach(s -> {
            log.info("trying {}", s);

            when(message.getText()).thenReturn(s);
            when(message.hasText()).thenReturn(true);
            spy.onUpdateReceived(update);
        });
        verify(spy, times(matching.size())).execute(isA(SendMessage.class));
    }


    private Map<String, Integer> getJokeTypesMap(Set<Integer> messageIds) {
        List<String> list = template.opsForHash().multiGet("jokesTypes.0." + DateTimeFormatter.ofPattern("dd.MM.yyyy").format(LocalDate.now()), messageIds.stream().map(Object::toString).collect(Collectors.toList()));
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
        when(message.getChatId()).thenReturn(0L);
        when(user.getId()).thenReturn(0);
        return message;
    }

    private static BotConfig prepareConfig() {
        BotConfig config = new BotConfig();
        HashMap<Long, BotConfig.ConfigEntry> map = new HashMap<>();
        config.setJokers(map);
        BotConfig.ConfigEntry entry = new BotConfig.ConfigEntry();
        entry.setNames(Arrays.asList("kek"));
        map.put(0L, entry);
        return config;
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
        doReturn(null).when(spy).execute(isA(SendSticker.class));
        return spy;
    }

    @AfterClass
    public static void cleanup() {
        server.stop();
    }

}
