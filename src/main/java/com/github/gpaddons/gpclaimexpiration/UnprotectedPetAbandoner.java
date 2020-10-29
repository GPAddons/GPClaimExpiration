package com.github.gpaddons.gpclaimexpiration;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Sittable;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

/**
 * Listener for expiring pet ownership.
 */
class UnprotectedPetAbandoner implements Listener
{

    private final GPClaimExpiration plugin;

    UnprotectedPetAbandoner(@NotNull GPClaimExpiration plugin)
    {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    private void onPlayerInteractEntity(@NotNull EntityDamageEvent event)
    {
        Player player = null;
        // Obtain damaging player if possible to leverage cached claims.
        if (event instanceof EntityDamageByEntityEvent)
        {
            Entity damager = ((EntityDamageByEntityEvent) event).getDamager();
            if (damager instanceof Player)
                player = (Player) damager;
            else if (damager instanceof Projectile)
            {
                Projectile projectile = (Projectile) damager;
                if (projectile.getShooter() instanceof Player)
                    player = (Player) projectile.getShooter();
            }
        }

        // Handle generic pet interaction.
        onPetInteract(player, event.getEntity());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    private void onPlayerInteractEntity(@NotNull PlayerInteractEntityEvent event)
    {
        // Handle generic pet interaction.
        onPetInteract(event.getPlayer(), event.getRightClicked());
    }

    private void onPetInteract(@Nullable Player actor, @NotNull Entity entity)
    {
        int days = plugin.getConfig().getInt("expiration.pets.days", -1);

        // Ensure expiration on unclaimed pets is set and entity is a pet.
        if (days < 0 || !(entity instanceof Tameable)) return;

        Tameable tameable = (Tameable) entity;
        final AnimalTamer animalTamer = tameable.getOwner();

        // Ensure pet is tamed.
        if (!(animalTamer instanceof OfflinePlayer)) return;

        OfflinePlayer owner = (OfflinePlayer) animalTamer;
        Claim claim = getClaim(tameable.getLocation(), actor);

        // Treat admin claims as unclaimed area - no owner to transfer to.
        if (claim != null && claim.isAdminClaim()) claim = null;

        // Ensure the pet is unclaimed or claimed by someone who does not have access to the claim it is in.
        if (claim != null && claim.getPermission(owner.getUniqueId().toString()) != null) return;

        // Ensure pet owner is not exempt from expiration.
        if (plugin.isExempt(owner)) return;

        // Ensure pet owner's last play session was long enough ago to expire pet ownership.
        if (plugin.getLastQualifyingSession(owner) >= System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert(days, TimeUnit.DAYS)) return;

        // If the pet is in a claim, transfer it to the claim owner instead of untaming.
        if (claim != null)
        {
            OfflinePlayer claimOwner = plugin.getServer().getOfflinePlayer(claim.getOwnerID());
            tameable.setOwner(claimOwner);
            return;
        }

        // Untame pet.
        tameable.setOwner(null);

        // Make untamed sittables stand.
        if (tameable instanceof Sittable)
        {
            ((Sittable) tameable).setSitting(false);
        }

        // Untame entities that can be tamed naturally.
        switch (tameable.getType())
        {
            case SKELETON_HORSE:
            case ZOMBIE_HORSE:
                break;
            default:
                tameable.setTamed(false);
        }

        // Run pet abandonment commands.
        for (String command : plugin.getCommandList("expiration.pets.commands", owner, tameable.getLocation()))
        {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
        }
    }

    private static @Nullable Claim getClaim(@NotNull Location location, @Nullable Player player)
    {
        // No player, no claim caching.
        if (player == null) return GriefPrevention.instance.dataStore.getClaimAt(location, false, null);

        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId());
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(location, false, playerData.lastClaim);

        // Update cached claim if present.
        if (claim != null) playerData.lastClaim = claim;

        return claim;
    }

}
