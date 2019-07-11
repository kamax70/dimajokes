package ru.dimajokes;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.generics.BotSession;

import javax.annotation.PostConstruct;

/**
 * Created by UMS on 5/7/2019.
 */
@Component
@Slf4j
public class BotInitializer implements ApplicationListener<ContextStoppedEvent> {

    private final JokesCache jokesCache;
    private final String token;
    private BotSession botSession;

    public BotInitializer(JokesCache jokesCache, @Value("${telegram.bot-token}") String token) {
        this.jokesCache = jokesCache;
        this.token = token;
    }

    @PostConstruct
    public void init() {
        ApiContextInitializer.init();
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try {
            Bot bot = new Bot(jokesCache, token);
            botSession = telegramBotsApi.registerBot(bot);
        } catch (TelegramApiRequestException e) {
            log.error("Error on init telegram bot", e);
        }
    }

    @Override
    public void onApplicationEvent(ContextStoppedEvent evt) {
        botSession.stop();
    }
}