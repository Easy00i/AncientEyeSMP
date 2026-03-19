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
    private final Map<UUID, EyeType> playerEyes = new HashMap<>();
    private final Map<UUID, Integer> playerLevels = new HashMap<>();
    private File dataFile;
    private FileConfiguration dataConfig;

    // Correct Constructor at class level
    public PlayerDataManager(AncientEyePlugin plugin) {
        this.plugin = plugin;
        setupConfig();
        loadData();
    }

    public void setEye(Player p, EyeType newEye, boolean isTrade) {
        EyeType oldEye = getEye(p);
        
        // Remove old eye message
        if (oldEye != EyeType.NONE && oldEye != null && !isTrade) {
            p.sendMessage("§cYou have lost the power of the " + oldEye.name() + " Eye.");
        }

        // Set new eye logic
        playerEyes.put(p.getUniqueId(), newEye);
        playerLevels.putIfAbsent(p.getUniqueId(), 1);
        saveData(); // Auto-save to data.yml

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
        saveData();
        p.sendMessage("§cYour Eye has been completely reset by Admin.");
    }

    public EyeType getEye(Player p) {
        return playerEyes.getOrDefault(p.getUniqueId(), EyeType.NONE);
    }

    public int getXP(Player p) {
    return playerXP.getOrDefault(p.getUniqueId(), 0);
    }
    
    public void addXp(Player p) {
        int current = getLevel(p);
        if (current < 3) {
            playerLevels.put(p.getUniqueId(), current + 1);
            saveData();
            p.sendMessage("§e§lLEVEL UP! §fYour Eye is now Level " + (current + 1));
        }
    }

    // --- DATA.YML SAVING SYSTEM ---
    private void setupConfig() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdir();
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void saveData() {
        for (UUID uuid : playerEyes.keySet()) {
            dataConfig.set("players." + uuid + ".eye", playerEyes.get(uuid).name());
            dataConfig.set("players." + uuid + ".level", playerLevels.getOrDefault(uuid, 1));
        }
        try { dataConfig.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }

    private void loadData() {
        if (dataConfig.getConfigurationSection("players") == null) return;
        for (String key : dataConfig.getConfigurationSection("players").getKeys(false)) {
            UUID uuid = UUID.fromString(key);
            String eyeStr = dataConfig.getString("players." + key + ".eye");
            int level = dataConfig.getInt("players." + key + ".level");
            playerEyes.put(uuid, EyeType.valueOf(eyeStr));
            playerLevels.put(uuid, level);
        }
    }
}
