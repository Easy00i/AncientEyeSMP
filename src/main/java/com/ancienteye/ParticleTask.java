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
            if (eye == null || eye == EyeType.NONE || eye.color == null) continue;

            Particle.DustOptions dust = new Particle.DustOptions(eye.color, 1.0f);

            // ── Exact eye position ────────────────────────────────────────────
            // Slightly in FRONT of the eye along look direction so it stays
            // outside the camera frustum — not visible in first-person.
            Location eyeLoc = owner.getEyeLocation()
                    .add(owner.getLocation().getDirection().multiply(0.55));

            // Left / right offsets so two small sparks flank each eye socket
            Location leftSpark  = eyeLoc.clone().add(
                    perpendicularLeft(owner).multiply(0.09));
            Location rightSpark = eyeLoc.clone().add(
                    perpendicularLeft(owner).multiply(-0.09));

            // ── Spawn only for OTHER nearby players (skip first-person owner) ─
            for (Player viewer : owner.getWorld().getPlayers()) {
                // Never send to the owner — they'd see it on their own screen
                if (viewer.getUniqueId().equals(owner.getUniqueId())) continue;
                // Skip viewers too far away (>64 blocks) for performance
                if (viewer.getLocation().distanceSquared(owner.getLocation()) > 64 * 64) continue;

                // ── Eye sparks ────────────────────────────────────────────────
                spawnFor(viewer, leftSpark,  dust, Particle.DUST, 1);
                spawnFor(viewer, rightSpark, dust, Particle.DUST, 1);

                // ── Rare / Event eye extras ───────────────────────────────────
                if (eye.isEventEye()) {

                    // A. Soft body aura  (waist → shoulder band, low spread)
                    Location waist   = owner.getLocation().add(0, 0.9, 0);
                    Location torso   = owner.getLocation().add(0, 1.4, 0);
                    Location shoulder= owner.getLocation().add(0, 1.8, 0);

                    spawnFor(viewer, waist,    dust, Particle.DUST, 3);
                    spawnFor(viewer, torso,    dust, Particle.DUST, 3);
                    spawnFor(viewer, shoulder, dust, Particle.DUST, 2);

                    // B. Orbit ring — single ring in XZ plane at waist level
                    double r = 1.1;
                    int ringPoints = 8;
                    for (int i = 0; i < ringPoints; i++) {
                        double angle = rotation + (Math.PI * 2 / ringPoints) * i;
                        Location rLoc = owner.getLocation().add(
                                Math.cos(angle) * r,
                                1.0,
                                Math.sin(angle) * r);
                        spawnFor(viewer, rLoc, dust, Particle.DUST, 1);
                    }

                    // C. Second tilted orbit ring (adds depth — 45° tilt on X)
                    for (int i = 0; i < ringPoints; i++) {
                        double angle = -rotation + (Math.PI * 2 / ringPoints) * i;
                        double rx = Math.cos(angle) * r;
                        double ry = Math.sin(angle) * r * 0.55; // flatten → tilt
                        double rz = Math.sin(angle) * r;
                        Location rLoc = owner.getLocation().add(rx, 1.1 + ry, rz);
                        spawnFor(viewer, rLoc, dust, Particle.DUST, 1);
                    }
                }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Spawn a DustOptions particle for ONE specific viewer only. */
    private void spawnFor(Player viewer, Location loc,
                          Particle.DustOptions dust, Particle type, int count) {
        viewer.spawnParticle(type, loc, count, 0.01, 0.01, 0.01, dust);
    }

    /**
     * Returns a unit vector perpendicular to the player's look direction,
     * lying in the horizontal plane — used to offset left/right eye sparks.
     */
    private org.bukkit.util.Vector perpendicularLeft(Player p) {
        org.bukkit.util.Vector dir = p.getLocation().getDirection().normalize();
        // Cross product of look-dir and world-up gives left vector
        return new org.bukkit.util.Vector(-dir.getZ(), 0, dir.getX()).normalize();
    }
}
