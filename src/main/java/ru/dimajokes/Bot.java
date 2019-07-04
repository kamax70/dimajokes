package ru.dimajokes;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@AllArgsConstructor
public class Bot extends TelegramLongPollingBot {

    private JokesCache jokesCache;

    private final String[] msgs = new String[] {"Да ладно, Димон опять пошутил! ", "Новая шутейка от Дмитрия. ", "Остановите его! Снова юмор! "};
    private final String[] suffix = new String[] {"И это уже ", "", "Счетчик улетает в космос! "};
    private final String end = " раз за день!";

    private void sendMsg(String s, Long chatId) {
        log.info("send message {}", s);
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setText(s);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Exception: ", e);
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage()) {
                Message message = update.getMessage();
                Optional.ofNullable(message.getReplyToMessage())
                        .filter(m -> m.getFrom().getUserName().equalsIgnoreCase("DmitrySedykh"))
                        .ifPresent(m -> {
                            if (message.getText().equalsIgnoreCase("лол") || message.getText().equalsIgnoreCase("кек") || message.getText().contains("хаха") || message.getText().contains("ХАХА")) {
                                if (jokesCache.save(m.getMessageId())) {
                                    sendMsg(getText(), message.getChatId());
                                }
                            }
                        });
            }
        } catch (Exception e) {
            log.error("Error processing message", e);
        }
    }

    private String getText() {
        String msg = msgs[ThreadLocalRandom.current().nextInt(3)];
        String suf = suffix[ThreadLocalRandom.current().nextInt(3)];
        return msg + suf + jokesCache.getCount() + end;
    }

    @Override
    public String getBotUsername() {
        return "DimaJokes";
    }

    @Override
    public String getBotToken() {
        return "819123597:AAGKqXhC9EsvR-mnMzoke6denz37MVUor8k";
    }

}
