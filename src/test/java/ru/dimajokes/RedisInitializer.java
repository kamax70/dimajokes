package ru.dimajokes;

import lombok.SneakyThrows;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import redis.embedded.RedisServer;

public class RedisInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext>, ApplicationListener<ContextClosedEvent> {

    private RedisServer server;

    @Override
    @SneakyThrows
    public void initialize(ConfigurableApplicationContext ctx) {
        server = new RedisServer(6379);
        server.start();
        TestPropertyValues.of(
                "spring.redis.host=localhost",
                "spring.redis.port=6379"
        ).applyTo(ctx);
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent contextClosedEvent) {
        server.stop();
    }
}
