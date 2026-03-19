package com.ancienteye;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final AncientEyePlugin plugin;

    public PlayerJoinListener(AncientEyePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        // Player ka saved Eye check karke passive effect lagao
        EyeType type = plugin.getPlayerData().getEye(p);
        plugin.getAbilityLogic().applyPassiveEffects(p, type);
    }
}

