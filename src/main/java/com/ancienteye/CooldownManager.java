package com.ancienteye;

import org.bukkit.entity.Player;
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
                p.sendMessage("§c§lWAIT! §7Cooldown: §f" + (remaining / 1000 + 1) + "s");
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
        // Accessing DataManager via the plugin instance passed in constructor
        int level = plugin.getDataManager().getLevel(p);
        
        // Ensure cooldown doesn't drop below 2 seconds (Balance)
        int finalSeconds = Math.max(2, baseSeconds - (level - 1));
        
        String key = p.getUniqueId().toString() + "_" + type;
        cooldowns.put(key, System.currentTimeMillis() + (finalSeconds * 1000L));
    }

    // Clean up memory when players leave (Optional but recommended)
    public void removePlayer(UUID uuid) {
        cooldowns.remove(uuid.toString() + "_P");
        cooldowns.remove(uuid.toString() + "_S");
    }
}
