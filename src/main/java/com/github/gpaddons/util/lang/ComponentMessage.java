package com.github.gpaddons.util.lang;

import org.jetbrains.annotations.NotNull;

/**
 * Interface representing a {@link Message} that provides a sane default so as to never be null.
 */
public interface ComponentMessage extends Message
{

    /**
     * Get the default message value.
     *
     * @return the default value
     */
    @NotNull String getDefault();

}
