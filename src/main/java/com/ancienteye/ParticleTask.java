package com.ancienteye;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class ParticleTask extends BukkitRunnable {

    private final AncientEyePlugin plugin;
    private double rotation = 0;

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

            // ── Dust options — only used for DUST particle type ───────────────
            // Kuch eyes ke particle DUST nahi hai (PORTAL, FLAME etc.)
            // Un ke liye DustOptions use nahi hogi
            Particle.DustOptions dust = null;
            if (eye.color != null && needsDust(eye)) {
                dust = new Particle.DustOptions(eye.color, 1.0f);
            }

            // Slightly in front — first-person mein nahi dikhega
            Location eyeLoc = owner.getEyeLocation()
                    .add(owner.getLocation().getDirection().multiply(0.55));

            Location leftSpark  = eyeLoc.clone().add(
                    perpendicularLeft(owner).multiply(0.09));
            Location rightSpark = eyeLoc.clone().add(
                    perpendicularLeft(owner).multiply(-0.09));

            // Only other nearby players ko dikhao — owner ko nahi
            for (Player viewer : owner.getWorld().getPlayers()) {
                if (viewer.getUniqueId().equals(owner.getUniqueId())) continue;
                if (viewer.getLocation().distanceSquared(owner.getLocation()) > 64 * 64) continue;

                // ── Eye sparks (left + right) ─────────────────────────────────
                spawnFor(viewer, leftSpark,  eye, dust, 1);
                spawnFor(viewer, rightSpark, eye, dust, 1);

                // ── Event eye extras — aura + orbit rings ─────────────────────
                if (eye.isEventEye()) {

                    // Body aura (waist → shoulder)
                    Location waist    = owner.getLocation().add(0, 0.9, 0);
                    Location torso    = owner.getLocation().add(0, 1.4, 0);
                    Location shoulder = owner.getLocation().add(0, 1.8, 0);
                    spawnFor(viewer, waist,    eye, dust, 3);
                    spawnFor(viewer, torso,    eye, dust, 3);
                    spawnFor(viewer, shoulder, eye, dust, 2);

                    // Orbit ring 1 — XZ plane
                    double r = 1.1;
                    int points = 8;
                    for (int i = 0; i < points; i++) {
                        double angle = rotation + (Math.PI * 2 / points) * i;
                        Location rLoc = owner.getLocation().add(
                                Math.cos(angle) * r, 1.0, Math.sin(angle) * r);
                        spawnFor(viewer, rLoc, eye, dust, 1);
                    }

                    // Orbit ring 2 — tilted
                    for (int i = 0; i < points; i++) {
                        double angle = -rotation + (Math.PI * 2 / points) * i;
                        double rx = Math.cos(angle) * r;
                        double ry = Math.sin(angle) * r * 0.55;
                        double rz = Math.sin(angle) * r;
                        Location rLoc = owner.getLocation().add(rx, 1.1 + ry, rz);
                        spawnFor(viewer, rLoc, eye, dust, 1);
                    }
                }
            }
        }
    }

    // ── Spawn particle for one viewer ────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private void spawnFor(Player viewer, Location loc,
                          EyeType eye, Particle.DustOptions dust, int count) {
        try {
            if (dust != null && needsDust(eye)) {
                // DUST particle — needs DustOptions as extra data
                viewer.spawnParticle(Particle.DUST, loc, count,
                        0.01, 0.01, 0.01, 0, dust);
            } else {
                // Non-dust particle — use EyeType's own particle directly
                viewer.spawnParticle(eye.particle, loc, count,
                        0.01, 0.01, 0.01, 0.02);
            }
        } catch (Exception ignored) {
            // Kuch particles specific worlds mein crash kar sakte hain — silently skip
        }
    }

    /**
     * Returns true if this eye's visual should use DUST particle
     * (colored dust = DustOptions needed).
     *
     * EyeType mein jo particles hain:
     *   DUST-based:   none directly — lekin hum color dikhane ke liye
     *                 DUST use karte hain jab eye.color != null
     *                 aur original particle DUST-compatible ho
     *
     * Non-dust particles (use as-is):
     *   PORTAL, SQUID_INK, ELECTRIC_SPARK, SNOWFLAKE, FLAME, SMOKE,
     *   CAMPFIRE_COSY_SMOKE, SOUL, REVERSE_PORTAL, CLOUD, SNEEZE,
     *   GLOW, ENCHANTED_HIT, INSTANT_EFFECT, SONIC_BOOM, ANGRY_VILLAGER,
     *   WITCH, NAUTILUS, SWEEP_ATTACK, LAVA, WHITE_SMOKE, DRIPPING_WATER,
     *   DRAGON_BREATH, TOTEM_OF_UNDYING
     *
     * We use DUST only for eyes whose particle is not already colored.
     */
    private boolean needsDust(EyeType eye) {
        return switch (eye) {
            // These particles have no inherent color — show colored DUST instead
            case EARTH, CRYSTAL, ECHO -> true;
            // All others use their own defined particle (already has color/shape)
            default -> false;
        };
    }

    // ── Left perpendicular vector (horizontal plane) ─────────────────────────
    private org.bukkit.util.Vector perpendicularLeft(Player p) {
        org.bukkit.util.Vector dir = p.getLocation().getDirection().normalize();
        return new org.bukkit.util.Vector(-dir.getZ(), 0, dir.getX()).normalize();
    }
}
