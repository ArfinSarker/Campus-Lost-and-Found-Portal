package com.sas.lostandfound;

public class Country {
    private final String name;
    private final String code;
    private final String flagEmoji;
    private final String shortCode;

    public Country(String name, String code, String flagEmoji, String shortCode) {
        this.name = name;
        this.code = code;
        this.flagEmoji = flagEmoji;
        this.shortCode = shortCode;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public String getFlagEmoji() {
        return flagEmoji;
    }

    public String getShortCode() {
        return shortCode;
    }
}
