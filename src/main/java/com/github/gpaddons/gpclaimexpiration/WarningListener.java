package com.github.gpaddons.gpclaimexpiration;

import com.github.gpaddons.gpclaimexpiration.lang.Message;
import com.github.gpaddons.util.lang.Lang;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.events.ClaimCreatedEvent;
import me.ryanhamshire.GriefPrevention.events.ClaimModifiedEvent;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * Listener for warning users about claim expiration times.
 */
class WarningListener implements Listener
{

    private final GPClaimExpiration plugin;

    WarningListener(GPClaimExpiration plugin)
    {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onClaim(@NotNull ClaimCreatedEvent event)
    {
        warn(event.getClaim(), event.getCreator());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onClaimResize(@NotNull ClaimModifiedEvent event)
    {
        warn(event.getTo(), event.getModifier());
    }

    private void warn(@NotNull Claim claim, CommandSender modifier)
    {
        // Ensure modification is by a player.
        if (!(modifier instanceof Player)) return;

        // Ensure message is set.
        if (!Lang.isSet(Message.NOTIFICATION_EXPIRATION)) return;

        // Ensure claim is a top level player claim.
        if (claim.ownerID == null || claim.parent != null) return;

        OfflinePlayer player = plugin.getServer().getOfflinePlayer(claim.ownerID);

        long protectionDuration = plugin.getProtectionDuration(claim);

        // Ensure claim is of a size that will actually expire.
        if (protectionDuration == Long.MAX_VALUE) return;

        // Ensure claim will be eligible for delete.
        if (plugin.isExempt(player)) return;

        long days = TimeUnit.DAYS.convert(protectionDuration, TimeUnit.MILLISECONDS);

        plugin.getServer().getScheduler().runTask(plugin, () -> Lang.sendMessage(modifier,
                Message.NOTIFICATION_EXPIRATION, value -> value.replace("$days", String.valueOf(days))));
    }

}
