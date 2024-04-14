package com.github.gpaddons.gpclaimexpiration.config;

import com.github.gpaddons.gpclaimexpiration.GPClaimExpiration;
import com.github.gpaddons.util.lang.MessageReplacement;
import com.github.jikoo.planarwrappers.config.Setting;
import com.github.jikoo.planarwrappers.config.SimpleSetSetting;
import com.github.jikoo.planarwrappers.config.impl.IntSetting;
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
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class Configuration
{

    private final GPClaimExpiration plugin;
    private final Setting<NavigableMap<Integer, Long>> areaProtectionDuration;
    private final Setting<Integer> exemptionClaimBlocks;
    private final Setting<Integer> exemptionBonusClaimBlocks;
    private final Setting<Set<String>> exemptionPermissions;
    private final Setting<List<String>> claimExpirationCommands;
    private final Setting<Integer> petProtectionDuration;
    private final Setting<List<String>> petExpirationCommands;

    public Configuration(GPClaimExpiration plugin)
    {
        this.plugin = plugin;


        TreeMap<Integer, Long> defaults = new TreeMap<>();
        defaults.put(250_000, Long.MAX_VALUE);
        defaults.put(10_000, TimeUnit.MILLISECONDS.convert(90, TimeUnit.DAYS));
        defaults.put(1_000, TimeUnit.MILLISECONDS.convert(60, TimeUnit.DAYS));
        defaults.put(0, 0L);

        areaProtectionDuration = new TreeMapSetting<>(plugin.getConfig(), "expiration.days_per_area", defaults)
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

        exemptionClaimBlocks = new IntSetting(plugin.getConfig(), "expiration.bypass.claim_blocks", -1);
        exemptionBonusClaimBlocks = new IntSetting(plugin.getConfig(), "expiration.bypass.bonus_claim_blocks", -1);
        exemptionPermissions = new SimpleSetSetting<>(plugin.getConfig(), "expiration.bypass.permissions", Set.of("gpclaimexpiration.persist")) {
            @Override
            protected @NotNull String convertValue(@NotNull String value)
            {
                return value;
            }
        };
        claimExpirationCommands = new StringListSetting(plugin.getConfig(), "expiration.claim.commands", List.of());

        petProtectionDuration = new IntSetting(plugin.getConfig(), "expiration.pets.days", 60);
        petExpirationCommands = new StringListSetting(plugin.getConfig(), "expiration.pet.commands", List.of());
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
     * @param worldName the name of the world
     * @return true if the player is exempt from expiration
     */
    public boolean isExempt(@NotNull OfflinePlayer player, @NotNull String worldName)
    {
        if (player.isOnline()) return true;

        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId());

        if (exceedsInt(exemptionClaimBlocks.get(worldName), playerData::getAccruedClaimBlocks)) return true;

        if (exceedsInt(exemptionBonusClaimBlocks.get(worldName), playerData::getBonusClaimBlocks)) return true;

      return exemptionPermissions.get(worldName).stream()
                .anyMatch(permission -> plugin.getPermissionBridge().hasPermission(player, permission, worldName));
    }

    private boolean exceedsInt(int configValue, @NotNull Supplier<Integer> integerSupplier)
    {
        if (configValue < 0) return false;

        return configValue <= integerSupplier.get();
    }

    public @NotNull List<String> getClaimCommandList(
            @NotNull String worldName,
            MessageReplacement @NotNull ... replacements)
    {
        return getCommandList(claimExpirationCommands, worldName, replacements);
    }

    protected @NotNull List<String> getCommandList(
            @NotNull Setting<List<String>> commands,
            @NotNull String worldName,
            MessageReplacement @NotNull ... replacements)
    {
        List<String> list = commands.get(worldName);

        if (list.isEmpty()) return list;

        return list.stream().map(command -> {
            for (MessageReplacement replacement : replacements) {
                command = replacement.replace(command);
            }
            return command;
        }).toList();
    }

    /**
     * Get the number of days before pets are considered abandoned.
     *
     * @param worldName the name of the world
     * @return the number of days until pets are considered abandoned, or -1 for never.
     */
    public int getPetProtectionDuration(@NotNull String worldName) {
        return petProtectionDuration.get(worldName);
    }

    public @NotNull List<String> getPetCommandList(
            @NotNull String worldName,
            MessageReplacement @NotNull ... replacements)
    {
        return getCommandList(petExpirationCommands, worldName, replacements);
    }

}
