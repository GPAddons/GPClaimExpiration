package com.github.gpaddons.util.lang.replacement;

import com.github.gpaddons.util.lang.CommonMessage;
import com.github.gpaddons.util.lang.Lang;
import com.github.gpaddons.util.lang.MessageReplacement;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * MessageReplacement providing variables representing a {@link Location}.
 *
 * <p>Supports $world, $locX, $locY, and $locZ.
 *
 * <p>Axial representations are whole numbers and may have their variables suffixed
 * to distingish multiple replacements in a single string (i.e. $locXMinimum).
 */
public class LocationReplacement implements MessageReplacement
{
    private final @NotNull String suffix;
    private final @NotNull Location location;

    public LocationReplacement(@NotNull Location location)
    {
        this("", location);
    }

    public LocationReplacement(@NotNull String suffix, @NotNull Location location)
    {
        this.suffix = suffix;
        this.location = location;
    }

    @Override
    public @NotNull String replace(@NotNull String value)
    {
        String worldName;
        if (location.isWorldLoaded())
        {
            worldName = Objects.requireNonNull(location.getWorld()).getName();
        }
        else
        {
            worldName = Lang.get(CommonMessage.UNKNOWN_WORLD);
        }

        value = value.replace("$world", worldName);
        value = value.replace("$locX" + suffix, String.valueOf(location.getBlockX()));
        value = value.replace("$locY" + suffix, String.valueOf(location.getBlockY()));
        value = value.replace("$locZ" + suffix, String.valueOf(location.getBlockZ()));

        return value;
    }

}
