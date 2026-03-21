package com.ancienteye;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;

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
    // FIX: PlayerDropItemEvent sirf tab fire hota hai jab haath mein item ho.
    // PlayerAnimationEvent (ARM_SWING) + isSneaking() se bina item ke bhi trigger hoga.
    // Lekin ARM_SWING bahut zyada fire hota hai — isliye cooldown map se debounce kiya.
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onShiftQ(PlayerDropItemEvent e) {
        Player p = e.getPlayer();

        if (p.isSneaking()) {
            e.setCancelled(true);
            triggerSecondary(p);
        }
    }

    // FIX: Jab haath mein koi item NAHI hai tab Q press = PlayerAnimationEvent fire hota hai
    // Hum sneak + Q combo detect karte hain via sneak state tracking
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAnimation(PlayerAnimationEvent e) {
        if (e.getAnimationType() != PlayerAnimationType.ARM_SWING) return;

        Player p = e.getPlayer();
        if (!p.isSneaking()) return;

        // Haath mein item hai toh PlayerDropItemEvent handle karega — skip karo
        org.bukkit.inventory.ItemStack mainHand = p.getInventory().getItemInMainHand();
        if (mainHand.getType() != org.bukkit.Material.AIR) return;

        triggerSecondary(p);
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
