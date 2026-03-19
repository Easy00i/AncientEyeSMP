package com.ancienteye;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class AbilityTrigger implements Listener {
    private final AncientEyePlugin plugin;

    public AbilityTrigger(AncientEyePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onShiftF(PlayerSwapHandItemsEvent e) {
        Player p = e.getPlayer();
        if (p.isSneaking()) {
            e.setCancelled(true); // Stop the item swap animation
            
            // Get the EyeType from DataManager
            EyeType eye = plugin.getPlayerData().getEye(p);
            if (eye == null || eye == EyeType.NONE) return;

            // Use "P" to match the CooldownManager logic
            if (!plugin.getCooldownManager().isOnCooldown(p, "P")) {
                // Changed from executePrimary to handlePrimary to match AbilityLogic
                plugin.getAbilityLogic().handlePrimary(p, eye);
                plugin.getCooldownManager().setCooldown(p, "P", 10);
            }
        }
    }

    @EventHandler
    public void onShiftQ(PlayerDropItemEvent e) {
        Player p = e.getPlayer();
        if (p.isSneaking()) {
            e.setCancelled(true); // Stop the item from dropping
            
            EyeType eye = plugin.getPlayerData().getEye(p);
            if (eye == null || eye == EyeType.NONE) return;

            // Use "S" to match the CooldownManager logic
            if (!plugin.getCooldownManager().isOnCooldown(p, "S")) {
                // Changed from executeSecondary to handleSecondary to match AbilityLogic
                plugin.getAbilityLogic().handleSecondary(p, eye);
                plugin.getCooldownManager().setCooldown(p, "S", 15);
            }
        }
    }
}
