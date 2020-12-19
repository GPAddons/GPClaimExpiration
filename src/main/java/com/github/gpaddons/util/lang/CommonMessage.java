package com.github.gpaddons.util.lang;

import org.jetbrains.annotations.NotNull;

/**
 * An enum for common {@link ComponentMessage ComponentMessages}.
 */
public enum CommonMessage implements ComponentMessage
{
    ADMIN("general.admin", "an administrator"),
    UNKNOWN_WORLD("general.unknown_world", "unknown world"),
    UNNAMED_PLAYER("general.unnamed_player", "someone ($uuid)");

    private final String key;
    private final String defaultVal;

    CommonMessage(String key, String defaultVal)
    {
        this.key = key;
        this.defaultVal = defaultVal;
    }

    @Override
    public @NotNull String getKey()
    {
        return this.key;
    }

    @Override
    public @NotNull String getDefault()
    {
        return defaultVal;
    }

}
