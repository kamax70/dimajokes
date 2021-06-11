package ru.dimajokes;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.generics.BotSession;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.dimajokes.featuretoggle.FeatureToggleService;

import javax.annotation.PostConstruct;

/**
 * Created by UMS on 5/7/2019.
 */
@Component
@Slf4j
public class BotInitializer implements ApplicationListener<ContextStoppedEvent> {

    private final JokesCache jokesCache;
    private final BotConfig config;
    private final FeatureToggleService featureToggleService;
    private BotSession botSession;

    public BotInitializer(JokesCache jokesCache, BotConfig config, FeatureToggleService featureToggleService) {
        this.jokesCache = jokesCache;
        this.config = config;
        this.featureToggleService = featureToggleService;
    }

    @PostConstruct
    public void init() throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        try {
            Bot bot = new Bot(jokesCache, config, true, featureToggleService);
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