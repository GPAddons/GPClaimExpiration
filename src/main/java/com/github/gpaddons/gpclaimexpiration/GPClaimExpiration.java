package com.github.gpaddons.gpclaimexpiration;

import com.github.gpaddons.gpclaimexpiration.config.Configuration;
import com.github.gpaddons.gpclaimexpiration.lang.Message;
import com.github.gpaddons.gpclaimexpiration.listener.LegacyWarningListener;
import com.github.gpaddons.gpclaimexpiration.listener.ModernWarningListener;
import com.github.gpaddons.util.lang.Lang;
import com.github.jikoo.planarwrappers.service.VaultPermission;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * A Bukkit plugin for improved expiration of GriefPrevention claims.
 */
public class GPClaimExpiration extends JavaPlugin
{

    private VaultPermission vault;
    private Configuration config;

    @Override
    public void onEnable()
    {
        saveDefaultConfig();

        // Load configured claim durations.
        this.config = new Configuration(this);

        // Unregister existing listeners.
        HandlerList.unregisterAll(this);

        this.vault = new VaultPermission(this);

        // Register listeners.
        getServer().getPluginManager().registerEvents(new UnprotectedPetAbandoner(this), this);

        // Only bother with warning listener if message is set.
        if (Lang.isSet(Message.NOTIFICATION_EXPIRATION))
        {
            try
            {
                Class.forName("me.ryanhamshire.GriefPrevention.events.ClaimResizeEvent");
                getServer().getPluginManager().registerEvents(new ModernWarningListener(this), this);
            }
            catch (ClassNotFoundException e)
            {
                getServer().getPluginManager().registerEvents(new LegacyWarningListener(this), this);
            }
        }

        // Cancel existing tasks.
        getServer().getScheduler().cancelTasks(this);

        // Schedule claim cleanup task.
        new EvaluationManager(this).startScheduling();
    }

    /**
     * Gets a player's last qualifying online session timestamp.
     *
     * @param player the OfflinePlayer to check
     * @return the player's last online
     */
    public long getLastQualifyingSession(@NotNull OfflinePlayer player)
    {
        if (player.isOnline()) return System.currentTimeMillis();

        // FUTURE feature: rolling session system / support Paper's getLastOnline / piggyback PLAN
        return player.getLastPlayed();
    }

    public @NotNull Configuration config()
    {
        return config;
    }

    public VaultPermission getPermissionBridge()
    {
        return vault;
    }

}

