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
import org.bukkit.inventory.ItemStack;
import org.bukkit.FluidCollisionMode; 
import org.bukkit.util.RayTraceResult;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.*;

public class AbilitySecondary implements Listener {

    private final AncientEyePlugin plugin;
    private final AbilityLogic logic;

    private static final java.util.HashMap<java.util.UUID, Integer> dashCount = new java.util.HashMap<>();
    private static final java.util.HashMap<java.util.UUID, Long> lastDashTime = new java.util.HashMap<>();

    public AbilitySecondary(AncientEyePlugin plugin, AbilityLogic logic) {
        this.plugin = plugin;
        this.logic  = logic;
    }

    public void activate(Player p, EyeType eye) {
        if (plugin.getCooldownManager().isOnCooldown(p, "S")) return;
        plugin.getCooldownManager().setCooldown(p, "S", logic.getCd(p, eye, "secondary"));

        Location loc = p.getLocation().clone();
        Vector   dir = p.getEyeLocation().getDirection().normalize();
        final double dm = logic.getDmg(p);
        final double dr = logic.getDur(p);
        World    w   = p.getWorld();

        switch (eye) {

                   case VOID -> {
                // 1. Aim Based Targeting (15 blocks ki range mein)
                org.bukkit.entity.Entity target = p.getTargetEntity(15);
                
                // Agar samne koi enemy nahi hai, toh ability waste na ho
                if (!(target instanceof org.bukkit.entity.LivingEntity enamy)) {
                    p.sendMessage("§c⚠️ No Target found!");
                    return;
                }

                Location targetLoc = enamy.getLocation();
                long durationTicks = 100L; // 5 seconds (20 ticks = 1s)

                // 2. Freeze aur Darkness Effects (5 seconds ka timer)
                enamy.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.DARKNESS, (int) durationTicks, 1, false, false));
                enamy.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS, (int) durationTicks, 1, false, false));
                // Move aur Jump block karne ke liye (Freeze effect)
                enamy.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, (int) durationTicks, 255, false, false));
                enamy.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.JUMP_BOOST, (int) durationTicks, 250, false, false));

                // Trap Activate Sound
                w.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1f, 0.5f);
                w.playSound(loc, Sound.ENTITY_ENDERMAN_STARE, 0.8f, 0.5f);

                // 3. Particle Task (Jail aur Orbiting Ring)
                new org.bukkit.scheduler.BukkitRunnable() {
                    int ticks = 0;
                    double ringAngle = 0;

                    @Override
                    public void run() {
                        // Agar 5 second (100 ticks) pure ho gaye YA enemy mar gaya/leave kar diya
                        if (ticks >= durationTicks || !enamy.isValid()) {
                            // Trap tutne ka sound
                            w.playSound(enamy.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 0.5f);
                            w.playSound(enamy.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1f);
                            this.cancel();
                            return;
                        }

                        Location eLoc = enamy.getLocation();

                        // --- SHAPE 1: DARK JAIL (Black Smoke ki deewarein) ---
                        // 8 pillars banayenge target ke charo taraf
                        for (int i = 0; i < 8; i++) {
                            double angle = (Math.PI / 4) * i;
                            double x = Math.cos(angle) * 1.2;
                            double z = Math.sin(angle) * 1.2;
                            
                            // Niche se upar tak particle (Jail ke sariye)
                            for (double y = 0; y <= 2.5; y += 0.5) {
                                w.spawnParticle(Particle.LARGE_SMOKE, eLoc.clone().add(x, y, z), 1, 0, 0, 0, 0);
                            }
                        }

                        // --- SHAPE 2: ORBITING VOID RING (Bich mein ghoomne wala ring) ---
                        ringAngle += 0.4; // Ghoomne ki speed
                        for (int i = 0; i < 6; i++) { // Ring ke 6 points
                            double orbit = ringAngle + (Math.PI / 3) * i;
                            double rx = Math.cos(orbit) * 1.5;
                            double rz = Math.sin(orbit) * 1.5;
                            // Y + 1.2 matlab enemy ki kamar/chhati ke paas ghoomega
                            w.spawnParticle(Particle.PORTAL, eLoc.clone().add(rx, 1.2, rz), 2, 0, 0, 0, 0);
                        }

                        ticks += 2; // Har 2 ticks mein update hoga
                    }
                }.runTaskTimer(plugin, 0L, 2L);
            }


            // ── PHANTOM — Reveal ──────────────────────────────────────────
            case PHANTOM -> {
                w.spawnParticle(Particle.END_ROD, loc, 100, 4, 4, 4, 0.09);
                w.spawnParticle(Particle.SONIC_BOOM, loc, 1, 0, 0, 0, 0);
                w.playSound(loc, Sound.ENTITY_PHANTOM_BITE, 0.8f, 2.0f);
                p.getNearbyEntities(30, 30, 30).forEach(e -> {
                    if (e instanceof Player t && e != p) {
                        t.removePotionEffect(PotionEffectType.INVISIBILITY);
                        t.setGlowing(true);
                        w.spawnParticle(Particle.END_ROD, t.getLocation(), 25, 0.4, 0.9, 0.4, 0.04);
                        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (t.isOnline()) t.setGlowing(false); }, logic.ticks(160, dr));
                    }
                });
                p.sendTitle("§b§lPHANTOM REVEAL", "§7All invisible players exposed!", 5, 50, 10);
            }

            // ── STORM — Electric Ring ─────────────────────────────────────
            case STORM -> {
                double qDmg = logic.ecfg("STORM", "secondary-damage", 10.0);
                w.playSound(p.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 2f, 0.5f);
                new BukkitRunnable() {
                    double radius = 1.0;
                    public void run() {
                        if (radius > 8.0 || !p.isOnline()) { cancel(); return; }
                        for (double a = 0; a < Math.PI*2; a += Math.PI/(radius*3)) {
                            Location ring = p.getLocation().clone().add(Math.cos(a)*radius, 0.2, Math.sin(a)*radius);
                            w.spawnParticle(Particle.ELECTRIC_SPARK, ring, 1,0,0,0,0);
                            if (radius > 4) w.spawnParticle(Particle.CLOUD, ring, 1,0,0,0, 0.02);
                        }
                        logic.applySafeDamage(p, p.getLocation(), radius, qDmg);
                        p.getWorld().getNearbyEntities(p.getLocation(), radius, 2, radius).forEach(e -> {
                            if (e instanceof LivingEntity le && e != p) {
                                Vector push = le.getLocation().toVector().subtract(p.getLocation().toVector()).normalize();
                                le.setVelocity(push.multiply(1.8).setY(0.5));
                            }
                        });
                        radius += 1.5;
                    }
                }.runTaskTimer(plugin, 0, 1);
            }

            // ── FROST — Ice Spikes ────────────────────────────────────────
            case FROST -> {
                w.spawnParticle(Particle.SNOWFLAKE, loc, 100, 2.5, 0.5, 2.5, 0.1);
                w.spawnParticle(Particle.WHITE_ASH, loc,  60, 2.0, 0.3, 2.0, 0.05);
                w.playSound(loc, Sound.ENTITY_PLAYER_HURT_FREEZE, 1f, 0.3f);
                w.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1f, 0.4f);
                List<Location> placedBlocks = new ArrayList<>();
                double[][] dirs  = {{ 1, 0},{ -1, 0},{ 0, 1},{ 0,-1}};
                double[][] perps = {{ 0, 1},{  0,-1},{ 1, 0},{-1, 0}};
                double cosA = Math.cos(Math.toRadians(40)), sinA = Math.sin(Math.toRadians(40));
                for (int d = 0; d < 4; d++) {
                    final int fd = d;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        double dx=dirs[fd][0], dz=dirs[fd][1], px=perps[fd][0], pz=perps[fd][1];
                        for (int s = 0; s < 5; s++) {
                            double spread = (s - 2) * 3.0;
                            double baseX = loc.getX() + dx*10 + px*spread;
                            double baseY = loc.getY();
                            double baseZ = loc.getZ() + dz*10 + pz*spread;
                            int spikeLen = 18 + (s==2 ? 7 : (s==1||s==3 ? 4 : 0));
                            for (int h = 0; h < spikeLen; h++) {
                                double outward = h * cosA, upward = h * sinA;
                                int halfW = h < spikeLen/3 ? 1 : 0;
                                for (int ww = -halfW; ww <= halfW; ww++) {
                                    int bx=(int)Math.round(baseX+dx*outward+px*ww);
                                    int by=(int)Math.round(baseY+upward);
                                    int bz=(int)Math.round(baseZ+dz*outward+pz*ww);
                                    Location bl = new Location(w, bx, by, bz);
                                    org.bukkit.block.Block blk = bl.getBlock();
                                    Material mat = blk.getType();
                                    if (mat==Material.AIR||mat==Material.CAVE_AIR||mat==Material.VOID_AIR) {
                                        blk.setType(h%3==0 ? Material.BLUE_ICE : Material.PACKED_ICE);
                                        synchronized(placedBlocks) { placedBlocks.add(bl); }
                                    }
                                }
                            }
                            Location base = new Location(w, baseX, baseY, baseZ);
                            w.spawnParticle(Particle.SNOWFLAKE, base, 20, 0.3, 0.2, 0.3, 0.04);
                            w.spawnParticle(Particle.WHITE_ASH, base, 10, 0.2, 0.3, 0.2, 0.02);
                        }
                        w.playSound(new Location(w, loc.getX()+dx*10, loc.getY(), loc.getZ()+dz*10), Sound.BLOCK_GLASS_BREAK, 1f, 0.5f);
                        w.playSound(new Location(w, loc.getX()+dx*10, loc.getY(), loc.getZ()+dz*10), Sound.ENTITY_PLAYER_HURT_FREEZE, 1f, 0.6f);
                    }, fd * 3L);
                }
                for (int ring = 1; ring <= 4; ring++) { final int fr = ring;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        double r = fr * 2.5;
                        for (int i = 0; i < 24; i++) { double a = Math.toRadians(i*15);
                            w.spawnParticle(Particle.SNOWFLAKE, loc.clone().add(Math.cos(a)*r, 0.1, Math.sin(a)*r), 2,0,0,0, 0.02);
                        }
                    }, ring * 3L);
                }
                new BukkitRunnable() {
                    int elapsed = 0;
                    public void run() {
                        if (elapsed >= 6) { cancel(); return; }
                        elapsed++;
                        p.getNearbyEntities(12, 12, 12).forEach(e -> {
                            if (!(e instanceof LivingEntity le) || e == p) return;
                            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 25, 5, false, false));
                            le.damage(1.5 * dm, p);
                            w.spawnParticle(Particle.SNOWFLAKE, le.getLocation(), 8, 0.3, 0.6, 0.3, 0.02);
                        });
                    }
                }.runTaskTimer(plugin, 20L, 20L);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    for (Location bl : placedBlocks) {
                        if (bl.getBlock().getType()==Material.BLUE_ICE||bl.getBlock().getType()==Material.PACKED_ICE) {
                            bl.getBlock().setType(Material.AIR);
                            w.spawnParticle(Particle.SNOWFLAKE, bl, 3, 0.2, 0.2, 0.2, 0.02);
                        }
                    }
                    placedBlocks.clear();
                    double[][] dirsF = {{1,0},{-1,0},{0,1},{0,-1}};
                    for (double[] df : dirsF) {
                        Location sLoc = loc.clone().add(df[0]*10, 0, df[1]*10);
                        w.playSound(sLoc, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.6f);
                        w.playSound(sLoc, Sound.ENTITY_PLAYER_HURT_FREEZE, 1f, 1.5f);
                        w.spawnParticle(Particle.SNOWFLAKE, sLoc, 40, 1, 1, 1, 0.06);
                        w.spawnParticle(Particle.WHITE_ASH, sLoc, 30, 1, 1, 1, 0.04);
                    }
                }, 120L);
            }

            // ── FLAME — Burst ─────────────────────────────────────────────
            case FLAME -> {
                w.spawnParticle(Particle.EXPLOSION, loc, 4, 0.7, 0, 0.7, 0);
                w.spawnParticle(Particle.FLAME, loc, 120, 2.8, 0.7, 2.8, 0.2);
                w.spawnParticle(Particle.LAVA, loc, 35, 2.2, 0.3, 2.2, 0.14);
                for (int i = 0; i < 24; i++) { double a = Math.toRadians(i*15);
                    for (int h = 0; h <= 3; h++)
                        w.spawnParticle(Particle.FLAME, loc.clone().add(Math.cos(a)*3.5,h*0.5,Math.sin(a)*3.5), 3, 0, 0.2, 0, 0.04);
                }
                w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.9f);
                w.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1f, 0.6f);
                p.getNearbyEntities(7, 7, 7).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        Vector vel = e.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(2.5).setY(0.8);
                        le.damage(8.0 * dm, p);
                        le.setFireTicks(logic.ticks(140, dr));
                        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (e.isValid()) e.setVelocity(vel); }, 1L);
                    }
                });
            }

            // ── SHADOW — Shadow Cloak ─────────────────────────────────────
            case SHADOW -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, logic.ticks(200, dr), 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, logic.ticks(300, dr), 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,        logic.ticks(160, dr), 1));
                w.spawnParticle(Particle.SMOKE, loc, 80, 0.5, 1.2, 0.5, 0.09);
                w.spawnParticle(Particle.DUST, loc, 60, 0.4, 0.9, 0.4, 0, new Particle.DustOptions(Color.BLACK, 2f));
                w.spawnParticle(Particle.PORTAL, loc, 40, 0.3, 0.7, 0.3, 0.07);
                w.playSound(loc, Sound.ENTITY_ENDERMAN_STARE, 0.6f, 1.3f);
                p.sendTitle("\u00a78\u00a7lSHADOW CLOAK", "\u00a77You vanish into darkness...", 5, 50, 10);
                org.bukkit.inventory.PlayerInventory inv = p.getInventory();
                final ItemStack h = inv.getHelmet()     != null ? inv.getHelmet().clone()     : null;
                final ItemStack c = inv.getChestplate() != null ? inv.getChestplate().clone() : null;
                final ItemStack l = inv.getLeggings()   != null ? inv.getLeggings().clone()   : null;
                final ItemStack b = inv.getBoots()      != null ? inv.getBoots().clone()      : null;
                final ItemStack oh = inv.getItemInOffHand().getType() != Material.AIR ? inv.getItemInOffHand().clone() : null;
                inv.setHelmet(null); inv.setChestplate(null); inv.setLeggings(null); inv.setBoots(null); inv.setItemInOffHand(null);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!p.isOnline()) return;
                    org.bukkit.inventory.PlayerInventory i2 = p.getInventory();
                    logic.restoreOrDrop(p, h,  () -> i2.setHelmet(h),     i2.getHelmet());
                    logic.restoreOrDrop(p, c,  () -> i2.setChestplate(c), i2.getChestplate());
                    logic.restoreOrDrop(p, l,  () -> i2.setLeggings(l),   i2.getLeggings());
                    logic.restoreOrDrop(p, b,  () -> i2.setBoots(b),      i2.getBoots());
                    logic.restoreOrDrop(p, oh, () -> i2.setItemInOffHand(oh), i2.getItemInOffHand());
                    w.spawnParticle(Particle.SMOKE, p.getLocation(), 60, 0.5, 1, 0.5, 0.08);
                    w.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.8f);
                    p.sendTitle("\u00a77Shadow Cloak", "\u00a78Faded...", 5, 30, 10);
                }, logic.ticks(200, dr));
            }

            // ── TITAN — Strength ──────────────────────────────────────────
            case TITAN -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH,   logic.ticks(120, dr), 2));
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,  logic.ticks(60, dr), 1));
                w.spawnParticle(Particle.DUST, loc, 80, 0.7, 1.4, 0.7, 0, new Particle.DustOptions(Color.fromRGB(180,90,20), 3f));
                w.spawnParticle(Particle.CRIT, loc, 40, 0.5, 1.0, 0.5, 0.12);
                w.spawnParticle(Particle.EXPLOSION, loc, 2, 0.4, 0, 0.4, 0);
                for (int ring = 1; ring <= 3; ring++) { final int fr = ring;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        for (int i = 0; i < 16; i++) { double a = Math.toRadians(i*22.5);
                            w.spawnParticle(Particle.DUST, loc.clone().add(Math.cos(a)*(fr*0.8),1,Math.sin(a)*(fr*0.8)), 3, 0, 0.5, 0, 0, new Particle.DustOptions(Color.fromRGB(180,90,20), 2f));
                        }
                    }, fr * 3L);
                }
                w.playSound(loc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 0.6f);
                w.playSound(loc, Sound.ENTITY_RAVAGER_ROAR, 0.8f, 0.8f);
                p.sendTitle("§6§l💪 TITAN STRENGTH!", "§7+Strength III active!", 5, 45, 10);
            }

            // ── HUNTER — Mark Target ──────────────────────────────────────
            case HUNTER -> {
                LivingEntity tgt = logic.aim(p, 30);
                if (tgt != null) {
                    w.spawnParticle(Particle.CRIT, tgt.getLocation(), 50, 0.5, 1.0, 0.5, 0.12);
                    w.spawnParticle(Particle.END_ROD, tgt.getLocation(), 25, 0.4, 0.8, 0.4, 0.06);
                    w.spawnParticle(Particle.SOUL, tgt.getLocation(), 20, 0.3, 0.7, 0.3, 0.05);
                    for (int ring = 1; ring <= 3; ring++) { final int fr = ring;
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            double r = fr * 0.6;
                            for (int i = 0; i < 12; i++) { double a = Math.toRadians(i*30);
                                w.spawnParticle(Particle.DUST, tgt.getLocation().clone().add(Math.cos(a)*r, 1.0, Math.sin(a)*r), 1,0,0,0,0, new Particle.DustOptions(Color.fromRGB(220,20,20), 1.5f));
                            }
                        }, ring * 3L);
                    }
                    w.playSound(loc, Sound.ENTITY_ARROW_HIT_PLAYER, 1f, 0.8f);
                    w.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1.2f);
                    tgt.setMetadata("hunterMarked", new org.bukkit.metadata.FixedMetadataValue(plugin, p.getUniqueId().toString()));
                    tgt.setGlowing(true);
                    p.sendTitle("§c§lMARKED!", "§7Target takes extra damage!", 5, 50, 10);
                    new BukkitRunnable() {
                        int    ticks = 0;
                        double angle = 0;
                        public void run() {
                            if (ticks++ >= logic.ticks(200, dr) || !tgt.isValid() || tgt.isDead()) {
                                tgt.removeMetadata("hunterMarked", plugin);
                                if (tgt.isValid()) tgt.setGlowing(false);
                                cancel(); return;
                            }
                            angle += 0.15;
                            for (int i = 0; i < 8; i++) { double a = angle + Math.toRadians(i*45);
                                w.spawnParticle(Particle.DUST, tgt.getLocation().clone().add(Math.cos(a)*0.7, 1.0, Math.sin(a)*0.7), 1,0,0,0,0, new Particle.DustOptions(Color.fromRGB(220,20,20), 1.2f));
                            }
                            w.spawnParticle(Particle.CRIT, tgt.getLocation().clone().add(0, tgt.getHeight()+0.3, 0), 1, 0.1, 0, 0.1, 0.02);
                        }
                    }.runTaskTimer(plugin, 0, 1);
                } else p.sendMessage("§7No target in range.");
            }

            // ── GRAVITY — Jump ────────────────────────────────────────────
case GRAVITY -> {
    w.spawnParticle(Particle.REVERSE_PORTAL, loc, 80, 0.8, 0.5, 0.8, 0.12);
    w.spawnParticle(Particle.PORTAL, loc, 50, 0.5, 0.3, 0.5, 0.08);
    w.spawnParticle(Particle.EXPLOSION, loc, 2, 0, 0, 0, 0);

    for (int i = 0; i < 16; i++) {
        double a = Math.toRadians(i * 22.5);
        w.spawnParticle(Particle.REVERSE_PORTAL,
                loc.clone().add(Math.cos(a) * 1.5, 0.1, Math.sin(a) * 1.5),
                3, 0, 0.3, 0, 0.05);
    }

    w.playSound(loc, Sound.ENTITY_GHAST_SHOOT, 0.9f, 0.4f);
    w.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 0.6f);

    // Jump
    p.setVelocity(new Vector(0, 8.0, 0));

    // Delay after jump
    Bukkit.getScheduler().runTaskLater(plugin, () -> {

        if (!p.isOnline()) return;

        // Apply long slow falling
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 9999, 0, false, false, false));

        // Check until player touches ground
        new BukkitRunnable() {
            @Override
            public void run() {

                if (!p.isOnline()) {
                    p.removePotionEffect(PotionEffectType.SLOW_FALLING);
                    cancel();
                    return;
                }

                // When player touches ground
                if (p.isOnGround()) {
                    p.removePotionEffect(PotionEffectType.SLOW_FALLING);

                    // Remove fall damage completely
                    p.setFallDistance(0);

                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 2L); // check every 2 ticks

    }, 40L);
}

            // ── WIND — Push ───────────────────────────────────────────────
            case WIND -> {
                w.spawnParticle(Particle.WHITE_ASH, loc, 120, 2.5, 1, 2.5, 0.22);
                w.spawnParticle(Particle.CLOUD, loc, 70, 2.0, 0.6, 2.0, 0.10);
                w.spawnParticle(Particle.EXPLOSION, loc, 2, 0.5, 0, 0.5, 0);
                for (int ring = 1; ring <= 4; ring++) { final int fr = ring;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        for (int i = 0; i < 20; i++) { double a = Math.toRadians(i*18);
                            w.spawnParticle(Particle.WHITE_ASH, loc.clone().add(Math.cos(a)*(fr*2.5),0.5,Math.sin(a)*(fr*2.5)), 4, 0, 0.3, 0, 0.06);
                        }
                    }, fr * 3L);
                }
                w.playSound(loc, Sound.ENTITY_PHANTOM_DEATH, 1f, 0.4f);
                w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 0.8f);
                p.getNearbyEntities(12, 12, 12).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        Vector vel = e.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(3.5).setY(0.7);
                        le.damage(3.0 * dm, p);
                        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (e.isValid()) e.setVelocity(vel); }, 1L);
                    }
                });
            }

            // ── POISON — Cloud ────────────────────────────────────────────
            case POISON -> {
                w.spawnParticle(Particle.ITEM_SLIME, loc, 140, 3.0, 0.8, 3.0, 0.12);
                w.spawnParticle(Particle.SNEEZE, loc, 90, 3.0, 0.8, 3.0, 0.07);
                w.spawnParticle(Particle.DUST, loc, 70, 2.5, 0.6, 2.5, 0, new Particle.DustOptions(Color.fromRGB(40,200,40), 1.8f));
                for (int i = 0; i < 16; i++) { double a = Math.toRadians(i*22.5);
                    w.spawnParticle(Particle.ITEM_SLIME, loc.clone().add(Math.cos(a)*4,0.8,Math.sin(a)*4), 5, 0, 0.4, 0, 0.04);
                }
                w.playSound(loc, Sound.ENTITY_SLIME_SQUISH, 1f, 0.3f);
                w.playSound(loc, Sound.ENTITY_WITCH_DRINK, 0.8f, 0.5f);
                p.getNearbyEntities(8, 8, 8).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        le.addPotionEffect(new PotionEffect(PotionEffectType.POISON,   logic.ticks(200, dr), 2));
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, logic.ticks(100, dr), 1));
                        le.addPotionEffect(new PotionEffect(PotionEffectType.WITHER,   logic.ticks(60,  dr), 0));
                        le.damage(3.0 * dm, p);
                    }
                });
            }

            // ── LIGHT — Continuous Beam (4 seconds, owner safe) ────────────────────
case LIGHT -> {
    final Vector beamDir = p.getEyeLocation().getDirection().normalize();
    final Location startLoc = p.getEyeLocation().clone();
    final int DURATION_TICKS = 80;          // 4 seconds
    final double MAX_RANGE = 50.0;          // beam max length
    final double DAMAGE_PER_HIT = 4.0;      // 2 hearts
    final int DAMAGE_COOLDOWN_TICKS = 10;    // damage only once per 0.5 seconds per entity
    final java.util.Map<UUID, Integer> lastDamageTick = new java.util.HashMap<>();

    new BukkitRunnable() {
        int ticks = 0;

        @Override
        public void run() {
            // 1. Agar player offline ho jaye ya 4 second pure ho jaye toh cancel
            if (!p.isOnline() || ticks >= DURATION_TICKS) {
                this.cancel(); // Isse particles hamesha ke liye gayab ho jayenge
                return;
            }

            Location eyeLoc = p.getEyeLocation();
            Vector dir = eyeLoc.getDirection().normalize();
            
            // ✅ FIX 1: Beam ko player ke hitbox se 0.5 blocks aage se shuru karo
            // Taaki beam aapke apne aap se na takraye
            Location beamStart = eyeLoc.clone().add(dir.clone().multiply(0.5));

            // ✅ FIX 2: RayTrace mein ignore filter lagao
            org.bukkit.util.RayTraceResult ray = w.rayTraceBlocks(beamStart, dir, MAX_RANGE,
                    FluidCollisionMode.NEVER, true);
            
            double distance = (ray != null && ray.getHitBlock() != null)
                    ? ray.getHitBlock().getLocation().distance(beamStart)
                    : MAX_RANGE;

            // --- Particles Spawn Logic ---
            int steps = (int) (distance * 3); // 3 particles per block kaafi hain
            for (int i = 0; i <= steps; i++) {
                double t = (double) i / steps;
                Location point = beamStart.clone().add(dir.clone().multiply(t * distance));
                
                // White Core
                w.spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0,
                        new Particle.DustOptions(Color.WHITE, 1.5f));
                // Outer Glow
                if (i % 2 == 0) {
                    w.spawnParticle(Particle.END_ROD, point, 1, 0.01, 0.01, 0.01, 0.01);
                }
            }

            // --- Damage Logic ---
            // Yahan bhi beamStart use karo taaki khud ko damage na ho
            org.bukkit.util.RayTraceResult entityRay = w.rayTraceEntities(beamStart, dir, distance,
                    0.5, e -> e instanceof LivingEntity && !e.equals(p));
            
            if (entityRay != null && entityRay.getHitEntity() instanceof LivingEntity target) {
                UUID targetId = target.getUniqueId();
                if (ticks - lastDamageTick.getOrDefault(targetId, -100) >= DAMAGE_COOLDOWN_TICKS) {
                    target.damage(DAMAGE_PER_HIT, p);
                    lastDamageTick.put(targetId, ticks);
                    target.getWorld().spawnParticle(Particle.FLASH, target.getLocation().add(0, 1, 0), 1);
                }
            }

            ticks++;
        }
    }.runTaskTimer(plugin, 0, 1);
}
            // ── EARTH — Slam ──────────────────────────────────────────────
            // ✅ FIX: BLOCK_CRACK instead of DUST_PLUME
            case EARTH -> {
                w.spawnParticle(Particle.DUST_PLUME, loc, 160, 3, 0.2, 3, 0.26, Material.DIRT.createBlockData());
                w.spawnParticle(Particle.DUST_PLUME, loc, 100, 3, 0.2, 3, 0.26, Material.STONE.createBlockData());
                w.spawnParticle(Particle.EXPLOSION, loc, 4, 0.9, 0, 0.9, 0);
                for (int ring = 1; ring <= 5; ring++) { final int fr = ring;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        double r = fr * 1.9;
                        for (int i = 0; i < 24; i++) { double a = Math.toRadians(i*15);
                            w.spawnParticle(Particle.DUST_PLUME, loc.clone().add(Math.cos(a)*r,0.2,Math.sin(a)*r), 4, 0, 0.6, 0, 0.14, Material.STONE.createBlockData());
                        }
                    }, ring * 3L);
                }
                w.playSound(loc, Sound.BLOCK_STONE_BREAK, 1f, 0.3f);
                w.playSound(loc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 0.4f);
                w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.5f);
                p.getNearbyEntities(10, 10, 10).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        Vector vel = e.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(2.8).setY(0.9);
                        le.damage(6.0 * dm, p);
                        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (e.isValid()) e.setVelocity(vel); }, 1L);
                    }
                });
            }

            // ── CRYSTAL — Crystal Rain (Aim-based, 5s, 8 crystals) ──────────────────────
// ── CRYSTAL — Crystal Rain FIXED ──────────────────────────────────────────────
case CRYSTAL -> {
    LivingEntity target = logic.aim(p, 30);
    if (target == null) {
        target = p.getWorld().getNearbyEntities(p.getLocation(), 15, 8, 15).stream()
            .filter(e -> e instanceof LivingEntity && e != p)
            .map(e -> (LivingEntity) e)
            .findFirst().orElse(null);
        if (target == null) { p.sendMessage("\u00a7cNo target!"); return; }
    }

    final LivingEntity tgt = target;

    w.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.2f);
    w.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 0.8f);
    p.sendTitle("\u00a7b\u00a7lCRYSTAL RAIN", "\u00a77Incoming!", 5, 40, 10);

    // FIX: Drop all 8 crystals evenly over 100 ticks (5s)
    // Crystal 1 at tick 0, 2 at tick 12, 3 at tick 24 ... 8 at tick 84
    for (int c = 0; c < 8; c++) {
        final int fc = c;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!tgt.isValid() || tgt.isDead()) return;

            // FIX: aim AT target — small random spread (1.5 blocks max)
            Location tLoc = tgt.getLocation().clone().add(
                (Math.random()-0.5)*3.0, 0, (Math.random()-0.5)*3.0);

            // FIX: spawn from exactly 20 blocks above target
            Location spawnLoc = tLoc.clone().add(0, 20, 0);

            // FIX: fast fall speed -2.5 (was -0.5)
            org.bukkit.entity.FallingBlock crystal = w.spawnFallingBlock(
                spawnLoc, org.bukkit.Material.AMETHYST_BLOCK.createBlockData());
            crystal.setDropItem(false);
            crystal.setHurtEntities(false);
            crystal.setGravity(true);
            crystal.setVelocity(new Vector(0, -2.5, 0));

            // Trail + impact detection
            new BukkitRunnable() {
                int t = 0;
                boolean hit = false;
                public void run() {
                    if (t++ > 60 || hit) { if (crystal.isValid()) crystal.remove(); cancel(); return; }
                    if (!crystal.isValid()) { cancel(); return; }

                    Location cur = crystal.getLocation();

                    // Trail particles
                    w.spawnParticle(Particle.END_ROD, cur, 2, 0.1,0.1,0.1, 0.01);
                    w.spawnParticle(Particle.DUST, cur, 2, 0.1,0.1,0.1, 0,
                        new Particle.DustOptions(Color.fromRGB(150,200,255), 1.4f));

                    // FIX: impact check — when near ground or target
                    boolean nearGround = cur.getBlock().getType().isSolid()
                        || cur.clone().subtract(0,0.5,0).getBlock().getType().isSolid();
                    boolean nearTarget = tgt.isValid() &&
                        cur.distanceSquared(tgt.getLocation().add(0,1,0)) < 3.0;

                    if (nearGround || nearTarget) {
                        hit = true;
                        // Impact particles
                        w.playSound(cur, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1f, 0.8f);
                        w.playSound(cur, Sound.BLOCK_AMETHYST_BLOCK_BREAK,   1f, 1.2f);
                        w.spawnParticle(Particle.DUST, cur, 20, 0.5,0.3,0.5, 0,
                            new Particle.DustOptions(Color.fromRGB(180,220,255), 1.8f));
                        w.spawnParticle(Particle.END_ROD, cur, 10, 0.4,0.2,0.4, 0.08);
                        w.spawnParticle(Particle.EXPLOSION, cur, 1, 0,0,0, 0);

                        // FIX: damage + push INTO ground (strong downward)
                        for (org.bukkit.entity.Entity e :
                                w.getNearbyEntities(cur, 2.5, 2.5, 2.5)) {
                            if (!(e instanceof LivingEntity le) || e.equals(p)) continue;
                            if (e instanceof Player ep &&
                                (ep.getGameMode()==GameMode.CREATIVE||ep.getGameMode()==GameMode.SPECTATOR)) continue;

                            le.damage(ecfg("CRYSTAL","primary-damage",2.0) * dm, p);

                            // FIX: strong downward push — into ground
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                if (le.isValid()) {
                                    le.setVelocity(new Vector(
                                        (Math.random()-0.5)*0.5,
                                        -2.8,  // FIX: strong downward
                                        (Math.random()-0.5)*0.5));
                                }
                            }, 1L);

                            le.addPotionEffect(new PotionEffect(
                                PotionEffectType.SLOWNESS, 40, 2, false, false));
                        }
                        crystal.remove();
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 0, 1);

        }, fc * 12L); // FIX: 0,12,24,36,48,60,72,84 — 8 crystals in 100 ticks
    }

    // Overhead warning particles before crystals fall
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
        if (!tgt.isValid()) return;
        Location tLoc = tgt.getLocation().clone().add(0, 1.5, 0);
        for (int i = 0; i < 20; i++) {
            double a = Math.toRadians(i*18);
            w.spawnParticle(Particle.DUST,
                tLoc.clone().add(Math.cos(a)*2, 0, Math.sin(a)*2),
                1, 0,0,0, 0,
                new Particle.DustOptions(Color.fromRGB(150,200,255), 1.5f));
        }
        w.playSound(tLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 0.6f);
    }, 2L);
}
                
            // ── ECHO — Sonic Boom / Shockwave ─────────────────────────────
            case ECHO -> {
                LivingEntity tgt = logic.aim(p, 25);
                if (tgt != null) {
                    w.spawnParticle(Particle.SONIC_BOOM, tgt.getLocation(), 3, 0, 0, 0, 0);
                    w.spawnParticle(Particle.END_ROD, tgt.getLocation(), 70, 1, 1.5, 1, 0.1);
                    w.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1f, 0.4f);
                    w.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 1f, 1.0f);
                    Location beam = p.getEyeLocation().clone();
                    Vector   step = tgt.getLocation().toVector().subtract(p.getEyeLocation().toVector()).normalize().multiply(0.6);
                    int      steps = (int)(p.getEyeLocation().distance(tgt.getLocation()) / 0.6);
                    for (int i = 0; i < steps; i++) {
                        beam.add(step);
                        w.spawnParticle(Particle.END_ROD, beam, 2, 0.04, 0.04, 0.04, 0);
                        w.spawnParticle(Particle.SONIC_BOOM, beam, 1, 0, 0, 0, 0);
                        if (i%3==0) w.spawnParticle(Particle.WHITE_ASH, beam, 2, 0.05, 0.05, 0.05, 0.02);
                    }
                    Vector vel = tgt.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(4.0).setY(1.0);
                    tgt.damage(9.0 * dm, p);
                    tgt.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, logic.ticks(80, dr), 2));
                    Bukkit.getScheduler().runTaskLater(plugin, () -> { if (tgt.isValid()) tgt.setVelocity(vel); }, 1L);
                } else {
                    w.spawnParticle(Particle.SONIC_BOOM, loc, 4, 0, 0, 0, 0);
                    w.spawnParticle(Particle.END_ROD, loc, 90, 4, 4, 4, 0.08);
                    w.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 1f, 0.3f);
                    w.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 1f, 0.7f);
                    for (int ring = 1; ring <= 5; ring++) { final int fr = ring;
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            double r = fr * 2.2; int pts = 36;
                            for (int i = 0; i < pts; i++) { double a = Math.toRadians(i*(360.0/pts));
                                w.spawnParticle(Particle.END_ROD, loc.clone().add(Math.cos(a)*r, 0.15, Math.sin(a)*r), 1,0,0,0,0);
                                w.spawnParticle(Particle.SONIC_BOOM, loc.clone().add(Math.cos(a)*r, 1.0, Math.sin(a)*r), 1,0,0,0,0);
                                w.spawnParticle(Particle.END_ROD, loc.clone().add(Math.cos(a)*r, 2.0, Math.sin(a)*r), 1,0,0,0,0);
                            }
                            w.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.6f, 0.3f + fr*0.15f);
                        }, fr * 3L);
                    }
                    p.getNearbyEntities(12, 12, 12).forEach(e -> {
                        if (!(e instanceof LivingEntity le) || e == p) return;
                        Vector vel = e.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(2.5).setY(0.5);
                        le.damage(5.0 * dm, p);
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, logic.ticks(60, dr), 1));
                        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (e.isValid()) e.setVelocity(vel); }, 1L);
                        w.spawnParticle(Particle.SONIC_BOOM, le.getLocation(), 1, 0, 0, 0, 0);
                    });
                }
            }

            // ── RAGE PRIMARY — Demon King Throne ─────────────────────────────────────────
// 5 block aage throne summon — black netherblock structure
// Sky red, thorns spin up from ground, lightning, blaze guards
// Owner safely sits — 5s timeout if not sat, 2s after standing = remove
case RAGE -> {
    final Location throneBase = loc.clone().add(
        p.getLocation().getDirection().clone().setY(0).normalize().multiply(5));
    throneBase.setY(loc.getBlockY());

    // ── Sky Red — BEACON beam trick + red particles ───────────────────────
    new BukkitRunnable() {
        int t = 0;
        public void run() {
            if (t++ >= 200) { cancel(); return; }
            for (Player viewer : throneBase.getWorld().getPlayers()) {
                if (viewer.getLocation().distanceSquared(throneBase) > 80*80) continue;
                // Red particles fill sky above player
                Location sky = viewer.getLocation().clone().add(0, 40, 0);
                for (int i = 0; i < 12; i++) {
                    double a = Math.random()*Math.PI*2;
                    double r = Math.random()*40;
                    viewer.spawnParticle(Particle.DUST,
                        sky.clone().add(Math.cos(a)*r, Math.random()*15-5, Math.sin(a)*r),
                        1, 0, 0, 0, 0,
                        new Particle.DustOptions(Color.fromRGB(180, 0, 0), 4.0f));
                }
                // RED beam shooting up from throne
                if (t % 2 == 0) {
                    for (double y = 0; y <= 60; y += 1.5) {
                        viewer.spawnParticle(Particle.DUST,
                            throneBase.clone().add(0, y, 0), 1, 0.1, 0, 0.1, 0,
                            new Particle.DustOptions(Color.fromRGB(200, 0, 0), 2.5f));
                    }
                }
            }
        }
    }.runTaskTimer(plugin, 0, 2);

    // ── Throne blocks ─────────────────────────────────────────────────────
    final java.util.Map<org.bukkit.block.Block, org.bukkit.block.data.BlockData> throneBlocks
        = new java.util.LinkedHashMap<>();

    org.bukkit.Material THRONE   = org.bukkit.Material.NETHER_BRICKS;
    org.bukkit.Material DARK     = org.bukkit.Material.BLACKSTONE;
    org.bukkit.Material SEAT_MAT = org.bukkit.Material.POLISHED_BLACKSTONE;
    org.bukkit.Material FIRE_MAT = org.bukkit.Material.NETHER_BRICK_FENCE;
    org.bukkit.Material LAVA_MAT = org.bukkit.Material.MAGMA_BLOCK;

    // Base platform
    int[][] base    = {{0,0,0},{1,0,0},{-1,0,0},{0,0,1},{0,0,-1},{1,0,1},{-1,0,1},{1,0,-1},{-1,0,-1}};
    int[][] pillars = {{-1,1,-1},{1,1,-1},{-1,2,-1},{1,2,-1},{-1,3,-1},{1,3,-1},{-1,4,-1},{1,4,-1}};
    int[][] arms    = {{-2,1,0},{2,1,0},{-2,2,0},{2,2,0},{-1,1,0},{1,1,0}};
    int[][] back    = {{0,1,-1},{0,2,-1},{0,3,-1},{-1,3,-1},{1,3,-1},{0,4,-1}};
    int[][] top     = {{-2,4,-1},{2,4,-1},{0,5,-1},{-1,5,-1},{1,5,-1},{0,4,-1}};
    int[][] seat    = {{0,1,0}};
    int[][] steps   = {{0,0,1},{-1,0,1},{1,0,1}};
    int[][] fences  = {{-1,1,-2},{1,1,-2},{-1,2,-2},{1,2,-2}};  // side pillars
    int[][] magma   = {{0,0,0},{-1,0,-1},{1,0,-1}};             // magma accents

    int delay = 0;
    // Place base
    for (int[] b : base) {
        final int[] fb = b; final org.bukkit.Material m = DARK;
        Bukkit.getScheduler().runTaskLater(plugin, () -> placeThrone(throneBase,fb,m,throneBlocks,w), delay++);
    }
    // Steps
    for (int[] b : steps) {
        final int[] fb = b;
        Bukkit.getScheduler().runTaskLater(plugin, () -> placeThrone(throneBase,fb,LAVA_MAT,throneBlocks,w), 3+delay++);
    }
    // Pillars
    for (int[] b : pillars) {
        final int[] fb = b;
        Bukkit.getScheduler().runTaskLater(plugin, () -> placeThrone(throneBase,fb,THRONE,throneBlocks,w), 5+delay++/2);
    }
    // Arms
    for (int[] b : arms) {
        final int[] fb = b;
        Bukkit.getScheduler().runTaskLater(plugin, () -> placeThrone(throneBase,fb,THRONE,throneBlocks,w), 7+delay++/2);
    }
    // Back
    for (int[] b : back) {
        final int[] fb = b;
        Bukkit.getScheduler().runTaskLater(plugin, () -> placeThrone(throneBase,fb,DARK,throneBlocks,w), 8+delay++/2);
    }
    // Fences
    for (int[] b : fences) {
        final int[] fb = b;
        Bukkit.getScheduler().runTaskLater(plugin, () -> placeThrone(throneBase,fb,FIRE_MAT,throneBlocks,w), 9+delay++/2);
    }
    // Top
    for (int[] b : top) {
        final int[] fb = b;
        Bukkit.getScheduler().runTaskLater(plugin, () -> placeThrone(throneBase,fb,THRONE,throneBlocks,w), 10+delay++/2);
    }
    // Seat
    for (int[] b : seat) {
        final int[] fb = b;
        Bukkit.getScheduler().runTaskLater(plugin, () -> placeThrone(throneBase,fb,SEAT_MAT,throneBlocks,w), 12);
    }
    // Magma accents
    for (int[] b : magma) {
        final int[] fb = b;
        Bukkit.getScheduler().runTaskLater(plugin, () -> placeThrone(throneBase,fb,LAVA_MAT,throneBlocks,w), 13+delay++/3);
    }

    // ── Thorns ────────────────────────────────────────────────────────────
    final org.bukkit.entity.FallingBlock[] thorns = new org.bukkit.entity.FallingBlock[4];
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
        w.strikeLightningEffect(throneBase.clone().add(0, 3, 0));
        w.playSound(throneBase, Sound.ENTITY_WITHER_SPAWN, 0.8f, 0.5f);
        for (int i = 0; i < 4; i++) {
            final int fi = i;
            double a = Math.toRadians(i * 90);
            org.bukkit.entity.FallingBlock thorn = w.spawnFallingBlock(
                throneBase.clone().add(Math.cos(a)*2.5, 0, Math.sin(a)*2.5),
                org.bukkit.Material.NETHER_BRICKS.createBlockData());
            thorn.setDropItem(false); thorn.setHurtEntities(false); thorn.setGravity(false);
            thorns[fi] = thorn;
        }
        new BukkitRunnable() {
            int t = 0; double thornAngle = 0;
            public void run() {
                if (t++ >= 40) { cancel(); return; }
                thornAngle += 0.08;
                for (int i = 0; i < 4; i++) {
                    if (thorns[i] == null || !thorns[i].isValid()) continue;
                    double ba = Math.toRadians(i*90) + thornAngle;
                    double wave = Math.sin(t*0.3)*0.3;
                    Location nl = throneBase.clone().add(Math.cos(ba)*2.5, 1.0+(t*0.08)+wave, Math.sin(ba)*2.5);
                    thorns[i].teleport(nl);
                    w.spawnParticle(Particle.LARGE_SMOKE, nl, 2, 0.1,0.1,0.1, 0.01);
                    w.spawnParticle(Particle.DUST, nl, 1, 0,0,0, 0,
                        new Particle.DustOptions(Color.fromRGB(80,0,0), 1.5f));
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }, 15L);

    // ── State ─────────────────────────────────────────────────────────────
    final boolean[] ownerSeated  = {false};
    final boolean[] throneActive = {true};
    final org.bukkit.entity.Blaze[] guards = {null, null};
    final Location seatLoc = throneBase.clone().add(0, 2, 0);

    // ── Summon guards ─────────────────────────────────────────────────────
    final Runnable summonGuards = () -> {
        for (int i = 0; i < 2; i++) {
            double a = Math.toRadians(i*180);
            Location gLoc = throneBase.clone().add(Math.cos(a)*3.5, 0, Math.sin(a)*3.5);
            org.bukkit.entity.Blaze blaze = (org.bukkit.entity.Blaze)
                w.spawnEntity(gLoc, org.bukkit.entity.EntityType.BLAZE);
            blaze.setAware(false);          // FIX: won't move or attack
            blaze.setInvulnerable(true);    // FIX: rain/players can't kill them
            blaze.setRemoveWhenFarAway(false);
            blaze.setCustomName("\u00a74\u00a7lDemon Guard");
            blaze.setCustomNameVisible(true);
            blaze.setTarget(null);
            guards[i] = blaze;
        }
    };

    // ── Main task ─────────────────────────────────────────────────────────
    new BukkitRunnable() {
        int unseated = 0, total = 0, ltimer = 0;

        public void run() {
            total++; ltimer++;
            if (!throneActive[0] || !p.isOnline()) { removeAll(); cancel(); return; }

            boolean onThrone = p.getLocation().distanceSquared(seatLoc) < 1.5*1.5;

            if (onThrone) {
                if (!ownerSeated[0]) {
                    ownerSeated[0] = true;
                    unseated = 0;
                    summonGuards.run();
                    p.sendTitle("\u00a74\u00a7lDEMON THRONE","\u00a77Power flows...",5,40,10);
                    w.playSound(seatLoc, Sound.ENTITY_WITHER_AMBIENT, 1f, 0.4f);
                }
                unseated = 0;

                // FIX: slap owner up slightly while seated (bouncy throne feel)
                if (total % 30 == 0) {
                    Vector cur = p.getVelocity();
                    if (cur.getY() < 0.1) p.setVelocity(cur.setY(0.25));
                }

                // Damage enemies
                if (total % 20 == 0) {
                    for (org.bukkit.entity.Entity e : w.getNearbyEntities(throneBase, 8, 8, 8)) {
                        if (!(e instanceof LivingEntity le) || e.equals(p)) continue;
                        if (guards[0]!=null&&e.equals(guards[0])) continue;
                        if (guards[1]!=null&&e.equals(guards[1])) continue;
                        if (e instanceof Player ep&&(ep.getGameMode()==GameMode.CREATIVE||ep.getGameMode()==GameMode.SPECTATOR)) continue;
                        le.damage(3.0*dm, p);
                    }
                }
            } else {
                unseated++;
                if (!ownerSeated[0] && total >= 100) {
                    throneActive[0] = false; removeAll();
                    p.sendMessage("\u00a7c\u00a7lThrone faded."); cancel(); return;
                }
                if (ownerSeated[0] && unseated >= 40) {
                    throneActive[0] = false; removeAll(); cancel(); return;
                }
            }

            if (ltimer >= 60) {
                ltimer = 0;
                w.strikeLightningEffect(seatLoc);
                w.playSound(seatLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.7f, 0.6f);
            }
            if (total % 5 == 0) {
                double ra = Math.random()*Math.PI*2;
                w.spawnParticle(Particle.LARGE_SMOKE,
                    throneBase.clone().add(Math.cos(ra)*2, Math.random()*4, Math.sin(ra)*2),
                    1,0.1,0.1,0.1,0.01);
                w.spawnParticle(Particle.FLAME,
                    throneBase.clone().add(Math.cos(ra)*1.5, 1+Math.random()*3, Math.sin(ra)*1.5),
                    1,0.05,0.05,0.05,0.02);
            }
        }

        void removeAll() {
            for (java.util.Map.Entry<org.bukkit.block.Block,org.bukkit.block.data.BlockData> e
                    : throneBlocks.entrySet())
                e.getKey().setBlockData(e.getValue());
            throneBlocks.clear();
            for (org.bukkit.entity.Blaze g : guards)
                if (g!=null&&g.isValid()) { g.getWorld().spawnParticle(Particle.LARGE_SMOKE,g.getLocation(),10,0.3,0.5,0.3,0.05); g.remove(); }
            for (org.bukkit.entity.FallingBlock t : thorns)
                if (t!=null&&t.isValid()) t.remove();
            if (p.isOnline()) {
                w.playSound(throneBase, Sound.BLOCK_BEACON_DEACTIVATE, 1f, 0.5f);
                w.spawnParticle(Particle.LARGE_SMOKE, throneBase.clone().add(0,2,0), 30,1,2,1,0.05);
            }
        }
    }.runTaskTimer(plugin, 20L, 1L);

    w.playSound(loc, Sound.ENTITY_RAVAGER_ROAR, 1f, 0.4f);
    w.playSound(loc, Sound.ENTITY_WITHER_AMBIENT, 0.8f, 0.3f);
    w.spawnParticle(Particle.LARGE_SMOKE, throneBase, 20, 1,1,1, 0.05);
    w.spawnParticle(Particle.DUST, throneBase, 40, 1.5,0.5,1.5, 0,
        new Particle.DustOptions(Color.fromRGB(120,0,0), 2.5f));
}



            // ── SPIRIT — Block Projectiles ────────────────────────────────
            case SPIRIT -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, logic.ticks(100, dr), 2));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,      logic.ticks(80,  dr), 1));
                w.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.9f, 1.4f);
                w.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.8f, 1.2f);
                p.sendTitle("\u00a77\u00a7lSPIRIT FORM", "\u00a78Blocks rising...", 5, 60, 10);
                int[][] offsets = {{0,0,2},{0,0,-2},{2,0,0},{-2,0,0}};
                for (int[] off : offsets) {
                    Location sideLoc = loc.clone().add(off[0], 0, off[2]);
                    org.bukkit.block.Block surface = sideLoc.getBlock();
                    for (int gy = 0; gy > -10; gy--) {
                        org.bukkit.block.Block check = sideLoc.clone().add(0, gy, 0).getBlock();
                        if (check.getType().isSolid()) { surface = check; break; }
                    }
                    final org.bukkit.block.Block groundBlock = surface;
                    final Material blockMat = groundBlock.getType().isSolid() ? groundBlock.getType() : Material.GRASS_BLOCK;
                    final Location spawnLoc = groundBlock.getLocation().clone().add(0.5, 0.0, 0.5);
                    groundBlock.setType(Material.AIR);
                    FallingBlock fb = w.spawnFallingBlock(spawnLoc, blockMat.createBlockData());
                    fb.setDropItem(false); fb.setHurtEntities(false); fb.setGravity(false);
                    w.spawnParticle(Particle.TOTEM_OF_UNDYING, spawnLoc, 20, 0.3, 0.3, 0.3, 0.06);
                    w.spawnParticle(Particle.END_ROD, spawnLoc, 10, 0.2, 0.2, 0.2, 0.03);
                    new BukkitRunnable() {
                        int ticks = 0;
                        public void run() {
                            if (!fb.isValid()) { cancel(); return; }
                            ticks++;
                            if (ticks <= 60) {
                                fb.setVelocity(new Vector(0, 0.033, 0));
                                w.spawnParticle(Particle.TOTEM_OF_UNDYING, fb.getLocation(), 3, 0.15, 0.1, 0.15, 0.03);
                                w.spawnParticle(Particle.WITCH, fb.getLocation(), 1, 0.1, 0.1, 0.1, 0.02);
                                if (ticks==1) w.playSound(fb.getLocation(), Sound.BLOCK_STONE_BREAK, 0.8f, 0.5f);
                            } else if (ticks == 61) {
                                double dx=off[0], dz=off[2];
                                Vector shootDir = new Vector(dx, 0, dz).normalize().multiply(3.5); shootDir.setY(0.3);
                                w.playSound(fb.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1f, 1.2f);
                                w.playSound(fb.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 0.8f);
                                w.spawnParticle(Particle.TOTEM_OF_UNDYING, fb.getLocation(), 30, 0.4, 0.2, 0.4, 0.08);
                                fb.setGravity(true); fb.setVelocity(shootDir);
                            } else if (ticks <= 100) {
                                w.spawnParticle(Particle.TOTEM_OF_UNDYING, fb.getLocation(), 5, 0.2, 0.1, 0.2, 0.05);
                                w.spawnParticle(Particle.END_ROD, fb.getLocation(), 2, 0.1, 0.1, 0.1, 0.02);
                                for (org.bukkit.entity.Entity e : w.getNearbyEntities(fb.getLocation(), 1.5, 1.5, 1.5)) {
                                    if (e instanceof LivingEntity le && e != p) {
                                        le.damage(10.0 * dm, p);
                                        w.spawnParticle(Particle.TOTEM_OF_UNDYING, le.getLocation(), 20, 0.4, 0.8, 0.4, 0.06);
                                        Vector knock = fb.getLocation().toVector().subtract(spawnLoc.toVector()).normalize().multiply(1.5).setY(0.8);
                                        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (e.isValid()) le.setVelocity(knock); }, 1L);
                                    }
                                }
                            } else {
                                w.spawnParticle(Particle.FALLING_DUST, fb.getLocation(), 20, 0.5, 0.3, 0.5, 0.1, blockMat.createBlockData());
                                w.spawnParticle(Particle.TOTEM_OF_UNDYING, fb.getLocation(), 15, 0.3, 0.3, 0.3, 0.05);
                                w.playSound(fb.getLocation(), Sound.BLOCK_STONE_BREAK, 1f, 0.8f);
                                fb.remove(); cancel();
                            }
                        }
                    }.runTaskTimer(plugin, 0, 1);
                }
            }

            case TIME -> {
    java.util.UUID id = p.getUniqueId();
    long now = System.currentTimeMillis();
    int counts = dashCount.getOrDefault(id, 0);

    // Agar 5 second (5000ms) guzar gaye, toh reset kar do
    if (counts > 0 && (now - lastDashTime.getOrDefault(id, 0L)) > 5000) {
        counts = 0; 
    }

    // Cooldown Check (2 baar se zyada nahi)
    if (counts >= 2) {
        p.sendTitle("", "\u00a7c\u00a7lTime Dash on Cooldown!", 0, 20, 10);
        w.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
        return;
    }

    // Dash Count update karo
    dashCount.put(id, counts + 1);
    lastDashTime.put(id, now);

    // Dash Direction & Setup
    Location timeLoc = p.getLocation();
    Vector dashDir = p.getEyeLocation().getDirection().clone().setY(0).normalize();
    if (dashDir.length() < 0.1) dashDir = new Vector(1, 0, 0); // Failsafe
    final Vector finalDashDir = dashDir.clone();

    // Sound & Initial Blast
    w.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 2.0f);
    w.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_3, 1.0f, 0.5f);

    p.setVelocity(finalDashDir.clone().multiply(2.8).setY(0.2)); // 15 block ki speed

    final java.util.HashSet<java.util.UUID> hitTargets = new java.util.HashSet<>();

    new BukkitRunnable() {
        int t = 0;
        public void run() {
            if (!p.isOnline() || p.isDead() || t++ >= 10) { 
                p.setGliding(false); // Pose normal karna
                p.setFallDistance(0f); 
                cancel(); 
                
                // Agar ye pehla dash tha, toh 5 second baad clean up (Slowness wagera hatana)
                if (dashCount.getOrDefault(id, 0) == 1) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (dashCount.getOrDefault(id, 0) < 2) {
                            dashCount.put(id, 0); // Reset timer
                            p.sendMessage("\u00a7e\u00a7oTime flow normalized...");
                        }
                    }, 100L); // 5 seconds (100 ticks)
                }
                return; 
            }

            Location cur = p.getLocation();
            
            // 🏃 PLAYER POSE: Vanilla Gliding (Elytra) pose bina armor stand ke!
            p.setGliding(true); // Isse player aage ki taraf jhuka hua (running/flying) dikhega

            // 💨 WIND SHIELD (Aage ki taraf hawa cheerne ka effect)
            Location front = cur.clone().add(finalDashDir.clone().multiply(1.5)).add(0, 1, 0);
            w.spawnParticle(Particle.CLOUD, front, 5, 0.3, 0.3, 0.3, 0.05);
            w.spawnParticle(Particle.ENCHANTED_HIT, front, 10, 0.4, 0.4, 0.4, 0.2);

            // ⚡ ELECTRIC TRAIL (Piche se bijli nikalna)
            Location back = cur.clone().subtract(finalDashDir.clone().multiply(1.0)).add(0, 1, 0);
            w.spawnParticle(Particle.ELECTRIC_SPARK, back, 15, 0.4, 0.4, 0.4, 0.1);
            Particle.DustOptions redLightning = new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 50, 50), 1.5f);
            w.spawnParticle(Particle.DUST, back, 10, 0.2, 0.2, 0.2, 0, redLightning);

            // 🕒 TIME HIT DETECTION (Raste mein koi aaya toh Time Rewind)
            for (org.bukkit.entity.Entity e : cur.getWorld().getNearbyEntities(cur, 2.0, 2.0, 2.0)) {
                if (e instanceof org.bukkit.entity.LivingEntity victim && !e.equals(p) && !hitTargets.contains(e.getUniqueId())) {
                    hitTargets.add(e.getUniqueId());
                    
                    // Cinematic Rewind Math (Piche dhakelna aur freeze karna)
                    Vector rewindDir = finalDashDir.clone().multiply(-1.5).setY(0.5);
                    victim.setVelocity(rewindDir);
                    
                    // Slowness 10 for 5 seconds
                    victim.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 100, 10, false, false));
                    victim.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS, 40, 1, false, false));
                    
                    w.spawnParticle(Particle.REVERSE_PORTAL, victim.getLocation().add(0,1,0), 40, 0.5, 0.5, 0.5, 0.1);
                    w.playSound(victim.getLocation(), Sound.BLOCK_BELL_USE, 1.0f, 0.5f); // Clock tick jaisa sound
                }
            }

            // Speed Maintain karna
            if (p.getVelocity().length() < 1.0) p.setVelocity(finalDashDir.clone().multiply(2.0).setY(0.1));
            p.setFallDistance(0f);
        }
    }.runTaskTimer(plugin, 0L, 1L);
}

            // ── WARRIOR — Charge ──────────────────────────────────────────
            case WARRIOR -> {
                p.setVelocity(dir.clone().multiply(3.0).setY(0.3));
                w.spawnParticle(Particle.CRIT, loc, 60, 0.5, 0.8, 0.5, 0.12);
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

// ── OCEAN — Tsunami Wave with Water Flood ─────────────────────
case OCEAN -> {
    // Custom effects
    w.playSound(p.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 2f, 0.5f);
    w.playSound(p.getLocation(), Sound.ITEM_BUCKET_EMPTY_FISH, 2f, 0.8f);
    p.sendTitle("§3§l🌊 TSUNAMI", "§bThe ocean rises!", 5, 60, 10);

    // Aim direction (horizontal only)
    Vector direction = p.getEyeLocation().getDirection().clone();
    direction.setY(0);
    direction.normalize();

    // Wave dimensions
    final double WAVE_WIDTH = 12.0;   // radius for wave visual
    final double WAVE_HEIGHT = 35.0;  // wave height
    final double SPEED = 1.2;         // wave speed (blocks per second)
    final int DURATION_TICKS = 200;   // 10 seconds

    // Flood area: cylinder radius 30, from y=0 to y=35 (adjust as needed)
    final int FLOOD_RADIUS = 30;
    final int FLOOD_TOP_Y = (int) (p.getLocation().getY() + 35); // 35 blocks above player's feet
    final int FLOOR_Y = (int) p.getLocation().getY() - 1; // ground level

    // Store affected blocks (optional – for performance we won't store, just set water then air)
    // Instead we'll simply set water and later remove.

    // Wave start position
    Location waveCenter = p.getLocation().clone().add(direction.clone().multiply(3));
    waveCenter.setY(waveCenter.getY() + 1);

    final Set<UUID> affected = new HashSet<>();
    final boolean[] waveActive = {true};

    // ── Red sky effect ──────────────────────────────────────────────
    new BukkitRunnable() {
        int t = 0;
        @Override
        public void run() {
            if (!waveActive[0] || t++ >= DURATION_TICKS / 5) {
                cancel();
                return;
            }
            Location skyLoc = p.getLocation().clone().add(0, 80, 0);
            for (int i = 0; i < 30; i++) {
                double xOff = (Math.random() - 0.5) * 40;
                double zOff = (Math.random() - 0.5) * 40;
                w.spawnParticle(Particle.DUST, skyLoc.clone().add(xOff, Math.random() * 20, zOff),
                        1, 0, 0, 0, 0,
                        new Particle.DustOptions(Color.fromRGB(200, 50, 50), 2.5f));
            }
        }
    }.runTaskTimer(plugin, 0, 5);

    // ── Place water blocks (flood) – done in layers to avoid lag ─────
    // We'll schedule a repeating task that places one Y‑level each tick
    final int startY = FLOOR_Y;
    final int endY = FLOOD_TOP_Y;
    final int centerX = p.getLocation().getBlockX();
    final int centerZ = p.getLocation().getBlockZ();
    final int radius = FLOOD_RADIUS;

    // Store the locations of water blocks to remove later
    final java.util.List<Location> waterBlocks = new java.util.ArrayList<>();

    new BukkitRunnable() {
        int y = startY;
        @Override
        public void run() {
            if (!waveActive[0] || y > endY) {
                cancel();
                return;
            }
            // Place water at this Y level
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x*x + z*z <= radius*radius) {
                        Location blockLoc = new Location(w, centerX + x, y, centerZ + z);
                        // Only replace non-solid blocks (air, plants, etc.) to avoid griefing
                        if (!blockLoc.getBlock().getType().isSolid() || blockLoc.getBlock().getType() == Material.AIR) {
                            blockLoc.getBlock().setType(Material.WATER);
                            waterBlocks.add(blockLoc.clone());
                        }
                    }
                }
            }
            y++;
        }
    }.runTaskTimer(plugin, 0, 1); // one layer per tick

    // ── Wave movement and damage ─────────────────────────────────────
    new BukkitRunnable() {
        int ticks = 0;
        double distance = 0;

        @Override
        public void run() {
            if (!waveActive[0] || ticks >= DURATION_TICKS) {
                // Cleanup
                waveActive[0] = false;
                cancel();
                return;
            }

            distance += SPEED / 20.0;
            Location currentWaveCenter = waveCenter.clone().add(direction.clone().multiply(distance));

            // Spawn wave particles (visual only)
            for (double wX = -WAVE_WIDTH; wX <= WAVE_WIDTH; wX += 0.5) {
                Vector perp = new Vector(-direction.getZ(), 0, direction.getX()).normalize();
                Location pt = currentWaveCenter.clone().add(perp.clone().multiply(wX));
                for (double y = 0; y <= WAVE_HEIGHT; y += 1.0) {
                    double centerFactor = 1 - Math.abs(wX) / WAVE_WIDTH;
                    double heightFactor = y / WAVE_HEIGHT;
                    double intensity = Math.sin(Math.PI * heightFactor) * centerFactor;
                    if (intensity < 0.1) continue;
                    Location waterLoc = pt.clone().add(0, y, 0);
                    w.spawnParticle(Particle.SPLASH, waterLoc, 2, 0.2, 0.1, 0.2, 0.05);
                    w.spawnParticle(Particle.BUBBLE_POP, waterLoc, 3, 0.2, 0.1, 0.2, 0.02);
                    if (y % 3 == 0) {
                        w.spawnParticle(Particle.DRIPPING_WATER, waterLoc, 1, 0.1, 0.1, 0.1, 0);
                    }
                }
            }

            // Apply effects to nearby entities (damage, push)
            for (org.bukkit.entity.Entity e : w.getNearbyEntities(currentWaveCenter, WAVE_WIDTH + 5, WAVE_HEIGHT + 5, WAVE_WIDTH + 5)) {
                if (!(e instanceof LivingEntity le)) continue;
                if (e.equals(p)) continue;
                if (e instanceof Player ep && (ep.getGameMode() == GameMode.CREATIVE || ep.getGameMode() == GameMode.SPECTATOR)) continue;

                Vector toEntity = e.getLocation().toVector().subtract(currentWaveCenter.toVector());
                double horizontalDist = new Vector(toEntity.getX(), 0, toEntity.getZ()).length();
                if (horizontalDist <= WAVE_WIDTH + 1.5 && Math.abs(e.getLocation().getY() - currentWaveCenter.getY()) <= WAVE_HEIGHT + 1) {
                    if (!affected.contains(e.getUniqueId())) {
                        affected.add(e.getUniqueId());
                        le.damage(16.0, p); // 8 hearts initial damage
                        le.getWorld().playSound(le.getLocation(), Sound.ENTITY_PLAYER_HURT_DROWN, 1f, 1f);
                        // Drowning effect every second
                        new BukkitRunnable() {
                            int t = 0;
                            @Override
                            public void run() {
                                if (!le.isValid() || t++ >= (DURATION_TICKS - ticks) / 20) {
                                    cancel();
                                    return;
                                }
                                if (le.isValid() && !le.equals(p)) {
                                    le.damage(2.0, p);
                                    le.getWorld().spawnParticle(Particle.SPLASH, le.getLocation().add(0,1,0), 5, 0.3,0.3,0.3,0.05);
                                }
                            }
                        }.runTaskTimer(plugin, 20L, 20L);
                    }
                    le.setVelocity(direction.clone().multiply(0.5).setY(0.2));
                }
            }

            // Sound effects
            if (ticks % 10 == 0) {
                w.playSound(currentWaveCenter, Sound.ENTITY_DOLPHIN_SPLASH, 1.5f, 0.8f);
            }
            if (ticks % 20 == 0) {
                w.playSound(currentWaveCenter, Sound.AMBIENT_UNDERWATER_ENTER, 2f, 0.6f);
            }

            ticks++;
        }
    }.runTaskTimer(plugin, 0, 1);

// ── After 10 seconds, remove ALL water in a larger area (radius 40) ──
Bukkit.getScheduler().runTaskLater(plugin, () -> {
    waveActive[0] = false;

    // Use a larger radius to catch water that flowed out
    int removeRadius = FLOOD_RADIUS + 10; // 38 blocks
    int minX = centerX - removeRadius;
    int maxX = centerX + removeRadius;
    int minZ = centerZ - removeRadius;
    int maxZ = centerZ + removeRadius;

    for (int x = minX; x <= maxX; x++) {
        for (int z = minZ; z <= maxZ; z++) {
            // Optional: keep it as a square (faster) or use circular check
            // Circle check is nicer but square is simpler and covers everything
            // We'll do square for safety
            for (int y = FLOOR_Y; y <= FLOOD_TOP_Y; y++) {
                Location blockLoc = new Location(w, x, y, z);
                Material type = blockLoc.getBlock().getType();
                if (type == Material.WATER || type == Material.WATER_CAULDRON) {
                    blockLoc.getBlock().setType(Material.AIR);
                }
            }
        }
    }

    // Also remove any water that might be in the original waterBlocks list (extra safety)
    for (Location waterLoc : waterBlocks) {
    if (waterLoc.getBlock().getType() == Material.WATER) {
        waterLoc.getBlock().setType(Material.AIR);
    }
}
    waterBlocks.clear();

    p.sendMessage("§bThe tsunami subsides. The water recedes.");
    p.playSound(p.getLocation(), Sound.ITEM_BUCKET_EMPTY_FISH, 1f, 0.5f);
  }, DURATION_TICKS);
}

            // ── ECLIPSE — Orbital Strike ───────────────────────────────────
            // ✅ FIX: TNT block break ON, more rings (5 instead of 3)
            case ECLIPSE -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,   logic.ticks(200, dr), 3));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,        logic.ticks(200, dr), 2));
                p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, logic.ticks(220, dr), 0));
                p.setFallDistance(0f);
                // ✅ 5 rings instead of 3
                int   DROP_HEIGHT = 35;
                int[] ringRadii  = {3, 5, 8, 11, 14};
                int[] ringCounts = {6, 8, 12, 16, 20};
                final java.util.List<org.bukkit.entity.TNTPrimed> allTNT = new java.util.ArrayList<>();
                final Location center = loc.clone();
                for (int ring = 0; ring < 5; ring++) {
                    final int   fr     = ring;
                    final int   count  = ringCounts[ring];
                    final double radius = ringRadii[ring];
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        for (int i = 0; i < count; i++) {
                            double angle = Math.toRadians(i * (360.0 / count));
                            double tx = center.getX() + Math.cos(angle)*radius;
                            double tz = center.getZ() + Math.sin(angle)*radius;
                            Location spawnLoc = new Location(w, tx, center.getY() + DROP_HEIGHT, tz);
                            org.bukkit.entity.TNTPrimed tnt = w.spawn(spawnLoc, org.bukkit.entity.TNTPrimed.class);
                            tnt.setFuseTicks(999999);
                            tnt.setVelocity(new Vector(0, -1.5, 0));
                            // ✅ FIX: setYield > 0 allows block break
                            tnt.setYield(3.0f);
                            synchronized(allTNT) { allTNT.add(tnt); }
                            new BukkitRunnable() {
                                int t = 0;
                                public void run() {
                                    if (!tnt.isValid() || t++ > 35) { cancel(); return; }
                                    w.spawnParticle(Particle.FLAME, tnt.getLocation(), 2, 0.1, 0.1, 0.1, 0.02);
                                    w.spawnParticle(Particle.SMOKE, tnt.getLocation(), 1, 0.05, 0.05, 0.05, 0.01);
                                    p.setFallDistance(0f);
                                }
                            }.runTaskTimer(plugin, 0, 1);
                        }
                        w.playSound(center, Sound.ENTITY_WITHER_SHOOT, 0.8f, 1.5f + fr*0.2f);
                    }, ring * 3L);
                }
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    w.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 0.3f);
                    for (int i = 0; i < 36; i++) { double a = Math.toRadians(i*10);
                        w.spawnParticle(Particle.FLAME, center.clone().add(Math.cos(a)*14, 0.5, Math.sin(a)*14), 2,0,0,0, 0.05);
                    }
                }, 25L);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    synchronized(allTNT) {
                        for (org.bukkit.entity.TNTPrimed tnt : allTNT) {
                            if (!tnt.isValid()) continue;
                            Location blastLoc = tnt.getLocation().clone();
                            tnt.remove();
                            // ✅ Real explosion — block break hoga
                            blastLoc.getWorld().createExplosion(blastLoc, 5f, true, true, p);
                            w.spawnParticle(Particle.EXPLOSION_EMITTER, blastLoc, 1,0,0,0,0);
                            w.spawnParticle(Particle.FLAME, blastLoc, 10, 0.4, 0.3, 0.4, 0.1);
                            w.playSound(blastLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
                            // Damage owner-safe
                            blastLoc.getWorld().getNearbyEntities(blastLoc, 5, 5, 5).forEach(e -> {
                                if (!(e instanceof LivingEntity le)) return;
                                if (e.getUniqueId().equals(p.getUniqueId())) return; // ✅ owner safe
                                le.damage(logic.ecfg("ECLIPSE","secondary-damage",8.0) * logic.getDmg(p), p);
                                Vector vel = e.getLocation().toVector().subtract(blastLoc.toVector()).normalize().multiply(2.5).setY(0.8);
                                Bukkit.getScheduler().runTaskLater(plugin, () -> { if (e.isValid()) e.setVelocity(vel); }, 1L);
                            });
                        }
                        allTNT.clear();
                    }
                    for (int ring = 1; ring <= 5; ring++) { final int fr = ring;
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            double rad = fr * 3.0;
                            for (int i = 0; i < 36; i++) { double a = Math.toRadians(i*10);
                                w.spawnParticle(Particle.EXPLOSION, center.clone().add(Math.cos(a)*rad, 0.3, Math.sin(a)*rad), 1,0,0,0,0);
                                w.spawnParticle(Particle.FLAME, center.clone().add(Math.cos(a)*rad, 0.5, Math.sin(a)*rad), 1,0,0,0, 0.03);
                            }
                            w.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.5f+fr*0.1f);
                        }, ring * 3L);
                    }
                    w.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 0.4f);
                    w.playSound(center, Sound.ENTITY_WITHER_BREAK_BLOCK, 1f, 0.3f);
                    p.setFallDistance(0f);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> { if (p.isOnline()) p.setFallDistance(0f); }, 20L);
                }, 40L);
            }

            // ── GUARDIAN — Titan Shockwave ────────────────────────────────
            case GUARDIAN -> {
                double eDmg = logic.ecfg("guardian","secondary-damage", 15.0) * dm;
                int    eRad = (int) logic.ecfg("guardian","secondary-radius", 20);
                w.spawnParticle(Particle.EXPLOSION, loc, 8, 2, 0, 2, 0);
                w.spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 150, 3.5, 2.5, 3.5, 0.14);
                w.spawnParticle(Particle.END_ROD, loc, 120, 3.5, 2.5, 3.5, 0.12);
                for (int wave = 1; wave <= 6; wave++) { final int fw = wave;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        double r = fw * eRad / 5.0;
                        for (int i = 0; i < 36; i++) { double a = Math.toRadians(i*10);
                            w.spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(Math.cos(a)*r,0.4,Math.sin(a)*r), 4, 0, 0.6, 0, 0.06);
                            w.spawnParticle(Particle.END_ROD, loc.clone().add(Math.cos(a)*r,0.4,Math.sin(a)*r), 2, 0, 0.3, 0, 0.02);
                        }
                    }, wave * 4L);
                }
                w.playSound(loc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 0.2f);
                w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.3f);
                w.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1f, 0.4f);
                p.getNearbyEntities(eRad, eRad, eRad).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        Vector vel = e.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(5.0).setY(1.8);
                        le.damage(eDmg, p);
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, logic.ticks(120, dr), 3));
                        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (e.isValid()) e.setVelocity(vel); }, 1L);
                        w.spawnParticle(Particle.TOTEM_OF_UNDYING, le.getLocation(), 25, 0.4, 0.7, 0.4, 0.05);
                    }
                });
                p.sendTitle("§6§l⚡ TITAN SHOCKWAVE!", "§7The ground shatters!", 5, 70, 15);
            }

// ── METEOR SECONDARY — 4 Pillar Trap (Thin, Black, 4 blocks away) ─────────
case METEOR -> {
    // ── Aim ─────────────────────────────────────────────────────────────────
    LivingEntity target = logic.aim(p, 30);
    if (target == null) {
        p.sendMessage("\u00a7cNo target in range!");
        return;
    }

    final Location center = target.getLocation().clone();
    final boolean[] done  = {false};

    // ── Block teleport/enderman escape ───────────────────────────────────
    final org.bukkit.event.Listener trapListener = new org.bukkit.event.Listener() {
        @org.bukkit.event.EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
        public void onTeleport(org.bukkit.event.player.PlayerTeleportEvent e) {
            if (done[0]) return;
            if (!e.getPlayer().getUniqueId().equals(target.getUniqueId())) return;
            e.setCancelled(true);
        }
        @org.bukkit.event.EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
        public void onEntityTeleport(org.bukkit.event.entity.EntityTeleportEvent e) {
            if (done[0]) return;
            if (!e.getEntity().getUniqueId().equals(target.getUniqueId())) return;
            e.setCancelled(true);
        }
    };
    plugin.getServer().getPluginManager().registerEvents(trapListener, plugin);

    // ── Sound + announcement ─────────────────────────────────────────────
    w.playSound(p.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1f, 0.4f);
    w.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 0.6f);
    p.sendTitle("\u00a76\u00a7lPILLAR TRAP", "\u00a77Rising...", 5, 40, 10);

    // ── Pillar block storage ──────────────────────────────────────────────
    final java.util.List<org.bukkit.block.Block> pillarBlocks = new java.util.ArrayList<>();
    final int PILLAR_H = 8;
    final double RADIUS = 4.0;   // ✅ 4 blocks away from enemy

    // 4 pillars at N/S/E/W – each 1x1 (thin)
    final double[] pillarAngles = {0, 90, 180, 270};
    final Location[] pillarBases = new Location[4];
    for (int i = 0; i < 4; i++) {
        double a = Math.toRadians(pillarAngles[i]);
        pillarBases[i] = center.clone().add(Math.cos(a) * RADIUS, 0, Math.sin(a) * RADIUS);
        pillarBases[i].setY(center.getBlockY());
    }

    // ── PHASE 1: Rise pillars layer by layer (thin, nether bricks) ─────────
    new BukkitRunnable() {
        int layer = 0;
        public void run() {
            if (layer >= PILLAR_H) { cancel(); return; }

            for (int i = 0; i < 4; i++) {
                Location base = pillarBases[i];
                // Single block per layer (thin pillar)
                org.bukkit.block.Block blk = base.clone().add(0, layer, 0).getBlock();
                if (!blk.getType().isSolid()) {
                    // Use nether bricks (blackish)
                    blk.setType(org.bukkit.Material.NETHER_BRICKS);
                    pillarBlocks.add(blk);
                    // Rise particles
                    w.spawnParticle(Particle.DUST,
                        blk.getLocation().add(0.5, 1, 0.5), 2, 0.2, 0.2, 0.2, 0,
                        new Particle.DustOptions(Color.fromRGB(80, 80, 80), 1.5f));
                }
            }
            // Sound per layer
            w.playSound(center, Sound.BLOCK_STONE_PLACE, 0.8f, 0.5f + layer * 0.08f);
            layer++;
        }
    }.runTaskTimer(plugin, 0, 2);

    // ── PHASE 2: After pillars up — freeze + beam + damage ───────────────
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
        if (!target.isValid()) return;

        // Freeze target
        target.setVelocity(new Vector(0, 0, 0));
        if (target instanceof Player pt) {
            pt.setWalkSpeed(0f);
            pt.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 120, 128, false, false, false));
            pt.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,   120, 10,  false, false, false));
        }
        // Float target 3 blocks up
        target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 120, 2, false, false, false));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (target.isValid()) {
                target.removePotionEffect(PotionEffectType.LEVITATION);
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 120, 0, false, false, false));
            }
        }, 20L);

        w.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1f, 0.5f);
        w.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1f, 0.8f);

        // Beam animation
        new BukkitRunnable() {
            int ticks = 0;
            public void run() {
                if (ticks++ >= 120 || !target.isValid() || done[0]) {
                    done[0] = true;
                    cleanup();
                    cancel(); return;
                }

                if (ticks % 5 == 0) {
                    target.setVelocity(new Vector(0, 0, 0));
                    if (target instanceof Player pt) pt.setWalkSpeed(0f);
                }

                Location tLoc = center.clone().add(0, 3, 0);

                for (int i = 0; i < 4; i++) {
                    Location pillarTop = pillarBases[i].clone().add(0, PILLAR_H, 0);
                    drawFireBeam(pillarTop, tLoc);
                }

                if (ticks % 10 == 0) {
                    if (target.isValid() && !target.equals(p)) {
                        target.damage(4.0 * logic.getDmg(p), p);
                        w.spawnParticle(Particle.FLAME, target.getLocation().add(0,1,0), 10, 0.4,0.6,0.4, 0.06);
                    }
                }

                if (ticks % 3 == 0) {
                    for (Location pb : pillarBases) {
                        Location top = pb.clone().add(0, PILLAR_H, 0);
                        w.spawnParticle(Particle.FLAME, top, 3, 0.3,0.1,0.3, 0.08);
                        w.spawnParticle(Particle.LAVA, top, 1, 0.2,0.1,0.2, 0.05);
                    }
                }
                if (ticks % 20 == 0) {
                    w.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1f, 1.2f);
                }
            }

            void cleanup() {
                org.bukkit.event.HandlerList.unregisterAll(trapListener);
                if (target.isValid()) {
                    target.removePotionEffect(PotionEffectType.LEVITATION);
                    target.removePotionEffect(PotionEffectType.SLOW_FALLING);
                    if (target instanceof Player pt) {
                        pt.setWalkSpeed(0.2f);
                        pt.removePotionEffect(PotionEffectType.JUMP_BOOST);
                        pt.removePotionEffect(PotionEffectType.SLOWNESS);
                    }
                }
                for (org.bukkit.block.Block blk : pillarBlocks) {
                    if (blk.getType() == org.bukkit.Material.NETHER_BRICKS) {
                        blk.setType(org.bukkit.Material.AIR);
                        w.spawnParticle(Particle.DUST,
                            blk.getLocation().add(0.5,0.5,0.5), 3, 0.2,0.2,0.2, 0,
                            new Particle.DustOptions(Color.fromRGB(80,80,80), 1.2f));
                    }
                }
                pillarBlocks.clear();
                w.strikeLightningEffect(center);
                w.spawnParticle(Particle.EXPLOSION_EMITTER, center.clone().add(0,3,0), 3, 1,1,1, 0);
                w.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 0.6f);
            }
        }.runTaskTimer(plugin, 0, 1);
    }, 20L);
}

                
// ── MIRAGE — Warden Minions ────────────────────────────────────
case MIRAGE -> {
    w.playSound(p.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.5f, 0.5f);
    w.playSound(p.getLocation(), Sound.ENTITY_WARDEN_EMERGE, 1.0f, 0.7f);
    w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, p.getLocation(), 80, 2, 1, 2, 0.05);
    w.spawnParticle(Particle.SQUID_INK, p.getLocation(), 50, 1.5, 1, 1.5, 0.1);
   final java.util.List<Warden> minions = new java.util.ArrayList<>();
    for (int i = 0; i < 5; i++) {
        double angle = i * (Math.PI * 2 / 5);
        Location spawnLoc = p.getLocation().clone().add(Math.cos(angle)*3, 0, Math.sin(angle)*3);
        Warden warden = (Warden) w.spawnEntity(spawnLoc, org.bukkit.entity.EntityType.WARDEN);
        warden.setCustomName("§7" + p.getName() + "'s §3Minion");
        warden.setCustomNameVisible(true);
        warden.setRemoveWhenFarAway(false);

        warden.setAI(true);
        warden.setSilent(true);
        warden.setCollidable(false);
        
        // Owner ko darkness nahi lagega
        warden.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 999999, 0, false, false, false));
        
        warden.setMetadata("MinionOf", new org.bukkit.metadata.FixedMetadataValue(plugin, p.getUniqueId().toString()));
        minions.add(warden);
        w.spawnParticle(Particle.SONIC_BOOM, spawnLoc.add(0, 1, 0), 1, 0, 0, 0, 0);
    }
    
    // Follow owner (every 5 ticks)
    new BukkitRunnable() {
        public void run() {
            if (!p.isOnline()) { cancel(); return; }
            for (Warden m : minions) {
                if (!m.isValid()) continue;
                if (m.getLocation().distance(p.getLocation()) > 10) {
                    Location target = p.getLocation();
                    Location current = m.getLocation();
                    Vector dir = target.toVector().subtract(current.toVector()).normalize();
                    m.setVelocity(dir.multiply(0.4));
                }
                if (m.getTarget() != null && m.getTarget().equals(p)) {
                    m.setTarget(null);
                }
            }
        }
    }.runTaskTimer(plugin, 0, 5);
    
    // Remove after 10 seconds
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
        for (Warden m : minions) {
            if (m.isValid()) {
                w.spawnParticle(Particle.FLASH, m.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0.05);
                m.remove();
            }
        }
    }, 200L);

    // Backup removal after 12 seconds (force remove if any left)
Bukkit.getScheduler().runTaskLater(plugin, () -> {
    for (Warden m : minions) {
        if (m != null && m.isValid()) {
            m.remove();
          }
       }
       minions.clear();
         }, 240L);           
      }
    }
  }
    
 // HELPERS mein add karo
// Fire beam from point A to point B — like anime/movie fire beam
private void drawFireBeam(Location from, Location to) {
    Vector dir = to.toVector().subtract(from.toVector());
    double len = dir.length();
    if (len == 0) return;
    Vector step = dir.clone().normalize().multiply(0.4);
    int steps = (int)(len / 0.4);

    for (int i = 0; i <= steps; i++) {
        Location pt = from.clone().add(step.clone().multiply(i));

        // Core — bright yellow/orange
        from.getWorld().spawnParticle(Particle.DUST, pt, 2, 0.03, 0.03, 0.03, 0,
            new Particle.DustOptions(Color.fromRGB(255, 160, 0), 1.8f));
        // Flame
        from.getWorld().spawnParticle(Particle.FLAME, pt, 1, 0.04, 0.04, 0.04, 0.01);
        // Glow every 3rd
        if (i % 3 == 0)
            from.getWorld().spawnParticle(Particle.DUST, pt, 1, 0.06, 0.06, 0.06, 0,
                new Particle.DustOptions(Color.fromRGB(255, 80, 0), 1.4f));
        }
     }  

// HELPERS mein add karo — applySafeDamage se pehle
private void placeThrone(Location base, int[] offset, org.bukkit.Material mat,
                         java.util.Map<org.bukkit.block.Block,
                         org.bukkit.block.data.BlockData> map, World w) {
    org.bukkit.block.Block blk = base.clone().add(offset[0], offset[1], offset[2]).getBlock();
    if (!blk.getType().isSolid() || blk.getType() == org.bukkit.Material.AIR) {
        map.put(blk, blk.getBlockData().clone());
        blk.setType(mat);
        w.spawnParticle(Particle.LARGE_SMOKE,
            blk.getLocation().add(0.5, 1, 0.5), 2, 0.2, 0.2, 0.2, 0.02);
    }
 }
}
