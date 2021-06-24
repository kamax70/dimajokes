package ru.dimajokes;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendSticker;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.dimajokes.featuretoggle.Feature;
import ru.dimajokes.featuretoggle.FeatureToggleService;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.concurrent.ThreadLocalRandom.current;
import static ru.dimajokes.MessageUtils.*;

@Slf4j
public class Bot extends TelegramLongPollingBot {

    private final JokesCache jokesCache;
    private final BotConfig config;
    private final Float probability;
    private final FeatureToggleService featureToggleService;

    public Bot(JokesCache jokesCache, BotConfig config, Boolean useProbabilities,
            FeatureToggleService featureToggleService) {
        this.jokesCache = jokesCache;
        this.config = config;
        this.probability = useProbabilities ? 0.3f : 1f;
        this.featureToggleService = featureToggleService;
    }

    private final String[] goodMsg = {"Да ладно, %s опять пошутил! ",
            "Остановите его! Снова юмор! "};
    private final String[] badMsg = {"%s, теряешь хватку. ",
            "Как то не очень, сорри. ", "Очень плохо %s... "};
    private final String[] goodSuffix = {"И это уже ", "",
            "Счетчик улетает в космос! "};
    private final String[] badSuffix = {"Давай, соберись. ",
            "Попробуй еще раз, что-ли... "};
    private final String goodEnd = " раз за день! ";
    private final String motivation = "Еще чуть-чуть, и ты выйдешь в плюс!";
    private final Function<Long, String> badEnd = l -> format(
            "Счетчик опустился до %d =\\", l);
    private final String voiceMessageReply = "Пошел нахуй.";
    private final String ukrainianPhrase = "слава украине";
    private final String revertedUkrainianPhrase = "украине слава";
    private final String ukrainianReplyPhrase = "Героям слава!";
    private final String[] belarusPhrases = {"беларуссия", "беларусии",
            "беларусия", "белорусия", "белоруссия", "беларуссией"};
    private final String[] belarusReplyPhrases = {"Беларусь!",
            "Беларусь, блядь!", "Беларусь, сука!"};
    private final String daPattern = "^д[aа]+[^a-zа-яё]*?$";
    private final String netPattern = "^н[еe]+т[^a-zа-яё0-9]*?$";
    private final String daStickerFileId = "CAACAgIAAxkBAAMDX7bMJOFQgcyoFHREeFGqJRAFgqMAAhQAAwqqXhcZv25vek7HrR4E";
    private final String ukraineStickerFileId = "CAACAgIAAxkBAAIdzl_XhJ0ZpBgkFwUikvcywOBcnTpcAAJDAAN46JAT00Q3cg6EdRceBA";

    private Set<Long> chatIds;

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage()) {
                Message message = update.getMessage();
                final String messageText = message.getText();

                if (message.hasText() && messageText.startsWith("/configure") && userIsAdmin(message.getChatId(), message.getFrom())) {
                    Map<Feature, Boolean> status = getFeaturesStatus(message.getChatId());
                    execute(SendMessage.builder()
                            .chatId(message.getChatId().toString())
                            .text("Выберите фичу которую вы хотите включить/выключить")
                            .replyMarkup(InlineKeyboardMarkup.builder().keyboard(buildKeyboard(status)).build())
                            .build());
                }

                if (message.hasText()
                        && messageText.toLowerCase().matches(daPattern)
                        && featureToggleService.isEnabled(Feature.DA, update.getMessage().getChatId())) {
                    executeWithProbability(probability, () -> sendSticker(daStickerFileId, message.getChatId(), message.getMessageId()));
                }

                if (message.hasText()
                        && messageText.toLowerCase().trim().matches(netPattern)
                        && featureToggleService.isEnabled(Feature.NET, update.getMessage().getChatId())
                ) {
                    executeWithProbability(probability, () -> sendMsg("Пидора ответ.", message.getChatId(), message));
                }

                if (message.hasText() && (
                        messageText.toLowerCase().contains(ukrainianPhrase)
                                || messageText.toLowerCase()
                                .contains(revertedUkrainianPhrase))) {
                    executeAnyRandomly(
                            () -> sendMsg(ukrainianReplyPhrase, message.getChatId(), message),
                            () -> sendSticker(ukraineStickerFileId, message.getChatId(), message.getMessageId())
                    );
                    return;
                }

                if (message.hasVoice() || message.hasVideoNote()) {
                    sendMsg(voiceMessageReply, message.getChatId(), message);
                    return;
                }

                if (message.hasText()) {
                    for (String phrase : belarusPhrases) {
                        if (messageText.contains(phrase)) {
                            sendMsg(belarusReplyPhrases[new Random()
                                            .nextInt(belarusReplyPhrases.length)],
                                    message.getChatId(), message);
                            return;
                        }
                    }
                }

                if (chatIds == null) {
                    chatIds = config.getJokers().keySet();
                }
                Optional.ofNullable(message.getReplyToMessage())
                        .filter(m -> chatIds
                                .contains(m.getFrom().getId()))
                        .ifPresent(reply -> {
                            MessageUtils.JokeType jokeType = testStringForKeywords(message.getText());
                            log.info("joke type of {} is {}", message.getText(), jokeType);
                            Long chatId = reply.getFrom().getId();
                            BotConfig.ConfigEntry joker = config.getJokers().get(chatId);
                            switch (jokeType) {
                                case GOOD:
                                    if (jokesCache
                                            .save(chatId, reply.getMessageId(),
                                                    reply.getText(), true)) {
                                        sendMsg(getText(chatId, true),
                                                message.getChatId());
                                    }
                                    break;
                                case BAD:
                                    if (joker.getCanBeDisliked() && jokesCache.save(chatId, reply.getMessageId(), reply.getText(), false)) {
                                        sendMsg(getText(chatId, false),
                                                message.getChatId());
                                    } else if (!joker.getCanBeDisliked()) {
                                        sendMsg("Этот человек неприкасаемый, епта.", message.getChatId());
                                    }
                                    break;
                                case UNKNOWN:
                                default:
                                    break;
                            }
                        });
            } else if (update.hasCallbackQuery()) {
                CallbackQuery query = update.getCallbackQuery();
                if (query.getData().contains("toggle_") && userIsAdmin(query.getMessage().getChatId(), query.getFrom())) {
                    Long chatId = query.getMessage().getChatId();
                    String featureStr = StringUtils.substringAfter(query.getData(), "toggle_");
                    Feature feature = Feature.valueOf(featureStr);
                    featureToggleService.toggleFeature(feature, chatId);
                    execute(AnswerCallbackQuery.builder().callbackQueryId(query.getId()).build());
                    execute(EditMessageReplyMarkup.builder()
                            .chatId(query.getMessage().getChatId().toString())
                            .messageId(query.getMessage().getMessageId())
                            .replyMarkup(InlineKeyboardMarkup.builder().keyboard(buildKeyboard(getFeaturesStatus(query.getMessage().getChatId()))).build())
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("Error processing message", e);
        }
    }

    private Map<Feature, Boolean> getFeaturesStatus(Long chatId) {
        return EnumSet.allOf(Feature.class)
                .stream()
                .map(it -> Pair.of(it, featureToggleService.isEnabled(it, chatId)))
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
    }

    private Collection<? extends List<InlineKeyboardButton>> buildKeyboard(Map<Feature, Boolean> status) {
        return status.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(it -> InlineKeyboardButton.builder()
                        .text(it.getKey().humanReadable + " " + (it.getValue() ? "✅" : "❌"))
                        .callbackData("toggle_" + it.getKey())
                        .build())
                .map(Collections::singletonList)
                .collect(Collectors.toList());
    }

    @SneakyThrows
    private boolean userIsAdmin(Long chatId, User from) {
        ArrayList<ChatMember> admins = execute(new GetChatAdministrators() {{
            setChatId(chatId.toString());
        }});
        Optional<ChatMember> chatMemberOptional = admins.stream().filter(it -> it.getUser().getId().equals(from.getId())).findFirst();
        if (chatMemberOptional.isPresent()) {
            ChatMember admin = chatMemberOptional.get();
            return admin.getStatus().equals("creator") || admin.getCanChangeInfo();
        }
        return false;
    }

    private void sendMsg(String s,
            Long chatId
    ) {
        log.info("send message {}", s);
        SendMessage sendMessage = new SendMessage(chatId.toString(), s){{enableMarkdown(true);}};
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Exception: ", e);
        }
    }

    private void sendSticker(String stickerId, Long chatId, Integer messageId) {
       log.info("send sticker {}", stickerId);
       SendSticker sendSticker = new SendSticker() {{
           setChatId(chatId.toString());
           setSticker(new InputFile(stickerId));
           setReplyToMessageId(messageId);
       }};
       try {
           execute(sendSticker);
       } catch (TelegramApiException e) {
           log.error("Exception: ", e);
       }
    }

    private void sendMsg(String s,
            Long chatId,
            Message replyMsg
    ) {
        log.info("send message {}", s);
        SendMessage sendMessage = new SendMessage(chatId.toString(), s){{enableMarkdown(true);}};
        sendMessage.setReplyToMessageId(replyMsg.getMessageId());
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Exception: ", e);
        }
    }

    private String getText(Long chatId,
            boolean good
    ) {
        String msg;
        String suf;
        String end;
        long count = jokesCache.getCount(chatId, good);
        List<String> names = config.getJokers().get(chatId).getNames();
        if (good) {
            msg = format(goodMsg[current().nextInt(goodMsg.length)],
                    names.get(current().nextInt(names.size())));
            suf = goodSuffix[current().nextInt(goodSuffix.length)];
            end = count + goodEnd;
            if (count < 0) {
                end += motivation;
            }
        } else {
            msg = format(badMsg[current().nextInt(badMsg.length)],
                    names.get(current().nextInt(names.size())));
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
