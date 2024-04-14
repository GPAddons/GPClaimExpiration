package com.github.gpaddons.gpclaimexpiration.listener;

import com.github.gpaddons.gpclaimexpiration.GPClaimExpiration;
import com.github.gpaddons.gpclaimexpiration.lang.Message;
import com.github.gpaddons.util.lang.Lang;
import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Base listener for warning users about claim expiration times.
 */
abstract class WarningListener implements Listener
{

    private final GPClaimExpiration plugin;

    WarningListener(GPClaimExpiration plugin)
    {
        this.plugin = plugin;
    }

    void warn(@NotNull Claim claim, @Nullable UUID modifier)
    {
        if (modifier == null) return;

        warn(claim, plugin.getServer().getPlayer(modifier));
    }

    void warn(@NotNull Claim claim, @Nullable CommandSender modifier)
    {
        // Ensure modification is by a player.
        if (!(modifier instanceof Player playerModifier)) return;

        // Ensure message is set.
        if (!Lang.isSet(Message.NOTIFICATION_EXPIRATION)) return;

        // Ensure claim is a top level player claim.
        if (claim.ownerID == null || claim.parent != null) return;

        OfflinePlayer player = plugin.getServer().getOfflinePlayer(claim.ownerID);

        long protectionDuration = plugin.config().getProtectionDuration(claim);

        // Ensure claim is of a size that will actually expire.
        if (protectionDuration == Long.MAX_VALUE) return;

        // Ensure claim will be eligible for delete.
        if (plugin.config().isExempt(player, playerModifier.getWorld().getName())) return;

        long days = TimeUnit.DAYS.convert(protectionDuration, TimeUnit.MILLISECONDS);

        plugin.getServer().getScheduler().runTask(plugin, () -> Lang.sendMessage(modifier,
                Message.NOTIFICATION_EXPIRATION, value -> value.replace("$days", String.valueOf(days))));
    }

}
