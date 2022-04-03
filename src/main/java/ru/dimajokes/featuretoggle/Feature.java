package ru.dimajokes.featuretoggle;

public enum Feature {
    DA("да - пизда"),
    NET("нет - пидора ответ"),
    UKRAINE_STICKER("стикер \"героям сала\""),
    TOPOR_CLEANER("запрет топора");

    public String humanReadable;

    Feature(String humanReadable) {
        this.humanReadable = humanReadable;
    }
}
