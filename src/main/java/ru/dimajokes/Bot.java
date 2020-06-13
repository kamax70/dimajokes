package ru.dimajokes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static java.lang.String.format;
import static java.util.concurrent.ThreadLocalRandom.current;
import static ru.dimajokes.MessageUtils.testStringForKeywords;

@Slf4j
@RequiredArgsConstructor
public class Bot extends TelegramLongPollingBot {

    private final JokesCache jokesCache;
    private final BotConfig config;

    private final String[] goodMsg = {"Да ладно, %s опять пошутил! ", "Остановите его! Снова юмор! "};
    private final String[] badMsg = {"%s, теряешь хватку. ", "Как то не очень, сорри. ", "Очень плохо %s... "};
    private final String[] goodSuffix = {"И это уже ", "", "Счетчик улетает в космос! "};
    private final String[] badSuffix = {"Давай, соберись. ", "Попробуй еще раз, что-ли... "};
    private final String goodEnd = " раз за день! ";
    private final String motivation = "Еще чуть-чуть, и ты выйдешь в плюс!";
    private final Function<Long, String> badEnd = l -> format("Счетчик опустился до %d =\\", l);
    private final String voiceMessageReply = "Пошел нахуй.";
    private Set<Long> chatIds;

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage()) {
                Message message = update.getMessage();

                if (message.hasVoice()) {
                    sendMsg(voiceMessageReply, message.getChatId(), message);
                    return;
                }

                if (chatIds == null) {
                    chatIds = config.getJokers().keySet();
                }
                Optional.ofNullable(message.getReplyToMessage())
                        .filter(m -> chatIds.contains(m.getFrom().getId().longValue()))
                        .ifPresent(reply -> {
                            MessageUtils.JokeType jokeType = testStringForKeywords(message.getText());
                            log.info("joke type of {} is {}", message.getText(), jokeType);
                            Long chatId = reply.getFrom().getId().longValue();
                            switch (jokeType) {
                                case GOOD:
                                    if (jokesCache.save(chatId, reply.getMessageId(), reply.getText(), true)) {
                                        sendMsg(getText(chatId, true), message.getChatId());
                                    }
                                    break;
                                case BAD:
                                    if (jokesCache.save(chatId, reply.getMessageId(), reply.getText(), false)) {
                                        sendMsg(getText(chatId, false), message.getChatId());
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

    private void sendMsg(String s, Long chatId, Message replyMsg) {
        log.info("send message {}", s);
        SendMessage sendMessage = new SendMessage(chatId, s)
                .enableMarkdown(true);
        sendMessage.setReplyToMessageId(replyMsg.getMessageId());
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Exception: ", e);
        }
    }

    private String getText(Long chatId, boolean good) {
        String msg;
        String suf;
        String end;
        long count = jokesCache.getCount(chatId, good);
        List<String> names = config.getJokers().get(chatId).getNames();
        if (good) {
            msg = format(goodMsg[current().nextInt(goodMsg.length)], names.get(current().nextInt(names.size())));
            suf = goodSuffix[current().nextInt(goodSuffix.length)];
            end = count + goodEnd;
            if (count < 0) {
                end += motivation;
            }
        } else {
            msg = format(badMsg[current().nextInt(badMsg.length)], names.get(current().nextInt(names.size())));
            suf = badSuffix[current().nextInt(badSuffix.length)];
            end = badEnd.apply(count);
        }
        return msg + suf + end;
    }

    @Override
    public String getBotUsername() {
        return "DimaJokes";
    }

    @Override
    public String getBotToken() {
        return config.getBotToken();
    }

}
