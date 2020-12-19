package com.github.gpaddons.util.lang.replacement;

import com.github.gpaddons.util.lang.CommonMessage;
import com.github.gpaddons.util.lang.Lang;
import com.github.gpaddons.util.lang.MessageReplacement;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * MessageReplacement providing variables representing the owner of an object.
 * The owner may be null, indicating administrative ownership.
 *
 * <p>Supports $Id and $Name.
 *
 * <p>By default, values are prefixed with "owner" (i.e. $ownerId, $ownerName) but may have differing
 * prefixes to support multiple replacements in a single string (i.e. $previousOwnerId, $nextOwnerName)
 */
public class OwnerReplacement implements MessageReplacement
{

    private final @NotNull String replaceId;
    private final @NotNull String replaceName;
    private final String uuidVal;
    private final @NotNull String nameVal;

    public OwnerReplacement(@NotNull OfflinePlayer player)
    {
        this("owner", player);
    }

    public OwnerReplacement(@NotNull String prefix, @NotNull OfflinePlayer player)
    {
        this(prefix, player.getUniqueId(), player);
    }

    public OwnerReplacement(@Nullable UUID uuid)
    {
        this("owner", uuid);
    }

    public OwnerReplacement(@NotNull String prefix, @Nullable UUID uuid)
    {
        this(prefix, uuid, uuid == null ? null : Bukkit.getOfflinePlayer(uuid));
    }

    private OwnerReplacement(@NotNull String prefix, @Nullable UUID uuid, @Nullable OfflinePlayer player)
    {
        if (prefix.isEmpty()) throw new IllegalArgumentException("Prefix may not be empty.");

        this.replaceId = '$' + prefix + "Id";
        this.replaceName = '$' + prefix + "Name";

        if (uuid == null)
        {
            this.uuidVal = new UUID(0, 0).toString();
        }
        else
        {
            this.uuidVal = uuid.toString();
        }

        if (player == null)
        {
            this.nameVal = Lang.get(CommonMessage.ADMIN);
        }
        else
        {
            nameVal = Lang.getName(player);
        }
    }

    @Override
    public @NotNull String replace(@NotNull String value)
    {
        value = value.replace(replaceId, uuidVal);
        value = value.replace(replaceName, nameVal);
        return value;
    }

}
