package com.ancienteye;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.GameMode;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import java.util.*;

public class AbilityLogic implements Listener {

    private final AncientEyePlugin plugin;
    private static final int INF = -1;

    // ── Level scaling ─────────────────────────────────────────────────────
    // getDmg  : L1=1.0  L2=1.2  L3=1.5
    // getDur  : L1=1.0  L2=1.5  L3=2.0  (potion tick multiplier)
    // getCd   : reads config, then subtracts 2s per level above 1
    // ─────────────────────────────────────────────────────────────────────

    public AbilityLogic(AncientEyePlugin plugin) { this.plugin = plugin; }

    // ══════════════════════════════════════════════════════════════════════
    //  GUI LOCK
    // ══════════════════════════════════════════════════════════════════════
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals("§8Your Ancient Eye Status")) e.setCancelled(true);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PASSIVE COMBAT — owner is NEVER the damaged entity from its own eye
    // ══════════════════════════════════════════════════════════════════════
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player victim) {
            EyeType ve = plugin.getPlayerData().getEye(victim);
            if (ve == EyeType.VOID && Math.random() < 0.15) {
                e.setCancelled(true);
                victim.sendMessage("§5§l✦ Void Dodge!");
                victim.getWorld().spawnParticle(Particle.PORTAL, victim.getLocation(), 30, 0.5, 1, 0.5, 0.1);
                return;
            }
            if (ve == EyeType.MIRAGE && Math.random() < 0.15) {
                e.setCancelled(true);
                victim.sendMessage("§e§l✦ Mirage! Attack missed!");
                victim.getWorld().spawnParticle(Particle.CLOUD, victim.getLocation(), 20, 0.4, 0.8, 0.4, 0.04);
                return;
            }
            if (ve == EyeType.GUARDIAN) e.setDamage(e.getDamage() * 0.65);
        }
        if (e.getDamager() instanceof Player atk) {
            EyeType ae = plugin.getPlayerData().getEye(atk);
            if (ae == EyeType.RAGE) {
                double hp  = atk.getHealth();
                double max = atk.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
                if (hp / max < 0.35) {
                    e.setDamage(e.getDamage() * 1.75);
                    atk.getWorld().spawnParticle(Particle.DUST, atk.getLocation(), 10, 0.3, 0.6, 0.3, 0,
                        new Particle.DustOptions(Color.RED, 1.5f));
                }
            }
            if (ae == EyeType.ECLIPSE) {
                long t = atk.getWorld().getTime();
                if (t > 13000 && t < 23000) e.setDamage(e.getDamage() * 1.30);
             
            
    // ── HUNTER MARK — extra damage handler ───────────────────────────────────────
// onEntityDamage mein add karo — existing code ke baad

// Agar entity marked hai toh extra damage do
if (e.getEntity() instanceof LivingEntity victim) {
    if (victim.hasMetadata("hunterMarked")) {
        // Mark lagane wala hunter check karo
        String markerUUID = victim.getMetadata("hunterMarked").get(0).asString();
        // Attacker hunter hai ya koi bhi attack kare — extra damage
        e.setDamage(e.getDamage() * 1.5); // 50% extra damage
    }
  }
}

        @EventHandler
public void onTitanDamage(EntityDamageEvent event) {
    if (event.getEntity() instanceof Player p) {
        // Agar player Titan Mode mein hai, toh uska damage cancel kar do
        if (p.hasMetadata("TitanMode")) {
            event.setCancelled(true);
        }
     }
    }

    
@EventHandler
public void onPlayerKillXP(org.bukkit.event.entity.PlayerDeathEvent e) {
    if (e.getEntity().getKiller() == null) return;
    Player killer = e.getEntity().getKiller();
    if (plugin.getPlayerData().getEye(killer) == EyeType.NONE) return;
    plugin.getPlayerData().addXp(killer,
        plugin.getConfig().getInt("settings.xp-per-kill", 1));
}
    //════════════════════════════════════════════════════════════════════
@EventHandler
public void onFootstep(org.bukkit.event.player.PlayerMoveEvent e) {
    Player mover = e.getPlayer();

    // Sirf ground movement — jump/fall ignore
    if (!mover.isOnGround()) return;

    // Har 10 ticks ek baar check — performance ke liye
    if (mover.getTicksLived() % 10 != 0) return;

    // Koi bhi ECHO eye holder nearby check karo
    for (Player owner : Bukkit.getOnlinePlayers()) {
        if (owner.getUniqueId().equals(mover.getUniqueId())) continue;
        EyeType ownerEye = plugin.getPlayerData().getEye(owner);
        if (ownerEye != EyeType.ECHO) continue;

        double distSq = owner.getLocation().distanceSquared(mover.getLocation());
        if (distSq > 20 * 20) continue; // 20 block range

        // Owner ko signal — action bar par dikhao
        double dist = Math.sqrt(distSq);
        String dir  = getDirectionLabel(owner, mover);
        owner.sendActionBar("§3👣 §f" + mover.getName()
            + " §7footstep §b" + (int)dist + "§7 blocks §3" + dir);

        // Footstep particle — sirf owner ko dikhao
        owner.spawnParticle(Particle.SONIC_BOOM,
            mover.getLocation().add(0, 0.1, 0), 1, 0, 0, 0, 0);

        // Footstep damage — 0.5 damage har step (passive)
        if (mover instanceof LivingEntity le) {
            // Sirf enemies — agar mover bhi ECHO hai toh skip
            EyeType moverEye = plugin.getPlayerData().getEye(mover);
            if (moverEye != EyeType.ECHO) {
                le.damage(0.5, owner);
            }
        }
    }
}

// Direction label helper
private String getDirectionLabel(Player from, Player to) {
    double dx = to.getLocation().getX() - from.getLocation().getX();
    double dz = to.getLocation().getZ() - from.getLocation().getZ();
    double angle = Math.toDegrees(Math.atan2(dz, dx));
    double yaw   = (from.getLocation().getYaw() + 90) % 360;
    double rel   = ((angle - yaw) % 360 + 360) % 360;
    if (rel < 45 || rel >= 315) return "▲ Front";
    if (rel < 135)              return "▶ Right";
    if (rel < 225)              return "▼ Behind";
    return                             "◀ Left";
}

    // ══════════════════════════════════════════════════════════════════════
    //  PRIMARY  (SHIFT + F)
    // ══════════════════════════════════════════════════════════════════════
    public void activatePrimary(Player p, EyeType eye) {
        if (plugin.getCooldownManager().isOnCooldown(p, "P")) return;
        plugin.getCooldownManager().setCooldown(p, "P", getCd(p, eye, "primary"));
        Location loc = p.getLocation().clone();
        Vector   dir = p.getEyeLocation().getDirection().normalize();
        double   dm  = getDmg(p);
        double   dr  = getDur(p);
        World    w   = p.getWorld();

        switch (eye) {

            // 1. VOID — Void Blink
            case VOID -> {
                voidRift(w, loc);
                w.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.6f);
                w.playSound(loc, Sound.BLOCK_PORTAL_AMBIENT, 0.5f, 1.5f);
                for (int i = 1; i <= 8; i++)
                    w.spawnParticle(Particle.PORTAL, loc.clone().add(dir.clone().multiply(i)), 5, 0.15, 0.35, 0.15, 0.08);
                Location dest = safe(loc.clone().add(dir.clone().multiply(8)));
                voidRift(w, dest);
                w.strikeLightningEffect(dest);
                p.teleport(dest);
            }

            // ── PHANTOM PRIMARY — Arcane Circle Trap ─────────────────────────────────────
// Ground par aim based arcane circle spawns
// 15 block radius, 6 seconds
// Inside enemies: freeze + 1 heart/second damage
// Owner safe, flat ground particle structure, spinning
case PHANTOM -> {
    // Aim — ground par raycast
    Location groundLoc = null;
    Location eyeLoc = p.getEyeLocation();
    Vector   eyeDir = eyeLoc.getDirection().normalize();

    // Ground dhundo — 50 blocks tak raycast
    for (int i = 1; i <= 50; i++) {
        Location check = eyeLoc.clone().add(eyeDir.clone().multiply(i));
        if (check.getBlock().getType().isSolid()) {
            groundLoc = check.clone().add(0, 1, 0); // block ke upar
            break;
        }
    }
    // Agar ground nahi mila — player ke neeche
    if (groundLoc == null) {
        groundLoc = p.getLocation().clone();
        groundLoc.setY(p.getLocation().getBlockY());
    }

    final Location center = groundLoc.clone();
    final double   RADIUS = 7.5; // 15 block diameter

    // ── SPAWN SOUNDS ─────────────────────────────────────────────────────
    w.playSound(center, Sound.ENTITY_ENDERMAN_STARE,      1f, 0.5f);
    w.playSound(center, Sound.BLOCK_BEACON_AMBIENT,       1f, 0.3f);
    w.playSound(center, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.8f, 0.6f);

    // ── ARCANE CIRCLE ANIMATION — 6 seconds ──────────────────────────────
    new BukkitRunnable() {
        int    ticks = 0;
        double spin  = 0; // rotation angle

        public void run() {
            if (ticks++ >= 120 || !p.isOnline()) { // 6s = 120 ticks
                // Fade out
                for (int i = 0; i < 36; i++) {
                    double a = Math.toRadians(i * 10);
                    w.spawnParticle(Particle.CLOUD,
                        center.clone().add(Math.cos(a)*RADIUS, 0.1, Math.sin(a)*RADIUS),
                        2, 0.2, 0.1, 0.2, 0.03);
                }
                w.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.7f);
                cancel();
                return;
            }

            spin += 0.04; // slow spin

            // ── LAYER 1: Outer ring ───────────────────────────────────────
            int outerPts = 60;
            for (int i = 0; i < outerPts; i++) {
                double a = spin + Math.toRadians(i * (360.0 / outerPts));
                Location pt = center.clone().add(
                    Math.cos(a) * RADIUS, 0.05, Math.sin(a) * RADIUS);
                Particle.DustOptions gold = new Particle.DustOptions(
                    org.bukkit.Color.fromRGB(200, 160, 20), 1.0f);
                w.spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0, gold);
            }

            // ── LAYER 2: Inner ring (counter-spin) ────────────────────────
            int innerPts = 40;
            double innerR = RADIUS * 0.65;
            for (int i = 0; i < innerPts; i++) {
                double a = -spin * 1.3 + Math.toRadians(i * (360.0 / innerPts));
                Location pt = center.clone().add(
                    Math.cos(a) * innerR, 0.05, Math.sin(a) * innerR);
                Particle.DustOptions gold2 = new Particle.DustOptions(
                    org.bukkit.Color.fromRGB(220, 180, 40), 0.9f);
                w.spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0, gold2);
            }

            // ── LAYER 3: Innermost ring ───────────────────────────────────
            double coreR = RADIUS * 0.30;
            int corePts = 24;
            for (int i = 0; i < corePts; i++) {
                double a = spin * 2.0 + Math.toRadians(i * (360.0 / corePts));
                Location pt = center.clone().add(
                    Math.cos(a) * coreR, 0.05, Math.sin(a) * coreR);
                Particle.DustOptions white = new Particle.DustOptions(
                    org.bukkit.Color.fromRGB(255, 240, 180), 1.1f);
                w.spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0, white);
            }

            // ── LAYER 4: 8 spokes from center to outer ring ───────────────
            for (int spoke = 0; spoke < 8; spoke++) {
                double spokeAngle = spin + Math.toRadians(spoke * 45);
                int spokePts = 14;
                for (int k = 1; k <= spokePts; k++) {
                    double r = (k / (double) spokePts) * RADIUS;
                    Location pt = center.clone().add(
                        Math.cos(spokeAngle) * r, 0.05, Math.sin(spokeAngle) * r);
                    Particle.DustOptions spokeDust = new Particle.DustOptions(
                        org.bukkit.Color.fromRGB(200, 160, 20), 0.8f);
                    w.spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0, spokeDust);
                }
            }

            // ── LAYER 5: 8 outer circles (like image) ─────────────────────
            for (int orb = 0; orb < 8; orb++) {
                double orbAngle = -spin * 0.5 + Math.toRadians(orb * 45);
                double orbX = Math.cos(orbAngle) * (RADIUS * 0.82);
                double orbZ = Math.sin(orbAngle) * (RADIUS * 0.82);
                // Small circle at each orb position
                for (int oi = 0; oi < 10; oi++) {
                    double oa = spin * 2 + Math.toRadians(oi * 36);
                    Location orbPt = center.clone().add(
                        orbX + Math.cos(oa) * 0.5, 0.05,
                        orbZ + Math.sin(oa) * 0.5);
                    Particle.DustOptions orbDust = new Particle.DustOptions(
                        org.bukkit.Color.fromRGB(230, 190, 50), 1.2f);
                    w.spawnParticle(Particle.DUST, orbPt, 1, 0, 0, 0, 0, orbDust);
                }
            }

            // ── LAYER 6: Cross/triangle inner pattern ─────────────────────
            for (int tri = 0; tri < 3; tri++) {
                double triAngle = spin * 0.7 + Math.toRadians(tri * 120);
                double triX = Math.cos(triAngle) * (RADIUS * 0.45);
                double triZ = Math.sin(triAngle) * (RADIUS * 0.45);
                // Line from center to triangle point
                for (int k = 1; k <= 8; k++) {
                    double t = k / 8.0;
                    Location triPt = center.clone().add(
                        triX * t, 0.05, triZ * t);
                    Particle.DustOptions triDust = new Particle.DustOptions(
                        org.bukkit.Color.fromRGB(210, 170, 30), 0.85f);
                    w.spawnParticle(Particle.DUST, triPt, 1, 0, 0, 0, 0, triDust);
                }
            }

            // ── CENTER glow ───────────────────────────────────────────────
            if (ticks % 3 == 0) {
                w.spawnParticle(Particle.END_ROD,
                    center.clone().add(0, 0.1, 0), 2, 0.1, 0.05, 0.1, 0.01);
                Particle.DustOptions centerDust = new Particle.DustOptions(
                    org.bukkit.Color.fromRGB(255, 220, 100), 1.3f);
                w.spawnParticle(Particle.DUST,
                    center.clone().add(0, 0.1, 0), 1, 0, 0, 0, 0, centerDust);
            }

            // ── DAMAGE + FREEZE — enemies inside circle ───────────────────
            if (ticks % 20 == 0) { // every second
                center.getWorld().getNearbyEntities(center, RADIUS, 2, RADIUS)
                    .forEach(e -> {
                        if (!(e instanceof LivingEntity le) || e == p) return;
                        // Distance check — circle mein hai?
                        double dist = Math.sqrt(
                            Math.pow(e.getLocation().getX()-center.getX(), 2) +
                            Math.pow(e.getLocation().getZ()-center.getZ(), 2));
                        if (dist > RADIUS) return;

                        // 1 heart = 2 damage per second
                        le.damage(2.0 * dm, p);
                        le.addPotionEffect(new PotionEffect(
                            PotionEffectType.SLOWNESS, 25, 10, false, false));

                        // Freeze particles on enemy
                        w.spawnParticle(Particle.SNOWFLAKE,
                            le.getLocation().add(0,1,0), 8, 0.3, 0.5, 0.3, 0.02);
                        w.spawnParticle(Particle.END_ROD,
                            le.getLocation().add(0,1,0), 3, 0.2, 0.4, 0.2, 0.02);
                    });
            }

            // ── Ambient portal particles rising ───────────────────────────
            if (ticks % 5 == 0) {
                double rndA = Math.random() * Math.PI * 2;
                double rndR = Math.random() * RADIUS;
                w.spawnParticle(Particle.REVERSE_PORTAL,
                    center.clone().add(Math.cos(rndA)*rndR, 0.1, Math.sin(rndA)*rndR),
                    1, 0, 0.1, 0, 0.01);
            }
        }
    }.runTaskTimer(plugin, 0, 1);
}

                
case STORM -> {
    double dmg = ecfg("STORM", "primary-damage", 12.0);
    LivingEntity target = aim(p, 30.0); // 30 block range tak aim
    
    if (target == null) {
        p.sendMessage("§cKoi target nahi mila!");
        return;
    }

    Location tLoc = target.getLocation();
    w.strikeLightningEffect(tLoc); // Visual lightning
    w.playSound(tLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2f, 1f);

    // Masterpiece Animation: Blocks udne wala effect
    for (int i = 0; i < 8; i++) {
        FallingBlock fb = w.spawnFallingBlock(tLoc.clone().add(0, 0.5, 0), w.getBlockAt(tLoc.clone().subtract(0, 1, 0)).getBlockData());
        fb.setVelocity(new Vector((Math.random() - 0.5) * 0.5, 0.6 + Math.random(), (Math.random() - 0.5) * 0.5));
        fb.setDropItem(false);
    }

    // Explosion aur Particles
    w.spawnParticle(Particle.EXPLOSION_EMITTER, tLoc, 1);
    w.spawnParticle(Particle.FLASH, tLoc, 5);
    w.spawnParticle(Particle.ELECTRIC_SPARK, tLoc, 50, 1, 1, 1, 0.1);

    // Safe Damage (Target ko lagega, Owner ko nahi)
    applySafeDamage(p, tLoc, 4.0, dmg);
  }


            /// ── FROST PRIMARY — Ice Shield Orbit ─────────────────────────────────────────
// 4 ice shields player ke center mein orbit karenge
// 5 seconds tak koi bhi attack (mob, arrow, player) owner ko hit nahi karega
case FROST -> {
    w.spawnParticle(Particle.SNOWFLAKE, loc, 60, 0.6, 0.4, 0.6, 0.08);
    w.spawnParticle(Particle.WHITE_ASH, loc, 30, 0.4, 0.2, 0.4, 0.05);
    w.playSound(loc, Sound.ENTITY_PLAYER_HURT_FREEZE, 1f, 1.5f);
    w.playSound(loc, Sound.BLOCK_GLASS_BREAK,         0.8f, 1.8f);
    w.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 1.8f);

    p.sendTitle("\u00a7b\u00a7lICE SHIELD", "\u00a77Protected for 5 seconds!", 5, 60, 10);

    // Shield active marker
    final boolean[] shieldActive = {true};

    // ── PROTECTION: EntityDamageByEntityEvent cancel ─────────────────────
    // Temporary Listener — 5s tak owner ko damage cancel
    final org.bukkit.event.Listener shieldListener = new org.bukkit.event.Listener() {

        @org.bukkit.event.EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
        public void onDamage(org.bukkit.event.entity.EntityDamageEvent ev) {
            if (!shieldActive[0]) return;
            if (!ev.getEntity().getUniqueId().equals(p.getUniqueId())) return;
            // Cancel all damage while shield is active
            ev.setCancelled(true);
            // Shield break effect on hit
            Location pLoc = p.getLocation();
            w.spawnParticle(Particle.SNOWFLAKE, pLoc, 20, 0.5, 0.8, 0.5, 0.05);
            w.spawnParticle(Particle.WHITE_ASH, pLoc, 10, 0.4, 0.6, 0.4, 0.03);
            w.playSound(pLoc, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.5f);
        }
    };
    plugin.getServer().getPluginManager().registerEvents(shieldListener, plugin);

    // ── SHIELD ORBIT ANIMATION — 4 ice panels rotating around player ─────
    new BukkitRunnable() {
        int    ticks       = 0;
        double orbitAngle  = 0;
        final int duration = ticks(100, dr); // 5 seconds

        public void run() {
            if (ticks++ >= duration || !p.isOnline()) {
                // Shield expired
                shieldActive[0] = false;
                org.bukkit.event.HandlerList.unregisterAll(shieldListener);

                // Break animation
                Location pLoc = p.getLocation().clone().add(0, 1, 0);
                w.spawnParticle(Particle.SNOWFLAKE, pLoc, 80, 1.0, 1.2, 1.0, 0.08);
                w.spawnParticle(Particle.WHITE_ASH, pLoc, 50, 0.8, 1.0, 0.8, 0.05);
                for (int i = 0; i < 16; i++) {
                    double a = Math.toRadians(i * 22.5);
                    w.spawnParticle(Particle.SNOWFLAKE,
                        pLoc.clone().add(Math.cos(a)*1.0, 0, Math.sin(a)*1.0),
                        3, 0.1, 0.2, 0.1, 0.03);
                }
                w.playSound(p.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 1f, 2.0f);
                w.playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK,         1f, 0.8f);
                cancel();
                return;
            }

            orbitAngle += 0.08; // orbit speed

            Location center = p.getLocation().clone().add(0, 1.0, 0);

            // 4 ice shield panels — 90 degrees apart
            for (int panel = 0; panel < 4; panel++) {
                double panelAngle = orbitAngle + (panel * Math.PI / 2); // 90° apart

                // Shield center position — 1.0 block from player center
                double sx = Math.cos(panelAngle) * 1.0;
                double sz = Math.sin(panelAngle) * 1.0;
                Location shieldCenter = center.clone().add(sx, 0, sz);

                // ── Ice panel — rectangular grid of particles ─────────────
                // Panel is perpendicular to orbit direction (tangent)
                double tangX = -Math.sin(panelAngle); // perpendicular
                double tangZ =  Math.cos(panelAngle);

                // Panel size: 0.8 wide x 1.2 tall, 5x7 grid
                for (int col = -2; col <= 2; col++) {
                    for (int row = -2; row <= 2; row++) {
                        double px = shieldCenter.getX() + tangX * col * 0.2;
                        double py = shieldCenter.getY() + row * 0.25;
                        double pz = shieldCenter.getZ() + tangZ * col * 0.2;

                        Location pt = new Location(w, px, py, pz);

                        // Ice blue color — brighter at edges
                        boolean isEdge = (Math.abs(col) == 2 || Math.abs(row) == 2);
                        Particle.DustOptions iceDust = isEdge
                            ? new Particle.DustOptions(
                                org.bukkit.Color.fromRGB(180, 240, 255), 1.3f)
                            : new Particle.DustOptions(
                                org.bukkit.Color.fromRGB(220, 248, 255), 0.9f);

                        w.spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0, iceDust);
                    }
                }

                // Panel glow — SNOWFLAKE on edges
                if (ticks % 3 == 0) {
                    for (int col = -2; col <= 2; col++) {
                        double px = shieldCenter.getX() + tangX * col * 0.2;
                        double pz = shieldCenter.getZ() + tangZ * col * 0.2;
                        w.spawnParticle(Particle.SNOWFLAKE,
                            new Location(w, px, shieldCenter.getY() + 0.5, pz),
                            1, 0, 0, 0, 0.01);
                        w.spawnParticle(Particle.SNOWFLAKE,
                            new Location(w, px, shieldCenter.getY() - 0.5, pz),
                            1, 0, 0, 0, 0.01);
                    }
                }

                // Ice crystal at panel center — sparkle effect
                if (ticks % 5 == 0) {
                    w.spawnParticle(Particle.END_ROD, shieldCenter, 1, 0, 0.1, 0, 0.01);
                }
            }

            // Inner glow ring around player
            if (ticks % 2 == 0) {
                for (int i = 0; i < 12; i++) {
                    double a = orbitAngle * 2 + Math.toRadians(i * 30);
                    Particle.DustOptions glow = new Particle.DustOptions(
                        org.bukkit.Color.fromRGB(150, 220, 255), 0.8f);
                    w.spawnParticle(Particle.DUST,
                        center.clone().add(Math.cos(a)*0.4, 0, Math.sin(a)*0.4),
                        1, 0, 0, 0, 0, glow);
                }
            }

            // Ambient ice particles
            if (ticks % 4 == 0) {
                w.spawnParticle(Particle.WHITE_ASH,
                    center, 3, 0.5, 0.5, 0.5, 0.02);
            }

            // Pulse sound every second
            if (ticks % 20 == 0) {
                w.playSound(p.getLocation(),
                    Sound.ENTITY_PLAYER_HURT_FREEZE, 0.3f, 1.8f);
            }
        }
    }.runTaskTimer(plugin, 0, 1);
}


            // 5. FLAME — Fire Tornado (6s Duration, 10 Block Height, Suck & Spin)
case FLAME -> {  
    // Aim-based: Jis block par dekhoge wahan spawn hoga (Max 30 blocks door)
    org.bukkit.block.Block targetBlock = p.getTargetBlock(null, 30);
    if (targetBlock.getType() == Material.AIR) {
        p.sendMessage("§cAim at a block to spawn the Tornado!");
        return;
    }
    
    // Tornado ka fixed center (zameen se thoda upar)
    final Location center = targetBlock.getLocation().add(0.5, 1.0, 0.5);

    new BukkitRunnable() {
        int ticks = 0;
        double angle = 0;

        @Override
        public void run() {
            // 6 seconds = 120 ticks (Exact 6s baad gayab)
            if (ticks >= 120) { 
                cancel(); 
                return; 
            }
            ticks++;
            angle += 0.4; // Tornado ke ghumne ki speed

            // ── 1. DRAW TORNADO SHAPE (10 Blocks High) ──
            for (double y = 0; y <= 10; y += 0.5) {
                // Shape math: Niche patla, upar chauda (Cone shape)
                double radius = 0.5 + (y * 0.25); 
                
                // 3 Spiral arms banayenge mast look ke liye
                for (int arm = 0; arm < 3; arm++) {
                    double armOffset = Math.toRadians(arm * 120);
                    // (y * 0.5) se tornado "twist" hota hua dikhega
                    double currentAngle = angle + (y * 0.5) + armOffset; 

                    double cx = center.getX() + radius * Math.cos(currentAngle);
                    double cz = center.getZ() + radius * Math.sin(currentAngle);
                    Location partLoc = new Location(center.getWorld(), cx, center.getY() + y, cz);

                    // Particles
                    center.getWorld().spawnParticle(Particle.FLAME, partLoc, 2, 0, 0, 0, 0);
                    if (y % 2 == 0) {
                        center.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, partLoc, 1, 0, 0, 0, 0);
                    }
                    if (Math.random() < 0.1) {
                        center.getWorld().spawnParticle(Particle.LAVA, partLoc, 1, 0, 0, 0, 0);
                    }
                }
            }

            // ── 2. SUCK, SPIN & DAMAGE ENTITIES ──
            // 8 blocks ke radius mein jo bhi hoga, kheencha chala aayega
            for (org.bukkit.entity.Entity e : center.getWorld().getNearbyEntities(center, 8, 10, 8)) {
                if (!(e instanceof LivingEntity le) || e == p) continue; // Owner is safe!

                Location eLoc = e.getLocation();
                
                // Center calculate karo taaki entity seedha khinche
                Location tCenter = center.clone();
                tCenter.setY(eLoc.getY()); 

                Vector toCenter = tCenter.toVector().subtract(eLoc.toVector());
                double dist = toCenter.length();

                // Agar 8 block ke andar hai, toh vacuum chalu
                if (dist > 0 && dist <= 8) {
                    toCenter.normalize();
                    
                    // Cross Product math: Entity ko gol ghumane ke liye (Spin vector)
                    Vector spin = new Vector(-toCenter.getZ(), 0, toCenter.getX());
                    
                    // Final Velocity: Andar khincho (0.15) + Gol ghumao (0.4) + Hawa mein uthao (0.12)
                    Vector vortexVel = toCenter.multiply(0.15).add(spin.multiply(0.4)).setY(0.12);
                    
                    e.setVelocity(vortexVel);
                    e.setFallDistance(0f); // Fall damage se bachane ke liye (Glitch fix)
                    
                    // Aag lagao
                    le.setFireTicks(60); 
                    
                    // Har 0.5 second mein (10 ticks) damage do
                    if (ticks % 10 == 0) {
                        le.damage(1.5 * dm, p); 
                    }
                }
            }
        }
    }.runTaskTimer(plugin, 0, 1);
}


            // 6. SHADOW — Shadow Step (teleport behind nearest player + backstab)
            case SHADOW -> {
                LivingEntity tgt = aim(p, 20);
                if (tgt != null) {
                    w.spawnParticle(Particle.SMOKE, loc, 50, 0.4, 0.8, 0.4, 0.07);
                    w.spawnParticle(Particle.DUST,  loc, 25, 0.3, 0.6, 0.3, 0, new Particle.DustOptions(Color.BLACK, 2f));
                    Vector behind = tgt.getLocation().getDirection().normalize().multiply(2.0);
                    Location dest = safe(tgt.getLocation().clone().subtract(behind));
                    dest.setYaw(tgt.getLocation().getYaw() + 180);
                    w.spawnParticle(Particle.SMOKE, dest, 50, 0.4, 0.8, 0.4, 0.07);
                    w.spawnParticle(Particle.PORTAL,dest, 30, 0.3, 0.7, 0.3, 0.08);
                    w.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.7f);
                    p.teleport(dest);
                    tgt.damage(7.0 * dm, p);
                    w.spawnParticle(Particle.CRIT, tgt.getLocation(), 30, 0.4, 0.8, 0.4, 0.1);
                } else p.sendMessage("§7No target in range.");
            }
// 7. TITAN — Giant Form (5s Transformation)
case TITAN -> {
    double dm = getDmg(p);
    
    // ── 1. TRANSFORMATION START (Sounds & Effects) ──
    w.playSound(loc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.5f, 0.5f);
    w.playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.2f, 0.5f);
    w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.4f);
    
    // Bada dikhne ke liye Initial Burst
    w.spawnParticle(Particle.EXPLOSION_EMITTER, loc, 5, 1, 2, 1, 0);
    w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 100, 1.5, 3, 1.5, 0.05);

    // Jump Boost 3 & Resistance 255 (Invincibility) - 5 seconds
    p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 100, 2)); // Level 3 (0 index = Level 1)
    p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 254)); // 255 Level = No Damage
    
    // Custom Metadata taaki doosre players aapko hit na kar sakein (Invincibility Check)
    p.setMetadata("TitanMode", new org.bukkit.metadata.FixedMetadataValue(plugin, true));

    p.sendTitle("§6§lTITAN FORM", "§eYou are Unstoppable!", 5, 40, 5);

    // ── 2. TITAN ANIMATION RUNNABLE (5s) ──
    new BukkitRunnable() {
        int ticks = 0;

        @Override
        public void run() {
            if (!p.isOnline() || ticks >= 100) { // 5 seconds complete
                // ── 3. NORMAL FORM BACK ──
                p.removeMetadata("TitanMode", plugin);
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_IRON_GOLEM_DEATH, 1f, 1.5f);
                p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation(), 50, 1, 2, 1, 0.1);
                p.sendMessage("§cTitan Form expired. You are back to normal.");
                cancel();
                return;
            }

            ticks++;
            Location cur = p.getLocation();

            // Giant Aura Animation (Aapke charo taraf dhool aur pathar udenge)
            if (ticks % 2 == 0) {
                w.spawnParticle(Particle.FALLING_DUST, cur, 15, 1.2, 0.1, 1.2, 0.05, Material.STONE.createBlockData());
                w.spawnParticle(Particle.DUST, cur, 10, 1.5, 2.5, 1.5, new Particle.DustOptions(Color.fromRGB(120, 80, 40), 2.0f));
            }

            // Heavy Footsteps (Har 10 ticks par zameen hilegi)
            if (ticks % 10 == 0) {
                w.playSound(cur, Sound.BLOCK_ANVIL_LAND, 0.5f, 0.5f);
                // Nearby enemies ko shockwave se peeche dhakelo
                p.getNearbyEntities(5, 3, 5).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        Vector push = le.getLocation().toVector().subtract(cur.toVector()).normalize().multiply(1.2).setY(0.4);
                        le.setVelocity(push);
                        le.damage(2.0 * dm, p);
                    }
                });
            }
        }
    }.runTaskTimer(plugin, 0, 1);
}

// ── HUNTER PRIMARY — Hunter Dash ─────────────────────────────────────────────
// Aim based — target ki taraf fast dash
// Owner ko koi damage nahi, enemy ko damage hoga
case HUNTER -> {
    LivingEntity tgt = aim(p, 30);
    if (tgt != null) {
        Location tgtLoc = tgt.getLocation();

        // Direction — player se target ki taraf
        Vector dashDir = tgtLoc.toVector()
            .subtract(p.getLocation().toVector())
            .normalize();

        // Origin particles
        w.spawnParticle(Particle.CRIT,     loc, 30, 0.3, 0.5, 0.3, 0.12);
        w.spawnParticle(Particle.END_ROD,  loc, 15, 0.2, 0.4, 0.2, 0.06);
        w.spawnParticle(Particle.SOUL,     loc, 10, 0.3, 0.5, 0.3, 0.05);
        w.playSound(loc, Sound.ENTITY_ARROW_SHOOT,  1f, 1.8f);
        w.playSound(loc, Sound.ENTITY_PHANTOM_FLAP, 0.8f, 1.5f);

        // No fall damage
        p.setFallDistance(0f);

        // Dash — 1 tick baad set karo
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            p.setVelocity(dashDir.clone().multiply(1.8).setY(0.35));
            p.setFallDistance(0f);
        }, 1L);

        // Trail + damage on hit
        new BukkitRunnable() {
            int t = 0;
            public void run() {
                if (t++ >= 10) {
                    p.setFallDistance(0f);
                    cancel(); return;
                }

                Location cur = p.getLocation();

                // Trail particles
                w.spawnParticle(Particle.CRIT,    cur, 5, 0.2, 0.3, 0.2, 0.08);
                w.spawnParticle(Particle.END_ROD, cur, 2, 0.1, 0.2, 0.1, 0.02);
                w.spawnParticle(Particle.SOUL,    cur, 3, 0.2, 0.3, 0.2, 0.04);

                p.setFallDistance(0f);

                // Hit check — target ke 2.5 block mein
                if (p.getLocation().distanceSquared(tgtLoc) < 2.5 * 2.5) {
                    tgt.damage(7.0 * dm, p);

                    // Hit burst
                    w.spawnParticle(Particle.CRIT,    tgtLoc, 40, 0.5, 0.8, 0.5, 0.12);
                    w.spawnParticle(Particle.END_ROD, tgtLoc,  20, 0.4, 0.7, 0.4, 0.08);
                    w.spawnParticle(Particle.SOUL,    tgtLoc,  15, 0.3, 0.6, 0.3, 0.05);
                    w.playSound(tgtLoc, Sound.ENTITY_ARROW_HIT_PLAYER, 1f, 1.2f);
                    w.playSound(tgtLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1f);

                    // Knockback
                    Vector knock = dashDir.clone().multiply(1.5).setY(0.4);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (tgt.isValid()) tgt.setVelocity(knock);
                    }, 1L);

                    p.setFallDistance(0f);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 2L, 1L);

    } else {
        p.sendMessage("§7No target in range.");
    }
}

            // 9. GRAVITY — Gravity Pull  [knockback 1 tick after damage]
            case GRAVITY -> {
                w.spawnParticle(Particle.REVERSE_PORTAL, loc, 100, 2.5, 2, 2.5, 0.14);
                w.spawnParticle(Particle.PORTAL,         loc,  60, 2.0, 1.5, 2.0, 0.10);
                for (int ring = 0; ring < 6; ring++) { final int fr = ring;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        double r = 3.5 - fr * 0.4;
                        for (int i = 0; i < 20; i++) { double a = Math.toRadians(i*18)+fr*0.5;
                            w.spawnParticle(Particle.REVERSE_PORTAL, loc.clone().add(Math.cos(a)*r, fr*0.4, Math.sin(a)*r), 2, 0, 0, 0, 0.03);
                        }
                    }, ring * 2L);
                }
                w.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE,  1f, 0.3f);
                w.playSound(loc, Sound.ENTITY_ENDERMAN_SCREAM, 0.5f, 0.5f);
                p.getNearbyEntities(8, 8, 8).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        Vector vel = loc.toVector().subtract(e.getLocation().toVector()).normalize().multiply(2.2);
                        le.damage(3.0 * dm, p);
                        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (e.isValid()) e.setVelocity(vel); }, 1L);
                    }
                });
            }

            // 10. WIND — Wind Dash  [knockback 1 tick after damage]
            case WIND -> {
                w.spawnParticle(Particle.WHITE_ASH, loc, 80, 0.8, 0.8, 0.8, 0.18);
                w.spawnParticle(Particle.CLOUD,     loc, 40, 0.5, 0.5, 0.5, 0.08);
                p.setVelocity(dir.clone().multiply(3.5).setY(0.2));
                w.playSound(loc, Sound.ENTITY_PHANTOM_DEATH, 0.8f, 2.0f);
                new BukkitRunnable() { int t = 0;
                    public void run() {
                        if (t++ >= 10) { cancel(); return; }
                        Location cur = p.getLocation();
                        for (int i = 0; i < 6; i++) { double a = Math.toRadians(i*60+t*30);
                            w.spawnParticle(Particle.WHITE_ASH, cur.clone().add(Math.cos(a)*1.2,0.5,Math.sin(a)*1.2), 2, 0, 0, 0, 0.05);
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

            // 11. POISON — Poison Strike
            case POISON -> {
                LivingEntity tgt = aim(p, 12);
                if (tgt != null) {
                    w.spawnParticle(Particle.ITEM_SLIME, tgt.getLocation(), 50, 0.5, 0.8, 0.5, 0.07);
                    w.spawnParticle(Particle.SNEEZE,     tgt.getLocation(), 25, 0.4, 0.7, 0.4, 0.04);
                    w.spawnParticle(Particle.DUST,       tgt.getLocation(), 30, 0.4, 0.7, 0.4, 0,
                        new Particle.DustOptions(Color.fromRGB(40,200,40), 1.8f));
                    for (int i = 0; i < 12; i++) { double a = Math.toRadians(i*30);
                        w.spawnParticle(Particle.ITEM_SLIME, tgt.getLocation().add(Math.cos(a)*1.5,0.5,Math.sin(a)*1.5), 3, 0, 0.2, 0, 0.02);
                    }
                    tgt.addPotionEffect(new PotionEffect(PotionEffectType.POISON,   ticks(200, dr), 2));
                    tgt.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks(100, dr), 1));
                    tgt.damage(4.0 * dm, p);
                    w.playSound(loc, Sound.ENTITY_SLIME_SQUISH, 1f, 0.4f);
                    w.playSound(loc, Sound.ENTITY_WITCH_DRINK,  0.8f, 0.6f);
                } else p.sendMessage("§7No target in range.");
            }
//════════════════════════════════════════════════════════════════════
case LIGHT -> {
    // Origin flash + expanding rings
    w.spawnParticle(Particle.FLASH,   loc, 3, 0, 0, 0, 0);
    w.spawnParticle(Particle.END_ROD, loc, 80, 2.5, 2.5, 2.5, 0.12);
    w.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT,    1f, 2.0f);
    w.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 2.0f);

    for (int ring = 1; ring <= 4; ring++) {
        final int fr = ring;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            double r = fr * 2.8;
            for (int i = 0; i < 24; i++) {
                double a = Math.toRadians(i * 15);
                w.spawnParticle(Particle.END_ROD,
                    loc.clone().add(Math.cos(a)*r, 0.5, Math.sin(a)*r),
                    2, 0, 0.2, 0, 0.01);
            }
        }, ring * 3L);
    }

    // Damage + white screen sab nearby entities
    p.getNearbyEntities(10, 10, 10).forEach(e -> {
        if (!(e instanceof LivingEntity le) || e == p) return;

        le.damage(5.0 * dm, p);
        w.spawnParticle(Particle.FLASH,   le.getLocation(), 1, 0, 0, 0, 0);
        w.spawnParticle(Particle.END_ROD, le.getLocation(), 20, 0.4, 0.8, 0.4, 0.05);

        // Sirf Player ka screen white hoga — mobs ke paas screen nahi
        if (le instanceof Player ep) {
            whiteLightScreen(ep, ticks(80, dr), plugin); // 4s
        }
    });
}


            // 14. CRYSTAL — Crystal Shield
            case CRYSTAL -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, ticks(200, dr), 4));
                crystalAura(p);
                w.spawnParticle(Particle.END_ROD, loc, 60, 1.2, 1.8, 1.2, 0.06);
                w.spawnParticle(Particle.DUST,    loc, 40, 1.0, 1.5, 1.0, 0, new Particle.DustOptions(Color.fromRGB(100,220,255), 2f));
                w.playSound(loc, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1f, 1.5f);
                w.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME,   1f, 1.2f);
                p.sendTitle("§b§l💎 CRYSTAL SHIELD", "§7Resistance V Active!", 5, 45, 10);
            }
//═══════════════════════════════════════════════════════════════════
case ECHO -> {
    // Sonic boom origin
    w.spawnParticle(Particle.SONIC_BOOM, loc, 1, 0, 0, 0, 0);
    w.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_CURSE,    0.8f, 1.8f);
    w.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT,  0.5f, 2.0f);

    // 5 expanding sonar rings
    for (int ring = 1; ring <= 5; ring++) {
        final int fr = ring;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            double r = fr * 3.5; // ring expands outward
            int    pts = 32;
            for (int i = 0; i < pts; i++) {
                double a = Math.toRadians(i * (360.0 / pts));
                // Main ring
                w.spawnParticle(Particle.END_ROD,
                    loc.clone().add(Math.cos(a)*r, 0.5, Math.sin(a)*r),
                    1, 0, 0, 0, 0);
                // Inner ring slightly smaller
                w.spawnParticle(Particle.SONIC_BOOM,
                    loc.clone().add(Math.cos(a)*(r-0.3), 0.5, Math.sin(a)*(r-0.3)),
                    1, 0, 0, 0, 0);
            }
            // Vertical ring (Y plane)
            for (int i = 0; i < 24; i++) {
                double a = Math.toRadians(i * 15);
                w.spawnParticle(Particle.END_ROD,
                    loc.clone().add(0, Math.cos(a)*r*0.4 + r*0.3, Math.sin(a)*r),
                    1, 0, 0, 0, 0);
            }
        }, ring * 4L);
    }

    // Reveal nearby players — glow + action bar
    p.getNearbyEntities(20, 20, 20).forEach(e -> {
        if (!(e instanceof LivingEntity le) || e == p) return;

        le.setGlowing(true);
        w.spawnParticle(Particle.END_ROD, le.getLocation(), 15, 0.4, 0.8, 0.4, 0.04);

        // Glow hat jaata hai baad mein
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (le.isValid()) le.setGlowing(false);
        }, ticks(140, dr));

        // Owner ko dikhao kaun reveal hua
        if (le instanceof Player ep) {
            p.sendActionBar("§3🔍 §fRevealed: §b" + ep.getName());
        }
    });

    p.sendTitle("§3§lSONAR PULSE", "§7All enemies revealed!", 5, 50, 10);
}


            // 16. RAGE — Rage Mode
            case RAGE -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE,    ticks(160, dr), 3));
                p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, ticks(100, dr), 1));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,    ticks(80,  dr), 1));
                w.spawnParticle(Particle.DUST,  loc, 100, 0.6, 1.2, 0.6, 0, new Particle.DustOptions(Color.RED, 2.5f));
                w.spawnParticle(Particle.CRIT,  loc,  60, 0.5, 1.0, 0.5, 0.14);
                w.spawnParticle(Particle.FLAME, loc,  40, 0.4, 0.8, 0.4, 0.09);
                for (int i = 0; i < 16; i++) { double a = Math.toRadians(i*22.5);
                    w.spawnParticle(Particle.DUST, loc.clone().add(Math.cos(a)*2,1,Math.sin(a)*2), 4, 0, 0.5, 0, 0,
                        new Particle.DustOptions(Color.fromRGB(220,20,20), 2f));
                }
                w.playSound(loc, Sound.ENTITY_RAVAGER_ROAR, 1f, 0.8f);
                w.playSound(loc, Sound.ENTITY_WITHER_HURT,  0.7f, 1.2f);
                p.sendTitle("§c§l⚡ RAGE MODE!", "§7+Haste III  +Strength  +Speed", 5, 50, 10);
            }

            // 17. SPIRIT — Spirit Heal
            case SPIRIT -> {
                double maxHp = p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
                p.setHealth(Math.min(p.getHealth() + 8.0 * dm, maxHp));
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, ticks(120, dr), 2));
                w.spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 100, 0.5, 1.2, 0.5, 0.1);
                w.spawnParticle(Particle.HEART,    loc,  18, 0.6, 0.6, 0.6, 0.08);
                w.spawnParticle(Particle.END_ROD,  loc,  40, 0.4, 0.9, 0.4, 0.05);
                for (int i = 0; i < 12; i++) { double a = Math.toRadians(i*30);
                    w.spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(Math.cos(a)*1.8,0.5,Math.sin(a)*1.8), 5, 0, 0.5, 0, 0.04);
                }
                w.playSound(loc, Sound.ENTITY_WITCH_CELEBRATE, 1f, 1.3f);
                w.playSound(loc, Sound.BLOCK_BEACON_AMBIENT,   1f, 1.5f);
                p.sendTitle("§a§l✦ SPIRIT HEAL", "§7Health Restored!", 5, 45, 10);
            }

            // 18. TIME — Time Slow
            case TIME -> {
                w.spawnParticle(Particle.REVERSE_PORTAL, loc, 90, 2.5, 2, 2.5, 0.12);
                w.spawnParticle(Particle.DUST,           loc, 60, 2, 2, 2, 0, new Particle.DustOptions(Color.fromRGB(150,80,255), 2f));
                for (int ring = 1; ring <= 3; ring++) { final int fr = ring;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        double r = fr * 2.5;
                        for (int i = 0; i < 12; i++) { double a = Math.toRadians(i*30);
                            w.spawnParticle(Particle.DUST, loc.clone().add(Math.cos(a)*r,0.5,Math.sin(a)*r), 3, 0, 0, 0, 0,
                                new Particle.DustOptions(Color.fromRGB(150,80,255), 1.5f));
                        }
                    }, ring * 4L);
                }
                w.playSound(loc, Sound.BLOCK_BEACON_AMBIENT, 1f, 0.2f);
                p.getNearbyEntities(10, 10, 10).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks(160, dr), 5));
                        le.addPotionEffect(new PotionEffect(PotionEffectType.WITHER,   ticks(160,  dr), 1));
                        w.spawnParticle(Particle.REVERSE_PORTAL, le.getLocation(), 20, 0.3, 0.6, 0.3, 0.06);
                    }
                });
                p.sendTitle("§d§lTIME SLOW", "§7Enemies frozen in time!", 5, 100, 10);
            }

            // 19. WARRIOR — War Cry
            case WARRIOR -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH,   ticks(160, dr), 2));
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, ticks(120, dr), 1));
                w.spawnParticle(Particle.DUST,  loc, 100, 0.7, 1.4, 0.7, 0, new Particle.DustOptions(Color.fromRGB(220,80,20), 2.5f));
                w.spawnParticle(Particle.CRIT,  loc,  50, 0.5, 1.0, 0.5, 0.12);
                w.spawnParticle(Particle.FLAME, loc,  30, 0.4, 0.8, 0.4, 0.07);
                for (int i = 0; i < 20; i++) { double a = Math.toRadians(i*18);
                    w.spawnParticle(Particle.DUST, loc.clone().add(Math.cos(a)*2.5,0.5,Math.sin(a)*2.5), 4, 0, 0.4, 0, 0,
                        new Particle.DustOptions(Color.fromRGB(220,80,20), 2f));
                }
                w.playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, 1f, 0.6f);
                w.playSound(loc, Sound.ENTITY_RAVAGER_ROAR,    0.9f, 0.8f);
                p.getNearbyEntities(8, 8, 8).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks(60, dr), 1));
                        le.damage(3.0 * dm, p);
                    }
                });
                p.sendTitle("§6§l⚔ WAR CRY!", "§7+Strength II  +Resistance", 5, 50, 10);
            }

                // 13. EARTH — Earthquake Wall
// Aim based — ground se wall uthti hai, 15 high 10 wide
// 10s baad crack sound ke saath neeche jaati hai
case EARTH -> {
    // ── Aim: ground pe raycast ────────────────────────────────────────────
    Location wallBase = null;
    Location eyeLoc   = p.getEyeLocation();
    Vector   eyeDir   = eyeLoc.getDirection().normalize();

    // Raycast — ground dhundo
    for (int i = 2; i <= 40; i++) {
        Location check = eyeLoc.clone().add(eyeDir.clone().multiply(i));
        if (check.getBlock().getType().isSolid()) {
            wallBase = check.clone().add(0, 1, 0);
            break;
        }
        // Agar neeche dekh raha hai aur block nahi mila
        Location below = check.clone().subtract(0, 1, 0);
        if (below.getBlock().getType().isSolid()) {
            wallBase = below.clone().add(0, 1, 0);
            break;
        }
    }
    if (wallBase == null) {
        wallBase = p.getLocation().clone().add(eyeDir.clone().multiply(8));
        wallBase.setY(p.getLocation().getBlockY());
    }

    final Location base    = wallBase.clone();
    // Wall direction = player ke saamne, perpendicular to aim
    final Vector   wallDir = new Vector(-eyeDir.getZ(), 0, eyeDir.getX()).normalize();
    final int      WIDTH   = 10; // blocks wide
    final int      HEIGHT  = 15; // blocks tall

    // Wall blocks track
    final java.util.List<Location> wallBlocks = new java.util.ArrayList<>();

    // ── Rise animation ────────────────────────────────────────────────────
    w.playSound(base, Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 0.3f);
    w.playSound(base, Sound.BLOCK_STONE_BREAK,        1f, 0.2f);
    w.playSound(base, Sound.ENTITY_GENERIC_EXPLODE,   0.8f, 0.4f);

    // Ground crack particles at base
    for (int i = 0; i < WIDTH; i++) {
        double offset = (i - WIDTH / 2.0) + 0.5;
        Location crackLoc = base.clone().add(
            wallDir.getX() * offset, 0, wallDir.getZ() * offset);
        w.spawnParticle(Particle.FALLING_DUST, crackLoc, 10,
            0.2, 0.1, 0.2, 0.05,
            org.bukkit.Material.DIRT.createBlockData());
        w.spawnParticle(Particle.EXPLOSION,   crackLoc, 1, 0.1, 0, 0.1, 0);
    }

    // Rise block by block — layer by layer upward
    new BukkitRunnable() {
        int layer = 0;

        @Override
        public void run() {
            if (layer >= HEIGHT) {
                cancel();
                // All blocks placed — damage nearby
                base.getWorld().getNearbyEntities(base, 8, HEIGHT, 8)
                    .forEach(e -> {
                        if (!(e instanceof LivingEntity le) || e == p) return;
                        le.damage(6.0 * dm, p);
                        Vector vel = e.getLocation().toVector()
                            .subtract(base.toVector())
                            .normalize().multiply(2.8).setY(0.9);
                        Bukkit.getScheduler().runTaskLater(plugin,
                            () -> { if (e.isValid()) e.setVelocity(vel); }, 1L);
                    });
                return;
            }

            // Place one layer
            for (int i = 0; i < WIDTH; i++) {
                double offset = (i - WIDTH / 2.0) + 0.5;
                Location blockLoc = base.clone().add(
                    wallDir.getX() * offset,
                    layer,
                    wallDir.getZ() * offset);

                org.bukkit.block.Block blk = blockLoc.getBlock();
                if (blk.getType() == org.bukkit.Material.AIR
                        || blk.getType() == org.bukkit.Material.CAVE_AIR) {

                    // Alternate stone/cobblestone for crack look
                    blk.setType(layer % 3 == 0
                        ? org.bukkit.Material.COBBLESTONE
                        : layer % 3 == 1
                            ? org.bukkit.Material.STONE
                            : org.bukkit.Material.GRAVEL);

                    wallBlocks.add(blockLoc.clone());
                }

                // Rise particles
                w.spawnParticle(Particle.FALLING_DUST, blockLoc, 5,
                    0.3, 0.1, 0.3, 0.04,
                    org.bukkit.Material.STONE.createBlockData());
            }

            // Rise sound every 3 layers
            if (layer % 3 == 0) {
                w.playSound(base, Sound.BLOCK_STONE_PLACE, 0.8f,
                    0.5f + layer * 0.03f);
            }

            layer++;
        }
    }.runTaskTimer(plugin, 0, 2); // 2 tick per layer = smooth rise

    // ── 10 seconds baad sink ─────────────────────────────────────────────
    Bukkit.getScheduler().runTaskLater(plugin, () -> {

        // Crack sound
        w.playSound(base, Sound.BLOCK_STONE_BREAK,        1.5f, 0.4f);
        w.playSound(base, Sound.ENTITY_IRON_GOLEM_ATTACK, 1f,   0.5f);
        w.playSound(base, Sound.ENTITY_GENERIC_EXPLODE,   0.8f, 0.6f);

        // Crack particles at top
        for (Location bl : wallBlocks) {
            if (bl.getY() >= base.getY() + HEIGHT - 3) {
                w.spawnParticle(Particle.FALLING_DUST, bl, 8,
                    0.3, 0.1, 0.3, 0.05,
                    org.bukkit.Material.GRAVEL.createBlockData());
                w.spawnParticle(Particle.EXPLOSION, bl, 1, 0.2, 0, 0.2, 0);
            }
        }

        // Sink animation — top to bottom
        new BukkitRunnable() {
            int layer = HEIGHT - 1;

            @Override
            public void run() {
                if (layer < 0) {
                    cancel();
                    wallBlocks.clear();
                    return;
                }

                // Remove this layer
                final int fl = layer;
                for (Location bl : wallBlocks) {
                    if ((int)(bl.getY() - base.getY()) == fl) {
                        org.bukkit.block.Block blk = bl.getBlock();
                        if (blk.getType() == org.bukkit.Material.COBBLESTONE
                                || blk.getType() == org.bukkit.Material.STONE
                                || blk.getType() == org.bukkit.Material.GRAVEL) {
                            blk.setType(org.bukkit.Material.AIR);
                            w.spawnParticle(Particle.FALLING_DUST, bl, 6,
                                0.2, 0.1, 0.2, 0.04,
                                org.bukkit.Material.GRAVEL.createBlockData());
                        }
                    }
                }

                if (layer % 3 == 0) {
                    w.playSound(base, Sound.BLOCK_GRAVEL_BREAK, 0.7f,
                        1.0f - layer * 0.02f);
                }

                layer--;
            }
        }.runTaskTimer(plugin, 0, 2);

    }, 200L); // 200 ticks = 10 seconds
}
                

            // ═══ EVENT EYES ══════════════════════════════════════════════════
// METEOR PRIMARY
case METEOR -> {
    double dmg = ecfg("METEOR", "primary-damage", 15.0);
    p.setVelocity(p.getLocation().getDirection().multiply(1.8).setY(1.5));
    w.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 0.5f);
    
    new BukkitRunnable() {
        public void run() {
            if (!p.isOnline() || p.isDead()) { cancel(); return; }
            
            // ⭐ OWNER SAFE: Hawa mein fall damage jama nahi hoga
            p.setFallDistance(0.0f); 
            
            // Epic aag ki trail jab player hawa mein hai
            w.spawnParticle(Particle.FLAME, p.getLocation(), 20, 0.5, 0.5, 0.5, 0.05);
            w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, p.getLocation(), 10, 0.3, 0.3, 0.3, 0.02);

            // Zameen par takrane ka check (falling down)
            if (p.getVelocity().getY() < -0.1 && p.isOnGround()) {
                w.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 3f, 0.5f);
                w.spawnParticle(Particle.EXPLOSION_EMITTER, p.getLocation(), 4);
                w.spawnParticle(Particle.LAVA, p.getLocation(), 80, 3, 1, 3, 0.5);
                
                // Shockwave particles
                for(int i = 0; i < 360; i+=10) {
                    Location circle = p.getLocation().add(Math.cos(Math.toRadians(i))*4, 0.2, Math.sin(Math.toRadians(i))*4);
                    w.spawnParticle(Particle.FLAME, circle, 2, 0, 0, 0, 0.2);
                }
                
                applySafeDamage(p, p.getLocation(), 7.0, dmg);
                
                // Dushmano ko hawa mein uchhalna (Unhe girne par fall damage padega!)
                p.getLocation().getWorld().getNearbyEntities(p.getLocation(), 7, 7, 7).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        Vector push = le.getLocation().toVector().subtract(p.getLocation().toVector()).normalize();
                        le.setVelocity(push.multiply(2.5).setY(1.2)); // Huge knockback
                    }
                });
                cancel();
            }
        }
    }.runTaskTimer(plugin, 5, 1);
}


case MIRAGE -> {
    // === Initial Activation Sound ===
    w.playSound(p.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.5f, 0.7f);
    w.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 0.5f);

    // === Aim-Based Placement (10 blocks aage player ke look direction mein) ===
    Vector dir = p.getLocation().getDirection().clone();
    dir.setY(0).normalize();
    final Location base = p.getLocation().clone().add(dir.multiply(10));
    base.setY(p.getLocation().getY());

    new BukkitRunnable() {
        int ticks = 0;
        double spin = 0;

        @Override
        public void run() {
            if (!p.isOnline() || p.isDead()) { cancel(); return; }

            // === 10 Seconds Baad: Explosion + Cleanup ===
            if (ticks >= 200) {
                w.playSound(base, Sound.ENTITY_GENERIC_EXPLODE, 3f, 0.3f);
                w.playSound(base, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 2f, 0.5f);
                w.spawnParticle(Particle.EXPLOSION_LARGE, base, 15, 3, 1, 3, 0);

                // 15 TNT equivalent - center + 6 surrounding
                base.getWorld().createExplosion(base.clone().add(0, 0.5, 0), 15f, false, false);
                for (int i = 0; i < 6; i++) {
                    double a = i * (Math.PI / 3);
                    base.getWorld().createExplosion(
                        base.clone().add(Math.cos(a) * 4, 0, Math.sin(a) * 4),
                        7f, false, false
                    );
                }
                cancel();
                return;
            }

            spin += 0.05;
            final double TOP = 18.0;

            // ========== TOP LARGE MAGIC CIRCLE (2nd photo jaisa bada circle upar) ==========
            Location top = base.clone().add(0, TOP, 0);

            // 4 concentric rings - outer se inner, opposite directions
            ring(w, top, 7.0,  90, spin);
            ring(w, top, 5.5,  70, -spin * 1.3);
            ring(w, top, 4.0,  55,  spin * 1.8);
            ring(w, top, 2.5,  35, -spin * 2.5);

            // 6 satellite circles on outer ring (photo mein jo chote circles hain)
            for (int i = 0; i < 6; i++) {
                double a = (Math.PI * 2.0 / 6) * i + spin;
                Location sc = top.clone().add(Math.cos(a) * 6.0, 0, Math.sin(a) * 6.0);
                ring(w, sc, 1.2, 22, -spin * 2);
                // Star inside each small circle
                for (int s = 0; s < 5; s++) {
                    double sa = (Math.PI * 2.0 / 5) * s - spin;
                    drawLine(w,
                        sc.clone().add(Math.cos(sa) * 1.0, 0, Math.sin(sa) * 1.0),
                        sc.clone().add(Math.cos(sa + Math.PI * 2.0 / 5 * 2) * 1.0, 0, Math.sin(sa + Math.PI * 2.0 / 5 * 2) * 1.0),
                        8);
                }
            }

            // Hexagram (Star of David lines) inside top circle
            for (int i = 0; i < 6; i++) {
                double a1 = (Math.PI * 2.0 / 6) * i + spin;
                double a2 = (Math.PI * 2.0 / 6) * ((i + 2) % 6) + spin;
                drawLine(w,
                    top.clone().add(Math.cos(a1) * 4.5, 0, Math.sin(a1) * 4.5),
                    top.clone().add(Math.cos(a2) * 4.5, 0, Math.sin(a2) * 4.5),
                    18);
            }

            // ========== MIDDLE RINGS (top se bottom tak descending - 2nd photo ka middle part) ==========
            double[][] midData = {{13.0, 5.0}, {10.0, 3.5}, {7.0, 2.8}, {4.5, 4.5}};
            for (int r = 0; r < midData.length; r++) {
                Location ml = base.clone().add(0, midData[r][0], 0);
                double ms = (r % 2 == 0) ? spin * 1.5 : -spin * 1.5;
                ring(w, ml, midData[r][1],        45, ms);
                ring(w, ml, midData[r][1] * 0.65, 30, -ms);
                // Cross lines inside each mid ring
                for (int i = 0; i < 4; i++) {
                    double a = (Math.PI / 2.0) * i + ms;
                    drawLine(w,
                        ml.clone().add(Math.cos(a) * midData[r][1], 0, Math.sin(a) * midData[r][1]),
                        ml.clone().add(Math.cos(a + Math.PI) * midData[r][1], 0, Math.sin(a + Math.PI) * midData[r][1]),
                        12);
                }
            }

            // ========== BOTTOM CIRCLE (ground pe - photo mein neeche wala bada circle) ==========
            Location bot = base.clone().add(0, 0.2, 0);
            ring(w, bot, 6.5, 80, -spin);
            ring(w, bot, 5.0, 65,  spin * 1.2);
            ring(w, bot, 3.5, 48, -spin * 1.8);
            ring(w, bot, 2.0, 32,  spin * 2.5);

            // Bottom satellite circles (5 chote circles neeche)
            for (int i = 0; i < 5; i++) {
                double a = (Math.PI * 2.0 / 5) * i - spin;
                Location sc = bot.clone().add(Math.cos(a) * 5.2, 0, Math.sin(a) * 5.2);
                ring(w, sc, 1.0, 18, spin * 2);
            }
            // Hexagram lines in bottom circle
            for (int i = 0; i < 6; i++) {
                double a1 = (Math.PI * 2.0 / 6) * i - spin;
                double a2 = (Math.PI * 2.0 / 6) * ((i + 2) % 6) - spin;
                drawLine(w,
                    bot.clone().add(Math.cos(a1) * 4.0, 0, Math.sin(a1) * 4.0),
                    bot.clone().add(Math.cos(a2) * 4.0, 0, Math.sin(a2) * 4.0),
                    14);
            }

            // ========== YELLOW LASER (top se ground tak — MOTA) ==========
            for (double y = 0.2; y <= TOP; y += 0.18) {
                Location lp = base.clone().add(0, y, 0);
                // Core bright yellow
                w.spawnParticle(Particle.DUST, lp, 3, 0.08, 0, 0.08, 0,
                    new Particle.DustOptions(Color.fromRGB(255, 255, 0), 2.8f));
                // Outer orange glow
                w.spawnParticle(Particle.DUST, lp, 2, 0.18, 0, 0.18, 0,
                    new Particle.DustOptions(Color.fromRGB(255, 210, 30), 3.8f));
            }
            // Laser sparkle particles
            w.spawnParticle(Particle.END_ROD, base.clone().add(0, TOP / 2.0, 0),
                5, 0.25, TOP / 2.0 * 0.85, 0.25, 0.004);

            // ========== DAMAGE NEARBY ENEMIES (30 block radius, 2 hearts = 4 HP, har 0.5s) ==========
            if (ticks % 10 == 0) {
                for (Entity e : base.getNearbyEntities(30, 30, 30)) {
                    if (!(e instanceof LivingEntity victim)) continue;
                    if (e.equals(p)) continue; // Owner ko kuch nahi hoga
                    if (e instanceof Player ep) {
                        if (ep.getGameMode() == GameMode.CREATIVE ||
                            ep.getGameMode() == GameMode.SPECTATOR) continue;
                    }
                    victim.damage(4.0, p);
                    e.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR,
                        victim.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0);
                }
            }

            // Ambient sounds
            if (ticks % 20 == 0) w.playSound(base, Sound.BLOCK_BEACON_AMBIENT, 1f, 0.3f);
            if (ticks % 7  == 0) w.playSound(base, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.25f, 2f);

            ticks++;
        }

        // === Helper: Circle/Ring draw karo ===
        private void ring(World world, Location center, double radius, int pts, double offset) {
            for (int i = 0; i < pts; i++) {
                double a = (Math.PI * 2.0 / pts) * i + offset;
                Location pt = center.clone().add(Math.cos(a) * radius, 0, Math.sin(a) * radius);
                world.spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(255, 50, 0), 1.8f));
            }
        }

        // === Helper: Line draw karo do points ke beech ===
        private void drawLine(World world, Location from, Location to, int steps) {
            for (int i = 0; i <= steps; i++) {
                double t = (double) i / steps;
                Location pt = from.clone().add(
                    (to.getX() - from.getX()) * t,
                    (to.getY() - from.getY()) * t,
                    (to.getZ() - from.getZ()) * t
                );
                world.spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(200, 40, 0), 1.5f));
            }
        }
    }.runTaskTimer(plugin, 0, 1);
}

// OCEAN PRIMARY
case OCEAN -> {
    double dmg = ecfg("OCEAN", "primary-damage", 12.0);
    w.playSound(p.getLocation(), Sound.ENTITY_DOLPHIN_SPLASH, 2f, 0.5f);
    
    new BukkitRunnable() {
        int ticks = 0;
        Location wave = p.getLocation().clone().add(0, 0.5, 0);
        Vector dir = p.getLocation().getDirection().setY(0).normalize().multiply(1.2); // Wave speed
        
        public void run() {
            if (ticks++ >= 15 || !p.isOnline()) { cancel(); return; } // Range
            
            wave.add(dir); // Wave aage badh rahi hai
            
            // Wall of Water (Tsunami animation)
            for(double x = -2; x <= 2; x+=0.5) {
                for(double y = 0; y <= 2; y+=0.5) {
                    Vector side = new Vector(-dir.getZ(), 0, dir.getX()).normalize().multiply(x);
                    Location partLoc = wave.clone().add(side).add(0, y, 0);
                    w.spawnParticle(Particle.DRIPPING_WATER, partLoc, 5, 0.1, 0.1, 0.1, 0.5);
                    w.spawnParticle(Particle.BUBBLE_POP, partLoc, 2, 0.1, 0.1, 0.1, 0.1);
                }
            }
            
            w.playSound(wave, Sound.BLOCK_WATER_AMBIENT, 0.5f, 1.5f);
            applySafeDamage(p, wave, 3.0, dmg);
            
            // Push enemies along with the wave
            wave.getWorld().getNearbyEntities(wave, 3, 3, 3).forEach(e -> {
                if (e instanceof LivingEntity le && e != p) {
                    le.setVelocity(dir.clone().multiply(1.5).setY(0.4));
                }
            });
        }
    }.runTaskTimer(plugin, 0, 1);
}


// ECLIPSE PRIMARY — FIXED
case ECLIPSE -> {
    double dmg = ecfg("ECLIPSE", "primary-damage", 18.0);
    LivingEntity target = aim(p, 30.0);

    if (target == null) {
        p.sendMessage("§cNo target found!");
        return;
    }

    w.playSound(p.getLocation(),      Sound.ENTITY_WITHER_SHOOT,   1f, 0.5f);
    w.playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_STARE, 0.8f, 0.5f);

    // FIX: boolean flag — blast sirf ek baar hoga
    final boolean[] blasted = {false};

    new BukkitRunnable() {
        // FIX: ticks++ instead of ticks += 2
        // runTaskTimer 2L interval — 30 ticks * 2 = 60 ticks = 3 seconds
        int    ticks     = 0;
        double cubeAngle = 0;

        public void run() {

            // FIX: target dead/invalid — blast karo phir cancel
            if (!target.isValid() || target.isDead()) {
                if (!blasted[0]) doEclipseBlast(p, w, target.getLocation().clone().add(0,1,0), dmg, plugin);
                blasted[0] = true;
                cancel();
                return;
            }

            ticks++;

            // ── Phase 1: BLACK CUBE (0-29 ticks * 2L = 3s) ───────────────
            if (ticks <= 30) {
                cubeAngle += 0.12;
                Location body = target.getLocation().clone().add(0, 1.0, 0);

                double r    = 0.75 + Math.sin(ticks * 0.3) * 0.08;
                double cosA = Math.cos(cubeAngle), sinA = Math.sin(cubeAngle);
                double cosT = Math.cos(0.4),       sinT = Math.sin(0.4);

                double[][] verts = {
                    {-r,-r,-r},{r,-r,-r},{r,r,-r},{-r,r,-r},
                    {-r,-r, r},{r,-r, r},{r,r, r},{-r,r, r}
                };
                int[][] edges = {
                    {0,1},{1,2},{2,3},{3,0},
                    {4,5},{5,6},{6,7},{7,4},
                    {0,4},{1,5},{2,6},{3,7}
                };

                double[][] rv = new double[8][3];
                for (int v = 0; v < 8; v++) {
                    double x = verts[v][0], y = verts[v][1], z = verts[v][2];
                    double nx  = x*cosA - z*sinA;
                    double nz  = x*sinA + z*cosA;
                    double ny2 = y*cosT - nz*sinT;
                    double nz2 = y*sinT + nz*cosT;
                    rv[v][0]=nx; rv[v][1]=ny2+r; rv[v][2]=nz2;
                }

                Particle.DustOptions black = new Particle.DustOptions(
                    org.bukkit.Color.fromRGB(10, 0, 20), 1.1f);
                for (int[] edge : edges) {
                    double[] va = rv[edge[0]], vb = rv[edge[1]];
                    for (int k = 0; k <= 5; k++) {
                        double t = (double)k/5;
                        Location pt = body.clone().add(
                            va[0]+(vb[0]-va[0])*t,
                            va[1]+(vb[1]-va[1])*t - r,
                            va[2]+(vb[2]-va[2])*t);
                        w.spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0, black);
                    }
                }

                w.spawnParticle(Particle.SQUID_INK,    body, 2, 0.1, 0.1, 0.1, 0.01);
                w.spawnParticle(Particle.DRAGON_BREATH, body, 3, 0.15, 0.15, 0.15, 0.02);

                // Warning sound — intensifies
                if (ticks % 10 == 0) {
                    float pitch = 0.4f + (ticks / 30f) * 1.2f;
                    w.playSound(target.getLocation(),
                        Sound.ENTITY_ENDERMAN_AMBIENT, 0.6f, pitch);
                }

            } else {
                // ── Phase 2: BLAST ─────────────────────────────────────
                if (!blasted[0]) {
                    blasted[0] = true;
                    doEclipseBlast(p, w, target.getLocation().clone().add(0,1,0), dmg, plugin);
                  }
                 cancel();
            }
        }
    }.runTaskTimer(plugin, 0, 2); // 2 tick interval
}


            /// GUARDIAN PRIMARY
case GUARDIAN -> {
    w.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 2f, 1.2f);
    
    new BukkitRunnable() {
        int ticks = 0;
        public void run() {
            if (ticks++ >= 120 || !p.isOnline() || p.isDead()) { 
                w.playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 2f, 1.2f);
                cancel(); return; 
            } // 6 seconds dome
            
            // Beautiful Half-Sphere (Dome) Animation
            for (double t = 0; t <= Math.PI / 2; t += Math.PI / 10) { // Math.PI/2 makes it a dome, not full sphere
                for (double r = 0; r <= 2 * Math.PI; r += Math.PI / 10) {
                    double x = 5.0 * Math.sin(t) * Math.cos(r);
                    double y = 5.0 * Math.cos(t); // Top half
                    double z = 5.0 * Math.sin(t) * Math.sin(r);
                    w.spawnParticle(Particle.ENCHANT, p.getLocation().clone().add(x, y, z), 1, 0,0,0,0);
                }
            }
            
            // Protection to allies and self
            p.getWorld().getNearbyEntities(p.getLocation(), 5, 5, 5).forEach(e -> {
                if (e instanceof Player ally) {
                    ally.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 1));
                 }
             });
         }
      }.runTaskTimer(plugin, 0, 3);
   }
        }  // end switch (activatePrimary)
        p.playSound(loc, Sound.ENTITY_ENDER_EYE_DEATH, 0.8f, 1f);
    }  // end activatePrimary



    // ══════════════════════════════════════════════════════════════════════
    //  SECONDARY  (SHIFT + Q)
    // ══════════════════════════════════════════════════════════════════════
    public void activateSecondary(Player p, EyeType eye) {
        if (plugin.getCooldownManager().isOnCooldown(p, "S")) return;
        plugin.getCooldownManager().setCooldown(p, "S", getCd(p, eye, "secondary"));
        Location loc = p.getLocation().clone();
        Vector   dir = p.getEyeLocation().getDirection().normalize();
        double   dm  = getDmg(p);
        double   dr  = getDur(p);
        World    w   = p.getWorld();

        switch (eye) {

            // 1. VOID — Void Trap (3 pull waves)  [knockback 1 tick after damage]
            case VOID -> {
                w.spawnParticle(Particle.PORTAL,         loc, 120, 2.5, 2.5, 2.5, 0.18);
                w.spawnParticle(Particle.REVERSE_PORTAL, loc,  80, 2.0, 2.0, 2.0, 0.12);
                w.spawnParticle(Particle.DRAGON_BREATH,  loc,  50, 1.5, 1.5, 1.5, 0.06);
                w.playSound(loc, Sound.BLOCK_BEACON_AMBIENT,   1f, 0.3f);
                w.playSound(loc, Sound.ENTITY_ENDERMAN_SCREAM, 0.6f, 0.4f);
                for (int wave = 0; wave < 3; wave++) {
                    Bukkit.getScheduler().runTaskLater(plugin, () ->
                        p.getNearbyEntities(12, 12, 12).forEach(e -> {
                            if (e instanceof LivingEntity le && e != p) {
                                Vector vel = loc.toVector().subtract(e.getLocation().toVector()).normalize().multiply(2.2);
                                le.damage(2.5 * dm, p);
                                Bukkit.getScheduler().runTaskLater(plugin, () -> { if (e.isValid()) e.setVelocity(vel); }, 1L);
                            }
                        }), wave * 10L);
                }
            }

            // 2. PHANTOM — Phantom Reveal
            case PHANTOM -> {
                w.spawnParticle(Particle.END_ROD,    loc, 100, 4, 4, 4, 0.09);
                w.spawnParticle(Particle.SONIC_BOOM, loc,   1, 0, 0, 0, 0);
                w.playSound(loc, Sound.ENTITY_PHANTOM_BITE, 0.8f, 2.0f);
                p.getNearbyEntities(30, 30, 30).forEach(e -> {
                    if (e instanceof Player t && e != p) {
                        t.removePotionEffect(PotionEffectType.INVISIBILITY);
                        t.setGlowing(true);
                        w.spawnParticle(Particle.END_ROD, t.getLocation(), 25, 0.4, 0.9, 0.4, 0.04);
                        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (t.isOnline()) t.setGlowing(false); }, ticks(160, dr));
                    }
                });
                p.sendTitle("§b§lPHANTOM REVEAL", "§7All invisible players exposed!", 5, 50, 10);
            }

            case STORM -> {
    double qDmg = ecfg("STORM", "secondary-damage", 10.0);
    w.playSound(p.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 2f, 0.5f);
    
    new BukkitRunnable() {
        double radius = 1.0;
        public void run() {
            if (radius > 8.0 || !p.isOnline()) { cancel(); return; }

            // Blue Electric Ring Animation
            for (double a = 0; a < Math.PI * 2; a += Math.PI / (radius * 3)) {
                double x = Math.cos(a) * radius;
                double z = Math.sin(a) * radius;
                Location ring = p.getLocation().clone().add(x, 0.2, z);
                w.spawnParticle(Particle.ELECTRIC_SPARK, ring, 1, 0, 0, 0, 0);
                if (radius > 4) w.spawnParticle(Particle.CLOUD, ring, 1, 0, 0, 0, 0.02);
            }

            // Damage aur Knockback (Owner is Safe)
            applySafeDamage(p, p.getLocation(), radius, qDmg);
            
            p.getWorld().getNearbyEntities(p.getLocation(), radius, 2, radius).forEach(e -> {
                if (e instanceof LivingEntity le && e != p) {
                    Vector push = le.getLocation().toVector().subtract(p.getLocation().toVector()).normalize();
                    le.setVelocity(push.multiply(1.8).setY(0.5));
                }
            });

            radius += 1.5;
        }
    }.runTaskTimer(plugin, 0, 1);
            }  // end case STORM


// ════════════════════════════════════════════════════════════════════
case FROST -> {
    w.spawnParticle(Particle.SNOWFLAKE, loc, 100, 2.5, 0.5, 2.5, 0.1);
    w.spawnParticle(Particle.WHITE_ASH, loc,  60, 2.0, 0.3, 2.0, 0.05);
    w.playSound(loc, Sound.ENTITY_PLAYER_HURT_FREEZE, 1f, 0.3f);
    w.playSound(loc, Sound.BLOCK_GLASS_BREAK,         1f, 0.4f);

    List<Location> placedBlocks = new ArrayList<>();

    // 4 sides: North, South, East, West
    // direction = spike points outward this way
    // perp = spread along this side
    double[][] dirs  = {{ 1, 0},{ -1, 0},{ 0, 1},{ 0,-1}};
    double[][] perps = {{ 0, 1},{  0,-1},{ 1, 0},{-1, 0}};

    double angleRad = Math.toRadians(40); // 40 degrees from ground
    double cosA = Math.cos(angleRad);     // horizontal component
    double sinA = Math.sin(angleRad);     // vertical component

    // Spawn spikes with slight delay per side for cinematic effect
    for (int d = 0; d < 4; d++) {
        final int fd = d;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            double dx = dirs[fd][0],  dz = dirs[fd][1];
            double px = perps[fd][0], pz = perps[fd][1];

            // 5 spikes per side, spread 3 blocks apart (-6,-3,0,3,6)
            for (int s = 0; s < 5; s++) {
                double spread = (s - 2) * 3.0;

                // Base of spike — 10 blocks from center
                double baseX = loc.getX() + dx * 10 + px * spread;
                double baseY = loc.getY();
                double baseZ = loc.getZ() + dz * 10 + pz * spread;

                // Spike height: 25 blocks along axis, tapering
                int spikeLen = 18 + (s == 2 ? 7 : (s == 1 || s == 3 ? 4 : 0)); // center tallest
                for (int h = 0; h < spikeLen; h++) {
                    // Position along 40° spike
                    double outward = h * cosA; // outward offset along ground plane
                    double upward  = h * sinA; // upward offset

                    // Width tapers: base=2, mid=1, tip=0
                    int halfW = h < spikeLen/3 ? 1 : 0;

                    for (int ww = -halfW; ww <= halfW; ww++) {
                        int bx = (int) Math.round(baseX + dx*outward + px*ww);
                        int by = (int) Math.round(baseY + upward);
                        int bz = (int) Math.round(baseZ + dz*outward + pz*ww);

                        Location bl = new Location(w, bx, by, bz);
                        org.bukkit.block.Block blk = bl.getBlock();
                        Material mat = blk.getType();

                        // Only place in air/non-solid — no griefing
                        if (mat == Material.AIR
                                || mat == Material.CAVE_AIR
                                || mat == Material.VOID_AIR) {
                            // Alternate BLUE_ICE and PACKED_ICE for crystal look
                            blk.setType(h % 3 == 0 ? Material.BLUE_ICE : Material.PACKED_ICE);
                            synchronized (placedBlocks) { placedBlocks.add(bl); }
                        }
                    }
                }

                // Ice spawn particles at base
                Location base = new Location(w, baseX, baseY, baseZ);
                w.spawnParticle(Particle.SNOWFLAKE, base, 20, 0.3, 0.2, 0.3, 0.04);
                w.spawnParticle(Particle.WHITE_ASH, base, 10, 0.2, 0.3, 0.2, 0.02);
            }

            // Sound per side
            w.playSound(new Location(w,
                loc.getX() + dx*10,
                loc.getY(),
                loc.getZ() + dz*10),
                Sound.BLOCK_GLASS_BREAK, 1f, 0.5f);
            w.playSound(new Location(w,
                loc.getX() + dx*10,
                loc.getY(),
                loc.getZ() + dz*10),
                Sound.ENTITY_PLAYER_HURT_FREEZE, 1f, 0.6f);

        }, fd * 3L); // each side 3 ticks apart — wave effect
    }

    // Center circle effect
    for (int ring = 1; ring <= 4; ring++) { final int fr = ring;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            double r = fr * 2.5;
            for (int i = 0; i < 24; i++) {
                double a = Math.toRadians(i * 15);
                w.spawnParticle(Particle.SNOWFLAKE,
                    loc.clone().add(Math.cos(a)*r, 0.1, Math.sin(a)*r),
                    2, 0, 0, 0, 0.02);
            }
        }, ring * 3L);
    }

    // ── DAMAGE TASK — 6 seconds, enemies inside freeze ───────────────
    new BukkitRunnable() {
        int elapsed = 0;
        public void run() {
            if (elapsed >= 6) { cancel(); return; }
            elapsed++;

            // All enemies within 12 block radius (covers all spikes)
            p.getNearbyEntities(12, 12, 12).forEach(e -> {
                if (!(e instanceof LivingEntity le) || e == p) return;
                le.addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOWNESS, 25, 5, false, false));
                le.damage(1.5 * dm, p);
                // Ice particle on enemy
                w.spawnParticle(Particle.SNOWFLAKE, le.getLocation(),
                    8, 0.3, 0.6, 0.3, 0.02);
            });
        }
    }.runTaskTimer(plugin, 20L, 20L); // every second for 6s

    // ── REMOVE BLOCKS after 6 seconds ────────────────────────────────
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
        // Break all placed blocks
        for (Location bl : placedBlocks) {
            if (bl.getBlock().getType() == Material.BLUE_ICE
                    || bl.getBlock().getType() == Material.PACKED_ICE) {
                bl.getBlock().setType(Material.AIR);
                // Particle at each block removal
                w.spawnParticle(Particle.SNOWFLAKE, bl, 3, 0.2, 0.2, 0.2, 0.02);
            }
        }
        placedBlocks.clear();

        // Break sounds — 4 sides
        double[][] dirsF = {{1,0},{-1,0},{0,1},{0,-1}};
        for (double[] df : dirsF) {
            Location sLoc = loc.clone().add(df[0]*10, 0, df[1]*10);
            w.playSound(sLoc, Sound.BLOCK_GLASS_BREAK,           1.5f, 0.6f);
            w.playSound(sLoc, Sound.ENTITY_PLAYER_HURT_FREEZE,   1f,   1.5f);
            w.spawnParticle(Particle.SNOWFLAKE, sLoc, 40, 1, 1, 1, 0.06);
            w.spawnParticle(Particle.WHITE_ASH, sLoc, 30, 1, 1, 1, 0.04);
        }
    }, 120L); // 120 ticks = 6 seconds
}

            // 5. FLAME — Flame Burst  [NO createExplosion — prevents owner self-damage]
            case FLAME -> {
                w.spawnParticle(Particle.EXPLOSION, loc,   4, 0.7, 0, 0.7, 0);
                w.spawnParticle(Particle.FLAME,     loc, 120, 2.8, 0.7, 2.8, 0.2);
                w.spawnParticle(Particle.LAVA,      loc,  35, 2.2, 0.3, 2.2, 0.14);
                for (int i = 0; i < 24; i++) { double a = Math.toRadians(i*15);
                    for (int h = 0; h <= 3; h++)
                        w.spawnParticle(Particle.FLAME, loc.clone().add(Math.cos(a)*3.5,h*0.5,Math.sin(a)*3.5), 3, 0, 0.2, 0, 0.04);
                }
                w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.9f);
                w.playSound(loc, Sound.ENTITY_BLAZE_SHOOT,     1f, 0.6f);
                p.getNearbyEntities(7, 7, 7).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        Vector vel = e.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(2.5).setY(0.8);
                        le.damage(8.0 * dm, p);
                        le.setFireTicks(ticks(140, dr));
                        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (e.isValid()) e.setVelocity(vel); }, 1L);
                    }
                });
            }

            // 6. SHADOW — Shadow Cloak
            case SHADOW -> {
    p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, ticks(200, dr), 0));
    p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION,  ticks(300, dr), 0));
    p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,         ticks(160, dr), 1));

    w.spawnParticle(Particle.SMOKE, loc, 80, 0.5, 1.2, 0.5, 0.09);
    w.spawnParticle(Particle.DUST,  loc, 60, 0.4, 0.9, 0.4, 0,
        new Particle.DustOptions(Color.BLACK, 2f));
    w.spawnParticle(Particle.PORTAL, loc, 40, 0.3, 0.7, 0.3, 0.07);
    for (int i = 0; i < 20; i++) {
        double a = Math.toRadians(i * 18);
        w.spawnParticle(Particle.SMOKE,
            loc.clone().add(Math.cos(a)*0.8, i*0.1, Math.sin(a)*0.8),
            3, 0, 0, 0, 0.02);
    }
    w.playSound(loc, Sound.ENTITY_ENDERMAN_STARE, 0.6f, 1.3f);
    p.sendTitle("\u00a78\u00a7lSHADOW CLOAK", "\u00a77You vanish into darkness...", 5, 50, 10);

    org.bukkit.inventory.PlayerInventory inv = p.getInventory();

    // ── Step 1: Store armor + offhand with their EXACT slots ─────────
    final ItemStack h = inv.getHelmet()     != null ? inv.getHelmet().clone()     : null;
    final ItemStack c = inv.getChestplate() != null ? inv.getChestplate().clone() : null;
    final ItemStack l = inv.getLeggings()   != null ? inv.getLeggings().clone()   : null;
    final ItemStack b = inv.getBoots()      != null ? inv.getBoots().clone()      : null;
    final ItemStack oh = inv.getItemInOffHand().getType() != Material.AIR
                       ? inv.getItemInOffHand().clone() : null;

    // ── Step 2: Remove armor + offhand ───────────────────────────────
    inv.setHelmet(null);
    inv.setChestplate(null);
    inv.setLeggings(null);
    inv.setBoots(null);
    inv.setItemInOffHand(null);

    // ── Step 3: 10s baad wapas do — slot-aware ───────────────────────
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
        if (!p.isOnline()) return;

        org.bukkit.inventory.PlayerInventory i2 = p.getInventory();

        // Armor slots return karo — agar kuch aur aa gaya toh drop karo
        restoreOrDrop(p, h,  () -> i2.setHelmet(h),     i2.getHelmet());
        restoreOrDrop(p, c,  () -> i2.setChestplate(c), i2.getChestplate());
        restoreOrDrop(p, l,  () -> i2.setLeggings(l),   i2.getLeggings());
        restoreOrDrop(p, b,  () -> i2.setBoots(b),      i2.getBoots());
        restoreOrDrop(p, oh, () -> i2.setItemInOffHand(oh), i2.getItemInOffHand());

        // Un-cloak effect
        w.spawnParticle(Particle.SMOKE,  p.getLocation(), 60, 0.5, 1, 0.5, 0.08);
        w.spawnParticle(Particle.PORTAL, p.getLocation(), 30, 0.3, 0.7, 0.3, 0.06);
        w.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.8f);
        p.sendTitle("\u00a77Shadow Cloak", "\u00a78Faded...", 5, 30, 10);

    }, ticks(200, dr));
}

            // 7. TITAN — Titan Strength
            case TITAN -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH,   ticks(120, dr), 2));
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,  ticks(60, dr), 1));
                w.spawnParticle(Particle.DUST, loc, 80, 0.7, 1.4, 0.7, 0, new Particle.DustOptions(Color.fromRGB(180,90,20), 3f));
                w.spawnParticle(Particle.CRIT, loc, 40, 0.5, 1.0, 0.5, 0.12);
                w.spawnParticle(Particle.EXPLOSION, loc, 2, 0.4, 0, 0.4, 0);
                for (int ring = 1; ring <= 3; ring++) { final int fr = ring;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        for (int i = 0; i < 16; i++) { double a = Math.toRadians(i*22.5);
                            w.spawnParticle(Particle.DUST, loc.clone().add(Math.cos(a)*(fr*0.8),1,Math.sin(a)*(fr*0.8)), 3, 0, 0.5, 0, 0,
                                new Particle.DustOptions(Color.fromRGB(180,90,20), 2f));
                        }
                    }, fr * 3L);
                }
                w.playSound(loc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 0.6f);
                w.playSound(loc, Sound.ENTITY_RAVAGER_ROAR,      0.8f, 0.8f);
                p.sendTitle("§6§l💪 TITAN STRENGTH!", "§7+Strength III active!", 5, 45, 10);
            }

            
// ── HUNTER SECONDARY — Mark Target ───────────────────────────────────────────
// Target mark hoga — 10s tak extra damage milega
// Mob aur player dono par kaam karega
case HUNTER -> {
    LivingEntity tgt = aim(p, 30);
    if (tgt != null) {

        // Mark particles + sound
        w.spawnParticle(Particle.CRIT,     tgt.getLocation(), 50, 0.5, 1.0, 0.5, 0.12);
        w.spawnParticle(Particle.END_ROD,  tgt.getLocation(), 25, 0.4, 0.8, 0.4, 0.06);
        w.spawnParticle(Particle.SOUL,     tgt.getLocation(), 20, 0.3, 0.7, 0.3, 0.05);

        // Red ring animation
        for (int ring = 1; ring <= 3; ring++) { final int fr = ring;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                double r = fr * 0.6;
                for (int i = 0; i < 12; i++) {
                    double a = Math.toRadians(i * 30);
                    Particle.DustOptions red = new Particle.DustOptions(
                        org.bukkit.Color.fromRGB(220, 20, 20), 1.5f);
                    w.spawnParticle(Particle.DUST,
                        tgt.getLocation().clone().add(Math.cos(a)*r, 1.0, Math.sin(a)*r),
                        1, 0, 0, 0, 0, red);
                }
            }, ring * 3L);
        }

        w.playSound(loc, Sound.ENTITY_ARROW_HIT_PLAYER,   1f, 0.8f);
        w.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1.2f);

        // Mark metadata
        tgt.setMetadata("hunterMarked",
            new org.bukkit.metadata.FixedMetadataValue(plugin, p.getUniqueId().toString()));
        tgt.setGlowing(true);

        p.sendTitle("§c§lMARKED!", "§7Target takes extra damage!", 5, 50, 10);

        // Mark aura — 10 seconds tak
        new BukkitRunnable() {
            int ticks = 0;
            double angle = 0;
            public void run() {
                if (ticks++ >= ticks(200, dr) || !tgt.isValid() || tgt.isDead()) {
                    // Remove mark
                    tgt.removeMetadata("hunterMarked", plugin);
                    if (tgt.isValid()) tgt.setGlowing(false);
                    cancel(); return;
                }

                angle += 0.15;

                // Rotating red ring around target
                for (int i = 0; i < 8; i++) {
                    double a = angle + Math.toRadians(i * 45);
                    Particle.DustOptions red = new Particle.DustOptions(
                        org.bukkit.Color.fromRGB(220, 20, 20), 1.2f);
                    w.spawnParticle(Particle.DUST,
                        tgt.getLocation().clone().add(Math.cos(a)*0.7, 1.0, Math.sin(a)*0.7),
                        1, 0, 0, 0, 0, red);
                }
                // Top indicator
                w.spawnParticle(Particle.CRIT,
                    tgt.getLocation().clone().add(0, tgt.getHeight() + 0.3, 0),
                    1, 0.1, 0, 0.1, 0.02);
            }
        }.runTaskTimer(plugin, 0, 1);

    } else {
        p.sendMessage("§7No target in range.");
    }
}

            // 9. GRAVITY — Gravity Jump
            // [FIX] Y velocity = 8.0 (was 3.5), slow fall after 40 ticks, lasts 80 ticks
            case GRAVITY -> {
                w.spawnParticle(Particle.REVERSE_PORTAL, loc, 80, 0.8, 0.5, 0.8, 0.12);
                w.spawnParticle(Particle.PORTAL,         loc, 50, 0.5, 0.3, 0.5, 0.08);
                w.spawnParticle(Particle.EXPLOSION,      loc,  2, 0,   0,   0,   0);
                for (int i = 0; i < 16; i++) { double a = Math.toRadians(i*22.5);
                    w.spawnParticle(Particle.REVERSE_PORTAL, loc.clone().add(Math.cos(a)*1.5,0.1,Math.sin(a)*1.5), 3, 0, 0.3, 0, 0.05);
                }
                w.playSound(loc, Sound.ENTITY_GHAST_SHOOT,    0.9f, 0.4f);
                w.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 0.6f);
                p.setVelocity(new Vector(0, 8.0, 0));   // HIGH launch
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (p.isOnline())
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, ticks(80, dr), 0));
                }, 40L);  // wait until near peak before slow fall
            }

            // 10. WIND — Wind Push  [knockback 1 tick after damage]
            case WIND -> {
                w.spawnParticle(Particle.WHITE_ASH, loc, 120, 2.5, 1, 2.5, 0.22);
                w.spawnParticle(Particle.CLOUD,     loc,  70, 2.0, 0.6, 2.0, 0.10);
                w.spawnParticle(Particle.EXPLOSION, loc,   2, 0.5, 0, 0.5, 0);
                for (int ring = 1; ring <= 4; ring++) { final int fr = ring;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        for (int i = 0; i < 20; i++) { double a = Math.toRadians(i*18);
                            w.spawnParticle(Particle.WHITE_ASH, loc.clone().add(Math.cos(a)*(fr*2.5),0.5,Math.sin(a)*(fr*2.5)), 4, 0, 0.3, 0, 0.06);
                        }
                    }, fr * 3L);
                }
                w.playSound(loc, Sound.ENTITY_PHANTOM_DEATH,  1f, 0.4f);
                w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE,0.7f, 0.8f);
                p.getNearbyEntities(12, 12, 12).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        Vector vel = e.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(3.5).setY(0.7);
                        le.damage(3.0 * dm, p);
                        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (e.isValid()) e.setVelocity(vel); }, 1L);
                    }
                });
            }

            // 11. POISON — Poison Cloud
            case POISON -> {
                w.spawnParticle(Particle.ITEM_SLIME, loc, 140, 3.0, 0.8, 3.0, 0.12);
                w.spawnParticle(Particle.SNEEZE,     loc,  90, 3.0, 0.8, 3.0, 0.07);
                w.spawnParticle(Particle.DUST,       loc,  70, 2.5, 0.6, 2.5, 0, new Particle.DustOptions(Color.fromRGB(40,200,40), 1.8f));
                for (int i = 0; i < 16; i++) { double a = Math.toRadians(i*22.5);
                    w.spawnParticle(Particle.ITEM_SLIME, loc.clone().add(Math.cos(a)*4,0.8,Math.sin(a)*4), 5, 0, 0.4, 0, 0.04);
                }
                w.playSound(loc, Sound.ENTITY_SLIME_SQUISH, 1f, 0.3f);
                w.playSound(loc, Sound.ENTITY_WITCH_DRINK,  0.8f, 0.5f);
                p.getNearbyEntities(8, 8, 8).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        le.addPotionEffect(new PotionEffect(PotionEffectType.POISON,   ticks(200, dr), 2));
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks(100, dr), 1));
                        le.addPotionEffect(new PotionEffect(PotionEffectType.WITHER,   ticks(60,  dr), 0));
                        le.damage(3.0 * dm, p);
                    }
                });
            }
// LIGHT — SHIFT + Q (Light Beam) FIXED
case LIGHT -> {
    w.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 2.0f);
    w.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE,        1f,   2.0f);

    final Vector beamDir = p.getEyeLocation().getDirection().normalize();
    final double startX  = p.getEyeLocation().getX() + beamDir.getX() * 1.5;
    final double startY  = p.getEyeLocation().getY() + beamDir.getY() * 1.5;
    final double startZ  = p.getEyeLocation().getZ() + beamDir.getZ() * 1.5;

    w.spawnParticle(Particle.FLASH,   p.getEyeLocation(), 1, 0, 0, 0, 0);
    w.spawnParticle(Particle.END_ROD, p.getEyeLocation(), 20, 0.1, 0.1, 0.1, 0.05);

    new BukkitRunnable() {
        int     step = 0;
        boolean hit  = false;

        @Override
        public void run() {
            if (hit || step >= 80) {
                Location endPt = pos();
                w.spawnParticle(Particle.FLASH,   endPt, 1, 0, 0, 0, 0);
                w.spawnParticle(Particle.END_ROD, endPt, 15, 0.4, 0.4, 0.4, 0.08);
                cancel();
                return;
            }

            step++;
            Location beam = pos();

            w.spawnParticle(Particle.END_ROD,   beam, 4, 0.02, 0.02, 0.02, 0.0);
            w.spawnParticle(Particle.WHITE_ASH, beam, 2, 0.04, 0.04, 0.04, 0.01);
            if (step % 4 == 0)
                w.spawnParticle(Particle.FLASH, beam, 1, 0, 0, 0, 0);

            for (int i = 0; i < 3; i++) {
                double a = Math.toRadians(i * 120 + step * 15);
                w.spawnParticle(Particle.END_ROD,
                    beam.clone().add(Math.cos(a)*0.12, Math.sin(a)*0.12, 0),
                    1, 0, 0, 0, 0);
            }

            if (beam.getBlock().getType().isSolid()) {
                w.spawnParticle(Particle.FLASH,     beam, 1,  0,   0,   0,   0);
                w.spawnParticle(Particle.END_ROD,   beam, 25, 0.5, 0.5, 0.5, 0.1);
                w.spawnParticle(Particle.WHITE_ASH, beam, 20, 0.4, 0.4, 0.4, 0.06);
                w.playSound(beam, Sound.BLOCK_GLASS_BREAK, 1f, 2.0f);
                hit = true;
                return;
            }

            for (org.bukkit.entity.Entity e :
                    w.getNearbyEntities(beam, 0.7, 0.7, 0.7)) {
                if (!(e instanceof LivingEntity le) || e == p) continue;

                le.damage(10.0 * dm, p); // 5 hearts

                Location eLoc = le.getLocation().add(0, 1, 0);
                w.spawnParticle(Particle.FLASH,     eLoc,  1, 0,   0,   0,   0);
                w.spawnParticle(Particle.END_ROD,   eLoc, 40, 0.6, 1.0, 0.6, 0.09);
                w.spawnParticle(Particle.WHITE_ASH, eLoc, 30, 0.5, 0.8, 0.5, 0.07);
                w.playSound(beam, Sound.ENTITY_PLAYER_LEVELUP,        0.8f, 2.0f);
                w.playSound(beam, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.6f, 2.0f);

                if (le instanceof Player ep && ep.isOnline())
                    whiteLightScreen(ep, ticks(40, dr), plugin);

                hit = true;
                return;
            }
        }        private Location pos() {
            // FIX: t value ko step ke saath multiply karo taaki beam aage badhe
            double t = (double) step * 1.2; // 0.5 se 0.8 kiya (Thoda fast aur smooth)
            return new Location(w,
                startX + (beamDir.getX() * t),
                startY + (beamDir.getY() * t),
                startZ + (beamDir.getZ() * t));
            }
          }
       }.runTaskTimer(plugin, 0, 1);
    }



            // 13. EARTH — Earth Slam  [knockback 1 tick after damage]
            case EARTH -> {
                w.spawnParticle(Particle.DUST_PLUME, loc, 160, 3, 0.2, 3, 0.26, Material.DIRT.createBlockData());
                w.spawnParticle(Particle.DUST_PLUME, loc, 100, 3, 0.2, 3, 0.26, Material.STONE.createBlockData());
                w.spawnParticle(Particle.EXPLOSION,  loc,   4, 0.9, 0, 0.9, 0);
                for (int ring = 1; ring <= 5; ring++) { final int fr = ring;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        double r = fr * 1.9;
                        for (int i = 0; i < 24; i++) { double a = Math.toRadians(i*15);
                            w.spawnParticle(Particle.DUST_PLUME, loc.clone().add(Math.cos(a)*r,0.2,Math.sin(a)*r),
                                4, 0, 0.6, 0, 0.14, Material.STONE.createBlockData());
                        }
                    }, ring * 3L);
                }
                w.playSound(loc, Sound.BLOCK_STONE_BREAK,        1f, 0.3f);
                w.playSound(loc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 0.4f);
                w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE,   0.8f, 0.5f);
                p.getNearbyEntities(10, 10, 10).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        Vector vel = e.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(2.8).setY(0.9);
                        le.damage(6.0 * dm, p);
                        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (e.isValid()) e.setVelocity(vel); }, 1L);
                    }
                });
            }

            // 14. CRYSTAL — Crystal Spikes
            case CRYSTAL -> {
                for (int i = 0; i < 16; i++) { double a = Math.toRadians(i*22.5);
                    Location sp = loc.clone().add(Math.cos(a)*4, 0, Math.sin(a)*4);
                    for (int h = 0; h <= 4; h++) {
                        w.spawnParticle(Particle.DUST, sp.clone().add(0,h*0.5,0), 3, 0.1, 0, 0.1, 0,
                            new Particle.DustOptions(Color.fromRGB(100,200,255), 2.5f));
                        w.spawnParticle(Particle.END_ROD, sp.clone().add(0,h*0.5,0), 2, 0, 0, 0, 0.01);
                    }
                }
                w.spawnParticle(Particle.END_ROD, loc, 60, 2, 0.4, 2, 0.07);
                w.playSound(loc, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1f, 0.7f);
                w.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME,  1f, 0.5f);
                p.getNearbyEntities(7, 7, 7).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        le.damage(8.0 * dm, p);
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks(70, dr), 2));
                    }
                });
            }
//═══════════════════════════════════════════════════════════════════
case ECHO -> {
    // Check aim — agar koi target aim mein hai toh focused blast
    LivingEntity tgt = aim(p, 25);

    if (tgt != null) {
        // ── FOCUSED BLAST — aim par direct sonic boom ─────────────
        w.spawnParticle(Particle.SONIC_BOOM, tgt.getLocation(), 3, 0, 0, 0, 0);
        w.spawnParticle(Particle.END_ROD,    tgt.getLocation(), 70, 1, 1.5, 1, 0.1);
        w.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1f, 0.4f);
        w.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM,    1f, 1.0f);

        // Beam line from owner to target — sonic wave path
        Location beam = p.getEyeLocation().clone();
        Vector   step = tgt.getLocation().toVector()
                .subtract(p.getEyeLocation().toVector()).normalize().multiply(0.6);
        double   dist = p.getEyeLocation().distance(tgt.getLocation());
        int      steps = (int)(dist / 0.6);
        for (int i = 0; i < steps; i++) {
            beam.add(step);
            w.spawnParticle(Particle.END_ROD,   beam, 2, 0.04, 0.04, 0.04, 0);
            w.spawnParticle(Particle.SONIC_BOOM, beam, 1, 0, 0, 0, 0);
            if (i % 3 == 0)
                w.spawnParticle(Particle.WHITE_ASH, beam, 2, 0.05, 0.05, 0.05, 0.02);
        }

        // Damage + knockback
        Vector vel = tgt.getLocation().toVector()
                .subtract(loc.toVector()).normalize().multiply(4.0).setY(1.0);
        tgt.damage(9.0 * dm, p);
        tgt.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks(80, dr), 2));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (tgt.isValid()) tgt.setVelocity(vel);
        }, 1L);

    } else {
        // ── AOE SHOCKWAVE — koi aim nahi, circle mein blast ───────
        w.spawnParticle(Particle.SONIC_BOOM, loc, 4, 0, 0, 0, 0);
        w.spawnParticle(Particle.END_ROD,    loc, 90, 4, 4, 4, 0.08);
        w.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 1f, 0.3f);
        w.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM,      1f, 0.7f);

        // 5 expanding shockwave rings — high animation
        for (int ring = 1; ring <= 5; ring++) {
            final int fr = ring;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                double r   = fr * 2.2;
                int    pts = 36;
                for (int i = 0; i < pts; i++) {
                    double a = Math.toRadians(i * (360.0 / pts));

                    // Ground ring
                    w.spawnParticle(Particle.END_ROD,
                        loc.clone().add(Math.cos(a)*r, 0.15, Math.sin(a)*r),
                        1, 0, 0, 0, 0);
                    // Mid ring
                    w.spawnParticle(Particle.SONIC_BOOM,
                        loc.clone().add(Math.cos(a)*r, 1.0,  Math.sin(a)*r),
                        1, 0, 0, 0, 0);
                    // Top ring
                    w.spawnParticle(Particle.END_ROD,
                        loc.clone().add(Math.cos(a)*r, 2.0,  Math.sin(a)*r),
                        1, 0, 0, 0, 0);
                }
                // Sound at each ring expansion
                w.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.6f,
                    0.3f + fr * 0.15f);
            }, fr * 3L);
        }

        // Damage + knockback sab nearby enemies
        p.getNearbyEntities(12, 12, 12).forEach(e -> {
            if (!(e instanceof LivingEntity le) || e == p) return;
            Vector vel = e.getLocation().toVector()
                    .subtract(loc.toVector()).normalize().multiply(2.5).setY(0.5);
            le.damage(5.0 * dm, p);
            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks(60, dr), 1));
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (e.isValid()) e.setVelocity(vel);
            }, 1L);
            w.spawnParticle(Particle.SONIC_BOOM, le.getLocation(), 1, 0, 0, 0, 0);
        });
    }
}

            // 16. RAGE — Rage Smash  [knockback 1 tick after damage]
            case RAGE -> {
                w.spawnParticle(Particle.EXPLOSION, loc,   5, 0.7, 0, 0.7, 0);
                w.spawnParticle(Particle.DUST,      loc, 100, 1.4, 0.7, 1.4, 0, new Particle.DustOptions(Color.RED, 2.5f));
                w.spawnParticle(Particle.CRIT,      loc,  70, 0.9, 0.5, 0.9, 0.14);
                w.spawnParticle(Particle.FLAME,     loc,  50, 0.8, 0.4, 0.8, 0.1);
                for (int ring = 1; ring <= 3; ring++) { final int fr = ring;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        for (int i = 0; i < 20; i++) { double a = Math.toRadians(i*18);
                            w.spawnParticle(Particle.DUST, loc.clone().add(Math.cos(a)*(fr*2.5),0.2,Math.sin(a)*(fr*2.5)), 4, 0, 0.4, 0, 0,
                                new Particle.DustOptions(Color.RED, 2f));
                        }
                    }, fr * 3L);
                }
                w.playSound(loc, Sound.ENTITY_RAVAGER_ROAR,   1f, 0.7f);
                w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE,0.9f, 0.8f);
                p.getNearbyEntities(8, 8, 8).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        Vector vel = e.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(3.8).setY(1.2);
                        le.damage(9.0 * dm, p);
                        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (e.isValid()) e.setVelocity(vel); }, 1L);
                    }
                });
            }

// Total 5s: 3s rise (2 blocks) + 2s shoot
case SPIRIT -> {
    p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, ticks(100, dr), 2));
    p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,       ticks(80,  dr), 1));

    w.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT,     0.9f, 1.4f);
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
        final Material blockMat = groundBlock.getType().isSolid()
            ? groundBlock.getType() : Material.GRASS_BLOCK;
        final Location spawnLoc = groundBlock.getLocation().clone().add(0.5, 0.0, 0.5);

        groundBlock.setType(Material.AIR);

        FallingBlock fb = w.spawnFallingBlock(spawnLoc, blockMat.createBlockData());
        fb.setDropItem(false);
        fb.setHurtEntities(false);
        fb.setGravity(false);

        w.spawnParticle(Particle.TOTEM_OF_UNDYING, spawnLoc, 20, 0.3, 0.3, 0.3, 0.06);
        w.spawnParticle(Particle.END_ROD,          spawnLoc, 10, 0.2, 0.2, 0.2, 0.03);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!fb.isValid()) { cancel(); return; }
                ticks++;

                if (ticks <= 60) {
                    // ── RISE 3s — FIX: sirf 2 block utha
                    // 60 ticks * 0.033 velocity = ~2 blocks
                    fb.setVelocity(new Vector(0, 0.033, 0));
                    w.spawnParticle(Particle.TOTEM_OF_UNDYING,
                        fb.getLocation(), 3, 0.15, 0.1, 0.15, 0.03);
                    w.spawnParticle(Particle.WITCH,
                        fb.getLocation(), 1, 0.1,  0.1, 0.1,  0.02);
                    if (ticks == 1)
                        w.playSound(fb.getLocation(), Sound.BLOCK_STONE_BREAK, 0.8f, 0.5f);

                } else if (ticks == 61) {
                    // ── SHOOT — FIX: uper nahi, aim direction mein sidha shoot
                    // 4 sides ke offsets se direction nikalo
                    double dx = off[0], dz = off[2];
                    Vector shootDir = new Vector(dx, 0, dz).normalize().multiply(3.5);
                    // Thoda upar bhi taaki arc ho
                    shootDir.setY(0.3);

                    w.playSound(fb.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1f, 1.2f);
                    w.playSound(fb.getLocation(), Sound.ENTITY_BLAZE_SHOOT,  1f, 0.8f);
                    w.spawnParticle(Particle.TOTEM_OF_UNDYING,
                        fb.getLocation(), 30, 0.4, 0.2, 0.4, 0.08);
                    fb.setGravity(true);
                    fb.setVelocity(shootDir);

                } else if (ticks <= 100) {
                    // ── SHOOT 2s
                    w.spawnParticle(Particle.TOTEM_OF_UNDYING,
                        fb.getLocation(), 5, 0.2, 0.1, 0.2, 0.05);
                    w.spawnParticle(Particle.END_ROD,
                        fb.getLocation(), 2, 0.1, 0.1, 0.1, 0.02);

                    for (org.bukkit.entity.Entity e :
                            w.getNearbyEntities(fb.getLocation(), 1.5, 1.5, 1.5)) {
                        if (e instanceof LivingEntity le && e != p) {
                            le.damage(10.0 * dm, p);
                            w.spawnParticle(Particle.TOTEM_OF_UNDYING,
                                le.getLocation(), 20, 0.4, 0.8, 0.4, 0.06);
                            Vector knock = fb.getLocation().toVector()
                                .subtract(spawnLoc.toVector())
                                .normalize().multiply(1.5).setY(0.8);
                            Bukkit.getScheduler().runTaskLater(plugin,
                                () -> { if (e.isValid()) le.setVelocity(knock); }, 1L);
                        }
                    }

                } else {
                    // ── END
                    w.spawnParticle(Particle.FALLING_DUST,
                        fb.getLocation(), 20, 0.5, 0.3, 0.5, 0.1,
                        blockMat.createBlockData());
                    w.spawnParticle(Particle.TOTEM_OF_UNDYING,
                        fb.getLocation(), 15, 0.3, 0.3, 0.3, 0.05);
                    w.playSound(fb.getLocation(), Sound.BLOCK_STONE_BREAK, 1f, 0.8f);
                    fb.remove();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}


                            // TIME — SHIFT + Q (Time Dash) FIXED
            case TIME -> {
                // Direction ko final lo taaki runnable ke andar access ho sake
                final Vector dashDir = p.getEyeLocation().getDirection().normalize();

                // 1. Origin Burst (No Change in Animation)
                w.spawnParticle(Particle.ELECTRIC_SPARK, loc, 60, 0.3, 0.5, 0.3, 0.15);
                w.spawnParticle(Particle.FLASH,          loc, 1,  0,   0,   0,   0);
                w.spawnParticle(Particle.REVERSE_PORTAL, loc, 30, 0.4, 0.6, 0.4, 0.1);
                w.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 2.0f);
                w.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_CURSE,   0.4f, 2.0f);

                // 2. FIXED VELOCITY LOGIC
                // Bukkit.getScheduler() hata kar direct velocity di hai taaki anti-cheat ise block na kare
                double boostY = dashDir.getY() > 0.1 ? dashDir.getY() * 2.5 : 0.3;
                p.setVelocity(dashDir.clone().multiply(2.0).setY(0.4)); 
                p.setFallDistance(0f);

                // 3. Trail Animation (Aapki original animation, no change)
                new BukkitRunnable() {
                    int t = 0;
                    double trailAngle = 0;

                    @Override
                    public void run() {
                        if (!p.isOnline() || p.isDead() || t++ >= 12) {
                            p.setFallDistance(0f);
                            cancel();
                            return;
                        }

                        trailAngle += 0.6;
                        Location cur = p.getLocation();

                        // Animation Particles (Same as yours)
                        w.spawnParticle(Particle.ELECTRIC_SPARK, cur, 8, 0.2, 0.3, 0.2, 0.08);
                        if (t % 2 == 0) w.spawnParticle(Particle.FLASH, cur, 1, 0, 0, 0, 0);
                        w.spawnParticle(Particle.REVERSE_PORTAL, cur, 5, 0.2, 0.4, 0.2, 0.06);

                        for (int i = 0; i < 6; i++) {
                            double a = trailAngle + Math.toRadians(i * 60);
                            w.spawnParticle(Particle.ELECTRIC_SPARK,
                                cur.clone().add(Math.cos(a)*0.4, 0.5 + Math.sin(a)*0.3, Math.sin(a)*0.4),
                                1, 0, 0, 0, 0.04);
                        }
                        w.spawnParticle(Particle.END_ROD, cur, 3, 0.1, 0.2, 0.1, 0.02);
                        
                        // Velocity Maintainer: Dash ke beech mein speed kam na ho
                        if (t < 6) {
                            p.setVelocity(dashDir.clone().multiply(1.5).setY(p.getVelocity().getY()));
                        }
                        p.setFallDistance(0f);
                    }
                }.runTaskTimer(plugin, 1L, 1L); // 1 tick baad trail start taaki velocity sync ho
            }


            // 19. WARRIOR — Warrior Charge
            case WARRIOR -> {
                p.setVelocity(dir.clone().multiply(3.0).setY(0.3));
                w.spawnParticle(Particle.CRIT,  loc, 60, 0.5, 0.8, 0.5, 0.12);
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

            // ═══ EVENT SECONDARY ══════════════════════════════════════════════

            // OCEAN SECONDARY
case OCEAN -> {
    w.playSound(p.getLocation(), Sound.ITEM_BUCKET_FILL, 2f, 0.5f);
    
    p.getWorld().getNearbyEntities(p.getLocation(), 6, 6, 6).forEach(e -> {
        if (e instanceof LivingEntity le && e != p) {
            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 4)); // Super Slow
            
            new BukkitRunnable() {
                int ticks = 0;
                public void run() {
                    if (ticks++ >= 80 || le.isDead()) { cancel(); return; } // 4 seconds prison
                    
                    // Sphere (Bubble) Animation Math around the enemy
                    for(double i = 0; i < Math.PI; i += Math.PI/6) {
                        for(double j = 0; j < 2*Math.PI; j += Math.PI/6) {
                            double x = 1.5 * Math.sin(i) * Math.cos(j);
                            double y = 1.5 * Math.sin(i) * Math.sin(j) + 1; // Center at body
                            double z = 1.5 * Math.cos(i);
                            w.spawnParticle(Particle.SPLASH, le.getLocation().clone().add(x,y,z), 1, 0,0,0,0);
                        }
                    }
                }
            }.runTaskTimer(plugin, 0, 3);
        }
    });
}


            //E4. ECLIPSE — Orbital Strike
// TNT rings layer by layer from sky, all land then SIMULTANEOUS blast
case ECLIPSE -> {
    p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,   ticks(200, dr), 3));
    p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,        ticks(200, dr), 2));
    p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, ticks(220, dr), 0));

    w.playSound(loc, Sound.ENTITY_WITHER_AMBIENT,       1f, 0.3f);
    w.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL,   1f, 0.5f);
    p.sendTitle("\u00a75\u00a7lORBITAL STRIKE", "\u00a78Incoming...", 5, 60, 10);

    // Fall damage cancel for owner
    p.setFallDistance(0f);

    // ── TNT Ring config ───────────────────────────────────────────────────
    // 3 rings: radii 4, 7, 10 blocks
    // Each ring: TNT spaced evenly
    // Drop height: 30 blocks above target
    int   DROP_HEIGHT = 30;
    int[] ringRadii   = {4, 7, 10};
    int[] ringCounts  = {8, 12, 16}; // TNT per ring

    final java.util.List<org.bukkit.entity.TNTPrimed> allTNT = new java.util.ArrayList<>();
    final Location center = loc.clone();

    // ── Spawn TNT rings ───────────────────────────────────────────────────
    for (int ring = 0; ring < 3; ring++) {
        final int   fr      = ring;
        final int   count   = ringCounts[ring];
        final double radius = ringRadii[ring];

        // Each ring drops slightly after previous — wave effect
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (int i = 0; i < count; i++) {
                double angle = Math.toRadians(i * (360.0 / count));
                double tx = center.getX() + Math.cos(angle) * radius;
                double tz = center.getZ() + Math.sin(angle) * radius;
                Location spawnLoc = new Location(w, tx, center.getY() + DROP_HEIGHT, tz);

                // Spawn TNTPrimed
                org.bukkit.entity.TNTPrimed tnt =
                    w.spawn(spawnLoc, org.bukkit.entity.TNTPrimed.class);
                tnt.setFuseTicks(999999); // never self-explode — we control it
                tnt.setVelocity(new Vector(0, -1.2, 0)); // fall down fast
                tnt.setYield(0f); // no block damage on its own
                synchronized (allTNT) { allTNT.add(tnt); }

                // Trail while falling
                new BukkitRunnable() {
                    int t = 0;
                    public void run() {
                        if (!tnt.isValid() || t++ > 30) { cancel(); return; }
                        w.spawnParticle(Particle.FLAME,
                            tnt.getLocation(), 2, 0.1, 0.1, 0.1, 0.02);
                        w.spawnParticle(Particle.SMOKE,
                            tnt.getLocation(), 1, 0.05, 0.05, 0.05, 0.01);
                        // Fall damage cancel for owner during strike
                        p.setFallDistance(0f);
                    }
                }.runTaskTimer(plugin, 0, 1);
            }

            // Sound per ring spawn
            w.playSound(center, Sound.ENTITY_WITHER_SHOOT, 0.8f, 1.5f + fr * 0.2f);

        }, ring * 3L); // rings drop 3 ticks apart — wave
    }

    // ── Wait for TNT to land (approx 1.5s fall) then BLAST ALL ───────────
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
        // Pre-blast warning
        w.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 0.3f);
        for (int i = 0; i < 36; i++) {
            double a = Math.toRadians(i * 10);
            double r = 10;
            w.spawnParticle(Particle.FLAME,
                center.clone().add(Math.cos(a)*r, 0.5, Math.sin(a)*r),
                2, 0, 0, 0, 0.05);
        }
    }, 20L); // 1s warning before blast

    Bukkit.getScheduler().runTaskLater(plugin, () -> {

        // ── SIMULTANEOUS BLAST ─────────────────────────────────────────
        synchronized (allTNT) {
            for (org.bukkit.entity.TNTPrimed tnt : allTNT) {
                if (!tnt.isValid()) continue;

                Location blastLoc = tnt.getLocation().clone();

                // Remove TNT entity
                tnt.remove();

                // Manual explosion at ground level — no block damage
                // Particles simulate blast
                w.spawnParticle(Particle.EXPLOSION_EMITTER,
                    blastLoc, 1, 0, 0, 0, 0);
                w.spawnParticle(Particle.EXPLOSION,
                    blastLoc, 5, 0.3, 0.3, 0.3, 0.05);
                w.spawnParticle(Particle.FLAME,
                    blastLoc, 10, 0.4, 0.3, 0.4, 0.1);
                w.spawnParticle(Particle.SMOKE,
                    blastLoc, 8, 0.3, 0.2, 0.3, 0.05);

                // Blast sound
                w.playSound(blastLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);

                // Damage enemies near each TNT — owner safe
                blastLoc.getWorld().getNearbyEntities(blastLoc, 4, 4, 4)
                    .forEach(e -> {
                        if (!(e instanceof LivingEntity le)) return;
                        if (e.getUniqueId().equals(p.getUniqueId())) return;
                        le.damage(ecfg("ECLIPSE","secondary-damage",8.0) * getDmg(p), p);
                        // Knockback away from blast
                        Vector vel = e.getLocation().toVector()
                            .subtract(blastLoc.toVector())
                            .normalize().multiply(2.5).setY(0.8);
                        Bukkit.getScheduler().runTaskLater(plugin,
                            () -> { if (e.isValid()) e.setVelocity(vel); }, 1L);
                    });
            }
            allTNT.clear();
        }

        // ── Big shockwave after all blasts ────────────────────────────
        for (int ring = 1; ring <= 4; ring++) {
            final int fr = ring;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                double rad = fr * 3.0;
                for (int i = 0; i < 36; i++) {
                    double a = Math.toRadians(i * 10);
                    w.spawnParticle(Particle.EXPLOSION,
                        center.clone().add(Math.cos(a)*rad, 0.3, Math.sin(a)*rad),
                        1, 0, 0, 0, 0);
                    w.spawnParticle(Particle.FLAME,
                        center.clone().add(Math.cos(a)*rad, 0.5, Math.sin(a)*rad),
                        1, 0, 0, 0, 0.03);
                }
                w.playSound(center, Sound.ENTITY_GENERIC_EXPLODE,
                    1f, 0.5f + fr * 0.1f);
            }, ring * 3L);
        }

        // Final big sound
        w.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 0.4f);
        w.playSound(center, Sound.ENTITY_WITHER_BREAK_BLOCK,     1f, 0.3f);

        // Fall damage cancel after blast
        p.setFallDistance(0f);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) p.setFallDistance(0f);
        }, 20L);

    }, 35L); // 35 ticks after spawn = all TNT landed
}

            // E5. GUARDIAN — Titan Shockwave  [knockback 1 tick after damage]
            case GUARDIAN -> {
                double eDmg = ecfg("guardian","secondary-damage", 15.0) * dm;
                int    eRad = (int) ecfg("guardian","secondary-radius", 20);
                w.spawnParticle(Particle.EXPLOSION,        loc,   8, 2,   0, 2, 0);
                w.spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 150, 3.5, 2.5, 3.5, 0.14);
                w.spawnParticle(Particle.END_ROD,          loc, 120, 3.5, 2.5, 3.5, 0.12);
                for (int wave = 1; wave <= 6; wave++) { final int fw = wave;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        double r = fw * eRad / 5.0;
                        for (int i = 0; i < 36; i++) { double a = Math.toRadians(i*10);
                            w.spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(Math.cos(a)*r,0.4,Math.sin(a)*r), 4, 0, 0.6, 0, 0.06);
                            w.spawnParticle(Particle.END_ROD,          loc.clone().add(Math.cos(a)*r,0.4,Math.sin(a)*r), 2, 0, 0.3, 0, 0.02);
                        }
                    }, wave * 4L);
                }
                w.playSound(loc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 0.2f);
                w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE,   1f, 0.3f);
                w.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE,    1f, 0.4f);
                p.getNearbyEntities(eRad, eRad, eRad).forEach(e -> {
                    if (e instanceof LivingEntity le && e != p) {
                        Vector vel = e.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(5.0).setY(1.8);
                        le.damage(eDmg, p);
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks(120, dr), 3));
                        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (e.isValid()) e.setVelocity(vel); }, 1L);
                        w.spawnParticle(Particle.TOTEM_OF_UNDYING, le.getLocation(), 25, 0.4, 0.7, 0.4, 0.05);
                    }
                });
                p.sendTitle("§6§l⚡ TITAN SHOCKWAVE!", "§7The ground shatters!", 5, 70, 15);
            }

case METEOR -> {
    double mDmg = ecfg("METEOR", "secondary-damage", 8.0);
    
    // 8 Seconds Duration = 160 Ticks. 20 Meteors total.
    // Har 8 ticks (0.4s) mein ek meteor girega.
    new BukkitRunnable() {
        int count = 0;

        @Override
        public void run() {
            if (count >= 20 || !p.isOnline()) {
                cancel();
                return;
            }
            count++;

            // 40 Block Radius mein Random Location
            double rx = (Math.random() * 80) - 40;
            double rz = (Math.random() * 80) - 40;
            Location dropLoc = p.getLocation().clone().add(rx, 40, rz); // 40 blocks upar se shuru

            // Meteor Falling Logic
            new BukkitRunnable() {
                Location current = dropLoc.clone();
                // Target ground location calculate karo trajectory ke liye
                Vector direction = new Vector(0, -1, 0); 

                @Override
                public void run() {
                    // 1. Zameen par takraya (Explosion & Shockwave)
                    if (current.getBlock().getType().isSolid() || current.getY() <= current.getWorld().getMinHeight()) {
                        
                        // 2 TNT Equivalent Explosion (Power 8f approx)
                        current.getWorld().createExplosion(current, 4f, false, false);
                        w.spawnParticle(Particle.EXPLOSION_EMITTER, current, 3, 1, 1, 1, 0);
                        w.playSound(current, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
                        w.playSound(current, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1f, 0.8f);

                        // Shockwave Animation (Expanding Ring)
                        for (int i = 0; i < 30; i++) {
                            double angle = i * (Math.PI * 2 / 30);
                            double x = Math.cos(angle) * 4;
                            double z = Math.sin(angle) * 4;
                            w.spawnParticle(Particle.DUST_PLUME, current.clone().add(x, 0.1, z), 2, 0, 0.2, 0, 0.1, Material.STONE.createBlockData());
                        }

                        // Damage Only Enemies
                        applySafeDamage(p, current, 6.0, mDmg); 
                        cancel(); 
                        return;
                    }

                    // 2. REAL METEOR ANIMATION (Photo Jaisa Trail)
                    // Meteor Head (Mota Fireball)
                    w.spawnParticle(Particle.LARGE_SMOKE, current, 5, 0.2, 0.2, 0.2, 0.02);
                    w.spawnParticle(Particle.LAVA,        current, 8, 0.3, 0.3, 0.3, 0.05);
                    
                    // Long Fire Tail (Peeche nikalti hui aag)
                    for (int t = 0; t < 5; t++) {
                        Location tail = current.clone().subtract(direction.clone().multiply(t * 0.3));
                        w.spawnParticle(Particle.FLAME, tail, 10, 0.2, 0.2, 0.2, 0.08);
                        if (t > 2) w.spawnParticle(Particle.DUST, tail, 5, 0.3, 0.3, 0.3, new Particle.DustOptions(Color.fromRGB(80, 40, 0), 2.0f));
                    }

                    // Tezi se neeche girna (Speed 1.2 blocks per tick)
                    current.add(0, -1.2, 0); 
                }
            }.runTaskTimer(plugin, 0, 1);
        }
    }.runTaskTimer(plugin, 0, 8); // Har meteor ke beech 0.4s ka gap
}

            case MIRAGE -> {
    w.playSound(p.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.5f, 0.5f);
    w.playSound(p.getLocation(), Sound.ENTITY_WARDEN_EMERGE, 1.0f, 0.7f);

    // Epic Smoke Poof (Mirror Effect)
    w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, p.getLocation(), 80, 2, 1, 2, 0.05);
    w.spawnParticle(Particle.SQUID_INK, p.getLocation(), 50, 1.5, 1, 1.5, 0.1);

    // List taaki 10s baad sabko ek saath remove kar sakein
    java.util.List<org.bukkit.entity.Warden> minions = new java.util.ArrayList<>();

    for (int i = 0; i < 5; i++) {
        // Player ke charo taraf spawn karne ke liye math
        double angle = i * (Math.PI * 2 / 5);
        Location spawnLoc = p.getLocation().clone().add(Math.cos(angle) * 3, 0, Math.sin(angle) * 3);
        
        Warden warden = (Warden) w.spawnEntity(spawnLoc, org.bukkit.entity.EntityType.WARDEN);
        
        // 1. Name Set Karo (NotEasyOk's Minion)
        warden.setCustomName("§7" + p.getName() + "'s §3Minion");
        warden.setCustomNameVisible(true);
        
        // 2. Owner Safety & Targeting
        warden.setRemoveWhenFarAway(true);
        // Metadata daal do taaki listener ise pehchan sake
        warden.setMetadata("MinionOf", new org.bukkit.metadata.FixedMetadataValue(plugin, p.getUniqueId().toString()));

        minions.add(warden);
        
        // Emergence effect
        w.spawnParticle(Particle.SONIC_BOOM, spawnLoc.add(0, 1, 0), 1, 0, 0, 0, 0);
    }

    // 3. 10 SECONDS TIMER (Follow & Cleanup)
    new BukkitRunnable() {
        int ticks = 0;
        @Override
        public void run() {
            if (ticks >= 200 || !p.isOnline()) { // 10 seconds (200 ticks)
                for (Warden m : minions) {
                    if (m.isValid()) {
                        w.spawnParticle(Particle.FLASH, m.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0.05);
                        m.remove();
                    }
                }
                cancel();
                return;
            }

            for (Warden m : minions) {
                if (!m.isValid()) continue;
                
                // Owner ko follow karwana (Agar door chale gaye)
                if (m.getLocation().distance(p.getLocation()) > 10) {
                    m.getPathfinder().moveTo(p.getLocation());
                }

                // Owner ko target karne se rokna (Double Safety)
                if (m.getTarget() != null && m.getTarget().equals(p)) {
                    m.setTarget(null);
                }
            }
            ticks += 10;
        }
    }.runTaskTimer(plugin, 0, 10);
}




    // ══════════════════════════════════════════════════════════════════════
    //  PASSIVE EFFECTS
    // ══════════════════════════════════════════════════════════════════════
    public void applyPassiveEffects(Player p, EyeType eye) {
        for (PotionEffect e : p.getActivePotionEffects()) p.removePotionEffect(e.getType());
        if (eye == null || eye == EyeType.NONE) return;
        switch (eye) {
            case VOID    -> p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION,    INF, 0, false, false));
            case PHANTOM -> p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION,    INF, 0, false, false));
            case STORM   -> p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,           INF, 1, false, false));
            case FROST   -> p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, INF, 0, false, false));
            case FLAME   -> p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, INF, 0, false, false));
            case SHADOW  -> { p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,         INF,0,false,false)); p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION,INF,0,false,false)); }
            case TITAN   -> p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,      INF, 1, false, false));
            case HUNTER  -> p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,           INF, 1, false, false));
            case GRAVITY -> p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST,      INF, 2, false, false));
            case WIND    -> p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,           INF, 1, false, false));
            case POISON  -> p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,    INF, 0, false, false));
            case LIGHT   -> p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION,    INF, 0, false, false));
            case EARTH   -> p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,      INF, 0, false, false));
            case CRYSTAL -> p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,      INF, 0, false, false));
            case ECHO    -> { p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION,  INF,0,false,false)); p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,INF,0,false,false)); }
            case RAGE    -> p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH,        INF, 0, false, false));
            case SPIRIT  -> p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,    INF, 1, false, false));
            case TIME    -> p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE,           INF, 1, false, false));
            case WARRIOR -> p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH,        INF, 1, false, false));
            case METEOR  -> { p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE,INF,0,false,false)); p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,INF,0,false,false)); }
            case MIRAGE  -> p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,           INF, 2, false, false));
            case OCEAN   -> { p.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING,INF,0,false,false)); p.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER,INF,0,false,false)); }
            case ECLIPSE -> { p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION,  INF,0,false,false)); p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE,INF,0,false,false)); }
            case GUARDIAN-> { p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,    INF,2,false,false)); p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION,INF,1,false,false)); }
        }
    }

            
        // ══════════════════════════════════════════════════════════════════════
    //  GUI  — XP auto-refreshes every second while the inventory is open
    // ══════════════════════════════════════════════════════════════════════
    public void openEyeGUI(Player p) {
        Inventory gui = Bukkit.createInventory(null, 54, "§8Your Ancient Eye Status");
        ItemStack border = pane(Material.PURPLE_STAINED_GLASS_PANE);
        ItemStack fill   = pane(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < 54; i++)
            gui.setItem(i, (i<9||i>=45||i%9==0||i%9==8) ? border : fill);
        gui.setItem(22, buildEyeItem(p));
        p.openInventory(gui);
        p.playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1.2f);

        new BukkitRunnable() {
            public void run() {
                if (!p.isOnline()) { cancel(); return; }
                Inventory top = p.getOpenInventory().getTopInventory();
                if (top == null || !p.getOpenInventory().getTitle().equals("§8Your Ancient Eye Status")) {
                    cancel(); return;
                }
                top.setItem(22, buildEyeItem(p));
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    // Builds the center eye item with live kill/level data
    private ItemStack buildEyeItem(Player p) {
        EyeType type  = plugin.getPlayerData().getEye(p);
        int     level = plugin.getPlayerData().getLevel(p);
        int     kills = plugin.getPlayerData().getXP(p);
        // FIX: config se kill thresholds
        int     maxKills = plugin.getPlayerData().getMaxXPForLevel(level);
        String  bar      = progressBar(kills, maxKills);
        String  col      = level==3?"§e§l":level==2?"§b§l":"§7§l";

        ItemStack eye = new ItemStack(Material.ENDER_EYE);
        ItemMeta  m   = eye.getItemMeta();
        m.setDisplayName(col + type.name().replace("_"," ") + " EYE");
        List<String> lore = new ArrayList<>();
        lore.add("§8━━━━━━━━━━━━━━━━━━");
        lore.add("§7Level    §f"+level+" §8/ §f3");
        // FIX: XP ki jagah Kills dikhao
        lore.add("§7Kills: "+bar+" §8(§c"+kills+"§8/§f"+maxKills+"§8)");
        lore.add("§8━━━━━━━━━━━━━━━━━━");
        lore.add("§6§lStats:");
        lore.add("§e- §7Damage Bonus:  §c+"+(int)((getDmg(p)-1)*100)+"%");
        lore.add("§e- §7Duration Mul:  §b"+(int)(getDur(p)*100)+"% of base");
        lore.add("§e- §7Cooldown Red:  §a-"+((level-1)*2)+"s per ability");
        lore.add("§8━━━━━━━━━━━━━━━━━━");
        lore.add("§b§lAbilities:");
        lore.add("§f  SHIFT+F  §7->  §bPrimary");
        lore.add("§f  SHIFT+Q  §7->  §bSecondary");
        lore.add("§8━━━━━━━━━━━━━━━━━━");
        lore.add("§5§oThe power is bound to your soul.");
        m.setLore(lore);
        eye.setItemMeta(m);
        return eye;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════════════

    private double getDmg(Player p) {
        int l = plugin.getPlayerData().getLevel(p);
        return l == 3 ? 1.5 : l == 2 ? 1.2 : 1.0;
    }

    private double getDur(Player p) {
        int l = plugin.getPlayerData().getLevel(p);
        return l == 3 ? 2.0 : l == 2 ? 1.5 : 1.0;
    }

    private int ticks(int base, double durMul) {
        return (int)(base * durMul);
    }

    private int getCd(Player p, EyeType eye, String type) {
        String key = "cooldowns." + eye.name().toLowerCase() + "." + type;
        int base = plugin.getConfig().getInt(key, 12);
        int lvl  = plugin.getPlayerData().getLevel(p);
        int reduction = (lvl - 1) * 2;
        return Math.max(2, base - reduction);
    }

    private void doMeteorStrike(Player p, World w, Location pos, double dmg, int rad) {
        w.strikeLightningEffect(pos);
        w.spawnParticle(Particle.EXPLOSION,  pos, 4, 1.2, 0, 1.2, 0);
        w.spawnParticle(Particle.FLAME,      pos, 100, rad*0.35, 0.3, rad*0.35, 0.18);
        w.spawnParticle(Particle.LAVA,       pos,  30, rad*0.25, 0.2, rad*0.25, 0.12);
        w.spawnParticle(Particle.DUST_PLUME, pos,  70, rad*0.25, 0.2, rad*0.25, 0.20, Material.MAGMA_BLOCK.createBlockData());
        for (int i = 0; i < 24; i++) {
            double a = Math.toRadians(i * 15);
            w.spawnParticle(Particle.FLAME, pos.clone().add(Math.cos(a)*rad,0.2,Math.sin(a)*rad), 4, 0, 0.4, 0, 0.06);
        }
        w.playSound(pos, Sound.ENTITY_GENERIC_EXPLODE,        1f, 0.4f);
        w.playSound(pos, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 0.6f);
        pos.getWorld().getNearbyEntities(pos, rad, rad, rad).forEach(e -> {
            if (e instanceof LivingEntity le && e != p) {
                Vector vel = e.getLocation().toVector().subtract(pos.toVector()).normalize().multiply(3.2).setY(1.8);
                le.damage(dmg, p);
                le.setFireTicks(100);
                Bukkit.getScheduler().runTaskLater(plugin, () -> { if (e.isValid()) e.setVelocity(vel); }, 1L);
            }
        });
    }

    private Location safe(Location l) {
        if (l.getBlock().getType().isSolid()) l.add(0,1,0);
        if (l.getBlock().getType().isSolid()) l.add(0,1,0);
        return l;
    }

    private LivingEntity aim(Player p, double range) {
        RayTraceResult r = p.getWorld().rayTraceEntities(p.getEyeLocation(), p.getEyeLocation().getDirection(), range, 1.5, e -> e != p);
        return (r != null && r.getHitEntity() instanceof LivingEntity le) ? le : null;
    }

    private void crystalAura(Player p) {
        World w = p.getWorld(); Location l = p.getLocation();
        Particle.DustOptions d = new Particle.DustOptions(Color.fromRGB(100,220,255), 2f);
        for (double a = 0; a < Math.PI*2; a += Math.PI/10) {
            double rx = Math.cos(a)*2, rz = Math.sin(a)*2;
            w.spawnParticle(Particle.DUST, l.clone().add(rx,1,rz), 3, 0, 0.4, 0, 0, d);
            w.spawnParticle(Particle.END_ROD, l.clone().add(rx,1,rz), 2, 0, 0.3, 0, 0.01);
        }
        w.spawnParticle(Particle.END_ROD, l, 40, 0.5, 1.2, 0.5, 0.04);
    }

    private void buildWall(Player p) {
        Block b = p.getTargetBlockExact(5); if (b == null) return;
        for (int y = 1; y <= 3; y++) b.getLocation().add(0,y,0).getBlock().setType(Material.COBBLESTONE);
    }

    private void voidRift(World w, Location l) {
        w.spawnParticle(Particle.PORTAL,         l, 70, 0.6, 0.9, 0.6, 0.28);
        w.spawnParticle(Particle.REVERSE_PORTAL, l, 45, 0.4, 0.7, 0.4, 0.16);
        w.spawnParticle(Particle.DRAGON_BREATH,  l, 30, 0.3, 0.5, 0.3, 0.06);
        for (int i = 0; i < 14; i++) { double a = Math.toRadians(i*(360.0/14));
            w.spawnParticle(Particle.PORTAL, l.clone().add(Math.cos(a)*1.4,0.5,Math.sin(a)*1.4), 3, 0, 0, 0, 0.09);
        }
    }

    private double ecfg(String eye, String key, double def) {
        return plugin.getConfig().getDouble("event-eyes."+eye+"."+key, def);
    }

    private ItemStack pane(Material mat) {
        ItemStack i = new ItemStack(mat);
        ItemMeta  m = i.getItemMeta();
        m.setDisplayName("§r");
        i.setItemMeta(m);
        return i;
    }

    private void whiteLightScreen(Player ep, int durationTicks, AncientEyePlugin plugin) {
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (!ep.isOnline() || t++ >= durationTicks) { cancel(); return; }
                Location eyeFront = ep.getEyeLocation().clone()
                        .add(ep.getEyeLocation().getDirection().multiply(0.1));
                ep.spawnParticle(Particle.FLASH, eyeFront, 1, 0, 0, 0, 0);
                for (int i = 0; i < 8; i++) {
                    double a = Math.toRadians(i * 45);
                    ep.spawnParticle(Particle.FLASH,
                        eyeFront.clone().add(Math.cos(a)*0.12, Math.sin(a)*0.12, 0),
                        1, 0, 0, 0, 0);
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private String progressBar(int cur, int max) {
        int bars = 12;
        int done = (int)((double)Math.min(cur, max) / max * bars);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bars; i++) {
            sb.append(i < done ? "§a|" : "§7|");
        }
        return sb.toString();
    }

    private void restoreOrDrop(Player p, ItemStack stored,
                               Runnable setter, ItemStack current) {
        if (stored == null) return;
        boolean slotEmpty = (current == null || current.getType() == Material.AIR);
        if (slotEmpty) {
            setter.run();
        } else {
            p.getWorld().dropItemNaturally(p.getLocation(), stored);
            p.sendMessage("\u00a77Your hidden item was dropped: \u00a7f"
                + stored.getType().name().toLowerCase().replace("_", " "));
        }
    }

// ── Eclipse blast helper — HELPERS section mein add karo ─────────────────────
private void doEclipseBlast(Player p, World w, Location blastLoc,
                             double dmg, AncientEyePlugin plugin) {
    double dm = getDmg(p);

    // Cube shatter
    w.spawnParticle(Particle.SQUID_INK,         blastLoc, 60, 0.5, 0.5, 0.5, 0.15);
    w.spawnParticle(Particle.DRAGON_BREATH,     blastLoc, 80, 0.8, 0.8, 0.8, 0.08);
    w.spawnParticle(Particle.EXPLOSION_EMITTER, blastLoc,  2, 0, 0, 0, 0);
    w.spawnParticle(Particle.EXPLOSION,         blastLoc, 20, 1.0, 0.5, 1.0, 0.1);

    // 2nd explosion — slight delay
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
        w.spawnParticle(Particle.EXPLOSION_EMITTER, blastLoc, 3, 0.3, 0, 0.3, 0);
        w.spawnParticle(Particle.SQUID_INK,         blastLoc, 40, 0.6, 0.6, 0.6, 0.12);
        w.spawnParticle(Particle.DRAGON_BREATH,     blastLoc, 60, 0.7, 0.7, 0.7, 0.07);
        w.playSound(blastLoc, Sound.ENTITY_GENERIC_EXPLODE,        2f, 0.4f);
        w.playSound(blastLoc, Sound.ENTITY_WITHER_BREAK_BLOCK,     1f, 0.5f);
    }, 5L);

    // Blast sounds
    w.playSound(blastLoc, Sound.ENTITY_GENERIC_EXPLODE,        2f, 0.3f);
    w.playSound(blastLoc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1f, 0.3f);
    w.playSound(blastLoc, Sound.ENTITY_WITHER_BREAK_BLOCK,      1f, 0.5f);

    // 5 expanding shockwave rings
    for (int ring = 1; ring <= 5; ring++) {
        final int fr = ring;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            double rad = fr * 2.0;
            int    pts = 36;
            for (int i = 0; i < pts; i++) {
                double a = Math.toRadians(i * (360.0 / pts));
                w.spawnParticle(Particle.SQUID_INK,
                    blastLoc.clone().add(Math.cos(a)*rad, 0.1, Math.sin(a)*rad),
                    1, 0, 0, 0, 0);
                w.spawnParticle(Particle.DRAGON_BREATH,
                    blastLoc.clone().add(Math.cos(a)*rad, 1.0, Math.sin(a)*rad),
                    1, 0, 0, 0, 0);
                w.spawnParticle(Particle.SQUID_INK,
                    blastLoc.clone().add(Math.cos(a)*rad, 2.0, Math.sin(a)*rad),
                    1, 0, 0, 0, 0);
            }
            w.playSound(blastLoc, Sound.ENTITY_WITHER_AMBIENT,
                0.5f, 0.3f + fr * 0.1f);
        }, ring * 2L);
    }

    // Block shake
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
        for (int i = 0; i < 30; i++) {
            double angle = Math.toRadians(i * 12);
            double dist  = 2 + (i % 5);
            Location blk = blastLoc.clone().add(
                Math.cos(angle)*dist, -0.5, Math.sin(angle)*dist);
            w.spawnParticle(Particle.FALLING_DUST, blk, 5,
                0.3, 0.1, 0.3, 0.05,
                org.bukkit.Material.OBSIDIAN.createBlockData());
        }
    }, 3L);

    // Damage + knockback — owner safe
    applySafeDamage(p, blastLoc, 10.0, dmg);
    blastLoc.getWorld().getNearbyEntities(blastLoc, 10, 10, 10).forEach(e -> {
        if (e instanceof LivingEntity le && e != p) {
            Vector vel = e.getLocation().toVector()
                .subtract(blastLoc.toVector())
                .normalize().multiply(3.5).setY(1.2);
            Bukkit.getScheduler().runTaskLater(plugin,
                () -> { if (e.isValid()) e.setVelocity(vel); }, 1L);
        }
    });
}

    @EventHandler
public void onMinionAttack(EntityTargetLivingEntityEvent event) {
    if (event.getEntity() instanceof Warden warden && event.getTarget() instanceof Player p) {
        // Agar Warden ke paas "MinionOf" metadata hai aur target owner hai
        if (warden.hasMetadata("MinionOf")) {
            String ownerUUID = warden.getMetadata("MinionOf").get(0).asString();
            if (p.getUniqueId().toString().equals(ownerUUID)) {
                event.setCancelled(true); // Owner par gussa nahi karega!
                event.setTarget(null);
            }
        }
    }
}


    private void applySafeDamage(Player owner, Location loc, double radius, double damage) {
        loc.getWorld().getNearbyEntities(loc, radius, radius, radius).forEach(entity -> {
            if (entity instanceof LivingEntity victim && !entity.equals(owner)) {
                victim.damage(damage * getDmg(owner), owner);
                victim.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1, 0), 5);
            }
        });
    }
}
