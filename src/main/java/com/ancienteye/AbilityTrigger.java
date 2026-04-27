package com.ancienteye;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;

public class AbilityTrigger implements Listener {
    private final AncientEyePlugin plugin;

    // Debounce maps
    private final java.util.Map<java.util.UUID, Long> lastPrimary = new java.util.HashMap<>();
    private final java.util.Map<java.util.UUID, Long> lastSecondary = new java.util.HashMap<>();

    public AbilityTrigger(AncientEyePlugin plugin) {
        this.plugin = plugin;
    }

    // ── SHIFT + LEFT CLICK (Primary Ability) ───────────────────────────────
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onShiftLeftClick(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!p.isSneaking()) return;

        Action action = e.getAction();
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            e.setCancelled(true); // cancel only when sneaking + left click
            triggerPrimary(p);
        }
    }

    // ── SHIFT + RIGHT CLICK (Secondary Ability) ──────────────────────────────
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onShiftRightClick(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!p.isSneaking()) return;

        Action action = e.getAction();
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            e.setCancelled(true); // cancel only when sneaking + right click
            triggerSecondary(p);
        }
    }

    // ── Primary trigger with debounce ──────────────────────────────────────
    private void triggerPrimary(Player p) {
        long now = System.currentTimeMillis();
        Long last = lastPrimary.get(p.getUniqueId());
        if (last != null && now - last < 200) return;
        lastPrimary.put(p.getUniqueId(), now);

        EyeType eye = plugin.getPlayerData().getEye(p);
        if (eye == null || eye == EyeType.NONE) return;

        plugin.getAbilityLogic().activatePrimary(p, eye);
    }

    // ── Secondary trigger with debounce ────────────────────────────────────
    private void triggerSecondary(Player p) {
        long now = System.currentTimeMillis();
        Long last = lastSecondary.get(p.getUniqueId());
        if (last != null && now - last < 200) return;
        lastSecondary.put(p.getUniqueId(), now);

        EyeType eye = plugin.getPlayerData().getEye(p);
        if (eye == null || eye == EyeType.NONE) return;

        plugin.getAbilityLogic().activateSecondary(p, eye);
    }
}
