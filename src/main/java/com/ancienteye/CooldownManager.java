package com.ancienteye;

import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {
    // String format: UUID_PRIMARY or UUID_SECONDARY
    private final Map<String, Long> cooldowns = new HashMap<>();

    public boolean isOnCooldown(Player p, String abilityType) {
        String key = p.getUniqueId().toString() + "_" + abilityType;
        if (cooldowns.containsKey(key)) {
            long timePassed = System.currentTimeMillis() - cooldowns.get(key);
            if (timePassed < 10000) { // 10 seconds base cooldown
                long timeLeft = (10000 - timePassed) / 1000;
                p.sendMessage("§cAbility on cooldown! Wait " + timeLeft + "s.");
                return true;
            }
        }
        return false;
    }

    public void setCooldown(Player p, String abilityType, int baseCooldownSec) {
        int level = AncientEyePlugin.get().getPlayerData().getLevel(p);
        long actualCooldown = (baseCooldownSec - (level * 1)) * 1000L; // Level up reduces cooldown
        
        String key = p.getUniqueId().toString() + "_" + abilityType;
        cooldowns.put(key, System.currentTimeMillis() - 10000 + actualCooldown);
    }
}

