package ru.dimajokes;

import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Sets.newHashSet;
import static java.lang.Math.min;
import static java.util.Arrays.stream;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;

public class MessageUtils {

    private static final Set<String> goodPrefix = newHashSet("хорошая", "пиздатая", "крутая", "клевая", "клёвая", "смешная");
    private static final Set<String> badPrefix = newHashSet("хуевая", "хуёвая", "дерьмовая", "плохая", "ниоч", "хуйня");

    public enum JokeType {
        GOOD, BAD, UNKNOWN
    }

    public static JokeType testStringForKeywords(String text) {
        JokeType result = containsIgnoreCase(text, "лол")
                || containsIgnoreCase(text, "кек")
                || containsIgnoreCase(text, "хаха") ? JokeType.GOOD : JokeType.UNKNOWN;
        if (result == JokeType.UNKNOWN) {
            OptionalInt min = Stream.of("смешно", "смишно")
                    .map(s -> determineNonNegative(text, s))
                    .mapToInt(Enum::ordinal)
                    .filter(i -> i < JokeType.UNKNOWN.ordinal())
                    .min();
            if (min.isPresent()) {
                return JokeType.values()[min.getAsInt()];
            }
            Optional<JokeType> jokeKeyword = determineJokeKeyword(text);
            if (jokeKeyword.isPresent()) {
                return jokeKeyword.get();
            }
        }
        return result;
    }

    // определяем шо там по слову "шутка"
    private static Optional<JokeType> determineJokeKeyword(String text) {
        String[] keywords = {"шутка", "шутейка"};
        Optional<String> first = stream(keywords)
                .map(s -> Pair.of(containsIgnoreCase(text, s), s))
                .filter(Pair::getKey)
                .map(Pair::getValue)
                .findFirst();
        if (first.isPresent()) {
            String keyword = first.get();
            String[] wordsArr = text.split(" ");
            if (wordsArr.length == 1) return Optional.empty();
            List<String> words = stream(wordsArr).map(MessageUtils::keepOnlyAlphabetical).collect(Collectors.toList());
            int i = words.indexOf(keyword);
            if (i == 0) return Optional.empty();
            String previousWord = words.get(i - 1).toLowerCase();
            if (goodPrefix.contains(previousWord)) {
                return Optional.of(JokeType.GOOD);
            } else if (badPrefix.contains(previousWord)) {
                return Optional.of(JokeType.BAD);
            }
        }
        return Optional.empty();
    }

    private static JokeType determineNonNegative(String text, String keyword) {
        if (containsIgnoreCase(text, keyword)) {
            String[] wordsArr = text.split(" ");
            if (wordsArr.length == 1) return JokeType.GOOD;
            List<String> words = stream(wordsArr).map(MessageUtils::keepOnlyAlphabetical).collect(Collectors.toList());
            int i = words.indexOf(keyword);
            if (i == 0) return JokeType.GOOD;
            words = words.subList(0, i);
            Collections.reverse(words);
            for (int j = 0; j < min(2, words.size()); ++j) {
                if ("не".equalsIgnoreCase(words.get(j))) return JokeType.BAD;
            }
            return JokeType.GOOD;
        }
        return JokeType.UNKNOWN;
    }

    private static String keepOnlyAlphabetical(String s) {
        return s.replaceAll("[^А-Яа-я]", "");
    }

}
