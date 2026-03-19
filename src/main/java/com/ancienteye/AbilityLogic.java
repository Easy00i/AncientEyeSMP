package com.ancienteye;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class AbilityLogic implements Listener {

    public void executePrimary(Player p, EyeType type) {
        Location loc = p.getLocation();
        Vector dir = p.getEyeLocation().getDirection();

        switch (type) {
            case VOID -> {
                p.teleport(loc.add(dir.multiply(8)));
                p.getWorld().spawnParticle(Particle.PORTAL, p.getLocation(), 30, 0.5, 1, 0.5, 0.1);
                p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            }
            case STORM -> {
                p.setVelocity(dir.multiply(2.5));
                p.getWorld().spawnParticle(Particle.FIREWORK, p.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
                p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 2f);
            }
            case FLAME -> {
                p.setVelocity(dir.multiply(1.8));
                p.getWorld().spawnParticle(Particle.FLAME, p.getLocation(), 40, 0.5, 0.5, 0.5, 0.1);
                p.playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 1);
            }
            case TITAN -> {
                p.getNearbyEntities(6, 6, 6).forEach(en -> {
                    if (en instanceof LivingEntity le && en != p) {
                        le.damage(5);
                        le.setVelocity(le.getLocation().toVector().subtract(p.getLocation().toVector()).normalize().multiply(1.5));
                    }
                });
                p.getWorld().spawnParticle(Particle.EXPLOSION, p.getLocation(), 5);
                p.playSound(p.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1, 0.5f);
            }
            case METEOR -> { // Event Eye
                p.setVelocity(new Vector(0, 2, 0));
                Bukkit.getScheduler().runTaskLater(AncientEyePlugin.get(), () -> {
                    p.setVelocity(new Vector(0, -3, 0));
                    p.getWorld().spawnParticle(Particle.EXPLOSION, p.getLocation(), 10);
                }, 15L);
            }
            // Baki sab eyes ka logic yahan add hoga same pattern mein...
        }
    }

    public void executeSecondary(Player p, EyeType type) {
        switch (type) {
            case VOID -> {
                p.getNearbyEntities(8, 8, 8).forEach(en -> {
                    if (en instanceof Player target) {
                        target.setVelocity(p.getLocation().toVector().subtract(target.getLocation().toVector()).normalize().multiply(1.2));
                    }
                });
                p.getWorld().spawnParticle(Particle.LARGE_SMOKE, p.getLocation(), 50, 2, 2, 2, 0.05);
            }
            case STORM -> {
                Player target = getTargetPlayer(p, 15);
                if (target != null) {
                    target.getWorld().strikeLightningEffect(target.getLocation());
                    target.damage(6);
                }
            }
            case FLAME -> {
                p.getNearbyEntities(5, 5, 5).forEach(en -> {
                    if (en instanceof LivingEntity le && en != p) le.setFireTicks(100);
                });
                p.getWorld().spawnParticle(Particle.LAVA, p.getLocation(), 30, 2, 2, 2, 0.1);
            }
            case SHADOW -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100, 1, false, false, false));
                p.getWorld().spawnParticle(Particle.SQUID_INK, p.getLocation(), 40, 1, 1, 1, 0.1);
            }
            // Baki saari abilities...
        }
    }

    // --- PASSIVE ABILITIES LOGIC ---
    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        EyeType eye = AncientEyePlugin.get().getPlayerData().getEye(p);

        if (eye == EyeType.GRAVITY && e.getCause() == EntityDamageEvent.DamageCause.FALL) {
            e.setCancelled(true); // Gravity eye cancels fall damage
        }
        if (eye == EyeType.FLAME && (e.getCause() == EntityDamageEvent.DamageCause.FIRE || e.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK)) {
            e.setCancelled(true); // Flame eye resists fire
        }
        if (eye == EyeType.TITAN && e.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
            e.setDamage(e.getDamage() * 0.5); // Titan reduces explosion damage
        }
    }

    private Player getTargetPlayer(Player p, int range) {
        return p.getNearbyEntities(range, range, range).stream()
                .filter(en -> en instanceof Player)
                .map(en -> (Player) en)
                .findFirst().orElse(null);
    }
        }
              
