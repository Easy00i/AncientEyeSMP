package com.ancienteye;

import org.bukkit.*;
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
        setCooldown(p, "P", 10); // 10s Base

        Location loc = p.getLocation();
        Vector dir = p.getEyeLocation().getDirection();

        switch (eye) {
            case VOID -> {
                p.teleport(loc.add(dir.multiply(8)));
                p.spawnParticle(Particle.PORTAL, p.getLocation(), 50, 0.5, 1, 0.5, 0.1);
            }
            case PHANTOM -> p.setVelocity(dir.multiply(2.5));
            case STORM -> {
                p.setVelocity(dir.multiply(2));
                p.getWorld().strikeLightningEffect(p.getLocation());
            }
            case FROST -> p.setVelocity(loc.getDirection().setY(0).multiply(3));
            case FLAME -> {
                p.setVelocity(dir.multiply(1.8));
                p.getWorld().spawnParticle(Particle.FLAME, loc, 40, 0.5, 0.5, 0.5, 0.1);
            }
            case SHADOW -> {
                Player target = getNearest(p, 15);
                if (target != null) p.teleport(target.getLocation().add(target.getLocation().getDirection().multiply(-1)));
            }
            case TITAN -> {
                p.getNearbyEntities(6, 6, 6).forEach(en -> en.setVelocity(en.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(2)));
            }
            case HUNTER -> {
                Player t = getNearest(p, 30);
                if(t != null) p.setVelocity(t.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(2));
            }
            case GRAVITY -> p.getNearbyEntities(7, 7, 7).forEach(en -> en.setVelocity(loc.toVector().subtract(en.getLocation().toVector()).normalize().multiply(1.5)));
            case WIND -> p.setVelocity(dir.multiply(3));
            case POISON -> {
                LivingEntity t = (LivingEntity) getNearest(p, 5);
                if(t != null) t.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 2));
            }
            case LIGHT -> p.getNearbyEntities(10, 10, 10).forEach(e -> { if(e instanceof Player t) t.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 1)); });
            case EARTH -> p.getLocation().add(dir.multiply(2)).getBlock().setType(Material.COBBLESTONE_WALL);
            case CRYSTAL -> p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 120, 3));
            case ECHO -> p.getNearbyEntities(25, 25, 25).forEach(e -> { if(e instanceof Player t) t.setGlowing(true); });
            case RAGE -> p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 200, 2));
            case SPIRIT -> p.setHealth(Math.min(p.getHealth() + 6, p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue()));
            case TIME -> p.getNearbyEntities(8, 8, 8).forEach(e -> { if(e instanceof LivingEntity le) le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 4)); });
            
            // EVENT EYES
            case METEOR -> {
                p.setVelocity(new Vector(0, 2, 0));
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    p.setVelocity(new Vector(0, -4, 0));
                    p.getWorld().createExplosion(p.getLocation(), 3f, false, false);
                }, 15L);
            }
            case MIRAGE -> {
                for(int i=0; i<5; i++) p.getWorld().spawnParticle(Particle.CLOUD, loc.add(new Random().nextDouble(), 1, new Random().nextDouble()), 20);
            }
            case OCEAN -> {
                p.getNearbyEntities(8, 8, 8).forEach(e -> e.setVelocity(dir.multiply(2.5)));
            }
            case ECLIPSE -> p.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc, 200, 4, 4, 4, 0.05);
            case GUARDIAN -> p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 300, 4));
        }
        p.playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1, 1);
    }

    public void handleSecondary(Player p, EyeType eye) {
        if (isOnCooldown(p, "S")) return;
        setCooldown(p, "S", 15); // 15s Base

        Location loc = p.getLocation();
        switch (eye) {
            case VOID -> p.getNearbyEntities(10, 10, 10).forEach(e -> e.setVelocity(loc.toVector().subtract(e.getLocation().toVector()).normalize().multiply(2)));
            case PHANTOM -> p.getNearbyEntities(25, 25, 25).forEach(e -> { if(e instanceof Player t) t.removePotionEffect(PotionEffectType.INVISIBILITY); });
            case STORM -> { Player t = getNearest(p, 20); if(t != null) t.getWorld().strikeLightning(t.getLocation()); }
            case FROST -> p.getNearbyEntities(6, 6, 6).forEach(e -> { if(e instanceof LivingEntity le) le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 10)); });
            case FLAME -> p.getWorld().createExplosion(loc, 4f, false, false);
            case SHADOW -> p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 200, 1));
            case TITAN -> p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 160, 2));
            case HUNTER -> { Player t = getNearest(p, 30); if(t != null) t.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 200, 1)); }
            case GRAVITY -> p.setVelocity(new Vector(0, 2.5, 0));
            case WIND -> p.getNearbyEntities(10, 10, 10).forEach(e -> e.setVelocity(e.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(3)));
            case POISON -> p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 200, 5, 2, 5, 0.1);
            case LIGHT -> p.getWorld().strikeLightning(p.getTargetBlockExact(40).getLocation());
            case EARTH -> p.getNearbyEntities(6, 6, 6).forEach(e -> e.setVelocity(new Vector(0, 1.5, 0)));
            case CRYSTAL -> p.getNearbyEntities(7, 7, 7).forEach(e -> { if(e instanceof LivingEntity le) le.damage(6); });
            case ECHO -> p.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2, 0.5f);
            case RAGE -> p.getNearbyEntities(6, 6, 6).forEach(e -> e.setVelocity(e.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(3)));
            case SPIRIT -> p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 120, 1));
            case TIME -> p.setVelocity(p.getEyeLocation().getDirection().multiply(5));
            
            // EVENT EYES
            case METEOR -> p.getNearbyEntities(12, 12, 12).forEach(e -> e.setFireTicks(100));
            case MIRAGE -> p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 4));
            case OCEAN -> p.getNearbyEntities(10, 10, 10).forEach(e -> { if(e instanceof LivingEntity le) le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 150, 3)); });
            case ECLIPSE -> p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 2));
            case GUARDIAN -> p.getNearbyEntities(15, 15, 15).forEach(e -> e.setVelocity(e.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(4)));
        }
        p.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 0.5f);
    }

    // --- PASSIVES & COMBAT BALANCE ---
    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        EyeType eye = plugin.getDataManager().getEye(p);
        if (eye == EyeType.GRAVITY && e.getCause() == EntityDamageEvent.DamageCause.FALL) e.setCancelled(true);
        if (eye == EyeType.FLAME && (e.getCause() == EntityDamageEvent.DamageCause.FIRE || e.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK)) e.setCancelled(true);
        if (eye == EyeType.GUARDIAN) e.setDamage(e.getDamage() * 0.6); // 40% reduction
    }

    @EventHandler
    public void onPvp(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player p) {
            EyeType eye = plugin.getDataManager().getEye(p);
            if (eye == EyeType.RAGE && p.getHealth() < 10) e.setDamage(e.getDamage() * 1.6);
            if (eye == EyeType.ECLIPSE && p.getWorld().getTime() > 13000) e.setDamage(e.getDamage() * 1.3);
        }
        if (e.getEntity() instanceof Player target) {
            EyeType eye = plugin.getDataManager().getEye(target);
            if ((eye == EyeType.VOID || eye == EyeType.MIRAGE) && new Random().nextInt(100) < 15) {
                e.setCancelled(true);
                target.sendMessage("§d§lDODGED!");
            }
        }
    }

    private Player getNearest(Player p, double r) {
        return p.getNearbyEntities(r, r, r).stream().filter(en -> en instanceof Player && en != p).map(en -> (Player) en).findFirst().orElse(null);
    }

    private boolean isOnCooldown(Player p, String type) {
        String key = p.getUniqueId().toString() + type;
        if (cooldowns.containsKey(key) && cooldowns.get(key) > System.currentTimeMillis()) {
            p.sendMessage("§cCooldown: " + (cooldowns.get(key) - System.currentTimeMillis()) / 1000 + "s");
            return true;
        }
        return false;
    }

    private void setCooldown(Player p, String type, int seconds) {
        int level = plugin.getDataManager().getLevel(p);
        int finalTime = seconds - (level - 1); // Reduced by level
        cooldowns.put(p.getUniqueId().toString() + type, System.currentTimeMillis() + (finalTime * 1000L));
    }
}
