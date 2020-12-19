package com.github.gpaddons.util.lang;

import org.jetbrains.annotations.NotNull;

/**
 * Interface representing a customizable message.
 */
public interface Message
{

    /**
     * Get the configuration key of the message.
     *
     * @return the configuration key
     */
    @NotNull String getKey();

}
