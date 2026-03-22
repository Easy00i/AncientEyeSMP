package com.ancienteye;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ParticleTask extends BukkitRunnable {

    private final AncientEyePlugin plugin;
    private double rotation = 0;

    // All eyes — 4s delay (task runs every 5L, 80/5=16 cycles)
    private final java.util.Map<java.util.UUID, Integer> eyeDelay = new java.util.HashMap<>();

    public ParticleTask(AncientEyePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        rotation += 0.15;
        if (rotation > Math.PI * 2) rotation = 0;

        for (Player owner : Bukkit.getOnlinePlayers()) {
            EyeType eye = plugin.getPlayerData().getEye(owner);
            if (eye == null || eye == EyeType.NONE) continue;

            // Config check — particles off hain to skip
            if (!plugin.getConfig().getBoolean("settings.particles-enabled", true)) continue;

            // Har eye — 4s delay pehli baar
            int cnt = eyeDelay.getOrDefault(owner.getUniqueId(), 0) + 1;
            eyeDelay.put(owner.getUniqueId(), cnt);
            if (cnt < 16) continue; // 16 * 5L = 80 ticks = 4s

            Particle.DustOptions dust = (eye.color != null)
                    ? new Particle.DustOptions(eye.color, 0.9f) : null; // opacity thoda kam

            for (Player viewer : owner.getWorld().getPlayers()) {
                if (viewer.getLocation().distanceSquared(
                        owner.getLocation()) > 48 * 48) continue;
                spawnAura(viewer, owner, eye, dust);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  AURA — CENTER of player — helix legs se head tak
    //  Owner ko bhi dikhega — first-person ke liye particles thode side mein
    // ══════════════════════════════════════════════════════════════════════
    private void spawnAura(Player viewer, Player owner,
                           EyeType eye, Particle.DustOptions dust) {
        Location loc = owner.getLocation();

        switch (eye) {

            // FIX: SMOKE hataya — PORTAL se replace
            case VOID -> {
                centerHelix(viewer, loc, 0.40, rotation,        Particle.PORTAL,         null);
                smallOrbit (viewer, loc, 5,    0.45, rotation,  Particle.PORTAL,         null);
                smallOrbit (viewer, loc, 3,    0.30, -rotation, Particle.REVERSE_PORTAL, null);
            }
            // FIX: SMOKE hataya — REVERSE_PORTAL se replace
            case PHANTOM -> {
                centerHelix(viewer, loc, 0.35, rotation,         Particle.REVERSE_PORTAL, null);
                centerHelix(viewer, loc, 0.25, -rotation,        Particle.SQUID_INK,      null);
                smallOrbit (viewer, loc, 5,    0.45, -rotation,  Particle.REVERSE_PORTAL, null);
            }
            case STORM -> {
                centerHelix(viewer, loc, 0.40, rotation * 1.5,  Particle.ELECTRIC_SPARK, null);
                smallOrbit (viewer, loc, 5,    0.45, rotation,   Particle.ELECTRIC_SPARK, null);
            }
            case FROST -> {
                centerHelix(viewer, loc, 0.35, rotation,         Particle.SNOWFLAKE,  null);
                smallOrbit (viewer, loc, 5,    0.45, rotation,   Particle.WHITE_ASH,  null);
            }
            case FLAME -> {
                centerHelix(viewer, loc, 0.35, rotation,         Particle.FLAME,          null);
                centerHelix(viewer, loc, 0.25, rotation + Math.PI, Particle.SOUL_FIRE_FLAME, null);
                smallOrbit (viewer, loc, 5,    0.40, rotation,   Particle.FLAME,          null);
            }
            // FIX: SMOKE hataya — PORTAL se replace
            case SHADOW -> {
                Particle.DustOptions black = new Particle.DustOptions(Color.fromRGB(5,0,10), 1.0f);
                centerHelix(viewer, loc, 0.35, rotation,         Particle.PORTAL, null);
                smallOrbit (viewer, loc, 4,    0.40, -rotation,  Particle.DUST,   black);
            }
            case TITAN -> {
                Particle.DustOptions gold = new Particle.DustOptions(Color.fromRGB(210,150,20), 1.2f);
                centerHelix(viewer, loc, 0.40, rotation,         Particle.DUST, gold);
                smallOrbit (viewer, loc, 5,    0.45, rotation,   Particle.CRIT, null);
            }
            case HUNTER -> {
                centerHelix(viewer, loc, 0.35, rotation,         Particle.SOUL, null);
                smallOrbit (viewer, loc, 5,    0.40, rotation,   Particle.CRIT, null);
            }
            case GRAVITY -> {
                centerHelix(viewer, loc, 0.40, rotation,          Particle.REVERSE_PORTAL, null);
                smallOrbit (viewer, loc, 5,    0.45, -rotation,   Particle.REVERSE_PORTAL, null);
            }
            case WIND -> {
                centerHelix(viewer, loc, 0.35, rotation,          Particle.CLOUD,     null);
                smallOrbit (viewer, loc, 5,    0.45, rotation,    Particle.WHITE_ASH, null);
            }
            case POISON -> {
                Particle.DustOptions green = new Particle.DustOptions(Color.fromRGB(40,200,40), 1.0f);
                centerHelix(viewer, loc, 0.35, rotation,          Particle.SNEEZE, null);
                smallOrbit (viewer, loc, 5,    0.40, rotation,    Particle.DUST,   green);
            }
            case LIGHT -> {
                centerHelix(viewer, loc, 0.40, rotation,          Particle.END_ROD, null);
                smallOrbit (viewer, loc, 5,    0.45, rotation * 2, Particle.END_ROD, null);
            }
            case EARTH -> {
                Particle.DustOptions brown = new Particle.DustOptions(Color.fromRGB(130,80,30), 1.1f);
                centerHelix(viewer, loc, 0.40, rotation,          Particle.DUST,          brown);
                smallOrbit (viewer, loc, 5,    0.45, rotation,    Particle.ENCHANTED_HIT, null);
            }
            case CRYSTAL -> {
                Particle.DustOptions cyan = new Particle.DustOptions(Color.fromRGB(80,215,255), 1.2f);
                centerHelix(viewer, loc, 0.35, rotation,          Particle.DUST,    cyan);
                smallOrbit (viewer, loc, 5,    0.40, rotation,    Particle.END_ROD, null);
            }
            case ECHO -> {
                Particle.DustOptions teal = new Particle.DustOptions(Color.fromRGB(50,195,195), 1.0f);
                centerHelix(viewer, loc, 0.35, rotation,          Particle.DUST,    teal);
                smallOrbit (viewer, loc, 5,    0.45, rotation,    Particle.END_ROD, null);
            }
            // FIX: ANGRY_VILLAGER hataya — CRIT se replace, FLAME rakh
            case RAGE -> {
                Particle.DustOptions red = new Particle.DustOptions(Color.fromRGB(215,15,15), 1.2f);
                centerHelix(viewer, loc, 0.40, rotation,          Particle.DUST,  red);
                centerHelix(viewer, loc, 0.28, -rotation,         Particle.FLAME, null);
                smallOrbit (viewer, loc, 5,    0.45, rotation,    Particle.CRIT,  null);
            }
            case SPIRIT -> {
                Particle.DustOptions lime = new Particle.DustOptions(Color.fromRGB(160,255,160), 1.0f);
                centerHelix(viewer, loc, 0.35, rotation,          Particle.DUST,            lime);
                smallOrbit (viewer, loc, 4,    0.40, rotation,    Particle.HEART,            null);
            }
            case TIME -> {
                Particle.DustOptions purple = new Particle.DustOptions(Color.fromRGB(150,70,215), 1.1f);
                centerHelix(viewer, loc, 0.35, rotation,          Particle.NAUTILUS,       null);
                centerHelix(viewer, loc, 0.25, -rotation,         Particle.REVERSE_PORTAL, null);
                smallOrbit (viewer, loc, 5,    0.45, rotation,    Particle.DUST,           purple);
            }
            case WARRIOR -> {
                Particle.DustOptions orange = new Particle.DustOptions(Color.fromRGB(225,115,15), 1.1f);
                centerHelix(viewer, loc, 0.40, rotation,          Particle.DUST,         orange);
                smallOrbit (viewer, loc, 5,    0.45, rotation,    Particle.SWEEP_ATTACK, null);
            }

            // ── EVENT EYES ────────────────────────────────────────────────
            case METEOR -> {
                Particle.DustOptions lava = new Particle.DustOptions(Color.fromRGB(255,50,0), 1.3f);
                centerHelix(viewer, loc, 0.40, rotation,          Particle.FLAME, null);
                centerHelix(viewer, loc, 0.30, -rotation,         Particle.LAVA,  null);
                smallOrbit (viewer, loc, 6,    0.50, rotation,    Particle.DUST,  lava);
                if ((int)(rotation * 10) % 10 == 0)
                    safe(viewer, loc.clone().add(0, 2.0, 0), Particle.DRIPPING_LAVA, null, 1);
            }
            case MIRAGE -> {
                Particle.DustOptions silver = new Particle.DustOptions(Color.fromRGB(200,200,200), 1.0f);
                centerHelix(viewer, loc, 0.35, rotation,          Particle.WHITE_SMOKE, null);
                smallOrbit (viewer, loc, 6,    0.50, rotation,    Particle.DUST,        silver);
            }
            case OCEAN -> {
                Particle.DustOptions blue = new Particle.DustOptions(Color.fromRGB(15,110,215), 1.1f);
                centerHelix(viewer, loc, 0.40, rotation,          Particle.BUBBLE_POP, null);
                smallOrbit (viewer, loc, 6,    0.50, rotation,    Particle.DUST,       blue);
            }
            case ECLIPSE -> {
                Particle.DustOptions dark = new Particle.DustOptions(Color.fromRGB(75,0,115), 1.3f);
                centerHelix(viewer, loc, 0.40, rotation,          Particle.PORTAL,        null);
                centerHelix(viewer, loc, 0.30, -rotation,         Particle.DRAGON_BREATH, null);
                smallOrbit (viewer, loc, 6,    0.50, rotation,    Particle.DUST,          dark);
            }
            case GUARDIAN -> {
                Particle.DustOptions gold2 = new Particle.DustOptions(Color.fromRGB(255,215,0), 1.2f);
                centerHelix(viewer, loc, 0.40, rotation,          Particle.TOTEM_OF_UNDYING, null);
                smallOrbit (viewer, loc, 6,    0.50, rotation,    Particle.DUST,             gold2);
            }
            default -> {
                centerHelix(viewer, loc, 0.35, rotation, Particle.DUST, dust);
                smallOrbit (viewer, loc, 4,    0.40, rotation, Particle.DUST, dust);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  CENTER HELIX — player ke CENTER mein, legs (y=0) se head (y=2)
    //  FIX: peeche nahi — bilkul center mein shank ki tarah
    // ══════════════════════════════════════════════════════════════════════
    private void centerHelix(Player viewer, Location loc,
                             double radius, double angle,
                             Particle type, Particle.DustOptions dust) {
        for (int y = 0; y <= 8; y++) {
            double yFrac  = y / 8.0;
            double helixA = angle * 3.0 + yFrac * Math.PI * 3.0;
            // Radius thoda narrow hota hai upar jaate jaate
            double r      = radius * (1.0 - yFrac * 0.25);
            // CENTER — directly on player, koi offset nahi
            Location pt = loc.clone().add(
                    Math.cos(helixA) * r,
                    yFrac * 1.9,
                    Math.sin(helixA) * r);
            safe(viewer, pt, type, dust, 1);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  SMALL ORBIT — head ke upar (4-6 points)
    // ══════════════════════════════════════════════════════════════════════
    private void smallOrbit(Player viewer, Location loc,
                            int points, double radius, double angle,
                            Particle type, Particle.DustOptions dust) {
        for (int i = 0; i < points; i++) {
            double a    = angle + Math.toRadians(i * (360.0 / points));
            Location pt = loc.clone().add(Math.cos(a)*radius, 2.1, Math.sin(a)*radius);
            safe(viewer, pt, type, dust, 1);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  SAFE SPAWN
    // ══════════════════════════════════════════════════════════════════════
    private void safe(Player viewer, Location pt,
                      Particle type, Particle.DustOptions dust, int count) {
        try {
            if (dust != null && type == Particle.DUST) {
                viewer.spawnParticle(Particle.DUST, pt, count, 0.01, 0.01, 0.01, 0, dust);
            } else {
                viewer.spawnParticle(type, pt, count, 0.01, 0.01, 0.01, 0.01);
            }
        } catch (Exception ignored) {}
    }

    private boolean needsDust(EyeType eye) {
        return switch (eye) {
            case EARTH, CRYSTAL, ECHO -> true;
            default -> false;
        };
    }

    private Vector perpendicularLeft(Player p) {
        Vector dir = p.getLocation().getDirection().normalize();
        return new Vector(-dir.getZ(), 0, dir.getX()).normalize();
    }
}
