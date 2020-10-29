package com.github.gpaddons.gpclaimexpiration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Enum representing evaluation rate types.
 */
enum EvaluationType
{
    PERCENT, COUNT;

    public static @NotNull EvaluationType of(@Nullable String value)
    {
        if (value == null) {
            return PERCENT;
        }
        try
        {
            return valueOf(value.toUpperCase(Locale.ENGLISH));
        }
        catch (IllegalArgumentException e)
        {
            return PERCENT;
        }
    }

}
