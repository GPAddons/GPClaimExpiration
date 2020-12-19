package com.github.gpaddons.util.lang;

import com.github.gpaddons.gpclaimexpiration.GPClaimExpiration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.UUID;
import java.util.logging.Level;

/**
 * A basic translation manager.
 */
public final class Lang
{

    private static final String LANG_FILE = "lang.yml";
    private static final @NotNull YamlConfiguration LANG;

    static
    {
        GPClaimExpiration plugin = GPClaimExpiration.getPlugin(GPClaimExpiration.class);
        File file = new File(plugin.getDataFolder(), LANG_FILE);

        if (!file.exists())
            write(plugin, file);

        LANG = YamlConfiguration.loadConfiguration(file);
    }

    private static void write(@NotNull Plugin plugin, @NotNull File file)
    {
        try (InputStream resource = plugin.getResource(LANG_FILE))
        {
            if (resource == null)
            {
                plugin.getLogger().log(Level.WARNING, () -> "Unable to load resource " + LANG_FILE);
                return;
            }

            Files.copy(resource, file.toPath());
        }
        catch (IOException e)
        {
            plugin.getLogger().log(Level.WARNING, e.getMessage(), e);
        }
    }

    /**
     * Check if a {@link Message} is set to a non-null and non-empty value.
     *
     * @param message the Message
     * @return true if the message has content
     */
    public static boolean isSet(@NotNull Message message)
    {
        String value = get(message);
        return value != null && !value.isEmpty();
    }

    private static @Nullable String get(@NotNull Message message)
    {
        return LANG.getString(message.getKey(), null);
    }

    /**
     * Get the value for a {@link ComponentMessage}.
     *
     * <p>Because component messages are used in replacement, they are never null.
     *
     * @param message the ComponentMessage
     * @return the value set or the default if unset
     */
    public static @NotNull String get(@NotNull ComponentMessage message)
    {
        String value = LANG.getString(message.getKey(), null);
        return value != null && !value.isEmpty() ? value : message.getDefault();
    }

    /**
     * Send a {@link Message} to the specified CommandSender after applying any variable replacements.
     *
     * <p>Messages that are configured to be null or blank are not sent.
     *
     * @param recipient the recipient of the messagee
     * @param message the Message to send
     * @param replacements the variable replacement providers
     */
    public static void sendMessage(@NotNull CommandSender recipient, @NotNull Message message, MessageReplacement @NotNull ... replacements)
    {
        String value = get(message);

        if (value == null || value.isEmpty()) return;

        for (MessageReplacement replacement : replacements) {
            value = replacement.replace(value);
        }

        recipient.sendMessage(value);
    }

    /**
     * Get a name for a UUID.
     *
     * <p>If the UUID is null, the name is the configured administrator name message.
     * @see #getName(OfflinePlayer)
     *
     * @param uuid the UUID
     * @return the name for the UUID
     */
    public static @NotNull String getName(@Nullable UUID uuid)
    {
        if (uuid == null) return get(CommonMessage.ADMIN);

        return getName(Bukkit.getOfflinePlayer(uuid));
    }

    /**
     * Get a name for an OfflinePlayer.
     *
     * <p>If the player has been removed from the user cache, the name will be the configured unnamed player message.
     *
     * @param offlinePlayer the player
     * @return the name of the player
     */
    public static @NotNull String getName(@NotNull OfflinePlayer offlinePlayer)
    {
        String name = offlinePlayer.getName();

        if (name != null) return name;

        return get(CommonMessage.UNNAMED_PLAYER).replace("$uuid", offlinePlayer.getUniqueId().toString());
    }

    private Lang() {}

}
