package com.github.gpaddons.gpclaimexpiration;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.events.ClaimExpirationEvent;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Runnable for evaluating and expiring
 */
class EvaluationRunnable implements Runnable
{

    private final GPClaimExpiration plugin;
    private final Random random;
    private Set<UUID> players;

    EvaluationRunnable(@NotNull GPClaimExpiration plugin)
    {
        this.plugin = plugin;
        this.random = new Random();
    }

    @Override
    public void run()
    {
        if (players == null || players.isEmpty()) players = refreshPlayers();

        checkNextPlayer();

        scheduleNextRun();
    }

    private Set<UUID> refreshPlayers()
    {
        final Future<Set<UUID>> playersFuture = plugin.getServer().getScheduler().callSyncMethod(plugin, () ->
                GriefPrevention.instance.dataStore.getClaims().stream().map(claim ->
                {
                    // Always skip child claims - will be revisited by expiration processing.
                    if (claim.parent != null) return null;

                    return claim.ownerID;
                }).filter(Objects::nonNull).collect(Collectors.toSet()));

        try
        {
            return playersFuture.get();
        }
        catch (InterruptedException | ExecutionException e)
        {
            plugin.getLogger().log(Level.WARNING, "Error fetching claim owners' UUIDs from main thread", e);
            return Collections.emptySet();
        }
    }

    private void checkNextPlayer()
    {
        // Ensure players are available.
        if (players == null || players.isEmpty()) return;

        Iterator<UUID> iterator = players.iterator();
        UUID playerUUID = iterator.next();

        // If random order is configured, iterate until the random index is hit.
        if (isRandom())
        {
            int index = random.nextInt(players.size());
            for (int i = 1; i < index && iterator.hasNext(); ++i)
            {
                playerUUID = iterator.next();
            }
        }

        // Remove from list, list will refresh when empty.
        iterator.remove();

        OfflinePlayer player = plugin.getServer().getOfflinePlayer(playerUUID);

        // Ensure player is not exempt from claim expiration.
        if (plugin.isExempt(player)) return;

        long timeSinceLastSession = System.currentTimeMillis() - plugin.getLastQualifyingSession(player);

        // Ensure last qualifying session is before the earliest time any claim could expire.
        if (timeSinceLastSession >= plugin.getShortestClaimExpiration()) return;

        evaluateClaims(player, timeSinceLastSession);
    }

    private void evaluateClaims(OfflinePlayer player, long timeSinceLastSession)
    {
        final Future<Collection<Claim>> playerClaims = plugin.getServer().getScheduler().callSyncMethod(plugin,
                () -> GriefPrevention.instance.dataStore.getClaims().stream()
                        .filter(claim -> claim.parent == null && player.getUniqueId().equals(claim.ownerID))
                        .collect(Collectors.toList()));

        try
        {
            playerClaims.get().forEach(claim -> evaluateClaim(player, claim, timeSinceLastSession));
        }
        catch (InterruptedException | ExecutionException e)
        {
            plugin.getLogger().log(Level.WARNING, String.format("Error fetching claims for %s from main thread", player.getUniqueId()), e);
        }
    }

    private void evaluateClaim(OfflinePlayer player, Claim claim, long timeSinceLastSession)
    {
        if (timeSinceLastSession <= plugin.getProtectionDuration(claim)) return;

        // Don't attempt to schedule if plugin is disabled.
        if (!plugin.isEnabled()) return;

        // Return to main thread - deleting claims is not thread safe.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            // Fire claim expiration event.
            ClaimExpirationEvent event = new ClaimExpirationEvent(claim);
            plugin.getServer().getPluginManager().callEvent(event);

            // Respect event cancellation.
            if (event.isCancelled()) return;

            // Fetch delete commands and add additional placeholders.
            Location max = claim.getGreaterBoundaryCorner();
            List<String> commandList = plugin.getCommandList("expiration.claim.commands", player, claim.getLesserBoundaryCorner())
                    .stream().map(command -> command.replace("$claimId", String.valueOf(claim.getID()))
                            .replace("$locMaxX", String.valueOf(max.getBlockX()))
                            .replace("$locMaxY", String.valueOf(max.getBlockY()))
                            .replace("$locMaxZ", String.valueOf(max.getBlockZ()))).collect(Collectors.toList());

            // Delete claim.
            GriefPrevention.instance.dataStore.deleteClaim(claim, true);

            // Run post-delete commands.
            for (String command : commandList)
            {
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
            }
        });
    }

    private void scheduleNextRun()
    {
        // Don't attempt to schedule if plugin is disabled.
        if (!plugin.isEnabled()) return;

        plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, this, calculateDelay());
    }

    private long calculateDelay()
    {
        // Always wait an hour between runs.
        if (players == null || players.isEmpty()) return 72000;

        if (getTaskEvaluationType() == EvaluationType.PERCENT)
        {
            // Schedule based on percentage per hour. Minimum 1 tick delay.
            return Math.max(1, (long) (72000 / getTaskEvaluationValue() * players.size()));
        }

        // Schedule a fixed number per hour. Minimum 1 tick delay.
        return Math.max(1, (long) (72000 / getTaskEvaluationValue()));
    }

    private boolean isRandom()
    {
        return plugin.getConfig().getBoolean("expiration.evaluation.random");
    }

    private @NotNull EvaluationType getTaskEvaluationType()
    {
        return EvaluationType.of(plugin.getConfig().getString("expiration.evaluation.rate.type"));
    }

    private double getTaskEvaluationValue()
    {
        return Math.max(0.1, plugin.getConfig().getDouble("expiration.evaluation.rate.value", 4.35));
    }

}
