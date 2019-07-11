package ru.dimajokes;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.RedisURI;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
@EnableCaching
public class RedisSpringConfiguration {

    private final RedisProperties props;

    public RedisSpringConfiguration(RedisProperties props) {
        this.props = props;
    }

    @Bean
    @Primary // шоб идея не ругалась, заебала
    JedisConnectionFactory connectionFactory() {
        String url = props.getUrl();
        RedisURI uri = url != null
                ? RedisURI.create(url)
                : RedisURI.builder().withHost(props.getHost()).withPort(props.getPort()).build();
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(uri.getHost());
        configuration.setPassword(uri.getPassword());
        configuration.setPort(uri.getPort());
        configuration.setDatabase(0);
        return new JedisConnectionFactory(configuration);
    }

    @Bean
    RedisTemplate<?, ?> redisTemplate(JedisConnectionFactory factory) {
        RedisTemplate<Object, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setEnableTransactionSupport(false);
        redisTemplate.setKeySerializer(redisTemplate.getStringSerializer());
        redisTemplate.setValueSerializer(redisTemplate.getStringSerializer());

        redisTemplate.setConnectionFactory(factory);

        RedisSerializer jacksonSerializer = new GenericJackson2JsonRedisSerializer(typeAwareJsonObjectMapper());
        redisTemplate.setValueSerializer(jacksonSerializer);
        redisTemplate.setHashValueSerializer(jacksonSerializer);

        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean
    public ObjectMapper typeAwareJsonObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        return objectMapper;
    }
}
