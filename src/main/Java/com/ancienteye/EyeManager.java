package com.ancienteye;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class EyeManager implements Listener {
    private final AncientEyePlugin plugin;
    public final NamespacedKey eyeKey, levelKey, xpKey, smpStartKey;

    public enum Eye {
        VOID(Particle.PORTAL, false), PHANTOM(Particle.SOUL, false), STORM(Particle.ENCHANTED_HIT, false),
        FROST(Particle.SNOWFLAKE, false), FLAME(Particle.FLAME, false), SHADOW(Particle.SQUID_INK, false),
        TITAN(Particle.EXPLOSION_NORMAL, false), HUNTER(Particle.CRIT, false), GRAVITY(Particle.REVERSE_PORTAL, false),
        WIND(Particle.CLOUD, false), POISON(Particle.SPELL_WITCH, false), LIGHT(Particle.END_ROD, false),
        EARTH(Particle.BLOCK_CRACK, false), CRYSTAL(Particle.AMETHYST_BLOCK, false), ECHO(Particle.SONIC_BOOM, false),
        RAGE(Particle.DAMAGE_INDICATOR, false), SPIRIT(Particle.TOTEM, false), TIME(Particle.NAUTILUS, false),
        METEOR(Particle.LAVA, true), MIRAGE(Particle.DRAGON_BREATH, true), OCEAN(Particle.WATER_SPLASH, true),
        ECLIPSE(Particle.SMOKE_LARGE, true), GUARDIAN(Particle.SPELL_INSTANT, true);

        public final Particle particle;
        public final boolean isEventOnly;
        Eye(Particle particle, boolean isEventOnly) { this.particle = particle; this.isEventOnly = isEventOnly; }
        
        public static Eye getRandomBase() {
            List<Eye> bases = Arrays.stream(values()).filter(e -> !e.isEventOnly).toList();
            return bases.get(new Random().nextInt(bases.size()));
        }
    }

    public EyeManager(AncientEyePlugin plugin) {
        this.plugin = plugin;
        this.eyeKey = new NamespacedKey(plugin, "active_eye");
        this.levelKey = new NamespacedKey(plugin, "eye_level");
        this.xpKey = new NamespacedKey(plugin, "eye_xp");
        this.smpStartKey = new NamespacedKey(plugin, "smp_started");
        startParticleTask();
    }

    public void setEye(Player p, Eye eye, boolean silent) {
        if (hasEye(p) && !silent) {
            p.sendMessage(ChatColor.RED + "You have lost the power of the " + getEye(p).name() + " Eye.");
        }
        p.getPersistentDataContainer().set(eyeKey, PersistentDataType.STRING, eye.name());
        if (!silent) {
            p.sendMessage(ChatColor.GREEN + "You have awakened the power of the " + eye.name() + " Eye.");
            p.sendTitle(ChatColor.DARK_PURPLE + eye.name() + " EYE", ChatColor.GRAY + "Power Awakened", 10, 70, 20);
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);
        }
    }

    public Eye getEye(Player p) {
        String e = p.getPersistentDataContainer().get(eyeKey, PersistentDataType.STRING);
        return e != null ? Eye.valueOf(e) : null;
    }

    public boolean hasEye(Player p) { return getEye(p) != null; }

    public int getLevel(Player p) { return p.getPersistentDataContainer().getOrDefault(levelKey, PersistentDataType.INTEGER, 1); }

    public void addXp(Player p, int amount) {
        int lvl = getLevel(p);
        if (lvl >= 3) return;
        int xp = p.getPersistentDataContainer().getOrDefault(xpKey, PersistentDataType.INTEGER, 0) + amount;
        if (xp >= 100 * lvl) {
            p.getPersistentDataContainer().set(levelKey, PersistentDataType.INTEGER, lvl + 1);
            p.getPersistentDataContainer().set(xpKey, PersistentDataType.INTEGER, 0);
            p.sendMessage(ChatColor.GOLD + "Level Up! Your Eye is now Level " + (lvl + 1));
        } else {
            p.getPersistentDataContainer().set(xpKey, PersistentDataType.INTEGER, xp);
        }
    }

    public void openGUI(Player p) {
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.DARK_PURPLE + "Ancient Eye Info");
        if (hasEye(p)) {
            Eye eye = getEye(p);
            ItemStack item = new ItemStack(Material.ENDER_EYE);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + eye.name() + " EYE");
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Level: " + getLevel(p) + "/3",
                ChatColor.GRAY + "XP: " + p.getPersistentDataContainer().getOrDefault(xpKey, PersistentDataType.INTEGER, 0),
                "", ChatColor.AQUA + "Shift+F: " + ChatColor.WHITE + "Primary Ability",
                ChatColor.AQUA + "Shift+Q: " + ChatColor.WHITE + "Secondary Ability"
            ));
            item.setItemMeta(meta);
            gui.setItem(13, item);
        }
        p.openInventory(gui);
    }

    @EventHandler
    public void onGUIClick(InventoryClickEvent e) {
        if (e.getView().getTitle().contains("Ancient Eye Info")) e.setCancelled(true);
    }

    private void startParticleTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (hasEye(p)) {
                        Location loc = p.getEyeLocation().add(p.getLocation().getDirection().multiply(0.4));
                        p.getWorld().spawnParticle(getEye(p).particle, loc, 2, 0.1, 0.1, 0.1, 0);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }
                          }
