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
    private static final int INF = -1;

    // ── Level scaling ─────────────────────────────────────────────────────
    // getDmg  : L1=1.0  L2=1.2  L3=1.5
    // getDur  : L1=1.0  L2=1.5  L3=2.0  (potion tick multiplier)
    // getCd   : reads config, then subtracts 2s per level above 1
    // ─────────────────────────────────────────────────────────────────────

    public AbilityLogic(AncientEyePlugin plugin) { this.plugin = plugin; }

    // ══════════════════════════════════════════════════════════════════════
    //  GUI LOCK
    // ══════════════════════════════════════════════════════════════════════
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals("§8Your Ancient Eye Status")) e.setCancelled(true);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PASSIVE COMBAT — owner is NEVER the damaged entity from its own eye
    // ══════════════════════════════════════════════════════════════════════
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player victim) {
            EyeType ve = plugin.getPlayerData().getEye(victim);
            if (ve == EyeType.VOID && Math.random() < 0.15) {
                e.setCancelled(true);
                victim.sendMessage("§5§l✦ Void Dodge!");
                victim.getWorld().spawnParticle(Particle.PORTAL, victim.getLocation(), 30, 0.5, 1, 0.5, 0.1);
                return;
            }
            if (ve == EyeType.MIRAGE && Math.random() < 0.15) {
                e.setCancelled(true);
                victim.sendMessage("§e§l✦ Mirage! Attack missed!");
                victim.getWorld().spawnParticle(Particle.CLOUD, victim.getLocation(), 20, 0.4, 0.8, 0.4, 0.04);
                return;
            }
            if (ve == EyeType.GUARDIAN) e.setDamage(e.getDamage() * 0.65);
        }
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
            if (ae == EyeType.ECLIPSE) {
                long t = atk.getWorld().getTime();
                if (t > 13000 && t < 23000) e.setDamage(e.getDamage() * 1.30);
            }
        }
    }
    / ── HUNTER MARK — extra damage handler ───────────────────────────────────────
// onEntityDamage mein add karo — existing code ke baad

// Agar entity marked hai toh extra damage do
if (e.getEntity() instanceof LivingEntity victim) {
    if (victim.hasMetadata("hunterMarked")) {
        // Mark lagane wala hunter check karo
        String markerUUID = victim.getMetadata("hunterMarked").get(0).asString();
        // Attacker hunter hai ya koi bhi attack kare — extra damage
        e.setDamage(e.getDamage() * 1.5); // 50% extra damage
    }
}
    
@EventHandler
public void onPlayerKillXP(org.bukkit.event.entity.PlayerDeathEvent e) {
    if (e.getEntity().getKiller() == null) return;
    Player killer = e.getEntity().getKiller();
    if (plugin.getPlayerData().getEye(killer) == EyeType.NONE) return;
    plugin.getPlayerData().addXp(killer,
        plugin.getConfig().getInt("settings.xp-per-kill", 1));
}
    //════════════════════════════════════════════════════════════════════
@EventHandler
public void onFootstep(org.bukkit.event.player.PlayerMoveEvent e) {
    Player mover = e.getPlayer();

    // Sirf ground movement — jump/fall ignore
    if (!mover.isOnGround()) return;

    // Har 10 ticks ek baar check — performance ke liye
    if (mover.getTicksLived() % 10 != 0) return;

    // Koi bhi ECHO eye holder nearby check karo
    for (Player owner : Bukkit.getOnlinePlayers()) {
        if (owner.getUniqueId().equals(mover.getUniqueId())) continue;
        EyeType ownerEye = plugin.getPlayerData().getEye(owner);
        if (ownerEye != EyeType.ECHO) continue;

        double distSq = owner.getLocation().distanceSquared(mover.getLocation());
        if (distSq > 20 * 20) continue; // 20 block range

        // Owner ko signal — action bar par dikhao
        double dist = Math.sqrt(distSq);
        String dir  = getDirectionLabel(owner, mover);
        owner.sendActionBar("§3👣 §f" + mover.getName()
            + " §7footstep §b" + (int)dist + "§7 blocks §3" + dir);

        // Footstep particle — sirf owner ko dikhao
        owner.spawnParticle(Particle.SONIC_BOOM,
            mover.getLocation().add(0, 0.1, 0), 1, 0, 0, 0, 0);

        // Footstep damage — 0.5 damage har step (passive)
        if (mover instanceof LivingEntity le) {
            // Sirf enemies — agar mover bhi ECHO hai toh skip
            EyeType moverEye = plugin.getPlayerData().getEye(mover);
            if (moverEye != EyeType.ECHO) {
                le.damage(0.5, owner);
            }
        }
    }
}

// Direction label helper
private String getDirectionLabel(Player from, Player to) {
    double dx = to.getLocation().getX() - from.getLocation().getX();
    double dz = to.getLocation().getZ() - from.getLocation().getZ();
    double angle = Math.toDegrees(Math.atan2(dz, dx));
    double yaw   = (from.getLocation().getYaw() + 90) % 360;
    double rel   = ((angle - yaw) % 360 + 360) % 360;
    if (rel < 45 || rel >= 315) return "▲ Front";
    if (rel < 135)              return "▶ Right";
    if (rel < 225)              return "▼ Behind";
    return                             "◀ Left";
}

    // ══════════════════════════════════════════════════════════════════════
    //  PRIMARY  (SHIFT + F)
    // ══════════════════════════════════════════════════════════════════════
    public void activatePrimary(Player p, EyeType eye) {
        if (plugin.getCooldownManager().isOnCooldown(p, "P")) return;
        plugin.getCooldownManager().setCooldown(p, "P", getCd(p, eye, "primary"));
        Location loc = p.getLocation().clone();
        Vector   dir = p.getEyeLocation().getDirection().normalize();
        double   dm  = getDmg(p);
        double   dr  = getDur(p);
        World    w   = p.getWorld();

        switch (eye) {

            // 1. VOID — Void Blink
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

            // 2. PHANTOM — Ghost Dash
            case PHANTOM -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, ticks(40, dr), 0));
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
                
case STORM -> {
    double dmg = ecfg("STORM", "primary-damage", 12.0);
    LivingEntity target = aim(p, 30.0); // 30 block range tak aim
    
    if (target == null) {
        p.sendMessage("§cKoi target nahi mila!");
        return;
    }

    Location tLoc = target.getLocation();
    w.strikeLightningEffect(tLoc); // Visual lightning
    w.playSound(tLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2f, 1f);

    // Masterpiece Animation: Blocks udne wala effect
    for (int i = 0; i < 8; i++) {
        FallingBlock fb = w.spawnFallingBlock(tLoc.clone().add(0, 0.5, 0), w.getBlockAt(tLoc.clone().subtract(0, 1, 0)).getBlockData());
        fb.setVelocity(new Vector((Math.random() - 0.5) * 0.5, 0.6 + Math.random(), (Math.random() - 0.5) * 0.5));
        fb.setDropItem(false);
    }

    // Explosion aur Particles
    w.spawnParticle(Particle.EXPLOSION_EMITTER, tLoc, 1);
    w.spawnParticle(Particle.FLASH, tLoc, 5);
    w.spawnParticle(Particle.ELECTRIC_SPARK, tLoc, 50, 1, 1, 1, 0.1);

    // Safe Damage (Target ko lagega, Owner ko nahi)
    applySafeDamage(p, tLoc, 4.0, dmg);
  }


            // 4. FROST — Ice Slide
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

            // 5. FLAME — Fire Dash
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
                            if (e instanceof LivingEntity le && e != p) {
                                le.setFireTicks(ticks(100, dr));
                                le.damage(1.5 * dm, p);
                            }
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

            // 7. TITAN — Ground Slam  [knockback 1 tick after damage]
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
                        Vector vel = e.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(2.2).setY(1.2);
                        le.damage(6.0 * dm, p);
                        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (e.isValid()) e.setVelocity(vel); }, 1L);
                    }
                });
            }
// ── HUNTER PRIMARY — Hunter Dash ─────────────────────────────────────────────
// Aim based — target ki taraf fast dash
// Owner ko koi damage nahi, enemy ko damage hoga
case HUNTER -> {
    LivingEntity tgt = aim(p, 30);
    if (tgt != null) {
        Location tgtLoc = tgt.getLocation();

        // Direction — player se target ki taraf
        Vector dashDir = tgtLoc.toVector()
            .subtract(p.getLocation().toVector())
            .normalize();

        // Origin particles
        w.spawnParticle(Particle.CRIT,     loc, 30, 0.3, 0.5, 0.3, 0.12);
        w.spawnParticle(Particle.END_ROD,  loc, 15, 0.2, 0.4, 0.2, 0.06);
        w.spawnParticle(Particle.SOUL,     loc, 10, 0.3, 0.5, 0.3, 0.05);
        w.playSound(loc, Sound.ENTITY_ARROW_SHOOT,  1f, 1.8f);
        w.playSound(loc, Sound.ENTITY_PHANTOM_FLAP, 0.8f, 1.5f);

        // No fall damage
        p.setFallDistance(0f);

        // Dash — 1 tick baad set karo
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            p.setVelocity(dashDir.clone().multiply(1.8).setY(0.35));
            p.setFallDistance(0f);
        }, 1L);

        // Trail + damage on hit
        new BukkitRunnable() {
            int t = 0;
            public void run() {
                if (t++ >= 10) {
                    p.setFallDistance(0f);
                    cancel(); return;
                }

                Location cur = p.getLocation();

                // Trail particles
                w.spawnParticle(Particle.CRIT,    cur, 5, 0.2, 0.3, 0.2, 0.08);
                w.spawnParticle(Particle.END_ROD, cur, 2, 0.1, 0.2, 0.1, 0.02);
                w.spawnParticle(Particle.SOUL,    cur, 3, 0.2, 0.3, 0.2, 0.04);

                p.setFallDistance(0f);

                // Hit check — target ke 2.5 block mein
                if (p.getLocation().distanceSquared(tgtLoc) < 2.5 * 2.5) {
                    tgt.damage(7.0 * dm, p);

                    // Hit burst
                    w.spawnParticle(Particle.CRIT,    tgtLoc, 40, 0.5, 0.8, 0.5, 0.12);
                    w.spawnParticle(Particle.END_ROD, tgtLoc,  20, 0.4, 0.7, 0.4, 0.08);
                    w.spawnParticle(Particle.SOUL,    tgtLoc,  15, 0.3, 0.6, 0.3, 0.05);
                    w.playSound(tgtLoc, Sound.ENTITY_ARROW_HIT_PLAYER, 1f, 1.2f);
                    w.playSound(tgtLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1f);

                    // Knockback
                    Vector knock = dashDir.clone().multiply(1.5).setY(0.4);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (tgt.isValid()) tgt.setVelocity(knock);
                    }, 1L);

                    p.setFallDistance(0f);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 2L, 1L);

    } else {
        p.sendMessage("§7No target in range.");
    }
}

            // 9. GRAVITY — Gravity Pull  [knockback 1 tick after damage]
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
                w.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE,  1f, 0.3f);
                w.playSound(loc, Sound.ENTITY_ENDERMAN_SCREAM, 0.5f, 0.5f);
                p.getNearbyEntities(8, 8, 8).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        Vector vel = loc.toVector().subtract(e.getLocation().toVector()).normalize().multiply(2.2);
                        le.damage(3.0 * dm, p);
                        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (e.isValid()) e.setVelocity(vel); }, 1L);
                    }
                });
            }

            // 10. WIND — Wind Dash  [knockback 1 tick after damage]
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
                            if (e instanceof LivingEntity && e != p) {
                                Vector vel = e.getLocation().toVector().subtract(cur.toVector()).normalize().multiply(1.8).setY(0.4);
                                Bukkit.getScheduler().runTaskLater(plugin, () -> { if (e.isValid()) e.setVelocity(vel); }, 1L);
                            }
                        });
                    }
                }.runTaskTimer(plugin, 0, 1);
            }

            // 11. POISON — Poison Strike
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
                    tgt.addPotionEffect(new PotionEffect(PotionEffectType.POISON,   ticks(200, dr), 2));
                    tgt.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks(100, dr), 1));
                    tgt.damage(4.0 * dm, p);
                    w.playSound(loc, Sound.ENTITY_SLIME_SQUISH, 1f, 0.4f);
                    w.playSound(loc, Sound.ENTITY_WITCH_DRINK,  0.8f, 0.6f);
                } else p.sendMessage("§7No target in range.");
            }
//════════════════════════════════════════════════════════════════════
case LIGHT -> {
    // Origin flash + expanding rings
    w.spawnParticle(Particle.FLASH,   loc, 3, 0, 0, 0, 0);
    w.spawnParticle(Particle.END_ROD, loc, 80, 2.5, 2.5, 2.5, 0.12);
    w.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT,    1f, 2.0f);
    w.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 2.0f);

    for (int ring = 1; ring <= 4; ring++) {
        final int fr = ring;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            double r = fr * 2.8;
            for (int i = 0; i < 24; i++) {
                double a = Math.toRadians(i * 15);
                w.spawnParticle(Particle.END_ROD,
                    loc.clone().add(Math.cos(a)*r, 0.5, Math.sin(a)*r),
                    2, 0, 0.2, 0, 0.01);
            }
        }, ring * 3L);
    }

    // Damage + white screen sab nearby entities
    p.getNearbyEntities(10, 10, 10).forEach(e -> {
        if (!(e instanceof LivingEntity le) || e == p) return;

        le.damage(5.0 * dm, p);
        w.spawnParticle(Particle.FLASH,   le.getLocation(), 1, 0, 0, 0, 0);
        w.spawnParticle(Particle.END_ROD, le.getLocation(), 20, 0.4, 0.8, 0.4, 0.05);

        // Sirf Player ka screen white hoga — mobs ke paas screen nahi
        if (le instanceof Player ep) {
            whiteLightScreen(ep, ticks(80, dr), plugin); // 4s
        }
    });
}


            // 14. CRYSTAL — Crystal Shield
            case CRYSTAL -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, ticks(200, dr), 4));
                crystalAura(p);
                w.spawnParticle(Particle.END_ROD, loc, 60, 1.2, 1.8, 1.2, 0.06);
                w.spawnParticle(Particle.DUST,    loc, 40, 1.0, 1.5, 1.0, 0, new Particle.DustOptions(Color.fromRGB(100,220,255), 2f));
                w.playSound(loc, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1f, 1.5f);
                w.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME,   1f, 1.2f);
                p.sendTitle("§b§l💎 CRYSTAL SHIELD", "§7Resistance V Active!", 5, 45, 10);
            }
//═══════════════════════════════════════════════════════════════════
case ECHO -> {
    // Sonic boom origin
    w.spawnParticle(Particle.SONIC_BOOM, loc, 1, 0, 0, 0, 0);
    w.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_CURSE,    0.8f, 1.8f);
    w.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT,  0.5f, 2.0f);

    // 5 expanding sonar rings
    for (int ring = 1; ring <= 5; ring++) {
        final int fr = ring;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            double r = fr * 3.5; // ring expands outward
            int    pts = 32;
            for (int i = 0; i < pts; i++) {
                double a = Math.toRadians(i * (360.0 / pts));
                // Main ring
                w.spawnParticle(Particle.END_ROD,
                    loc.clone().add(Math.cos(a)*r, 0.5, Math.sin(a)*r),
                    1, 0, 0, 0, 0);
                // Inner ring slightly smaller
                w.spawnParticle(Particle.SONIC_BOOM,
                    loc.clone().add(Math.cos(a)*(r-0.3), 0.5, Math.sin(a)*(r-0.3)),
                    1, 0, 0, 0, 0);
            }
            // Vertical ring (Y plane)
            for (int i = 0; i < 24; i++) {
                double a = Math.toRadians(i * 15);
                w.spawnParticle(Particle.END_ROD,
                    loc.clone().add(0, Math.cos(a)*r*0.4 + r*0.3, Math.sin(a)*r),
                    1, 0, 0, 0, 0);
            }
        }, ring * 4L);
    }

    // Reveal nearby players — glow + action bar
    p.getNearbyEntities(20, 20, 20).forEach(e -> {
        if (!(e instanceof LivingEntity le) || e == p) return;

        le.setGlowing(true);
        w.spawnParticle(Particle.END_ROD, le.getLocation(), 15, 0.4, 0.8, 0.4, 0.04);

        // Glow hat jaata hai baad mein
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (le.isValid()) le.setGlowing(false);
        }, ticks(140, dr));

        // Owner ko dikhao kaun reveal hua
        if (le instanceof Player ep) {
            p.sendActionBar("§3🔍 §fRevealed: §b" + ep.getName());
        }
    });

    p.sendTitle("§3§lSONAR PULSE", "§7All enemies revealed!", 5, 50, 10);
}


            // 16. RAGE — Rage Mode
            case RAGE -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE,    ticks(160, dr), 3));
                p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, ticks(100, dr), 1));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,    ticks(80,  dr), 1));
                w.spawnParticle(Particle.DUST,  loc, 100, 0.6, 1.2, 0.6, 0, new Particle.DustOptions(Color.RED, 2.5f));
                w.spawnParticle(Particle.CRIT,  loc,  60, 0.5, 1.0, 0.5, 0.14);
                w.spawnParticle(Particle.FLAME, loc,  40, 0.4, 0.8, 0.4, 0.09);
                for (int i = 0; i < 16; i++) { double a = Math.toRadians(i*22.5);
                    w.spawnParticle(Particle.DUST, loc.clone().add(Math.cos(a)*2,1,Math.sin(a)*2), 4, 0, 0.5, 0, 0,
                        new Particle.DustOptions(Color.fromRGB(220,20,20), 2f));
                }
                w.playSound(loc, Sound.ENTITY_RAVAGER_ROAR, 1f, 0.8f);
                w.playSound(loc, Sound.ENTITY_WITHER_HURT,  0.7f, 1.2f);
                p.sendTitle("§c§l⚡ RAGE MODE!", "§7+Haste III  +Strength  +Speed", 5, 50, 10);
            }

            // 17. SPIRIT — Spirit Heal
            case SPIRIT -> {
                double maxHp = p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
                p.setHealth(Math.min(p.getHealth() + 8.0 * dm, maxHp));
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, ticks(120, dr), 2));
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

            // 18. TIME — Time Slow
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
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks(160, dr), 5));
                        le.addPotionEffect(new PotionEffect(PotionEffectType.WITHER,   ticks(160,  dr), 1));
                        w.spawnParticle(Particle.REVERSE_PORTAL, le.getLocation(), 20, 0.3, 0.6, 0.3, 0.06);
                    }
                });
                p.sendTitle("§d§lTIME SLOW", "§7Enemies frozen in time!", 5, 100, 10);
            }

            // 19. WARRIOR — War Cry
            case WARRIOR -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH,   ticks(160, dr), 2));
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, ticks(120, dr), 1));
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
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks(60, dr), 1));
                        le.damage(3.0 * dm, p);
                    }
                });
                p.sendTitle("§6§l⚔ WAR CRY!", "§7+Strength II  +Resistance", 5, 50, 10);
            }

            // ═══ EVENT EYES ══════════════════════════════════════════════════
// METEOR PRIMARY
case METEOR -> {
    double dmg = ecfg("METEOR", "primary-damage", 15.0);
    p.setVelocity(p.getLocation().getDirection().multiply(1.8).setY(1.5));
    w.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 0.5f);
    
    new BukkitRunnable() {
        public void run() {
            if (!p.isOnline() || p.isDead()) { cancel(); return; }
            
            // ⭐ OWNER SAFE: Hawa mein fall damage jama nahi hoga
            p.setFallDistance(0.0f); 
            
            // Epic aag ki trail jab player hawa mein hai
            w.spawnParticle(Particle.FLAME, p.getLocation(), 20, 0.5, 0.5, 0.5, 0.05);
            w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, p.getLocation(), 10, 0.3, 0.3, 0.3, 0.02);

            // Zameen par takrane ka check (falling down)
            if (p.getVelocity().getY() < -0.1 && p.isOnGround()) {
                w.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 3f, 0.5f);
                w.spawnParticle(Particle.EXPLOSION_EMITTER, p.getLocation(), 4);
                w.spawnParticle(Particle.LAVA, p.getLocation(), 80, 3, 1, 3, 0.5);
                
                // Shockwave particles
                for(int i = 0; i < 360; i+=10) {
                    Location circle = p.getLocation().add(Math.cos(Math.toRadians(i))*4, 0.2, Math.sin(Math.toRadians(i))*4);
                    w.spawnParticle(Particle.FLAME, circle, 2, 0, 0, 0, 0.2);
                }
                
                applySafeDamage(p, p.getLocation(), 7.0, dmg);
                
                // Dushmano ko hawa mein uchhalna (Unhe girne par fall damage padega!)
                p.getLocation().getWorld().getNearbyEntities(p.getLocation(), 7, 7, 7).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        Vector push = le.getLocation().toVector().subtract(p.getLocation().toVector()).normalize();
                        le.setVelocity(push.multiply(2.5).setY(1.2)); // Huge knockback
                    }
                });
                cancel();
            }
        }
    }.runTaskTimer(plugin, 5, 1);
}


            // MIRAGE PRIMARY
case MIRAGE -> {
    w.playSound(p.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1f, 1f);
    new BukkitRunnable() {
        int ticks = 0;
        public void run() {
            if (ticks++ >= 100 || !p.isOnline() || p.isDead()) { cancel(); return; } // 5 seconds
            
            // 3 Clones (Particles rotating around player)
            for (int i = 0; i < 3; i++) {
                double angle = (ticks * 0.2) + (i * (Math.PI * 2 / 3)); // Math magic for rotation
                Location clone = p.getLocation().clone().add(Math.cos(angle) * 3, 1, Math.sin(angle) * 3);
                
                // Human shape particles
                w.spawnParticle(Particle.WITCH, clone, 10, 0.2, 0.8, 0.2, 0.05);
                w.spawnParticle(Particle.PORTAL, clone, 15, 0.3, 1.0, 0.3, 0.1);
            }
        }
    }.runTaskTimer(plugin, 0, 1);
}

// OCEAN PRIMARY
case OCEAN -> {
    double dmg = ecfg("OCEAN", "primary-damage", 12.0);
    w.playSound(p.getLocation(), Sound.ENTITY_DOLPHIN_SPLASH, 2f, 0.5f);
    
    new BukkitRunnable() {
        int ticks = 0;
        Location wave = p.getLocation().clone().add(0, 0.5, 0);
        Vector dir = p.getLocation().getDirection().setY(0).normalize().multiply(1.2); // Wave speed
        
        public void run() {
            if (ticks++ >= 15 || !p.isOnline()) { cancel(); return; } // Range
            
            wave.add(dir); // Wave aage badh rahi hai
            
            // Wall of Water (Tsunami animation)
            for(double x = -2; x <= 2; x+=0.5) {
                for(double y = 0; y <= 2; y+=0.5) {
                    Vector side = new Vector(-dir.getZ(), 0, dir.getX()).normalize().multiply(x);
                    Location partLoc = wave.clone().add(side).add(0, y, 0);
                    w.spawnParticle(Particle.DRIPPING_WATER, partLoc, 5, 0.1, 0.1, 0.1, 0.5);
                    w.spawnParticle(Particle.BUBBLE_POP, partLoc, 2, 0.1, 0.1, 0.1, 0.1);
                }
            }
            
            w.playSound(wave, Sound.BLOCK_WATER_AMBIENT, 0.5f, 1.5f);
            applySafeDamage(p, wave, 3.0, dmg);
            
            // Push enemies along with the wave
            wave.getWorld().getNearbyEntities(wave, 3, 3, 3).forEach(e -> {
                if (e instanceof LivingEntity le && e != p) {
                    le.setVelocity(dir.clone().multiply(1.5).setY(0.4));
                }
            });
        }
    }.runTaskTimer(plugin, 0, 1);
}


            // ECLIPSE PRIMARY
case ECLIPSE -> {
    double dmg = ecfg("ECLIPSE", "primary-damage", 18.0);
    LivingEntity target = aim(p, 30.0);

    if (target == null) {
        p.sendMessage("§cNo target found!");
        return;
    }

    w.playSound(p.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1f, 0.5f);
    w.playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_STARE, 0.8f, 0.5f);

    new BukkitRunnable() {
        int    ticks     = 0;
        double cubeAngle = 0;

        public void run() {
            // Safety — agar target mar gaya ya 3s ho gayi
            if (!target.isValid() || target.isDead()) { cancel(); return; }

            // ── 3 SECONDS (60 ticks) BLACK CUBE ANIMATION ────────────────
            if (ticks < 60) {
                cubeAngle += 0.08;
                Location body = target.getLocation().clone().add(0, 1.0, 0);

                // ── Rotating BLACK CUBE — 8 vertices, 12 edges ──────────
                double r   = 0.75 + Math.sin(ticks * 0.15) * 0.08; // slight pulse
                double cosA = Math.cos(cubeAngle), sinA = Math.sin(cubeAngle);
                double cosT = Math.cos(0.4),       sinT = Math.sin(0.4);

                double[][] verts = {
                    {-r,-r,-r},{r,-r,-r},{r,r,-r},{-r,r,-r},
                    {-r,-r, r},{r,-r, r},{r,r, r},{-r,r, r}
                };
                int[][] edges = {
                    {0,1},{1,2},{2,3},{3,0},
                    {4,5},{5,6},{6,7},{7,4},
                    {0,4},{1,5},{2,6},{3,7}
                };
                // Rotate vertices
                double[][] rv = new double[8][3];
                for (int v = 0; v < 8; v++) {
                    double x = verts[v][0], y = verts[v][1], z = verts[v][2];
                    double nx  = x*cosA - z*sinA;
                    double nz  = x*sinA + z*cosA;
                    double ny2 = y*cosT - nz*sinT;
                    double nz2 = y*sinT + nz*cosT;
                    rv[v][0]=nx; rv[v][1]=ny2+r; rv[v][2]=nz2;
                }
                // Draw edges
                Particle.DustOptions black = new Particle.DustOptions(
                    org.bukkit.Color.fromRGB(10, 0, 20), 1.1f);
                for (int[] edge : edges) {
                    double[] va = rv[edge[0]], vb = rv[edge[1]];
                    for (int k = 0; k <= 5; k++) {
                        double t  = (double)k/5;
                        Location pt = body.clone().add(
                            va[0]+(vb[0]-va[0])*t,
                            va[1]+(vb[1]-va[1])*t - r,
                            va[2]+(vb[2]-va[2])*t);
                        w.spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0, black);
                    }
                }

                // Center dark core
                w.spawnParticle(Particle.SQUID_INK, body, 2, 0.1, 0.1, 0.1, 0.01);
                w.spawnParticle(Particle.DRAGON_BREATH, body, 3, 0.15, 0.15, 0.15, 0.02);

                // Warning sound intensifies
                if (ticks % 20 == 0) {
                    float pitch = 0.4f + (ticks / 60f) * 1.2f;
                    w.playSound(target.getLocation(),
                        Sound.ENTITY_ENDERMAN_AMBIENT, 0.6f, pitch);
                }

                ticks += 2;

            } else {
                // ── BLAST — cube disappear, TNT black explosion ──────────
                cancel();

                Location blastLoc = target.getLocation().clone().add(0, 1, 0);

                // Cube disappear flash
                w.spawnParticle(Particle.SQUID_INK,      blastLoc, 60, 0.5, 0.5, 0.5, 0.15);
                w.spawnParticle(Particle.DRAGON_BREATH,  blastLoc, 80, 0.8, 0.8, 0.8, 0.08);
                w.spawnParticle(Particle.EXPLOSION_EMITTER, blastLoc, 2, 0, 0, 0, 0);
                w.spawnParticle(Particle.EXPLOSION,      blastLoc, 20, 1.0, 0.5, 1.0, 0.1);

                // Blast sounds
                w.playSound(blastLoc, Sound.ENTITY_GENERIC_EXPLODE,         2f, 0.4f);
                w.playSound(blastLoc, Sound.ENTITY_WITHER_BREAK_BLOCK,       1f, 0.5f);
                w.playSound(blastLoc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE,  1f, 0.3f);

                // ── 10 block radius shockwave rings ─────────────────────
                for (int ring = 1; ring <= 5; ring++) {
                    final int fr = ring;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        double rad = fr * 2.0;
                        int    pts = 36;
                        for (int i = 0; i < pts; i++) {
                            double a = Math.toRadians(i * (360.0 / pts));
                            // Ground ring
                            w.spawnParticle(Particle.SQUID_INK,
                                blastLoc.clone().add(Math.cos(a)*rad, 0.1, Math.sin(a)*rad),
                                1, 0, 0, 0, 0);
                            // Mid ring
                            w.spawnParticle(Particle.DRAGON_BREATH,
                                blastLoc.clone().add(Math.cos(a)*rad, 1.0, Math.sin(a)*rad),
                                1, 0, 0, 0, 0);
                            // Top ring
                            w.spawnParticle(Particle.SQUID_INK,
                                blastLoc.clone().add(Math.cos(a)*rad, 2.0, Math.sin(a)*rad),
                                1, 0, 0, 0, 0);
                        }
                        // Ring sound
                        w.playSound(blastLoc, Sound.ENTITY_WITHER_AMBIENT,
                            0.5f, 0.3f + fr * 0.1f);
                    }, ring * 2L);
                }

                // ── Block shake — nearby blocks vibrate effect ───────────
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    for (int i = 0; i < 30; i++) {
                        double angle = Math.toRadians(i * 12);
                        double dist  = 2 + (i % 5);
                        Location blk = blastLoc.clone().add(
                            Math.cos(angle)*dist, -0.5, Math.sin(angle)*dist);
                        w.spawnParticle(Particle.FALLING_DUST, blk, 5,
                            0.3, 0.1, 0.3, 0.05,
                            org.bukkit.Material.OBSIDIAN.createBlockData());
                    }
                }, 3L);

                // ── Damage — owner safe, 10 block radius ─────────────────
                applySafeDamage(p, blastLoc, 10.0, dmg);

                // Knockback — enemies ko blast se door phenk do
                blastLoc.getWorld().getNearbyEntities(blastLoc, 10, 10, 10)
                    .forEach(e -> {
                        if (e instanceof LivingEntity le && e != p) {
                            Vector vel = e.getLocation().toVector()
                                .subtract(blastLoc.toVector())
                                .normalize().multiply(3.5).setY(1.2);
                            Bukkit.getScheduler().runTaskLater(plugin,
                                () -> { if (e.isValid()) e.setVelocity(vel); }, 1L);
                        }
                    });
            }
        }
    }.runTaskTimer(plugin, 0, 2);
}

            /// GUARDIAN PRIMARY
case GUARDIAN -> {
    w.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 2f, 1.2f);
    
    new BukkitRunnable() {
        int ticks = 0;
        public void run() {
            if (ticks++ >= 120 || !p.isOnline() || p.isDead()) { 
                w.playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 2f, 1.2f);
                cancel(); return; 
            } // 6 seconds dome
            
            // Beautiful Half-Sphere (Dome) Animation
            for (double t = 0; t <= Math.PI / 2; t += Math.PI / 10) { // Math.PI/2 makes it a dome, not full sphere
                for (double r = 0; r <= 2 * Math.PI; r += Math.PI / 10) {
                    double x = 5.0 * Math.sin(t) * Math.cos(r);
                    double y = 5.0 * Math.cos(t); // Top half
                    double z = 5.0 * Math.sin(t) * Math.sin(r);
                    w.spawnParticle(Particle.ENCHANT, p.getLocation().clone().add(x, y, z), 1, 0,0,0,0);
                }
            }
            
            // Protection to allies and self
            p.getWorld().getNearbyEntities(p.getLocation(), 5, 5, 5).forEach(e -> {
                if (e instanceof Player ally) {
                    ally.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 1));
                 }
             });
         }
      }.runTaskTimer(plugin, 0, 3);
   }
        }  // end switch (activatePrimary)
        p.playSound(loc, Sound.ENTITY_ENDER_EYE_DEATH, 0.8f, 1f);
    }  // end activatePrimary



    // ══════════════════════════════════════════════════════════════════════
    //  SECONDARY  (SHIFT + Q)
    // ══════════════════════════════════════════════════════════════════════
    public void activateSecondary(Player p, EyeType eye) {
        if (plugin.getCooldownManager().isOnCooldown(p, "S")) return;
        plugin.getCooldownManager().setCooldown(p, "S", getCd(p, eye, "secondary"));
        Location loc = p.getLocation().clone();
        Vector   dir = p.getEyeLocation().getDirection().normalize();
        double   dm  = getDmg(p);
        double   dr  = getDur(p);
        World    w   = p.getWorld();

        switch (eye) {

            // 1. VOID — Void Trap (3 pull waves)  [knockback 1 tick after damage]
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
                                Vector vel = loc.toVector().subtract(e.getLocation().toVector()).normalize().multiply(2.2);
                                le.damage(2.5 * dm, p);
                                Bukkit.getScheduler().runTaskLater(plugin, () -> { if (e.isValid()) e.setVelocity(vel); }, 1L);
                            }
                        }), wave * 10L);
                }
            }

            // 2. PHANTOM — Phantom Reveal
            case PHANTOM -> {
                w.spawnParticle(Particle.END_ROD,    loc, 100, 4, 4, 4, 0.09);
                w.spawnParticle(Particle.SONIC_BOOM, loc,   1, 0, 0, 0, 0);
                w.playSound(loc, Sound.ENTITY_PHANTOM_BITE, 0.8f, 2.0f);
                p.getNearbyEntities(30, 30, 30).forEach(e -> {
                    if (e instanceof Player t && e != p) {
                        t.removePotionEffect(PotionEffectType.INVISIBILITY);
                        t.setGlowing(true);
                        w.spawnParticle(Particle.END_ROD, t.getLocation(), 25, 0.4, 0.9, 0.4, 0.04);
                        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (t.isOnline()) t.setGlowing(false); }, ticks(160, dr));
                    }
                });
                p.sendTitle("§b§lPHANTOM REVEAL", "§7All invisible players exposed!", 5, 50, 10);
            }

            case STORM -> {
    double qDmg = ecfg("STORM", "secondary-damage", 10.0);
    w.playSound(p.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 2f, 0.5f);
    
    new BukkitRunnable() {
        double radius = 1.0;
        public void run() {
            if (radius > 8.0 || !p.isOnline()) { cancel(); return; }

            // Blue Electric Ring Animation
            for (double a = 0; a < Math.PI * 2; a += Math.PI / (radius * 3)) {
                double x = Math.cos(a) * radius;
                double z = Math.sin(a) * radius;
                Location ring = p.getLocation().clone().add(x, 0.2, z);
                w.spawnParticle(Particle.ELECTRIC_SPARK, ring, 1, 0, 0, 0, 0);
                if (radius > 4) w.spawnParticle(Particle.CLOUD, ring, 1, 0, 0, 0, 0.02);
            }

            // Damage aur Knockback (Owner is Safe)
            applySafeDamage(p, p.getLocation(), radius, qDmg);
            
            p.getWorld().getNearbyEntities(p.getLocation(), radius, 2, radius).forEach(e -> {
                if (e instanceof LivingEntity le && e != p) {
                    Vector push = le.getLocation().toVector().subtract(p.getLocation().toVector()).normalize();
                    le.setVelocity(push.multiply(1.8).setY(0.5));
                }
            });

            radius += 1.5;
        }
    }.runTaskTimer(plugin, 0, 1);
            }  // end case STORM


// ════════════════════════════════════════════════════════════════════
case FROST -> {
    w.spawnParticle(Particle.SNOWFLAKE, loc, 100, 2.5, 0.5, 2.5, 0.1);
    w.spawnParticle(Particle.WHITE_ASH, loc,  60, 2.0, 0.3, 2.0, 0.05);
    w.playSound(loc, Sound.ENTITY_PLAYER_HURT_FREEZE, 1f, 0.3f);
    w.playSound(loc, Sound.BLOCK_GLASS_BREAK,         1f, 0.4f);

    List<Location> placedBlocks = new ArrayList<>();

    // 4 sides: North, South, East, West
    // direction = spike points outward this way
    // perp = spread along this side
    double[][] dirs  = {{ 1, 0},{ -1, 0},{ 0, 1},{ 0,-1}};
    double[][] perps = {{ 0, 1},{  0,-1},{ 1, 0},{-1, 0}};

    double angleRad = Math.toRadians(40); // 40 degrees from ground
    double cosA = Math.cos(angleRad);     // horizontal component
    double sinA = Math.sin(angleRad);     // vertical component

    // Spawn spikes with slight delay per side for cinematic effect
    for (int d = 0; d < 4; d++) {
        final int fd = d;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            double dx = dirs[fd][0],  dz = dirs[fd][1];
            double px = perps[fd][0], pz = perps[fd][1];

            // 5 spikes per side, spread 3 blocks apart (-6,-3,0,3,6)
            for (int s = 0; s < 5; s++) {
                double spread = (s - 2) * 3.0;

                // Base of spike — 10 blocks from center
                double baseX = loc.getX() + dx * 10 + px * spread;
                double baseY = loc.getY();
                double baseZ = loc.getZ() + dz * 10 + pz * spread;

                // Spike height: 25 blocks along axis, tapering
                int spikeLen = 18 + (s == 2 ? 7 : (s == 1 || s == 3 ? 4 : 0)); // center tallest
                for (int h = 0; h < spikeLen; h++) {
                    // Position along 40° spike
                    double outward = h * cosA; // outward offset along ground plane
                    double upward  = h * sinA; // upward offset

                    // Width tapers: base=2, mid=1, tip=0
                    int halfW = h < spikeLen/3 ? 1 : 0;

                    for (int ww = -halfW; ww <= halfW; ww++) {
                        int bx = (int) Math.round(baseX + dx*outward + px*ww);
                        int by = (int) Math.round(baseY + upward);
                        int bz = (int) Math.round(baseZ + dz*outward + pz*ww);

                        Location bl = new Location(w, bx, by, bz);
                        org.bukkit.block.Block blk = bl.getBlock();
                        Material mat = blk.getType();

                        // Only place in air/non-solid — no griefing
                        if (mat == Material.AIR
                                || mat == Material.CAVE_AIR
                                || mat == Material.VOID_AIR) {
                            // Alternate BLUE_ICE and PACKED_ICE for crystal look
                            blk.setType(h % 3 == 0 ? Material.BLUE_ICE : Material.PACKED_ICE);
                            synchronized (placedBlocks) { placedBlocks.add(bl); }
                        }
                    }
                }

                // Ice spawn particles at base
                Location base = new Location(w, baseX, baseY, baseZ);
                w.spawnParticle(Particle.SNOWFLAKE, base, 20, 0.3, 0.2, 0.3, 0.04);
                w.spawnParticle(Particle.WHITE_ASH, base, 10, 0.2, 0.3, 0.2, 0.02);
            }

            // Sound per side
            w.playSound(new Location(w,
                loc.getX() + dx*10,
                loc.getY(),
                loc.getZ() + dz*10),
                Sound.BLOCK_GLASS_BREAK, 1f, 0.5f);
            w.playSound(new Location(w,
                loc.getX() + dx*10,
                loc.getY(),
                loc.getZ() + dz*10),
                Sound.ENTITY_PLAYER_HURT_FREEZE, 1f, 0.6f);

        }, fd * 3L); // each side 3 ticks apart — wave effect
    }

    // Center circle effect
    for (int ring = 1; ring <= 4; ring++) { final int fr = ring;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            double r = fr * 2.5;
            for (int i = 0; i < 24; i++) {
                double a = Math.toRadians(i * 15);
                w.spawnParticle(Particle.SNOWFLAKE,
                    loc.clone().add(Math.cos(a)*r, 0.1, Math.sin(a)*r),
                    2, 0, 0, 0, 0.02);
            }
        }, ring * 3L);
    }

    // ── DAMAGE TASK — 6 seconds, enemies inside freeze ───────────────
    new BukkitRunnable() {
        int elapsed = 0;
        public void run() {
            if (elapsed >= 6) { cancel(); return; }
            elapsed++;

            // All enemies within 12 block radius (covers all spikes)
            p.getNearbyEntities(12, 12, 12).forEach(e -> {
                if (!(e instanceof LivingEntity le) || e == p) return;
                le.addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOWNESS, 25, 5, false, false));
                le.damage(1.5 * dm, p);
                // Ice particle on enemy
                w.spawnParticle(Particle.SNOWFLAKE, le.getLocation(),
                    8, 0.3, 0.6, 0.3, 0.02);
            });
        }
    }.runTaskTimer(plugin, 20L, 20L); // every second for 6s

    // ── REMOVE BLOCKS after 6 seconds ────────────────────────────────
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
        // Break all placed blocks
        for (Location bl : placedBlocks) {
            if (bl.getBlock().getType() == Material.BLUE_ICE
                    || bl.getBlock().getType() == Material.PACKED_ICE) {
                bl.getBlock().setType(Material.AIR);
                // Particle at each block removal
                w.spawnParticle(Particle.SNOWFLAKE, bl, 3, 0.2, 0.2, 0.2, 0.02);
            }
        }
        placedBlocks.clear();

        // Break sounds — 4 sides
        double[][] dirsF = {{1,0},{-1,0},{0,1},{0,-1}};
        for (double[] df : dirsF) {
            Location sLoc = loc.clone().add(df[0]*10, 0, df[1]*10);
            w.playSound(sLoc, Sound.BLOCK_GLASS_BREAK,           1.5f, 0.6f);
            w.playSound(sLoc, Sound.ENTITY_PLAYER_HURT_FREEZE,   1f,   1.5f);
            w.spawnParticle(Particle.SNOWFLAKE, sLoc, 40, 1, 1, 1, 0.06);
            w.spawnParticle(Particle.WHITE_ASH, sLoc, 30, 1, 1, 1, 0.04);
        }
    }, 120L); // 120 ticks = 6 seconds
}

            // 5. FLAME — Flame Burst  [NO createExplosion — prevents owner self-damage]
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
                p.getNearbyEntities(7, 7, 7).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        Vector vel = e.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(2.5).setY(0.8);
                        le.damage(8.0 * dm, p);
                        le.setFireTicks(ticks(140, dr));
                        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (e.isValid()) e.setVelocity(vel); }, 1L);
                    }
                });
            }

            // 6. SHADOW — Shadow Cloak
            case SHADOW -> {
    p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, ticks(200, dr), 0));
    p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION,  ticks(300, dr), 0));
    p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,         ticks(160, dr), 1));

    w.spawnParticle(Particle.SMOKE, loc, 80, 0.5, 1.2, 0.5, 0.09);
    w.spawnParticle(Particle.DUST,  loc, 60, 0.4, 0.9, 0.4, 0,
        new Particle.DustOptions(Color.BLACK, 2f));
    w.spawnParticle(Particle.PORTAL, loc, 40, 0.3, 0.7, 0.3, 0.07);
    for (int i = 0; i < 20; i++) {
        double a = Math.toRadians(i * 18);
        w.spawnParticle(Particle.SMOKE,
            loc.clone().add(Math.cos(a)*0.8, i*0.1, Math.sin(a)*0.8),
            3, 0, 0, 0, 0.02);
    }
    w.playSound(loc, Sound.ENTITY_ENDERMAN_STARE, 0.6f, 1.3f);
    p.sendTitle("\u00a78\u00a7lSHADOW CLOAK", "\u00a77You vanish into darkness...", 5, 50, 10);

    org.bukkit.inventory.PlayerInventory inv = p.getInventory();

    // ── Step 1: Store armor + offhand with their EXACT slots ─────────
    final ItemStack h = inv.getHelmet()     != null ? inv.getHelmet().clone()     : null;
    final ItemStack c = inv.getChestplate() != null ? inv.getChestplate().clone() : null;
    final ItemStack l = inv.getLeggings()   != null ? inv.getLeggings().clone()   : null;
    final ItemStack b = inv.getBoots()      != null ? inv.getBoots().clone()      : null;
    final ItemStack oh = inv.getItemInOffHand().getType() != Material.AIR
                       ? inv.getItemInOffHand().clone() : null;

    // ── Step 2: Remove armor + offhand ───────────────────────────────
    inv.setHelmet(null);
    inv.setChestplate(null);
    inv.setLeggings(null);
    inv.setBoots(null);
    inv.setItemInOffHand(null);

    // ── Step 3: 10s baad wapas do — slot-aware ───────────────────────
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
        if (!p.isOnline()) return;

        org.bukkit.inventory.PlayerInventory i2 = p.getInventory();

        // Armor slots return karo — agar kuch aur aa gaya toh drop karo
        restoreOrDrop(p, h,  () -> i2.setHelmet(h),     i2.getHelmet());
        restoreOrDrop(p, c,  () -> i2.setChestplate(c), i2.getChestplate());
        restoreOrDrop(p, l,  () -> i2.setLeggings(l),   i2.getLeggings());
        restoreOrDrop(p, b,  () -> i2.setBoots(b),      i2.getBoots());
        restoreOrDrop(p, oh, () -> i2.setItemInOffHand(oh), i2.getItemInOffHand());

        // Un-cloak effect
        w.spawnParticle(Particle.SMOKE,  p.getLocation(), 60, 0.5, 1, 0.5, 0.08);
        w.spawnParticle(Particle.PORTAL, p.getLocation(), 30, 0.3, 0.7, 0.3, 0.06);
        w.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.8f);
        p.sendTitle("\u00a77Shadow Cloak", "\u00a78Faded...", 5, 30, 10);

    }, ticks(200, dr));
}

            // 7. TITAN — Titan Strength
            case TITAN -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH,   ticks(120, dr), 2));
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,  ticks(60, dr), 1));
                w.spawnParticle(Particle.DUST, loc, 80, 0.7, 1.4, 0.7, 0, new Particle.DustOptions(Color.fromRGB(180,90,20), 3f));
                w.spawnParticle(Particle.CRIT, loc, 40, 0.5, 1.0, 0.5, 0.12);
                w.spawnParticle(Particle.EXPLOSION, loc, 2, 0.4, 0, 0.4, 0);
                for (int ring = 1; ring <= 3; ring++) { final int fr = ring;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        for (int i = 0; i < 16; i++) { double a = Math.toRadians(i*22.5);
                            w.spawnParticle(Particle.DUST, loc.clone().add(Math.cos(a)*(fr*0.8),1,Math.sin(a)*(fr*0.8)), 3, 0, 0.5, 0, 0,
                                new Particle.DustOptions(Color.fromRGB(180,90,20), 2f));
                        }
                    }, fr * 3L);
                }
                w.playSound(loc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 0.6f);
                w.playSound(loc, Sound.ENTITY_RAVAGER_ROAR,      0.8f, 0.8f);
                p.sendTitle("§6§l💪 TITAN STRENGTH!", "§7+Strength III active!", 5, 45, 10);
            }

            
// ── HUNTER SECONDARY — Mark Target ───────────────────────────────────────────
// Target mark hoga — 10s tak extra damage milega
// Mob aur player dono par kaam karega
case HUNTER -> {
    LivingEntity tgt = aim(p, 30);
    if (tgt != null) {

        // Mark particles + sound
        w.spawnParticle(Particle.CRIT,     tgt.getLocation(), 50, 0.5, 1.0, 0.5, 0.12);
        w.spawnParticle(Particle.END_ROD,  tgt.getLocation(), 25, 0.4, 0.8, 0.4, 0.06);
        w.spawnParticle(Particle.SOUL,     tgt.getLocation(), 20, 0.3, 0.7, 0.3, 0.05);

        // Red ring animation
        for (int ring = 1; ring <= 3; ring++) { final int fr = ring;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                double r = fr * 0.6;
                for (int i = 0; i < 12; i++) {
                    double a = Math.toRadians(i * 30);
                    Particle.DustOptions red = new Particle.DustOptions(
                        org.bukkit.Color.fromRGB(220, 20, 20), 1.5f);
                    w.spawnParticle(Particle.DUST,
                        tgt.getLocation().clone().add(Math.cos(a)*r, 1.0, Math.sin(a)*r),
                        1, 0, 0, 0, 0, red);
                }
            }, ring * 3L);
        }

        w.playSound(loc, Sound.ENTITY_ARROW_HIT_PLAYER,   1f, 0.8f);
        w.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1.2f);

        // Mark metadata
        tgt.setMetadata("hunterMarked",
            new org.bukkit.metadata.FixedMetadataValue(plugin, p.getUniqueId().toString()));
        tgt.setGlowing(true);

        p.sendTitle("§c§lMARKED!", "§7Target takes extra damage!", 5, 50, 10);

        // Mark aura — 10 seconds tak
        new BukkitRunnable() {
            int ticks = 0;
            double angle = 0;
            public void run() {
                if (ticks++ >= ticks(200, dr) || !tgt.isValid() || tgt.isDead()) {
                    // Remove mark
                    tgt.removeMetadata("hunterMarked", plugin);
                    if (tgt.isValid()) tgt.setGlowing(false);
                    cancel(); return;
                }

                angle += 0.15;

                // Rotating red ring around target
                for (int i = 0; i < 8; i++) {
                    double a = angle + Math.toRadians(i * 45);
                    Particle.DustOptions red = new Particle.DustOptions(
                        org.bukkit.Color.fromRGB(220, 20, 20), 1.2f);
                    w.spawnParticle(Particle.DUST,
                        tgt.getLocation().clone().add(Math.cos(a)*0.7, 1.0, Math.sin(a)*0.7),
                        1, 0, 0, 0, 0, red);
                }
                // Top indicator
                w.spawnParticle(Particle.CRIT,
                    tgt.getLocation().clone().add(0, tgt.getHeight() + 0.3, 0),
                    1, 0.1, 0, 0.1, 0.02);
            }
        }.runTaskTimer(plugin, 0, 1);

    } else {
        p.sendMessage("§7No target in range.");
    }
}

            // 9. GRAVITY — Gravity Jump
            // [FIX] Y velocity = 8.0 (was 3.5), slow fall after 40 ticks, lasts 80 ticks
            case GRAVITY -> {
                w.spawnParticle(Particle.REVERSE_PORTAL, loc, 80, 0.8, 0.5, 0.8, 0.12);
                w.spawnParticle(Particle.PORTAL,         loc, 50, 0.5, 0.3, 0.5, 0.08);
                w.spawnParticle(Particle.EXPLOSION,      loc,  2, 0,   0,   0,   0);
                for (int i = 0; i < 16; i++) { double a = Math.toRadians(i*22.5);
                    w.spawnParticle(Particle.REVERSE_PORTAL, loc.clone().add(Math.cos(a)*1.5,0.1,Math.sin(a)*1.5), 3, 0, 0.3, 0, 0.05);
                }
                w.playSound(loc, Sound.ENTITY_GHAST_SHOOT,    0.9f, 0.4f);
                w.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 0.6f);
                p.setVelocity(new Vector(0, 8.0, 0));   // HIGH launch
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (p.isOnline())
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, ticks(80, dr), 0));
                }, 40L);  // wait until near peak before slow fall
            }

            // 10. WIND — Wind Push  [knockback 1 tick after damage]
            case WIND -> {
                w.spawnParticle(Particle.WHITE_ASH, loc, 120, 2.5, 1, 2.5, 0.22);
                w.spawnParticle(Particle.CLOUD,     loc,  70, 2.0, 0.6, 2.0, 0.10);
                w.spawnParticle(Particle.EXPLOSION, loc,   2, 0.5, 0, 0.5, 0);
                for (int ring = 1; ring <= 4; ring++) { final int fr = ring;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        for (int i = 0; i < 20; i++) { double a = Math.toRadians(i*18);
                            w.spawnParticle(Particle.WHITE_ASH, loc.clone().add(Math.cos(a)*(fr*2.5),0.5,Math.sin(a)*(fr*2.5)), 4, 0, 0.3, 0, 0.06);
                        }
                    }, fr * 3L);
                }
                w.playSound(loc, Sound.ENTITY_PHANTOM_DEATH,  1f, 0.4f);
                w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE,0.7f, 0.8f);
                p.getNearbyEntities(12, 12, 12).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        Vector vel = e.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(3.5).setY(0.7);
                        le.damage(3.0 * dm, p);
                        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (e.isValid()) e.setVelocity(vel); }, 1L);
                    }
                });
            }

            // 11. POISON — Poison Cloud
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
                        le.addPotionEffect(new PotionEffect(PotionEffectType.POISON,   ticks(200, dr), 2));
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks(100, dr), 1));
                        le.addPotionEffect(new PotionEffect(PotionEffectType.WITHER,   ticks(60,  dr), 0));
                        le.damage(3.0 * dm, p);
                    }
                });
            }

            //══════════════════════════════════════════════════════════════════
// LIGHT — SHIFT + Q (Light Beam) FIXED
case LIGHT -> {
    w.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 2.0f);
    w.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE,        1f,   2.0f);

    final Vector   beamDir   = p.getEyeLocation().getDirection().normalize();
    // FIX: beam 1.5 block aage se shuru ho — player body miss karega
    final Location beamStart = p.getEyeLocation().clone()
                                .add(beamDir.clone().multiply(1.5));

    // Origin burst
    w.spawnParticle(Particle.FLASH,   p.getEyeLocation(), 1, 0, 0, 0, 0);
    w.spawnParticle(Particle.END_ROD, p.getEyeLocation(), 20, 0.1, 0.1, 0.1, 0.05);

    new BukkitRunnable() {
        final Location beamPos = beamStart.clone();
        int     traveled = 0;
        boolean hit      = false;

        @Override
        public void run() {
            if (hit || traveled >= 80) { // 80 * 0.5 = 40 blocks max
                // Beam end burst
                w.spawnParticle(Particle.FLASH,   beamPos, 1, 0, 0, 0, 0);
                w.spawnParticle(Particle.END_ROD, beamPos, 15, 0.4, 0.4, 0.4, 0.08);
                cancel();
                return;
            }

            // 0.5 block per tick — smooth
            beamPos.add(beamDir.clone().multiply(0.5));
            traveled++;

            // ── Beam particles — white laser ──────────────────────
            w.spawnParticle(Particle.END_ROD,   beamPos, 4, 0.02, 0.02, 0.02, 0.0);
            w.spawnParticle(Particle.WHITE_ASH, beamPos, 2, 0.04, 0.04, 0.04, 0.01);

            // FLASH pulse har 4th step — laser flash feel
            if (traveled % 4 == 0) {
                w.spawnParticle(Particle.FLASH, beamPos, 1, 0, 0, 0, 0);
            }

            // Outer glow — beam ke around glow effect
            for (int i = 0; i < 3; i++) {
                double a = Math.toRadians(i * 120 + traveled * 15);
                Location glowPt = beamPos.clone().add(
                    Math.cos(a) * 0.12, Math.sin(a) * 0.12, 0);
                w.spawnParticle(Particle.END_ROD, glowPt, 1, 0, 0, 0, 0.0);
            }

            // ── Block hit ─────────────────────────────────────────
            if (beamPos.getBlock().getType().isSolid()) {
                w.spawnParticle(Particle.FLASH,   beamPos, 1, 0, 0, 0, 0);
                w.spawnParticle(Particle.END_ROD, beamPos, 25, 0.5, 0.5, 0.5, 0.1);
                w.spawnParticle(Particle.WHITE_ASH, beamPos, 20, 0.4, 0.4, 0.4, 0.06);
                w.playSound(beamPos, Sound.BLOCK_GLASS_BREAK,
                    1f, 2.0f);
                hit = true;
                return;
            }

            // ── Entity hit ────────────────────────────────────────
            for (org.bukkit.entity.Entity e :
                    w.getNearbyEntities(beamPos, 0.7, 0.7, 0.7)) {
                if (!(e instanceof LivingEntity le) || e == p) continue;

                // 4 hearts = 8 damage
                le.damage(8.0 * dm, p);

                // Big hit burst
                w.spawnParticle(Particle.FLASH,
                    le.getLocation().add(0,1,0), 1, 0, 0, 0, 0);
                w.spawnParticle(Particle.END_ROD,
                    le.getLocation().add(0,1,0), 40, 0.6, 1.0, 0.6, 0.09);
                w.spawnParticle(Particle.WHITE_ASH,
                    le.getLocation().add(0,1,0), 30, 0.5, 0.8, 0.5, 0.07);
                w.playSound(beamPos, Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 2.0f);
                w.playSound(beamPos, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.6f, 2.0f);

                // White screen — sirf players pe
                if (le instanceof Player ep && ep.isOnline()) {
                    whiteLightScreen(ep, ticks(40, dr), plugin);
                }

                hit = true;
                return;
            }
        }
    }.runTaskTimer(plugin, 0, 1);
}


            // 13. EARTH — Earth Slam  [knockback 1 tick after damage]
            case EARTH -> {
                w.spawnParticle(Particle.DUST_PLUME, loc, 160, 3, 0.2, 3, 0.26, Material.DIRT.createBlockData());
                w.spawnParticle(Particle.DUST_PLUME, loc, 100, 3, 0.2, 3, 0.26, Material.STONE.createBlockData());
                w.spawnParticle(Particle.EXPLOSION,  loc,   4, 0.9, 0, 0.9, 0);
                for (int ring = 1; ring <= 5; ring++) { final int fr = ring;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        double r = fr * 1.9;
                        for (int i = 0; i < 24; i++) { double a = Math.toRadians(i*15);
                            w.spawnParticle(Particle.DUST_PLUME, loc.clone().add(Math.cos(a)*r,0.2,Math.sin(a)*r),
                                4, 0, 0.6, 0, 0.14, Material.STONE.createBlockData());
                        }
                    }, ring * 3L);
                }
                w.playSound(loc, Sound.BLOCK_STONE_BREAK,        1f, 0.3f);
                w.playSound(loc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 0.4f);
                w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE,   0.8f, 0.5f);
                p.getNearbyEntities(10, 10, 10).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        Vector vel = e.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(2.8).setY(0.9);
                        le.damage(6.0 * dm, p);
                        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (e.isValid()) e.setVelocity(vel); }, 1L);
                    }
                });
            }

            // 14. CRYSTAL — Crystal Spikes
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
                        le.damage(8.0 * dm, p);
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks(70, dr), 2));
                    }
                });
            }
//═══════════════════════════════════════════════════════════════════
case ECHO -> {
    // Check aim — agar koi target aim mein hai toh focused blast
    LivingEntity tgt = aim(p, 25);

    if (tgt != null) {
        // ── FOCUSED BLAST — aim par direct sonic boom ─────────────
        w.spawnParticle(Particle.SONIC_BOOM, tgt.getLocation(), 3, 0, 0, 0, 0);
        w.spawnParticle(Particle.END_ROD,    tgt.getLocation(), 70, 1, 1.5, 1, 0.1);
        w.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1f, 0.4f);
        w.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM,    1f, 1.0f);

        // Beam line from owner to target — sonic wave path
        Location beam = p.getEyeLocation().clone();
        Vector   step = tgt.getLocation().toVector()
                .subtract(p.getEyeLocation().toVector()).normalize().multiply(0.6);
        double   dist = p.getEyeLocation().distance(tgt.getLocation());
        int      steps = (int)(dist / 0.6);
        for (int i = 0; i < steps; i++) {
            beam.add(step);
            w.spawnParticle(Particle.END_ROD,   beam, 2, 0.04, 0.04, 0.04, 0);
            w.spawnParticle(Particle.SONIC_BOOM, beam, 1, 0, 0, 0, 0);
            if (i % 3 == 0)
                w.spawnParticle(Particle.WHITE_ASH, beam, 2, 0.05, 0.05, 0.05, 0.02);
        }

        // Damage + knockback
        Vector vel = tgt.getLocation().toVector()
                .subtract(loc.toVector()).normalize().multiply(4.0).setY(1.0);
        tgt.damage(9.0 * dm, p);
        tgt.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks(80, dr), 2));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (tgt.isValid()) tgt.setVelocity(vel);
        }, 1L);

    } else {
        // ── AOE SHOCKWAVE — koi aim nahi, circle mein blast ───────
        w.spawnParticle(Particle.SONIC_BOOM, loc, 4, 0, 0, 0, 0);
        w.spawnParticle(Particle.END_ROD,    loc, 90, 4, 4, 4, 0.08);
        w.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 1f, 0.3f);
        w.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM,      1f, 0.7f);

        // 5 expanding shockwave rings — high animation
        for (int ring = 1; ring <= 5; ring++) {
            final int fr = ring;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                double r   = fr * 2.2;
                int    pts = 36;
                for (int i = 0; i < pts; i++) {
                    double a = Math.toRadians(i * (360.0 / pts));

                    // Ground ring
                    w.spawnParticle(Particle.END_ROD,
                        loc.clone().add(Math.cos(a)*r, 0.15, Math.sin(a)*r),
                        1, 0, 0, 0, 0);
                    // Mid ring
                    w.spawnParticle(Particle.SONIC_BOOM,
                        loc.clone().add(Math.cos(a)*r, 1.0,  Math.sin(a)*r),
                        1, 0, 0, 0, 0);
                    // Top ring
                    w.spawnParticle(Particle.END_ROD,
                        loc.clone().add(Math.cos(a)*r, 2.0,  Math.sin(a)*r),
                        1, 0, 0, 0, 0);
                }
                // Sound at each ring expansion
                w.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.6f,
                    0.3f + fr * 0.15f);
            }, fr * 3L);
        }

        // Damage + knockback sab nearby enemies
        p.getNearbyEntities(12, 12, 12).forEach(e -> {
            if (!(e instanceof LivingEntity le) || e == p) return;
            Vector vel = e.getLocation().toVector()
                    .subtract(loc.toVector()).normalize().multiply(2.5).setY(0.5);
            le.damage(5.0 * dm, p);
            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks(60, dr), 1));
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (e.isValid()) e.setVelocity(vel);
            }, 1L);
            w.spawnParticle(Particle.SONIC_BOOM, le.getLocation(), 1, 0, 0, 0, 0);
        });
    }
}

            // 16. RAGE — Rage Smash  [knockback 1 tick after damage]
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
                    }, fr * 3L);
                }
                w.playSound(loc, Sound.ENTITY_RAVAGER_ROAR,   1f, 0.7f);
                w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE,0.9f, 0.8f);
                p.getNearbyEntities(8, 8, 8).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        Vector vel = e.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(3.8).setY(1.2);
                        le.damage(9.0 * dm, p);
                        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (e.isValid()) e.setVelocity(vel); }, 1L);
                    }
                });
            }

            // Total 5s: 3s rise + 2s shoot
case SPIRIT -> {
    p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, ticks(100, dr), 2));
    p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,       ticks(80,  dr), 1));

    w.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT,     0.9f, 1.4f);
    w.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.8f, 1.2f);
    p.sendTitle("\u00a77\u00a7lSPIRIT FORM", "\u00a78Blocks rising...", 5, 60, 10);

    // 4 sides — exactly 2 blocks from player
    int[][] offsets = {{0,0,2},{0,0,-2},{2,0,0},{-2,0,0}};

    for (int[] off : offsets) {
        Location sideLoc = loc.clone().add(off[0], 0, off[2]);

        // Surface block dhundo
        org.bukkit.block.Block surface = sideLoc.getBlock();
        for (int gy = 0; gy > -10; gy--) {
            org.bukkit.block.Block check = sideLoc.clone().add(0, gy, 0).getBlock();
            if (check.getType().isSolid()) { surface = check; break; }
        }

        final org.bukkit.block.Block groundBlock = surface;
        final Material blockMat = groundBlock.getType().isSolid()
            ? groundBlock.getType() : Material.GRASS_BLOCK;
        final Location spawnLoc = groundBlock.getLocation().clone().add(0.5, 0.0, 0.5);

        // Remove ground block
        groundBlock.setType(Material.AIR);

        // FallingBlock spawn
        FallingBlock fb = w.spawnFallingBlock(spawnLoc, blockMat.createBlockData());
        fb.setDropItem(false);
        fb.setHurtEntities(false);
        fb.setGravity(false);

        w.spawnParticle(Particle.TOTEM_OF_UNDYING, spawnLoc, 20, 0.3, 0.3, 0.3, 0.06);
        w.spawnParticle(Particle.END_ROD,          spawnLoc, 10, 0.2, 0.2, 0.2, 0.03);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!fb.isValid()) { cancel(); return; }
                ticks++;

                if (ticks <= 60) {
                    // ── RISE 3s (60 ticks) ────────────────────────────────
                    fb.setVelocity(new Vector(0, 0.08, 0));
                    w.spawnParticle(Particle.TOTEM_OF_UNDYING,
                        fb.getLocation(), 3, 0.15, 0.1, 0.15, 0.03);
                    w.spawnParticle(Particle.WITCH,
                        fb.getLocation(), 1, 0.1,  0.1, 0.1,  0.02);
                    if (ticks == 1)
                        w.playSound(fb.getLocation(), Sound.BLOCK_STONE_BREAK, 0.8f, 0.5f);

                } else if (ticks == 61) {
                    // ── SHOOT START ───────────────────────────────────────
                    w.playSound(fb.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1f, 1.2f);
                    w.playSound(fb.getLocation(), Sound.ENTITY_BLAZE_SHOOT,  1f, 0.8f);
                    w.spawnParticle(Particle.TOTEM_OF_UNDYING,
                        fb.getLocation(), 30, 0.4, 0.2, 0.4, 0.08);
                    fb.setGravity(true);
                    fb.setVelocity(new Vector(0, 3.2, 0));

                } else if (ticks <= 100) {
                    // ── SHOOT 2s (61-100 ticks) ───────────────────────────
                    w.spawnParticle(Particle.TOTEM_OF_UNDYING,
                        fb.getLocation(), 5, 0.2, 0.1, 0.2, 0.05);
                    w.spawnParticle(Particle.END_ROD,
                        fb.getLocation(), 2, 0.1, 0.1, 0.1, 0.02);

                    // Damage — jo bhi block se touch kare
                    for (org.bukkit.entity.Entity e :
                            w.getNearbyEntities(fb.getLocation(), 1.5, 1.5, 1.5)) {
                        if (e instanceof LivingEntity le && e != p) {
                            le.damage(10.0 * dm, p); // 5 hearts
                            w.spawnParticle(Particle.TOTEM_OF_UNDYING,
                                le.getLocation(), 20, 0.4, 0.8, 0.4, 0.06);
                            Vector knock = le.getLocation().toVector()
                                .subtract(fb.getLocation().toVector())
                                .normalize().multiply(1.5).setY(0.8);
                            Bukkit.getScheduler().runTaskLater(plugin,
                                () -> { if (e.isValid()) e.setVelocity(knock); }, 1L);
                        }
                    }

                } else {
                    // ── END — block remove ────────────────────────────────
                    w.spawnParticle(Particle.FALLING_DUST,
                        fb.getLocation(), 20, 0.5, 0.3, 0.5, 0.1,
                        blockMat.createBlockData());
                    w.spawnParticle(Particle.TOTEM_OF_UNDYING,
                        fb.getLocation(), 15, 0.3, 0.3, 0.3, 0.05);
                    w.playSound(fb.getLocation(), Sound.BLOCK_STONE_BREAK, 1f, 0.8f);
                    fb.remove();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}

    // TIME — SHIFT + Q (Time Dash) FIXED
case TIME -> {
    final Vector dashDir = p.getEyeLocation().getDirection().normalize();

    // Origin burst
    w.spawnParticle(Particle.ELECTRIC_SPARK, loc, 60, 0.3, 0.5, 0.3, 0.15);
    w.spawnParticle(Particle.FLASH,          loc, 1,  0,   0,   0,   0);
    w.spawnParticle(Particle.REVERSE_PORTAL, loc, 30, 0.4, 0.6, 0.4, 0.1);
    w.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 2.0f);
    w.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_CURSE,   0.4f, 2.0f);

    // FIX: velocity 1 tick baad set karo — ground detection se cancel nahi hoga
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
        if (!p.isOnline()) return;
        // Y: agar player upar dekh raha hai toh upar bhi jaaye, warna flat dash
        double yVel = dashDir.getY() > 0.2 ? dashDir.getY() * 3.0 : 0.2;
        p.setVelocity(dashDir.clone().multiply(3.8).setY(yVel));
        p.setFallDistance(0f);
    }, 1L);

    // Trail animation
    new BukkitRunnable() {
        int    t          = 0;
        double trailAngle = 0;

        @Override
        public void run() {
            if (t++ >= 12) {
                Location endLoc = p.getLocation();
                w.spawnParticle(Particle.ELECTRIC_SPARK, endLoc, 50, 0.4, 0.6, 0.4, 0.12);
                w.spawnParticle(Particle.FLASH,          endLoc,  1, 0,   0,   0,   0);
                w.spawnParticle(Particle.END_ROD,        endLoc, 20, 0.3, 0.5, 0.3, 0.08);
                w.playSound(endLoc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.7f, 1.8f);
                p.setFallDistance(0f);
                cancel();
                return;
            }

            trailAngle += 0.6;
            Location cur = p.getLocation();

            w.spawnParticle(Particle.ELECTRIC_SPARK, cur, 8, 0.2, 0.3, 0.2, 0.08);

            if (t % 2 == 0)
                w.spawnParticle(Particle.FLASH, cur, 1, 0, 0, 0, 0);

            w.spawnParticle(Particle.REVERSE_PORTAL, cur, 5, 0.2, 0.4, 0.2, 0.06);

            for (int i = 0; i < 6; i++) {
                double a = trailAngle + Math.toRadians(i * 60);
                w.spawnParticle(Particle.ELECTRIC_SPARK,
                    cur.clone().add(Math.cos(a)*0.4, 0.5 + Math.sin(a)*0.3, Math.sin(a)*0.4),
                    1, 0, 0, 0, 0.04);
            }

            w.spawnParticle(Particle.END_ROD, cur, 3, 0.1, 0.2, 0.1, 0.02);

            p.setFallDistance(0f);

            // Velocity boost maintain — agar player slow ho jaaye
            if (t < 8) {
                Vector vel = p.getVelocity();
                if (vel.lengthSquared() < 1.2) {
                    double yMaintain = vel.getY();
                    p.setVelocity(dashDir.clone().multiply(2.5).setY(yMaintain));
                }
            }
        }
    }.runTaskTimer(plugin, 2L, 1L); // 2 tick delay — velocity set hone ke baad trail start
}
            // 19. WARRIOR — Warrior Charge
            case WARRIOR -> {
                p.setVelocity(dir.clone().multiply(3.0).setY(0.3));
                w.spawnParticle(Particle.CRIT,  loc, 60, 0.5, 0.8, 0.5, 0.12);
                w.spawnParticle(Particle.FLAME, loc, 30, 0.4, 0.6, 0.4, 0.08);
                w.playSound(loc, Sound.ENTITY_RAVAGER_ROAR, 1f, 1.0f);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    p.getNearbyEntities(3, 3, 3).forEach(e -> {
                        if (e instanceof LivingEntity le && e != p) {
                            Vector vel = dir.clone().multiply(2.5).setY(0.5);
                            le.damage(6.0 * dm, p);
                            Bukkit.getScheduler().runTaskLater(plugin, () -> { if (e.isValid()) e.setVelocity(vel); }, 1L);
                        }
                    });
                }, 8L);
            }

            // ═══ EVENT SECONDARY ══════════════════════════════════════════════

            // OCEAN SECONDARY
case OCEAN -> {
    w.playSound(p.getLocation(), Sound.ITEM_BUCKET_FILL, 2f, 0.5f);
    
    p.getWorld().getNearbyEntities(p.getLocation(), 6, 6, 6).forEach(e -> {
        if (e instanceof LivingEntity le && e != p) {
            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 4)); // Super Slow
            
            new BukkitRunnable() {
                int ticks = 0;
                public void run() {
                    if (ticks++ >= 80 || le.isDead()) { cancel(); return; } // 4 seconds prison
                    
                    // Sphere (Bubble) Animation Math around the enemy
                    for(double i = 0; i < Math.PI; i += Math.PI/6) {
                        for(double j = 0; j < 2*Math.PI; j += Math.PI/6) {
                            double x = 1.5 * Math.sin(i) * Math.cos(j);
                            double y = 1.5 * Math.sin(i) * Math.sin(j) + 1; // Center at body
                            double z = 1.5 * Math.cos(i);
                            w.spawnParticle(Particle.SPLASH, le.getLocation().clone().add(x,y,z), 1, 0,0,0,0);
                        }
                    }
                }
            }.runTaskTimer(plugin, 0, 3);
        }
    });
}


            // E4. ECLIPSE — Shadow Phase
            case ECLIPSE -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, ticks(200, dr), 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,   ticks(200, dr), 3));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,        ticks(200, dr), 2));
                p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, ticks(220, dr), 0));
                w.spawnParticle(Particle.PORTAL,        loc, 120, 0.9, 1.8, 0.9, 0.17);
                w.spawnParticle(Particle.DRAGON_BREATH, loc,  90, 0.7, 1.4, 0.7, 0.09);
                w.spawnParticle(Particle.DUST,          loc,  70, 0.6, 1.2, 0.6, 0, new Particle.DustOptions(Color.BLACK, 3.5f));
                for (int i = 0; i < 8; i++) { double a = Math.toRadians(i*45);
                    for (int h = 0; h <= 6; h++)
                        w.spawnParticle(Particle.PORTAL, loc.clone().add(Math.cos(a)*1.5,h*0.4,Math.sin(a)*1.5), 3, 0.1, 0, 0.1, 0.04);
                }
                w.playSound(loc, Sound.ENTITY_ENDERMAN_STARE, 0.8f, 0.6f);
                w.playSound(loc, Sound.ENTITY_WITHER_AMBIENT,  0.7f, 0.8f);
                p.sendTitle("§5§l🌑 SHADOW PHASE", "§8Untouchable in the dark...", 5, 70, 15);
            }

            // E5. GUARDIAN — Titan Shockwave  [knockback 1 tick after damage]
            case GUARDIAN -> {
                double eDmg = ecfg("guardian","secondary-damage", 15.0) * dm;
                int    eRad = (int) ecfg("guardian","secondary-radius", 20);
                w.spawnParticle(Particle.EXPLOSION,        loc,   8, 2,   0, 2, 0);
                w.spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 150, 3.5, 2.5, 3.5, 0.14);
                w.spawnParticle(Particle.END_ROD,          loc, 120, 3.5, 2.5, 3.5, 0.12);
                for (int wave = 1; wave <= 6; wave++) { final int fw = wave;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        double r = fw * eRad / 5.0;
                        for (int i = 0; i < 36; i++) { double a = Math.toRadians(i*10);
                            w.spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(Math.cos(a)*r,0.4,Math.sin(a)*r), 4, 0, 0.6, 0, 0.06);
                            w.spawnParticle(Particle.END_ROD,          loc.clone().add(Math.cos(a)*r,0.4,Math.sin(a)*r), 2, 0, 0.3, 0, 0.02);
                        }
                    }, wave * 4L);
                }
                w.playSound(loc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 0.2f);
                w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE,   1f, 0.3f);
                w.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE,    1f, 0.4f);
                p.getNearbyEntities(eRad, eRad, eRad).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        Vector vel = e.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(5.0).setY(1.8);
                        le.damage(eDmg, p);
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks(120, dr), 3));
                        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (e.isValid()) e.setVelocity(vel); }, 1L);
                        w.spawnParticle(Particle.TOTEM_OF_UNDYING, le.getLocation(), 25, 0.4, 0.7, 0.4, 0.05);
                    }
                });
                p.sendTitle("§6§l⚡ TITAN SHOCKWAVE!", "§7The ground shatters!", 5, 70, 15);
            }

            /// METEOR SECONDARY
case METEOR -> {
    double mDmg = ecfg("METEOR", "secondary-damage", 8.0);
    w.playSound(p.getLocation(), Sound.ENTITY_GHAST_WARN, 1f, 0.5f);
    
    for (int i = 0; i < 8; i++) { // 8 Meteors
        Location dropLoc = p.getLocation().clone().add((Math.random() * 14) - 7, 12, (Math.random() * 14) - 7);
        new BukkitRunnable() {
            Location current = dropLoc.clone();
            public void run() {
                if (current.getBlock().getType().isSolid()) { // Zameen par takraya
                    w.spawnParticle(Particle.EXPLOSION, current, 2);
                    w.playSound(current, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1f, 1f);
                    applySafeDamage(p, current, 4.0, mDmg);
                    cancel(); return;
                }
                // Falling animation
                w.spawnParticle(Particle.LAVA, current, 10, 0.3, 0.5, 0.3, 0.05);
                w.spawnParticle(Particle.FLAME, current, 15, 0.4, 0.4, 0.4, 0.1);
                current.subtract(0, 0.8, 0); // Tezi se neeche aana
            }
        }.runTaskTimer(plugin, i * 4, 1); // Ek ke baad ek girenge
    }
}
            // MIRAGE SECONDARY
            case MIRAGE -> {
                w.playSound(p.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.5f, 0.8f);
                w.playSound(p.getLocation(), Sound.BLOCK_CONDUIT_DEACTIVATE, 1.5f, 2.0f);
    
                // Epic Smoke Poof
                w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, p.getLocation(), 80, 1.5, 1.5, 1.5, 0.05);
                w.spawnParticle(Particle.SQUID_INK, p.getLocation(), 50, 1, 1, 1, 0.1);
    
                p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 120, 0)); // 6 secs
    p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 120, 2));        // Speed 3
            }  // end case MIRAGE

        }  // end switch (activateSecondary)
        p.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.5f);
    }  // end activateSecondary



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
            case SHADOW  -> { p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,         INF,0,false,false)); p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION,INF,0,false,false)); }
            case TITAN   -> p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,      INF, 1, false, false));
            case HUNTER  -> p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,           INF, 1, false, false));
            case GRAVITY -> p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST,      INF, 2, false, false));
            case WIND    -> p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,           INF, 1, false, false));
            case POISON  -> p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,    INF, 0, false, false));
            case LIGHT   -> p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION,    INF, 0, false, false));
            case EARTH   -> p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,      INF, 0, false, false));
            case CRYSTAL -> p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,      INF, 0, false, false));
            case ECHO    -> { p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION,  INF,0,false,false)); p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,INF,0,false,false)); }
            case RAGE    -> p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH,        INF, 0, false, false));
            case SPIRIT  -> p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,    INF, 1, false, false));
            case TIME    -> p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE,           INF, 1, false, false));
            case WARRIOR -> p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH,        INF, 1, false, false));
            case METEOR  -> { p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE,INF,0,false,false)); p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,INF,0,false,false)); }
            case MIRAGE  -> p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,           INF, 2, false, false));
            case OCEAN   -> { p.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING,INF,0,false,false)); p.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER,INF,0,false,false)); }
            case ECLIPSE -> { p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION,  INF,0,false,false)); p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE,INF,0,false,false)); }
            case GUARDIAN-> { p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,    INF,2,false,false)); p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION,INF,1,false,false)); }
        }
    }

            
        // ══════════════════════════════════════════════════════════════════════
    //  GUI  — XP auto-refreshes every second while the inventory is open
    // ══════════════════════════════════════════════════════════════════════
    public void openEyeGUI(Player p) {
        Inventory gui = Bukkit.createInventory(null, 54, "§8Your Ancient Eye Status");
        ItemStack border = pane(Material.PURPLE_STAINED_GLASS_PANE);
        ItemStack fill   = pane(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < 54; i++)
            gui.setItem(i, (i<9||i>=45||i%9==0||i%9==8) ? border : fill);
        gui.setItem(22, buildEyeItem(p));
        p.openInventory(gui);
        p.playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1.2f);

        new BukkitRunnable() {
            public void run() {
                if (!p.isOnline()) { cancel(); return; }
                Inventory top = p.getOpenInventory().getTopInventory();
                if (top == null || !p.getOpenInventory().getTitle().equals("§8Your Ancient Eye Status")) {
                    cancel(); return;
                }
                top.setItem(22, buildEyeItem(p));
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    // Builds the center eye item with live kill/level data
    private ItemStack buildEyeItem(Player p) {
        EyeType type  = plugin.getPlayerData().getEye(p);
        int     level = plugin.getPlayerData().getLevel(p);
        int     kills = plugin.getPlayerData().getXP(p);
        // FIX: config se kill thresholds
        int     maxKills = plugin.getPlayerData().getMaxXPForLevel(level);
        String  bar      = progressBar(kills, maxKills);
        String  col      = level==3?"§e§l":level==2?"§b§l":"§7§l";

        ItemStack eye = new ItemStack(Material.ENDER_EYE);
        ItemMeta  m   = eye.getItemMeta();
        m.setDisplayName(col + type.name().replace("_"," ") + " EYE");
        List<String> lore = new ArrayList<>();
        lore.add("§8━━━━━━━━━━━━━━━━━━");
        lore.add("§7Level    §f"+level+" §8/ §f3");
        // FIX: XP ki jagah Kills dikhao
        lore.add("§7Kills: "+bar+" §8(§c"+kills+"§8/§f"+maxKills+"§8)");
        lore.add("§8━━━━━━━━━━━━━━━━━━");
        lore.add("§6§lStats:");
        lore.add("§e- §7Damage Bonus:  §c+"+(int)((getDmg(p)-1)*100)+"%");
        lore.add("§e- §7Duration Mul:  §b"+(int)(getDur(p)*100)+"% of base");
        lore.add("§e- §7Cooldown Red:  §a-"+((level-1)*2)+"s per ability");
        lore.add("§8━━━━━━━━━━━━━━━━━━");
        lore.add("§b§lAbilities:");
        lore.add("§f  SHIFT+F  §7->  §bPrimary");
        lore.add("§f  SHIFT+Q  §7->  §bSecondary");
        lore.add("§8━━━━━━━━━━━━━━━━━━");
        lore.add("§5§oThe power is bound to your soul.");
        m.setLore(lore);
        eye.setItemMeta(m);
        return eye;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════════════

    private double getDmg(Player p) {
        int l = plugin.getPlayerData().getLevel(p);
        return l == 3 ? 1.5 : l == 2 ? 1.2 : 1.0;
    }

    private double getDur(Player p) {
        int l = plugin.getPlayerData().getLevel(p);
        return l == 3 ? 2.0 : l == 2 ? 1.5 : 1.0;
    }

    private int ticks(int base, double durMul) {
        return (int)(base * durMul);
    }

    private int getCd(Player p, EyeType eye, String type) {
        String key = "cooldowns." + eye.name().toLowerCase() + "." + type;
        int base = plugin.getConfig().getInt(key, 12);
        int lvl  = plugin.getPlayerData().getLevel(p);
        int reduction = (lvl - 1) * 2;
        return Math.max(2, base - reduction);
    }

    private void doMeteorStrike(Player p, World w, Location pos, double dmg, int rad) {
        w.strikeLightningEffect(pos);
        w.spawnParticle(Particle.EXPLOSION,  pos, 4, 1.2, 0, 1.2, 0);
        w.spawnParticle(Particle.FLAME,      pos, 100, rad*0.35, 0.3, rad*0.35, 0.18);
        w.spawnParticle(Particle.LAVA,       pos,  30, rad*0.25, 0.2, rad*0.25, 0.12);
        w.spawnParticle(Particle.DUST_PLUME, pos,  70, rad*0.25, 0.2, rad*0.25, 0.20, Material.MAGMA_BLOCK.createBlockData());
        for (int i = 0; i < 24; i++) {
            double a = Math.toRadians(i * 15);
            w.spawnParticle(Particle.FLAME, pos.clone().add(Math.cos(a)*rad,0.2,Math.sin(a)*rad), 4, 0, 0.4, 0, 0.06);
        }
        w.playSound(pos, Sound.ENTITY_GENERIC_EXPLODE,        1f, 0.4f);
        w.playSound(pos, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 0.6f);
        pos.getWorld().getNearbyEntities(pos, rad, rad, rad).forEach(e -> {
            if (e instanceof LivingEntity le && e != p) {
                Vector vel = e.getLocation().toVector().subtract(pos.toVector()).normalize().multiply(3.2).setY(1.8);
                le.damage(dmg, p);
                le.setFireTicks(100);
                Bukkit.getScheduler().runTaskLater(plugin, () -> { if (e.isValid()) e.setVelocity(vel); }, 1L);
            }
        });
    }

    private Location safe(Location l) {
        if (l.getBlock().getType().isSolid()) l.add(0,1,0);
        if (l.getBlock().getType().isSolid()) l.add(0,1,0);
        return l;
    }

    private LivingEntity aim(Player p, double range) {
        RayTraceResult r = p.getWorld().rayTraceEntities(p.getEyeLocation(), p.getEyeLocation().getDirection(), range, 1.5, e -> e != p);
        return (r != null && r.getHitEntity() instanceof LivingEntity le) ? le : null;
    }

    private void crystalAura(Player p) {
        World w = p.getWorld(); Location l = p.getLocation();
        Particle.DustOptions d = new Particle.DustOptions(Color.fromRGB(100,220,255), 2f);
        for (double a = 0; a < Math.PI*2; a += Math.PI/10) {
            double rx = Math.cos(a)*2, rz = Math.sin(a)*2;
            w.spawnParticle(Particle.DUST, l.clone().add(rx,1,rz), 3, 0, 0.4, 0, 0, d);
            w.spawnParticle(Particle.END_ROD, l.clone().add(rx,1,rz), 2, 0, 0.3, 0, 0.01);
        }
        w.spawnParticle(Particle.END_ROD, l, 40, 0.5, 1.2, 0.5, 0.04);
    }

    private void buildWall(Player p) {
        Block b = p.getTargetBlockExact(5); if (b == null) return;
        for (int y = 1; y <= 3; y++) b.getLocation().add(0,y,0).getBlock().setType(Material.COBBLESTONE);
    }

    private void voidRift(World w, Location l) {
        w.spawnParticle(Particle.PORTAL,         l, 70, 0.6, 0.9, 0.6, 0.28);
        w.spawnParticle(Particle.REVERSE_PORTAL, l, 45, 0.4, 0.7, 0.4, 0.16);
        w.spawnParticle(Particle.DRAGON_BREATH,  l, 30, 0.3, 0.5, 0.3, 0.06);
        for (int i = 0; i < 14; i++) { double a = Math.toRadians(i*(360.0/14));
            w.spawnParticle(Particle.PORTAL, l.clone().add(Math.cos(a)*1.4,0.5,Math.sin(a)*1.4), 3, 0, 0, 0, 0.09);
        }
    }

    private double ecfg(String eye, String key, double def) {
        return plugin.getConfig().getDouble("event-eyes."+eye+"."+key, def);
    }

    private ItemStack pane(Material mat) {
        ItemStack i = new ItemStack(mat);
        ItemMeta  m = i.getItemMeta();
        m.setDisplayName("§r");
        i.setItemMeta(m);
        return i;
    }

    private void whiteLightScreen(Player ep, int durationTicks, AncientEyePlugin plugin) {
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (!ep.isOnline() || t++ >= durationTicks) { cancel(); return; }
                Location eyeFront = ep.getEyeLocation().clone()
                        .add(ep.getEyeLocation().getDirection().multiply(0.1));
                ep.spawnParticle(Particle.FLASH, eyeFront, 1, 0, 0, 0, 0);
                for (int i = 0; i < 8; i++) {
                    double a = Math.toRadians(i * 45);
                    ep.spawnParticle(Particle.FLASH,
                        eyeFront.clone().add(Math.cos(a)*0.12, Math.sin(a)*0.12, 0),
                        1, 0, 0, 0, 0);
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private String progressBar(int cur, int max) {
        int bars = 12;
        int done = (int)((double)Math.min(cur, max) / max * bars);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bars; i++) {
            sb.append(i < done ? "§a|" : "§7|");
        }
        return sb.toString();
    }

    private void restoreOrDrop(Player p, ItemStack stored,
                               Runnable setter, ItemStack current) {
        if (stored == null) return;
        boolean slotEmpty = (current == null || current.getType() == Material.AIR);
        if (slotEmpty) {
            setter.run();
        } else {
            p.getWorld().dropItemNaturally(p.getLocation(), stored);
            p.sendMessage("\u00a77Your hidden item was dropped: \u00a7f"
                + stored.getType().name().toLowerCase().replace("_", " "));
        }
    }

    private void applySafeDamage(Player owner, Location loc, double radius, double damage) {
        loc.getWorld().getNearbyEntities(loc, radius, radius, radius).forEach(entity -> {
            if (entity instanceof LivingEntity victim && !entity.equals(owner)) {
                victim.damage(damage * getDmg(owner), owner);
                victim.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1, 0), 5);
            }
        });
    }
}
