package com.ancienteye;

import org.bukkit.*;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;
import java.util.*;

public class AbilityPrimary implements Listener {

    private final AncientEyePlugin plugin;
    private final AbilityLogic logic;

    public AbilityPrimary(AncientEyePlugin plugin, AbilityLogic logic) {
        this.plugin = plugin;
        this.logic  = logic;
    }

    public void activate(Player p, EyeType eye) {
        if (plugin.getCooldownManager().isOnCooldown(p, "P")) return;
        plugin.getCooldownManager().setCooldown(p, "P", logic.getCd(p, eye, "primary"));

        Location loc = p.getLocation().clone();
        Vector   dir = p.getEyeLocation().getDirection().normalize();
        final double dm = logic.getDmg(p);
        final double dr = logic.getDur(p);
        World    w   = p.getWorld();

        switch (eye) {

            // ── VOID — Void Blink ─────────────────────────────────────────
            case VOID -> {
                logic.voidRift(w, loc);
                w.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.6f);
                w.playSound(loc, Sound.BLOCK_PORTAL_AMBIENT, 0.5f, 1.5f);
                for (int i = 1; i <= 8; i++)
                    w.spawnParticle(Particle.PORTAL, loc.clone().add(dir.clone().multiply(i)), 5, 0.15, 0.35, 0.15, 0.08);
                Location dest = logic.safe(loc.clone().add(dir.clone().multiply(8)));
                logic.voidRift(w, dest);
                w.strikeLightningEffect(dest);
                p.teleport(dest);
            }

            // ── PHANTOM — Arcane Circle Trap ──────────────────────────────
            case PHANTOM -> {
                Location groundLoc = null;
                Location eyeLoc = p.getEyeLocation();
                Vector   eyeDir = eyeLoc.getDirection().normalize();
                for (int i = 1; i <= 50; i++) {
                    Location check = eyeLoc.clone().add(eyeDir.clone().multiply(i));
                    if (check.getBlock().getType().isSolid()) {
                        groundLoc = check.clone().add(0, 1, 0); break;
                    }
                }
                if (groundLoc == null) {
                    groundLoc = p.getLocation().clone();
                    groundLoc.setY(p.getLocation().getBlockY());
                }
                final Location center = groundLoc.clone();
                final double RADIUS = 7.5;
                w.playSound(center, Sound.ENTITY_ENDERMAN_STARE, 1f, 0.5f);
                w.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1f, 0.3f);
                w.playSound(center, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.8f, 0.6f);

                new BukkitRunnable() {
                    int    ticks = 0;
                    double spin  = 0;
                    public void run() {
                        if (ticks++ >= 120 || !p.isOnline()) {
                            for (int i = 0; i < 36; i++) {
                                double a = Math.toRadians(i * 10);
                                w.spawnParticle(Particle.CLOUD, center.clone().add(Math.cos(a)*RADIUS, 0.1, Math.sin(a)*RADIUS), 2, 0.2, 0.1, 0.2, 0.03);
                            }
                            w.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.7f);
                            cancel(); return;
                        }
                        spin += 0.04;
                        // Outer ring
                        for (int i = 0; i < 60; i++) {
                            double a = spin + Math.toRadians(i * 6.0);
                            w.spawnParticle(Particle.DUST, center.clone().add(Math.cos(a)*RADIUS, 0.05, Math.sin(a)*RADIUS), 1, 0,0,0,0, new Particle.DustOptions(Color.fromRGB(200,160,20), 1.0f));
                        }
                        // Inner ring
                        double innerR = RADIUS * 0.65;
                        for (int i = 0; i < 40; i++) {
                            double a = -spin*1.3 + Math.toRadians(i * 9.0);
                            w.spawnParticle(Particle.DUST, center.clone().add(Math.cos(a)*innerR, 0.05, Math.sin(a)*innerR), 1, 0,0,0,0, new Particle.DustOptions(Color.fromRGB(220,180,40), 0.9f));
                        }
                        // Core ring
                        double coreR = RADIUS * 0.30;
                        for (int i = 0; i < 24; i++) {
                            double a = spin*2.0 + Math.toRadians(i * 15.0);
                            w.spawnParticle(Particle.DUST, center.clone().add(Math.cos(a)*coreR, 0.05, Math.sin(a)*coreR), 1, 0,0,0,0, new Particle.DustOptions(Color.fromRGB(255,240,180), 1.1f));
                        }
                        // Spokes
                        for (int spoke = 0; spoke < 8; spoke++) {
                            double sa = spin + Math.toRadians(spoke * 45);
                            for (int k = 1; k <= 14; k++) {
                                double r = (k / 14.0) * RADIUS;
                                w.spawnParticle(Particle.DUST, center.clone().add(Math.cos(sa)*r, 0.05, Math.sin(sa)*r), 1, 0,0,0,0, new Particle.DustOptions(Color.fromRGB(200,160,20), 0.8f));
                            }
                        }
                        // Orb circles
                        for (int orb = 0; orb < 8; orb++) {
                            double oa = -spin*0.5 + Math.toRadians(orb * 45);
                            double ox = Math.cos(oa) * (RADIUS * 0.82), oz = Math.sin(oa) * (RADIUS * 0.82);
                            for (int oi = 0; oi < 10; oi++) {
                                double ia = spin*2 + Math.toRadians(oi * 36);
                                w.spawnParticle(Particle.DUST, center.clone().add(ox+Math.cos(ia)*0.5, 0.05, oz+Math.sin(ia)*0.5), 1, 0,0,0,0, new Particle.DustOptions(Color.fromRGB(230,190,50), 1.2f));
                            }
                        }
                        if (ticks % 3 == 0) {
                            w.spawnParticle(Particle.END_ROD, center.clone().add(0, 0.1, 0), 2, 0.1, 0.05, 0.1, 0.01);
                            w.spawnParticle(Particle.DUST,    center.clone().add(0, 0.1, 0), 1, 0,0,0,0, new Particle.DustOptions(Color.fromRGB(255,220,100), 1.3f));
                        }
                        if (ticks % 20 == 0) {
                            center.getWorld().getNearbyEntities(center, RADIUS, 2, RADIUS).forEach(e -> {
                                if (!(e instanceof LivingEntity le) || e == p) return;
                                double dist = Math.sqrt(Math.pow(e.getLocation().getX()-center.getX(),2)+Math.pow(e.getLocation().getZ()-center.getZ(),2));
                                if (dist > RADIUS) return;
                                le.damage(2.0 * dm, p);
                                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 25, 10, false, false));
                                w.spawnParticle(Particle.SNOWFLAKE, le.getLocation().add(0,1,0), 8, 0.3, 0.5, 0.3, 0.02);
                            });
                        }
                        if (ticks % 5 == 0) {
                            double rndA = Math.random()*Math.PI*2, rndR = Math.random()*RADIUS;
                            w.spawnParticle(Particle.REVERSE_PORTAL, center.clone().add(Math.cos(rndA)*rndR, 0.1, Math.sin(rndA)*rndR), 1, 0, 0.1, 0, 0.01);
                        }
                    }
                }.runTaskTimer(plugin, 0, 1);
            }

            // ── STORM — Lightning Strike ───────────────────────────────────
            case STORM -> {
                double dmg = logic.ecfg("STORM", "primary-damage", 12.0);
                LivingEntity target = logic.aim(p, 30.0);
                if (target == null) { p.sendMessage("§cKoi target nahi mila!"); return; }
                Location tLoc = target.getLocation();
                w.strikeLightningEffect(tLoc);
                w.playSound(tLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2f, 1f);
                for (int i = 0; i < 8; i++) {
                    FallingBlock fb = w.spawnFallingBlock(tLoc.clone().add(0, 0.5, 0), w.getBlockAt(tLoc.clone().subtract(0,1,0)).getBlockData());
                    fb.setVelocity(new Vector((Math.random()-0.5)*0.5, 0.6+Math.random(), (Math.random()-0.5)*0.5));
                    fb.setDropItem(false);
                }
                w.spawnParticle(Particle.EXPLOSION_EMITTER, tLoc, 1);
                w.spawnParticle(Particle.FLASH, tLoc, 5);
                w.spawnParticle(Particle.ELECTRIC_SPARK, tLoc, 50, 1,1,1, 0.1);
                logic.applySafeDamage(p, tLoc, 4.0, dmg);
            }

            // ── FROST — Ice Shield ────────────────────────────────────────
            case FROST -> {
                w.spawnParticle(Particle.SNOWFLAKE, loc, 60, 0.6, 0.4, 0.6, 0.08);
                w.spawnParticle(Particle.WHITE_ASH, loc, 30, 0.4, 0.2, 0.4, 0.05);
                w.playSound(loc, Sound.ENTITY_PLAYER_HURT_FREEZE, 1f, 1.5f);
                w.playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.8f);
                w.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 1.8f);
                p.sendTitle("\u00a7b\u00a7lICE SHIELD", "\u00a77Protected for 5 seconds!", 5, 60, 10);
                final boolean[] shieldActive = {true};
                final org.bukkit.event.Listener shieldListener = new org.bukkit.event.Listener() {
                    @org.bukkit.event.EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
                    public void onDamage(org.bukkit.event.entity.EntityDamageEvent ev) {
                        if (!shieldActive[0]) return;
                        if (!ev.getEntity().getUniqueId().equals(p.getUniqueId())) return;
                        ev.setCancelled(true);
                        Location pLoc = p.getLocation();
                        w.spawnParticle(Particle.SNOWFLAKE, pLoc, 20, 0.5, 0.8, 0.5, 0.05);
                        w.spawnParticle(Particle.WHITE_ASH, pLoc, 10, 0.4, 0.6, 0.4, 0.03);
                        w.playSound(pLoc, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.5f);
                    }
                };
                plugin.getServer().getPluginManager().registerEvents(shieldListener, plugin);
                new BukkitRunnable() {
                    int    ticks      = 0;
                    double orbitAngle = 0;
                    final int duration = logic.ticks(100, dr);
                    public void run() {
                        if (ticks++ >= duration || !p.isOnline()) {
                            shieldActive[0] = false;
                            org.bukkit.event.HandlerList.unregisterAll(shieldListener);
                            Location pLoc = p.getLocation().clone().add(0, 1, 0);
                            w.spawnParticle(Particle.SNOWFLAKE, pLoc, 80, 1.0, 1.2, 1.0, 0.08);
                            w.spawnParticle(Particle.WHITE_ASH, pLoc, 50, 0.8, 1.0, 0.8, 0.05);
                            w.playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 0.8f);
                            cancel(); return;
                        }
                        orbitAngle += 0.08;
                        Location center = p.getLocation().clone().add(0, 1.0, 0);
                        for (int panel = 0; panel < 4; panel++) {
                            double panelAngle = orbitAngle + (panel * Math.PI / 2);
                            double sx = Math.cos(panelAngle)*1.0, sz = Math.sin(panelAngle)*1.0;
                            Location sc2 = center.clone().add(sx, 0, sz);
                            double tx = -Math.sin(panelAngle), tz = Math.cos(panelAngle);
                            for (int col = -2; col <= 2; col++) {
                                for (int row = -2; row <= 2; row++) {
                                    Location pt = new Location(w, sc2.getX()+tx*col*0.2, sc2.getY()+row*0.25, sc2.getZ()+tz*col*0.2);
                                    boolean isEdge = (Math.abs(col)==2 || Math.abs(row)==2);
                                    w.spawnParticle(Particle.DUST, pt, 1,0,0,0,0, new Particle.DustOptions(isEdge ? Color.fromRGB(180,240,255) : Color.fromRGB(220,248,255), isEdge?1.3f:0.9f));
                                }
                            }
                            if (ticks % 5 == 0) w.spawnParticle(Particle.END_ROD, sc2, 1, 0, 0.1, 0, 0.01);
                        }
                        if (ticks % 20 == 0) w.playSound(p.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 0.3f, 1.8f);
                    }
                }.runTaskTimer(plugin, 0, 1);
            }

            // ── FLAME — Fire Tornado ──────────────────────────────────────
            case FLAME -> {
                org.bukkit.block.Block targetBlock = p.getTargetBlock(null, 30);
                if (targetBlock.getType() == Material.AIR) { p.sendMessage("§cAim at a block!"); return; }
                final Location center = targetBlock.getLocation().add(0.5, 1.0, 0.5);
                new BukkitRunnable() {
                    int    ticks = 0;
                    double angle = 0;
                    public void run() {
                        if (ticks >= 120) { cancel(); return; }
                        ticks++; angle += 0.4;
                        for (double y = 0; y <= 10; y += 0.5) {
                            double radius = 0.5 + (y * 0.25);
                            for (int arm = 0; arm < 3; arm++) {
                                double currentAngle = angle + (y * 0.5) + Math.toRadians(arm * 120);
                                Location partLoc = new Location(center.getWorld(), center.getX()+radius*Math.cos(currentAngle), center.getY()+y, center.getZ()+radius*Math.sin(currentAngle));
                                center.getWorld().spawnParticle(Particle.FLAME, partLoc, 2,0,0,0,0);
                                if (y % 2 == 0) center.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, partLoc, 1,0,0,0,0);
                                if (Math.random() < 0.1) center.getWorld().spawnParticle(Particle.LAVA, partLoc, 1,0,0,0,0);
                            }
                        }
                        for (org.bukkit.entity.Entity e : center.getWorld().getNearbyEntities(center, 8, 10, 8)) {
                            if (!(e instanceof LivingEntity le) || e == p) continue;
                            Location eLoc = e.getLocation();
                            Location tCenter = center.clone(); tCenter.setY(eLoc.getY());
                            Vector toCenter = tCenter.toVector().subtract(eLoc.toVector());
                            double dist = toCenter.length();
                            if (dist > 0 && dist <= 8) {
                                toCenter.normalize();
                                Vector spin = new Vector(-toCenter.getZ(), 0, toCenter.getX());
                                e.setVelocity(toCenter.multiply(0.15).add(spin.multiply(0.4)).setY(0.12));
                                e.setFallDistance(0f);
                                le.setFireTicks(60);
                                if (ticks % 10 == 0) le.damage(1.5 * dm, p);
                            }
                        }
                    }
                }.runTaskTimer(plugin, 0, 1);
            }

            // ── SHADOW — Shadow Step ──────────────────────────────────────
            case SHADOW -> {
                LivingEntity tgt = logic.aim(p, 20);
                if (tgt != null) {
                    w.spawnParticle(Particle.SMOKE, loc, 50, 0.4, 0.8, 0.4, 0.07);
                    w.spawnParticle(Particle.DUST, loc, 25, 0.3, 0.6, 0.3, 0, new Particle.DustOptions(Color.BLACK, 2f));
                    Vector behind = tgt.getLocation().getDirection().normalize().multiply(2.0);
                    Location dest = logic.safe(tgt.getLocation().clone().subtract(behind));
                    dest.setYaw(tgt.getLocation().getYaw() + 180);
                    w.spawnParticle(Particle.SMOKE, dest, 50, 0.4, 0.8, 0.4, 0.07);
                    w.spawnParticle(Particle.PORTAL, dest, 30, 0.3, 0.7, 0.3, 0.08);
                    w.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.7f);
                    p.teleport(dest);
                    tgt.damage(7.0 * dm, p);
                    w.spawnParticle(Particle.CRIT, tgt.getLocation(), 30, 0.4, 0.8, 0.4, 0.1);
                } else p.sendMessage("§7No target in range.");
            }

            // ── TITAN — Giant Form ────────────────────────────────────────
            case TITAN -> {
                final double titanDm = logic.getDmg(p);
                w.playSound(loc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.5f, 0.5f);
                w.playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.2f, 0.5f);
                w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.4f);
                w.spawnParticle(Particle.EXPLOSION_EMITTER, loc, 5, 1, 2, 1, 0);
                w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 100, 1.5, 3, 1.5, 0.05);
                p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 100, 2));
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 254));
                p.setMetadata("TitanMode", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                p.sendTitle("§6§lTITAN FORM", "§eYou are Unstoppable!", 5, 40, 5);
                new BukkitRunnable() {
                    int ticks = 0;
                    public void run() {
                        if (!p.isOnline() || ticks >= 100) {
                            p.removeMetadata("TitanMode", plugin);
                            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_IRON_GOLEM_DEATH, 1f, 1.5f);
                            p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation(), 50, 1, 2, 1, 0.1);
                            p.sendMessage("§cTitan Form expired.");
                            cancel(); return;
                        }
                        ticks++;
                        Location cur = p.getLocation();
                        if (ticks % 2 == 0) {
                            w.spawnParticle(Particle.DUST_PLUME, cur, 15, 1.2, 0.1, 1.2, 0.05, Material.STONE.createBlockData());
                            w.spawnParticle(Particle.DUST, cur, 10, 1.5, 2.5, 1.5, 0, new Particle.DustOptions(Color.fromRGB(120, 80, 40), 2.0f));
                        }
                        if (ticks % 10 == 0) {
                            w.playSound(cur, Sound.BLOCK_ANVIL_LAND, 0.5f, 0.5f);
                            p.getNearbyEntities(5, 3, 5).forEach(e -> {
                                if (e instanceof LivingEntity le && e != p) {
                                    Vector push = le.getLocation().toVector().subtract(cur.toVector()).normalize().multiply(1.2).setY(0.4);
                                    le.setVelocity(push);
                                    le.damage(2.0 * titanDm, p);
                                }
                            });
                        }
                    }
                }.runTaskTimer(plugin, 0, 1);
            }

            // ── HUNTER — Dash ─────────────────────────────────────────────
            case HUNTER -> {
                LivingEntity tgt = logic.aim(p, 30);
                if (tgt != null) {
                    Location tgtLoc = tgt.getLocation();
                    Vector dashDir = tgtLoc.toVector().subtract(p.getLocation().toVector()).normalize();
                    w.spawnParticle(Particle.CRIT, loc, 30, 0.3, 0.5, 0.3, 0.12);
                    w.spawnParticle(Particle.END_ROD, loc, 15, 0.2, 0.4, 0.2, 0.06);
                    w.spawnParticle(Particle.SOUL, loc, 10, 0.3, 0.5, 0.3, 0.05);
                    w.playSound(loc, Sound.ENTITY_ARROW_SHOOT, 1f, 1.8f);
                    w.playSound(loc, Sound.ENTITY_PHANTOM_FLAP, 0.8f, 1.5f);
                    p.setFallDistance(0f);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (!p.isOnline()) return;
                        p.setVelocity(dashDir.clone().multiply(1.8).setY(0.35));
                        p.setFallDistance(0f);
                    }, 1L);
                    new BukkitRunnable() {
                        int t = 0;
                        public void run() {
                            if (t++ >= 10) { p.setFallDistance(0f); cancel(); return; }
                            Location cur = p.getLocation();
                            w.spawnParticle(Particle.CRIT, cur, 5, 0.2, 0.3, 0.2, 0.08);
                            w.spawnParticle(Particle.END_ROD, cur, 2, 0.1, 0.2, 0.1, 0.02);
                            w.spawnParticle(Particle.SOUL, cur, 3, 0.2, 0.3, 0.2, 0.04);
                            p.setFallDistance(0f);
                            if (p.getLocation().distanceSquared(tgtLoc) < 2.5 * 2.5) {
                                tgt.damage(7.0 * dm, p);
                                w.spawnParticle(Particle.CRIT, tgtLoc, 40, 0.5, 0.8, 0.5, 0.12);
                                w.spawnParticle(Particle.END_ROD, tgtLoc, 20, 0.4, 0.7, 0.4, 0.08);
                                w.playSound(tgtLoc, Sound.ENTITY_ARROW_HIT_PLAYER, 1f, 1.2f);
                                w.playSound(tgtLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1f);
                                Vector knock = dashDir.clone().multiply(1.5).setY(0.4);
                                Bukkit.getScheduler().runTaskLater(plugin, () -> { if (tgt.isValid()) tgt.setVelocity(knock); }, 1L);
                                p.setFallDistance(0f); cancel();
                            }
                        }
                    }.runTaskTimer(plugin, 2L, 1L);
                } else p.sendMessage("§7No target in range.");
            }

            // ── GRAVITY — Pull ────────────────────────────────────────────
            case GRAVITY -> {
                w.spawnParticle(Particle.REVERSE_PORTAL, loc, 100, 2.5, 2, 2.5, 0.14);
                w.spawnParticle(Particle.PORTAL, loc, 60, 2.0, 1.5, 2.0, 0.10);
                for (int ring = 0; ring < 6; ring++) { final int fr = ring;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        double r = 3.5 - fr * 0.4;
                        for (int i = 0; i < 20; i++) { double a = Math.toRadians(i*18)+fr*0.5;
                            w.spawnParticle(Particle.REVERSE_PORTAL, loc.clone().add(Math.cos(a)*r, fr*0.4, Math.sin(a)*r), 2, 0,0,0, 0.03);
                        }
                    }, ring * 2L);
                }
                w.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1f, 0.3f);
                w.playSound(loc, Sound.ENTITY_ENDERMAN_SCREAM, 0.5f, 0.5f);
                p.getNearbyEntities(8, 8, 8).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        Vector vel = loc.toVector().subtract(e.getLocation().toVector()).normalize().multiply(2.2);
                        le.damage(3.0 * dm, p);
                        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (e.isValid()) e.setVelocity(vel); }, 1L);
                    }
                });
            }

            // ── WIND — Dash ───────────────────────────────────────────────
            case WIND -> {
                w.spawnParticle(Particle.WHITE_ASH, loc, 80, 0.8, 0.8, 0.8, 0.18);
                w.spawnParticle(Particle.CLOUD, loc, 40, 0.5, 0.5, 0.5, 0.08);
                p.setVelocity(dir.clone().multiply(3.5).setY(0.2));
                w.playSound(loc, Sound.ENTITY_PHANTOM_DEATH, 0.8f, 2.0f);
                new BukkitRunnable() { int t = 0;
                    public void run() {
                        if (t++ >= 10) { cancel(); return; }
                        Location cur = p.getLocation();
                        for (int i = 0; i < 6; i++) { double a = Math.toRadians(i*60+t*30);
                            w.spawnParticle(Particle.WHITE_ASH, cur.clone().add(Math.cos(a)*1.2,0.5,Math.sin(a)*1.2), 2,0,0,0, 0.05);
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

            // ── POISON — Strike ───────────────────────────────────────────
            case POISON -> {
                LivingEntity tgt = logic.aim(p, 12);
                if (tgt != null) {
                    w.spawnParticle(Particle.ITEM_SLIME, tgt.getLocation(), 50, 0.5, 0.8, 0.5, 0.07);
                    w.spawnParticle(Particle.SNEEZE, tgt.getLocation(), 25, 0.4, 0.7, 0.4, 0.04);
                    w.spawnParticle(Particle.DUST, tgt.getLocation(), 30, 0.4, 0.7, 0.4, 0, new Particle.DustOptions(Color.fromRGB(40,200,40), 1.8f));
                    for (int i = 0; i < 12; i++) { double a = Math.toRadians(i*30);
                        w.spawnParticle(Particle.ITEM_SLIME, tgt.getLocation().add(Math.cos(a)*1.5,0.5,Math.sin(a)*1.5), 3, 0, 0.2, 0, 0.02);
                    }
                    tgt.addPotionEffect(new PotionEffect(PotionEffectType.POISON,   logic.ticks(200, dr), 2));
                    tgt.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, logic.ticks(100, dr), 1));
                    tgt.damage(4.0 * dm, p);
                    w.playSound(loc, Sound.ENTITY_SLIME_SQUISH, 1f, 0.4f);
                    w.playSound(loc, Sound.ENTITY_WITCH_DRINK, 0.8f, 0.6f);
                } else p.sendMessage("§7No target in range.");
            }

            // ── LIGHT — Flash Burst ───────────────────────────────────────
            case LIGHT -> {
                w.spawnParticle(Particle.FLASH, loc, 3, 0, 0, 0, 0);
                w.spawnParticle(Particle.END_ROD, loc, 80, 2.5, 2.5, 2.5, 0.12);
                w.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, 1f, 2.0f);
                w.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 2.0f);
                for (int ring = 1; ring <= 4; ring++) { final int fr = ring;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        double r = fr * 2.8;
                        for (int i = 0; i < 24; i++) { double a = Math.toRadians(i * 15);
                            w.spawnParticle(Particle.END_ROD, loc.clone().add(Math.cos(a)*r, 0.5, Math.sin(a)*r), 2, 0, 0.2, 0, 0.01);
                        }
                    }, ring * 3L);
                }
                p.getNearbyEntities(10, 10, 10).forEach(e -> {
                    if (!(e instanceof LivingEntity le) || e == p) return;
                    le.damage(5.0 * dm, p);
                    w.spawnParticle(Particle.FLASH, le.getLocation(), 1, 0, 0, 0, 0);
                    w.spawnParticle(Particle.END_ROD, le.getLocation(), 20, 0.4, 0.8, 0.4, 0.05);
                    if (le instanceof Player ep) logic.whiteLightScreen(ep, logic.ticks(80, dr), plugin);
                });
            }

            // ── CRYSTAL — Shield ──────────────────────────────────────────
            case CRYSTAL -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, logic.ticks(200, dr), 4));
                logic.crystalAura(p);
                w.spawnParticle(Particle.END_ROD, loc, 60, 1.2, 1.8, 1.2, 0.06);
                w.spawnParticle(Particle.DUST, loc, 40, 1.0, 1.5, 1.0, 0, new Particle.DustOptions(Color.fromRGB(100,220,255), 2f));
                w.playSound(loc, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1f, 1.5f);
                w.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.2f);
                p.sendTitle("§b§l💎 CRYSTAL SHIELD", "§7Resistance V Active!", 5, 45, 10);
            }

            // ── ECHO — Sonar Pulse ────────────────────────────────────────
            case ECHO -> {
                w.spawnParticle(Particle.SONIC_BOOM, loc, 1, 0, 0, 0, 0);
                w.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.8f, 1.8f);
                w.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 0.5f, 2.0f);
                for (int ring = 1; ring <= 5; ring++) { final int fr = ring;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        double r = fr * 3.5; int pts = 32;
                        for (int i = 0; i < pts; i++) { double a = Math.toRadians(i * (360.0/pts));
                            w.spawnParticle(Particle.END_ROD, loc.clone().add(Math.cos(a)*r, 0.5, Math.sin(a)*r), 1,0,0,0,0);
                            w.spawnParticle(Particle.SONIC_BOOM, loc.clone().add(Math.cos(a)*(r-0.3), 0.5, Math.sin(a)*(r-0.3)), 1,0,0,0,0);
                        }
                        for (int i = 0; i < 24; i++) { double a = Math.toRadians(i * 15);
                            w.spawnParticle(Particle.END_ROD, loc.clone().add(0, Math.cos(a)*r*0.4+r*0.3, Math.sin(a)*r), 1,0,0,0,0);
                        }
                    }, ring * 4L);
                }
                p.getNearbyEntities(20, 20, 20).forEach(e -> {
                    if (!(e instanceof LivingEntity le) || e == p) return;
                    le.setGlowing(true);
                    w.spawnParticle(Particle.END_ROD, le.getLocation(), 15, 0.4, 0.8, 0.4, 0.04);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> { if (le.isValid()) le.setGlowing(false); }, logic.ticks(140, dr));
                    if (le instanceof Player ep) p.sendActionBar("§3🔍 §fRevealed: §b" + ep.getName());
                });
                p.sendTitle("§3§lSONAR PULSE", "§7All enemies revealed!", 5, 50, 10);
            }

            // ── RAGE — Rage Mode ──────────────────────────────────────────
            case RAGE -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE,    logic.ticks(160, dr), 3));
                p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, logic.ticks(100, dr), 1));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,    logic.ticks(80,  dr), 1));
                w.spawnParticle(Particle.DUST, loc, 100, 0.6, 1.2, 0.6, 0, new Particle.DustOptions(Color.RED, 2.5f));
                w.spawnParticle(Particle.CRIT, loc, 60, 0.5, 1.0, 0.5, 0.14);
                w.spawnParticle(Particle.FLAME, loc, 40, 0.4, 0.8, 0.4, 0.09);
                for (int i = 0; i < 16; i++) { double a = Math.toRadians(i*22.5);
                    w.spawnParticle(Particle.DUST, loc.clone().add(Math.cos(a)*2,1,Math.sin(a)*2), 4, 0, 0.5, 0, 0, new Particle.DustOptions(Color.fromRGB(220,20,20), 2f));
                }
                w.playSound(loc, Sound.ENTITY_RAVAGER_ROAR, 1f, 0.8f);
                w.playSound(loc, Sound.ENTITY_WITHER_HURT, 0.7f, 1.2f);
                p.sendTitle("§c§l⚡ RAGE MODE!", "§7+Haste III  +Strength  +Speed", 5, 50, 10);
            }

            // ── SPIRIT — Heal ─────────────────────────────────────────────
            case SPIRIT -> {
                double maxHp = p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
                p.setHealth(Math.min(p.getHealth() + 8.0 * dm, maxHp));
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, logic.ticks(120, dr), 2));
                w.spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 100, 0.5, 1.2, 0.5, 0.1);
                w.spawnParticle(Particle.HEART, loc, 18, 0.6, 0.6, 0.6, 0.08);
                w.spawnParticle(Particle.END_ROD, loc, 40, 0.4, 0.9, 0.4, 0.05);
                for (int i = 0; i < 12; i++) { double a = Math.toRadians(i*30);
                    w.spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(Math.cos(a)*1.8,0.5,Math.sin(a)*1.8), 5, 0, 0.5, 0, 0.04);
                }
                w.playSound(loc, Sound.ENTITY_WITCH_CELEBRATE, 1f, 1.3f);
                w.playSound(loc, Sound.BLOCK_BEACON_AMBIENT, 1f, 1.5f);
                p.sendTitle("§a§l✦ SPIRIT HEAL", "§7Health Restored!", 5, 45, 10);
            }

            // ── TIME — Time Slow ──────────────────────────────────────────
            case TIME -> {
                w.spawnParticle(Particle.REVERSE_PORTAL, loc, 90, 2.5, 2, 2.5, 0.12);
                w.spawnParticle(Particle.DUST, loc, 60, 2, 2, 2, 0, new Particle.DustOptions(Color.fromRGB(150,80,255), 2f));
                for (int ring = 1; ring <= 3; ring++) { final int fr = ring;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        double r = fr * 2.5;
                        for (int i = 0; i < 12; i++) { double a = Math.toRadians(i*30);
                            w.spawnParticle(Particle.DUST, loc.clone().add(Math.cos(a)*r,0.5,Math.sin(a)*r), 3, 0,0,0,0, new Particle.DustOptions(Color.fromRGB(150,80,255), 1.5f));
                        }
                    }, ring * 4L);
                }
                w.playSound(loc, Sound.BLOCK_BEACON_AMBIENT, 1f, 0.2f);
                p.getNearbyEntities(10, 10, 10).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, logic.ticks(160, dr), 5));
                        le.addPotionEffect(new PotionEffect(PotionEffectType.WITHER,   logic.ticks(160, dr), 1));
                        w.spawnParticle(Particle.REVERSE_PORTAL, le.getLocation(), 20, 0.3, 0.6, 0.3, 0.06);
                    }
                });
                p.sendTitle("§d§lTIME SLOW", "§7Enemies frozen in time!", 5, 100, 10);
            }

            // ── WARRIOR — War Cry ─────────────────────────────────────────
            case WARRIOR -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH,   logic.ticks(160, dr), 2));
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, logic.ticks(120, dr), 1));
                w.spawnParticle(Particle.DUST, loc, 100, 0.7, 1.4, 0.7, 0, new Particle.DustOptions(Color.fromRGB(220,80,20), 2.5f));
                w.spawnParticle(Particle.CRIT, loc, 50, 0.5, 1.0, 0.5, 0.12);
                w.spawnParticle(Particle.FLAME, loc, 30, 0.4, 0.8, 0.4, 0.07);
                for (int i = 0; i < 20; i++) { double a = Math.toRadians(i*18);
                    w.spawnParticle(Particle.DUST, loc.clone().add(Math.cos(a)*2.5,0.5,Math.sin(a)*2.5), 4, 0, 0.4, 0, 0, new Particle.DustOptions(Color.fromRGB(220,80,20), 2f));
                }
                w.playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, 1f, 0.6f);
                w.playSound(loc, Sound.ENTITY_RAVAGER_ROAR, 0.9f, 0.8f);
                p.getNearbyEntities(8, 8, 8).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, logic.ticks(60, dr), 1));
                        le.damage(3.0 * dm, p);
                    }
                });
                p.sendTitle("§6§l⚔ WAR CRY!", "§7+Strength II  +Resistance", 5, 50, 10);
            }

            // ── EARTH — Earthquake Wall ────────────────────────────────────
            case EARTH -> {
                Location wallBase = null;
                Location eyeLoc   = p.getEyeLocation();
                Vector   eyeDir   = eyeLoc.getDirection().normalize();
                for (int i = 2; i <= 40; i++) {
                    Location check = eyeLoc.clone().add(eyeDir.clone().multiply(i));
                    if (check.getBlock().getType().isSolid()) { wallBase = check.clone().add(0, 1, 0); break; }
                    Location below = check.clone().subtract(0, 1, 0);
                    if (below.getBlock().getType().isSolid()) { wallBase = below.clone().add(0, 1, 0); break; }
                }
                if (wallBase == null) { wallBase = p.getLocation().clone().add(eyeDir.clone().multiply(8)); wallBase.setY(p.getLocation().getBlockY()); }
                final Location base    = wallBase.clone();
                final Vector   wallDir = new Vector(-eyeDir.getZ(), 0, eyeDir.getX()).normalize();
                final int      WIDTH   = 10, HEIGHT = 15;
                final java.util.List<Location> wallBlocks = new java.util.ArrayList<>();
                w.playSound(base, Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 0.3f);
                w.playSound(base, Sound.BLOCK_STONE_BREAK, 1f, 0.2f);
                w.playSound(base, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.4f);
                for (int i = 0; i < WIDTH; i++) {
                    double offset = (i - WIDTH / 2.0) + 0.5;
                    Location crackLoc = base.clone().add(wallDir.getX()*offset, 0, wallDir.getZ()*offset);
                    w.spawnParticle(Particle.FALLING_DUST, crackLoc, 10, 0.2, 0.1, 0.2, 0.05, Material.DIRT.createBlockData());
                    w.spawnParticle(Particle.EXPLOSION, crackLoc, 1, 0.1, 0, 0.1, 0);
                }
                new BukkitRunnable() {
                    int layer = 0;
                    public void run() {
                        if (layer >= HEIGHT) {
                            cancel();
                            base.getWorld().getNearbyEntities(base, 8, HEIGHT, 8).forEach(e -> {
                                if (!(e instanceof LivingEntity le) || e == p) return;
                                le.damage(6.0 * dm, p);
                                Vector vel = e.getLocation().toVector().subtract(base.toVector()).normalize().multiply(2.8).setY(0.9);
                                Bukkit.getScheduler().runTaskLater(plugin, () -> { if (e.isValid()) e.setVelocity(vel); }, 1L);
                            });
                            return;
                        }
                        for (int i = 0; i < WIDTH; i++) {
                            double offset = (i - WIDTH / 2.0) + 0.5;
                            Location blockLoc = base.clone().add(wallDir.getX()*offset, layer, wallDir.getZ()*offset);
                            org.bukkit.block.Block blk = blockLoc.getBlock();
                            if (blk.getType() == Material.AIR || blk.getType() == Material.CAVE_AIR) {
                                blk.setType(layer % 3 == 0 ? Material.COBBLESTONE : layer % 3 == 1 ? Material.STONE : Material.GRAVEL);
                                wallBlocks.add(blockLoc.clone());
                            }
                            w.spawnParticle(Particle.FALLING_DUST, blockLoc, 5, 0.3, 0.1, 0.3, 0.04, Material.STONE.createBlockData());
                        }
                        if (layer % 3 == 0) w.playSound(base, Sound.BLOCK_STONE_PLACE, 0.8f, 0.5f + layer * 0.03f);
                        layer++;
                    }
                }.runTaskTimer(plugin, 0, 2);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    w.playSound(base, Sound.BLOCK_STONE_BREAK, 1.5f, 0.4f);
                    w.playSound(base, Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 0.5f);
                    for (Location bl : wallBlocks) {
                        if (bl.getY() >= base.getY() + HEIGHT - 3) {
                            w.spawnParticle(Particle.FALLING_DUST, bl, 8, 0.3, 0.1, 0.3, 0.05, Material.GRAVEL.createBlockData());
                            w.spawnParticle(Particle.EXPLOSION, bl, 1, 0.2, 0, 0.2, 0);
                        }
                    }
                    new BukkitRunnable() {
                        int layer = HEIGHT - 1;
                        public void run() {
                            if (layer < 0) { cancel(); wallBlocks.clear(); return; }
                            final int fl = layer;
                            for (Location bl : wallBlocks) {
                                if ((int)(bl.getY() - base.getY()) == fl) {
                                    org.bukkit.block.Block blk = bl.getBlock();
                                    if (blk.getType() == Material.COBBLESTONE || blk.getType() == Material.STONE || blk.getType() == Material.GRAVEL) {
                                        blk.setType(Material.AIR);
                                        w.spawnParticle(Particle.FALLING_DUST, bl, 6, 0.2, 0.1, 0.2, 0.04, Material.GRAVEL.createBlockData());
                                    }
                                }
                            }
                            if (layer % 3 == 0) w.playSound(base, Sound.BLOCK_GRAVEL_BREAK, 0.7f, 1.0f - layer * 0.02f);
                            layer--;
                        }
                    }.runTaskTimer(plugin, 0, 2);
                }, 200L);
            }

            // ── METEOR PRIMARY — Launch ────────────────────────────────────
            case METEOR -> {
                double dmg = logic.ecfg("METEOR", "primary-damage", 15.0);
                p.setVelocity(p.getLocation().getDirection().multiply(1.8).setY(1.5));
                w.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 0.5f);
                new BukkitRunnable() {
                    public void run() {
                        if (!p.isOnline() || p.isDead()) { cancel(); return; }
                        p.setFallDistance(0.0f);
                        w.spawnParticle(Particle.FLAME, p.getLocation(), 20, 0.5, 0.5, 0.5, 0.05);
                        w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, p.getLocation(), 10, 0.3, 0.3, 0.3, 0.02);
                        if (p.getVelocity().getY() < -0.1 && p.isOnGround()) {
                            w.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 3f, 0.5f);
                            w.spawnParticle(Particle.EXPLOSION_EMITTER, p.getLocation(), 4);
                            w.spawnParticle(Particle.LAVA, p.getLocation(), 80, 3, 1, 3, 0.5);
                            for (int i = 0; i < 360; i+=10) {
                                Location circle = p.getLocation().add(Math.cos(Math.toRadians(i))*4, 0.2, Math.sin(Math.toRadians(i))*4);
                                w.spawnParticle(Particle.FLAME, circle, 2, 0, 0, 0, 0.2);
                            }
                            logic.applySafeDamage(p, p.getLocation(), 7.0, dmg);
                            p.getLocation().getWorld().getNearbyEntities(p.getLocation(), 7, 7, 7).forEach(e -> {
                                if (e instanceof LivingEntity le && e != p) {
                                    Vector push = le.getLocation().toVector().subtract(p.getLocation().toVector()).normalize();
                                    le.setVelocity(push.multiply(2.5).setY(1.2));
                                }
                            });
                            cancel();
                        }
                    }
                }.runTaskTimer(plugin, 5, 1);
            }

            // ── MIRAGE — Magic Circle ─────────────────────────────────────
            // ✅ FIX: laser y=1.5, no block break, bigger rings
            case MIRAGE -> {
                w.playSound(p.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.5f, 0.7f);
                w.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 0.5f);
                final Vector mirDir = p.getLocation().getDirection().clone();
                mirDir.setY(0); mirDir.normalize();
                final Location base = p.getLocation().clone().add(mirDir.multiply(10));
                base.setY(p.getLocation().getY());
                new BukkitRunnable() {
                    int    ticks = 0;
                    double spin  = 0;
                    public void run() {
                        if (!p.isOnline() || p.isDead()) { cancel(); return; }
                        if (ticks++ >= 200) {
                            // ✅ FIX: NO block break — manual damage only
                            w.playSound(base, Sound.ENTITY_GENERIC_EXPLODE, 3f, 0.3f);
                            w.playSound(base, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 2f, 0.5f);
                            w.spawnParticle(Particle.EXPLOSION_EMITTER, base.clone().add(0,1,0), 20, 4, 2, 4, 0);
                            w.spawnParticle(Particle.FLASH, base.clone().add(0,1,0), 3, 2, 1, 2, 0);
                            w.createExplosion(base, 25.0f, false, true);
                            // Manual damage — no block break
                            for (org.bukkit.entity.Entity e : base.getNearbyEntities(30,30,30)) {
                                if (!(e instanceof LivingEntity victim)) continue;
                                if (e.equals(p)) continue;
                                if (e instanceof Player ep && (ep.getGameMode()==GameMode.CREATIVE||ep.getGameMode()==GameMode.SPECTATOR)) continue;
                                double dist = e.getLocation().distance(base);
                                victim.damage(Math.max(5, 60 - dist*1.5), p);
                                victim.setVelocity(e.getLocation().toVector().subtract(base.toVector()).normalize().multiply(2).setY(0.8));
                            }
                            cancel(); return;
                        }
                        spin += 0.05;
                        final double TOP = 18.0;
                        // ✅ Bigger rings
                        Location top = base.clone().add(0, TOP, 0);
                        mRing(w, top, 10.0, 110, spin);         // bigger
                        mRing(w, top, 8.0,  90, -spin*1.3);
                        mRing(w, top, 6.0,  70,  spin*1.8);
                        mRing(w, top, 4.0,  50, -spin*2.5);
                        mRing(w, top, 2.5,  35,  spin*3.0);
                        for (int i = 0; i < 8; i++) {    // 8 satellite circles
                            double a = (Math.PI*2.0/8)*i + spin;
                            Location sc = top.clone().add(Math.cos(a)*8.5, 0, Math.sin(a)*8.5);
                            mRing(w, sc, 1.5, 28, -spin*2);
                            for (int s = 0; s < 5; s++) {
                                double sa  = (Math.PI*2.0/5)*s - spin;
                                double sa2 = (Math.PI*2.0/5)*((s+2)%5) - spin;
                                mLine(w, sc.clone().add(Math.cos(sa)*1.3,0,Math.sin(sa)*1.3), sc.clone().add(Math.cos(sa2)*1.3,0,Math.sin(sa2)*1.3), 10);
                            }
                        }
                        for (int i = 0; i < 6; i++) {  // Hexagram
                            double a1=(Math.PI*2.0/6)*i+spin, a2=(Math.PI*2.0/6)*((i+2)%6)+spin;
                            mLine(w, top.clone().add(Math.cos(a1)*6.0,0,Math.sin(a1)*6.0), top.clone().add(Math.cos(a2)*6.0,0,Math.sin(a2)*6.0), 20);
                        }
                        // Middle rings
                        double[][] midData = {{13.0,6.0},{10.0,5.0},{7.0,4.0},{4.5,5.5}};
                        for (int r = 0; r < midData.length; r++) {
                            Location ml = base.clone().add(0, midData[r][0], 0);
                            double ms = (r%2==0) ? spin*1.5 : -spin*1.5;
                            mRing(w, ml, midData[r][1], 55, ms);
                            mRing(w, ml, midData[r][1]*0.65, 40, -ms);
                            for (int i = 0; i < 4; i++) { double a=(Math.PI/2.0)*i+ms;
                                mLine(w, ml.clone().add(Math.cos(a)*midData[r][1],0,Math.sin(a)*midData[r][1]), ml.clone().add(Math.cos(a+Math.PI)*midData[r][1],0,Math.sin(a+Math.PI)*midData[r][1]), 14);
                            }
                        }
                        // Bottom circle — y=1.5 ground safe
                        Location bot = base.clone().add(0, 1.5, 0);
                        mRing(w, bot, 8.0, 100, -spin);
                        mRing(w, bot, 6.5,  80,  spin*1.2);
                        mRing(w, bot, 5.0,  65, -spin*1.8);
                        mRing(w, bot, 3.5,  48,  spin*2.5);
                        mRing(w, bot, 2.0,  32, -spin*3.0);
                        for (int i = 0; i < 6; i++) {
                            double a=(Math.PI*2.0/6)*i-spin;
                            Location sc = bot.clone().add(Math.cos(a)*6.5,0,Math.sin(a)*6.5);
                            mRing(w, sc, 1.2, 20, spin*2);
                        }
                        for (int i = 0; i < 6; i++) {
                            double a1=(Math.PI*2.0/6)*i-spin, a2=(Math.PI*2.0/6)*((i+2)%6)-spin;
                            mLine(w, bot.clone().add(Math.cos(a1)*5.0,0,Math.sin(a1)*5.0), bot.clone().add(Math.cos(a2)*5.0,0,Math.sin(a2)*5.0), 16);
                        }
                        // ✅ Laser y=1.5 se start — ground block touch nahi
                        for (double y = 1.5; y <= TOP; y += 0.18) {
                            Location lp = base.clone().add(0, y, 0);
                            w.spawnParticle(Particle.DUST, lp, 3, 0.08, 0, 0.08, 0, new Particle.DustOptions(Color.fromRGB(255,255,0), 2.8f));
                            w.spawnParticle(Particle.DUST, lp, 2, 0.18, 0, 0.18, 0, new Particle.DustOptions(Color.fromRGB(255,210,30), 3.8f));
                        }
                        w.spawnParticle(Particle.END_ROD, base.clone().add(0,TOP/2.0,0), 5, 0.25, TOP/2.0*0.85, 0.25, 0.004);
                        // Damage 30 block radius
                        if (ticks % 10 == 0) {
                            for (org.bukkit.entity.Entity e : base.getNearbyEntities(30,30,30)) {
                                if (!(e instanceof LivingEntity victim)) continue;
                                if (e.equals(p)) continue;
                                if (e instanceof Player ep && (ep.getGameMode()==GameMode.CREATIVE||ep.getGameMode()==GameMode.SPECTATOR)) continue;
                                victim.damage(4.0, p);
                                e.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, victim.getLocation().add(0,1,0), 5, 0.3,0.3,0.3,0);
                            }
                        }
                        if (ticks % 20 == 0) w.playSound(base, Sound.BLOCK_BEACON_AMBIENT, 1f, 0.3f);
                        if (ticks % 7  == 0) w.playSound(base, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.25f, 2f);
                        ticks++;
                    }
                    private void mRing(World world, Location center, double radius, int pts, double offset) {
                        for (int i = 0; i < pts; i++) {
                            double a = (Math.PI*2.0/pts)*i + offset;
                            Location pt = center.clone().add(Math.cos(a)*radius, 0, Math.sin(a)*radius);
                            world.spawnParticle(Particle.DUST, pt, 1,0,0,0,0, new Particle.DustOptions(Color.fromRGB(255,50,0), 1.8f));
                        }
                    }
                    private void mLine(World world, Location from, Location to, int steps) {
                        for (int i = 0; i <= steps; i++) {
                            double t = (double)i/steps;
                            Location pt = from.clone().add((to.getX()-from.getX())*t,(to.getY()-from.getY())*t,(to.getZ()-from.getZ())*t);
                            world.spawnParticle(Particle.DUST, pt, 1,0,0,0,0, new Particle.DustOptions(Color.fromRGB(200,40,0), 1.5f));
                        }
                    }
                }.runTaskTimer(plugin, 0, 1);
            }

            // ── OCEAN — Tsunami Wave ───────────────────────────────────────
            case OCEAN -> {
                double dmg = logic.ecfg("OCEAN", "primary-damage", 12.0);
                w.playSound(p.getLocation(), Sound.ENTITY_DOLPHIN_SPLASH, 2f, 0.5f);
                new BukkitRunnable() {
                    int      ticks = 0;
                    Location wave  = p.getLocation().clone().add(0, 0.5, 0);
                    final Vector wDir = p.getLocation().getDirection().setY(0).normalize().multiply(1.2);
                    public void run() {
                        if (ticks++ >= 15 || !p.isOnline()) { cancel(); return; }
                        wave.add(wDir);
                        for (double x = -2; x <= 2; x+=0.5) {
                            for (double y = 0; y <= 2; y+=0.5) {
                                Vector side = new Vector(-wDir.getZ(), 0, wDir.getX()).normalize().multiply(x);
                                Location partLoc = wave.clone().add(side).add(0, y, 0);
                                w.spawnParticle(Particle.DRIPPING_WATER, partLoc, 5, 0.1, 0.1, 0.1, 0.5);
                                w.spawnParticle(Particle.BUBBLE_POP, partLoc, 2, 0.1, 0.1, 0.1, 0.1);
                            }
                        }
                        w.playSound(wave, Sound.BLOCK_WATER_AMBIENT, 0.5f, 1.5f);
                        logic.applySafeDamage(p, wave, 3.0, dmg);
                        wave.getWorld().getNearbyEntities(wave, 3, 3, 3).forEach(e -> {
                            if (e instanceof LivingEntity le && e != p) le.setVelocity(wDir.clone().multiply(1.5).setY(0.4));
                        });
                    }
                }.runTaskTimer(plugin, 0, 1);
            }

            // ── ECLIPSE PRIMARY — Black Cube + Blast ─────────────────────
            // ✅ FIX: cube shows for 3s then blasts — target.isDead check
            case ECLIPSE -> {
                double dmg = logic.ecfg("ECLIPSE", "primary-damage", 18.0);
                LivingEntity target = logic.aim(p, 30.0);
                if (target == null) { p.sendMessage("§cNo target found!"); return; }
                w.playSound(p.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1f, 0.5f);
                w.playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_STARE, 0.8f, 0.5f);
                final boolean[] blasted = {false};
                new BukkitRunnable() {
                    int    ticks     = 0;
                    double cubeAngle = 0;
                    public void run() {
                        if (!target.isValid() || target.isDead()) {
                            if (!blasted[0]) { blasted[0] = true; logic.doEclipseBlast(p, w, target.getLocation().clone().add(0,1,0), dmg, plugin); }
                            cancel(); return;
                        }
                        ticks++;
                        if (ticks <= 30) {
                            cubeAngle += 0.12;
                            Location body = target.getLocation().clone().add(0, 1.0, 0);
                            double r = 0.75 + Math.sin(ticks*0.3)*0.08;
                            double cosA=Math.cos(cubeAngle), sinA=Math.sin(cubeAngle), cosT=Math.cos(0.4), sinT=Math.sin(0.4);
                            double[][] verts = {{-r,-r,-r},{r,-r,-r},{r,r,-r},{-r,r,-r},{-r,-r,r},{r,-r,r},{r,r,r},{-r,r,r}};
                            int[][] edges = {{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},{0,4},{1,5},{2,6},{3,7}};
                            double[][] rv = new double[8][3];
                            for (int v = 0; v < 8; v++) {
                                double x=verts[v][0],y=verts[v][1],z=verts[v][2];
                                double nx=x*cosA-z*sinA, nz=x*sinA+z*cosA;
                                double ny2=y*cosT-nz*sinT, nz2=y*sinT+nz*cosT;
                                rv[v][0]=nx; rv[v][1]=ny2+r; rv[v][2]=nz2;
                            }
                            Particle.DustOptions black = new Particle.DustOptions(Color.fromRGB(10,0,20), 1.1f);
                            for (int[] edge : edges) {
                                double[] va=rv[edge[0]], vb=rv[edge[1]];
                                for (int k = 0; k <= 5; k++) {
                                    double t=(double)k/5;
                                    Location pt = body.clone().add(va[0]+(vb[0]-va[0])*t, va[1]+(vb[1]-va[1])*t-r, va[2]+(vb[2]-va[2])*t);
                                    w.spawnParticle(Particle.DUST, pt, 1,0,0,0,0, black);
                                }
                            }
                            w.spawnParticle(Particle.SQUID_INK, body, 2, 0.1, 0.1, 0.1, 0.01);
                            w.spawnParticle(Particle.DRAGON_BREATH, body, 3, 0.15, 0.15, 0.15, 0.02);
                            if (ticks % 10 == 0) {
                                float pitch = 0.4f + (ticks/30f)*1.2f;
                                w.playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_AMBIENT, 0.6f, pitch);
                            }
                        } else {
                            if (!blasted[0]) { blasted[0] = true; logic.doEclipseBlast(p, w, target.getLocation().clone().add(0,1,0), dmg, plugin); }
                            cancel();
                        }
                    }
                }.runTaskTimer(plugin, 0, 2);
            }

            // ── GUARDIAN — Dome ───────────────────────────────────────────
            case GUARDIAN -> {
                w.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 2f, 1.2f);
                new BukkitRunnable() {
                    int ticks = 0;
                    public void run() {
                        if (ticks++ >= 120 || !p.isOnline() || p.isDead()) {
                            w.playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 2f, 1.2f);
                            cancel(); return;
                        }
                        for (double t = 0; t <= Math.PI/2; t += Math.PI/10) {
                            for (double r = 0; r <= 2*Math.PI; r += Math.PI/10) {
                                double x=5.0*Math.sin(t)*Math.cos(r), y=5.0*Math.cos(t), z=5.0*Math.sin(t)*Math.sin(r);
                                w.spawnParticle(Particle.ENCHANT, p.getLocation().clone().add(x, y, z), 1,0,0,0,0);
                            }
                        }
                        p.getWorld().getNearbyEntities(p.getLocation(), 5, 5, 5).forEach(e -> {
                            if (e instanceof Player ally) ally.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 1));
                        });
                    }
                }.runTaskTimer(plugin, 0, 3);
            }
        }
        p.playSound(loc, Sound.ENTITY_ENDER_EYE_DEATH, 0.8f, 1f);
    }
}
