package com.ancienteye;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import java.util.*;

public class AbilityLogic implements Listener {

    private final AncientEyePlugin plugin;
    private final Map<String, Long> cooldowns = new HashMap<>();
    private static final int INF = -1;

    public AbilityLogic(AncientEyePlugin plugin) { this.plugin = plugin; }

    // ══════════════════════════════════════════════════════════════════════
    //  GUI LOCK — no item stealing
    // ══════════════════════════════════════════════════════════════════════
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals("§8Your Ancient Eye Status")) e.setCancelled(true);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PASSIVE COMBAT EVENTS
    // ══════════════════════════════════════════════════════════════════════
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        // VOID passive: 15% dodge
        if (e.getEntity() instanceof Player victim) {
            EyeType ve = plugin.getPlayerData().getEye(victim);
            if (ve == EyeType.VOID && Math.random() < 0.15) {
                e.setCancelled(true);
                victim.sendMessage("§5§l✦ Void Dodge!");
                victim.getWorld().spawnParticle(Particle.PORTAL, victim.getLocation(), 30, 0.5, 1, 0.5, 0.1);
                return;
            }
            // MIRAGE passive: 15% miss
            if (ve == EyeType.MIRAGE && Math.random() < 0.15) {
                e.setCancelled(true);
                victim.sendMessage("§e§l✦ Mirage! Attack missed!");
                victim.getWorld().spawnParticle(Particle.CLOUD, victim.getLocation(), 20, 0.4, 0.8, 0.4, 0.04);
                return;
            }
            // GUARDIAN passive: 35% damage reduction
            if (ve == EyeType.GUARDIAN) e.setDamage(e.getDamage() * 0.65);
        }
        // RAGE passive: bonus damage when low HP
        if (e.getDamager() instanceof Player atk) {
            EyeType ae = plugin.getPlayerData().getEye(atk);
            if (ae == EyeType.RAGE) {
                double hp  = atk.getHealth();
                double max = atk.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
                if (hp / max < 0.35) {
                    e.setDamage(e.getDamage() * 1.75);
                    atk.getWorld().spawnParticle(Particle.DUST, atk.getLocation(), 10, 0.3, 0.6, 0.3, 0,
                        new Particle.DustOptions(Color.RED, 1.5f));
                }
            }
            // ECLIPSE passive: +30% damage at night
            if (ae == EyeType.ECLIPSE) {
                long t = atk.getWorld().getTime();
                if (t > 13000 && t < 23000) e.setDamage(e.getDamage() * 1.30);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PRIMARY  (SHIFT + F)
    // ══════════════════════════════════════════════════════════════════════
    public void activatePrimary(Player p, EyeType eye) {
        if (checkCD(p, "P")) return;
        setCD(p, "P", 12);
        Location loc = p.getLocation().clone();
        Vector   dir = p.getEyeLocation().getDirection().normalize();
        double   dm  = getDmg(p);
        World    w   = p.getWorld();

        switch (eye) {

            // 1. VOID — Void Blink (8 blocks forward + rift visuals)
            case VOID -> {
                voidRift(w, loc);
                w.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.6f);
                w.playSound(loc, Sound.BLOCK_PORTAL_AMBIENT, 0.5f, 1.5f);
                for (int i = 1; i <= 8; i++)
                    w.spawnParticle(Particle.PORTAL, loc.clone().add(dir.clone().multiply(i)), 5, 0.15, 0.35, 0.15, 0.08);
                Location dest = safe(loc.clone().add(dir.clone().multiply(8)));
                voidRift(w, dest);
                w.strikeLightningEffect(dest);
                p.teleport(dest);
            }

            // 2. PHANTOM — Ghost Dash (fast dash + ghost trail)
            case PHANTOM -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 22, 0));
                p.setVelocity(dir.clone().multiply(2.8).setY(0.35));
                w.playSound(loc, Sound.ENTITY_PHANTOM_FLAP, 1f, 1.8f);
                new BukkitRunnable() { int t = 0;
                    public void run() {
                        if (t++ >= 8) { cancel(); return; }
                        w.spawnParticle(Particle.CLOUD,          p.getLocation(), 10, 0.3, 0.6, 0.3, 0.02);
                        w.spawnParticle(Particle.SMOKE,          p.getLocation(),  6, 0.2, 0.5, 0.2, 0.01);
                        w.spawnParticle(Particle.REVERSE_PORTAL, p.getLocation(),  4, 0.2, 0.4, 0.2, 0.03);
                    }
                }.runTaskTimer(plugin, 0, 1);
            }

            // 3. STORM — Lightning Dash (dash + lightning strike on land)
            case STORM -> {
                w.spawnParticle(Particle.ELECTRIC_SPARK, loc, 80, 0.8, 1, 0.8, 0.12);
                p.setVelocity(dir.clone().multiply(2.5).setY(0.3));
                w.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.3f);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Location land = p.getLocation();
                    w.strikeLightning(land);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, land, 100, 0.8, 1.5, 0.8, 0.18);
                    for (int i = 0; i < 16; i++) { double a = Math.toRadians(i * 22.5);
                        w.spawnParticle(Particle.ELECTRIC_SPARK, land.clone().add(Math.cos(a)*2.5,0.2,Math.sin(a)*2.5), 4, 0, 0.5, 0, 0.08);
                    }
                    land.getWorld().getNearbyEntities(land, 3, 3, 3).forEach(e -> {
                        if (e instanceof LivingEntity le && e != p) le.damage(5.0 * dm, p);
                    });
                }, 8L);
            }

            // 4. FROST — Ice Slide (horizontal slide + frost trail)
            case FROST -> {
                w.spawnParticle(Particle.SNOWFLAKE, loc, 60, 0.6, 0.4, 0.6, 0.08);
                w.spawnParticle(Particle.WHITE_ASH, loc, 30, 0.4, 0.2, 0.4, 0.05);
                p.setVelocity(dir.clone().setY(0).normalize().multiply(2.8));
                w.playSound(loc, Sound.ENTITY_PLAYER_HURT_FREEZE, 1f, 1.5f);
                w.playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.8f);
                new BukkitRunnable() { int t = 0;
                    public void run() {
                        if (t++ >= 10) { cancel(); return; }
                        w.spawnParticle(Particle.SNOWFLAKE, p.getLocation(), 8, 0.4, 0.1, 0.4, 0.02);
                        w.spawnParticle(Particle.WHITE_ASH, p.getLocation(), 5, 0.3, 0.1, 0.3, 0.01);
                    }
                }.runTaskTimer(plugin, 0, 1);
            }

            // 5. FLAME — Fire Dash (dash + ignite trail + damage)
            case FLAME -> {
                w.spawnParticle(Particle.FLAME, loc, 40, 0.5, 0.8, 0.5, 0.1);
                w.spawnParticle(Particle.LAVA,  loc, 12, 0.3, 0.2, 0.3, 0.07);
                p.setVelocity(dir.clone().multiply(2.3).setY(0.3));
                w.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1f, 1f);
                new BukkitRunnable() { int t = 0;
                    public void run() {
                        if (t++ >= 8) { cancel(); return; }
                        Location cur = p.getLocation();
                        w.spawnParticle(Particle.FLAME,          cur, 15, 0.4, 0.5, 0.4, 0.07);
                        w.spawnParticle(Particle.LAVA,           cur,  4, 0.2, 0.1, 0.2, 0.05);
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME,cur,  3, 0.3, 0.4, 0.3, 0.03);
                        p.getNearbyEntities(1.8, 1.8, 1.8).forEach(e -> {
                            if (e instanceof LivingEntity le && e != p) { le.setFireTicks(80); le.damage(1.5 * dm, p); }
                        });
                    }
                }.runTaskTimer(plugin, 0, 1);
            }

            // 6. SHADOW — Shadow Step (teleport behind nearest player + backstab)
            case SHADOW -> {
                LivingEntity tgt = aim(p, 20);
                if (tgt != null) {
                    w.spawnParticle(Particle.SMOKE, loc, 50, 0.4, 0.8, 0.4, 0.07);
                    w.spawnParticle(Particle.DUST,  loc, 25, 0.3, 0.6, 0.3, 0, new Particle.DustOptions(Color.BLACK, 2f));
                    Vector behind = tgt.getLocation().getDirection().normalize().multiply(2.0);
                    Location dest = safe(tgt.getLocation().clone().subtract(behind));
                    dest.setYaw(tgt.getLocation().getYaw() + 180);
                    w.spawnParticle(Particle.SMOKE, dest, 50, 0.4, 0.8, 0.4, 0.07);
                    w.spawnParticle(Particle.PORTAL,dest, 30, 0.3, 0.7, 0.3, 0.08);
                    w.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.7f);
                    p.teleport(dest);
                    tgt.damage(7.0 * dm, p);
                    w.spawnParticle(Particle.CRIT, tgt.getLocation(), 30, 0.4, 0.8, 0.4, 0.1);
                } else p.sendMessage("§7No target in range.");
            }

            // 7. TITAN — Ground Slam (knockup + heavy damage + 4-wave shockwave)
            case TITAN -> {
                w.spawnParticle(Particle.EXPLOSION,  loc, 5, 1.0, 0, 1.0, 0);
                w.spawnParticle(Particle.DUST_PLUME, loc, 120, 2.5, 0.2, 2.5, 0.2, Material.STONE.createBlockData());
                w.spawnParticle(Particle.DUST,       loc, 70, 2, 0.3, 2, 0, new Particle.DustOptions(Color.fromRGB(100,60,20), 3f));
                for (int ring = 1; ring <= 4; ring++) { final int fr = ring;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        for (int i = 0; i < 24; i++) { double a = Math.toRadians(i * 15); double r = fr * 1.8;
                            w.spawnParticle(Particle.DUST_PLUME, loc.clone().add(Math.cos(a)*r,0.1,Math.sin(a)*r),
                                4, 0, 0.6, 0, 0.12, Material.STONE.createBlockData());
                        }
                    }, ring * 3L);
                }
                w.playSound(loc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 0.4f);
                w.playSound(loc, Sound.BLOCK_STONE_BREAK,        1f, 0.3f);
                w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.5f);
                p.getNearbyEntities(7, 7, 7).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        e.setVelocity(e.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(2.2).setY(1.2));
                        le.damage(6.0 * dm, p);
                    }
                });
            }

            // 8. HUNTER — Hunter Dash (fast dash toward aimed target, damage on arrival)
            case HUNTER -> {
                LivingEntity tgt = aim(p, 30);
                if (tgt != null) {
                    w.spawnParticle(Particle.CRIT, loc, 30, 0.3, 0.5, 0.3, 0.12);
                    p.setVelocity(tgt.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(3.0).setY(0.3));
                    w.playSound(loc, Sound.ENTITY_ARROW_SHOOT, 1f, 1.8f);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (!tgt.isValid()) return;
                        if (tgt.getLocation().distanceSquared(p.getLocation()) < 20) {
                            tgt.damage(7.0 * dm, p);
                            w.spawnParticle(Particle.CRIT,    tgt.getLocation(), 40, 0.5, 0.8, 0.5, 0.12);
                            w.spawnParticle(Particle.EXPLOSION,tgt.getLocation(),  2, 0.3, 0, 0.3, 0);
                            w.playSound(tgt.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1f, 1.2f);
                        }
                    }, 10L);
                } else p.sendMessage("§7No target in range.");
            }

            // 9. GRAVITY — Gravity Pull (rising vortex rings + pull+lift enemies)
            case GRAVITY -> {
                w.spawnParticle(Particle.REVERSE_PORTAL, loc, 100, 2.5, 2, 2.5, 0.14);
                w.spawnParticle(Particle.PORTAL,         loc,  60, 2.0, 1.5, 2.0, 0.10);
                for (int ring = 0; ring < 6; ring++) { final int fr = ring;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        double r = 3.5 - fr * 0.4;
                        for (int i = 0; i < 20; i++) { double a = Math.toRadians(i*18)+fr*0.5;
                            w.spawnParticle(Particle.REVERSE_PORTAL, loc.clone().add(Math.cos(a)*r, fr*0.4, Math.sin(a)*r), 2, 0, 0, 0, 0.03);
                        }
                    }, ring * 2L);
                }
                w.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE,   1f, 0.3f);
                w.playSound(loc, Sound.ENTITY_ENDERMAN_SCREAM, 0.5f, 0.5f);
                p.getNearbyEntities(8, 8, 8).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        e.setVelocity(loc.toVector().subtract(e.getLocation().toVector()).normalize().multiply(2.2));
                        le.damage(3.0 * dm, p);
                    }
                });
            }

            // 10. WIND — Wind Dash (very fast dash + tornado pushes along path)
            case WIND -> {
                w.spawnParticle(Particle.WHITE_ASH, loc, 80, 0.8, 0.8, 0.8, 0.18);
                w.spawnParticle(Particle.CLOUD,     loc, 40, 0.5, 0.5, 0.5, 0.08);
                p.setVelocity(dir.clone().multiply(3.5).setY(0.2));
                w.playSound(loc, Sound.ENTITY_PHANTOM_DEATH, 0.8f, 2.0f);
                new BukkitRunnable() { int t = 0;
                    public void run() {
                        if (t++ >= 10) { cancel(); return; }
                        Location cur = p.getLocation();
                        for (int i = 0; i < 6; i++) { double a = Math.toRadians(i*60+t*30);
                            w.spawnParticle(Particle.WHITE_ASH, cur.clone().add(Math.cos(a)*1.2,0.5,Math.sin(a)*1.2), 2, 0, 0, 0, 0.05);
                        }
                        p.getNearbyEntities(2, 2, 2).forEach(e -> {
                            if (e instanceof LivingEntity le && e != p)
                                e.setVelocity(e.getLocation().toVector().subtract(cur.toVector()).normalize().multiply(1.8).setY(0.4));
                        });
                    }
                }.runTaskTimer(plugin, 0, 1);
            }

            // 11. POISON — Poison Strike (aim-based: poison + slow + venom ring)
            case POISON -> {
                LivingEntity tgt = aim(p, 12);
                if (tgt != null) {
                    w.spawnParticle(Particle.ITEM_SLIME, tgt.getLocation(), 50, 0.5, 0.8, 0.5, 0.07);
                    w.spawnParticle(Particle.SNEEZE,     tgt.getLocation(), 25, 0.4, 0.7, 0.4, 0.04);
                    w.spawnParticle(Particle.DUST,       tgt.getLocation(), 30, 0.4, 0.7, 0.4, 0,
                        new Particle.DustOptions(Color.fromRGB(40,200,40), 1.8f));
                    for (int i = 0; i < 12; i++) { double a = Math.toRadians(i*30);
                        w.spawnParticle(Particle.ITEM_SLIME, tgt.getLocation().add(Math.cos(a)*1.5,0.5,Math.sin(a)*1.5), 3, 0, 0.2, 0, 0.02);
                    }
                    tgt.addPotionEffect(new PotionEffect(PotionEffectType.POISON,   180, 2));
                    tgt.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,  80, 1));
                    tgt.damage(4.0 * dm, p);
                    w.playSound(loc, Sound.ENTITY_SLIME_SQUISH, 1f, 0.4f);
                    w.playSound(loc, Sound.ENTITY_WITCH_DRINK,  0.8f, 0.6f);
                } else p.sendMessage("§7No target in range.");
            }

            // 12. LIGHT — Flash Burst (blind all nearby + 4 expanding light rings)
            case LIGHT -> {
                w.spawnParticle(Particle.FLASH,   loc, 3,   0,   0,   0,   0);
                w.spawnParticle(Particle.END_ROD, loc, 120, 3,   3,   3, 0.14);
                for (int ring = 1; ring <= 4; ring++) { final int fr = ring;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        double r = fr * 2.8;
                        for (int i = 0; i < 24; i++) { double a = Math.toRadians(i*15);
                            w.spawnParticle(Particle.END_ROD, loc.clone().add(Math.cos(a)*r,0.5,Math.sin(a)*r), 3, 0, 0.3, 0, 0.02);
                        }
                    }, ring * 3L);
                }
                w.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT,    1f, 2.0f);
                w.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.7f, 2.0f);
                p.getNearbyEntities(10, 10, 10).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 1));
                        le.damage(5.0 * dm, p);
                        w.spawnParticle(Particle.FLASH, le.getLocation(), 1, 0, 0, 0, 0);
                    }
                });
            }

            // 13. EARTH — Earth Wall (stone wall + debris explosion)
            case EARTH -> {
                w.spawnParticle(Particle.DUST_PLUME, loc, 100, 1.5, 0.4, 1.5, 0.2, Material.DIRT.createBlockData());
                w.spawnParticle(Particle.EXPLOSION,  loc,   2, 0.4, 0,   0.4, 0);
                w.playSound(loc, Sound.BLOCK_GRAVEL_BREAK,       1f, 0.3f);
                w.playSound(loc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 0.5f);
                buildWall(p);
            }

            // 14. CRYSTAL — Crystal Shield (Resistance V + crystal aura ring)
            case CRYSTAL -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 4));
                crystalAura(p);
                w.spawnParticle(Particle.END_ROD, loc, 60, 1.2, 1.8, 1.2, 0.06);
                w.spawnParticle(Particle.DUST,    loc, 40, 1.0, 1.5, 1.0, 0, new Particle.DustOptions(Color.fromRGB(100,220,255), 2f));
                w.playSound(loc, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1f, 1.5f);
                w.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME,   1f, 1.2f);
                p.sendTitle("§b§l💎 CRYSTAL SHIELD", "§7Resistance V Active!", 5, 45, 10);
            }

            // 15. ECHO — Echo Pulse (5 sonar rings + reveal + stun nearby)
            case ECHO -> {
                w.spawnParticle(Particle.SONIC_BOOM, loc, 1, 0, 0, 0, 0);
                w.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.8f, 1.6f);
                for (int ring = 1; ring <= 5; ring++) { final int fr = ring;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        double r = fr * 3.0;
                        for (int i = 0; i < 30; i++) { double a = Math.toRadians(i*12);
                            w.spawnParticle(Particle.END_ROD, loc.clone().add(Math.cos(a)*r,0.8,Math.sin(a)*r), 1, 0, 0, 0, 0);
                        }
                    }, ring * 5L);
                }
                p.getNearbyEntities(20, 20, 20).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        le.setGlowing(true);
                        le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 0));
                        w.spawnParticle(Particle.END_ROD, le.getLocation(), 15, 0.4, 0.8, 0.4, 0.04);
                        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (le.isValid()) le.setGlowing(false); }, 120L);
                    }
                });
                p.sendTitle("§3§lSONAR PULSE", "§7All enemies revealed!", 5, 50, 10);
            }

            // 16. RAGE — Rage Mode (Haste III + Strength II + Speed + rage aura ring)
            case RAGE -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE,    160, 3));
                p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 100, 1));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,     80, 1));
                w.spawnParticle(Particle.DUST,  loc, 100, 0.6, 1.2, 0.6, 0, new Particle.DustOptions(Color.RED, 2.5f));
                w.spawnParticle(Particle.CRIT,  loc,  60, 0.5, 1.0, 0.5, 0.14);
                w.spawnParticle(Particle.FLAME, loc,  40, 0.4, 0.8, 0.4, 0.09);
                for (int i = 0; i < 16; i++) { double a = Math.toRadians(i*22.5);
                    w.spawnParticle(Particle.DUST, loc.clone().add(Math.cos(a)*2,1,Math.sin(a)*2), 4, 0, 0.5, 0, 0,
                        new Particle.DustOptions(Color.fromRGB(220,20,20), 2f));
                }
                w.playSound(loc, Sound.ENTITY_RAVAGER_ROAR,  1f, 0.8f);
                w.playSound(loc, Sound.ENTITY_WITHER_HURT,  0.7f, 1.2f);
                p.sendTitle("§c§l⚡ RAGE MODE!", "§7+Haste III  +Strength  +Speed", 5, 50, 10);
            }

            // 17. SPIRIT — Spirit Heal (heal + Regen III + totem burst)
            case SPIRIT -> {
                double maxHp = p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
                p.setHealth(Math.min(p.getHealth() + 8.0 * dm, maxHp));
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 120, 2));
                w.spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 100, 0.5, 1.2, 0.5, 0.1);
                w.spawnParticle(Particle.HEART,    loc,  18, 0.6, 0.6, 0.6, 0.08);
                w.spawnParticle(Particle.END_ROD,  loc,  40, 0.4, 0.9, 0.4, 0.05);
                for (int i = 0; i < 12; i++) { double a = Math.toRadians(i*30);
                    w.spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(Math.cos(a)*1.8,0.5,Math.sin(a)*1.8), 5, 0, 0.5, 0, 0.04);
                }
                w.playSound(loc, Sound.ENTITY_WITCH_CELEBRATE, 1f, 1.3f);
                w.playSound(loc, Sound.BLOCK_BEACON_AMBIENT,   1f, 1.5f);
                p.sendTitle("§a§l✦ SPIRIT HEAL", "§7Health Restored!", 5, 45, 10);
            }

            // 18. TIME — Time Slow (Slowness VI + Wither + clock rings on all nearby)
            case TIME -> {
                w.spawnParticle(Particle.REVERSE_PORTAL, loc, 90, 2.5, 2, 2.5, 0.12);
                w.spawnParticle(Particle.DUST,           loc, 60, 2, 2, 2, 0, new Particle.DustOptions(Color.fromRGB(150,80,255), 2f));
                for (int ring = 1; ring <= 3; ring++) { final int fr = ring;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        double r = fr * 2.5;
                        for (int i = 0; i < 12; i++) { double a = Math.toRadians(i*30);
                            w.spawnParticle(Particle.DUST, loc.clone().add(Math.cos(a)*r,0.5,Math.sin(a)*r), 3, 0, 0, 0, 0,
                                new Particle.DustOptions(Color.fromRGB(150,80,255), 1.5f));
                        }
                    }, ring * 4L);
                }
                w.playSound(loc, Sound.BLOCK_BEACON_AMBIENT, 1f, 0.2f);
                p.getNearbyEntities(10, 10, 10).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 120, 5));
                        le.addPotionEffect(new PotionEffect(PotionEffectType.WITHER,    60, 1));
                        w.spawnParticle(Particle.REVERSE_PORTAL, le.getLocation(), 20, 0.3, 0.6, 0.3, 0.06);
                    }
                });
                p.sendTitle("§d§lTIME SLOW", "§7Enemies frozen in time!", 5, 50, 10);
            }

            // 19. WARRIOR — War Cry (Strength II + Resistance + war cry ring + fear nearby)
            case WARRIOR -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH,   160, 2));
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 120, 1));
                w.spawnParticle(Particle.DUST,  loc, 100, 0.7, 1.4, 0.7, 0, new Particle.DustOptions(Color.fromRGB(220,80,20), 2.5f));
                w.spawnParticle(Particle.CRIT,  loc,  50, 0.5, 1.0, 0.5, 0.12);
                w.spawnParticle(Particle.FLAME, loc,  30, 0.4, 0.8, 0.4, 0.07);
                for (int i = 0; i < 20; i++) { double a = Math.toRadians(i*18);
                    w.spawnParticle(Particle.DUST, loc.clone().add(Math.cos(a)*2.5,0.5,Math.sin(a)*2.5), 4, 0, 0.4, 0, 0,
                        new Particle.DustOptions(Color.fromRGB(220,80,20), 2f));
                }
                w.playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, 1f, 0.6f);
                w.playSound(loc, Sound.ENTITY_RAVAGER_ROAR,    0.9f, 0.8f);
                p.getNearbyEntities(8, 8, 8).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
                        le.damage(3.0 * dm, p);
                    }
                });
                p.sendTitle("§6§l⚔ WAR CRY!", "§7+Strength II  +Resistance", 5, 50, 10);
            }

            // ═══ EVENT EYES — PRIMARY ═══════════════════════════════════════

            // E1. METEOR — Meteor Crash
            case METEOR -> {
                double eDmg = ecfg("meteor","primary-damage",14.0)*dm;
                int    eRad = (int)ecfg("meteor","primary-radius",8);
                p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 80, 0));
                w.spawnParticle(Particle.FLAME, loc, 50, 0.5, 0.3, 0.5, 0.1);
                p.setVelocity(new Vector(0, 3.0, 0));
                w.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1f, 0.5f);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!p.isOnline()) return;
                    w.spawnParticle(Particle.FLAME, p.getLocation(), 80, 1, 2, 1, 0.18);
                    p.setVelocity(new Vector(0, -5.5, 0));
                    w.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.5f);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (!p.isOnline()) return;
                        Location imp = p.getLocation();
                        w.spawnParticle(Particle.EXPLOSION,  imp, 8, 1.5, 0, 1.5, 0);
                        w.spawnParticle(Particle.FLAME,      imp, 200, eRad*0.4, 0.4, eRad*0.4, 0.2);
                        w.spawnParticle(Particle.LAVA,       imp,  50, eRad*0.3, 0.2, eRad*0.3, 0.15);
                        w.spawnParticle(Particle.DUST_PLUME, imp, 120, eRad*0.3, 0.2, eRad*0.3, 0.25, Material.MAGMA_BLOCK.createBlockData());
                        w.strikeLightningEffect(imp);
                        for (int i = 0; i < 36; i++) { double a = Math.toRadians(i*10);
                            w.spawnParticle(Particle.FLAME, imp.clone().add(Math.cos(a)*eRad,0.2,Math.sin(a)*eRad), 5, 0, 0.4, 0, 0.06);
                        }
                        w.playSound(imp, Sound.ENTITY_GENERIC_EXPLODE,        1f, 0.4f);
                        w.playSound(imp, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 0.5f);
                        imp.getWorld().getNearbyEntities(imp, eRad, eRad, eRad).forEach(e -> {
                            if (e instanceof LivingEntity le && e != p) {
                                le.damage(eDmg, p); le.setFireTicks(100);
                                e.setVelocity(e.getLocation().toVector().subtract(imp.toVector()).normalize().multiply(3.2).setY(1.8));
                            }
                        });
                    }, 18L);
                }, 15L);
            }

            // E2. MIRAGE — Mirage Army (5 clones + blind + nausea)
            case MIRAGE -> {
                double eDmg = ecfg("mirage","primary-damage",4.0)*dm;
                w.playSound(loc, Sound.ENTITY_ENDERMAN_SCREAM,       0.8f, 1.5f);
                w.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f,   1.0f);
                for (int i = 0; i < 5; i++) { final int fi = i;
                    double a = i*(Math.PI*2/5);
                    Location cl = loc.clone().add(Math.cos(a)*3.2, 0, Math.sin(a)*3.2);
                    w.spawnParticle(Particle.CLOUD,         cl, 50, 0.3, 1.2, 0.3, 0.05);
                    w.spawnParticle(Particle.SMOKE,         cl, 30, 0.2, 0.9, 0.2, 0.03);
                    w.spawnParticle(Particle.END_ROD,       cl, 12, 0.1, 0.5, 0.1, 0.02);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        cl.getWorld().getNearbyEntities(cl, 4, 4, 4).forEach(e -> {
                            if (e instanceof LivingEntity le && e != p) {
                                le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 50, 0));
                                le.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA,    70, 0));
                                le.damage(eDmg, p);
                            }
                        });
                        w.spawnParticle(Particle.CLOUD, cl, 25, 0.3, 0.9, 0.3, 0.04);
                    }, (fi+1)*10L);
                }
            }

            // E3. OCEAN — Tidal Crash (water wave push + 4 wave rings)
            case OCEAN -> {
                double eDmg = ecfg("ocean","primary-damage",8.0)*dm;
                int    eRad = (int)ecfg("ocean","primary-radius",12);
                w.spawnParticle(Particle.SPLASH,     loc, 250, 3.5, 0.8, 3.5, 0.45);
                w.spawnParticle(Particle.BUBBLE_POP, loc, 120, 3.0, 0.8, 3.0, 0.15);
                w.spawnParticle(Particle.RAIN,       loc, 180, 5,   2,   5,   0.18);
                for (int wave = 1; wave <= 4; wave++) { final int fw = wave;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        double wr = fw*3.0;
                        for (int i = 0; i < 28; i++) { double a = Math.toRadians(i*(360.0/28));
                            w.spawnParticle(Particle.SPLASH, loc.clone().add(Math.cos(a)*wr,0.6,Math.sin(a)*wr), 6, 0, 0.4, 0, 0.12);
                        }
                    }, wave*5L);
                }
                w.playSound(loc, Sound.ENTITY_GUARDIAN_ATTACK, 1f, 0.3f);
                w.playSound(loc, Sound.BLOCK_WATER_AMBIENT,    1f, 0.2f);
                p.getNearbyEntities(eRad, eRad, eRad).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        e.setVelocity(e.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(4.0).setY(0.9));
                        le.damage(eDmg, p);
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 2));
                    }
                });
            }

            // E4. ECLIPSE — Eclipse Blast (dark void explosion + 3 dark rings)
            case ECLIPSE -> {
                double eDmg = ecfg("eclipse","primary-damage",13.0)*dm;
                int    eRad = (int)ecfg("eclipse","primary-radius",9);
                w.spawnParticle(Particle.PORTAL,         loc, 180, 3, 3, 3, 0.22);
                w.spawnParticle(Particle.REVERSE_PORTAL, loc, 120, 2.5, 2.5, 2.5, 0.16);
                w.spawnParticle(Particle.DRAGON_BREATH,  loc,  80, 2, 2, 2, 0.09);
                w.spawnParticle(Particle.DUST,           loc,  70, 3, 3, 3, 0, new Particle.DustOptions(Color.BLACK, 4f));
                w.spawnParticle(Particle.EXPLOSION,      loc,   6, 1.2, 0, 1.2, 0);
                for (int ring = 1; ring <= 3; ring++) { final int fr = ring;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        double r = fr*eRad/3.0;
                        for (int i = 0; i < 24; i++) { double a = Math.toRadians(i*15);
                            w.spawnParticle(Particle.DRAGON_BREATH, loc.clone().add(Math.cos(a)*r,0.5,Math.sin(a)*r), 4, 0, 0.2, 0, 0.03);
                        }
                    }, ring*3L);
                }
                w.playSound(loc, Sound.ENTITY_WITHER_SHOOT,         1f, 0.5f);
                w.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.8f, 0.4f);
                p.getNearbyEntities(eRad, eRad, eRad).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        le.damage(eDmg, p);
                        le.addPotionEffect(new PotionEffect(PotionEffectType.WITHER,    120, 2));
                        le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,  80, 1));
                        e.setVelocity(e.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(2.8).setY(1.2));
                        w.spawnParticle(Particle.DRAGON_BREATH, le.getLocation(), 25, 0.4, 0.7, 0.4, 0.04);
                    }
                });
            }

            // E5. GUARDIAN — Guardian Dome (hemisphere dome + Res V + ally buffs)
            case GUARDIAN -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 300, 5));
                p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, 3));
                for (double a = 0; a < Math.PI*2; a += Math.PI/18)
                    for (double b = 0; b <= Math.PI/2; b += Math.PI/12) {
                        double rx = Math.sin(b)*Math.cos(a)*5, ry = Math.cos(b)*5, rz = Math.sin(b)*Math.sin(a)*5;
                        w.spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(rx,ry+0.5,rz), 1, 0, 0, 0, 0.01);
                        if (b%(Math.PI/6)<0.01)
                            w.spawnParticle(Particle.END_ROD, loc.clone().add(rx,ry+0.5,rz), 1, 0, 0, 0, 0.005);
                    }
                w.spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 100, 2, 2.5, 2, 0.08);
                w.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE,    1f, 0.7f);
                w.playSound(loc, Sound.ENTITY_IRON_GOLEM_REPAIR, 1f, 1.0f);
                p.getNearbyEntities(8, 8, 8).forEach(e -> {
                    if (e instanceof Player ally && e != p) {
                        ally.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 3));
                        ally.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 160, 2));
                        w.spawnParticle(Particle.TOTEM_OF_UNDYING, ally.getLocation(), 30, 0.4, 1, 0.4, 0.05);
                        ally.sendTitle("§a§l🛡 DOME PROTECTED!", "§fGuardian's Blessing!", 5, 50, 10);
                    }
                });
                p.sendTitle("§6§l🛡 GUARDIAN DOME!", "§7+Resistance V  +Absorption III", 5, 60, 10);
            }
        }
        p.playSound(loc, Sound.ENTITY_ENDER_EYE_DEATH, 0.8f, 1f);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  SECONDARY  (SHIFT + Q)
    // ══════════════════════════════════════════════════════════════════════
    public void activateSecondary(Player p, EyeType eye) {
        if (checkCD(p, "S")) return;
        setCD(p, "S", 20);
        Location loc = p.getLocation().clone();
        Vector   dir = p.getEyeLocation().getDirection().normalize();
        double   dm  = getDmg(p);
        World    w   = p.getWorld();

        switch (eye) {

            // 1. VOID — Void Trap (3 pull waves)
            case VOID -> {
                w.spawnParticle(Particle.PORTAL,         loc, 120, 2.5, 2.5, 2.5, 0.18);
                w.spawnParticle(Particle.REVERSE_PORTAL, loc,  80, 2.0, 2.0, 2.0, 0.12);
                w.spawnParticle(Particle.DRAGON_BREATH,  loc,  50, 1.5, 1.5, 1.5, 0.06);
                w.playSound(loc, Sound.BLOCK_BEACON_AMBIENT,   1f, 0.3f);
                w.playSound(loc, Sound.ENTITY_ENDERMAN_SCREAM, 0.6f, 0.4f);
                for (int wave = 0; wave < 3; wave++) {
                    Bukkit.getScheduler().runTaskLater(plugin, () ->
                        p.getNearbyEntities(12, 12, 12).forEach(e -> {
                            if (e instanceof LivingEntity le && e != p) {
                                e.setVelocity(loc.toVector().subtract(e.getLocation().toVector()).normalize().multiply(2.2));
                                le.damage(2.5 * dm, p);
                            }
                        }), wave * 10L);
                }
            }

            // 2. PHANTOM — Phantom Reveal (strip invis + glow all 30r)
            case PHANTOM -> {
                w.spawnParticle(Particle.END_ROD,    loc, 100, 4, 4, 4, 0.09);
                w.spawnParticle(Particle.SONIC_BOOM, loc,   1, 0, 0, 0, 0);
                w.playSound(loc, Sound.ENTITY_PHANTOM_BITE, 0.8f, 2.0f);
                p.getNearbyEntities(30, 30, 30).forEach(e -> {
                    if (e instanceof Player t && e != p) {
                        t.removePotionEffect(PotionEffectType.INVISIBILITY);
                        t.setGlowing(true);
                        w.spawnParticle(Particle.END_ROD, t.getLocation(), 25, 0.4, 0.9, 0.4, 0.04);
                        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (t.isOnline()) t.setGlowing(false); }, 160L);
                    }
                });
                p.sendTitle("§b§lPHANTOM REVEAL", "§7All invisible players exposed!", 5, 50, 10);
            }

            // 3. STORM — Thunder Strike (3 lightning bolts on aimed target)
            case STORM -> {
                LivingEntity tgt = aim(p, 30);
                if (tgt != null) {
                    w.spawnParticle(Particle.ELECTRIC_SPARK, loc, 60, 0.5, 1, 0.5, 0.12);
                    for (int i = 0; i < 3; i++) { final int fi = i;
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (!tgt.isValid()) return;
                            w.strikeLightning(tgt.getLocation());
                            w.spawnParticle(Particle.ELECTRIC_SPARK, tgt.getLocation(), 80, 0.6, 1.2, 0.6, 0.15);
                            tgt.damage(4.5 * dm, p);
                        }, fi * 8L);
                    }
                    w.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 0.8f);
                } else p.sendMessage("§7No target in range.");
            }

            // 4. FROST — Freeze Trap (~2 sec freeze nearby)
            case FROST -> {
                w.spawnParticle(Particle.SNOWFLAKE, loc, 100, 2.5, 0.5, 2.5, 0.1);
                w.spawnParticle(Particle.WHITE_ASH, loc,  60, 2.0, 0.3, 2.0, 0.05);
                w.spawnParticle(Particle.EXPLOSION, loc,   2, 0.5, 0,   0.5, 0);
                for (int ring = 1; ring <= 3; ring++) { final int fr = ring;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        double r = fr*2.0;
                        for (int i = 0; i < 20; i++) { double a = Math.toRadians(i*18);
                            w.spawnParticle(Particle.SNOWFLAKE, loc.clone().add(Math.cos(a)*r,0.2,Math.sin(a)*r), 3, 0, 0, 0, 0.02);
                        }
                    }, ring*3L);
                }
                w.playSound(loc, Sound.ENTITY_PLAYER_HURT_FREEZE, 1f, 0.5f);
                w.playSound(loc, Sound.BLOCK_GLASS_BREAK,         1f, 0.4f);
                p.getNearbyEntities(6, 6, 6).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 10));
                        le.damage(4.0 * dm, p);
                        w.spawnParticle(Particle.SNOWFLAKE, le.getLocation(), 20, 0.4, 0.8, 0.4, 0.03);
                    }
                });
            }

            // 5. FLAME — Flame Burst (AOE fire explosion + ring)
            case FLAME -> {
                w.spawnParticle(Particle.EXPLOSION, loc,   4, 0.7, 0, 0.7, 0);
                w.spawnParticle(Particle.FLAME,     loc, 120, 2.8, 0.7, 2.8, 0.2);
                w.spawnParticle(Particle.LAVA,      loc,  35, 2.2, 0.3, 2.2, 0.14);
                for (int i = 0; i < 24; i++) { double a = Math.toRadians(i*15);
                    for (int h = 0; h <= 3; h++)
                        w.spawnParticle(Particle.FLAME, loc.clone().add(Math.cos(a)*3.5,h*0.5,Math.sin(a)*3.5), 3, 0, 0.2, 0, 0.04);
                }
                w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.9f);
                w.playSound(loc, Sound.ENTITY_BLAZE_SHOOT,     1f, 0.6f);
                w.createExplosion(loc, 2.5f, false, false);
                p.getNearbyEntities(7, 7, 7).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) { le.damage(8.0*dm, p); le.setFireTicks(140); }
                });
            }

            // 6. SHADOW — Shadow Cloak (invis + night vision + speed + spiral)
            case SHADOW -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 240, 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 300, 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,        160, 1));
                w.spawnParticle(Particle.SMOKE, loc, 80, 0.5, 1.2, 0.5, 0.09);
                w.spawnParticle(Particle.DUST,  loc, 60, 0.4, 0.9, 0.4, 0, new Particle.DustOptions(Color.BLACK, 2f));
                w.spawnParticle(Particle.PORTAL,loc, 40, 0.3, 0.7, 0.3, 0.07);
                for (int i = 0; i < 20; i++) { double a = Math.toRadians(i*18);
                    w.spawnParticle(Particle.SMOKE, loc.clone().add(Math.cos(a)*0.8,i*0.1,Math.sin(a)*0.8), 3, 0, 0, 0, 0.02);
                }
                w.playSound(loc, Sound.ENTITY_ENDERMAN_STARE, 0.6f, 1.3f);
                p.sendTitle("§8§l👁 SHADOW CLOAK", "§7You vanish into darkness...", 5, 50, 10);
            }

            // 7. TITAN — Titan Strength (Strength III 6s + power rings)
            case TITAN -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH,   120, 2));
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,  60, 1));
                w.spawnParticle(Particle.DUST, loc, 80, 0.7, 1.4, 0.7, 0, new Particle.DustOptions(Color.fromRGB(180,90,20), 3f));
                w.spawnParticle(Particle.CRIT, loc, 40, 0.5, 1.0, 0.5, 0.12);
                w.spawnParticle(Particle.EXPLOSION, loc, 2, 0.4, 0, 0.4, 0);
                for (int ring = 1; ring <= 3; ring++) { final int fr = ring;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        for (int i = 0; i < 16; i++) { double a = Math.toRadians(i*22.5);
                            w.spawnParticle(Particle.DUST, loc.clone().add(Math.cos(a)*(fr*0.8),1,Math.sin(a)*(fr*0.8)), 3, 0, 0.5, 0, 0,
                                new Particle.DustOptions(Color.fromRGB(180,90,20), 2f));
                        }
                    }, fr*3L);
                }
                w.playSound(loc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 0.6f);
                w.playSound(loc, Sound.ENTITY_RAVAGER_ROAR,      0.8f, 0.8f);
                p.sendTitle("§6§l💪 TITAN STRENGTH!", "§7+Strength III active!", 5, 45, 10);
            }

            // 8. HUNTER — Mark Target (glow + hunterMarked + slow + mark ring)
            case HUNTER -> {
                LivingEntity tgt = aim(p, 30);
                if (tgt != null) {
                    tgt.setGlowing(true);
                    tgt.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,  240, 0));
                    tgt.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,  80, 1));
                    tgt.setMetadata("hunterMarked", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                    for (int i = 0; i < 12; i++) { double a = Math.toRadians(i*30);
                        w.spawnParticle(Particle.CRIT, tgt.getLocation().add(Math.cos(a)*1.5,1,Math.sin(a)*1.5), 4, 0.1, 0.2, 0.1, 0.06);
                        w.spawnParticle(Particle.DUST, tgt.getLocation().add(Math.cos(a)*1.5,1,Math.sin(a)*1.5), 2, 0, 0, 0, 0, new Particle.DustOptions(Color.RED, 1.8f));
                    }
                    w.spawnParticle(Particle.CRIT, tgt.getLocation(), 50, 0.5, 1, 0.5, 0.12);
                    w.playSound(loc, Sound.ENTITY_ARROW_HIT_PLAYER, 1f, 0.8f);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        tgt.removeMetadata("hunterMarked", plugin);
                        if (tgt.isValid()) tgt.setGlowing(false);
                    }, 240L);
                    p.sendTitle("§c§l🎯 MARKED!", "§7Target takes extra damage!", 5, 50, 10);
                } else p.sendMessage("§7No target in range.");
            }

            // 9. GRAVITY — Gravity Jump (high launch + slow fall)
            case GRAVITY -> {
                w.spawnParticle(Particle.REVERSE_PORTAL, loc, 80, 0.8, 0.5, 0.8, 0.12);
                w.spawnParticle(Particle.PORTAL,         loc, 50, 0.5, 0.3, 0.5, 0.08);
                w.spawnParticle(Particle.EXPLOSION,      loc,  2, 0,   0,   0,   0);
                for (int i = 0; i < 16; i++) { double a = Math.toRadians(i*22.5);
                    w.spawnParticle(Particle.REVERSE_PORTAL, loc.clone().add(Math.cos(a)*1.5,0.1,Math.sin(a)*1.5), 3, 0, 0.3, 0, 0.05);
                }
                w.playSound(loc, Sound.ENTITY_GHAST_SHOOT,    0.9f, 0.4f);
                w.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 0.6f);
                p.setVelocity(new Vector(0, 3.5, 0));
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (p.isOnline()) p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 60, 0));
                }, 25L);
            }

            // 10. WIND — Wind Push (massive AOE push + outward rings)
            case WIND -> {
                w.spawnParticle(Particle.WHITE_ASH, loc, 120, 2.5, 1, 2.5, 0.22);
                w.spawnParticle(Particle.CLOUD,     loc,  70, 2.0, 0.6, 2.0, 0.10);
                w.spawnParticle(Particle.EXPLOSION, loc,   2, 0.5, 0, 0.5, 0);
                for (int ring = 1; ring <= 4; ring++) { final int fr = ring;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        for (int i = 0; i < 20; i++) { double a = Math.toRadians(i*18);
                            w.spawnParticle(Particle.WHITE_ASH, loc.clone().add(Math.cos(a)*(fr*2.5),0.5,Math.sin(a)*(fr*2.5)), 4, 0, 0.3, 0, 0.06);
                        }
                    }, fr*3L);
                }
                w.playSound(loc, Sound.ENTITY_PHANTOM_DEATH,  1f, 0.4f);
                w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE,0.7f, 0.8f);
                p.getNearbyEntities(12, 12, 12).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        e.setVelocity(e.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(3.5).setY(0.7));
                        le.damage(3.0*dm, p);
                    }
                });
            }

            // 11. POISON — Poison Cloud (area poison + wither)
            case POISON -> {
                w.spawnParticle(Particle.ITEM_SLIME, loc, 140, 3.0, 0.8, 3.0, 0.12);
                w.spawnParticle(Particle.SNEEZE,     loc,  90, 3.0, 0.8, 3.0, 0.07);
                w.spawnParticle(Particle.DUST,       loc,  70, 2.5, 0.6, 2.5, 0, new Particle.DustOptions(Color.fromRGB(40,200,40), 1.8f));
                for (int i = 0; i < 16; i++) { double a = Math.toRadians(i*22.5);
                    w.spawnParticle(Particle.ITEM_SLIME, loc.clone().add(Math.cos(a)*4,0.8,Math.sin(a)*4), 5, 0, 0.4, 0, 0.04);
                }
                w.playSound(loc, Sound.ENTITY_SLIME_SQUISH, 1f, 0.3f);
                w.playSound(loc, Sound.ENTITY_WITCH_DRINK,  0.8f, 0.5f);
                p.getNearbyEntities(8, 8, 8).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        le.addPotionEffect(new PotionEffect(PotionEffectType.POISON,   200, 2));
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1));
                        le.addPotionEffect(new PotionEffect(PotionEffectType.WITHER,    60, 0));
                        le.damage(3.0*dm, p);
                    }
                });
            }

            // 12. LIGHT — Light Beam (aim: 30-block penetrating beam)
            case LIGHT -> {
                w.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.7f, 2.0f);
                Location beam = p.getEyeLocation().clone();
                boolean hit = false;
                for (int i = 0; i < 30 && !hit; i++) {
                    beam.add(dir);
                    w.spawnParticle(Particle.END_ROD, beam, 5, 0.1, 0.1, 0.1, 0.01);
                    if (i % 4 == 0) w.spawnParticle(Particle.FLASH, beam, 1, 0, 0, 0, 0);
                    if (beam.getBlock().getType().isSolid()) { w.spawnParticle(Particle.FLASH, beam, 2, 0.3, 0.3, 0.3, 0); break; }
                    for (Entity e : w.getNearbyEntities(beam, 1.2, 1.2, 1.2)) {
                        if (e instanceof LivingEntity le && e != p) {
                            le.damage(11.0*dm, p);
                            le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 1));
                            w.spawnParticle(Particle.FLASH,   le.getLocation(), 2, 0, 0, 0, 0);
                            w.spawnParticle(Particle.END_ROD, le.getLocation(), 40, 0.5, 1, 0.5, 0.07);
                            w.playSound(beam, Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 2.0f);
                            hit = true; break;
                        }
                    }
                }
            }

            // 13. EARTH — Earth Slam (5-wave expanding rings + knockback)
            case EARTH -> {
                w.spawnParticle(Particle.DUST_PLUME, loc, 160, 3, 0.2, 3, 0.26, Material.DIRT.createBlockData());
                w.spawnParticle(Particle.DUST_PLUME, loc, 100, 3, 0.2, 3, 0.26, Material.STONE.createBlockData());
                w.spawnParticle(Particle.EXPLOSION,  loc,   4, 0.9, 0, 0.9, 0);
                for (int ring = 1; ring <= 5; ring++) { final int fr = ring;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        double r = fr*1.9;
                        for (int i = 0; i < 24; i++) { double a = Math.toRadians(i*15);
                            w.spawnParticle(Particle.DUST_PLUME, loc.clone().add(Math.cos(a)*r,0.2,Math.sin(a)*r),
                                4, 0, 0.6, 0, 0.14, Material.STONE.createBlockData());
                        }
                    }, ring*3L);
                }
                w.playSound(loc, Sound.BLOCK_STONE_BREAK,        1f, 0.3f);
                w.playSound(loc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 0.4f);
                w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE,   0.8f, 0.5f);
                p.getNearbyEntities(10, 10, 10).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        e.setVelocity(e.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(2.8).setY(0.9));
                        le.damage(6.0*dm, p);
                    }
                });
            }

            // 14. CRYSTAL — Crystal Spikes (16-spike ring + AOE damage)
            case CRYSTAL -> {
                for (int i = 0; i < 16; i++) { double a = Math.toRadians(i*22.5);
                    Location sp = loc.clone().add(Math.cos(a)*4, 0, Math.sin(a)*4);
                    for (int h = 0; h <= 4; h++) {
                        w.spawnParticle(Particle.DUST, sp.clone().add(0,h*0.5,0), 3, 0.1, 0, 0.1, 0,
                            new Particle.DustOptions(Color.fromRGB(100,200,255), 2.5f));
                        w.spawnParticle(Particle.END_ROD, sp.clone().add(0,h*0.5,0), 2, 0, 0, 0, 0.01);
                    }
                }
                w.spawnParticle(Particle.END_ROD, loc, 60, 2, 0.4, 2, 0.07);
                w.playSound(loc, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1f, 0.7f);
                w.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME,  1f, 0.5f);
                p.getNearbyEntities(7, 7, 7).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        le.damage(8.0*dm, p);
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 70, 2));
                    }
                });
            }

            // 15. ECHO — Echo Blast (aim: sonic knockback; AOE fallback)
            case ECHO -> {
                LivingEntity tgt = aim(p, 25);
                if (tgt != null) {
                    w.spawnParticle(Particle.SONIC_BOOM, tgt.getLocation(), 2, 0, 0, 0, 0);
                    w.spawnParticle(Particle.END_ROD,    tgt.getLocation(), 70, 1, 1.5, 1, 0.1);
                    tgt.damage(9.0*dm, p);
                    tgt.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
                    tgt.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,  80, 2));
                    tgt.setVelocity(tgt.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(4.0).setY(1.0));
                    w.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1f, 0.4f);
                } else {
                    w.spawnParticle(Particle.SONIC_BOOM, loc, 2, 0, 0, 0, 0);
                    w.spawnParticle(Particle.END_ROD,    loc, 90, 4, 4, 4, 0.08);
                    w.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 1f, 0.3f);
                    p.getNearbyEntities(12, 12, 12).forEach(e -> {
                        if (e instanceof LivingEntity le && e != p) {
                            le.damage(5.0*dm, p);
                            e.setVelocity(e.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(2.5).setY(0.5));
                        }
                    });
                }
            }

            // 16. RAGE — Rage Smash (largest knockback + 3-wave shockwave)
            case RAGE -> {
                w.spawnParticle(Particle.EXPLOSION, loc,   5, 0.7, 0, 0.7, 0);
                w.spawnParticle(Particle.DUST,      loc, 100, 1.4, 0.7, 1.4, 0, new Particle.DustOptions(Color.RED, 2.5f));
                w.spawnParticle(Particle.CRIT,      loc,  70, 0.9, 0.5, 0.9, 0.14);
                w.spawnParticle(Particle.FLAME,     loc,  50, 0.8, 0.4, 0.8, 0.1);
                for (int ring = 1; ring <= 3; ring++) { final int fr = ring;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        for (int i = 0; i < 20; i++) { double a = Math.toRadians(i*18);
                            w.spawnParticle(Particle.DUST, loc.clone().add(Math.cos(a)*(fr*2.5),0.2,Math.sin(a)*(fr*2.5)), 4, 0, 0.4, 0, 0,
                                new Particle.DustOptions(Color.RED, 2f));
                        }
                    }, fr*3L);
                }
                w.playSound(loc, Sound.ENTITY_RAVAGER_ROAR,   1f, 0.7f);
                w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE,0.9f, 0.8f);
                p.getNearbyEntities(8, 8, 8).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        e.setVelocity(e.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(3.8).setY(1.2));
                        le.damage(9.0*dm, p);
                    }
                });
            }

            // 17. SPIRIT — Spirit Form (Resistance 255 + invis + speed)
            case SPIRIT -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,  100, 255));
                p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100, 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,         80, 1));
                w.spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 100, 0.6, 1.2, 0.6, 0.12);
                w.spawnParticle(Particle.END_ROD,          loc,  50, 0.4, 0.9, 0.4, 0.05);
                w.spawnParticle(Particle.HEART,            loc,  12, 0.5, 0.5, 0.5, 0.05);
                for (int i = 0; i < 20; i++) { double a = Math.toRadians(i*18);
                    w.spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(Math.cos(a)*0.8,i*0.12,Math.sin(a)*0.8), 2, 0, 0, 0, 0.03);
                }
                w.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.8f);
                w.playSound(loc, Sound.BLOCK_BEACON_AMBIENT,     1f,   1.5f);
                p.sendTitle("§f§l👻 SPIRIT FORM", "§7Transcending the physical...", 5, 50, 10);
            }

            // 18. TIME — Time Dash (instant 10-block blink + time trail)
            case TIME -> {
                w.spawnParticle(Particle.DUST,           loc, 60, 0.4, 0.7, 0.4, 0, new Particle.DustOptions(Color.fromRGB(150,80,255), 2f));
                w.spawnParticle(Particle.REVERSE_PORTAL, loc, 35, 0.3, 0.5, 0.3, 0.07);
                Location dest = safe(loc.clone().add(dir.clone().multiply(10)));
                for (int i = 0; i <= 20; i++) { double t = (double)i/20;
                    Location trail = loc.clone().add(dir.clone().multiply(10*t));
                    w.spawnParticle(Particle.DUST, trail, 3, 0.1, 0.3, 0.1, 0, new Particle.DustOptions(Color.fromRGB(150,80,255), 1.5f));
                    if (i%4==0) w.spawnParticle(Particle.REVERSE_PORTAL, trail, 2, 0.1, 0.2, 0.1, 0.03);
                }
                w.spawnParticle(Particle.DUST, dest, 60, 0.4, 0.7, 0.4, 0, new Particle.DustOptions(Color.fromRGB(150,80,255), 2f));
                w.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.8f);
                p.teleport(dest);
            }

            // 19. WARRIOR — Shield Bash (aim: stun + massive damage; fallback: adrenaline)
            case WARRIOR -> {
                LivingEntity tgt = aim(p, 8);
                if (tgt != null) {
                    w.spawnParticle(Particle.EXPLOSION, tgt.getLocation(),  3, 0.4, 0, 0.4, 0);
                    w.spawnParticle(Particle.CRIT,      tgt.getLocation(), 60, 0.6, 1, 0.6, 0.14);
                    w.spawnParticle(Particle.DUST,      tgt.getLocation(), 40, 0.5, 0.8, 0.5, 0,
                        new Particle.DustOptions(Color.fromRGB(220,80,20), 2f));
                    tgt.damage(13.0*dm, p);
                    tgt.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,  100, 10));
                    tgt.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,  50, 0));
                    tgt.setVelocity(tgt.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(3.0).setY(1.0));
                    w.playSound(loc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 0.5f);
                    p.sendTitle("§6§l⚔ SHIELD BASH!", "§7Enemy stunned!", 5, 50, 10);
                } else {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 120, 2));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,     80, 1));
                    w.spawnParticle(Particle.CRIT, loc, 50, 0.5, 1, 0.5, 0.12);
                    w.spawnParticle(Particle.FLAME,loc, 30, 0.4, 0.8, 0.4, 0.08);
                    w.playSound(loc, Sound.ENTITY_RAVAGER_ROAR, 0.8f, 1.0f);
                    p.sendTitle("§6§l⚡ ADRENALINE!", "§7+Strength II +Speed", 5, 45, 10);
                }
            }

            // ═══ EVENT EYES — SECONDARY ═════════════════════════════════════

            // E1. METEOR — Meteor Shower
            case METEOR -> {
                int    count = (int)ecfg("meteor","secondary-count",  8);
                double eDmg  =      ecfg("meteor","secondary-damage", 6.0)*dm;
                w.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 0.4f);
                for (int i = 0; i < count; i++) {
                    final double rx = (Math.random()-0.5)*14, rz = (Math.random()-0.5)*14;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        Location imp = loc.clone().add(rx, 0, rz);
                        for (int step = 0; step <= 5; step++) { final int fs = step;
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                w.spawnParticle(Particle.FLAME, imp.clone().add(0,10-fs*2,0), 12, 0.2, 0, 0.2, 0.18);
                                w.spawnParticle(Particle.LAVA,  imp.clone().add(0,10-fs*2,0),  4, 0.1, 0, 0.1, 0.12);
                            }, fs*1L);
                        }
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            w.spawnParticle(Particle.EXPLOSION,  imp,  2, 0.5, 0, 0.5, 0);
                            w.spawnParticle(Particle.FLAME,      imp, 50, 1.8, 0.3, 1.8, 0.14);
                            w.spawnParticle(Particle.LAVA,       imp, 12, 1.2, 0.1, 1.2, 0.09);
                            w.spawnParticle(Particle.DUST_PLUME, imp, 30, 1.2, 0.1, 1.2, 0.2, Material.MAGMA_BLOCK.createBlockData());
                            w.playSound(imp, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.1f);
                            imp.getWorld().getNearbyEntities(imp, 3.5, 3.5, 3.5).forEach(e -> {
                                if (e instanceof LivingEntity le && e != p) { le.damage(eDmg, p); le.setFireTicks(80); }
                            });
                        }, 7L);
                    }, i*7L);
                }
            }

            // E2. MIRAGE — Phantom Escape (invis + Speed IV + afterimage trail)
            case MIRAGE -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,        200, 3));
                p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 200, 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 220, 0));
                w.spawnParticle(Particle.CLOUD,   loc, 100, 0.9, 1.6, 0.9, 0.09);
                w.spawnParticle(Particle.SMOKE,   loc,  60, 0.7, 1.3, 0.7, 0.06);
                w.spawnParticle(Particle.END_ROD, loc,  35, 0.5, 1.2, 0.5, 0.04);
                new BukkitRunnable() { int t = 0;
                    public void run() {
                        if (t++ >= 8) { cancel(); return; }
                        w.spawnParticle(Particle.CLOUD, p.getLocation(), 20, 0.4, 1.0, 0.4, 0.03);
                        w.spawnParticle(Particle.SMOKE, p.getLocation(), 12, 0.3, 0.8, 0.3, 0.02);
                    }
                }.runTaskTimer(plugin, 0, 3);
                w.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT,      0.9f, 1.4f);
                w.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL,  0.8f, 1.2f);
                p.sendTitle("§7§l👁 PHANTOM ESCAPE", "§8Speed IV — Invisible!", 5, 60, 10);
            }

            // E3. OCEAN — Ocean Prison (pull + Slowness V + Wither)
            case OCEAN -> {
                double eDmg = ecfg("ocean","secondary-damage",6.0)*dm;
                w.spawnParticle(Particle.SPLASH,          loc, 180, 3, 1.2, 3, 0.28);
                w.spawnParticle(Particle.BUBBLE_POP,      loc, 120, 3, 1.2, 3, 0.14);
                w.spawnParticle(Particle.BUBBLE_COLUMN_UP,loc,  70, 2.5, 0.6, 2.5, 0.06);
                for (int s = 0; s < 5; s++) { final int fs = s;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        double r = 5.5-fs*0.8;
                        for (int i = 0; i < 24; i++) { double a = Math.toRadians(i*15)+fs*0.9;
                            w.spawnParticle(Particle.SPLASH, loc.clone().add(Math.cos(a)*r,fs*0.25,Math.sin(a)*r), 4, 0, 0, 0, 0.06);
                        }
                    }, s*4L);
                }
                w.playSound(loc, Sound.ENTITY_GUARDIAN_ATTACK, 1f, 0.3f);
                w.playSound(loc, Sound.BLOCK_WATER_AMBIENT,    1f, 0.2f);
                p.getNearbyEntities(10, 10, 10).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        e.setVelocity(loc.toVector().subtract(e.getLocation().toVector()).normalize().multiply(2.5));
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 140, 4));
                        le.addPotionEffect(new PotionEffect(PotionEffectType.WITHER,    60, 0));
                        le.damage(eDmg, p);
                    }
                });
            }

            // E4. ECLIPSE — Shadow Phase (invis + Res III + Speed II + pillars)
            case ECLIPSE -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 200, 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,   200, 3));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,        200, 2));
                p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 220, 0));
                w.spawnParticle(Particle.PORTAL,         loc, 120, 0.9, 1.8, 0.9, 0.17);
                w.spawnParticle(Particle.DRAGON_BREATH,  loc,  90, 0.7, 1.4, 0.7, 0.09);
                w.spawnParticle(Particle.DUST,           loc,  70, 0.6, 1.2, 0.6, 0, new Particle.DustOptions(Color.BLACK, 3.5f));
                for (int i = 0; i < 8; i++) { double a = Math.toRadians(i*45);
                    for (int h = 0; h <= 6; h++)
                        w.spawnParticle(Particle.PORTAL, loc.clone().add(Math.cos(a)*1.5,h*0.4,Math.sin(a)*1.5), 3, 0.1, 0, 0.1, 0.04);
                }
                w.playSound(loc, Sound.ENTITY_ENDERMAN_STARE, 0.8f, 0.6f);
                w.playSound(loc, Sound.ENTITY_WITHER_AMBIENT,  0.7f, 0.8f);
                p.sendTitle("§5§l🌑 SHADOW PHASE", "§8Untouchable in the dark...", 5, 70, 15);
            }

            // E5. GUARDIAN — Titan Shockwave (6-wave rings + largest AOE)
            case GUARDIAN -> {
                double eDmg = ecfg("guardian","secondary-damage",15.0)*dm;
                int    eRad = (int)ecfg("guardian","secondary-radius",20);
                w.spawnParticle(Particle.EXPLOSION,        loc,   8, 2,   0, 2, 0);
                w.spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 150, 3.5, 2.5, 3.5, 0.14);
                w.spawnParticle(Particle.END_ROD,          loc, 120, 3.5, 2.5, 3.5, 0.12);
                for (int wave = 1; wave <= 6; wave++) { final int fw = wave;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        double r = fw*eRad/5.0;
                        for (int i = 0; i < 36; i++) { double a = Math.toRadians(i*10);
                            w.spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(Math.cos(a)*r,0.4,Math.sin(a)*r), 4, 0, 0.6, 0, 0.06);
                            w.spawnParticle(Particle.END_ROD,          loc.clone().add(Math.cos(a)*r,0.4,Math.sin(a)*r), 2, 0, 0.3, 0, 0.02);
                        }
                    }, wave*4L);
                }
                w.playSound(loc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 0.2f);
                w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE,   1f, 0.3f);
                w.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE,    1f, 0.4f);
                p.getNearbyEntities(eRad, eRad, eRad).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        e.setVelocity(e.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(5.0).setY(1.8));
                        le.damage(eDmg, p);
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 120, 3));
                        w.spawnParticle(Particle.TOTEM_OF_UNDYING, le.getLocation(), 25, 0.4, 0.7, 0.4, 0.05);
                    }
                });
                p.sendTitle("§6§l⚡ TITAN SHOCKWAVE!", "§7The ground shatters!", 5, 70, 15);
            }
        }
        p.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.5f);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PASSIVE EFFECTS
    // ══════════════════════════════════════════════════════════════════════
    public void applyPassiveEffects(Player p, EyeType eye) {
        for (PotionEffect e : p.getActivePotionEffects()) p.removePotionEffect(e.getType());
        if (eye == null || eye == EyeType.NONE) return;
        switch (eye) {
            case VOID    -> p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION,    INF, 0, false, false));
            case PHANTOM -> p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION,    INF, 0, false, false));
            case STORM   -> p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,           INF, 1, false, false));
            case FROST   -> p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, INF, 0, false, false));
            case FLAME   -> p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, INF, 0, false, false));
            case SHADOW  -> { p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,        INF,0,false,false)); p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION,INF,0,false,false)); }
            case TITAN   -> p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,      INF, 1, false, false));
            case HUNTER  -> p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,           INF, 1, false, false));
            case GRAVITY -> p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST,      INF, 2, false, false));
            case WIND    -> p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,           INF, 1, false, false));
            case POISON  -> p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,    INF, 0, false, false));
            case LIGHT   -> p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION,    INF, 0, false, false));
            case EARTH   -> p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,      INF, 0, false, false));
            case CRYSTAL -> p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,      INF, 0, false, false));
            case ECHO    -> { p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION,INF,0,false,false)); p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,INF,0,false,false)); }
            case RAGE    -> p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH,        INF, 0, false, false));
            case SPIRIT  -> p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,    INF, 1, false, false));
            case TIME    -> p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE,           INF, 1, false, false));
            case WARRIOR -> p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH,        INF, 1, false, false));
            case METEOR  -> { p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE,INF,0,false,false)); p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,INF,0,false,false)); }
            case MIRAGE  -> p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,           INF, 2, false, false));
            case OCEAN   -> { p.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING,INF,0,false,false)); p.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER,INF,0,false,false)); }
            case ECLIPSE -> { p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION,INF,0,false,false)); p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE,INF,0,false,false)); }
            case GUARDIAN-> { p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,INF,2,false,false)); p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION,INF,1,false,false)); }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GUI  (/eye command)
    // ══════════════════════════════════════════════════════════════════════
    public void openEyeGUI(Player p) {
        Inventory gui = Bukkit.createInventory(null, 54, "§8Your Ancient Eye Status");
        EyeType type  = plugin.getPlayerData().getEye(p);
        int     level = plugin.getPlayerData().getLevel(p);
        int     xp    = plugin.getPlayerData().getXP(p);
        int     maxXP = 100;
        String  bar   = progressBar(xp, maxXP);
        String  col   = level==3?"§e§l":level==2?"§b§l":"§7§l";

        ItemStack eye = new ItemStack(Material.ENDER_EYE);
        ItemMeta  m   = eye.getItemMeta();
        m.setDisplayName(col + type.name().replace("_"," ") + " EYE");
        List<String> lore = new ArrayList<>();
        lore.add("§8━━━━━━━━━━━━━━━━━━");
        lore.add("§7Level    §f"+level+" §8/ §f3");
        lore.add("§7XP: "+bar+" §8("+xp+"/"+maxXP+")");
        lore.add("§8━━━━━━━━━━━━━━━━━━");
        lore.add("§6§lStats:");
        lore.add("§e• §7Damage Bonus:  §c+"+(int)((getDmg(p)-1)*100)+"%");
        lore.add("§e• §7Cooldown Red:  §a-"+(level*2)+"s");
        lore.add("§8━━━━━━━━━━━━━━━━━━");
        lore.add("§b§lAbilities:");
        lore.add("§f  SHIFT+F  §7→  §bPrimary");
        lore.add("§f  SHIFT+Q  §7→  §bSecondary");
        lore.add("§8━━━━━━━━━━━━━━━━━━");
        lore.add("§5§oThe power is bound to your soul.");
        m.setLore(lore); eye.setItemMeta(m);

        ItemStack border = pane(Material.PURPLE_STAINED_GLASS_PANE);
        ItemStack fill   = pane(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < 54; i++)
            gui.setItem(i, (i<9||i>=45||i%9==0||i%9==8) ? border : fill);
        gui.setItem(22, eye);
        p.openInventory(gui);
        p.playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1.2f);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════════════
    private double getDmg(Player p)   { int l=plugin.getPlayerData().getLevel(p); return l==3?1.5:l==2?1.2:1.0; }
    private Location safe(Location l) { if(l.getBlock().getType().isSolid())l.add(0,1,0); if(l.getBlock().getType().isSolid())l.add(0,1,0); return l; }
    private LivingEntity aim(Player p, double range) {
        RayTraceResult r = p.getWorld().rayTraceEntities(p.getEyeLocation(), p.getEyeLocation().getDirection(), range, 1.5, e->e!=p);
        return (r!=null && r.getHitEntity() instanceof LivingEntity le) ? le : null;
    }
    private void crystalAura(Player p) {
        World w=p.getWorld(); Location l=p.getLocation();
        Particle.DustOptions d=new Particle.DustOptions(Color.fromRGB(100,220,255),2f);
        for(double a=0;a<Math.PI*2;a+=Math.PI/10){double rx=Math.cos(a)*2,rz=Math.sin(a)*2;w.spawnParticle(Particle.DUST,l.clone().add(rx,1,rz),3,0,0.4,0,0,d);w.spawnParticle(Particle.END_ROD,l.clone().add(rx,1,rz),2,0,0.3,0,0.01);}
        w.spawnParticle(Particle.END_ROD,l,40,0.5,1.2,0.5,0.04);
    }
    private void buildWall(Player p) {
        Block b=p.getTargetBlockExact(5); if(b==null)return;
        for(int y=1;y<=3;y++) b.getLocation().add(0,y,0).getBlock().setType(Material.COBBLESTONE);
    }
    private void voidRift(World w, Location l) {
        w.spawnParticle(Particle.PORTAL,         l, 70, 0.6, 0.9, 0.6, 0.28);
        w.spawnParticle(Particle.REVERSE_PORTAL, l, 45, 0.4, 0.7, 0.4, 0.16);
        w.spawnParticle(Particle.DRAGON_BREATH,  l, 30, 0.3, 0.5, 0.3, 0.06);
        for(int i=0;i<14;i++){double a=Math.toRadians(i*(360.0/14));w.spawnParticle(Particle.PORTAL,l.clone().add(Math.cos(a)*1.4,0.5,Math.sin(a)*1.4),3,0,0,0,0.09);}
    }
    private boolean checkCD(Player p, String t) {
        String k=p.getUniqueId()+t;
        if(cooldowns.containsKey(k)&&cooldowns.get(k)>System.currentTimeMillis()){p.sendMessage("§c§lCOOLDOWN §7— "+(cooldowns.get(k)-System.currentTimeMillis())/1000+"s");return true;}
        return false;
    }
    private void setCD(Player p, String t, int sec) {
        int cd=Math.max(2,sec-(plugin.getPlayerData().getLevel(p)*2));
        cooldowns.put(p.getUniqueId()+t, System.currentTimeMillis()+cd*1000L);
    }
    private double ecfg(String eye, String key, double def) { return plugin.getConfig().getDouble("event-eyes."+eye+"."+key, def); }
    private ItemStack pane(Material mat) { ItemStack i=new ItemStack(mat); ItemMeta m=i.getItemMeta(); m.setDisplayName("§r"); i.setItemMeta(m); return i; }
    private String progressBar(int cur, int max) {
        int bars=12, done=(int)((double)cur/max*bars);
        StringBuilder sb=new StringBuilder("§a");
        for(int i=0;i<bars;i++){if(i==done)sb.append("§7");sb.append("┃");}
        return sb.toString();
    }
}
