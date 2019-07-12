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

import static ru.dimajokes.MessageUtils.testStringForKeywords;

@Slf4j
@RequiredArgsConstructor
public class Bot extends TelegramLongPollingBot {

    private final JokesCache jokesCache;
    private final String token;

    private final String[] msgs = new String[]{"Да ладно, Димон опять пошутил! ", "Новая шутейка от Дмитрия. ", "Остановите его! Снова юмор! "};
    private final String[] suffix = new String[]{"И это уже ", "", "Счетчик улетает в космос! "};
    private final String end = " раз за день!";
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
                            if (testStringForKeywords(text)) {
                                if (jokesCache.save(m.getMessageId(), m.getText())) {
                                    sendMsg(getText(), message.getChatId());
                                }
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

    private String getText() {
        String msg = msgs[ThreadLocalRandom.current().nextInt(msgs.length)];
        String suf = suffix[ThreadLocalRandom.current().nextInt(suffix.length)];
        return msg + suf + jokesCache.getCount() + end;
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
