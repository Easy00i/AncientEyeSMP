package com.ancienteye;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class ParticleTask extends BukkitRunnable {

    private final AncientEyePlugin plugin;
    private double rotation = 0;
    
    // Timer check karne ke liye variable
    private int tickCounter = 0;

    public ParticleTask(AncientEyePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        // Config se seconds nikalo (default 5s) aur usko ticks mein badlo (1s = 20 ticks)
        int delaySeconds = plugin.getConfig().getInt("settings.particle-interval", 5);
        int targetTicks = delaySeconds * 20;

        tickCounter += 5; // Kyunki ye task har 5 ticks mein chalta hai
        
        // Jab tak target time nahi hota, aage mat badho
        if (tickCounter < targetTicks) {
            return;
        }

        // Time ho gaya, ab particles spawn karo aur counter reset karo
        tickCounter = 0;
        
        // Shankh ke liye rotation set karo
        rotation += 0.5;
        if (rotation > Math.PI * 2) rotation = 0;

        for (Player owner : Bukkit.getOnlinePlayers()) {
            EyeType eye = plugin.getPlayerData().getEye(owner);
            if (eye == null || eye == EyeType.NONE) continue;

            if (!plugin.getConfig().getBoolean("settings.particles-enabled", true)) continue;

            // Opacity thodi kam rakhi hai (0.7f)
            Particle.DustOptions dust = (eye.color != null)
                    ? new Particle.DustOptions(eye.color, 0.7f) : null;

            for (Player viewer : owner.getWorld().getPlayers()) {
                if (viewer.getLocation().distanceSquared(owner.getLocation()) > 48 * 48) continue;
                spawnAuraWave(viewer, owner, eye, dust);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  AURA WAVE — Sirf Legs se Head tak, koi head orbit nahi
    // ══════════════════════════════════════════════════════════════════════
    private void spawnAuraWave(Player viewer, Player owner, EyeType eye, Particle.DustOptions dust) {
        Location loc = owner.getLocation();

        switch (eye) {
            case VOID -> {
                centerHelix(viewer, loc, 0.45, rotation, Particle.PORTAL, null);
                centerHelix(viewer, loc, 0.35, -rotation, Particle.REVERSE_PORTAL, null);
            }
            case PHANTOM -> {
                centerHelix(viewer, loc, 0.40, rotation, Particle.REVERSE_PORTAL, null);
                centerHelix(viewer, loc, 0.25, -rotation, Particle.SQUID_INK, null);
            }
            case STORM -> {
                centerHelix(viewer, loc, 0.40, rotation, Particle.ELECTRIC_SPARK, null);
            }
            case FROST -> {
                centerHelix(viewer, loc, 0.40, rotation, Particle.SNOWFLAKE, null);
            }
            case FLAME -> {
                centerHelix(viewer, loc, 0.40, rotation, Particle.FLAME, null);
                centerHelix(viewer, loc, 0.30, -rotation, Particle.SOUL_FIRE_FLAME, null);
            }
            case SHADOW -> {
                Particle.DustOptions black = new Particle.DustOptions(Color.fromRGB(5,0,10), 0.8f);
                centerHelix(viewer, loc, 0.40, rotation, Particle.PORTAL, null);
                centerHelix(viewer, loc, 0.30, -rotation, Particle.DUST, black);
            }
            case TITAN -> {
                Particle.DustOptions gold = new Particle.DustOptions(Color.fromRGB(210,150,20), 0.9f);
                centerHelix(viewer, loc, 0.45, rotation, Particle.DUST, gold);
            }
            case HUNTER -> {
                centerHelix(viewer, loc, 0.40, rotation, Particle.SOUL, null);
            }
            case GRAVITY -> {
                centerHelix(viewer, loc, 0.40, rotation, Particle.REVERSE_PORTAL, null);
            }
            case WIND -> {
                centerHelix(viewer, loc, 0.40, rotation, Particle.CLOUD, null);
            }
            case POISON -> {
                Particle.DustOptions green = new Particle.DustOptions(Color.fromRGB(40,200,40), 0.8f);
                centerHelix(viewer, loc, 0.40, rotation, Particle.SNEEZE, null);
                centerHelix(viewer, loc, 0.30, -rotation, Particle.DUST, green);
            }
            case LIGHT -> {
                centerHelix(viewer, loc, 0.45, rotation, Particle.END_ROD, null);
            }
            case EARTH -> {
                Particle.DustOptions brown = new Particle.DustOptions(Color.fromRGB(130,80,30), 0.9f);
                centerHelix(viewer, loc, 0.45, rotation, Particle.DUST, brown);
            }
            case CRYSTAL -> {
                Particle.DustOptions cyan = new Particle.DustOptions(Color.fromRGB(80,215,255), 0.9f);
                centerHelix(viewer, loc, 0.40, rotation, Particle.DUST, cyan);
            }
            case ECHO -> {
                Particle.DustOptions teal = new Particle.DustOptions(Color.fromRGB(50,195,195), 0.8f);
                centerHelix(viewer, loc, 0.40, rotation, Particle.DUST, teal);
            }
            case RAGE -> {
                Particle.DustOptions red = new Particle.DustOptions(Color.fromRGB(215,15,15), 0.9f);
                centerHelix(viewer, loc, 0.45, rotation, Particle.DUST, red);
                centerHelix(viewer, loc, 0.30, -rotation, Particle.FLAME, null);
            }
            case SPIRIT -> {
                Particle.DustOptions lime = new Particle.DustOptions(Color.fromRGB(160,255,160), 0.8f);
                centerHelix(viewer, loc, 0.40, rotation, Particle.DUST, lime);
            }
            case TIME -> {
                Particle.DustOptions purple = new Particle.DustOptions(Color.fromRGB(150,70,215), 0.9f);
                centerHelix(viewer, loc, 0.40, rotation, Particle.NAUTILUS, null);
                centerHelix(viewer, loc, 0.30, -rotation, Particle.DUST, purple);
            }
            case WARRIOR -> {
                Particle.DustOptions orange = new Particle.DustOptions(Color.fromRGB(225,115,15), 0.9f);
                centerHelix(viewer, loc, 0.45, rotation, Particle.DUST, orange);
            }

            // ── EVENT EYES ────────────────────────────────────────────────
            case METEOR -> {
                centerHelix(viewer, loc, 0.45, rotation, Particle.FLAME, null);
                centerHelix(viewer, loc, 0.35, -rotation, Particle.LAVA, null);
            }
            case MIRAGE -> {
                Particle.DustOptions silver = new Particle.DustOptions(Color.fromRGB(200,200,200), 0.8f);
                centerHelix(viewer, loc, 0.40, rotation, Particle.WHITE_SMOKE, null);
                centerHelix(viewer, loc, 0.30, -rotation, Particle.DUST, silver);
            }
            case OCEAN -> {
                Particle.DustOptions blue = new Particle.DustOptions(Color.fromRGB(15,110,215), 0.9f);
                centerHelix(viewer, loc, 0.45, rotation, Particle.BUBBLE_POP, null);
                centerHelix(viewer, loc, 0.35, -rotation, Particle.DUST, blue);
            }
            case ECLIPSE -> {
                Particle.DustOptions dark = new Particle.DustOptions(Color.fromRGB(75,0,115), 0.9f);
                centerHelix(viewer, loc, 0.45, rotation, Particle.PORTAL, null);
                centerHelix(viewer, loc, 0.35, -rotation, Particle.DUST, dark);
            }
            case GUARDIAN -> {
                Particle.DustOptions gold2 = new Particle.DustOptions(Color.fromRGB(255,215,0), 0.9f);
                centerHelix(viewer, loc, 0.45, rotation, Particle.TOTEM_OF_UNDYING, null);
                centerHelix(viewer, loc, 0.35, -rotation, Particle.DUST, gold2);
            }
            default -> {
                centerHelix(viewer, loc, 0.40, rotation, Particle.DUST, dust);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  CENTER HELIX — Fast Wave (Legs se Head tak)
    // ══════════════════════════════════════════════════════════════════════
    private void centerHelix(Player viewer, Location loc, double radius, double angle, Particle type, Particle.DustOptions dust) {
        // Sirf 12 points banayenge taaki particles kam hon aur shape smooth rahe
        for (int y = 0; y <= 12; y++) {
            double yFrac = y / 12.0;
            // Helix shape calculation
            double helixA = angle + (yFrac * Math.PI * 4.0); // 2 chakkar lagayega
            double r = radius * (1.0 - yFrac * 0.1); // Upar jaate thoda patla hoga

            Location pt = loc.clone().add(
                    Math.cos(helixA) * r,
                    yFrac * 2.0, // Maximum height 2.0 (Head tak)
                    Math.sin(helixA) * r
            );
            safe(viewer, pt, type, dust, 1);
        }
    }

    private void safe(Player viewer, Location pt, Particle type, Particle.DustOptions dust, int count) {
        try {
            if (dust != null && type == Particle.DUST) {
                viewer.spawnParticle(Particle.DUST, pt, count, 0.0, 0.0, 0.0, 0, dust);
            } else {
                viewer.spawnParticle(type, pt, count, 0.0, 0.0, 0.0, 0.0);
            }
        } catch (Exception ignored) {}
    }
}
