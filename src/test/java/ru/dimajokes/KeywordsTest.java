package ru.dimajokes;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.dimajokes.MessageUtils.testStringForKeywords;

@RunWith(JUnit4.class)
public class KeywordsTest {

    @Test
    public void lolTest() {
        assertTrue(testStringForKeywords("лол кек блять"));
    }

    @Test
    public void kekTest() {
        assertTrue(testStringForKeywords("кек блять"));
    }

    @Test
    public void smeshnoTest() {
        assertTrue(testStringForKeywords("смешно)))"));
    }

    @Test
    public void smishnoTest() {
        assertTrue(testStringForKeywords("смишно)))"));
    }

    @Test
    public void neSmeshnoTest() {
        assertFalse(testStringForKeywords("не смешно("));
    }

    @Test
    public void neOchenSmeshnoTest() {
        assertFalse(testStringForKeywords("не очень смешно("));
    }

    @Test
    public void negativeTest() {
        assertFalse(testStringForKeywords("блять"));
    }

}
