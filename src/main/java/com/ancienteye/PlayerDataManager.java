package com.ancienteye;

import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
        // FIX: putIfAbsent -> put — new eye milne par level hamesha 1 reset hoga
        playerLevels.put(p.getUniqueId(), 1);
        // FIX 1: XP reset to 0 when a new eye is assigned
        playerXP.put(p.getUniqueId(), 0);
        peacefulPlayers.remove(p.getUniqueId());
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

    public void setPeaceful(Player p) {
            peacefulPlayers.put(p.getUniqueId(), true);
           saveData();
          }

         public boolean isPeaceful(Player p) {
        return peacefulPlayers.getOrDefault(p.getUniqueId(), false);
         }

    public void resetEye(Player p) {
        playerEyes.remove(p.getUniqueId());
        playerLevels.remove(p.getUniqueId());
        // FIX 2: XP bhi reset hona chahiye
        playerXP.remove(p.getUniqueId());
        peacefulPlayers.remove(p.getUniqueId());
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

    // FIX A: config se kill thresholds
    public int getMaxXPForLevel(int level) {
        return switch (level) {
            case 1  -> plugin.getConfig().getInt("settings.kills-for-level-2", 5);
            case 2  -> plugin.getConfig().getInt("settings.kills-for-level-3", 10);
            default -> plugin.getConfig().getInt("settings.kills-for-level-3", 10);
        };
    }

    public void addXp(Player p, int amount) {
        UUID id = p.getUniqueId();
        int currentLevel = getLevel(p);
        if (currentLevel >= 3) return; // Max level check

        int currentXP = getXP(p);
        int totalXP   = currentXP + amount;
        // FIX B: maxXP config se
        int maxXP     = getMaxXPForLevel(currentLevel);

        if (totalXP >= maxXP) {
            playerLevels.put(id, currentLevel + 1);
            playerXP.put(id, 0);
            p.sendMessage("§e§lLEVEL UP! §fLevel §6" + (currentLevel + 1));
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        } else {
            playerXP.put(id, totalXP);
        }

        // FIX C: action bar — kills dikhao
        p.sendActionBar("§c⚔ Kills: §f" + getXP(p) + " §8/ §f" + maxXP
            + "  §7-> §eLv." + getLevel(p));

        saveData();
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
    public void saveData() {
        // 1. Pehle purana players section poora saaf karo taaki junk data na rahe
        dataConfig.set("players", null); 

        Set<UUID> allUUIDs = new HashSet<>();
        allUUIDs.addAll(playerEyes.keySet());
        allUUIDs.addAll(playerLevels.keySet());
        allUUIDs.addAll(playerXP.keySet());

        for (UUID uuid : allUUIDs) {
            String path = "players." + uuid;
            EyeType eye = playerEyes.get(uuid);
            
            // 2. Sirf unhi ko save karo jinke paas Eye hai
            if (eye != null && eye != EyeType.NONE) {
                dataConfig.set(path + ".eye", eye.name());
                dataConfig.set(path + ".level", playerLevels.getOrDefault(uuid, 1));
                dataConfig.set(path + ".xp", playerXP.getOrDefault(uuid, 0));
            }
            if (peacefulPlayers.getOrDefault(uuid, false)) {
               dataConfig.set(path + ".peaceful", true);
          }       
        }
        
        try { 
            dataConfig.save(dataFile); 
        } catch (IOException e) { 
            e.printStackTrace(); 
        }
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
                boolean peaceful = dataConfig.getBoolean("players." + key + ".peaceful", false);

                playerEyes.put(uuid,   EyeType.valueOf(eyeStr));
                playerLevels.put(uuid, level);
                playerXP.put(uuid,     xp);
            }
               if (peaceful) {
                peacefulPlayers.put(uuid, true);
              }
            
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("[AncientEye] Skipping corrupt data entry: " + key + " — " + ex.getMessage());
            }
        }
    }
}
