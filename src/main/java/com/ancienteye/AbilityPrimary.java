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
import org.bukkit.FluidCollisionMode;
import org.bukkit.util.RayTraceResult;
import java.util.HashMap;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import java.util.Map;
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

                        case VOID -> {
                Location center = p.getLocation();
                int size = 8; // 16x16 ka box matlab radius 8
                long durationTicks = 200L; // 10 seconds
                
                // 1. Box Banane ka Logic (3 Seconds = 60 Ticks)
                new BukkitRunnable() {
                    int layer = 0;
                    final Map<Location, org.bukkit.block.BlockState> oldBlocks = new HashMap<>();

                    @Override
                    public void run() {
                        if (layer > size) { // Box complete
                            this.cancel();
                            
                            // 2. 10 Seconds ka Wait aur Darkness Effect
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    // 3. Box Hatane ka Logic (3 Seconds)
                                    logic.removeBoxGradually(center, size, oldBlocks);

                                }
                            }.runTaskLater(plugin, durationTicks);
                            return;
                        }

                        // Ek-ek karke walls aur roof banana (Animation)
                        for (int x = -size; x <= size; x++) {
                            for (int y = 0; y <= size; y++) {
                                for (int z = -size; z <= size; z++) {
                                    // Check agar ye boundary (wall/floor/roof) hai
                                    if (Math.abs(x) == size || y == 0 || y == size || Math.abs(z) == size) {
                                        Location bLoc = center.clone().add(x, y, z);
                                        // Sirf agar hawa hai ya replaceable block hai
                                        if (bLoc.getBlock().getType() == Material.AIR || bLoc.getBlock().isPassable()) {
                                            if (!oldBlocks.containsKey(bLoc)) {
                                                oldBlocks.put(bLoc, bLoc.getBlock().getState());
                                                bLoc.getBlock().setType(Material.OBSIDIAN);
                                                
                                                // Sound aur Particle
                                                if (layer % 2 == 0) {
                                                    bLoc.getWorld().playSound(bLoc, Sound.BLOCK_STONE_PLACE, 0.5f, 0.5f);
                                                    bLoc.getWorld().spawnParticle(Particle.PORTAL, bLoc, 2, 0.1, 0.1, 0.1, 0.05);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Enamy ko Darkness dena (Box ke andar)
                        for (Entity e : center.getWorld().getNearbyEntities(center, size, size, size)) {
                            if (e instanceof Player target && !target.equals(p)) {
                                target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 40, 1, false, false));
                                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 1, false, false));
                            }
                        }
                        
                        layer++;
                    }
                }.runTaskTimer(plugin, 0L, 3L); // 3 ticks delay per layer (approx 3s total)
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
                if (target == null) { p.sendMessage("§ctarget!"); return; }
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
                p.sendMessage("\u00a7b\u00a7lICE SHIELD", "\u00a77Protected for 5 seconds!", 5, 60, 10);
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

    // Effects
    p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 100, 2));
    p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 254));
    p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 100, 1));

    // ✅ IMPORTANT — fake "giant power"
    p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(10.0);
    p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_KNOCKBACK).setBaseValue(2.0);

    // ✅ Paper ONLY (agar support karta hai to player bada dikhega)
    try {
        p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_SCALE).setBaseValue(4.0); // 1.0 = normal
    } catch (Exception ignored) {}

    p.setMetadata("TitanMode", new org.bukkit.metadata.FixedMetadataValue(plugin, true));

    p.sendMessage("§6§lTITAN FORM", "§eYou are Unstoppable!", 5, 40, 5);

    new BukkitRunnable() {
        int ticks = 0;

        public void run() {
            if (!p.isOnline() || ticks >= 100) {

                // RESET
                p.removeMetadata("TitanMode", plugin);

                try {
                    p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_SCALE).setBaseValue(1.0);
                } catch (Exception ignored) {}

                p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(1.0);
                p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_KNOCKBACK).setBaseValue(0.0);

                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_IRON_GOLEM_DEATH, 1f, 1.5f);
                p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation(), 50, 1, 2, 1, 0.1);

                p.sendMessage("§cTitan Form expired.");
                cancel();
                return;
            }

            ticks++;
            Location cur = p.getLocation();

            // Visual heavy effect
            if (ticks % 2 == 0) {
                w.spawnParticle(Particle.DUST_PLUME, cur, 20, 1.5, 0.2, 1.5, 0.05, Material.STONE.createBlockData());
                w.spawnParticle(Particle.DUST, cur, 15, 2.0, 3.0, 2.0, 0,
                        new Particle.DustOptions(Color.fromRGB(120, 80, 40), 2.5f));
            }

            // Smash effect
            if (ticks % 10 == 0) {
                w.playSound(cur, Sound.BLOCK_ANVIL_LAND, 0.7f, 0.5f);

                p.getNearbyEntities(6, 4, 6).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {

                        Vector push = le.getLocation().toVector()
                                .subtract(cur.toVector())
                                .normalize()
                                .multiply(1.5)
                                .setY(0.5);

                        le.setVelocity(push);
                        le.damage(3.0 * titanDm, p);
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


// ── WIND PRIMARY — Ancient Eagle Spirit ──────────────────────────────────────
// Owner ke peeche large white eagle particle structure
// Wings animate up/down — each flap = shockwave + damage
// 6 seconds, no armor stands, pure particles, no glitch
case WIND -> {
    // ── Direction setup ───────────────────────────────────────────────────
    final Vector fwd   = p.getLocation().getDirection().clone().setY(0).normalize();
    final Vector right = new Vector(-fwd.getZ(), 0, fwd.getX()).normalize();
    // Eagle base: 1 block BEHIND owner, at ground level
    final Location eagleBase = loc.clone().subtract(fwd.clone().multiply(2));
    eagleBase.setY(loc.getY());

    p.sendMessage("\u00a7f\u00a7lWIND EAGLE", "\u00a77Ancient guardian rises...", 5, 50, 10);
    w.playSound(eagleBase, Sound.ENTITY_PHANTOM_FLAP,        1.5f, 0.25f);
    w.playSound(eagleBase, Sound.ENTITY_ENDER_DRAGON_FLAP,   1f,   0.4f);
    w.playSound(eagleBase, Sound.ENTITY_PHANTOM_AMBIENT,     1f,   0.3f);
    w.spawnParticle(Particle.CLOUD, eagleBase.clone().add(0,4,0), 30, 1.5,2,1.5, 0.12);

    new BukkitRunnable() {
        int    ticks    = 0;
        double wingAng  = 0;
        double prevWing = 0;

        // ── Draw point at right/up/fwd offset from eagleBase ─────────────
        void P(double r, double u, double f,
               Particle.DustOptions d) {
            Location l = eagleBase.clone()
                .add(right.getX()*r + fwd.getX()*f,
                     u,
                     right.getZ()*r + fwd.getZ()*f);
            w.spawnParticle(Particle.DUST, l, 1, 0,0,0,0, d);
        }
        void G(double r, double u, double f) { // END_ROD glow
            Location l = eagleBase.clone()
                .add(right.getX()*r + fwd.getX()*f,
                     u,
                     right.getZ()*r + fwd.getZ()*f);
            w.spawnParticle(Particle.END_ROD, l, 1, 0,0,0,0);
        }

        @Override
        public void run() {
            if (ticks++ >= 120 || !p.isOnline()) {
                w.playSound(eagleBase, Sound.ENTITY_PHANTOM_DEATH, 1f, 0.5f);
                w.spawnParticle(Particle.CLOUD, eagleBase.clone().add(0,4,0), 40, 2,2,2, 0.08);
                cancel(); return;
            }

            Particle.DustOptions W  = new Particle.DustOptions(Color.fromRGB(240,250,255), 1.8f);
            Particle.DustOptions W2 = new Particle.DustOptions(Color.fromRGB(190,230,255), 1.5f);
            Particle.DustOptions W3 = new Particle.DustOptions(Color.fromRGB(150,210,255), 1.3f);

            // Wing oscillation — smooth sine wave
            prevWing = wingAng;
            wingAng  = Math.sin(ticks * 0.22) * 0.75; // ±43 degrees
            boolean flap = (prevWing >= 0 && wingAng < 0); // peak → down = flap

            // ════════════════════════════════════════════════════════════
            // EAGLE BODY — center vertical oval (y 2.0 to 6.5)
            // ════════════════════════════════════════════════════════════
            for (int i = 0; i < 20; i++) {
                double t  = i / 19.0;
                double by = 2.0 + t * 4.5;
                double br = 1.3 * Math.sin(t * Math.PI); // oval
                for (int j = 0; j < 10; j++) {
                    double a  = Math.toRadians(j * 36);
                    double rx = Math.cos(a) * br;
                    double fz = Math.sin(a) * br * 0.55;
                    P(rx, by, fz, W);
                }
            }
            // Body center spine
            for (double y = 2.0; y <= 6.5; y += 0.5) {
                P(0, y, 0, W);
            }

            // ════════════════════════════════════════════════════════════
            // HEAD — sphere at y=8.0, face forward
            // ════════════════════════════════════════════════════════════
            for (int i = 0; i < 20; i++) {
                double a  = Math.toRadians(i * 18);
                double hr = 1.6;
                // Horizontal ring
                P(Math.cos(a)*hr,       8.0,            Math.sin(a)*hr*0.4, W);
                // Vertical ring front
                P(Math.cos(a)*hr*0.5,   8.0+Math.sin(a)*hr*0.6, Math.sin(a)*hr*0.3, W);
            }
            // Eye sockets + glowing eyes
            P( 0.7, 8.3, 1.0, W2); P( 0.7, 8.3, 1.0, W2);
            P(-0.7, 8.3, 1.0, W2); P(-0.7, 8.3, 1.0, W2);
            G( 0.6, 8.4, 1.1);
            G(-0.6, 8.4, 1.1);
            // Forehead ridge
            for (double r = -0.8; r <= 0.8; r += 0.4) P(r, 9.0, 0.3, W);
            for (double r = -0.6; r <= 0.6; r += 0.4) P(r, 9.3, 0.0, W);
            // Beak — forward taper
            for (int k = 0; k <= 5; k++) {
                double bf = 0.5 + k*0.3;
                double bw = 0.4 - k*0.07;
                P( bw, 7.9-k*0.07, bf, W);
                P(-bw, 7.9-k*0.07, bf, W);
                P(0,   7.7-k*0.07, bf, W);
            }
            // Beak hook at tip
            P(0, 7.4, 2.1, W); P(0, 7.2, 2.0, W);
            // Feather crown on head
            for (int i = 0; i < 7; i++) {
                double a   = Math.toRadians(-60 + i*20);
                double len = 1.2 + Math.sin(ticks*0.15 + i*0.5)*0.2; // subtle sway
                for (double k = 0.2; k <= len; k += 0.25) {
                    P(Math.sin(a)*k*0.7, 9.0+k*0.9, Math.cos(a)*k*0.3, W2);
                }
            }

            // ════════════════════════════════════════════════════════════
            // NECK — connect head to body
            // ════════════════════════════════════════════════════════════
            for (int i = 0; i < 8; i++) {
                double t  = i / 7.0;
                double ny = 6.5 + t * 1.3;
                double nr = 1.1 - t*0.4;
                for (int j = 0; j < 8; j++) {
                    double a = Math.toRadians(j*45);
                    P(Math.cos(a)*nr, ny, Math.sin(a)*nr*0.4, W);
                }
            }

            // ════════════════════════════════════════════════════════════
            // WINGS — animated, both sides
            // wingAng = current angle (positive = up, negative = down)
            // ════════════════════════════════════════════════════════════
            for (int side = -1; side <= 1; side += 2) {
                // Wing has 16 segments from body to tip
                int SEGS = 16;
                for (int seg = 1; seg <= SEGS; seg++) {
                    double t = seg / (double)SEGS;

                    // Horizontal extent
                    double rx = side * seg * 0.65;

                    // Wing height: tip affected more by angle
                    double wh = Math.sin(wingAng) * t * 4.5;

                    // Wing sweep back (trailing edge)
                    double sweep = -t * 2.0;

                    double wy = 5.5 + wh;

                    // Leading edge (top of wing)
                    Particle.DustOptions col = (t > 0.7) ? W3 : (t > 0.4) ? W2 : W;
                    P(rx, wy,          sweep,       col);
                    P(rx, wy + 0.15,   sweep + 0.1, col);

                    // Primary feathers — hanging from leading edge
                    int featherCount = Math.max(1, 4 - (int)(t*3));
                    for (int f = 1; f <= featherCount; f++) {
                        double fdy = -f * 0.45;
                        double fdz = sweep - f * 0.5;
                        P(rx, wy + fdy, fdz, col);
                        // Feather tip glow (outer primaries only)
                        if (t > 0.5 && f == featherCount) G(rx, wy + fdy, fdz);
                    }

                    // Secondary feathers (inner half only)
                    if (seg <= 8) {
                        for (int sf = 1; sf <= 3; sf++) {
                            P(rx, wy - sf*0.3, sweep - sf*0.25, W2);
                        }
                    }

                    // Wing membrane fill (dots between segments)
                    if (seg > 1) {
                        double prevRx = side * (seg-1) * 0.65;
                        double midRx  = (rx + prevRx) / 2.0;
                        P(midRx, wy, sweep - 0.3, W2);
                    }
                }

                // Wing root shoulder
                for (int k = 0; k < 4; k++) {
                    P(side*(0.4+k*0.25), 5.5+k*0.1, -k*0.1, W);
                }
            }

            // ════════════════════════════════════════════════════════════
            // TAIL — fan behind body
            // ════════════════════════════════════════════════════════════
            for (int i = 0; i < 9; i++) {
                double ta = Math.toRadians(-40 + i*10);
                for (double tl = 0.3; tl <= 2.8; tl += 0.35) {
                    double tailWave = Math.sin(ticks*0.2) * 0.15 * tl;
                    P(Math.sin(ta)*tl, 2.5 - tl*0.15 + tailWave, -tl*0.9, W2);
                }
            }
            // Tail glow tips
            for (int i = 0; i < 5; i++) {
                double ta = Math.toRadians(-20 + i*10);
                G(Math.sin(ta)*2.8, 2.0, -2.5);
            }

            // ════════════════════════════════════════════════════════════
            // LEGS — from body bottom to ground
            // ════════════════════════════════════════════════════════════
            for (int side = -1; side <= 1; side += 2) {
                // Upper thigh
                for (int k = 0; k <= 5; k++) {
                    double ly = 2.2 - k*0.38;
                    double lr = 0.5 + k*0.04;
                    P(side*lr, ly, k*0.08, W);
                }
                // Lower leg + ankle
                P(side*0.6, 0.7, 0.35, W);
                P(side*0.55, 0.45, 0.5, W);
                P(side*0.5, 0.2, 0.6, W);
                // Talons — 3 forward, 1 back
                double[][] talons = {{0.5,0.0,1.0},{0.8,0.0,0.7},{0.2,0.0,0.9},{0.5,0.0,-0.3}};
                for (double[] t : talons) {
                    for (double k = 0; k <= 1.0; k += 0.3) {
                        P(side*(t[0] - k*(t[0]-0.5)*0.5), t[1], t[2]*k + 0.6*(1-k), W2);
                    }
                }
            }

            // ════════════════════════════════════════════════════════════
            // WING FLAP → SHOCKWAVE
            // ════════════════════════════════════════════════════════════
            if (flap) {
                w.playSound(eagleBase, Sound.ENTITY_PHANTOM_FLAP,      1.5f, 0.3f);
                w.playSound(eagleBase, Sound.ENTITY_ENDER_DRAGON_FLAP, 1f,   0.5f);
                w.playSound(eagleBase, Sound.ENTITY_PHANTOM_BITE,      0.8f, 0.6f);

                // 3 expanding rings shooting forward
                for (int ring = 1; ring <= 4; ring++) {
                    final int fr = ring;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        double dist = fr * 2.5;
                        Location wCenter = eagleBase.clone()
                            .add(fwd.getX()*dist, 4.0, fwd.getZ()*dist);
                        double rSize = 1.5 + fr * 0.8;
                        int pts = 28;
                        for (int i = 0; i < pts; i++) {
                            double a = Math.toRadians(i * (360.0/pts));
                            Location wl = wCenter.clone()
                                .add(right.getX()*Math.cos(a)*rSize,
                                     Math.sin(a)*rSize,
                                     right.getZ()*Math.cos(a)*rSize);
                            w.spawnParticle(Particle.DUST, wl, 1, 0.05,0.05,0.05, 0,
                                new Particle.DustOptions(Color.fromRGB(200,245,255), 1.8f));
                            w.spawnParticle(Particle.END_ROD, wl, 1, 0.03,0.03,0.03, 0.01);
                        }
                        w.playSound(wCenter, Sound.BLOCK_GLASS_BREAK, 0.5f, 1.8f);
                    }, fr * 2L);
                }

                // Damage enemies forward
                Location atkLoc = eagleBase.clone().add(fwd.getX()*8, 3, fwd.getZ()*8);
                for (org.bukkit.entity.Entity e : w.getNearbyEntities(atkLoc, 10, 6, 10)) {
                    if (!(e instanceof LivingEntity le)) continue;
                    if (e.equals(p)) continue;
                    if (e instanceof Player ep &&
                        (ep.getGameMode()==GameMode.CREATIVE||ep.getGameMode()==GameMode.SPECTATOR)) continue;
                    le.damage(4.0 * dm, p);
                    Vector knock = fwd.clone().multiply(2.5).setY(0.6);
                    Bukkit.getScheduler().runTaskLater(plugin,
                        () -> { if (e.isValid()) e.setVelocity(knock); }, 1L);
                }
            }

            // Ambient wind swirls
            if (ticks % 4 == 0) {
                double ra = Math.random()*Math.PI*2;
                double rr = 2 + Math.random()*4;
                double ry = 1 + Math.random()*7;
                Location al = eagleBase.clone()
                    .add(right.getX()*Math.cos(ra)*rr + fwd.getX()*Math.sin(ra)*rr,
                         ry,
                         right.getZ()*Math.cos(ra)*rr + fwd.getZ()*Math.sin(ra)*rr);
                w.spawnParticle(Particle.CLOUD, al, 1, 0.1,0.1,0.1, 0.03);
            }

            // Eagle cry every 2s
            if (ticks % 40 == 0) {
                w.playSound(eagleBase, Sound.ENTITY_PHANTOM_AMBIENT, 0.9f, 0.35f);
            }
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

            // ── CRYSTAL SECONDARY — Magic Circle + Healing Light ─────────────────────────
// Photo jaisa cyan magic circle ground par, owner center mein
// Outer ring + inner rings + star + triangles + satellites + spokes
// Sun/divine light from above — 5s, heals HP + hunger + repairs armor
// Nobody can enter 3x3 radius, no damage from outside
case CRYSTAL -> {
    final Location center = p.getLocation().clone();
    final boolean[] active = {true};

    w.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 2f, 0.6f);
    w.playSound(center, Sound.BLOCK_BEACON_ACTIVATE,      1f, 1.2f);
    w.playSound(center, Sound.ENTITY_PLAYER_LEVELUP,      1f, 0.8f);
    p.sendMessage("\u00a7b\u00a7lANCIENT SEAL",
        "\u00a77The circle protects you...", 5, 80, 10);

    // ── Heal + armor repair immediately ───────────────────────────────────
    p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + p.getMaxHealth()));
    p.setFoodLevel(20);
    p.setSaturation(20f);
    // Repair armor
    for (org.bukkit.inventory.ItemStack item : p.getInventory().getArmorContents()) {
        if (item == null || item.getType().isAir()) continue;
        if (item.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable d) {
            d.setDamage(0);
            item.setItemMeta((org.bukkit.inventory.meta.ItemMeta) d);
        }
    }
    // Resistance + shield
    p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 4, false, false, false));

    new BukkitRunnable() {
        int    ticks = 0;
        double spin  = 0;

        // ── Ring draw helper (photo: cyan DUST) ───────────────────────────
        void ring(double r, int pts, double off) {
            Particle.DustOptions C1 = new Particle.DustOptions(Color.fromRGB(0,220,230), 1.6f);
            Particle.DustOptions C2 = new Particle.DustOptions(Color.fromRGB(80,240,255), 1.4f);
            for (int i = 0; i < pts; i++) {
                double a = (Math.PI*2/pts)*i + off;
                Location pt = center.clone().add(Math.cos(a)*r, 0.05, Math.sin(a)*r);
                w.spawnParticle(Particle.DUST, pt, 1, 0,0,0,0, C1);
                if (i % 3 == 0)
                    w.spawnParticle(Particle.DUST, pt, 1, 0.03,0,0.03,0, C2);
            }
        }

        // ── Line from a to b ──────────────────────────────────────────────
        void line(Location a, Location b, int steps) {
            Particle.DustOptions CL = new Particle.DustOptions(Color.fromRGB(0,210,220), 1.4f);
            for (int i = 0; i <= steps; i++) {
                double t = i / (double)steps;
                Location pt = new Location(w,
                    a.getX() + (b.getX()-a.getX())*t,
                    0.05 + center.getY(),
                    a.getZ() + (b.getZ()-a.getZ())*t);
                w.spawnParticle(Particle.DUST, pt, 1, 0,0,0,0, CL);
            }
        }

        // ── Small circle at satellite point ──────────────────────────────
        void sat(double cx, double cz, double r, int pts, double off, int type) {
            Particle.DustOptions CS = new Particle.DustOptions(
                type == 0 ? Color.fromRGB(0,220,230) : Color.fromRGB(100,255,255), 1.3f);
            for (int i = 0; i < pts; i++) {
                double a = (Math.PI*2/pts)*i + off;
                Location pt = center.clone().add(cx + Math.cos(a)*r, 0.05, cz + Math.sin(a)*r);
                w.spawnParticle(Particle.DUST, pt, 1, 0,0,0,0, CS);
            }
        }

        public void run() {
            ticks++;
            if (ticks > 100 || !p.isOnline()) { // 5s
                active[0] = false;
                // Remove effects
                w.spawnParticle(Particle.END_ROD, center.clone().add(0,0.1,0),
                    30, 4,0.1,4, 0.05);
                w.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1f, 1.8f);
                cancel(); return;
            }

            spin += 0.018; // slow smooth spin

            // ── Block entities from entering ──────────────────────────────
            for (org.bukkit.entity.Entity e : w.getNearbyEntities(center, 3.5, 3, 3.5)) {
                if (e.equals(p)) continue;
                if (!(e instanceof LivingEntity)) continue;
                // Push away
                Vector push = e.getLocation().toVector()
                    .subtract(center.toVector()).setY(0);
                if (push.lengthSquared() < 0.01) push = new Vector(1,0,0);
                e.setVelocity(push.normalize().multiply(0.8).setY(0.1));
            }

            // ── Continuous heal every 2s ──────────────────────────────────
            if (ticks % 40 == 0) {
                p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 4));
                p.setFoodLevel(20);
            }

            // ══════════════════════════════════════════════════════════════
            // PHOTO JAISI MAGIC CIRCLE STRUCTURE
            // All rings flat on ground (y=0.05)
            // ══════════════════════════════════════════════════════════════

            // Outer ring (biggest) — 2 concentric
            ring(12.0, 200, spin);
            ring(11.3, 190, -spin*0.8);
            ring(10.5, 180,  spin*0.6);

            // Tick marks on outer ring (photo: segments between beads)
            for (int i = 0; i < 40; i++) {
                double a = (Math.PI*2/40)*i + spin;
                double r1 = 10.7, r2 = 11.0;
                Location p1 = center.clone().add(Math.cos(a)*r1, 0.05, Math.sin(a)*r1);
                Location p2 = center.clone().add(Math.cos(a)*r2, 0.05, Math.sin(a)*r2);
                line(p1, p2, 3);
            }
            // Bead dots on outer ring
            for (int i = 0; i < 60; i++) {
                double a = (Math.PI*2/60)*i - spin;
                Location bead = center.clone().add(Math.cos(a)*11.6, 0.05, Math.sin(a)*11.6);
                w.spawnParticle(Particle.END_ROD, bead, 1, 0.01,0,0.01, 0.001);
            }

            // Mid ring
            ring(8.5, 140,  spin*1.2);
            ring(7.8, 128, -spin*1.1);

            // Inner rings
            ring(5.5, 100,  spin*1.5);
            ring(4.8,  88, -spin*1.4);
            ring(4.0,  75,  spin*1.8);

            // Innermost
            ring(2.5,  55,  spin*2.0);
            ring(1.8,  40, -spin*2.5);
            ring(1.0,  25,  spin*3.0);

            // ── 8-pointed star (photo: big star inside mid ring) ──────────
            int starPts = 8;
            for (int i = 0; i < starPts; i++) {
                double a1 = (Math.PI*2/starPts)*i + spin;
                double a2 = (Math.PI*2/starPts)*((i+2)%starPts) + spin; // skip-2
                double a3 = (Math.PI*2/starPts)*((i+3)%starPts) + spin; // skip-3
                Location from = center.clone().add(Math.cos(a1)*7.5, 0.05, Math.sin(a1)*7.5);
                Location to2  = center.clone().add(Math.cos(a2)*7.5, 0.05, Math.sin(a2)*7.5);
                Location to3  = center.clone().add(Math.cos(a3)*7.5, 0.05, Math.sin(a3)*7.5);
                line(from, to2, 20);
                line(from, to3, 20);
            }

            // ── Inner triangles (photo: 2 triangles = hexagram inner) ─────
            for (int tri = 0; tri < 2; tri++) {
                double triOff = tri == 0 ? spin : -spin + Math.PI/3;
                for (int i = 0; i < 3; i++) {
                    double a1 = (Math.PI*2/3)*i + triOff;
                    double a2 = (Math.PI*2/3)*((i+1)%3) + triOff;
                    Location from = center.clone().add(Math.cos(a1)*4.5, 0.05, Math.sin(a1)*4.5);
                    Location to   = center.clone().add(Math.cos(a2)*4.5, 0.05, Math.sin(a2)*4.5);
                    line(from, to, 14);
                }
            }

            // ── Spokes — outer to inner (photo: 8 spokes) ────────────────
            for (int i = 0; i < 8; i++) {
                double a = (Math.PI*2/8)*i + spin;
                Location inner = center.clone().add(Math.cos(a)*2.6, 0.05, Math.sin(a)*2.6);
                Location outer = center.clone().add(Math.cos(a)*7.6, 0.05, Math.sin(a)*7.6);
                line(inner, outer, 15);
            }

            // ── Satellites on outer ring (photo: 4 swirl + 4 snowflake) ──
            for (int i = 0; i < 8; i++) {
                double a  = (Math.PI*2/8)*i + spin;
                double sx = Math.cos(a)*9.0;
                double sz = Math.sin(a)*9.0;

                if (i % 2 == 0) {
                    // Swirl satellite (3 concentric rings)
                    sat(sx, sz, 0.9, 18, -spin*2.5, 0);
                    sat(sx, sz, 0.55, 12,  spin*3.0, 0);
                    sat(sx, sz, 0.22,  8,  spin*4.0, 1);
                    // Center dot
                    w.spawnParticle(Particle.END_ROD,
                        center.clone().add(sx, 0.05, sz), 1, 0.02,0,0.02, 0.001);
                } else {
                    // Snowflake satellite (6 spokes)
                    for (int sp = 0; sp < 6; sp++) {
                        double sa = (Math.PI/3)*sp + spin*2;
                        Location sc = center.clone().add(sx, 0.05, sz);
                        for (double r = 0.15; r <= 0.85; r += 0.18) {
                            Location sp1 = sc.clone().add(Math.cos(sa)*r, 0, Math.sin(sa)*r);
                            w.spawnParticle(Particle.DUST, sp1, 1, 0,0,0,0,
                                new Particle.DustOptions(Color.fromRGB(150,240,255), 1.2f));
                        }
                        // Branch marks
                        for (double r = 0.35; r <= 0.65; r += 0.25) {
                            double ba = sa + Math.PI/6;
                            Location br = sc.clone().add(Math.cos(sa)*r + Math.cos(ba)*0.2, 0,
                                                          Math.sin(sa)*r + Math.sin(ba)*0.2);
                            w.spawnParticle(Particle.DUST, br, 1, 0,0,0,0,
                                new Particle.DustOptions(Color.fromRGB(150,240,255), 1.1f));
                        }
                    }
                }
            }

            // ── Connection dots (photo: small circles on star vertices) ───
            for (int i = 0; i < 8; i++) {
                double a = (Math.PI*2/8)*i + spin;
                sat(Math.cos(a)*7.5, Math.sin(a)*7.5, 0.3, 8, -spin*3, 1);
            }
            // Inner connection dots
            for (int i = 0; i < 3; i++) {
                double a = (Math.PI*2/3)*i + spin;
                sat(Math.cos(a)*4.5, Math.sin(a)*4.5, 0.25, 7, spin*3, 1);
            }

            // ── CENTER SNOWFLAKE (photo: large snowflake in center) ───────
            for (int sp = 0; sp < 12; sp++) {
                double sa = (Math.PI/6)*sp + spin*1.5;
                for (double r = 0.1; r <= 1.5; r += 0.15) {
                    w.spawnParticle(Particle.DUST,
                        center.clone().add(Math.cos(sa)*r, 0.05, Math.sin(sa)*r),
                        1, 0,0,0,0,
                        new Particle.DustOptions(Color.fromRGB(0,220,230), 1.4f));
                }
                if (sp % 2 == 0) {
                    for (double r = 0.5; r <= 1.0; r += 0.45) {
                        double ba = sa + Math.PI/8;
                        w.spawnParticle(Particle.DUST,
                            center.clone().add(
                                Math.cos(sa)*r + Math.cos(ba)*0.25, 0.05,
                                Math.sin(sa)*r + Math.sin(ba)*0.25),
                            1,0,0,0,0,
                            new Particle.DustOptions(Color.fromRGB(100,240,255), 1.2f));
                    }
                }
            }

            // ── DIVINE LIGHT — sun beam from above ────────────────────────
            // Column of golden/white light falling on owner
            if (ticks % 2 == 0) {
                for (double y = 0.5; y <= 15; y += 0.8) {
                    double spread = (y / 15.0) * 1.5; // wider at top
                    double a = Math.random()*Math.PI*2;
                    double r = Math.random()*spread;
                    Location lp = center.clone().add(
                        Math.cos(a)*r, y, Math.sin(a)*r);
                    // Alternating gold/white
                    Color lc = (ticks + (int)(y*3)) % 4 < 2
                        ? Color.fromRGB(255, 240, 120)
                        : Color.fromRGB(255, 255, 220);
                    w.spawnParticle(Particle.DUST, lp, 1, 0.05,0,0.05,0,
                        new Particle.DustOptions(lc, 1.8f));
                }
                // Outer light ring at ground — 3x3 radius = 1.5 blocks
                for (int i = 0; i < 20; i++) {
                    double a = (Math.PI*2/20)*i + spin*2;
                    Location lr = center.clone().add(Math.cos(a)*1.5, 0.3, Math.sin(a)*1.5);
                    w.spawnParticle(Particle.END_ROD, lr, 1, 0.03,0.05,0.03, 0.002);
                }
            }

            // Glowing ground under owner
            if (ticks % 3 == 0) {
                w.spawnParticle(Particle.END_ROD,
                    center.clone().add(0, 0.1, 0), 2, 1.5,0.05,1.5, 0.01);
            }

            // Sound pulse
            if (ticks % 20 == 0) {
                w.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.0f + (ticks/100f));
            }
        }
    }.runTaskTimer(plugin, 0, 1);
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
                p.sendMessage("§3§lSONAR PULSE", "§7All enemies revealed!", 5, 50, 10);
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
                p.sendMessage("§c§l⚡ RAGE MODE!", "§7+Haste III  +Strength  +Speed", 5, 50, 10);
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
                p.sendMessage("§a§l✦ SPIRIT HEAL", "§7Health Restored!", 5, 45, 10);
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
                p.sendMessage("§d§lTIME SLOW", "§7Enemies frozen in time!", 5, 100, 10);
            }

            // ── WARRIOR SECONDARY — Spirit Swords ─────────────────────────────────────────
// 3 swords (L:2 + Head:1) form 3s, hold 4s with blood drip, then track+hit enemy
// Aim-lock on target — tracks even if fleeing, ignores other entities
case WARRIOR -> {
    // ── Aim lock target ───────────────────────────────────────────────────
    LivingEntity locked = logic.aim(p, 40);
    if (locked == null) {
        p.sendMessage("\u00a7cNo target to lock!");
        return;
    }
    final LivingEntity target = locked;

    p.sendMessage("\u00a76\u00a7lSWORD SUMMON", "\u00a77Blades forming...", 5, 40, 10);
    w.playSound(loc, Sound.BLOCK_ANVIL_LAND,         1f, 1.5f);
    w.playSound(loc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 0.5f);

    // ── Sword draw helper ─────────────────────────────────────────────────
    // Sword = handle (back) + guard (cross) + blade (front)
    // origin = sword center, fwd = blade direction, up = up direction
    // Returns: draws sword particles at given pose

    // Phase state
    final int[] phase = {0}; // 0=forming, 1=hold, 2=shoot
    final boolean[] hit = {false, false, false};

    // Sword world positions (updated each tick)
    // Each sword: Location center, Vector bladeDir
    // 3 swords: [0]=left-low [1]=left-high [2]=head
    final Location[] swordLoc = {
        loc.clone(), loc.clone(), loc.clone()
    };
    final boolean[] swordAlive = {true, true, true};

    new BukkitRunnable() {
        int  ticks    = 0;
        double wave   = 0;
        double spin   = 0;

        // ── Draw one sword ────────────────────────────────────────────────
        void drawSword(Location center, Vector fwd, Vector up, float progress) {
            // progress 0.0→1.0 (forming animation)
            Vector right2 = fwd.clone().crossProduct(up).normalize();
            Particle.DustOptions BLADE   = new Particle.DustOptions(Color.fromRGB(200,220,255), 1.6f);
            Particle.DustOptions GUARD   = new Particle.DustOptions(Color.fromRGB(180,150, 80), 1.5f);
            Particle.DustOptions HANDLE  = new Particle.DustOptions(Color.fromRGB(120, 70, 30), 1.4f);
            Particle.DustOptions GLOW    = new Particle.DustOptions(Color.fromRGB(255,240,200), 1.2f);

            // Blade length determined by progress
            double bladeLen = 2.2 * progress;
            int bPts = Math.max(1, (int)(bladeLen / 0.22));
            for (int i = 0; i <= bPts; i++) {
                double t = i / (double)bPts;
                double taper = 1.0 - t * 0.7; // taper to tip
                Location bp = center.clone().add(
                    fwd.getX()*t*bladeLen,
                    up.getY()*0 + fwd.getY()*t*bladeLen,
                    fwd.getZ()*t*bladeLen);
                w.spawnParticle(Particle.DUST, bp, 1, 0,0,0,0, BLADE);
                // Blade edge glow
                if (i % 2 == 0)
                    w.spawnParticle(Particle.END_ROD, bp, 1, 0.02,0.02,0.02, 0.001);
                // Blade width (flat sword shape)
                if (taper > 0.3) {
                    Location bp2 = bp.clone().add(right2.getX()*taper*0.18,
                                                   right2.getY()*taper*0.18,
                                                   right2.getZ()*taper*0.18);
                    Location bp3 = bp.clone().subtract(right2.getX()*taper*0.18,
                                                        right2.getY()*taper*0.18,
                                                        right2.getZ()*taper*0.18);
                    w.spawnParticle(Particle.DUST, bp2, 1, 0,0,0,0, BLADE);
                    w.spawnParticle(Particle.DUST, bp3, 1, 0,0,0,0, BLADE);
                }
            }

            if (progress < 0.4) return; // guard + handle only after partial form

            // Guard — cross perpendicular
            for (int k = -3; k <= 3; k++) {
                if (k == 0) continue;
                Location gp = center.clone().add(
                    right2.getX()*k*0.2, right2.getY()*k*0.2, right2.getZ()*k*0.2);
                w.spawnParticle(Particle.DUST, gp, 1, 0,0,0,0, GUARD);
            }
            // Guard depth
            for (int k = -2; k <= 2; k++) {
                Location gp = center.clone().add(
                    up.getX()*k*0.15 - fwd.getX()*0.1,
                    up.getY()*k*0.15 - fwd.getY()*0.1,
                    up.getZ()*k*0.15 - fwd.getZ()*0.1);
                w.spawnParticle(Particle.DUST, gp, 1, 0,0,0,0, GUARD);
            }

            if (progress < 0.7) return;

            // Handle
            for (int k = 1; k <= 4; k++) {
                Location hp = center.clone().subtract(
                    fwd.getX()*k*0.22, fwd.getY()*k*0.22, fwd.getZ()*k*0.22);
                w.spawnParticle(Particle.DUST, hp, 1, 0,0,0,0, HANDLE);
            }
            // Pommel
            Location pommel = center.clone().subtract(
                fwd.getX()*1.0, fwd.getY()*1.0, fwd.getZ()*1.0);
            w.spawnParticle(Particle.DUST, pommel, 2, 0.05,0.05,0.05, 0, GLOW);
        }

        @Override
        public void run() {
            ticks++;
            wave += 0.08;
            spin += 0.04;

            if (!p.isOnline() || (!target.isValid() && phase[0] < 2)) {
                cancel(); return;
            }

            // ── Player look direction (horizontal) ────────────────────────
            Vector pFwd   = p.getLocation().getDirection().clone().normalize();
            Vector pRight = new Vector(-pFwd.getZ(), 0, pFwd.getX()).normalize();
            Vector pUp    = new Vector(0, 1, 0);
            Location pLoc = p.getLocation().clone().add(0, 1.0, 0);

            // ── PHASE 0: FORMING (0-60 ticks = 3s) ───────────────────────
            if (phase[0] == 0) {
                float prog = ticks / 60f;

                // Sword positions: Left-Low, Left-High, Head
                double wv = Math.sin(wave) * 0.15 * prog;

                Location sL1 = pLoc.clone()
                    .add(pRight.clone().multiply(-1.5))
                    .add(0, -0.3 + wv, 0);
                Location sL2 = pLoc.clone()
                    .add(pRight.clone().multiply(-1.5))
                    .add(0,  0.7 + wv, 0);
                Location sH  = pLoc.clone().add(0, 1.5 + wv, 0);

                swordLoc[0] = sL1; swordLoc[1] = sL2; swordLoc[2] = sH;

                // Spin during formation
                double spinAngle = spin * 2;
                Vector spinFwd = new Vector(
                    Math.cos(spinAngle)*pFwd.getX() - Math.sin(spinAngle)*pFwd.getZ(),
                    pFwd.getY(),
                    Math.sin(spinAngle)*pFwd.getX() + Math.cos(spinAngle)*pFwd.getZ()).normalize();

                drawSword(sL1, spinFwd, pUp, Math.min(1f, prog * 2));
                drawSword(sL2, spinFwd, pUp, Math.min(1f, prog * 2));
                drawSword(sH,  spinFwd, pUp, Math.min(1f, prog * 2));

                // Formation sparks
                if (ticks % 5 == 0) {
                    for (Location sl : new Location[]{sL1, sL2, sH}) {
                        w.spawnParticle(Particle.CRIT, sl, 3, 0.3,0.3,0.3, 0.08);
                        w.spawnParticle(Particle.END_ROD, sl, 1, 0.2,0.2,0.2, 0.02);
                    }
                    w.playSound(p.getLocation(), Sound.BLOCK_ANVIL_USE, 0.4f, 1.8f);
                }

                if (ticks >= 60) {
                    phase[0] = 1;
                    w.playSound(p.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1f, 1.2f);
                    p.sendTitle("\u00a76\u00a7lBLADES READY", "\u00a77Unleashing...", 3, 60, 10);
                }

            // ── PHASE 1: HOLD (60-140 ticks = 4s) ────────────────────────
            } else if (phase[0] == 1) {
                int holdTick = ticks - 60;

                double wv = Math.sin(wave) * 0.25; // gentle wave
                // Slow spin during hold
                Vector holdFwd = new Vector(
                    Math.cos(spin)*pFwd.getX() - Math.sin(spin)*pFwd.getZ(),
                    pFwd.getY(),
                    Math.sin(spin)*pFwd.getX() + Math.cos(spin)*pFwd.getZ()).normalize();

                Location sL1 = pLoc.clone().add(pRight.clone().multiply(-1.5)).add(0,-0.3+wv,0);
                Location sL2 = pLoc.clone().add(pRight.clone().multiply(-1.5)).add(0, 0.7+wv,0);
                Location sH  = pLoc.clone().add(0, 1.5+wv, 0);

                swordLoc[0] = sL1; swordLoc[1] = sL2; swordLoc[2] = sH;

                drawSword(sL1, holdFwd, pUp, 1.0f);
                drawSword(sL2, holdFwd, pUp, 1.0f);
                drawSword(sH,  holdFwd, pUp, 1.0f);

                // Blood drip to ground (movie effect)
                if (holdTick % 8 == 0) {
                    for (Location sl : new Location[]{sL1, sL2, sH}) {
                        w.spawnParticle(Particle.DRIPPING_LAVA,
                            sl.clone().add(0.0, -0.2, 0.0), 2, 0.15,0,0.15, 0.01);
                        w.spawnParticle(Particle.FALLING_LAVA,
                            sl.clone().add(0.0, -0.1, 0.0), 1, 0.1,0,0.1, 0.01);
                        // Red dust blood
                        w.spawnParticle(Particle.DUST,
                            sl.clone().add(0, -0.5, 0), 1, 0.08,0.3,0.08, 0,
                            new Particle.DustOptions(Color.fromRGB(180,0,0), 1.5f));
                    }
                }

                if (holdTick >= 80) {
                    phase[0] = 2;
                    w.playSound(p.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 1f, 0.6f);
                    w.playSound(p.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 1.5f);
                    // Launch sparks
                    for (Location sl : swordLoc) {
                        w.spawnParticle(Particle.EXPLOSION, sl, 1, 0.2,0.2,0.2, 0);
                        w.spawnParticle(Particle.END_ROD, sl, 8, 0.4,0.4,0.4, 0.1);
                    }
                }

            // ── PHASE 2: SHOOT + TRACK ────────────────────────────────────
            } else {
                // All 3 swords track target — separate sub-tasks already launched
                // but we do it inline for simplicity
                boolean anyAlive = false;
                for (int i = 0; i < 3; i++) {
                    if (!swordAlive[i]) continue;
                    anyAlive = true;

                    if (!target.isValid() || target.isDead()) {
                        swordAlive[i] = false; continue;
                    }

                    // Move sword toward target — smooth lerp
                    Location tLoc = target.getLocation().clone().add(0, 0.8, 0);
                    Location cur  = swordLoc[i].clone();
                    Vector toTarget = tLoc.toVector().subtract(cur.toVector());
                    double dist = toTarget.length();

                    // Speed: slightly less than sprint (player sprint ~5.6 b/s, sword ~4.5)
                    double speed = 0.22; // per tick = ~4.4 b/s
                    if (dist < 0.5) {
                        // HIT
                        swordAlive[i] = false;
                        // Damage on first hit only (combined)
                        if (!hit[i]) {
                            hit[i] = true;
                            target.damage(16.0 * dm, p); // 8 hearts
                            w.spawnParticle(Particle.CRIT,
                                tLoc, 20, 0.5,0.5,0.5, 0.12);
                            w.spawnParticle(Particle.DUST,
                                tLoc, 15, 0.4,0.3,0.4, 0,
                                new Particle.DustOptions(Color.fromRGB(180,0,0), 1.8f));
                            w.spawnParticle(Particle.EXPLOSION, tLoc, 1, 0,0,0, 0);
                            w.playSound(tLoc, Sound.ENTITY_PLAYER_HURT,         1f, 0.7f);
                            w.playSound(tLoc, Sound.ENTITY_IRON_GOLEM_ATTACK,   1f, 0.9f);
                            w.playSound(tLoc, Sound.ENTITY_RAVAGER_ATTACK,      1f, 0.8f);
                        }
                        continue;
                    }

                    // Move toward target (ignore others)
                    Vector moveVec = toTarget.normalize().multiply(Math.min(speed, dist));
                    swordLoc[i] = cur.clone().add(moveVec);

                    // Orient blade toward target
                    Vector bladeDir = toTarget.clone().normalize();
                    drawSword(swordLoc[i], bladeDir, pUp, 1.0f);

                    // Trailing trail
                    w.spawnParticle(Particle.END_ROD,
                        swordLoc[i], 2, 0.05,0.05,0.05, 0.01);
                    w.spawnParticle(Particle.DUST,
                        swordLoc[i].clone().subtract(bladeDir.clone().multiply(0.3)),
                        2, 0.1,0.1,0.1, 0,
                        new Particle.DustOptions(Color.fromRGB(220,200,100), 1.4f));
                    // Handle trail
                    w.spawnParticle(Particle.CRIT,
                        swordLoc[i].clone().subtract(bladeDir.clone().multiply(0.8)),
                        1, 0.05,0.05,0.05, 0.03);
                }

                if (!anyAlive) { cancel(); return; }

                // Timeout safety (15s max tracking)
                if (ticks > 440) { cancel(); }
            }
        }
    }.runTaskTimer(plugin, 0, 1);
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
// MIRAGE PRIMARY — Photo jaisa: UPAR bada ring, neeche chota
// Laser center se upar tak, rings upar se neeche stack
case MIRAGE -> {
    Location groundLoc = null;
    Location eyeLoc = p.getEyeLocation();
    Vector   eyeDir = p.getEyeLocation().getDirection().normalize();
    for (int i = 1; i <= 80; i++) {
        Location check = eyeLoc.clone().add(eyeDir.clone().multiply(i));
        if (check.getBlock().getType().isSolid()) {
            groundLoc = check.clone().add(0, 1, 0); break;
        }
    }
    if (groundLoc == null) {
        groundLoc = p.getLocation().clone().add(eyeDir.clone().multiply(20));
        groundLoc.setY(p.getLocation().getBlockY());
    }

    final Location base = groundLoc.clone();
    final boolean[] done = {false};

    w.playSound(p.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.5f, 0.7f);
    w.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 0.5f);
    w.playSound(base, Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 0.6f);
    p.sendMessage("\u00a7c\u00a7lMIRAGE", "\u00a77Ancient circle summoned...", 5, 50, 10);

    java.util.List<org.bukkit.entity.FallingBlock> floatingBlocks = new java.util.ArrayList<>();
    for (int fx = -3; fx <= 3; fx++) {
        for (int fz = -3; fz <= 3; fz++) {
            if (Math.sqrt(fx*fx+fz*fz) > 3.5) continue;
            org.bukkit.block.Block blk = base.clone().add(fx,-1,fz).getBlock();
            if (!blk.getType().isSolid()) continue;
            org.bukkit.entity.FallingBlock fb = w.spawnFallingBlock(
                blk.getLocation().clone().add(0.5,1.0,0.5), blk.getType().createBlockData());
            fb.setDropItem(false); fb.setHurtEntities(false); fb.setGravity(false);
            fb.setVelocity(new Vector((Math.random()-0.5)*0.04,
                0.015+Math.random()*0.025, (Math.random()-0.5)*0.04));
            floatingBlocks.add(fb);
        }
    }

    new BukkitRunnable() {
        int    ticks = 0;
        double spin  = 0;

        // ── Core ring draw ───────────────────────────────────────────
        void R(Location c, double r, int pts, double off, int rgb) {
            Particle.DustOptions d = new Particle.DustOptions(
                Color.fromRGB((rgb>>16)&0xFF,(rgb>>8)&0xFF,rgb&0xFF), 1.7f);
            for (int i=0;i<pts;i++) {
                double a=(Math.PI*2.0/pts)*i+off;
                w.spawnParticle(Particle.DUST,
                    c.clone().add(Math.cos(a)*r,0,Math.sin(a)*r),1,0,0,0,0,d);
            }
        }

        // ── Spokes: center to radius ─────────────────────────────────
        void spokes(Location c, double r, int count, double off, int rgb) {
            Particle.DustOptions d = new Particle.DustOptions(
                Color.fromRGB((rgb>>16)&0xFF,(rgb>>8)&0xFF,rgb&0xFF), 1.4f);
            for (int s=0;s<count;s++) {
                double a=(Math.PI*2.0/count)*s+off;
                for (int k=1;k<=8;k++) {
                    double t=k/8.0*r;
                    w.spawnParticle(Particle.DUST,
                        c.clone().add(Math.cos(a)*t,0,Math.sin(a)*t),1,0,0,0,0,d);
                }
            }
        }

        // ── Full ring group: outer+inner rings + spokes + satellites ──
        void ringGroup(Location c, double outerR, double spin, boolean ccw) {
            double s = ccw ? -spin : spin;
            int RED   = 0xFF2800;
            int RED2  = 0xC81E00;
            int RED3  = 0xFF6030;

            // Outer ring
            R(c, outerR,      (int)(outerR*12), s,       RED);
            // Inner ring 1
            R(c, outerR*0.75, (int)(outerR*9), -s*1.3,   RED2);
            // Inner ring 2
            R(c, outerR*0.5,  (int)(outerR*7),  s*1.8,   RED3);
            // Inner ring 3 (core)
            R(c, outerR*0.28, (int)(outerR*4), -s*2.5,   RED);

            // Spokes from center
            spokes(c, outerR*0.95, 8, s*0.5, RED2);

            // Satellites — circles on outer ring
            int satCount = outerR > 8 ? 4 : outerR > 5 ? 3 : 2;
            for (int i=0;i<satCount;i++) {
                double a=(Math.PI*2.0/satCount)*i + s*0.7;
                double satR = outerR*0.12;
                Location sc = c.clone().add(Math.cos(a)*outerR*0.88, 0, Math.sin(a)*outerR*0.88);
                // Satellite outer ring
                R(sc, satR,      16, -s*2,   RED);
                // Satellite inner ring
                R(sc, satR*0.55, 10,  s*3,   RED2);
                // Spokes inside satellite
                spokes(sc, satR*0.9, 5, s, RED3);
            }

            // Tick marks on outer ring edge
            int ticks2 = (int)(outerR*2.5);
            for (int i=0;i<ticks2;i++) {
                double a=(Math.PI*2.0/ticks2)*i+s;
                double pa=a+Math.PI/2;
                Location tl=c.clone().add(Math.cos(a)*outerR,0,Math.sin(a)*outerR);
                for (int k=0;k<2;k++) {
                    w.spawnParticle(Particle.DUST,
                        tl.clone().add(Math.cos(pa)*(k-0.5)*0.4,0,Math.sin(pa)*(k-0.5)*0.4),
                        1,0,0,0,0, new Particle.DustOptions(Color.fromRGB(255,40,0),1.5f));
                }
            }
        }

        public void run() {
            if (!p.isOnline()) { cleanup(); cancel(); return; }

            if (ticks++ >= 120) {
                if (!done[0]) {
                    done[0] = true;
                    cleanup();
                    for (int b=0;b<20;b++) {
                        final int fb2=b;
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            double bx=base.getX()+(Math.random()-0.5)*14;
                            double bz=base.getZ()+(Math.random()-0.5)*14;
                            Location bl=new Location(w,bx,base.getY(),bz);
                            w.createExplosion(bl,3.5f,true,true);
                            w.spawnParticle(Particle.EXPLOSION_EMITTER,bl,1,0,0,0,0);
                            w.playSound(bl,Sound.ENTITY_GENERIC_EXPLODE,1.5f,0.6f);
                            for (org.bukkit.entity.Entity e:w.getNearbyEntities(bl,8,8,8)) {
                                if (!(e instanceof LivingEntity le)) continue;
                                if (e.getUniqueId().equals(p.getUniqueId())) continue;
                                if (e instanceof Player ep&&(ep.getGameMode()==GameMode.CREATIVE||ep.getGameMode()==GameMode.SPECTATOR)) continue;
                                le.damage(6.0*logic.getDmg(p),p);
                                Vector vel=e.getLocation().toVector().subtract(bl.toVector())
                                    .normalize().multiply(2.5).setY(0.8);
                                Bukkit.getScheduler().runTaskLater(plugin,
                                    ()->{if(e.isValid())e.setVelocity(vel);},1L);
                            }
                        }, fb2*3L);
                    }
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        w.createExplosion(base,10.0f,true,true);
                        w.spawnParticle(Particle.EXPLOSION_EMITTER,base,5,2,0.5,2,0);
                        w.playSound(base,Sound.ENTITY_WITHER_DEATH,1f,0.5f);
                    }, 62L);
                }
                cancel(); return;
            }

            spin += 0.035; // smooth slow spin

            // ── 5 RINGS — UPAR bada, neeche chota ────────────────────
            // Ring 1 TOP — BIGGEST (y=30) clockwise
            ringGroup(base.clone().add(0, 30.0, 0), 12.0, spin, false);

            // Ring 2 (y=22) — counter-clockwise
            ringGroup(base.clone().add(0, 22.0, 0), 8.5, spin, true);

            // Ring 3 (y=14) — clockwise
            ringGroup(base.clone().add(0, 14.0, 0), 6.0, spin, false);

            // Ring 4 (y=7) — counter-clockwise
            ringGroup(base.clone().add(0, 7.0, 0), 4.0, spin, true);

            // Ring 5 BOTTOM — SMALLEST (y=0.1) clockwise
            ringGroup(base.clone().add(0, 0.1, 0), 2.5, spin, false);

            // ── YELLOW LASER — ground se top tak ─────────────────────
            for (double y=0; y<=31.0; y+=0.35) {
                Location lp=base.clone().add(0,y,0);
                w.spawnParticle(Particle.DUST,lp,1,0.02,0,0.02,0,
                    new Particle.DustOptions(Color.fromRGB(255,230,0),2.2f));
                if (((int)(y*10))%3==0)
                    w.spawnParticle(Particle.DUST,lp,1,0.06,0,0.06,0,
                        new Particle.DustOptions(Color.fromRGB(255,180,0),1.8f));
                if (((int)(y*10))%8==0)
                    w.spawnParticle(Particle.END_ROD,lp,1,0.02,0,0.02,0.001);
            }

            // Floating blocks
            for (org.bukkit.entity.FallingBlock fb2:floatingBlocks) {
                if (!fb2.isValid()) continue;
                Vector vel=fb2.getVelocity();
                if (vel.getY()<0.08) fb2.setVelocity(vel.setY(vel.getY()+0.002));
            }

            if (ticks%4==0) {
                double ra=Math.random()*Math.PI*2;
                w.spawnParticle(Particle.FLAME,
                    base.clone().add(Math.cos(ra)*10,0.1,Math.sin(ra)*10),
                    1,0.1,0.1,0.1,0.02);
            }
            if (ticks%20==0) {
                for (org.bukkit.entity.Entity e:base.getWorld().getNearbyEntities(base,15,35,15)) {
                    if (!(e instanceof LivingEntity victim)) continue;
                    if (e.getUniqueId().equals(p.getUniqueId())) continue;
                    if (e instanceof Player ep&&(ep.getGameMode()==GameMode.CREATIVE||ep.getGameMode()==GameMode.SPECTATOR)) continue;
                    victim.damage(3.0,p);
                }
                w.playSound(base,Sound.BLOCK_BEACON_AMBIENT,1f,0.5f);
            }
        }

        void cleanup() {
            for (org.bukkit.entity.FallingBlock fb2:floatingBlocks)
                if (fb2.isValid()) fb2.remove();
            floatingBlocks.clear();
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
// ── ECLIPSE SECONDARY — White Ring + Black Beam + 20 TNT Blast ───────────────
// Aim based, 6 seconds, owner safe
// Photo 2 jaisi white ring structure + center se black beam neeche girega
case ECLIPSE -> {
    p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, logic.ticks(200, dr), 3));
    p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,       logic.ticks(200, dr), 2));
    p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, logic.ticks(220, dr), 0));

    // Aim — ground pe raycast
    Location groundLoc = null;
    Location eyeLoc = p.getEyeLocation();
    Vector   eyeDir = p.getEyeLocation().getDirection().normalize();
    for (int i = 1; i <= 60; i++) {
        Location check = eyeLoc.clone().add(eyeDir.clone().multiply(i));
        if (check.getBlock().getType().isSolid()) {
            groundLoc = check.clone().add(0, 1, 0);
            break;
        }
    }
    if (groundLoc == null) groundLoc = p.getLocation().clone().add(eyeDir.multiply(20));
    groundLoc.setY(groundLoc.getWorld().getHighestBlockYAt(groundLoc));

    final Location center = groundLoc.clone();
    final Location beamTop = center.clone().add(0, 80, 0); // beam upar se aayega
    final boolean[] done = {false};

    w.playSound(center, Sound.ENTITY_WITHER_AMBIENT,        1f, 0.3f);
    w.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL,    1f, 0.5f);
    p.sendMessage("\u00a75\u00a7lECLIPSE", "\u00a78Darkness descends...", 5, 60, 10);

    new BukkitRunnable() {
        int    ticks = 0;
        double spin  = 0;

        // White ring helper
        void wring(Location c, double r, int pts, double off) {
            Particle.DustOptions white = new Particle.DustOptions(
                org.bukkit.Color.fromRGB(220, 220, 255), 3.5f);
            for (int i = 0; i < pts; i++) {
                double a = (Math.PI*2.0/pts)*i + off;
                Location pt = c.clone().add(Math.cos(a)*r, 0, Math.sin(a)*r);
                w.spawnParticle(Particle.DUST, pt, 3, 0.1,0.1,0.1,0, white);
            }
        }

        public void run() {
            if (!p.isOnline()) { cancel(); return; }

            if (ticks++ >= 120) { // 6 seconds
                if (!done[0]) {
                    done[0] = true;
                    // 20 TNT blast at ground center
                    w.playSound(center, Sound.ENTITY_WITHER_BREAK_BLOCK, 1f, 0.3f);
                    w.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL,  1f, 0.4f);
                    for (int b = 0; b < 20; b++) {
                        final int fb = b;
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            // Random spread around center
                            double bx = center.getX() + (Math.random()-0.5)*8;
                            double bz = center.getZ() + (Math.random()-0.5)*8;
                            Location blastLoc = new Location(w, bx, center.getY(), bz);
                            w.spawnParticle(Particle.EXPLOSION_EMITTER, blastLoc, 1, 0,0,0,0);
                            w.spawnParticle(Particle.SQUID_INK, blastLoc, 20, 0.5, 0.5, 0.5, 0.1);
                            w.spawnParticle(Particle.DRAGON_BREATH, blastLoc, 15, 0.4, 0.4, 0.4, 0.06);
                            w.playSound(blastLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
                            // Damage nearby
                            for (org.bukkit.entity.Entity e : blastLoc.getWorld().getNearbyEntities(blastLoc, 6, 6, 6)) {
                                if (!(e instanceof LivingEntity le)) continue;
                                if (e.getUniqueId().equals(p.getUniqueId())) continue;
                                if (e instanceof Player ep && (ep.getGameMode()==GameMode.CREATIVE||ep.getGameMode()==GameMode.SPECTATOR)) continue;
                                le.damage(8.0 * logic.getDmg(p), p);
                                Vector vel = e.getLocation().toVector().subtract(blastLoc.toVector())
                                    .normalize().multiply(3.0).setY(1.2);
                                Bukkit.getScheduler().runTaskLater(plugin,
                                    () -> { if (e.isValid()) e.setVelocity(vel); }, 1L);
                            }
                        }, fb * 2L);
                    }
                    // Final big blast
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        w.spawnParticle(Particle.EXPLOSION_EMITTER, center, 5, 2, 0.5, 2, 0);
                        w.spawnParticle(Particle.SQUID_INK, center, 80, 3, 2, 3, 0.2);
                        w.playSound(center, Sound.ENTITY_WITHER_DEATH, 1f, 0.5f);
                    }, 42L);
                }
                cancel(); return;
            }

            spin += 0.05;

            // ── PHOTO 2 STRUCTURE — white rings ───────────────────────

            // BOTTOM small rings (ground)
            wring(center.clone().add(0, 0.1, 0), 5.0, 60, spin);
            wring(center.clone().add(0, 0.1, 0), 3.5, 48, -spin*1.3);
            wring(center.clone().add(0, 2.0, 0), 8.0, 80, -spin*0.8);
            wring(center.clone().add(0, 2.0, 0), 6.0, 64,  spin*1.1);

            // LARGE MIDDLE ring — biggest
            Location bigRing = center.clone().add(0, 10.0, 0);
            wring(bigRing, 22.0, 200,  spin*0.3);
            wring(bigRing, 20.0, 180, -spin*0.4);
            wring(bigRing, 18.0, 160,  spin*0.5);
            // Tick marks
            for (int i = 0; i < 24; i++) {
                double a = (Math.PI*2.0/24)*i + spin*0.3;
                Particle.DustOptions white = new Particle.DustOptions(org.bukkit.Color.fromRGB(220,220,255), 3.0f);
                for (int k = -2; k <= 2; k++) {
                    double perp = a + Math.PI/2;
                    Location tick = bigRing.clone().add(
                        Math.cos(a)*21.0 + Math.cos(perp)*k*0.5, 0,
                        Math.sin(a)*21.0 + Math.sin(perp)*k*0.5);
                    w.spawnParticle(Particle.DUST, tick, 3,0.1,0.1,0.1,0, white);
                }
            }

            // UPPER stacking rings
            double[] upY   = {18.0, 26.0, 34.0, 42.0};
            double[] upRad = {14.0, 10.0, 7.0,  4.5};
            for (int r = 0; r < 4; r++) {
                Location ul = center.clone().add(0, upY[r], 0);
                double us = (r%2==0) ? spin*0.9 : -spin*0.9;
                wring(ul, upRad[r],       100, us);
                wring(ul, upRad[r]*0.72,   80, -us*1.3);
                wring(ul, upRad[r]*0.45,   60,  us*1.8);
            }

            // TOP cluster
            Location top = center.clone().add(0, 52.0, 0);
            wring(top, 5.0, 55, spin);
            wring(top, 3.0, 38, -spin*1.5);
            for (int i = 0; i < 3; i++) {
                double a = (Math.PI*2.0/3)*i + spin*0.5;
                Location sc = top.clone().add(Math.cos(a)*5.5, 0, Math.sin(a)*5.5);
                wring(sc, 1.2, 20, -spin*2);
            }

                        // ── BLACK BEAM — upar se neeche zameen tak (FIXED & OPTIMIZED) ────────
            // Color ko pura black (0,0,0) kiya aur Size badha kar 5.0f kar diya
            Particle.DustOptions blackCore = new Particle.DustOptions(org.bukkit.Color.fromRGB(0, 0, 0), 5.0f); 
            Particle.DustOptions darkPurple = new Particle.DustOptions(org.bukkit.Color.fromRGB(80, 0, 150), 3.0f);

            double topY = 52.0; 
            // Gap 0.5 ka kiya (limit cross nahi hogi aur laser solid dikhegi)
            for (double y = topY; y >= 0; y -= 0.5) {
                Location lp = center.clone().add(0, y, 0);
                
                // Core - mota black dust
                w.spawnParticle(Particle.DUST, lp, 1, 0, 0, 0, 0, blackCore);
                // Glow - purple dust
                w.spawnParticle(Particle.DUST, lp, 1, 0, 0, 0, 0, darkPurple);
                
                // Extra texture ke liye (Har 2 block par smoke/ink)
                if (y % 2 == 0) {
                    w.spawnParticle(Particle.SQUID_INK, lp, 2, 0.2, 0.2, 0.2, 0.02);
                }
                // Sparkle (Har 3 block par)
                if (y % 3 == 0) {
                    w.spawnParticle(Particle.END_ROD, lp, 1, 0.1, 0.1, 0.1, 0.01);
                }
            }


            // Damage
            if (ticks % 20 == 0) {
                for (org.bukkit.entity.Entity e : center.getWorld().getNearbyEntities(center, 25, 25, 25)) {
                    if (!(e instanceof LivingEntity victim)) continue;
                    if (e.getUniqueId().equals(p.getUniqueId())) continue;
                    if (e instanceof Player ep && (ep.getGameMode()==GameMode.CREATIVE||ep.getGameMode()==GameMode.SPECTATOR)) continue;
                    victim.damage(3.0, p);
                }
            }
            if (ticks % 20 == 0) w.playSound(center, Sound.ENTITY_ENDERMAN_AMBIENT, 0.5f, 0.4f);
        }
    }.runTaskTimer(plugin, 0, 1);
}


            // ── GUARDIAN — Meteor Ring (Aim-based) ─────────────────────────────────────
case GUARDIAN -> {
    // Ray trace to find where the player is looking (up to 50 blocks)
    org.bukkit.util.RayTraceResult ray = p.getWorld().rayTraceBlocks(
            p.getEyeLocation(),
            p.getEyeLocation().getDirection(),
            50,
            FluidCollisionMode.NEVER,
            true
    );
    Location targetLoc;
    if (ray != null && ray.getHitBlock() != null) {
        targetLoc = ray.getHitBlock().getLocation().clone().add(0.5, 0, 0.5);
    } else {
        // fallback: 30 blocks in front
        targetLoc = p.getEyeLocation().clone()
                .add(p.getEyeLocation().getDirection().normalize().multiply(30));
        targetLoc.setY(targetLoc.getY() - 2); // keep it near ground level
    }

    // The ring will be 20 blocks above the target location
    Location ringCenter = targetLoc.clone().add(0, 20, 0);
    Location meteorStart = ringCenter.clone(); // meteor starts at ring center

    // Custom effects (same as before)
    p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, logic.ticks(100, dr), 2, false, false, true));
    p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,       logic.ticks(100, dr), 1, false, false, true));
    p.playSound(p.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.5f, 0.6f);
    p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.2f, 0.8f);
    p.sendMessage("§b§l🌀 GUARDIAN RING", "§7The heavens are watching...", 5, 40, 10);

    // Store meteor entity for cleanup
    final org.bukkit.entity.ItemDisplay[] meteorEntity = {null};
    final boolean[] meteorLanded = {false};
    final boolean[] cleaned = {false};

    // Ring animation runnable (identical to before, but uses new ringCenter)
    new BukkitRunnable() {
        int ticks = 0;
        double spinAngle = 0;
        final double RING_RADIUS = 3.5;
        final int RING_POINTS = 64;

        @Override
        public void run() {
            if (!p.isOnline() || p.isDead()) {
                cleanup();
                this.cancel();
                return;
            }

            // Main ring
            for (int i = 0; i < RING_POINTS; i++) {
                double a = spinAngle + (Math.PI * 2 / RING_POINTS) * i;
                double x = ringCenter.getX() + Math.cos(a) * RING_RADIUS;
                double z = ringCenter.getZ() + Math.sin(a) * RING_RADIUS;
                Location pt = new Location(w, x, ringCenter.getY(), z);
                w.spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0,
                        new Particle.DustOptions(Color.fromRGB(0, 200, 255), 1.8f));
                if (i % 6 == 0) {
                    w.spawnParticle(Particle.END_ROD, pt, 1, 0.1, 0.1, 0.1, 0.02);
                }
            }

            // Inner rings
            double innerR = RING_RADIUS * 0.7;
            for (int i = 0; i < 48; i++) {
                double a = -spinAngle * 1.3 + (Math.PI * 2 / 48) * i;
                double x = ringCenter.getX() + Math.cos(a) * innerR;
                double z = ringCenter.getZ() + Math.sin(a) * innerR;
                Location pt = new Location(w, x, ringCenter.getY() - 0.2, z);
                w.spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0,
                        new Particle.DustOptions(Color.fromRGB(100, 150, 255), 1.4f));
            }

            if (ticks % 20 == 0) {
                w.playSound(ringCenter, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.2f + ticks * 0.02f);
            }

            // After 2 seconds (40 ticks), spawn the meteor
            if (ticks >= 40 && !meteorLanded[0]) {
                meteorLanded[0] = true;
                spawnMeteor();
            }

            // After 5 seconds (100 ticks), cancel everything
            if (ticks >= 100) {
                cleanup();
                this.cancel();
                return;
            }

            ticks++;
            spinAngle += 0.1;
        }

        private void spawnMeteor() {
            if (cleaned[0]) return;
            w.playSound(ringCenter, Sound.ENTITY_ENDER_DRAGON_GROWL, 2f, 0.5f);

            // Meteor item with custom model data (set your own ID)
            org.bukkit.inventory.ItemStack meteorItem = new org.bukkit.inventory.ItemStack(Material.FIRE_CHARGE);
            org.bukkit.inventory.meta.ItemMeta meta = meteorItem.getItemMeta();
            meta.setCustomModelData(1001); // change to your model ID
            meteorItem.setItemMeta(meta);

            meteorEntity[0] = (org.bukkit.entity.ItemDisplay) w.spawnEntity(meteorStart, org.bukkit.entity.EntityType.ITEM_DISPLAY);
            meteorEntity[0].setItemStack(meteorItem);
            meteorEntity[0].setItemDisplayTransform(org.bukkit.entity.ItemDisplay.ItemDisplayTransform.GROUND);
            meteorEntity[0].setGravity(false);
            meteorEntity[0].setGlowing(true);
            meteorEntity[0].setGlowColorOverride(Color.fromRGB(255, 80, 0));

            new BukkitRunnable() {
                int fallTicks = 0;
                Location current = meteorStart.clone();
                final Vector fallDir = new Vector(0, -1.2, 0);

                @Override
                public void run() {
                    if (cleaned[0] || meteorEntity[0] == null || !meteorEntity[0].isValid()) {
                        this.cancel();
                        return;
                    }
                    current.add(fallDir);
                    meteorEntity[0].teleport(current);

                    // Fire trail
                    for (int i = 0; i < 8; i++) {
                        double offX = (Math.random() - 0.5) * 0.6;
                        double offZ = (Math.random() - 0.5) * 0.6;
                        w.spawnParticle(Particle.FLAME, current.clone().add(offX, -0.3, offZ), 1, 0, 0, 0, 0.02);
                        w.spawnParticle(Particle.LAVA, current.clone().add(offX, -0.5, offZ), 1, 0, 0.1, 0, 0.01);
                    }
                    w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, current.clone().add(0, -0.5, 0), 2, 0.2, 0.1, 0.2, 0.03);

                    // Ground detection
                    Location groundCheck = current.clone();
                    groundCheck.setY(groundCheck.getY() - 1);
                    if (groundCheck.getBlock().getType().isSolid() || fallTicks >= 30) {
                        impact();
                        this.cancel();
                    }
                    fallTicks++;
                }

                private void impact() {
                    if (cleaned[0]) return;
                    cleaned[0] = true;
                    
               try {
                    Location impactLoc = current.clone();
                    impactLoc.getWorld().createExplosion(impactLoc, 10.0f, true, true, p);

                    w.spawnParticle(Particle.EXPLOSION_EMITTER, impactLoc, 5, 1, 0.5, 1, 0);
                    w.spawnParticle(Particle.FLASH, impactLoc, 3, 0.5, 0.5, 0.5, 0);
                    w.spawnParticle(Particle.LAVA, impactLoc, 60, 2, 1, 2, 0.2);
                    w.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 3f, 0.5f);
                    w.playSound(impactLoc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 2f, 0.7f);

                    // Damage nearby enemies (owner safe)
                    for (org.bukkit.entity.Entity e : impactLoc.getWorld().getNearbyEntities(impactLoc, 12, 12, 12)) {
                        if (!(e instanceof LivingEntity le)) continue;
                        if (e.equals(p)) continue;
                        if (e instanceof Player ep && (ep.getGameMode() == GameMode.CREATIVE || ep.getGameMode() == GameMode.SPECTATOR)) continue;
                        le.damage(15.0 * logic.getDmg(p), p);
                        le.setVelocity(e.getLocation().toVector().subtract(impactLoc.toVector()).normalize().multiply(2.5).setY(1.0));
                    }
           } catch (Exception e) {
             e.printStackTrace();
           } finally {
            // ✅ HAR HAAL MEIN METEOR REMOVE KARO
        if (meteorEntity[0] != null && meteorEntity[0].isValid()) {
            meteorEntity[0].remove();
          }
             meteorEntity[0] = null;
           }
       }
            }.runTaskTimer(plugin, 0, 1);

                        // ✅ SAFETY TIMEOUT - 5 SECOND BAAD METEOR FORCE REMOVE
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!cleaned[0] && meteorEntity[0] != null && meteorEntity[0].isValid()) {
                        meteorEntity[0].remove();
                        meteorEntity[0] = null;
                        cleaned[0] = true;
                    }
                }, 100L);
            }

        private void cleanup() {
            if (cleaned[0]) return;
            cleaned[0] = true;
            if (meteorEntity[0] != null && meteorEntity[0].isValid()) meteorEntity[0].remove();
            // final particles
            for (int i = 0; i < 30; i++) {
                double rad = Math.random() * 4;
                double ang = Math.random() * Math.PI * 2;
                Location pt = ringCenter.clone().add(Math.cos(ang) * rad, Math.random() * 2, Math.sin(ang) * rad);
                w.spawnParticle(Particle.CLOUD, pt, 1, 0, 0, 0, 0.05);
            }
            w.playSound(ringCenter, Sound.ENTITY_ENDER_EYE_DEATH, 1f, 1f);
        }
    }.runTaskTimer(plugin, 0, 1);
}
        }
    }
}
