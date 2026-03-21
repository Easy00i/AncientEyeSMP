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

            Particle.DustOptions dust = (eye.color != null)
                    ? new Particle.DustOptions(eye.color, 1.0f) : null;

            for (Player viewer : owner.getWorld().getPlayers()) {
                if (viewer.getLocation().distanceSquared(owner.getLocation()) > 64 * 64) continue;

                boolean isOwner = viewer.getUniqueId().equals(owner.getUniqueId());

                if (isOwner) {
                    // ── OWNER: sirf peeche spawn — first-person safe ──────────
                    spawnOwnerAura(owner, eye, dust);
                } else {
                    // ── VIEWER: full body helix + head orbit ─────────────────
                    spawnFullAura(viewer, owner, eye, dust);
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  OWNER AURA — particles player ke PEECHE spawn hote hain
    //  Camera forward direction ke opposite → first-person mein NAHI dikha
    // ══════════════════════════════════════════════════════════════════════
    private void spawnOwnerAura(Player owner, EyeType eye, Particle.DustOptions dust) {
        Location loc   = owner.getLocation();
        Vector   dir   = loc.getDirection().normalize();
        Vector   back  = dir.clone().multiply(-1);   // peeche
        Vector   left  = new Vector(-dir.getZ(), 0, dir.getX()).normalize();

        // Body helix — peeche ki taraf, 8 points legs to head
        for (int y = 0; y <= 8; y++) {
            double yFrac  = y / 8.0;
            double helixA = rotation * 3.0 + yFrac * Math.PI * 3;

            // Back offset + side swing — kamera se door
            Vector offset = back.clone().multiply(0.35 + yFrac * 0.1)
                    .add(left.clone().multiply(Math.cos(helixA) * 0.3))
                    .add(new Vector(0, yFrac * 1.9, 0));

            spawnOwnerPt(owner, loc.clone().add(offset), eye, dust);
        }

        // Head orbit — sirf back half
        for (int i = 0; i < 4; i++) {
            double a = rotation * 2.0 + Math.toRadians(i * 90);
            // Sirf cos(a) < 0 side — matlab peeche wale points
            Vector offset = back.clone().multiply(0.3 + Math.abs(Math.sin(a)) * 0.25)
                    .add(left.clone().multiply(Math.cos(a) * 0.35))
                    .add(new Vector(0, 2.0, 0));
            spawnOwnerPt(owner, loc.clone().add(offset), eye, dust);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  FULL AURA — dusre viewers ke liye — har eye ka alag style
    //  Body: legs se head tak helix (shank/spiral)
    //  Head: upar orbit ring
    // ══════════════════════════════════════════════════════════════════════
    private void spawnFullAura(Player viewer, Player owner,
                               EyeType eye, Particle.DustOptions dust) {
        Location loc = owner.getLocation();

        switch (eye) {

            case VOID -> {
                bodyHelix(viewer, loc, 3, 0.50, rotation,          Particle.PORTAL,         null, 1);
                headOrbit(viewer, loc, 8, 0.55, rotation * 1.5,    Particle.PORTAL,         null, 1);
                headOrbit(viewer, loc, 4, 0.35, -rotation,         Particle.REVERSE_PORTAL, null, 1);
            }
            case PHANTOM -> {
                bodyHelix(viewer, loc, 2, 0.45, rotation,           Particle.SMOKE,          null, 1);
                bodyHelix(viewer, loc, 2, 0.30, -rotation,          Particle.SQUID_INK,      null, 1);
                headOrbit(viewer, loc, 6, 0.55, -rotation * 1.5,   Particle.REVERSE_PORTAL, null, 1);
            }
            case STORM -> {
                bodyHelix(viewer, loc, 2, 0.45, rotation * 1.5,    Particle.ELECTRIC_SPARK, null, 1);
                headOrbit(viewer, loc, 8, 0.60, rotation * 2,      Particle.ELECTRIC_SPARK, null, 1);
                if ((int)(rotation * 10) % 4 == 0)
                    spawnAt(viewer, loc.clone().add(0, 2.1, 0), Particle.ELECTRIC_SPARK, null, 3);
            }
            case FROST -> {
                bodyHelix(viewer, loc, 3, 0.40, rotation,           Particle.SNOWFLAKE,  null, 1);
                headOrbit(viewer, loc, 8, 0.55, rotation,           Particle.WHITE_ASH,  null, 1);
                headOrbit(viewer, loc, 4, 0.35, -rotation,          Particle.SNOWFLAKE,  null, 1);
            }
            case FLAME -> {
                bodyHelix(viewer, loc, 2, 0.40, rotation,            Particle.FLAME,          null, 1);
                bodyHelix(viewer, loc, 2, 0.30, rotation + Math.PI,  Particle.SOUL_FIRE_FLAME, null, 1);
                headOrbit(viewer, loc, 6, 0.55, rotation * 1.8,     Particle.FLAME,          null, 1);
            }
            case SHADOW -> {
                Particle.DustOptions black = new Particle.DustOptions(Color.fromRGB(5,0,10), 1.3f);
                bodyHelix(viewer, loc, 3, 0.50, rotation,            Particle.SMOKE, null,  1);
                headOrbit(viewer, loc, 8, 0.60, -rotation * 2,      Particle.DUST,  black, 1);
                headOrbit(viewer, loc, 4, 0.35, rotation,            Particle.PORTAL, null, 1);
            }
            case TITAN -> {
                Particle.DustOptions gold = new Particle.DustOptions(Color.fromRGB(210,150,20), 1.5f);
                bodyHelix(viewer, loc, 3, 0.55, rotation,            Particle.DUST, gold, 1);
                headOrbit(viewer, loc, 8, 0.65, rotation,            Particle.CRIT, null, 1);
                headOrbit(viewer, loc, 4, 0.45, -rotation,           Particle.DUST, gold, 1);
            }
            case HUNTER -> {
                bodyHelix(viewer, loc, 2, 0.40, rotation,            Particle.SOUL,  null, 1);
                headOrbit(viewer, loc, 8, 0.55, rotation * 2,       Particle.CRIT,  null, 1);
                headOrbit(viewer, loc, 4, 0.30, -rotation,           Particle.SOUL,  null, 1);
            }
            case GRAVITY -> {
                bodyHelix(viewer, loc, 3, 0.50, rotation,            Particle.REVERSE_PORTAL, null, 1);
                bodyHelix(viewer, loc, 3, 0.35, -rotation,           Particle.PORTAL,         null, 1);
                headOrbit(viewer, loc, 8, 0.60, rotation,            Particle.REVERSE_PORTAL, null, 1);
            }
            case WIND -> {
                bodyHelix(viewer, loc, 2, 0.45, rotation,            Particle.CLOUD,     null, 1);
                bodyHelix(viewer, loc, 2, 0.30, rotation + Math.PI,  Particle.WHITE_ASH, null, 1);
                headOrbit(viewer, loc, 10, 0.65, rotation * 1.5,    Particle.WHITE_ASH, null, 1);
            }
            case POISON -> {
                Particle.DustOptions green = new Particle.DustOptions(Color.fromRGB(40,200,40), 1.2f);
                bodyHelix(viewer, loc, 2, 0.45, rotation,            Particle.SNEEZE,    null,  1);
                headOrbit(viewer, loc, 8, 0.55, rotation,            Particle.DUST,      green, 1);
                headOrbit(viewer, loc, 4, 0.35, -rotation,           Particle.ITEM_SLIME, null, 1);
            }
            case LIGHT -> {
                bodyHelix(viewer, loc, 3, 0.45, rotation,            Particle.END_ROD, null, 1);
                headOrbit(viewer, loc, 8, 0.60, rotation * 2,       Particle.END_ROD, null, 1);
                if ((int)(rotation * 10) % 6 == 0)
                    spawnAt(viewer, loc.clone().add(0, 1.0, 0), Particle.FLASH, null, 1);
            }
            case EARTH -> {
                Particle.DustOptions brown = new Particle.DustOptions(Color.fromRGB(130,80,30), 1.3f);
                bodyHelix(viewer, loc, 3, 0.50, rotation,            Particle.DUST,         brown, 1);
                headOrbit(viewer, loc, 8, 0.60, rotation,            Particle.ENCHANTED_HIT, null, 1);
            }
            case CRYSTAL -> {
                Particle.DustOptions cyan = new Particle.DustOptions(Color.fromRGB(80,215,255), 1.4f);
                bodyHelix(viewer, loc, 3, 0.45, rotation,            Particle.DUST,          cyan, 1);
                bodyHelix(viewer, loc, 3, 0.30, -rotation,           Particle.INSTANT_EFFECT, null, 1);
                headOrbit(viewer, loc, 8, 0.60, rotation,            Particle.END_ROD,        null, 1);
                headOrbit(viewer, loc, 4, 0.40, -rotation,           Particle.DUST,           cyan, 1);
            }
            case ECHO -> {
                Particle.DustOptions teal = new Particle.DustOptions(Color.fromRGB(50,195,195), 1.2f);
                bodyHelix(viewer, loc, 2, 0.45, rotation,            Particle.DUST,   teal, 1);
                headOrbit(viewer, loc, 8, 0.60, rotation * 1.5,     Particle.END_ROD, null, 1);
                if ((int)(rotation * 10) % 8 == 0)
                    spawnAt(viewer, loc.clone().add(0, 1.0, 0), Particle.SONIC_BOOM, null, 1);
            }
            case RAGE -> {
                Particle.DustOptions red = new Particle.DustOptions(Color.fromRGB(215,15,15), 1.4f);
                bodyHelix(viewer, loc, 3, 0.50, rotation,            Particle.DUST,  red,  1);
                bodyHelix(viewer, loc, 3, 0.35, -rotation,           Particle.FLAME, null, 1);
                headOrbit(viewer, loc, 8, 0.65, rotation * 2,       Particle.CRIT,  null, 1);
                headOrbit(viewer, loc, 4, 0.45, -rotation,           Particle.DUST,  red,  1);
            }
            case SPIRIT -> {
                Particle.DustOptions lime = new Particle.DustOptions(Color.fromRGB(160,255,160), 1.2f);
                bodyHelix(viewer, loc, 2, 0.45, rotation,            Particle.DUST,            lime, 1);
                bodyHelix(viewer, loc, 2, 0.30, rotation + Math.PI,  Particle.TOTEM_OF_UNDYING, null, 1);
                headOrbit(viewer, loc, 6, 0.55, rotation,            Particle.HEART,            null, 1);
                headOrbit(viewer, loc, 4, 0.40, -rotation,           Particle.END_ROD,          null, 1);
            }
            case TIME -> {
                Particle.DustOptions purple = new Particle.DustOptions(Color.fromRGB(150,70,215), 1.3f);
                bodyHelix(viewer, loc, 2, 0.45, rotation,            Particle.NAUTILUS,       null,   1);
                bodyHelix(viewer, loc, 2, 0.35, -rotation,           Particle.REVERSE_PORTAL, null,   1);
                headOrbit(viewer, loc, 8, 0.60, rotation * 2,       Particle.DUST,           purple, 1);
                headOrbit(viewer, loc, 4, 0.40, -rotation,           Particle.NAUTILUS,       null,   1);
            }
            case WARRIOR -> {
                Particle.DustOptions orange = new Particle.DustOptions(Color.fromRGB(225,115,15), 1.3f);
                bodyHelix(viewer, loc, 3, 0.50, rotation,            Particle.DUST,         orange, 1);
                headOrbit(viewer, loc, 6, 0.60, rotation * 1.5,     Particle.SWEEP_ATTACK, null,   1);
                headOrbit(viewer, loc, 4, 0.40, -rotation,           Particle.CRIT,         null,   1);
            }

            // ── EVENT EYES ─────────────────────────────────────────────────────
            case METEOR -> {
                Particle.DustOptions lava = new Particle.DustOptions(Color.fromRGB(255,50,0), 1.5f);
                bodyHelix(viewer, loc, 3, 0.50, rotation,            Particle.FLAME, null, 1);
                bodyHelix(viewer, loc, 3, 0.40, -rotation,           Particle.LAVA,  null, 1);
                headOrbit(viewer, loc, 8, 0.70, rotation * 2,       Particle.DUST,  lava, 1);
                headOrbit(viewer, loc, 4, 0.50, -rotation,           Particle.FLAME, null, 1);
                if ((int)(rotation * 10) % 5 == 0)
                    spawnAt(viewer, loc.clone().add(0, 2.2, 0), Particle.DRIPPING_LAVA, null, 2);
            }
            case MIRAGE -> {
                Particle.DustOptions silver = new Particle.DustOptions(Color.fromRGB(200,200,200), 1.2f);
                bodyHelix(viewer, loc, 3, 0.50, rotation,            Particle.WHITE_SMOKE, null,   1);
                bodyHelix(viewer, loc, 2, 0.35, -rotation,           Particle.CLOUD,       null,   1);
                headOrbit(viewer, loc, 10, 0.65, rotation,           Particle.DUST,        silver, 1);
                headOrbit(viewer, loc,  5, 0.45, -rotation * 1.5,   Particle.END_ROD,     null,   1);
            }
            case OCEAN -> {
                Particle.DustOptions blue = new Particle.DustOptions(Color.fromRGB(15,110,215), 1.3f);
                bodyHelix(viewer, loc, 3, 0.50, rotation,            Particle.BUBBLE_POP,       null, 1);
                bodyHelix(viewer, loc, 3, 0.40, -rotation,           Particle.SPLASH,            null, 1);
                headOrbit(viewer, loc, 10, 0.70, rotation,           Particle.DUST,             blue, 1);
                headOrbit(viewer, loc,  5, 0.50, -rotation,          Particle.BUBBLE_COLUMN_UP, null, 1);
            }
            case ECLIPSE -> {
                Particle.DustOptions dark = new Particle.DustOptions(Color.fromRGB(75,0,115), 1.5f);
                bodyHelix(viewer, loc, 3, 0.55, rotation,            Particle.PORTAL,        null, 1);
                bodyHelix(viewer, loc, 3, 0.45, -rotation,           Particle.DRAGON_BREATH, null, 1);
                headOrbit(viewer, loc, 10, 0.70, rotation * 1.5,    Particle.DUST,          dark, 1);
                headOrbit(viewer, loc,  5, 0.50, -rotation * 2,     Particle.DRAGON_BREATH, null, 1);
                if ((int)(rotation * 10) % 3 == 0)
                    for (int i = 0; i < 6; i++) {
                        double a = rotation * 3 + Math.toRadians(i * 60);
                        spawnAt(viewer, loc.clone().add(Math.cos(a)*0.8, 1.0, Math.sin(a)*0.8),
                                Particle.SQUID_INK, null, 1);
                    }
            }
            case GUARDIAN -> {
                Particle.DustOptions gold2 = new Particle.DustOptions(Color.fromRGB(255,215,0), 1.4f);
                bodyHelix(viewer, loc, 3, 0.55, rotation,            Particle.TOTEM_OF_UNDYING, null,  1);
                bodyHelix(viewer, loc, 3, 0.40, -rotation,           Particle.END_ROD,          null,  1);
                headOrbit(viewer, loc, 10, 0.70, rotation,           Particle.DUST,             gold2, 1);
                headOrbit(viewer, loc,  5, 0.50, -rotation,          Particle.TOTEM_OF_UNDYING, null,  1);
                if ((int)(rotation * 10) % 4 == 0)
                    for (int i = 0; i < 8; i++) {
                        double a = rotation * 2 + Math.toRadians(i * 45);
                        spawnAt(viewer, loc.clone().add(Math.cos(a)*1.2, 0.2, Math.sin(a)*1.2),
                                Particle.END_ROD, null, 1);
                    }
            }
            default -> {
                bodyHelix(viewer, loc, 2, 0.40, rotation, Particle.DUST, dust, 1);
                headOrbit(viewer, loc, 6, 0.50, rotation, Particle.DUST, dust, 1);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  BODY HELIX — legs (y=0) se head (y=2) tak shank/spiral
    //  helices = spiral arms ki count
    //  radius  = spiral radius
    //  angle   = current rotation
    // ══════════════════════════════════════════════════════════════════════
    private void bodyHelix(Player viewer, Location loc,
                           int helices, double radius, double angle,
                           Particle type, Particle.DustOptions dust, int count) {
        for (int h = 0; h < helices; h++) {
            double hOff = h * (Math.PI * 2.0 / helices);
            for (int y = 0; y <= 12; y++) {
                double yFrac  = y / 12.0;
                double helixA = angle * 3.5 + hOff + yFrac * Math.PI * 3.5;
                double r      = radius * (1.0 - yFrac * 0.25); // top pe thoda narrow
                Location pt   = loc.clone().add(
                        Math.cos(helixA) * r,
                        yFrac * 1.95,
                        Math.sin(helixA) * r);
                spawnAt(viewer, pt, type, dust, count);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  HEAD ORBIT — head ke upar orbit ring
    // ══════════════════════════════════════════════════════════════════════
    private void headOrbit(Player viewer, Location loc,
                           int points, double radius, double angle,
                           Particle type, Particle.DustOptions dust, int count) {
        for (int i = 0; i < points; i++) {
            double a  = angle + Math.toRadians(i * (360.0 / points));
            Location pt = loc.clone().add(Math.cos(a)*radius, 2.1, Math.sin(a)*radius);
            spawnAt(viewer, pt, type, dust, count);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  OWNER POINT SPAWN — first-person safe
    // ══════════════════════════════════════════════════════════════════════
    private void spawnOwnerPt(Player owner, Location pt,
                              EyeType eye, Particle.DustOptions dust) {
        try {
            if (needsDust(eye) && dust != null) {
                owner.spawnParticle(Particle.DUST, pt, 1, 0.01, 0.01, 0.01, 0, dust);
            } else if (eye.particle != null) {
                owner.spawnParticle(eye.particle, pt, 1, 0.01, 0.01, 0.01, 0.01);
            }
        } catch (Exception ignored) {}
    }

    // ══════════════════════════════════════════════════════════════════════
    //  SAFE SPAWN — null check with try-catch
    // ══════════════════════════════════════════════════════════════════════
    private void spawnAt(Player viewer, Location pt,
                         Particle type, Particle.DustOptions dust, int count) {
        try {
            if (dust != null && type == Particle.DUST) {
                viewer.spawnParticle(Particle.DUST, pt, count, 0.01, 0.01, 0.01, 0, dust);
            } else {
                viewer.spawnParticle(type, pt, count, 0.01, 0.01, 0.01, 0.02);
            }
        } catch (Exception ignored) {}
    }

    // ══════════════════════════════════════════════════════════════════════
    //  NEEDS DUST
    // ══════════════════════════════════════════════════════════════════════
    private boolean needsDust(EyeType eye) {
        return switch (eye) {
            case EARTH, CRYSTAL, ECHO -> true;
            default -> false;
        };
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PERPENDICULAR LEFT
    // ══════════════════════════════════════════════════════════════════════
    private Vector perpendicularLeft(Player p) {
        Vector dir = p.getLocation().getDirection().normalize();
        return new Vector(-dir.getZ(), 0, dir.getX()).normalize();
    }
}
