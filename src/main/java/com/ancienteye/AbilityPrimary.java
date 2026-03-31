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

    p.sendTitle("§6§lTITAN FORM", "§eYou are Unstoppable!", 5, 40, 5);

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
case MIRAGE -> {
    w.playSound(p.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.5f, 0.7f);
    w.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 0.5f);
    final Vector mirDir = p.getLocation().getDirection().clone();
    mirDir.setY(0); mirDir.normalize();
    final Location base = p.getLocation().clone().add(mirDir.multiply(10));
    base.setY(p.getLocation().getY());
    final boolean[] blasted = {false};

    new BukkitRunnable() {
        int    ticks = 0;
        double spin  = 0;

        // Inline ring draw — color parameter se
        void ring(Location center, double radius, int pts, double offset, Color col) {
            for (int i = 0; i < pts; i++) {
                double a = (Math.PI*2.0/pts)*i + offset;
                Location pt = center.clone().add(Math.cos(a)*radius, 0, Math.sin(a)*radius);
                w.spawnParticle(Particle.DUST, pt, 1, 0,0,0,0,
                    new Particle.DustOptions(col, 1.6f));
            }
        }

        // Inline horizontal bar draw
        void bar(Location center, double len, double angle, Color col) {
            for (int k = -10; k <= 10; k++) {
                double t = k / 10.0 * len;
                Location pt = center.clone().add(Math.cos(angle)*t, 0, Math.sin(angle)*t);
                w.spawnParticle(Particle.DUST, pt, 1, 0,0,0,0,
                    new Particle.DustOptions(col, 1.4f));
            }
        }

        public void run() {
            if (!p.isOnline() || p.isDead()) { cancel(); return; }

            if (ticks++ >= 200) {
                if (!blasted[0]) {
                    blasted[0] = true;
                    w.playSound(base, Sound.ENTITY_GENERIC_EXPLODE, 3f, 0.3f);
                    w.playSound(base, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 2f, 0.5f);
                    w.spawnParticle(Particle.EXPLOSION_EMITTER, base.clone().add(0,1,0), 20, 4, 2, 4, 0);
                    w.spawnParticle(Particle.FLASH, base.clone().add(0,1,0), 3, 2, 1, 2, 0);
                    w.createExplosion(base, 25.0f, false, true);
                    for (org.bukkit.entity.Entity e : base.getNearbyEntities(30,30,30)) {
                        if (!(e instanceof LivingEntity victim)) continue;
                        if (e.equals(p)) continue;
                        if (e instanceof Player ep && (ep.getGameMode()==GameMode.CREATIVE||ep.getGameMode()==GameMode.SPECTATOR)) continue;
                        double dist = e.getLocation().distance(base);
                        victim.damage(Math.max(5, 60 - dist*1.5), p);
                        victim.setVelocity(e.getLocation().toVector().subtract(base.toVector()).normalize().multiply(2).setY(0.8));
                    }
                }
                cancel(); return;
            }

            spin += 0.04;

            Color WHITE  = Color.fromRGB(255, 255, 255);
            Color BRIGHT = Color.fromRGB(220, 240, 255);
            Color DIM    = Color.fromRGB(160, 200, 255);

            // ── PHOTO STRUCTURE ────────────────────────────────────────
            // Photo 2: bottom small rings → middle big ring → upper rings stacking

            // BOTTOM — 2 small rings near ground
            Location b1 = base.clone().add(0, 0.1, 0);
            Location b2 = base.clone().add(0, 2.0, 0);
            ring(b1, 5.0, 64, spin,       WHITE);
            ring(b1, 3.5, 48, -spin*1.3,  BRIGHT);
            ring(b1, 2.0, 32, spin*2.0,   DIM);
            ring(b2, 7.0, 80, -spin*0.8,  WHITE);
            ring(b2, 5.5, 64,  spin*1.1,  BRIGHT);

            // MIDDLE BARS — horizontal tick marks on large ring (photo jaisa)
            Location bm = base.clone().add(0, 8.0, 0);
            ring(bm, 20.0, 200, spin*0.3,  WHITE);   // largest ring
            ring(bm, 18.0, 180, -spin*0.4, BRIGHT);
            ring(bm, 16.0, 160, spin*0.5,  DIM);
            // Tick bars on large ring
            for (int i = 0; i < 24; i++) {
                double a = (Math.PI*2.0/24)*i + spin*0.3;
                Location barLoc = bm.clone().add(Math.cos(a)*19.0, 0, Math.sin(a)*19.0);
                bar(barLoc, 1.5, a + Math.PI/2, WHITE);
            }

            // UPPER RINGS — stacking smaller going up
            double[] upY   = {16.0, 24.0, 32.0, 40.0};
            double[] upRad = {12.0, 9.0,  7.0,  5.0};
            for (int r = 0; r < 4; r++) {
                Location ul = base.clone().add(0, upY[r], 0);
                double us = (r%2==0) ? spin*0.8 : -spin*0.8;
                ring(ul, upRad[r],       100, us,        WHITE);
                ring(ul, upRad[r]*0.75,   80, -us*1.3,   BRIGHT);
                ring(ul, upRad[r]*0.5,    60, us*1.8,    DIM);
            }

            // TOP — small rings + satellites (photo jaisa top cluster)
            Location top = base.clone().add(0, 50.0, 0);
            ring(top, 4.0, 50, spin,      WHITE);
            ring(top, 2.5, 35, -spin*1.5, BRIGHT);
            ring(top, 1.2, 20, spin*2.5,  DIM);
            // 3 satellite circles on top
            for (int i = 0; i < 3; i++) {
                double a = (Math.PI*2.0/3)*i + spin*0.5;
                Location sc = top.clone().add(Math.cos(a)*4.5, 0, Math.sin(a)*4.5);
                ring(sc, 1.0, 18, -spin*2, BRIGHT);
            }

            // ── LASER — center se neeche zameen tak ───────────────────
            // Thin white beam — photo jaisa slim
            for (double y = 50.0; y >= 0; y -= 0.3) {
                Location lp = base.clone().add(0, y, 0);
                w.spawnParticle(Particle.DUST, lp, 1, 0.01, 0, 0.01, 0,
                    new Particle.DustOptions(Color.fromRGB(255,255,255), 2.0f));
                if (y % 3 < 0.3)
                    w.spawnParticle(Particle.END_ROD, lp, 1, 0.03, 0, 0.03, 0.001);
            }

            // Damage
            if (ticks % 10 == 0) {
                for (org.bukkit.entity.Entity e : base.getNearbyEntities(25,25,25)) {
                    if (!(e instanceof LivingEntity victim)) continue;
                    if (e.equals(p)) continue;
                    if (e instanceof Player ep && (ep.getGameMode()==GameMode.CREATIVE||ep.getGameMode()==GameMode.SPECTATOR)) continue;
                    victim.damage(4.0, p);
                    e.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, victim.getLocation().add(0,1,0), 5, 0.3,0.3,0.3,0);
                }
            }
            if (ticks % 20 == 0) w.playSound(base, Sound.BLOCK_BEACON_AMBIENT, 1f, 0.3f);
            if (ticks % 7  == 0) w.playSound(base, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.25f, 2f);
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
    p.sendTitle("\u00a75\u00a7lECLIPSE", "\u00a78Darkness descends...", 5, 60, 10);

    new BukkitRunnable() {
        int    ticks = 0;
        double spin  = 0;

        // White ring helper
        void wring(Location c, double r, int pts, double off) {
            Particle.DustOptions white = new Particle.DustOptions(
                org.bukkit.Color.fromRGB(220, 220, 255), 1.5f);
            for (int i = 0; i < pts; i++) {
                double a = (Math.PI*2.0/pts)*i + off;
                Location pt = c.clone().add(Math.cos(a)*r, 0, Math.sin(a)*r);
                w.spawnParticle(Particle.DUST, pt, 1, 0,0,0,0, white);
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
                Particle.DustOptions white = new Particle.DustOptions(org.bukkit.Color.fromRGB(220,220,255), 1.4f);
                for (int k = -2; k <= 2; k++) {
                    double perp = a + Math.PI/2;
                    Location tick = bigRing.clone().add(
                        Math.cos(a)*21.0 + Math.cos(perp)*k*0.5, 0,
                        Math.sin(a)*21.0 + Math.sin(perp)*k*0.5);
                    w.spawnParticle(Particle.DUST, tick, 1,0,0,0,0, white);
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

            // ── BLACK BEAM — upar se neeche zameen tak ────────────────
            Particle.DustOptions black = new Particle.DustOptions(
                org.bukkit.Color.fromRGB(10, 0, 20), 2.0f);
            Particle.DustOptions darkPurple = new Particle.DustOptions(
                org.bukkit.Color.fromRGB(50, 0, 80), 1.5f);
            for (double y = 60.0; y >= 0; y -= 0.35) {
                Location lp = center.clone().add(0, y, 0);
                w.spawnParticle(Particle.DUST, lp, 1, 0.02, 0, 0.02, 0, black);
                if (((int)(y*10)) % 4 == 0)
                    w.spawnParticle(Particle.DUST, lp, 1, 0.06, 0, 0.06, 0, darkPurple);
                // Dragon breath particles for dark feel
                if (((int)(y*10)) % 8 == 0)
                    w.spawnParticle(Particle.DRAGON_BREATH, lp, 1, 0.08, 0, 0.08, 0.01);
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
    p.sendTitle("§b§l🌀 GUARDIAN RING", "§7The heavens are watching...", 5, 40, 10);

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
        }

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
