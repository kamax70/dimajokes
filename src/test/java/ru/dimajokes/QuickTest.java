package ru.dimajokes;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.Set;

public class QuickTest {

    public static void main(String[] args) {
        ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger("ROOT").setLevel(Level.OFF);
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName("ec2-52-49-47-111.eu-west-1.compute.amazonaws.com");
        config.setPort(28089);
        config.setPassword("pca8b6a8ee45b25b480d1e2a3ef036200e7a1f0db5c67e52cb4e874571645207a");
        RedisTemplate template = new RedisSpringConfiguration(null).redisTemplate(new JedisConnectionFactory(config));
        Set keys = template.keys("joke.*");
        List list = template.opsForValue().multiGet(keys);
        list.forEach(s -> {
            System.out.println("* " + s);
        });
    }

}
