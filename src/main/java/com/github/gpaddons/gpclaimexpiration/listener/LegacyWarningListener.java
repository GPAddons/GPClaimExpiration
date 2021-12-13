package com.github.gpaddons.gpclaimexpiration.listener;

import com.github.gpaddons.gpclaimexpiration.GPClaimExpiration;
import me.ryanhamshire.GriefPrevention.events.ClaimCreatedEvent;
import me.ryanhamshire.GriefPrevention.events.ClaimModifiedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.jetbrains.annotations.NotNull;

/**
 * A warning listener for outdated GP events.
 */
public class LegacyWarningListener extends WarningListener
{

    public LegacyWarningListener(GPClaimExpiration plugin)
    {
        super(plugin);
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

}
