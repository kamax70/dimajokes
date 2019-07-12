package ru.dimajokes;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertSame;
import static ru.dimajokes.MessageUtils.testStringForKeywords;

@RunWith(JUnit4.class)
public class KeywordsTest {

    @Test
    public void lolTest() {
        assertSame(MessageUtils.JokeType.GOOD, testStringForKeywords("лол кек блять"));
    }

    @Test
    public void kekTest() {
        assertSame(MessageUtils.JokeType.GOOD, testStringForKeywords("кек блять"));
    }

    @Test
    public void smeshnoTest() {
        assertSame(MessageUtils.JokeType.GOOD, testStringForKeywords("смешно)))"));
    }

    @Test
    public void smishnoTest() {
        assertSame(MessageUtils.JokeType.GOOD, testStringForKeywords("смишно)))"));
    }

    @Test
    public void neSmeshnoTest() {
        assertSame(MessageUtils.JokeType.BAD, testStringForKeywords("не смешно("));
    }

    @Test
    public void neOchenSmeshnoTest() {
        assertSame(MessageUtils.JokeType.BAD, testStringForKeywords("не очень смешно("));
    }

    @Test
    public void weirdTest() {
        assertSame(MessageUtils.JokeType.GOOD, testStringForKeywords("смишно не смешно("));
    }

    @Test
    public void weird2Test() {
        assertSame(MessageUtils.JokeType.BAD, testStringForKeywords("не смешно( но смешно"));
    }

    @Test
    public void weird3Test() {
        assertSame(MessageUtils.JokeType.GOOD, testStringForKeywords("смешно смешно"));
    }

    @Test
    public void negativeTest() {
        assertSame(MessageUtils.JokeType.UNKNOWN, testStringForKeywords("блять"));
    }

}
