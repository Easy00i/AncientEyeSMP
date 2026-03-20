package com.ancienteye;

import org.bukkit.entity.Player;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {
    private final AncientEyePlugin plugin;
    // Key: UUID + "_P" or "_S" | Value: Expiry time in milliseconds
    private final Map<String, Long> cooldowns = new HashMap<>();

    public CooldownManager(AncientEyePlugin plugin) {
        this.plugin = plugin;
    }

    // Eye-specific icons for action bar
    private String getEyeIcon(EyeType eye) {
        if (eye == null) return "✦";
        return switch (eye) {
            case VOID     -> "⬛";
            case PHANTOM  -> "👁";
            case STORM    -> "⚡";
            case FROST    -> "❄";
            case FLAME    -> "🔥";
            case SHADOW   -> "🌑";
            case TITAN    -> "🗿";
            case HUNTER   -> "🏹";
            case GRAVITY  -> "🌀";
            case WIND     -> "💨";
            case POISON   -> "☠";
            case LIGHT    -> "✨";
            case EARTH    -> "🪨";
            case CRYSTAL  -> "💎";
            case ECHO     -> "🔊";
            case RAGE     -> "❤";
            case SPIRIT   -> "👻";
            case TIME     -> "⏳";
            case WARRIOR  -> "⚔";
            case METEOR   -> "☄";
            case MIRAGE   -> "🌫";
            case OCEAN    -> "🌊";
            case ECLIPSE  -> "🌘";
            case GUARDIAN -> "🛡";
            default       -> "✦";
        };
    }

    // Eye-specific color for action bar
    private String getEyeColor(EyeType eye) {
        if (eye == null) return "§7";
        return switch (eye) {
            case VOID     -> "§5";
            case PHANTOM  -> "§f";
            case STORM    -> "§e";
            case FROST    -> "§b";
            case FLAME    -> "§c";
            case SHADOW   -> "§8";
            case TITAN    -> "§6";
            case HUNTER   -> "§a";
            case GRAVITY  -> "§d";
            case WIND     -> "§f";
            case POISON   -> "§2";
            case LIGHT    -> "§e";
            case EARTH    -> "§6";
            case CRYSTAL  -> "§b";
            case ECHO     -> "§3";
            case RAGE     -> "§c";
            case SPIRIT   -> "§a";
            case TIME     -> "§d";
            case WARRIOR  -> "§6";
            case METEOR   -> "§c";
            case MIRAGE   -> "§7";
            case OCEAN    -> "§3";
            case ECLIPSE  -> "§5";
            case GUARDIAN -> "§e";
            default       -> "§7";
        };
    }

    // Cooldown bar builder (filled + empty blocks)
    private String buildBar(long remaining, long total) {
        int bars    = 10;
        int filled  = (int) Math.round((double)(total - remaining) / total * bars);
        filled      = Math.max(0, Math.min(bars, filled));
        StringBuilder sb = new StringBuilder();
        sb.append("§a");
        for (int i = 0; i < filled; i++)  sb.append("█");
        sb.append("§8");
        for (int i = filled; i < bars; i++) sb.append("█");
        return sb.toString();
    }

    /**
     * Sends a professional action bar cooldown message to the player.
     */
    private void sendCooldownBar(Player p, String type, long remaining, long total) {
        EyeType eye  = plugin.getDataManager().getEye(p);
        String  icon = getEyeIcon(eye);
        String  col  = getEyeColor(eye);
        String  bar  = buildBar(remaining, total);
        String  name = (type.equals("P")) ? "Primary" : "Secondary";
        String  key  = (type.equals("P")) ? "§b[SHIFT+F]" : "§d[SHIFT+Q]";
        long    secs = (remaining / 1000) + 1;

        String msg = col + "§l" + icon + " " + name + " " + key
                   + " §8| " + bar
                   + " §8| " + col + "§l" + secs + "s";

        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
    }

    /**
     * Checks if an ability is currently on cooldown.
     * @param p The player to check.
     * @param type "P" for Primary, "S" for Secondary.
     * @return true if still on cooldown.
     */
    public boolean isOnCooldown(Player p, String type) {
        String key = p.getUniqueId().toString() + "_" + type;

        if (cooldowns.containsKey(key)) {
            long remaining = cooldowns.get(key) - System.currentTimeMillis();
            if (remaining > 0) {
                long total = getTotalCooldown(p, type);
                sendCooldownBar(p, type, remaining, total);
                return true;
            }
        }
        return false;
    }

    /**
     * Sets a new cooldown based on player level.
     * @param p The player.
     * @param type "P" or "S".
     * @param baseSeconds The standard cooldown time.
     */
    public void setCooldown(Player p, String type, int baseSeconds) {
        int level = plugin.getDataManager().getLevel(p);

        // Ensure cooldown doesn't drop below 2 seconds (Balance)
        int finalSeconds = Math.max(2, baseSeconds - (level - 1));

        String key = p.getUniqueId().toString() + "_" + type;
        cooldowns.put(key, System.currentTimeMillis() + (finalSeconds * 1000L));

        // Also store total cooldown for bar calculation
        storeTotalCooldown(p, type, finalSeconds * 1000L);

        // Show "ACTIVATED" bar briefly
        EyeType eye  = plugin.getDataManager().getEye(p);
        String  icon = getEyeIcon(eye);
        String  col  = getEyeColor(eye);
        String  name = (type.equals("P")) ? "Primary" : "Secondary";
        String  keyLabel = (type.equals("P")) ? "§b[SHIFT+F]" : "§d[SHIFT+Q]";

        String msg = col + "§l" + icon + " " + name + " " + keyLabel
                   + " §8| §a§l██████████ §8| " + col + "§l✔ ACTIVATED!";

        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
    }

    // Storage for total cooldown duration (for bar progress)
    private final Map<String, Long> totalCooldowns = new HashMap<>();

    private void storeTotalCooldown(Player p, String type, long millis) {
        totalCooldowns.put(p.getUniqueId().toString() + "_T_" + type, millis);
    }

    private long getTotalCooldown(Player p, String type) {
        String k = p.getUniqueId().toString() + "_T_" + type;
        return totalCooldowns.getOrDefault(k, 12000L);
    }

    // Clean up memory when players leave (Optional but recommended)
    public void removePlayer(UUID uuid) {
        String id = uuid.toString();
        cooldowns.remove(id + "_P");
        cooldowns.remove(id + "_S");
        totalCooldowns.remove(id + "_T_P");
        totalCooldowns.remove(id + "_T_S");
    }
}
