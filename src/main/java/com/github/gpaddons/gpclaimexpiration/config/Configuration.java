package com.github.gpaddons.gpclaimexpiration.config;

import com.github.gpaddons.gpclaimexpiration.GPClaimExpiration;
import com.github.gpaddons.util.lang.MessageReplacement;
import com.github.jikoo.planarwrappers.config.Setting;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Configuration
{

    private final GPClaimExpiration plugin;
    private final Setting<NavigableMap<Integer, Long>> areaProtectionDuration;

    public Configuration(GPClaimExpiration plugin)
    {
        this.plugin = plugin;


        TreeMap<Integer, Long> defaults = new TreeMap<>();
        defaults.put(250_000, Long.MAX_VALUE);
        defaults.put(10_000, TimeUnit.MILLISECONDS.convert(90, TimeUnit.DAYS));
        defaults.put(1_000, TimeUnit.MILLISECONDS.convert(60, TimeUnit.DAYS));
        defaults.put(0, 0L);

        areaProtectionDuration = new TreeMapSetting<Integer, Long>(plugin.getConfig(), "expiration.days_per_area", defaults)
        {
            @Override
            protected @Nullable Integer convertKey(@NotNull String key)
            {
                try
                {
                    return Integer.parseInt(key);
                }
                catch (NumberFormatException e)
                {
                    plugin.getLogger().warning(String.format("Invalid area size %s - must be a whole number!", key));
                    return null;
                }
            }

            @Override
            protected @NotNull Long convertValue(@NotNull String value)
            {
                try
                {
                    int days = Integer.parseInt(value);

                    if (days < 0) return Long.MAX_VALUE;

                    return TimeUnit.MILLISECONDS.convert(days, TimeUnit.DAYS);
                }
                catch (NumberFormatException e)
                {
                    plugin.getLogger().warning(String.format("Invalid day count %s - must be a whole number!", value));
                    return Long.MAX_VALUE;
                }
            }
        };
    }

    /**
     * Get protection durations in milliseconds mapped to claim area.
     *
     * @return a Map containing durations in milliseconds related to area measurements
     */
    public @NotNull Setting<NavigableMap<Integer, Long>> getProtectionDurations()
    {
        return areaProtectionDuration;
    }

    /**
     * Get the shortest expiration time across all claim areas or {@link Long#MAX_VALUE} if no times are configured.
     *
     * @return the shortest claim expiration
     */
    public long getShortestClaimExpiration()
    {
        Optional<Long> shortestExpirationTime = plugin.getServer().getWorlds().stream()
                .map(world -> areaProtectionDuration.get(world.getName()))
                .flatMap(map -> map.values().stream())
                .sorted()
                .filter(value -> value > 0)
                .findFirst();

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
        World world = claim.getLesserBoundaryCorner().getWorld();

        // Shouldn't be possible, but we'll leave malformed claims for GP to handle.
        if (world == null) return Long.MAX_VALUE;

        final Map.Entry<Integer, Long> areaProtection = areaProtectionDuration.get(world.getName()).floorEntry(claim.getArea());

        return areaProtection != null ? areaProtection.getValue() : Long.MAX_VALUE;
    }

    /**
     * Check whether a player is exempt from expiration.
     *
     * @param player the OfflinePlayer to check
     * @return true if the player is exempt from expiration
     */
    public boolean isExempt(@NotNull OfflinePlayer player)
    {
        if (player.isOnline()) return true;

        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId());

        // TODO: per-world
        if (exceedsConfigInt("expiration.bypass.claim_blocks", playerData::getAccruedClaimBlocks)) return true;

        // TODO: per-world
        if (exceedsConfigInt("expiration.bypass.bonus_claim_blocks", playerData::getBonusClaimBlocks)) return true;

        // TODO: per-world
        return plugin.getConfig().getStringList("expiration.bypass.permissions").stream()
                .anyMatch(permission -> plugin.getPermissionBridge().hasPermission(player, permission));
    }

    private boolean exceedsConfigInt(@NotNull String path, @NotNull Supplier<Integer> integerSupplier)
    {
        int configValue = plugin.getConfig().getInt(path, -1);

        if (configValue < 0) return false;

        return configValue <= integerSupplier.get();
    }

    public @NotNull List<String> getCommandList(@NotNull String key, MessageReplacement @NotNull ... replacements)
    {
        // TODO: per-world
        List<String> list = plugin.getConfig().getStringList(key);

        if (list.isEmpty()) return list;

        return list.stream().map(command -> {
            for (MessageReplacement replacement : replacements) {
                command = replacement.replace(command);
            }
            return command;
        }).collect(Collectors.toList());
    }

}
