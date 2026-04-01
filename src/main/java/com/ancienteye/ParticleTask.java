package com.ancienteye;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class ParticleTask extends BukkitRunnable {

    private final AncientEyePlugin plugin;
    private double rotation    = 0;
    private int    tickCounter = 0;

    public ParticleTask(AncientEyePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (!plugin.getConfig().getBoolean("settings.particles-enabled", true)) return;

        int delaySeconds = plugin.getConfig().getInt("settings.particle-interval", 5);
        int targetTicks  = delaySeconds * 20;

        tickCounter += 5;
        if (tickCounter < targetTicks) return;
        tickCounter = 0;

        rotation += 0.6;
        if (rotation > Math.PI * 2) rotation = 0;

        for (Player owner : Bukkit.getOnlinePlayers()) {
            EyeType eye = plugin.getPlayerData().getEye(owner);
            if (eye == null || eye == EyeType.NONE) continue;

            for (Player viewer : owner.getWorld().getPlayers()) {
                if (viewer.getLocation().distanceSquared(owner.getLocation()) > 48 * 48) continue;
                spawnShankWave(viewer, owner, eye);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  SHANK WAVE — Legs se Head tak, har eye alag particle + color
    //  Har 5s ek baar — wave upar jaata hai
    // ══════════════════════════════════════════════════════════════════════
    private void spawnShankWave(Player viewer, Player owner, EyeType eye) {
        Location loc = owner.getLocation();

        switch (eye) {
            case VOID     -> wave(viewer, loc, Particle.PORTAL,         null,                               0xFF00FF, 0.45);
            case PHANTOM  -> wave(viewer, loc, Particle.REVERSE_PORTAL, null,                               0x8800AA, 0.40);
            case STORM    -> wave(viewer, loc, Particle.ELECTRIC_SPARK, null,                               0x00AAFF, 0.45);
            case FROST    -> wave(viewer, loc, Particle.SNOWFLAKE,      null,                               0xAAEEFF, 0.40);
            case FLAME    -> wave(viewer, loc, Particle.FLAME,          null,                               0xFF4400, 0.45);
            case SHADOW   -> wave(viewer, loc, Particle.SMOKE,          mk(5,0,10),                         0x220033, 0.40);
            case TITAN    -> wave(viewer, loc, Particle.DUST,           mk(210,150,20),                     0xD29614, 0.45);
            case HUNTER   -> wave(viewer, loc, Particle.SOUL,           null,                               0x00CC44, 0.40);
            case GRAVITY  -> wave(viewer, loc, Particle.REVERSE_PORTAL, null,                               0x6600FF, 0.40);
            case WIND     -> wave(viewer, loc, Particle.CLOUD,          null,                               0xCCFFFF, 0.40);
            case POISON   -> wave(viewer, loc, Particle.SNEEZE,         null,                               0x22CC22, 0.40);
            case LIGHT    -> wave(viewer, loc, Particle.END_ROD,        null,                               0xFFFFFF, 0.45);
            case EARTH    -> wave(viewer, loc, Particle.DUST,           mk(130,80,30),                      0x824E1E, 0.45);
            case CRYSTAL  -> wave(viewer, loc, Particle.DUST,           mk(80,215,255),                     0x50D7FF, 0.45);
            case ECHO     -> wave(viewer, loc, Particle.DUST,           mk(50,195,195),                     0x32C3C3, 0.40);
            case RAGE     -> wave(viewer, loc, Particle.FLAME,          null,                               0xCC0000, 0.45);
            case SPIRIT   -> wave(viewer, loc, Particle.TOTEM_OF_UNDYING, null,                             0xA0FFA0, 0.40);
            case TIME     -> wave(viewer, loc, Particle.NAUTILUS,       null,                               0x9646D7, 0.40);
            case WARRIOR  -> wave(viewer, loc, Particle.DUST,           mk(225,115,15),                     0xE1730F, 0.45);
            case METEOR   -> wave(viewer, loc, Particle.FLAME,          null,                               0xFF3200, 0.45);
            case MIRAGE   -> wave(viewer, loc, Particle.WHITE_SMOKE,    null,                               0xC8C8C8, 0.40);
            case OCEAN    -> wave(viewer, loc, Particle.BUBBLE_POP,     null,                               0x0F6ED7, 0.45);
            case ECLIPSE  -> wave(viewer, loc, Particle.PORTAL,         null,                               0x4B0073, 0.45);
            case GUARDIAN -> wave(viewer, loc, Particle.TOTEM_OF_UNDYING, null,                             0xFFD700, 0.45);
            default -> {}
        }
    }

    // ── Wave: legs (y=0) se head (y=2) tak shank shape ───────────────────
    // 20 points — smooth helix upar jaata hai
    private void wave(Player viewer, Location loc, Particle type,
                      Particle.DustOptions dust, int hexColor, double radius) {
        // Dust particle ke liye color use karo
        Particle.DustOptions d = dust;
        if (d == null && type == Particle.DUST) {
            d = new Particle.DustOptions(
                Color.fromRGB((hexColor>>16)&0xFF,(hexColor>>8)&0xFF,hexColor&0xFF), 1.8f);
        }
        // Agar dust type nahi — bhi ek colored DUST trail saath mein
        Particle.DustOptions trail = new Particle.DustOptions(
            Color.fromRGB((hexColor>>16)&0xFF,(hexColor>>8)&0xFF,hexColor&0xFF), 1.6f);

        for (int y = 0; y <= 20; y++) {
            double yFrac  = y / 20.0;
            double helixA = rotation + (yFrac * Math.PI * 3.5); // shank spiral
            double r      = radius * (1.0 - yFrac * 0.15);      // thoda narrow top

            Location pt = loc.clone().add(
                Math.cos(helixA) * r,
                yFrac * 2.0,                                      // 0 = leg, 2 = head
                Math.sin(helixA) * r);

            // Main particle
            safe(viewer, pt, type, d, 1);

            // Colored DUST trail — hamesha visible (opacity 1.8f)
            if (y % 2 == 0)
                safe(viewer, pt, Particle.DUST, trail, 1);
        }
    }

    // Helper: DustOptions banana
    private Particle.DustOptions mk(int r, int g, int b) {
        return new Particle.DustOptions(Color.fromRGB(r,g,b), 1.8f);
    }

    private void safe(Player viewer, Location pt, Particle type,
                      Particle.DustOptions dust, int count) {
        try {
            if (dust != null && type == Particle.DUST) {
                viewer.spawnParticle(Particle.DUST, pt, count, 0.0, 0.0, 0.0, 0, dust);
            } else {
                viewer.spawnParticle(type, pt, count, 0.0, 0.0, 0.0, 0.0);
            }
        } catch (Exception ignored) {}
    }
}
