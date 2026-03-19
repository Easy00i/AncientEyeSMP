package com.ancienteye;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class AbilityTrigger implements Listener {

    @EventHandler
    public void onShiftF(PlayerSwapHandItemsEvent e) {
        Player p = e.getPlayer();
        if (p.isSneaking()) {
            e.setCancelled(true); // Prevent item swap
            
            EyeType eye = AncientEyePlugin.get().getPlayerData().getEye(p);
            if (eye == EyeType.NONE) return;

            if (!AncientEyePlugin.get().getCooldownManager().isOnCooldown(p, "PRIMARY")) {
                AncientEyePlugin.get().getAbilityLogic().executePrimary(p, eye);
                AncientEyePlugin.get().getCooldownManager().setCooldown(p, "PRIMARY", 12);
            }
        }
    }

    @EventHandler
    public void onShiftQ(PlayerDropItemEvent e) {
        Player p = e.getPlayer();
        if (p.isSneaking()) {
            e.setCancelled(true); // Prevent item drop
            
            EyeType eye = AncientEyePlugin.get().getPlayerData().getEye(p);
            if (eye == EyeType.NONE) return;

            if (!AncientEyePlugin.get().getCooldownManager().isOnCooldown(p, "SECONDARY")) {
                AncientEyePlugin.get().getAbilityLogic().executeSecondary(p, eye);
                AncientEyePlugin.get().getCooldownManager().setCooldown(p, "SECONDARY", 15);
            }
        }
    }
}

