package com.ancienteye;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ParticleTask extends BukkitRunnable {
    private final AncientEyePlugin plugin;
    private double rotation = 0; // Orbit ghumane ke liye variable

    public ParticleTask(AncientEyePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        rotation += 0.2; // Orbit ki speed
        if (rotation > Math.PI * 2) rotation = 0;

        for (Player p : Bukkit.getOnlinePlayers()) {
            EyeType eye = plugin.getPlayerData().getEye(p);
            if (eye == null || eye == EyeType.NONE || eye.color == null) continue;

            // DustOptions taaki hum Enum wala Color use kar sakein
            Particle.DustOptions dust = new Particle.DustOptions(eye.color, 1.0f);

            // --- 1. NORMAL EYE LOGIC (Sirf Aankhon par) ---
            p.getWorld().spawnParticle(
                Particle.DUST, 
                p.getEyeLocation().add(p.getLocation().getDirection().multiply(0.2)), 
                2, 0.1, 0.1, 0.1, dust
            );

            // --- 2. RARE/EVENT EYE LOGIC (Full Body + Orbit) ---
            if (eye.isEventEye()) {
                // A. Body Aura (Halka sa dhuan puri body par)
                p.getWorld().spawnParticle(Particle.DUST, p.getLocation().add(0, 1, 0), 5, 0.4, 0.8, 0.4, dust);

                // B. Rotating Orbit (Player ke charo taraf ghumne wala ring)
                double radius = 1.2;
                double x = Math.cos(rotation) * radius;
                double z = Math.sin(rotation) * radius;
                
                // Orbit Location (Player ke waist level par)
                p.getWorld().spawnParticle(
                    Particle.DUST, 
                    p.getLocation().add(x, 1, z), 
                    1, 0, 0, 0, dust
                );

                // Ek aur particle opposite side par (Double Orbit look)
                p.getWorld().spawnParticle(
                    Particle.DUST, 
                    p.getLocation().add(-x, 1.5, -z), 
                    1, 0, 0, 0, dust
                );
            }
        }
    }
}

