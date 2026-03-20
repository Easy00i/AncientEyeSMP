package com.ancienteye;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * EyeHUDTask — Custom Model Data version for 1.21.1
 *
 * Har eye type ke liye ek ENDER_EYE item banata hai
 * jisme custom_model_data set hota hai.
 * Resource pack us CMD se colored eye texture dikhata hai.
 *
 * Item player ke OFFHAND mein diya jata hai — woh
 * hotbar ke RIGHT side mein hamesha visible rehta hai
 * (exactly jaise TokenSMP karta hai).
 *
 * Add to AncientEyePlugin.onEnable():
 *   new EyeHUDTask(this).runTaskTimer(this, 20L, 20L);
 *
 * Add to AncientEyePlugin.onDisable():
 *   clearAllHudItems();
 */
public class EyeHUDTask extends BukkitRunnable {

    private final AncientEyePlugin plugin;

    // Track which players have HUD item in offhand
    private final Map<UUID, EyeType> activeHud = new HashMap<>();

    public EyeHUDTask(AncientEyePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            EyeType eye = plugin.getPlayerData().getEye(p);

            if (eye == null || eye == EyeType.NONE) {
                clearHudItem(p);
                continue;
            }

            // Only update if eye changed (avoid flickering)
            EyeType current = activeHud.get(p.getUniqueId());
            if (current == eye) continue;

            // Set HUD item in offhand
            setHudItem(p, eye);
            activeHud.put(p.getUniqueId(), eye);
        }

        // Clean offline players
        activeHud.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
    }

    // ── Set eye icon item in offhand ──────────────────────────────────────────
    private void setHudItem(Player p, EyeType eye) {
        ItemStack item = new ItemStack(Material.ENDER_EYE);
        ItemMeta meta = item.getItemMeta();

        // Set custom model data — resource pack uses this to show colored eye texture
        meta.setCustomModelData(getCustomModelData(eye));

        // Display name
        meta.setDisplayName(getEyeColor(eye) + "§l" + eye.name().replace("_"," ") + " EYE");

        // Mark as HUD item so other systems ignore it
        meta.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey(plugin, "hud_item"),
            org.bukkit.persistence.PersistentDataType.BYTE,
            (byte) 1
        );

        item.setItemMeta(meta);

        // Place in offhand — offhand shows on RIGHT of hotbar
        p.getInventory().setItemInOffHand(item);
    }

    // ── Remove HUD item from offhand ──────────────────────────────────────────
    public void clearHudItem(Player p) {
        ItemStack offhand = p.getInventory().getItemInOffHand();
        if (offhand != null && offhand.getType() == Material.ENDER_EYE) {
            ItemMeta meta = offhand.getItemMeta();
            if (meta != null && meta.getPersistentDataContainer().has(
                    new org.bukkit.NamespacedKey(plugin, "hud_item"),
                    org.bukkit.persistence.PersistentDataType.BYTE)) {
                p.getInventory().setItemInOffHand(null);
            }
        }
        activeHud.remove(p.getUniqueId());
    }

    // ── Clear all HUD items (call from onDisable) ─────────────────────────────
    public void clearAllHudItems() {
        for (UUID uuid : activeHud.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) clearHudItem(p);
        }
        activeHud.clear();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CUSTOM MODEL DATA MAP  — matches resource pack ender_eye.json entries
    // ══════════════════════════════════════════════════════════════════════════
    private int getCustomModelData(EyeType eye) {
        return switch (eye) {
            case VOID     -> 1001;
            case PHANTOM  -> 1002;
            case STORM    -> 1003;
            case FROST    -> 1004;
            case FLAME    -> 1005;
            case SHADOW   -> 1006;
            case TITAN    -> 1007;
            case HUNTER   -> 1008;
            case GRAVITY  -> 1009;
            case WIND     -> 1010;
            case POISON   -> 1011;
            case LIGHT    -> 1012;
            case EARTH    -> 1013;
            case CRYSTAL  -> 1014;
            case ECHO     -> 1015;
            case RAGE     -> 1016;
            case SPIRIT   -> 1017;
            case TIME     -> 1018;
            case WARRIOR  -> 1019;
            case METEOR   -> 1020;
            case MIRAGE   -> 1021;
            case OCEAN    -> 1022;
            case ECLIPSE  -> 1023;
            case GUARDIAN -> 1024;
            default       -> 0;
        };
    }

    // ── Chat color per eye ────────────────────────────────────────────────────
    private String getEyeColor(EyeType eye) {
        return switch (eye) {
            case VOID, ECLIPSE          -> "§5";
            case PHANTOM, SHADOW        -> "§8";
            case STORM, OCEAN           -> "§9";
            case FROST, CRYSTAL         -> "§b";
            case FLAME, METEOR          -> "§c";
            case TITAN, WARRIOR         -> "§6";
            case HUNTER                 -> "§2";
            case GRAVITY, TIME          -> "§d";
            case WIND                   -> "§f";
            case POISON, SPIRIT,
                 GUARDIAN               -> "§a";
            case LIGHT, MIRAGE          -> "§e";
            case EARTH, ECHO            -> "§3";
            case RAGE                   -> "§4";
            default                     -> "§f";
        };
    }
}
