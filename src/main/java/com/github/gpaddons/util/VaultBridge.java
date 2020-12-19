package com.github.gpaddons.util;

import com.github.gpaddons.gpclaimexpiration.GPClaimExpiration;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A bridge for Vault-supporting permissions plugins.
 */
public class VaultBridge implements Listener
{

    private final @NotNull GPClaimExpiration plugin;
    private boolean setupDone = false;
    private @Nullable PermissionWrapper permissionWrapper = null;

    public VaultBridge(@NotNull GPClaimExpiration plugin)
    {
        this.plugin = plugin;
    }

    /**
     * Check if a player is granted a permission.
     *
     * @param player the OfflinePlayer to check permissions for
     * @param permission the permission to check
     * @return true if the player has the permission
     */
    public boolean hasPermission(@NotNull OfflinePlayer player, @NotNull String permission)
    {
        loadPermission(false);

        if (this.permissionWrapper == null) return false;

        return this.permissionWrapper.getPermission().playerHas(null, player, permission);
    }

    @EventHandler
    private void onPluginEnable(@NotNull PluginEnableEvent event)
    {
        loadPermission(true);
    }

    @EventHandler
    private void onPluginDisable(@NotNull PluginDisableEvent event)
    {
        loadPermission(true);
    }

    private void loadPermission(boolean setupState)
    {
        // If no change is likely, have we already obtained the Permission?
        if (setupState != setupDone) return;

        // Are any bypass permissions configured?
        if (plugin.getConfig().getStringList("expiration.bypass.permissions").isEmpty())
        {
            finishSetup(false, null);
            return;
        }

        // Ensure Vault present.
        try
        {
            Class.forName("net.milkbowl.vault.permission.Permission");
        }
        catch (ClassNotFoundException e)
        {
            finishSetup(false, "[GPClaimExpiration] ERROR: Vault is required for permission integration.");
            return;
        }

        RegisteredServiceProvider<Permission> registration = plugin.getServer().getServicesManager().getRegistration(Permission.class);

        // Ensure a Permission is available.
        if (registration == null)
        {
            finishSetup(false, "[GPClaimExpiration] ERROR: Vault was unable to find a supported permissions plugin. It should default to Bukkit's SuperPerms system.");
            return;
        }

        Permission newPermission = registration.getProvider();

        // If Permission hasn't changed, do nothing.
        if (permissionWrapper != null && permissionWrapper.getPermission().equals(newPermission)) return;

        // Set setupDone false to force log line for changing Permission.
        setupDone = false;
        permissionWrapper = new PermissionWrapper(newPermission);

        finishSetup(true, "[GPClaimExpiration] Hooked into permissions system: " + permissionWrapper.getPermission().getName() + ". Ready to check offline permissions!");
    }

    private void finishSetup(boolean ready, @Nullable String log) {
        if (!ready) this.permissionWrapper = null;

        if (log != null && !setupDone) GriefPrevention.AddLogEntry(log);

        this.setupDone = true;
    }

    private static class PermissionWrapper
    {

        private final @NotNull Permission permission;

        private PermissionWrapper(@NotNull Permission permission)
        {
            this.permission = permission;
        }

        @NotNull Permission getPermission()
        {
            return this.permission;
        }

    }

}
