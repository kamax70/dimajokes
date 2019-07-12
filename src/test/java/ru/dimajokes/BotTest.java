package ru.dimajokes;

import lombok.SneakyThrows;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import redis.embedded.RedisServer;

import static org.mockito.Mockito.*;

@RunWith(JUnit4.class)
public class BotTest {

    private static Bot bot;
    private static RedisServer server;

    @BeforeClass
    @SneakyThrows
    public static void init() {
        server = new RedisServer(6379);
        server.start();
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName("localhost");
        configuration.setPort(6379);
        RedisTemplate<?, ?> template = new RedisSpringConfiguration(null).redisTemplate(new JedisConnectionFactory(configuration));
        JokesCache cache = new JokesCache(template);
        cache.saveChatId(1);
        bot = new Bot(cache, null);
    }

    @Test
    @SneakyThrows
    public void testHaAa() {
        Update update = mock(Update.class);
        when(update.hasMessage()).thenReturn(true);
        Message message = mock(Message.class);
        when(update.getMessage()).thenReturn(message);
        when(message.getReplyToMessage()).thenReturn(message);
        User user = mock(User.class);
        when(message.getFrom()).thenReturn(user);
        when(user.getId()).thenReturn(1);
        when(message.getText()).thenReturn("хахаха");
        Bot spy = spy(bot);
        doReturn(null).when(spy).execute(((SendMessage) any()));
        spy.onUpdateReceived(update);
        verify(spy, times(1)).execute(((SendMessage) any()));
    }

    @AfterClass
    public static void cleanup() {
        server.stop();
    }

}
