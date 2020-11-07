package com.github.gpaddons.gpclaimexpiration;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
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

        // Disable GriefPrevention's claim cleanup system.
        disableGPClaimExpiration();

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
        getServer().getScheduler().runTaskLaterAsynchronously(this, new EvaluationRunnable(this), 300L);
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
    public long getProtectionDuration(Claim claim)
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
    public long getLastQualifyingSession(OfflinePlayer player)
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
    public boolean isExempt(OfflinePlayer player)
    {
        if (player.isOnline()) return true;

        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId());

        if (exceedsConfigInt("expiration.bypass.claim_blocks", playerData::getAccruedClaimBlocks)) return true;

        if (exceedsConfigInt("expiration.bypass.bonus_claim_blocks", playerData::getBonusClaimBlocks)) return true;

        return getConfig().getStringList("expiration.bypass.permissions").stream()
                .anyMatch(permission -> vault.hasPermission(player, permission));
    }

    private void disableGPClaimExpiration() {
        // Attempt to cancel all of GP's claim cleanup tasks.
        try
        {
            Set<Object> taskClasses = new HashSet<>();
            taskClasses.add(Class.forName("me.ryanhamshire.GriefPrevention.CleanupUnusedClaimPreTask"));
            taskClasses.add(Class.forName("me.ryanhamshire.GriefPrevention.CleanupUnusedClaimTask"));

            Class<?> craftTask = Class.forName(getServer().getClass().getPackage().getName() + ".scheduler.CraftTask");
            Method getTaskClass = craftTask.getDeclaredMethod("getTaskClass");
            getTaskClass.setAccessible(true);

            Set<BukkitTask> tasks = new HashSet<>();

            Consumer<Object> taskListConsumer = object ->
            {
                if (!(object instanceof Collection)) return;

                Collection<?> collection = (Collection<?>) object;

                for (Object runnable : collection)
                {
                    if (!craftTask.isInstance(runnable)) continue;

                    try
                    {
                        if (taskClasses.contains(getTaskClass.invoke(runnable)))
                        {
                            tasks.add((BukkitTask) runnable);
                        }
                    }
                    catch (IllegalAccessException | InvocationTargetException e)
                    {
                        getLogger().log(Level.WARNING, "Unable to cancel GriefPrevention's claim expiration tasks!", e);
                    }
                }
            };

            final BukkitScheduler scheduler = getServer().getScheduler();

            final Class<? extends @NotNull BukkitScheduler> craftScheduler = scheduler.getClass();

            for (String listName : new String[] { "pending", "temp" })
            {
                final Field fieldTaskList = craftScheduler.getDeclaredField(listName);
                fieldTaskList.setAccessible(true);

                taskListConsumer.accept(fieldTaskList.get(scheduler));
            }

            tasks.forEach(BukkitTask::cancel);

        }
        catch (ReflectiveOperationException e)
        {
            getLogger().log(Level.WARNING, "Unable to cancel GriefPrevention's claim expiration tasks!", e);
        }

        // Configure GP's claim expiration checks to run as infrequently as possible.
        GriefPrevention.instance.config_claims_expirationDays = Integer.MAX_VALUE;
        GriefPrevention.instance.config_claims_expirationExemptionTotalBlocks = Integer.MAX_VALUE;
        GriefPrevention.instance.config_claims_expirationExemptionBonusBlocks = Integer.MAX_VALUE;
        GriefPrevention.instance.config_advanced_claim_expiration_check_rate = Integer.MAX_VALUE;
        GriefPrevention.instance.config_claims_chestClaimExpirationDays = Integer.MAX_VALUE;
        GriefPrevention.instance.config_claims_unusedClaimExpirationDays = Integer.MAX_VALUE;
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

    @NotNull List<String> getCommandList(@NotNull String key, @NotNull OfflinePlayer player, @NotNull Location location)
    {
        List<String> list = getConfig().getStringList(key);

        if (list.isEmpty()) return list;

        String playerName = player.getName() != null ? player.getName() : player.getUniqueId().toString();
        String playerUUID = player.getUniqueId().toString();
        String world = location.getWorld() == null ? "Unloaded World" : location.getWorld().getName();

        return list.stream().map(command -> command
                .replace("$playerName", playerName).replace("$playerUUID", playerUUID).replace("$world", world)
                .replace("$locX", String.valueOf(location.getBlockX())).replace("$locY", String.valueOf(location.getBlockY())).replace("$locZ", String.valueOf(location.getBlockZ()))
        ).collect(Collectors.toList());
    }

}
