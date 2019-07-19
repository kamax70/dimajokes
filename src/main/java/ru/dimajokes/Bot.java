package ru.dimajokes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

import static java.lang.String.format;
import static ru.dimajokes.MessageUtils.testStringForKeywords;

@Slf4j
@RequiredArgsConstructor
public class Bot extends TelegramLongPollingBot {

    private final JokesCache jokesCache;
    private final String token;

    private final String[] goodMsg = {"Да ладно, Димон опять пошутил! ", "Новая шутейка от Дмитрия. ", "Остановите его! Снова юмор! "};
    private final String[] badMsg = {"Димон, теряешь хватку. ", "Как то не очень, сорри. ", "Очень плохо Дмитрий. "};
    private final String[] goodSuffix = {"И это уже ", "", "Счетчик улетает в космос! "};
    private final String[] badSuffix = {"Давай, соберись. ", "Попробуй еще раз, что-ли... "};
    private final String goodEnd = " раз за день!";
    private final Function<Long, String> badEnd = l -> format("Счетчик опустился до %d =\\", l);
    private Long chatId;

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage()) {
                Message message = update.getMessage();
                if (chatId == null) {
                    chatId = jokesCache.getChatId();
                }
                Optional.ofNullable(message.getReplyToMessage())
                        .filter(m -> m.getFrom().getId().longValue() == chatId)
                        .ifPresent(m -> {
                            String text = message.getText();
                            MessageUtils.JokeType jokeType = testStringForKeywords(text);
                            log.info("joke type of {} is {}", text, jokeType);
                            switch (jokeType) {
                                case GOOD:
                                    if (jokesCache.save(m.getMessageId(), m.getText(), true)) {
                                        sendMsg(getText(true), message.getChatId());
                                    }
                                    break;
                                case BAD:
                                    if (jokesCache.save(m.getMessageId(), m.getText(), false)) {
                                        sendMsg(getText(false), message.getChatId());
                                    }
                                    break;
                                case UNKNOWN:
                                default:
                                    break;
                            }
                        });
            }
        } catch (Exception e) {
            log.error("Error processing message", e);
        }
    }

    private void sendMsg(String s, Long chatId) {
        log.info("send message {}", s);
        SendMessage sendMessage = new SendMessage(chatId, s)
                .enableMarkdown(true);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Exception: ", e);
        }
    }

    private String getText(boolean good) {
        String msg;
        String suf;
        String end;
        if (good) {
            msg = goodMsg[ThreadLocalRandom.current().nextInt(goodMsg.length)];
            suf = goodSuffix[ThreadLocalRandom.current().nextInt(goodSuffix.length)];
            end = jokesCache.getCount(good) + goodEnd;
        } else {
            msg = badMsg[ThreadLocalRandom.current().nextInt(badMsg.length)];
            suf = badSuffix[ThreadLocalRandom.current().nextInt(badSuffix.length)];
            end = badEnd.apply(jokesCache.getCount(good));
        }
        return msg + suf + end;
    }

    @Override
    public String getBotUsername() {
        return "DimaJokes";
    }

    @Override
    public String getBotToken() {
        return token;
    }

}
