package com.ancienteye;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.attribute.Attribute;
import org.bukkit.Particle;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.entity.*;
import org.bukkit.potion.*;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import java.util.*;

public class AbilityLogic implements Listener {

    private final AncientEyePlugin plugin;
    private final Map<String, Long> cooldowns = new HashMap<>();

    public AbilityLogic(AncientEyePlugin plugin) { this.plugin = plugin; }

    // =====================================================================
    //  PRIMARY ABILITIES  (SHIFT + F)
    // =====================================================================
    public void activatePrimary(Player p, EyeType eye) {
        if (checkCooldown(p, "P")) return;
        setCooldown(p, "P", 12);

        Location loc    = p.getLocation().clone();
        Vector   dir    = p.getEyeLocation().getDirection().normalize();
        double   dmgMod = getDamageMultiplier(p);
        World    world  = p.getWorld();

        switch (eye) {

            // ----------------------------------------------------------
            // 1. VOID EYE  –  Void Blink  (teleport 8 blocks forward)
            // ----------------------------------------------------------
            case VOID -> {
                world.spawnParticle(Particle.PORTAL, loc, 40, 0.4, 0.8, 0.4, 0.2);
                world.spawnParticle(Particle.REVERSE_PORTAL, loc, 20, 0.3, 0.6, 0.3, 0.1);

                Location dest = getSafeLocation(loc.clone().add(dir.clone().multiply(8)));
                world.spawnParticle(Particle.PORTAL, dest, 40, 0.4, 0.8, 0.4, 0.2);
                world.spawnParticle(Particle.REVERSE_PORTAL, dest, 20, 0.3, 0.6, 0.3, 0.1);
                world.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);
                p.teleport(dest);
            }

            // ----------------------------------------------------------
            // 2. PHANTOM EYE  –  Ghost Dash (fast forward dash)
            // ----------------------------------------------------------
            case PHANTOM -> {
                world.spawnParticle(Particle.CLOUD, loc, 25, 0.3, 0.5, 0.3, 0.05);
                world.spawnParticle(Particle.SMOKE, loc, 15, 0.2, 0.4, 0.2, 0.03);
                p.setVelocity(dir.clone().multiply(2.5).setY(0.3));
                world.playSound(loc, Sound.ENTITY_PHANTOM_FLAP, 1f, 1.5f);
            }

            // ----------------------------------------------------------
            // 3. STORM EYE  –  Lightning Dash (dash + lightning effect)
            // ----------------------------------------------------------
            case STORM -> {
                world.spawnParticle(Particle.ELECTRIC_SPARK, loc, 40, 0.5, 1, 0.5, 0.1);
                p.setVelocity(dir.clone().multiply(2.2).setY(0.25));

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    world.strikeLightningEffect(p.getLocation());
                    world.spawnParticle(Particle.ELECTRIC_SPARK, p.getLocation(), 60, 0.5, 1, 0.5, 0.15);
                }, 5L);
                world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.4f);
            }

            // ----------------------------------------------------------
            // 4. FROST EYE  –  Ice Slide (horizontal slide)
            // ----------------------------------------------------------
            case FROST -> {
                world.spawnParticle(Particle.SNOWFLAKE, loc, 40, 0.5, 0.3, 0.5, 0.08);
                world.spawnParticle(Particle.WHITE_ASH, loc, 20, 0.4, 0.2, 0.4, 0.05);
                Vector slide = dir.clone().setY(0).normalize().multiply(2.5);
                p.setVelocity(slide);
                world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.8f);
            }

            // ----------------------------------------------------------
            // 5. FLAME EYE  –  Fire Dash (dash + flame trail)
            // ----------------------------------------------------------
            case FLAME -> {
                world.spawnParticle(Particle.FLAME, loc, 30, 0.4, 0.6, 0.4, 0.08);
                world.spawnParticle(Particle.LAVA, loc, 10, 0.3, 0.3, 0.3, 0.05);
                p.setVelocity(dir.clone().multiply(2.0).setY(0.2));

                // Leave flame particles along dash trail
                Bukkit.getScheduler().runTaskLater(plugin, () ->
                    world.spawnParticle(Particle.FLAME, p.getLocation(), 25, 0.3, 0.5, 0.3, 0.06), 4L);
                world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1f, 1.3f);
            }

            // ----------------------------------------------------------
            // 6. SHADOW EYE  –  Shadow Step (teleport behind nearest player)
            // ----------------------------------------------------------
            case SHADOW -> {
                LivingEntity target = getTarget(p, 15);
                if (target != null) {
                    world.spawnParticle(Particle.SMOKE, loc, 30, 0.3, 0.5, 0.3, 0.05);
                    Vector behind = target.getLocation().getDirection().normalize().multiply(1.5);
                    Location dest = getSafeLocation(target.getLocation().clone().subtract(behind));
                    world.spawnParticle(Particle.SMOKE, dest, 30, 0.3, 0.5, 0.3, 0.05);
                    world.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.7f);
                    p.teleport(dest);
                } else {
                    p.sendMessage("§7No target in range.");
                }
            }

            // ----------------------------------------------------------
            // 7. TITAN EYE  –  Ground Slam (knockback + heavy damage)
            // ----------------------------------------------------------
            case TITAN -> {
                world.spawnParticle(Particle.EXPLOSION, loc, 3, 0.5, 0, 0.5, 0);
                world.spawnParticle(Particle.DUST_PLUME, loc, 80, 1, 0.1, 1, 0.15,
                    Material.STONE.createBlockData());
                world.spawnParticle(Particle.DUST, loc, 50, 1.5, 0.2, 1.5, 0,
                    new Particle.DustOptions(Color.fromRGB(100, 60, 30), 2.5f));
                world.playSound(loc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 0.5f);
                world.playSound(loc, Sound.BLOCK_STONE_BREAK, 1f, 0.6f);
                pushNearby(p, 6, 1.5, true, 4.0 * dmgMod);
            }

            // ----------------------------------------------------------
            // 8. HUNTER EYE  –  Hunter Dash (fast dash toward enemy)
            // ----------------------------------------------------------
            case HUNTER -> {
                LivingEntity target = getTarget(p, 30);
                if (target != null) {
                    world.spawnParticle(Particle.CRIT, loc, 20, 0.2, 0.3, 0.2, 0.1);
                    Vector dash = target.getLocation().toVector()
                        .subtract(loc.toVector()).normalize().multiply(2.5);
                    p.setVelocity(dash);
                    world.playSound(loc, Sound.ENTITY_ARROW_SHOOT, 1f, 1.5f);
                } else {
                    p.sendMessage("§7No target in range.");
                }
            }

            // ----------------------------------------------------------
            // 9. GRAVITY EYE  –  Gravity Pull (pull nearby enemies)
            // ----------------------------------------------------------
            case GRAVITY -> {
                world.spawnParticle(Particle.REVERSE_PORTAL, loc, 60, 1.5, 1.5, 1.5, 0.1);
                world.spawnParticle(Particle.PORTAL, loc, 40, 1, 1, 1, 0.08);
                world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 0.5f);
                pullNearby(p, 8, 1.2, 2.0 * dmgMod);
            }

            // ----------------------------------------------------------
            // 10. WIND EYE  –  Wind Dash (very fast forward dash)
            // ----------------------------------------------------------
            case WIND -> {
                world.spawnParticle(Particle.WHITE_ASH, loc, 40, 0.5, 0.5, 0.5, 0.1);
                world.spawnParticle(Particle.CLOUD, loc, 20, 0.3, 0.3, 0.3, 0.05);
                p.setVelocity(dir.clone().multiply(3.0).setY(0.15));
                world.playSound(loc, Sound.ENTITY_PHANTOM_DEATH, 0.6f, 2.0f);
            }

            // ----------------------------------------------------------
            // 11. POISON EYE  –  Poison Strike (aim: poison targeted enemy)
            // ----------------------------------------------------------
            case POISON -> {
                LivingEntity target = getTarget(p, 8);
                if (target != null) {
                    world.spawnParticle(Particle.ITEM_SLIME, target.getLocation(), 30, 0.4, 0.6, 0.4, 0.05);
                    world.spawnParticle(Particle.SNEEZE, target.getLocation(), 15, 0.3, 0.5, 0.3, 0.03);
                    target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 2));
                    world.playSound(loc, Sound.ENTITY_SLIME_SQUISH, 1f, 0.6f);
                } else {
                    p.sendMessage("§7No target in range.");
                }
            }

            // ----------------------------------------------------------
            // 12. LIGHT EYE  –  Flash Burst (blind nearby enemies)
            // ----------------------------------------------------------
            case LIGHT -> {
                world.spawnParticle(Particle.END_ROD, loc, 80, 2, 2, 2, 0.1);
                world.spawnParticle(Particle.FLASH, loc, 1, 0, 0, 0, 0);
                world.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, 1f, 2.0f);
                stunNearby(p, 10, 80);
            }

            // ----------------------------------------------------------
            // 13. EARTH EYE  –  Earth Wall (wall in front of player)
            // ----------------------------------------------------------
            case EARTH -> {
                world.spawnParticle(Particle.DUST_PLUME, loc, 60, 0.5, 0.5, 0.5, 0.1,
                    Material.DIRT.createBlockData());
                world.playSound(loc, Sound.BLOCK_GRAVEL_BREAK, 1f, 0.5f);
                spawnWall(p);
            }

            // ----------------------------------------------------------
            // 14. CRYSTAL EYE  –  Crystal Shield (resistance + particles)
            // ----------------------------------------------------------
            case CRYSTAL -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 160, 3));
                spawnCrystalAura(p);
                world.playSound(loc, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1f, 1.4f);
                world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.0f);
            }

            // ----------------------------------------------------------
            // 15. ECHO EYE  –  Echo Pulse (reveal nearby players as glowing)
            // ----------------------------------------------------------
            case ECHO -> {
                world.spawnParticle(Particle.SONIC_BOOM, loc, 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.END_ROD, loc, 50, 2.5, 2.5, 2.5, 0.05);
                world.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.7f, 1.8f);
                revealNearby(p, 20);
            }

            // ----------------------------------------------------------
            // 16. RAGE EYE  –  Rage Mode (haste + speed boost)
            // ----------------------------------------------------------
            case RAGE -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 120, 2));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 80, 1));
                world.spawnParticle(Particle.DUST, loc, 60, 0.5, 1, 0.5, 0,
                    new Particle.DustOptions(Color.RED, 2.0f));
                world.spawnParticle(Particle.CRIT, loc, 30, 0.4, 0.8, 0.4, 0.1);
                world.playSound(loc, Sound.ENTITY_RAVAGER_ROAR, 1f, 1.0f);
            }

            // ----------------------------------------------------------
            // 17. SPIRIT EYE  –  Spirit Heal (heal self based on level)
            // ----------------------------------------------------------
            case SPIRIT -> {
                double heal = 4.0 * dmgMod;
                p.setHealth(Math.min(p.getHealth() + heal, p.getAttribute(
                    org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue()));
                world.spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 60, 0.4, 0.8, 0.4, 0.08);
                world.spawnParticle(Particle.HEART, loc, 10, 0.5, 0.5, 0.5, 0.05);
                world.playSound(loc, Sound.ENTITY_WITCH_CELEBRATE, 1f, 1.3f);
            }

            // ----------------------------------------------------------
            // 18. TIME EYE  –  Time Slow (slow all nearby enemies)
            // ----------------------------------------------------------
            case TIME -> {
                world.spawnParticle(Particle.REVERSE_PORTAL, loc, 50, 2, 2, 2, 0.08);
                world.spawnParticle(Particle.DUST, loc, 40, 1.5, 1.5, 1.5, 0,
                    new Particle.DustOptions(Color.fromRGB(180, 130, 255), 1.8f));
                world.playSound(loc, Sound.BLOCK_BEACON_AMBIENT, 1f, 0.3f);
                p.getNearbyEntities(8, 8, 8).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p)
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 3));
                });
            }

            // ----------------------------------------------------------
            // 19. WARRIOR EYE  –  Warrior Strength
            // ----------------------------------------------------------
            case WARRIOR -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 100, 1));
                world.spawnParticle(Particle.DUST, loc, 50, 0.5, 1, 0.5, 0,
                    new Particle.DustOptions(Color.fromRGB(220, 80, 20), 2.0f));
                world.spawnParticle(Particle.CRIT, loc, 25, 0.3, 0.7, 0.3, 0.1);
                world.playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, 1f, 0.8f);
            }

            // =================== EVENT EYES ==========================

            // ----------------------------------------------------------
            // E1. METEOR EYE  –  Meteor Crash (launch up → crash down)
            // ----------------------------------------------------------
            case METEOR -> {
                world.spawnParticle(Particle.FLAME, loc, 30, 0.4, 0.2, 0.4, 0.06);
                world.spawnParticle(Particle.LAVA, loc, 10, 0.3, 0.1, 0.3, 0.05);
                world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1f, 0.6f);
                p.setVelocity(new Vector(0, 2.2, 0));

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    world.spawnParticle(Particle.FLAME, p.getLocation(), 40, 0.5, 1, 0.5, 0.1);
                    p.setVelocity(new Vector(0, -4.5, 0));

                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        Location impact = p.getLocation();
                        world.spawnParticle(Particle.EXPLOSION, impact, 4, 0.8, 0, 0.8, 0);
                        world.spawnParticle(Particle.FLAME, impact, 80, 2, 0.2, 2, 0.12);
                        world.spawnParticle(Particle.LAVA, impact, 20, 1.5, 0.1, 1.5, 0.1);
                        world.spawnParticle(Particle.DUST_PLUME, impact, 60, 1.5, 0.1, 1.5, 0.2,
                            Material.MAGMA_BLOCK.createBlockData());
                        world.playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.7f);
                        world.playSound(impact, Sound.BLOCK_STONE_BREAK, 1f, 0.4f);
                        damageNearby(p, 6, 8.0 * dmgMod);
                        pushNearby(p, 6, 2.0, true, 0);
                    }, 18L);
                }, 12L);
            }

            // ----------------------------------------------------------
            // E2. MIRAGE EYE  –  Mirage Army (illusion clones + particles)
            // ----------------------------------------------------------
            case MIRAGE -> {
                world.playSound(loc, Sound.ENTITY_ENDERMAN_SCREAM, 0.7f, 1.5f);
                for (int i = 0; i < 5; i++) {
                    double angle = i * (Math.PI * 2 / 5);
                    double cx = Math.cos(angle) * 2.5;
                    double cz = Math.sin(angle) * 2.5;
                    Location cloneLoc = loc.clone().add(cx, 0, cz);
                    world.spawnParticle(Particle.CLOUD, cloneLoc, 30, 0.2, 0.8, 0.2, 0.04);
                    world.spawnParticle(Particle.SMOKE, cloneLoc, 15, 0.15, 0.6, 0.15, 0.02);
                    world.spawnParticle(Particle.ENTITY_EFFECT, cloneLoc, 20, 0.2, 0.8, 0.2, 1);
                }
            }

            // ----------------------------------------------------------
            // E3. OCEAN EYE  –  Tidal Crash (water wave push + damage)
            // ----------------------------------------------------------
            case OCEAN -> {
                world.spawnParticle(Particle.SPLASH, loc, 100, 2, 0.5, 2, 0.3);
                world.spawnParticle(Particle.BUBBLE_POP, loc, 60, 2, 0.5, 2, 0.1);
                world.spawnParticle(Particle.RAIN, loc, 80, 3, 1, 3, 0.1);
                world.playSound(loc, Sound.ENTITY_GUARDIAN_ATTACK, 1f, 0.5f);
                world.playSound(loc, Sound.BLOCK_WATER_AMBIENT, 1f, 0.3f);
                pushNearby(p, 10, 2.0, false, 5.0 * dmgMod);
            }

            // ----------------------------------------------------------
            // E4. ECLIPSE EYE  –  Eclipse Blast (dark energy explosion)
            // ----------------------------------------------------------
            case ECLIPSE -> {
                world.spawnParticle(Particle.PORTAL, loc, 80, 2, 2, 2, 0.15);
                world.spawnParticle(Particle.REVERSE_PORTAL, loc, 60, 1.5, 1.5, 1.5, 0.12);
                world.spawnParticle(Particle.DUST, loc, 50, 2, 2, 2, 0,
                    new Particle.DustOptions(Color.BLACK, 3.0f));
                world.spawnParticle(Particle.EXPLOSION, loc, 2, 0.5, 0, 0.5, 0);
                world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, 1f, 0.6f);
                damageNearby(p, 6, 7.0 * dmgMod);
            }

            // ----------------------------------------------------------
            // E5. GUARDIAN EYE  –  Guardian Dome (protective dome)
            // ----------------------------------------------------------
            case GUARDIAN -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 4));
                // Also protect nearby allies
                p.getNearbyEntities(5, 5, 5).forEach(e -> {
                    if (e instanceof Player ally && ally != p)
                        ally.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 2));
                });
                // Dome particle ring effect
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 18) {
                    double rx = Math.cos(angle) * 4;
                    double rz = Math.sin(angle) * 4;
                    world.spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(rx, 1, rz), 3, 0, 0.3, 0, 0.01);
                    world.spawnParticle(Particle.END_ROD, loc.clone().add(rx, 1, rz), 2, 0, 0.2, 0, 0.01);
                }
                world.spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 60, 1, 2, 1, 0.05);
                world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1f, 0.8f);
                world.playSound(loc, Sound.ENTITY_IRON_GOLEM_REPAIR, 1f, 1.2f);
            }
        }    
        p.playSound(loc, Sound.ENTITY_ENDER_EYE_DEATH, 1f, 1f);
    }

    // =====================================================================
    //  SECONDARY ABILITIES  (SHIFT + Q)
    // =====================================================================
    public void activateSecondary(Player p, EyeType eye) {
        if (checkCooldown(p, "S")) return;
        setCooldown(p, "S", 20);

        Location loc    = p.getLocation().clone();
        double   dmgMod = getDamageMultiplier(p);
        World    world  = p.getWorld();

        switch (eye) {

            // ----------------------------------------------------------
            // 1. VOID EYE  –  Void Trap (pull enemies toward you)
            // ----------------------------------------------------------
            case VOID -> {
                world.spawnParticle(Particle.PORTAL, loc, 80, 2, 2, 2, 0.15);
                world.spawnParticle(Particle.REVERSE_PORTAL, loc, 40, 1.5, 1.5, 1.5, 0.1);
                world.playSound(loc, Sound.BLOCK_BEACON_AMBIENT, 1f, 0.4f);
                pullNearby(p, 10, 1.5, 3.0 * dmgMod);
            }

            // ----------------------------------------------------------
            // 2. PHANTOM EYE  –  Phantom Reveal (reveal invisible in 25r)
            // ----------------------------------------------------------
            case PHANTOM -> {
                world.spawnParticle(Particle.END_ROD, loc, 60, 3, 3, 3, 0.06);
                world.playSound(loc, Sound.ENTITY_PHANTOM_BITE, 0.8f, 1.8f);
                p.getNearbyEntities(25, 25, 25).forEach(e -> {
                    if (e instanceof Player t) {
                        t.removePotionEffect(PotionEffectType.INVISIBILITY);
                        t.setGlowing(true);
                        Bukkit.getScheduler().runTaskLater(plugin, () -> t.setGlowing(false), 100L);
                    }
                });
            }

            // ----------------------------------------------------------
            // 3. STORM EYE  –  Thunder Strike (lightning on aimed target)
            // ----------------------------------------------------------
            case STORM -> {
                LivingEntity target = getTarget(p, 25);
                if (target != null) {
                    world.spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation(), 60, 0.5, 1, 0.5, 0.1);
                    world.strikeLightning(target.getLocation());
                    world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 1.0f);
                } else {
                    p.sendMessage("§7No target in range.");
                }
            }

            // ----------------------------------------------------------
            // 4. FROST EYE  –  Freeze Trap (freeze nearby enemies)
            // ----------------------------------------------------------
            case FROST -> {
                world.spawnParticle(Particle.SNOWFLAKE, loc, 60, 2, 0.5, 2, 0.08);
                world.spawnParticle(Particle.WHITE_ASH, loc, 40, 2, 0.3, 2, 0.04);
                world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1f, 0.5f);
                world.playSound(loc, Sound.ENTITY_PLAYER_HURT_FREEZE, 1f, 0.8f);
                freezeNearby(p, 6);
            }

            // ----------------------------------------------------------
            // 5. FLAME EYE  –  Flame Burst (area fire explosion)
            // ----------------------------------------------------------
            case FLAME -> {
                world.spawnParticle(Particle.EXPLOSION, loc, 3, 0.5, 0, 0.5, 0);
                world.spawnParticle(Particle.FLAME, loc, 80, 2, 0.5, 2, 0.15);
                world.spawnParticle(Particle.LAVA, loc, 20, 1.5, 0.3, 1.5, 0.1);
                world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.0f);
                world.createExplosion(loc, 3f, false, false);
                damageNearby(p, 5, 6.0 * dmgMod);
            }

            // ----------------------------------------------------------
            // 6. SHADOW EYE  –  Shadow Cloak (become invisible)
            // ----------------------------------------------------------
            case SHADOW -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 200, 0));
                world.spawnParticle(Particle.SMOKE, loc, 50, 0.4, 0.8, 0.4, 0.06);
                world.spawnParticle(Particle.DUST, loc, 30, 0.3, 0.7, 0.3, 0,
                    new Particle.DustOptions(Color.BLACK, 1.5f));
                world.playSound(loc, Sound.ENTITY_ENDERMAN_STARE, 0.6f, 1.2f);
            }

            // ----------------------------------------------------------
            // 7. TITAN EYE  –  Titan Strength (strength buff)
            // ----------------------------------------------------------
            case TITAN -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 120, 1));
                world.spawnParticle(Particle.DUST, loc, 60, 0.6, 1.2, 0.6, 0,
                    new Particle.DustOptions(Color.fromRGB(180, 90, 20), 2.5f));
                world.spawnParticle(Particle.CRIT, loc, 25, 0.4, 0.8, 0.4, 0.1);
                world.playSound(loc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 0.7f);
            }

            // ----------------------------------------------------------
            // 8. HUNTER EYE  –  Mark Target (glowing, takes extra damage)
            // ----------------------------------------------------------
            case HUNTER -> {
                LivingEntity target = getTarget(p, 30);
                if (target != null) {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 200, 0));
                    target.setMetadata("hunterMarked", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                    Bukkit.getScheduler().runTaskLater(plugin,
                        () -> target.removeMetadata("hunterMarked", plugin), 200L);
                    world.spawnParticle(Particle.CRIT, target.getLocation(), 30, 0.4, 0.8, 0.4, 0.1);
                    world.spawnParticle(Particle.DUST, target.getLocation(), 20, 0.3, 0.6, 0.3, 0,
                        new Particle.DustOptions(Color.RED, 1.5f));
                    world.playSound(loc, Sound.ENTITY_ARROW_HIT_PLAYER, 1f, 1.5f);
                } else {
                    p.sendMessage("§7No target in range.");
                }
            }

            // ----------------------------------------------------------
            // 9. GRAVITY EYE  –  Gravity Jump (launch player high)
            // ----------------------------------------------------------
            case GRAVITY -> {
                world.spawnParticle(Particle.REVERSE_PORTAL, loc, 40, 0.5, 0.3, 0.5, 0.08);
                world.spawnParticle(Particle.PORTAL, loc, 25, 0.4, 0.2, 0.4, 0.06);
                world.playSound(loc, Sound.ENTITY_GHAST_SHOOT, 0.7f, 0.5f);
                p.setVelocity(new Vector(0, 2.2, 0));
            }

            // ----------------------------------------------------------
            // 10. WIND EYE  –  Wind Push (push enemies with wind blast)
            // ----------------------------------------------------------
            case WIND -> {
                world.spawnParticle(Particle.WHITE_ASH, loc, 80, 3, 1, 3, 0.15);
                world.spawnParticle(Particle.CLOUD, loc, 40, 2, 0.5, 2, 0.08);
                world.playSound(loc, Sound.ENTITY_PHANTOM_DEATH, 0.8f, 0.5f);
                pushNearby(p, 10, 2.5, false, 2.0);
            }

            // ----------------------------------------------------------
            // 11. POISON EYE  –  Poison Cloud (area poison around you)
            // ----------------------------------------------------------
            case POISON -> {
                world.spawnParticle(Particle.ITEM_SLIME, loc, 80, 2, 0.5, 2, 0.08);
                world.spawnParticle(Particle.SNEEZE, loc, 50, 2, 0.5, 2, 0.05);
                world.spawnParticle(Particle.DUST, loc, 40, 2, 0.5, 2, 0,
                    new Particle.DustOptions(Color.fromRGB(50, 200, 30), 1.5f));
                world.playSound(loc, Sound.ENTITY_SLIME_SQUISH, 1f, 0.4f);
                p.getNearbyEntities(5, 5, 5).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p)
                        le.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 1));
                });
            }

            // ----------------------------------------------------------
            // 12. LIGHT EYE  –  Light Beam (aim: fire a damaging beam)
            // ----------------------------------------------------------
            case LIGHT -> {
                Vector dir = p.getEyeLocation().getDirection().normalize();
                Location beamLoc = p.getEyeLocation().clone();
                boolean hit = false;
                for (int i = 0; i < 30 && !hit; i++) {
                    beamLoc.add(dir);
                    world.spawnParticle(Particle.END_ROD, beamLoc, 4, 0.1, 0.1, 0.1, 0.01);
                    if (beamLoc.getBlock().getType().isSolid()) break;
                    for (Entity e : world.getNearbyEntities(beamLoc, 1, 1, 1)) {
                        if (e instanceof LivingEntity le && e != p) {
                            le.damage(8.0 * dmgMod, p);
                            world.spawnParticle(Particle.FLASH, le.getLocation(), 1, 0, 0, 0, 0);
                            world.playSound(beamLoc, Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 2.0f);
                            hit = true;
                            break;
                        }
                    }
                }
                world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.6f, 2.0f);
            }

            // ----------------------------------------------------------
            // 13. EARTH EYE  –  Earth Slam (smash ground, knockback)
            // ----------------------------------------------------------
            case EARTH -> {
                world.spawnParticle(Particle.DUST_PLUME, loc, 100, 2, 0.1, 2, 0.2,
                    Material.DIRT.createBlockData());
                world.spawnParticle(Particle.DUST_PLUME, loc, 60, 2, 0.1, 2, 0.2,
                    Material.STONE.createBlockData());
                world.spawnParticle(Particle.EXPLOSION, loc, 2, 0.5, 0, 0.5, 0);
                world.playSound(loc, Sound.BLOCK_STONE_BREAK, 1f, 0.4f);
                world.playSound(loc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 0.5f);
                pushNearby(p, 8, 1.8, true, 4.0);
            }

            // ----------------------------------------------------------
            // 14. CRYSTAL EYE  –  Crystal Spikes (sharp crystals rise, dmg)
            // ----------------------------------------------------------
            case CRYSTAL -> {
                world.spawnParticle(Particle.DUST, loc, 80, 2, 0.3, 2, 0,
                    new Particle.DustOptions(Color.fromRGB(120, 220, 255), 2.5f));
                world.spawnParticle(Particle.END_ROD, loc, 40, 1.5, 0.2, 1.5, 0.05);
                world.playSound(loc, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1f, 0.8f);
                world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 0.5f);
                damageNearby(p, 6, 5.0 * dmgMod);
            }

            // ----------------------------------------------------------
            // 16. ECHO EYE  –  Echo Blast (sound shockwave, damages enemies)
            // ----------------------------------------------------------
            case ECHO -> {
                world.spawnParticle(Particle.SONIC_BOOM, loc, 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.END_ROD, loc, 60, 3, 3, 3, 0.06);
                world.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1f, 0.5f);
                world.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 0.8f, 0.3f);
                damageNearby(p, 10, 4.0);
            }

            // ----------------------------------------------------------
            // 17. RAGE EYE  –  Rage Smash (powerful knockback attack)
            // ----------------------------------------------------------
            case RAGE -> {
                world.spawnParticle(Particle.EXPLOSION, loc, 2, 0.3, 0, 0.3, 0);
                world.spawnParticle(Particle.DUST, loc, 60, 1, 0.5, 1, 0,
                    new Particle.DustOptions(Color.RED, 2.0f));
                world.spawnParticle(Particle.CRIT, loc, 30, 0.6, 0.3, 0.6, 0.1);
                world.playSound(loc, Sound.ENTITY_RAVAGER_ROAR, 1f, 0.8f);
                pushNearby(p, 6, 2.0, false, 5.0);
            }

            // ----------------------------------------------------------
            // 18. SPIRIT EYE  –  Spirit Form (invisible for a few seconds)
            // ----------------------------------------------------------
            case SPIRIT -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 120, 0));
                world.spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 50, 0.4, 0.8, 0.4, 0.07);
                world.spawnParticle(Particle.END_ROD, loc, 20, 0.3, 0.6, 0.3, 0.04);
                world.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.5f);
            }

            // ----------------------------------------------------------
            // 19. TIME EYE  –  Time Dash (instant high-speed dash)
            // ----------------------------------------------------------
            case TIME -> {
                Vector dir = p.getEyeLocation().getDirection().normalize().multiply(3.2);
                world.spawnParticle(Particle.DUST, loc, 40, 0.3, 0.5, 0.3, 0,
                    new Particle.DustOptions(Color.fromRGB(180, 130, 255), 1.5f));
                world.spawnParticle(Particle.REVERSE_PORTAL, loc, 25, 0.2, 0.4, 0.2, 0.06);
                p.setVelocity(dir);
                Bukkit.getScheduler().runTaskLater(plugin, () ->
                    world.spawnParticle(Particle.DUST, p.getLocation(), 30, 0.3, 0.5, 0.3, 0,
                        new Particle.DustOptions(Color.fromRGB(180, 130, 255), 1.5f)), 5L);
                world.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.8f);
            }

            // =================== EVENT EYES ==========================

            // ----------------------------------------------------------
            // E1. METEOR EYE  –  Meteor Shower (meteors fall around player)
            // ----------------------------------------------------------
            case METEOR -> {
                world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 0.5f);
                for (int i = 0; i < 6; i++) {
                    final int delay  = i * 7;
                    final double rx  = (Math.random() - 0.5) * 12;
                    final double rz  = (Math.random() - 0.5) * 12;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        Location impact = loc.clone().add(rx, 0, rz);
                        // Trail from above
                        Location above = impact.clone().add(0, 8, 0);
                        world.spawnParticle(Particle.FLAME, above, 20, 0, -1, 0, 0.12);
                        world.spawnParticle(Particle.LAVA, above, 5, 0, -0.5, 0, 0.08);

                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            world.spawnParticle(Particle.EXPLOSION, impact, 1, 0, 0, 0, 0);
                            world.spawnParticle(Particle.FLAME, impact, 30, 1, 0.2, 1, 0.1);
                            world.spawnParticle(Particle.LAVA, impact, 8, 0.8, 0.1, 0.8, 0.08);
                            world.playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.0f);
                            // Damage anything at impact
                            impact.getWorld().getNearbyEntities(impact, 3, 3, 3).forEach(e -> {
                                if (e instanceof LivingEntity le && e != p)
                                    le.damage(4.0 * dmgMod, p);
                            });
                        }, 8L);
                    }, delay);
                }
            }

            // ----------------------------------------------------------
            // E2. MIRAGE EYE  –  Phantom Escape (invis + strong speed)
            // ----------------------------------------------------------
            case MIRAGE -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 160, 3));
                p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 160, 0));
                world.spawnParticle(Particle.CLOUD, loc, 50, 0.5, 1, 0.5, 0.06);
                world.spawnParticle(Particle.SMOKE, loc, 30, 0.4, 0.8, 0.4, 0.04);
                world.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.3f);
            }

            // ----------------------------------------------------------
            // E3. OCEAN EYE  –  Ocean Prison (water bubble, slows enemies)
            // ----------------------------------------------------------
            case OCEAN -> {
                world.spawnParticle(Particle.SPLASH, loc, 100, 2, 1, 2, 0.2);
                world.spawnParticle(Particle.BUBBLE_POP, loc, 60, 2, 1, 2, 0.1);
                world.spawnParticle(Particle.BUBBLE_COLUMN_UP, loc, 40, 1.5, 0.5, 1.5, 0.05);
                world.playSound(loc, Sound.ENTITY_GUARDIAN_ATTACK, 1f, 0.4f);
                p.getNearbyEntities(8, 8, 8).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p)
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 4));
                });
            }

            // ----------------------------------------------------------
            // E4. ECLIPSE EYE  –  Shadow Phase (invis + damage reduction)
            // ----------------------------------------------------------
            case ECLIPSE -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 160, 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 160, 1));
                world.spawnParticle(Particle.PORTAL, loc, 60, 0.5, 1, 0.5, 0.1);
                world.spawnParticle(Particle.DUST, loc, 40, 0.4, 0.8, 0.4, 0,
                    new Particle.DustOptions(Color.BLACK, 2.0f));
                world.playSound(loc, Sound.ENTITY_ENDERMAN_STARE, 0.7f, 0.8f);
            }

            // ----------------------------------------------------------
            // E5. GUARDIAN EYE  –  Titan Shockwave (massive knockback)
            // ----------------------------------------------------------
            case GUARDIAN -> {
                world.spawnParticle(Particle.EXPLOSION, loc, 4, 1, 0, 1, 0);
                world.spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 100, 3, 1, 3, 0.1);
                world.spawnParticle(Particle.DUST_PLUME, loc, 80, 3, 0.1, 3, 0.2,
                    Material.STONE.createBlockData());
                world.playSound(loc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 0.3f);
                world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.9f, 0.5f);
                pushNearby(p, 15, 3.0, true, 6.0);
            }
        }
        p.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 0.5f);
    }
    
    // =====================================================================
    //  CORE MECHANICS & HELPERS  (unchanged structure, no logic changes)
    // =====================================================================

    private double getDamageMultiplier(Player p) {
        int lvl = plugin.getPlayerData().getLevel(p);
        return (lvl == 3) ? 1.5 : (lvl == 2) ? 1.2 : 1.0;
    }

    /** Safe teleport: steps up 1 block if destination is solid. */
    private Location getSafeLocation(Location loc) {
        Location safe = loc.clone();
        if (safe.getBlock().getType().isSolid()) safe.setY(safe.getY() + 1);
        if (safe.getBlock().getType().isSolid()) safe.setY(safe.getY() + 1);
        return safe;
    }

    private void damageNearby(Player p, double r, double d) {
        p.getNearbyEntities(r, r, r).forEach(e -> {
            if (e instanceof LivingEntity le && e != p) le.damage(d, p);
        });
    }

    private void pushNearby(Player p, double r, double f, boolean up, double d) {
        p.getNearbyEntities(r, r, r).forEach(e -> {
            if (e instanceof LivingEntity le && e != p) {
                Vector v = e.getLocation().toVector()
                    .subtract(p.getLocation().toVector()).normalize().multiply(f);
                if (up) v.setY(0.5);
                e.setVelocity(v);
                if (d > 0) le.damage(d, p);
            }
        });
    }

    private void pullNearby(Player p, double r, double f, double d) {
        p.getNearbyEntities(r, r, r).forEach(e -> {
            if (e instanceof LivingEntity le && e != p) {
                e.setVelocity(p.getLocation().toVector()
                    .subtract(e.getLocation().toVector()).normalize().multiply(f));
                if (d > 0) le.damage(d, p);
            }
        });
    }

    private LivingEntity getTarget(Player p, double range) {
        RayTraceResult res = p.getWorld().rayTraceEntities(
            p.getEyeLocation(), p.getEyeLocation().getDirection(), range, 1.5, (e) -> e != p);
        return (res != null && res.getHitEntity() instanceof LivingEntity le) ? le : null;
    }

    private void freezeNearby(Player p, double r) {
        p.getNearbyEntities(r, r, r).forEach(e -> {
            if (e instanceof LivingEntity le)
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 10));
        });
    }

    private void revealNearby(Player p, double r) {
        p.getNearbyEntities(r, r, r).forEach(e -> {
            if (e instanceof LivingEntity le) {
                le.setGlowing(true);
                Bukkit.getScheduler().runTaskLater(plugin, () -> le.setGlowing(false), 100L);
            }
        });
    }

    private void spawnCrystalAura(Player p) {
        World world = p.getWorld();
        Location loc = p.getLocation();
        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
            double rx = Math.cos(angle) * 1.5;
            double rz = Math.sin(angle) * 1.5;
            world.spawnParticle(Particle.DUST, loc.clone().add(rx, 1, rz), 4, 0, 0.3, 0, 0,
                new Particle.DustOptions(Color.fromRGB(120, 220, 255), 2.0f));
            world.spawnParticle(Particle.END_ROD, loc.clone().add(rx, 1, rz), 2, 0, 0.2, 0, 0.01);
        }
        world.spawnParticle(Particle.END_ROD, loc, 30, 0.4, 0.8, 0.4, 0.03);
    }

    private void spawnWall(Player p) {
        Block b = p.getTargetBlockExact(4);
        if (b != null) {
            for (int y = 1; y <= 3; y++)
                b.getLocation().add(0, y, 0).getBlock().setType(Material.COBBLESTONE);
        }
    }

    private void spawnAura(Player p, Particle part, int count) {
        p.getWorld().spawnParticle(part, p.getLocation(), count, 0.5, 1, 0.5, 0.05);
    }

    private void stunNearby(Player p, double r, int t) {
        p.getNearbyEntities(r, r, r).forEach(e -> {
            if (e instanceof LivingEntity le && e != p)
                le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, t, 0));
        });
    }

    private boolean checkCooldown(Player p, String type) {
        String key = p.getUniqueId().toString() + type;
        if (cooldowns.containsKey(key) && cooldowns.get(key) > System.currentTimeMillis()) {
            p.sendMessage("§c§lWAIT! §7Cooldown: "
                + (cooldowns.get(key) - System.currentTimeMillis()) / 1000 + "s");
            return true;
        }
        return false;
    }

    private void setCooldown(Player p, String type, int sec) {
        int lvl      = plugin.getPlayerData().getLevel(p);
        int finalSec = Math.max(2, sec - (lvl * 2));
        cooldowns.put(p.getUniqueId().toString() + type,
            System.currentTimeMillis() + (finalSec * 1000L));
    }

    private void safeDamage(Player owner, Entity target, double damage) {
    // Agar target koi zinda cheez hai aur wo khud player (owner) nahi hai
    if (target instanceof LivingEntity le && target != owner) {
        le.damage(damage, owner); // Sirf tabhi damage hoga
    }
 }

        public void applyPassiveEffects(Player p, EyeType eye) {
    // Pehle saare purane effects hata do taaki naya Eye lene par purana effect chala jaye
    for (PotionEffect effect : p.getActivePotionEffects()) {
        p.removePotionEffect(effect.getType());
    }

    // Agar Eye NONE hai, toh koi effect mat do
    if (eye == EyeType.NONE || eye == null) return;

    // Har Eye ke liye unique Permanent Effect (-1 = Infinite)
    switch (eye) {
        // --- Normal Eyes ---
        case VOID -> p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, -1, 0, false, false));
        case PHANTOM -> p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, -1, 0, false, false));
        case STORM -> p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, -1, 1, false, false));
        case FROST -> p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, -1, 0, false, false)); // Cold immunity
        case FLAME -> p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, -1, 0, false, false));
        case SHADOW -> p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, -1, 0, false, false));
        case TITAN -> p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, -1, 1, false, false));
        case HUNTER -> p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, -1, 1, false, false));
        case GRAVITY -> p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, -1, 0, false, false));
        case WIND -> p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, -1, 1, false, false));
        case POISON -> p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, -1, 0, false, false)); // Toxic blood regen
        case LIGHT -> p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, -1, 0, false, false)); 
        case EARTH -> p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, -1, 0, false, false));
        case CRYSTAL -> p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, -1, 0, false, false));
        case ECHO -> p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, -1, 0, false, false));
        case RAGE -> p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, -1, 0, false, false));
        case SPIRIT -> p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, -1, 0, false, false));
        case TIME -> p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, -1, 1, false, false));
        case WARRIOR -> p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, -1, 1, false, false));

        // --- Event Eyes (Super Powerful) ---
        case METEOR -> {
            p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, -1, 0, false, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, -1, 1, false, false));
        }
        case MIRAGE -> p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, -1, 2, false, false));
        case OCEAN -> {
            p.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, -1, 0, false, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER, -1, 0, false, false));
        }
        case ECLIPSE -> {
            p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, -1, 0, false, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, -1, 1, false, false));
        }
        case GUARDIAN -> {
            p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, -1, 2, false, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, -1, 1, false, false));
        }
    }
    }
        

    // =====================================================================
    //  GUI SYSTEM  (/eye command)  — unchanged
    // =====================================================================
    public void openEyeGUI(Player p) {
        Inventory gui = Bukkit.createInventory(null, 27, "§8Your Ancient Eye Status");

        EyeType type       = plugin.getPlayerData().getEye(p);
        int     level      = plugin.getPlayerData().getLevel(p);
        int     xp         = plugin.getPlayerData().getXP(p);
        int     nextLevelXP = 100;

        String progressBar = generateProgressBar(xp, nextLevelXP);
        String color       = (level == 3) ? "§e§l" : "§b";

        ItemStack eyeItem = new ItemStack(Material.ENDER_EYE);
        ItemMeta  meta    = eyeItem.getItemMeta();
        meta.setDisplayName(color + type.name().replace("_", " ") + " EYE");

        List<String> lore = new ArrayList<>();
        lore.add("§8Ancient Artifact");
        lore.add("");
        lore.add("§7Level: §f" + level + "/3");
        lore.add("§7Progress: " + progressBar + " §8(" + xp + "/" + nextLevelXP + ")");
        lore.add("");
        lore.add("§6§lCurrent Buffs:");
        lore.add("§e• §7Damage: §c+" + (int) ((getDamageMultiplier(p) - 1) * 100) + "%");
        lore.add("§e• §7Cooldown: §a-" + (level * 2) + "s");
        lore.add("");
        lore.add("§b§lAbilities:");
        lore.add("§f• Shift+F: §bPrimary");
        lore.add("§f• Shift+Q: §bSecondary");
        lore.add("");
        lore.add("§e§oThe power is bound to your soul.");

        meta.setLore(lore);
        eyeItem.setItemMeta(meta);

        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta  gMeta = glass.getItemMeta();
        gMeta.setDisplayName(" ");
        glass.setItemMeta(gMeta);

        for (int i = 0; i < 27; i++)
            gui.setItem(i, i == 13 ? eyeItem : glass);

        p.openInventory(gui);
        p.playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1.2f);
    }

    private String generateProgressBar(int current, int max) {
        int bars      = 10;
        int completed = (int) ((double) current / max * bars);
        StringBuilder sb = new StringBuilder("§a");
        for (int i = 0; i < bars; i++) {
            if (i == completed) sb.append("§7");
            sb.append("┃");
        }
        return sb.toString();
    }
                            }
