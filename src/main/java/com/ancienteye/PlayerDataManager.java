package com.ancienteye;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {
    private final Map<UUID, EyeType> playerEyes = new HashMap<>();
    private final Map<UUID, Integer> playerLevels = new HashMap<>();

    public void setEye(Player p, EyeType newEye, boolean isTrade) {
        EyeType oldEye = getEye(p);
        
        // Remove old eye if exists
        if (oldEye != EyeType.NONE && oldEye != null && !isTrade) {
            p.sendMessage("§cYou have lost the power of the " + oldEye.name() + " Eye.");
        }

        // Set new eye
        playerEyes.put(p.getUniqueId(), newEye);
        playerLevels.putIfAbsent(p.getUniqueId(), 1);

        // Success message
        if (!isTrade) {
            p.sendMessage("§aYou have awakened the power of the " + newEye.name() + " Eye.");
            p.sendTitle("§d§l" + newEye.name() + " EYE", "§fPower Awakened!", 10, 50, 10);
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
        } else {
            p.sendMessage("§aTrade completed successfully.");
        }
    }

    public void resetEye(Player p) {
        playerEyes.remove(p.getUniqueId());
        playerLevels.remove(p.getUniqueId());
        p.sendMessage("§cYour Eye has been completely reset by Admin.");
    }

    public EyeType getEye(Player p) {
        return playerEyes.getOrDefault(p.getUniqueId(), EyeType.NONE);
    }

    public int getLevel(Player p) {
        return playerLevels.getOrDefault(p.getUniqueId(), 1);
    }
    
    public void addXp(Player p) {
        // Logic for leveling up max level 3 (To be expanded in events)
        int current = getLevel(p);
        if (current < 3) {
            playerLevels.put(p.getUniqueId(), current + 1);
            p.sendMessage("§e§lLEVEL UP! §fYour Eye is now Level " + (current + 1));
        }
    }
}

