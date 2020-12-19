package com.github.gpaddons.gpclaimexpiration.lang;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Enum containing message keys for translation.
 */
public enum Message implements com.github.gpaddons.util.lang.Message
{

    NOTIFICATION_EXPIRATION;

    private final @NotNull String key;

    Message() {
        key = name().toLowerCase(Locale.ENGLISH).replace('_', '.');
    }

    @Override
    public @NotNull String getKey() {
        return this.key;
    }

}
