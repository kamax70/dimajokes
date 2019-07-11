package ru.dimajokes;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Math.min;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;

public class MessageUtils {

    public static boolean testStringForKeywords(String text) {
        return containsIgnoreCase(text, "лол")
                || containsIgnoreCase(text, "кек")
                || containsIgnoreCase(text, "хаха")
                || containsIgnoreCase(text, "ха-ха")
                || determineNonNegative(text, "смешно")
                || determineNonNegative(text, "смишно");
    }

    private static boolean determineNonNegative(String text, String keyword) {
        if (containsIgnoreCase(text, keyword)) {
            keyword = keepOnlyAlphabetical(keyword);
            String[] wordsArr = text.split(" ");
            if (wordsArr.length == 1) return true;
            List<String> words = Arrays.stream(wordsArr).map(MessageUtils::keepOnlyAlphabetical).collect(Collectors.toList());
            int i = words.indexOf(keyword);
            if (i == 0) return true;
            words = words.subList(0, i);
            Collections.reverse(words);
            for (int j = 0; j < min(2, words.size()); ++j) {
                if ("не".equalsIgnoreCase(words.get(j))) return false;
            }
            return true;
        }
        return false;
    }

    private static String keepOnlyAlphabetical(String s) {
        return s.replaceAll("[^А-Яа-я]", "");
    }

}
