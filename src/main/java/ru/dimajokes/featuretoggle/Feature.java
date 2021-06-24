package ru.dimajokes.featuretoggle;

public enum Feature {
    DA("да - пизда"), NET("нет - пидора ответ");

    public String humanReadable;

    Feature(String humanReadable) {
        this.humanReadable = humanReadable;
    }
}
