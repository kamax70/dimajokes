package ru.dimajokes.featuretoggle;

public enum Feature {
    DA("да"), NET("нет");

    public String humanReadable;

    Feature(String humanReadable) {
        this.humanReadable = humanReadable;
    }
}
