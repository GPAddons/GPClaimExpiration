package com.github.gpaddons.gpclaimexpiration;

import com.github.gpaddons.util.VaultBridge;
import com.github.gpaddons.util.lang.MessageReplacement;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A Bukkit plugin for improved expiration of GriefPrevention claims.
 */
public class GPClaimExpiration extends JavaPlugin
{

    private final VaultBridge vault = new VaultBridge(this);
    private final TreeMap<Integer, Long> areaProtectionDuration = new TreeMap<>();

    @Override
    public void onEnable()
    {
        saveDefaultConfig();

        // Load configured claim durations.
        loadAreaDurations();

        // Unregister existing listeners.
        HandlerList.unregisterAll(this);

        // Register listeners.
        getServer().getPluginManager().registerEvents(vault, this);
        getServer().getPluginManager().registerEvents(new UnprotectedPetAbandoner(this), this);

        // Cancel existing tasks.
        getServer().getScheduler().cancelTasks(this);

        // Schedule claim cleanup task.
        new EvaluationManager(this).startScheduling();
    }

    /**
     * Get protection durations in milliseconds mapped to claim area.
     *
     * @return a Map containing durations in milliseconds related to area measurements
     */
    public @NotNull Map<Integer, Long> getProtectionDurations()
    {
        return Collections.unmodifiableMap(areaProtectionDuration);
    }

    /**
     * Get the shortest expiration time across all claim areas or {@link Long#MAX_VALUE} if no times are configured.
     *
     * @return the shortest claim expiration
     */
    public long getShortestClaimExpiration()
    {
        Optional<Long> shortestExpirationTime = areaProtectionDuration.values().stream().sorted().findFirst();

        return shortestExpirationTime.orElse(Long.MAX_VALUE);
    }

    /**
     * Get the expiration time for the closest configured area equal to or less than the Claim's area.
     * If no times are available at or below the area, {@link Long#MAX_VALUE} is returned instead.
     *
     * @return the Claim's protection duration
     */
    public long getProtectionDuration(@NotNull Claim claim)
    {
        final Map.Entry<Integer, Long> areaProtection = areaProtectionDuration.floorEntry(claim.getArea());

        return areaProtection != null ? areaProtection.getValue() : Long.MAX_VALUE;
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

    /**
     * Check whether or not a player is exempt from expiration.
     *
     * @param player the OfflinePlayer to check
     * @return true if the player is exempt from expiration
     */
    public boolean isExempt(@NotNull OfflinePlayer player)
    {
        if (player.isOnline()) return true;

        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId());

        if (exceedsConfigInt("expiration.bypass.claim_blocks", playerData::getAccruedClaimBlocks)) return true;

        if (exceedsConfigInt("expiration.bypass.bonus_claim_blocks", playerData::getBonusClaimBlocks)) return true;

        return getConfig().getStringList("expiration.bypass.permissions").stream()
                .anyMatch(permission -> vault.hasPermission(player, permission));
    }

    private void loadAreaDurations()
    {
        areaProtectionDuration.clear();

        ConfigurationSection areaDays = getConfig().getConfigurationSection("expiration.days_per_area");

        if (areaDays == null) return;

        for (String areaString : areaDays.getKeys(false))
        {
            int volume;

            // Parse volume.
            try
            {
                volume = Integer.parseInt(areaString);
            }
            catch (NumberFormatException e)
            {
                getLogger().warning(String.format("Invalid area size %s - must be a whole number!", areaString));
                continue;
            }

            // Get configured duration.
            long duration = areaDays.getLong(areaString, -1);

            // Treat values < 0 as eternal.
            if (duration < 0)
                duration = Long.MAX_VALUE;
            else
                duration = TimeUnit.MILLISECONDS.convert(duration, TimeUnit.DAYS);

            areaProtectionDuration.put(volume, duration);
        }

    }

    private boolean exceedsConfigInt(@NotNull String path, @NotNull Supplier<Integer> integerSupplier)
    {
        int configValue = getConfig().getInt(path, -1);

        if (configValue < 0) return false;

        return configValue <= integerSupplier.get();
    }

    @NotNull List<String> getCommandList(@NotNull String key, MessageReplacement @NotNull ... replacements)
    {
        List<String> list = getConfig().getStringList(key);

        if (list.isEmpty()) return list;

        return list.stream().map(command -> {
            for (MessageReplacement replacement : replacements) {
                command = replacement.replace(command);
            }
            return command;
        }).collect(Collectors.toList());
    }

}

