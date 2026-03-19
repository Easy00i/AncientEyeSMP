package com.ancienteye;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.potion.*;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import java.util.*;

public class AbilityLogic {
    private final AncientEyePlugin plugin;
    private final Map<String, Long> cooldowns = new HashMap<>();

    public AbilityLogic(AncientEyePlugin plugin) { this.plugin = plugin; }

    // --- PRIMARY ABILITIES (SHIFT + F) ---
    public void activatePrimary(Player p, EyeType eye) {
        if (checkCooldown(p, "P")) return;
        setCooldown(p, "P", 12); 

        Location loc = p.getLocation();
        Vector dir = p.getEyeLocation().getDirection().normalize();
        double dmgMod = getDamageMultiplier(p);

        switch (eye) {
            case VOID -> p.teleport(loc.add(dir.multiply(8))); // Void Blink
            case PHANTOM -> p.setVelocity(dir.multiply(2.5).setY(0.3)); // Ghost Dash
            case STORM -> { p.setVelocity(dir.multiply(2.2)); p.getWorld().strikeLightningEffect(loc); } // Lightning Dash
            case FROST -> p.setVelocity(dir.setY(0).multiply(2.5)); // Ice Slide
            case FLAME -> { p.setVelocity(dir.multiply(2.0)); spawnAura(p, Particle.FLAME, 20); } // Fire Dash
            case SHADOW -> { // Shadow Step
                LivingEntity t = getTarget(p, 15);
                if (t != null) p.teleport(t.getLocation().subtract(t.getLocation().getDirection().multiply(1.2)));
            }
            case TITAN -> pushNearby(p, 6, 1.5, true, 4.0 * dmgMod); // Ground Slam
            case HUNTER -> { // Hunter Dash
                LivingEntity t = getTarget(p, 30);
                if (t != null) p.setVelocity(t.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(2.5));
            }
            case GRAVITY -> pullNearby(p, 8, 1.2, 2.0 * dmgMod); // Gravity Pull
            case WIND -> p.setVelocity(dir.multiply(3.0)); // Wind Dash
            case POISON -> applyEffect(p, PotionEffectType.POISON, 100, 2, 8); // Poison Strike
            case LIGHT -> stunNearby(p, 10, 80); // Flash Burst
            case EARTH -> spawnWall(p); // Earth Wall
            case CRYSTAL -> p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 160, 3)); // Crystal Shield
            case ECHO -> revealNearby(p, 20); // Echo Pulse
            case RAGE -> p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 120, 2)); // Rage Mode
            case SPIRIT -> p.setHealth(Math.min(p.getHealth() + (4 * dmgMod), 20)); // Spirit Heal
            case TIME -> p.getNearbyEntities(8, 8, 8).forEach(e -> { if(e instanceof LivingEntity le) le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 3)); }); // Time Slow
            case WARRIOR -> p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 100, 1)); // Warrior Strength
            
            // --- EVENT EYES ---
            case METEOR -> { // Meteor Crash
                p.setVelocity(new Vector(0, 2, 0));
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    p.setVelocity(new Vector(0, -4, 0));
                    damageNearby(p, 6, 8.0 * dmgMod);
                    p.getWorld().spawnParticle(Particle.EXPLOSION, p.getLocation(), 1);
                }, 12L);
            }
            case MIRAGE -> spawnClones(p); // Mirage Army
            case OCEAN -> pushNearby(p, 10, 2.0, false, 5.0 * dmgMod); // Tidal Crash
            case ECLIPSE -> damageNearby(p, 6, 7.0 * dmgMod); // Eclipse Blast
            case GUARDIAN -> createDome(p); // Guardian Dome
        }
        p.playSound(loc, Sound.ENTITY_ENDER_EYE_DEATH, 1, 1);
    }

    // --- SECONDARY ABILITIES (SHIFT + Q) ---
    public void activateSecondary(Player p, EyeType eye) {
        if (checkCooldown(p, "S")) return;
        setCooldown(p, "S", 20);

        Location loc = p.getLocation();
        double dmgMod = getDamageMultiplier(p);

        switch (eye) {
            case VOID -> pullNearby(p, 10, 1.5, 3.0 * dmgMod); // Void Trap
            case PHANTOM -> p.getNearbyEntities(25, 25, 25).forEach(e -> { if(e instanceof Player t) t.removePotionEffect(PotionEffectType.INVISIBILITY); }); // Reveal
            case STORM -> { LivingEntity t = getTarget(p, 25); if(t != null) t.getWorld().strikeLightning(t.getLocation()); } // Thunder Strike
            case FROST -> freezeNearby(p, 6); // Freeze Trap
            case FLAME -> { p.getWorld().createExplosion(loc, 3f, false, false); damageNearby(p, 5, 6.0 * dmgMod); } // Flame Burst
            case SHADOW -> p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 200, 0)); // Shadow Cloak
            case TITAN -> p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 120, 1)); // Titan Strength
            case HUNTER -> markTarget(p, 30); // Mark Target
            case GRAVITY -> p.setVelocity(new Vector(0, 2, 0)); // Gravity Jump
            case WIND -> pushNearby(p, 10, 2.5, false, 2.0); // Wind Push
            case POISON -> spawnAura(p, Particle.SNEEZE, 100); // Poison Cloud
            case LIGHT -> fireBeam(p); // Light Beam
            case EARTH -> pushNearby(p, 8, 1.8, true, 4.0); // Earth Slam
            case CRYSTAL -> damageNearby(p, 6, 5.0 * dmgMod); // Crystal Spikes
            case ECHO -> damageNearby(p, 10, 4.0); // Echo Blast
            case RAGE -> pushNearby(p, 6, 2.0, false, 5.0); // Rage Smash
            case SPIRIT -> p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 120, 0)); // Spirit Form
            case TIME -> p.setVelocity(p.getEyeLocation().getDirection().multiply(3)); // Time Dash
            
            // --- EVENT EYES ---
            case METEOR -> p.getNearbyEntities(10, 10, 10).forEach(e -> e.setFireTicks(100)); // Meteor Shower
            case MIRAGE -> { p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 160, 3)); p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 160, 0)); } // Escape
            case OCEAN -> p.getNearbyEntities(8, 8, 8).forEach(e -> { if(e instanceof LivingEntity le) le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 4)); }); // Prison
            case ECLIPSE -> p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 160, 0)); // Shadow Phase
            case GUARDIAN -> pushNearby(p, 15, 3.0, true, 6.0); // Titan Shockwave
        }
        p.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1, 0.5f);
    }

    // --- CORE MECHANICS & HELPERS ---

    private double getDamageMultiplier(Player p) {
        int lvl = plugin.getPlayerData().getLevel(p);
        return (lvl == 3) ? 1.5 : (lvl == 2) ? 1.2 : 1.0;
    }

    private void damageNearby(Player p, double r, double d) {
        p.getNearbyEntities(r, r, r).forEach(e -> { if(e instanceof LivingEntity le && e != p) le.damage(d, p); });
    }

    private void pushNearby(Player p, double r, double f, boolean up, double d) {
        p.getNearbyEntities(r, r, r).forEach(e -> {
            if(e instanceof LivingEntity le && e != p) {
                Vector v = e.getLocation().toVector().subtract(p.getLocation().toVector()).normalize().multiply(f);
                if(up) v.setY(0.5);
                e.setVelocity(v);
                le.damage(d, p);
            }
        });
    }

    private void pullNearby(Player p, double r, double f, double d) {
        p.getNearbyEntities(r, r, r).forEach(e -> {
            if(e instanceof LivingEntity le && e != p) {
                e.setVelocity(p.getLocation().toVector().subtract(e.getLocation().toVector()).normalize().multiply(f));
                le.damage(d, p);
            }
        });
    }

    private LivingEntity getTarget(Player p, double range) {
        RayTraceResult res = p.getWorld().rayTraceEntities(p.getEyeLocation(), p.getEyeLocation().getDirection(), range, 1.5, (e) -> e != p);
        return (res != null && res.getHitEntity() instanceof LivingEntity le) ? le : null;
    }

    private void freezeNearby(Player p, double r) {
        p.getNearbyEntities(r, r, r).forEach(e -> { if(e instanceof LivingEntity le) le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 10)); });
    }

    private void fireBeam(Player p) {
        Block b = p.getTargetBlockExact(30);
        if(b != null) p.getWorld().strikeLightning(b.getLocation());
    }

    private void revealNearby(Player p, double r) {
        p.getNearbyEntities(r, r, r).forEach(e -> { if(e instanceof LivingEntity le) le.setGlowing(true); });
    }

    private void markTarget(Player p, double r) {
        LivingEntity t = getTarget(p, r);
        if(t != null) t.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 200, 0));
    }

    private void spawnClones(Player p) {
        for(int i=0; i<5; i++) p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation().add(Math.random()*2, 1, Math.random()*2), 20);
    }

    private void createDome(Player p) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 4));
        spawnAura(p, Particle.TOTEM_OF_UNDYING, 100);
    }

    private void spawnWall(Player p) {
        Block b = p.getTargetBlockExact(4);
        if(b != null) b.getLocation().add(0, 1, 0).getBlock().setType(Material.COBBLESTONE);
    }

    private void spawnAura(Player p, Particle part, int count) {
        p.getWorld().spawnParticle(part, p.getLocation(), count, 0.5, 1, 0.5, 0.05);
    }

    private void applyEffect(Player p, PotionEffectType type, int time, int amp, double r) {
        LivingEntity t = getTarget(p, r);
        if(t != null) t.addPotionEffect(new PotionEffect(type, time, amp));
    }

    private void stunNearby(Player p, double r, int t) {
        p.getNearbyEntities(r, r, r).forEach(e -> { if(e instanceof LivingEntity le && e != p) le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, t, 0)); });
    }

    private boolean checkCooldown(Player p, String type) {
        String key = p.getUniqueId().toString() + type;
        if (cooldowns.containsKey(key) && cooldowns.get(key) > System.currentTimeMillis()) {
            p.sendMessage("§c§lWAIT! §7Cooldown: " + (cooldowns.get(key) - System.currentTimeMillis())/1000 + "s");
            return true;
        }
        return false;
    }

    private void setCooldown(Player p, String type, int sec) {
        int lvl = plugin.getPlayerData().getLevel(p);
        int finalSec = Math.max(2, sec - (lvl * 2)); 
        cooldowns.put(p.getUniqueId().toString() + type, System.currentTimeMillis() + (finalSec * 1000L));
    }

    // ... (Purana imports aur abilities wala code same rahega)

    // --- 2. THE GUI SYSTEM (/eye command) ---
    public void openEyeGUI(Player p) {
        // 27 slot ki inventory (3 rows)
        Inventory gui = Bukkit.createInventory(null, 27, "§8Your Ancient Eye Status");
        
        EyeType type = plugin.getPlayerData().getEye(p);
        int level = plugin.getPlayerData().getLevel(p);
        int xp = plugin.getPlayerData().getXP(p); // Make sure you have getXP in PlayerData
        int nextLevelXP = 100; // Requirement per level

        // XP Progress Bar Design
        String progressBar = generateProgressBar(xp, nextLevelXP);
        String color = (level == 3) ? "§e§l" : "§b";

        // Eye Item (Center slot 13)
        ItemStack eyeItem = new ItemStack(Material.ENDER_EYE);
        ItemMeta meta = eyeItem.getItemMeta();
        meta.setDisplayName(color + type.name().replace("_", " ") + " EYE");
        
        List<String> lore = new ArrayList<>();
        lore.add("§8Ancient Artifact");
        lore.add("");
        lore.add("§7Level: §f" + level + "/3");
        lore.add("§7Progress: " + progressBar + " §8(" + xp + "/" + nextLevelXP + ")");
        lore.add("");
        lore.add("§6§lCurrent Buffs:");
        lore.add("§e• §7Damage: §c+" + (int)((getDamageMultiplier(p) - 1) * 100) + "%");
        lore.add("§e• §7Cooldown: §a-" + (level * 2) + "s");
        lore.add("");
        lore.add("§b§lAbilities:");
        lore.add("§f• Shift+F: §bPrimary");
        lore.add("§f• Shift+Q: §bSecondary");
        lore.add("");
        lore.add("§e§oThe power is bound to your soul.");
        
        meta.setLore(lore);
        eyeItem.setItemMeta(meta);

        // GUI Background (Panes)
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta gMeta = glass.getItemMeta();
        gMeta.setDisplayName(" ");
        glass.setItemMeta(gMeta);

        for (int i = 0; i < 27; i++) {
            if (i == 13) gui.setItem(i, eyeItem);
            else gui.setItem(i, glass);
        }

        p.openInventory(gui);
        p.playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, 1.2f);
    }

    // Helper to make a cool progress bar
    private String generateProgressBar(int current, int max) {
        int bars = 10;
        int completed = (int) ((double) current / max * bars);
        StringBuilder sb = new StringBuilder("§a");
        for (int i = 0; i < bars; i++) {
            if (i == completed) sb.append("§7");
            sb.append("┃");
        }
        return sb.toString();
    }
    
}

