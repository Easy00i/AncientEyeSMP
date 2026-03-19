package com.ancienteye;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.potion.*;
import org.bukkit.util.Vector;
import java.util.*;

public class AbilityLogic implements Listener {
    private final AncientEyePlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public AbilityLogic(AncientEyePlugin plugin) { this.plugin = plugin; }

    public void handlePrimary(Player p, EyeType eye) {
        if (isOnCooldown(p, "P")) return;
        setCooldown(p, "P", 10);

        Location loc = p.getLocation();
        Vector dir = p.getEyeLocation().getDirection();

        switch (eye) {
            case VOID -> p.teleport(loc.add(dir.multiply(8))); // Void Blink
            case PHANTOM -> p.setVelocity(dir.multiply(2.5)); // Ghost Dash
            case STORM -> { // Lightning Dash
                p.setVelocity(dir.multiply(2));
                p.getWorld().strikeLightningEffect(p.getLocation());
            }
            case FROST -> p.setVelocity(loc.getDirection().setY(0).multiply(2.5)); // Ice Slide
            case FLAME -> { // Fire Dash
                p.setVelocity(dir.multiply(1.8));
                p.getWorld().spawnParticle(Particle.FLAME, loc, 50, 0.2, 0.2, 0.2, 0.05);
            }
            case SHADOW -> { // Shadow Step (Behind nearest)
                Player t = getNearest(p, 15);
                if (t != null) p.teleport(t.getLocation().add(t.getLocation().getDirection().multiply(-1)));
            }
            case TITAN -> p.getNearbyEntities(6, 6, 6).forEach(en -> en.setVelocity(en.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(1.5))); // Ground Slam
            case HUNTER -> { // Hunter Dash (Toward enemy)
                Player t = getNearest(p, 30);
                if(t != null) p.setVelocity(t.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(2));
            }
            case GRAVITY -> p.getNearbyEntities(7, 7, 7).forEach(en -> en.setVelocity(loc.toVector().subtract(en.getLocation().toVector()).normalize().multiply(1.2))); // Gravity Pull
            case WIND -> p.setVelocity(dir.multiply(3)); // Wind Dash
            case POISON -> { // Poison Strike (Apply to targeted)
                LivingEntity t = getTargetEntity(p, 5);
                if(t != null) t.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 2));
            }
            case LIGHT -> p.getNearbyEntities(10, 10, 10).forEach(e -> { if(e instanceof Player t) t.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 1)); }); // Flash Burst
            case EARTH -> { // Earth Wall
                Block b = p.getTargetBlockExact(3);
                if (b != null) b.getLocation().add(0, 1, 0).getBlock().setType(Material.COBBLESTONE_WALL);
            }
            case CRYSTAL -> p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 160, 3)); // Crystal Shield
            case ECHO -> p.getNearbyEntities(25, 25, 25).forEach(e -> { if(e instanceof Player t) t.setGlowing(true); }); // Echo Pulse
            case RAGE -> p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 200, 2)); // Rage Mode
            case SPIRIT -> p.setHealth(Math.min(p.getHealth() + 4, 20)); // Spirit Heal
            case TIME -> p.getNearbyEntities(8, 8, 8).forEach(e -> { if(e instanceof LivingEntity le) le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 4)); }); // Time Slow
            case WARRIOR -> p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 100, 1)); // Warrior Strength

            // --- EVENT EYES ---
            case METEOR -> { // Meteor Crash
                p.setVelocity(new Vector(0, 2, 0));
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    p.setVelocity(new Vector(0, -4, 0));
                    p.getWorld().createExplosion(p.getLocation(), 4f, false, false);
                }, 15L);
            }
            case MIRAGE -> { // Mirage Army (Particles to confuse)
                for(int i=0; i<5; i++) p.getWorld().spawnParticle(Particle.CLOUD, loc.add(new Random().nextDouble()*2, 1, new Random().nextDouble()*2), 20);
            }
            case OCEAN -> p.getNearbyEntities(8, 8, 8).forEach(e -> e.setVelocity(dir.multiply(2.5))); // Tidal Crash
            case ECLIPSE -> { // Eclipse Blast
                p.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc, 200, 4, 4, 4, 0.05);
                p.getNearbyEntities(5, 5, 5).forEach(e -> { if(e instanceof LivingEntity le) le.damage(6); });
            }
            case GUARDIAN -> p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 300, 4)); // Guardian Dome
        }
        p.playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1, 1);
    }

    public void handleSecondary(Player p, EyeType eye) {
        if (isOnCooldown(p, "S")) return;
        setCooldown(p, "S", 15);

        Location loc = p.getLocation();
        switch (eye) {
            case VOID -> p.getNearbyEntities(10, 10, 10).forEach(e -> e.setVelocity(loc.toVector().subtract(e.getLocation().toVector()).normalize().multiply(2)));
            case PHANTOM -> p.getNearbyEntities(25, 25, 25).forEach(e -> { if(e instanceof Player t) t.removePotionEffect(PotionEffectType.INVISIBILITY); });
            case STORM -> { LivingEntity t = getTargetEntity(p, 20); if(t != null) t.getWorld().strikeLightning(t.getLocation()); }
            case FROST -> p.getNearbyEntities(6, 6, 6).forEach(e -> { if(e instanceof LivingEntity le) le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 10)); });
            case FLAME -> p.getWorld().createExplosion(loc, 4f, false, false);
            case SHADOW -> p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 200, 1));
            case TITAN -> p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 120, 2));
            case HUNTER -> { LivingEntity t = getTargetEntity(p, 30); if(t != null) t.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 200, 1)); }
            case GRAVITY -> p.setVelocity(new Vector(0, 2.5, 0));
            case WIND -> p.getNearbyEntities(10, 10, 10).forEach(e -> e.setVelocity(e.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(3)));
            case POISON -> p.getWorld().spawnParticle(Particle.SNEEZE, loc, 300, 3, 2, 3, 0.1);
            case LIGHT -> { // Light Beam
                p.getWorld().strikeLightning(p.getTargetBlockExact(40).getLocation());
            }
            case EARTH -> p.getNearbyEntities(6, 6, 6).forEach(e -> e.setVelocity(new Vector(0, 1.5, 0)));
            case CRYSTAL -> p.getNearbyEntities(7, 7, 7).forEach(e -> { if(e instanceof LivingEntity le) le.damage(6); });
            case ECHO -> p.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2, 0.5f);
            case RAGE -> p.getNearbyEntities(6, 6, 6).forEach(e -> e.setVelocity(e.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(3)));
            case SPIRIT -> p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 120, 1));
            case TIME -> p.setVelocity(p.getEyeLocation().getDirection().multiply(4));
            case WARRIOR -> p.setVelocity(new Vector(0, 1, 0));

            // --- EVENT EYES ---
            case METEOR -> p.getNearbyEntities(12, 12, 12).forEach(e -> e.setFireTicks(100));
            case MIRAGE -> { p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 4)); p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 200, 1)); }
            case OCEAN -> p.getNearbyEntities(10, 10, 10).forEach(e -> { if(e instanceof LivingEntity le) le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 150, 3)); });
            case ECLIPSE -> p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 200, 1));
            case GUARDIAN -> p.getNearbyEntities(15, 15, 15).forEach(e -> e.setVelocity(e.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(4)));
        }
        p.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 0.5f);
    }

    // --- Helper for Aim-Based Powers ---
    private LivingEntity getTargetEntity(Player player, int range) {
        List<Entity> nearby = player.getNearbyEntities(range, range, range);
        for (Entity e : nearby) {
            if (e instanceof LivingEntity le && player.hasLineOfSight(e)) {
                Vector toEntity = le.getEyeLocation().toVector().subtract(player.getEyeLocation().toVector());
                double dot = toEntity.normalize().dot(player.getEyeLocation().getDirection());
                if (dot > 0.98) return le; // Very accurate aim
            }
        }
        return null;
    }

    private Player getNearest(Player p, double r) {
        return p.getNearbyEntities(r, r, r).stream().filter(en -> en instanceof Player && en != p).map(en -> (Player) en).findFirst().orElse(null);
    }

    private boolean isOnCooldown(Player p, String type) {
        String key = p.getUniqueId().toString() + type;
        if (cooldowns.containsKey(key) && cooldowns.get(key) > System.currentTimeMillis()) {
            p.sendMessage("§c§lWAIT! §7Cooldown: §f" + (cooldowns.get(key) - System.currentTimeMillis()) / 1000 + "s");
            return true;
        }
        return false;
    }

    private void setCooldown(Player p, String type, int seconds) {
        int level = plugin.getDataManager().getLevel(p);
        int finalTime = Math.max(2, seconds - (level - 1));
        cooldowns.put(p.getUniqueId().toString() + type, System.currentTimeMillis() + (finalTime * 1000L));
    }
}
