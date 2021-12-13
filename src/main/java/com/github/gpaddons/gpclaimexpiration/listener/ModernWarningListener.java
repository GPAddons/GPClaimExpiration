package com.github.gpaddons.gpclaimexpiration.listener;

import com.github.gpaddons.gpclaimexpiration.GPClaimExpiration;
import me.ryanhamshire.GriefPrevention.events.ClaimCreatedEvent;
import me.ryanhamshire.GriefPrevention.events.ClaimResizeEvent;
import me.ryanhamshire.GriefPrevention.events.ClaimTransferEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.jetbrains.annotations.NotNull;

/**
 * A warning listener for modern GP events.
 */
public class ModernWarningListener extends WarningListener
{

    public ModernWarningListener(GPClaimExpiration plugin)
    {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onClaim(@NotNull ClaimCreatedEvent event)
    {
        warn(event.getClaim(), event.getCreator());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onClaimResize(@NotNull ClaimResizeEvent event)
    {
        warn(event.getTo(), event.getModifier());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onClaimTransfer(@NotNull ClaimTransferEvent event)
    {
        warn(event.getClaim(), event.getNewOwner());
    }

}
