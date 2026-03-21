package com.ancienteye;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class AnimationTradeManager {

    private final Map<UUID, UUID> pendingTrades = new HashMap<>();
    private final AncientEyePlugin plugin;

    public AnimationTradeManager(AncientEyePlugin plugin) {
        this.plugin = plugin;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  /smpstart  RITUAL ANIMATION  — upgraded spin
    // ══════════════════════════════════════════════════════════════════════════
    public void startSmpRitual(Player p) {
        // Config se duration — default 8s = 160 ticks
        int duration = plugin.getConfig().getInt("animation.smp-spin-ticks", 160);

        new BukkitRunnable() {
            int ticks = 0;
            final EyeType[] allBasicEyes = getBasicEyes();
            final Random rng = new Random();

            // Per-eye color cycling for spin flash
            int colorIndex = 0;
            final Color[] flashColors = {
                Color.AQUA, Color.PURPLE, Color.WHITE,
                Color.YELLOW, Color.LIME, Color.FUCHSIA
            };

            @Override
            public void run() {
                if (ticks >= duration) {
                    // ── FINAL BURST ──────────────────────────────────────────
                    EyeType finalEye = EyeType.getRandomStartEye();
                    plugin.getPlayerData().setEye(p, finalEye, false);

                    // Explosion of final eye color
                    Particle.DustOptions finalDust =
                            new Particle.DustOptions(finalEye.color != null ? finalEye.color : Color.WHITE, 2.0f);
                    for (int i = 0; i < 50; i++) {
                        double ang = Math.toRadians(i * 7.2);
                        double r   = 1.5;
                        p.getWorld().spawnParticle(Particle.DUST,
                                p.getLocation().add(
                                        Math.cos(ang) * r,
                                        1 + Math.sin(ang * 2) * 0.8,
                                        Math.sin(ang) * r),
                                1, 0, 0, 0, finalDust);
                    }
                    // Crown ring upward
                    for (int i = 0; i < 24; i++) {
                        double ang = Math.toRadians(i * 15);
                        p.getWorld().spawnParticle(Particle.END_ROD,
                                p.getLocation().add(
                                        Math.cos(ang) * 0.8, 2.0, Math.sin(ang) * 0.8),
                                1, 0, 0.1, 0, 0.05);
                    }

                    p.sendTitle("§6§l" + finalEye.name(), "§a✨ Power Awakened!", 10, 60, 20);
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL,   0.6f, 1.5f);
                    this.cancel();
                    return;
                }

                double progress = (double) ticks / duration; // 0.0 → 1.0

                // ── LAYER 1: Ascending helix (legs → head) ───────────────────
                // Two opposing helixes
                for (int helix = 0; helix < 2; helix++) {
                    double helixAngle = ticks * 0.35 + helix * Math.PI;
                    double yOff = (ticks % 20) * 0.1;
                    double rad  = 0.5 + Math.sin(ticks * 0.05) * 0.15;

                    Color col = flashColors[(colorIndex + helix) % flashColors.length];
                    Particle.DustOptions hDust = new Particle.DustOptions(col, 1.2f);

                    p.getWorld().spawnParticle(Particle.DUST,
                            p.getLocation().add(
                                    Math.cos(helixAngle) * rad,
                                    yOff,
                                    Math.sin(helixAngle) * rad),
                            2, 0, 0, 0, hDust);
                }

                // ── LAYER 2: Eye-level expanding ring ────────────────────────
                if (ticks % 4 == 0) {
                    int ringPoints = 16;
                    double ringR = 0.3 + progress * 1.2;
                    for (int i = 0; i < ringPoints; i++) {
                        double a = Math.toRadians(i * (360.0 / ringPoints)) + ticks * 0.1;
                        Color col = flashColors[(colorIndex + i / 4) % flashColors.length];
                        p.getWorld().spawnParticle(Particle.DUST,
                                p.getEyeLocation().add(
                                        Math.cos(a) * ringR, 0, Math.sin(a) * ringR),
                                1, 0, 0, 0, new Particle.DustOptions(col, 1.0f));
                    }
                }

                // ── LAYER 3: Snowflake & sparkle trail ───────────────────────
                if (ticks % 3 == 0) {
                    p.getWorld().spawnParticle(Particle.SNOWFLAKE,
                            p.getLocation().add(
                                    (rng.nextDouble() - 0.5) * 0.8,
                                    rng.nextDouble() * 2.0,
                                    (rng.nextDouble() - 0.5) * 0.8),
                            1, 0, 0, 0, 0.01);
                    p.getWorld().spawnParticle(Particle.END_ROD,
                            p.getLocation().add(
                                    (rng.nextDouble() - 0.5),
                                    rng.nextDouble() * 2.0,
                                    (rng.nextDouble() - 0.5)),
                            1, 0, 0, 0, 0.02);
                }

                // ── LAYER 4: Soul fire flame crown (top) ─────────────────────
                if (ticks % 5 == 0) {
                    for (int i = 0; i < 4; i++) {
                        double a = Math.toRadians(i * 90 + ticks * 3);
                        p.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                                p.getLocation().add(
                                        Math.cos(a) * 0.3, 2.1, Math.sin(a) * 0.3),
                                1, 0.05, 0.1, 0.05, 0.01);
                    }
                }

                // ── SPINNING TITLE ────────────────────────────────────────────
                if (ticks % 3 == 0) {
                    EyeType displayEye = allBasicEyes[rng.nextInt(allBasicEyes.length)];
                    String[] spinColors = {"§b", "§d", "§e", "§a", "§c", "§f"};
                    String sc = spinColors[rng.nextInt(spinColors.length)];
                    p.sendTitle("§f§l? " + sc + "§l? §f§l?",
                            "§7Destiny: " + sc + "§l" + displayEye.name(),
                            0, 7, 0);
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT,  0.4f, 2f);
                    colorIndex = (colorIndex + 1) % flashColors.length;
                }

                // ── Buildup sound (intensifies over time) ─────────────────────
                if (ticks % 20 == 0) {
                    float pitch = 0.5f + (float) progress * 1.5f;
                    p.playSound(p.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.5f, pitch);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TRADE REQUEST
    // ══════════════════════════════════════════════════════════════════════════
    public void sendTradeRequest(Player sender, Player receiver) {
        pendingTrades.put(sender.getUniqueId(), receiver.getUniqueId());

        TextComponent msg    = new TextComponent("§d" + sender.getName() + " wants to trade Eyes! ");
        TextComponent accept = new TextComponent("§a§l[ACCEPT] ");
        TextComponent reject = new TextComponent("§c§l[REJECT]");
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tradeaccept " + sender.getName()));
        reject.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tradereject " + sender.getName()));
        msg.addExtra(accept);
        msg.addExtra(reject);

        receiver.spigot().sendMessage(msg);
        sender.sendMessage("§5§l[Trade] §fRequest sent to §d" + receiver.getName() + "§f. Waiting...");

        int timeout = plugin.getConfig().getInt("trade.request-timeout", 30);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pendingTrades.containsKey(sender.getUniqueId())) {
                pendingTrades.remove(sender.getUniqueId());
                if (sender.isOnline())   sender.sendMessage("§c[Trade] Request expired.");
                if (receiver.isOnline()) receiver.sendMessage("§c[Trade] Request from " + sender.getName() + " expired.");
            }
        }, (long) timeout * 20L);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TRADE EXECUTE
    // ══════════════════════════════════════════════════════════════════════════
    public void executeTrade(Player sender, Player receiver) {
        if (!pendingTrades.containsKey(sender.getUniqueId())) {
            receiver.sendMessage("§c[Trade] No pending trade request found!");
            return;
        }
        pendingTrades.remove(sender.getUniqueId());

        EyeType sEye = plugin.getPlayerData().getEye(sender);
        EyeType rEye = plugin.getPlayerData().getEye(receiver);

        // Safe TP: receiver ko sender ke paas bhejo agar 100+ blocks dur ho
        if (sender.getWorld().equals(receiver.getWorld()) &&
                sender.getLocation().distanceSquared(receiver.getLocation()) > 100 * 100) {
            Location tpLoc = findSafeNearby(sender);
            receiver.teleport(tpLoc);
            receiver.sendMessage("§5§l[Trade] §fTeleported to " + sender.getName() + "!");
            sender.sendMessage("§5§l[Trade] §f" + receiver.getName() + " teleported to you!");
        }

        runTradeAnimation(sender, receiver, sEye, rEye);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SAFE NEARBY LOCATION
    // ══════════════════════════════════════════════════════════════════════════
    private Location findSafeNearby(Player sender) {
        Location base = sender.getLocation().clone();
        double[] ox = {2.5, -2.5, 0, 0};
        double[] oz = {0, 0, 2.5, -2.5};
        for (int i = 0; i < 4; i++) {
            Location test = base.clone().add(ox[i], 0, oz[i]);
            if (!test.getBlock().getType().isSolid()
                    && !test.clone().add(0,1,0).getBlock().getType().isSolid()) {
                test.setYaw(base.getYaw() + 180);
                return test;
            }
        }
        Location fallback = base.clone().add(sender.getLocation().getDirection().multiply(-2));
        fallback.setYaw(base.getYaw() + 180);
        return fallback;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  MASTERPIECE TRADE ANIMATION  (10 seconds = 200 ticks)
    // ══════════════════════════════════════════════════════════════════════════
    private void runTradeAnimation(Player sender, Player receiver,
                                   EyeType sEye, EyeType rEye) {

        final Location sStartLoc = sender.getLocation().clone();
        final Location rStartLoc = receiver.getLocation().clone();

        // Freeze both
        freezePlayer(sender);
        freezePlayer(receiver);

        // Lift 3 blocks
        sender.addPotionEffect(new PotionEffect(
                PotionEffectType.LEVITATION, 30, 2, false, false, false));
        receiver.addPotionEffect(new PotionEffect(
                PotionEffectType.LEVITATION, 30, 2, false, false, false));

        // Announcement
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage("§8§m================================");
            online.sendMessage("§5§l  ⬡  EYE TRADE  ⬡");
            online.sendMessage("§f  §d" + sender.getName() + " §f↔ §d" + receiver.getName());
            online.sendMessage("§7  Trading their Ancient Eyes...");
            online.sendMessage("§8§m================================");
        }

        sender.sendTitle("§5§l⬡ EYE TRADE ⬡",   "§fBinding Soul Connection...", 10, 220, 10);
        receiver.sendTitle("§5§l⬡ EYE TRADE ⬡", "§fBinding Soul Connection...", 10, 220, 10);

        sender.playSound(sender.getLocation(),     Sound.ENTITY_ENDER_EYE_LAUNCH, 1f, 0.4f);
        receiver.playSound(receiver.getLocation(), Sound.ENTITY_ENDER_EYE_LAUNCH, 1f, 0.4f);
        sender.playSound(sender.getLocation(),     Sound.ENTITY_WITHER_AMBIENT,   0.6f, 0.3f);
        receiver.playSound(receiver.getLocation(), Sound.ENTITY_WITHER_AMBIENT,   0.6f, 0.3f);

        final Color sColor = (sEye != null && sEye.color != null) ? sEye.color : Color.AQUA;
        final Color rColor = (rEye != null && rEye.color != null) ? rEye.color : Color.PURPLE;
        final Particle.DustOptions blackDust  = new Particle.DustOptions(Color.fromRGB(10, 0, 20),   1.3f);
        final Particle.DustOptions redDust    = new Particle.DustOptions(Color.fromRGB(220, 20, 20), 1.1f);
        final Particle.DustOptions purpleDust = new Particle.DustOptions(Color.fromRGB(160, 0, 255), 1.2f);

        new BukkitRunnable() {
            int    ticks     = 0;
            double angle     = 0;
            double cubeAngle = 0;

            @Override
            public void run() {

                // DISCONNECT CHECK — full reset
                if (!sender.isOnline() || !receiver.isOnline()) {
                    cleanupTrade(sender, receiver, sStartLoc, rStartLoc);
                    this.cancel();
                    return;
                }

                angle     += 0.08;
                cubeAngle += 0.04;

                // POSITION LOCK — players ko freeze karo
                if (ticks > 5) {
                    forceLock(sender,   sStartLoc);
                    forceLock(receiver, rStartLoc);
                }

                Location sHover = sStartLoc.clone().add(0, 3.0, 0);
                Location rHover = rStartLoc.clone().add(0, 3.0, 0);
                Location sBody  = sHover.clone().subtract(0, 1.0, 0);
                Location rBody  = rHover.clone().subtract(0, 1.0, 0);

                // 1. PURPLE BEAM — wire connecting 2 players
                drawPurpleBeam(sHover, rHover, angle, purpleDust);

                // 2. BLACK AURA — legs to head, orbiting
                spawnBlackAura(sender,   sBody,  angle,  blackDust);
                spawnBlackAura(receiver, rBody, -angle,  blackDust);

                // 3. RED MAGIC CUBE — accurate rotating cube
                spawnRotatingCube(sBody, cubeAngle,           redDust);
                spawnRotatingCube(rBody, cubeAngle + Math.PI, redDust);

                // 4. EYE COLOR AURA
                spawnEyeAura(sender,   sBody,  angle * 1.5,  new Particle.DustOptions(sColor, 1.0f));
                spawnEyeAura(receiver, rBody, -angle * 1.5,  new Particle.DustOptions(rColor, 1.0f));

                // 5. SOUNDS
                if (ticks % 15 == 0) {
                    float pitch = 0.5f + (ticks / 200f) * 1.5f;
                    sender.playSound(sender.getLocation(),     Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, pitch);
                    receiver.playSound(receiver.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, pitch);
                }
                if (ticks % 40 == 0) {
                    sender.playSound(sender.getLocation(),     Sound.ENTITY_ENDERMAN_AMBIENT, 0.5f, 0.4f);
                    receiver.playSound(receiver.getLocation(), Sound.ENTITY_ENDERMAN_AMBIENT, 0.5f, 0.4f);
                }

                // 6. COUNTDOWN TITLE
                if (ticks % 20 == 0) {
                    int secLeft = (200 - ticks) / 20;
                    String sub = secLeft > 0 ? "§e" + secLeft + "s remaining..." : "§a§lNow!";
                    sender.sendTitle("§5§l⬡ EYE TRADE ⬡",   sub, 0, 25, 0);
                    receiver.sendTitle("§5§l⬡ EYE TRADE ⬡", sub, 0, 25, 0);
                }

                // 7. 200 TICKS = 10 SECONDS — SWAP
                if (ticks >= 200) {
                    this.cancel();
                    performSwap(sender, receiver, sEye, rEye, sStartLoc, rStartLoc);
                    return;
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 5, 1);
    }

    // ── PURPLE BEAM — accurate braided wire ──────────────────────────────────
    private void drawPurpleBeam(Location a, Location b, double angle,
                                Particle.DustOptions purpleDust) {
        World  w     = a.getWorld();
        int    steps = 30;
        double dx    = b.getX() - a.getX();
        double dy    = b.getY() - a.getY();
        double dz    = b.getZ() - a.getZ();

        Vector dir   = new Vector(dx, dy, dz).normalize();
        Vector perp1 = getPerpendicular(dir);
        Vector perp2 = dir.clone().crossProduct(perp1).normalize();

        for (int i = 0; i <= steps; i++) {
            double t    = (double) i / steps;
            Location main = a.clone().add(dx*t, dy*t, dz*t);
            w.spawnParticle(Particle.DUST, main, 1, 0, 0, 0, purpleDust);
            if (i % 3 == 0) w.spawnParticle(Particle.END_ROD, main, 1, 0, 0, 0, 0);

            double waveAmp = 0.10;
            double wave    = angle + t * Math.PI * 4;
            double ox = perp1.getX()*Math.cos(wave)*waveAmp + perp2.getX()*Math.sin(wave)*waveAmp;
            double oy = perp1.getY()*Math.cos(wave)*waveAmp + perp2.getY()*Math.sin(wave)*waveAmp;
            double oz = perp1.getZ()*Math.cos(wave)*waveAmp + perp2.getZ()*Math.sin(wave)*waveAmp;
            w.spawnParticle(Particle.DRAGON_BREATH, main.clone().add(ox,oy,oz), 1, 0,0,0,0);
        }
    }

    // ── BLACK AURA — triple helix, legs to head ───────────────────────────────
    private void spawnBlackAura(Player p, Location body, double angle,
                                Particle.DustOptions blackDust) {
        World w = p.getWorld();
        for (int helix = 0; helix < 3; helix++) {
            double hOff = helix * (Math.PI * 2.0 / 3.0);
            for (int y = 0; y <= 20; y++) {
                double yFrac  = y / 20.0;
                double helixA = angle * 3.0 + hOff + yFrac * Math.PI * 4;
                double radius = 0.55 - yFrac * 0.15;
                Location pt   = body.clone().add(
                        Math.cos(helixA)*radius, yFrac*2.0 - 0.9, Math.sin(helixA)*radius);
                w.spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, blackDust);
            }
        }
        // Mid ring
        for (int i = 0; i < 16; i++) {
            double a  = angle*2.0 + Math.toRadians(i*(360.0/16));
            Location pt = body.clone().add(Math.cos(a)*0.7, 0.1, Math.sin(a)*0.7);
            w.spawnParticle(Particle.SMOKE, pt, 1, 0, 0, 0, 0.01);
        }
        // Crown
        for (int i = 0; i < 10; i++) {
            double a  = -angle*2.5 + Math.toRadians(i*(360.0/10));
            Location pt = body.clone().add(Math.cos(a)*0.4, 1.7, Math.sin(a)*0.4);
            w.spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, blackDust);
        }
    }

    // ── RED MAGIC CUBE — 8 vertices, 12 edges, rotating ─────────────────────
    private void spawnRotatingCube(Location center, double cubeAngle,
                                   Particle.DustOptions redDust) {
        World  w   = center.getWorld();
        double r   = 1.1;
        int    pts = 8;
        double[][] verts = {
            {-r,-r,-r},{r,-r,-r},{r,r,-r},{-r,r,-r},
            {-r,-r, r},{r,-r, r},{r,r, r},{-r,r, r}
        };
        int[][] edges = {
            {0,1},{1,2},{2,3},{3,0},
            {4,5},{5,6},{6,7},{7,4},
            {0,4},{1,5},{2,6},{3,7}
        };
        double cosA = Math.cos(cubeAngle), sinA = Math.sin(cubeAngle);
        double cosT = Math.cos(0.35),      sinT = Math.sin(0.35);
        double[][] rv = new double[8][3];
        for (int v = 0; v < 8; v++) {
            double x = verts[v][0], y = verts[v][1], z = verts[v][2];
            double nx  = x*cosA - z*sinA;
            double nz  = x*sinA + z*cosA;
            double ny2 = y*cosT - nz*sinT;
            double nz2 = y*sinT + nz*cosT;
            rv[v][0]=nx; rv[v][1]=ny2+r; rv[v][2]=nz2;
        }
        for (int[] edge : edges) {
            double[] va = rv[edge[0]], vb = rv[edge[1]];
            for (int k = 0; k <= pts; k++) {
                double t  = (double)k/pts;
                Location pt = center.clone().add(
                        va[0]+(vb[0]-va[0])*t,
                        va[1]+(vb[1]-va[1])*t - r,
                        va[2]+(vb[2]-va[2])*t);
                w.spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, redDust);
                if (k%3==0) w.spawnParticle(Particle.CRIT, pt, 1, 0.02, 0.02, 0.02, 0);
            }
        }
    }

    // ── EYE COLOR AURA ────────────────────────────────────────────────────────
    private void spawnEyeAura(Player p, Location body, double angle,
                              Particle.DustOptions dust) {
        World w = p.getWorld();
        for (int i = 0; i < 20; i++) {
            double a    = angle + Math.toRadians(i*(360.0/20));
            double pulsR = 0.85 + Math.sin(angle*3+i)*0.1;
            Location pt = body.clone().add(Math.cos(a)*pulsR, 0.5, Math.sin(a)*pulsR);
            w.spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, dust);
        }
    }

    // ── FORCE LOCK ────────────────────────────────────────────────────────────
    private void forceLock(Player p, Location startLoc) {
        if (!p.isOnline()) return;
        Location cur = p.getLocation();
        double targetY = startLoc.getY() + 3.0;
        double distXZ  = Math.sqrt(
                Math.pow(cur.getX()-startLoc.getX(),2)+Math.pow(cur.getZ()-startLoc.getZ(),2));
        if (distXZ > 0.5 || Math.abs(cur.getY()-targetY) > 1.0) {
            Location lock = startLoc.clone();
            lock.setY(targetY);
            lock.setYaw(cur.getYaw());
            lock.setPitch(cur.getPitch());
            p.teleport(lock);
        }
        p.setVelocity(new Vector(0,0,0));
    }

    // ── FREEZE ────────────────────────────────────────────────────────────────
    private void freezePlayer(Player p) {
        p.setWalkSpeed(0f);
        p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 300, 128, false, false, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,   300, 10,  false, false, false));
    }

    private void unfreezePlayer(Player p) {
        if (!p.isOnline()) return;
        p.setWalkSpeed(0.2f);
        p.removePotionEffect(PotionEffectType.JUMP_BOOST);
        p.removePotionEffect(PotionEffectType.SLOWNESS);
        p.removePotionEffect(PotionEffectType.LEVITATION);
        p.setVelocity(new Vector(0,0,0));
    }

    // ── CLEANUP — disconnect ya cancel ────────────────────────────────────────
    private void cleanupTrade(Player sender, Player receiver,
                              Location sStartLoc, Location rStartLoc) {
        unfreezePlayer(sender);
        unfreezePlayer(receiver);
        if (sender.isOnline()) {
            Location sg = sStartLoc.clone();
            sg.setYaw(sender.getLocation().getYaw());
            sender.teleport(sg);
            sender.sendMessage("§c[Trade] Animation cancelled — connection lost.");
        }
        if (receiver.isOnline()) {
            Location rg = rStartLoc.clone();
            rg.setYaw(receiver.getLocation().getYaw());
            receiver.teleport(rg);
            receiver.sendMessage("§c[Trade] Animation cancelled — connection lost.");
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage("§c§l[AncientEye] §7Trade between §d"
                    + sender.getName() + " §7and §d" + receiver.getName() + " §7was interrupted.");
        }
    }

    // ── PERFORM SWAP ──────────────────────────────────────────────────────────
    private void performSwap(Player sender, Player receiver,
                             EyeType sEye, EyeType rEye,
                             Location sStartLoc, Location rStartLoc) {
        plugin.getPlayerData().setEye(sender,   rEye, true);
        plugin.getPlayerData().setEye(receiver, sEye, true);

        unfreezePlayer(sender);
        unfreezePlayer(receiver);

        if (sender.isOnline()) {
            Location sg = sStartLoc.clone();
            sg.setYaw(sender.getLocation().getYaw());
            sender.teleport(sg);
        }
        if (receiver.isOnline()) {
            Location rg = rStartLoc.clone();
            rg.setYaw(receiver.getLocation().getYaw());
            receiver.teleport(rg);
        }

        for (Player pl : new Player[]{sender, receiver}) {
            if (!pl.isOnline()) continue;
            pl.getWorld().strikeLightningEffect(pl.getLocation());
            Random rng = new Random();
            for (int i = 0; i < 60; i++) {
                pl.getWorld().spawnParticle(Particle.END_ROD, pl.getLocation().add(0,1,0), 1,
                        (rng.nextDouble()-0.5)*2, rng.nextDouble()*2.5,
                        (rng.nextDouble()-0.5)*2, 0.12);
                pl.getWorld().spawnParticle(Particle.DRAGON_BREATH, pl.getLocation().add(0,1,0), 1,
                        (rng.nextDouble()-0.5)*1.5, rng.nextDouble()*2,
                        (rng.nextDouble()-0.5)*1.5, 0.04);
            }
            pl.playSound(pl.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.0f);
            pl.playSound(pl.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME,  1f, 1.5f);
            pl.playSound(pl.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL,   0.5f, 1.6f);
            pl.sendTitle("§5§l✨ TRADE COMPLETE!",
                    "§fYour Eye: §e§l" + plugin.getPlayerData().getEye(pl).name(),
                    10, 100, 20);
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage("§8§m================================");
            online.sendMessage("§5§l  ✨  TRADE SUCCESSFUL!");
            online.sendMessage("§f  §d" + sender.getName()   + " §7→ §e" + rEye.name());
            online.sendMessage("§f  §d" + receiver.getName() + " §7→ §e" + sEye.name());
            online.sendMessage("§8§m================================");
        }
    }

    // ── MATH: perpendicular vector ─────────────────────────────────────────────
    private Vector getPerpendicular(Vector v) {
        Vector ref = Math.abs(v.getX()) < 0.9 ? new Vector(1,0,0) : new Vector(0,1,0);
        return v.clone().crossProduct(ref).normalize();
    }

        // ── Helper ────────────────────────────────────────────────────────────────
    private EyeType[] getBasicEyes() {
        java.util.List<EyeType> list = new java.util.ArrayList<>();
        for (EyeType t : EyeType.values()) {
            if (t != EyeType.NONE && !t.isEventEye()) list.add(t);
        }
        return list.toArray(new EyeType[0]);
    }
}
