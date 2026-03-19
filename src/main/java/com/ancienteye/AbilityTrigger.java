package com.ancienteye;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class AbilityTrigger implements Listener {
    private final AncientEyePlugin plugin;

    public AbilityTrigger(AncientEyePlugin plugin) {
        this.plugin = plugin;
    }

    // --- SHIFT + F (Primary Ability) ---
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onShiftF(PlayerSwapHandItemsEvent e) {
        Player p = e.getPlayer();
        
        if (p.isSneaking()) {
            e.setCancelled(true); // Stop item swap animation
            
            EyeType eye = plugin.getPlayerData().getEye(p);
            if (eye == null || eye == EyeType.NONE) return;

            // Direct call to AbilityLogic methods
            plugin.getAbilityLogic().activatePrimary(p, eye);
        }
    }

    // --- SHIFT + Q (Secondary Ability) ---
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onShiftQ(PlayerDropItemEvent e) {
        Player p = e.getPlayer();
        
        // Shift + Q check for full stack drop / secondary power
        if (p.isSneaking()) {
            e.setCancelled(true); // Stop item from dropping
            
            EyeType eye = plugin.getPlayerData().getEye(p);
            if (eye == null || eye == EyeType.NONE) return;

            // Direct call to AbilityLogic methods
            plugin.getAbilityLogic().activateSecondary(p, eye);
        }
    }
}
