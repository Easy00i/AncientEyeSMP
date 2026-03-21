package com.ancienteye;

import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {
    private final AncientEyePlugin plugin;
    private final Map<UUID, EyeType> playerEyes   = new HashMap<>();
    private final Map<UUID, Integer> playerLevels = new HashMap<>();
    private final Map<UUID, Integer> playerXP     = new HashMap<>();

    private File              dataFile;
    private FileConfiguration dataConfig;

    public PlayerDataManager(AncientEyePlugin plugin) {
        this.plugin = plugin;
        setupConfig();
        loadData();
    }

    public void setEye(Player p, EyeType newEye, boolean isTrade) {
        EyeType oldEye = getEye(p);

        if (oldEye != EyeType.NONE && oldEye != null && !isTrade) {
            p.sendMessage("§cYou have lost the power of the " + oldEye.name() + " Eye.");
        }

        playerEyes.put(p.getUniqueId(), newEye);
        playerLevels.putIfAbsent(p.getUniqueId(), 1);
        // FIX 1: XP reset to 0 when a new eye is assigned
        playerXP.put(p.getUniqueId(), 0);
        saveData();

        plugin.getAbilityLogic().applyPassiveEffects(p, newEye);

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
        // FIX 2: XP bhi reset hona chahiye — pehle sirf map se remove nahi ho raha tha
        playerXP.remove(p.getUniqueId());
        saveData();
        p.sendMessage("§cYour Eye has been completely reset by Admin.");
    }

    public EyeType getEye(Player p) {
        return playerEyes.getOrDefault(p.getUniqueId(), EyeType.NONE);
    }

    public int getXP(Player p) {
        return playerXP.getOrDefault(p.getUniqueId(), 0);
    }

    public int getLevel(Player p) {
        return playerLevels.getOrDefault(p.getUniqueId(), 1);
    }

    public void addXp(Player p, int amount) {
    UUID id = p.getUniqueId();
    int currentLevel = getLevel(p);
    if (currentLevel >= 3) return; // Max level check

    int currentXP = getXP(p);
    int totalXP = currentXP + amount;
    int maxXP = 100; 

    if (totalXP >= maxXP) {
        playerLevels.put(id, currentLevel + 1);
        playerXP.put(id, 0);
        p.sendMessage("§e§lLEVEL UP! §fLevel §6" + (currentLevel + 1));
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
    } else {
        playerXP.put(id, totalXP);
    }
    
    // ⭐ Action Bar par live update dikhana
    p.sendActionBar("§bAncient XP: §f" + getXP(p) + " §8/ §f100");
    
    saveData(); // Data save logic
}


    // ── DATA.YML ──────────────────────────────────────────────────────────────

    private void setupConfig() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdir();
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    // Public method — called by onDisable() to force-save before shutdown
    public void saveAllData() { saveData(); }

    private void saveData() {
        for (UUID uuid : playerEyes.keySet()) {
            String path = "players." + uuid;
            dataConfig.set(path + ".eye",   playerEyes.get(uuid).name());
            dataConfig.set(path + ".level", playerLevels.getOrDefault(uuid, 1));
            // FIX 3: XP bhi save hona chahiye — pehle save nahi ho raha tha
            dataConfig.set(path + ".xp",    playerXP.getOrDefault(uuid, 0));
        }
        try { dataConfig.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }

    private void loadData() {
        if (dataConfig.getConfigurationSection("players") == null) return;
        for (String key : dataConfig.getConfigurationSection("players").getKeys(false)) {
            // FIX 4: Invalid UUID ya invalid EyeType se crash — try-catch se guard kiya
            try {
                UUID   uuid   = UUID.fromString(key);
                String eyeStr = dataConfig.getString("players." + key + ".eye", "NONE");
                int    level  = dataConfig.getInt("players." + key + ".level", 1);
                int    xp     = dataConfig.getInt("players." + key + ".xp",    0);

                playerEyes.put(uuid,   EyeType.valueOf(eyeStr));
                playerLevels.put(uuid, level);
                playerXP.put(uuid,     xp);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("[AncientEye] Skipping corrupt data entry: " + key + " — " + ex.getMessage());
            }
        }
    }
}
