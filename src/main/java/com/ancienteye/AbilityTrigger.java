package com.ancienteye;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class AbilityTrigger implements Listener {
    private final AncientEyePlugin plugin;

    // Debounce — same tick mein double fire na ho
    private final java.util.Map<java.util.UUID, Long> lastSecondary = new java.util.HashMap<>();

    public AbilityTrigger(AncientEyePlugin plugin) {
        this.plugin = plugin;
    }

    // ── SHIFT + F (Primary Ability) ───────────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onShiftF(PlayerSwapHandItemsEvent e) {
        Player p = e.getPlayer();

        if (p.isSneaking()) {
            e.setCancelled(true);

            EyeType eye = plugin.getPlayerData().getEye(p);
            if (eye == null || eye == EyeType.NONE) return;

            plugin.getAbilityLogic().activatePrimary(p, eye);
        }
    }

    // ── SHIFT + Q (Secondary Ability) ────────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onShiftQ(PlayerDropItemEvent e) {
        Player p = e.getPlayer();

        if (p.isSneaking()) {
            e.setCancelled(true);
            triggerSecondary(p);
        }
    }

    // ── Common secondary trigger with debounce ────────────────────────────────
    private void triggerSecondary(Player p) {
        // Debounce: 200ms ke andar double fire nahi hoga
        long now = System.currentTimeMillis();
        Long last = lastSecondary.get(p.getUniqueId());
        if (last != null && now - last < 200) return;
        lastSecondary.put(p.getUniqueId(), now);

        EyeType eye = plugin.getPlayerData().getEye(p);
        if (eye == null || eye == EyeType.NONE) return;

        plugin.getAbilityLogic().activateSecondary(p, eye);
    }
}
