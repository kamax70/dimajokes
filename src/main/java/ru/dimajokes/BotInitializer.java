package ru.dimajokes;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import javax.annotation.PostConstruct;

/**
 * Created by UMS on 5/7/2019.
 */
@Component
@Slf4j
public class BotInitializer {

    private Bot bot;

    @Autowired
    private JokesCache jokesCache;

    @PostConstruct
    public void init() {
        ApiContextInitializer.init();
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try {
            bot = new Bot(jokesCache);
            telegramBotsApi.registerBot(bot);
        } catch (TelegramApiRequestException e) {
            log.error("Error on init telegram bot", e);
        }
    }
}