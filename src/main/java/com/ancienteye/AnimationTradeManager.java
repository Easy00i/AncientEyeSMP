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
        Bukkit.getScheduler().runTaskLater(plugin, () -> pendingTrades.remove(sender.getUniqueId()), 600L);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TRADE EXECUTE  — 5s animation then swap
    // ══════════════════════════════════════════════════════════════════════════
    public void executeTrade(Player sender, Player receiver) {
        if (!pendingTrades.containsKey(sender.getUniqueId())) return;
        pendingTrades.remove(sender.getUniqueId());

        EyeType sEye = plugin.getPlayerData().getEye(sender);
        EyeType rEye = plugin.getPlayerData().getEye(receiver);

        // ── If players are > 50 blocks apart, tp receiver to sender ──────────
        if (sender.getWorld().equals(receiver.getWorld()) &&
                sender.getLocation().distanceSquared(receiver.getLocation()) > 50 * 50) {
            // Face sender from 2 blocks away
            Location tpLoc = sender.getLocation().clone().add(
                    sender.getLocation().getDirection().multiply(-2));
            tpLoc.setYaw(receiver.getLocation().getYaw());
            tpLoc.setPitch(receiver.getLocation().getPitch());
            receiver.teleport(tpLoc);
            receiver.sendMessage("§5§l[Trade] §fTumhe sender ke paas teleport kiya gaya.");
        }

        runTradeAnimation(sender, receiver, sEye, rEye);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TRADE ANIMATION  (5 seconds = 100 ticks)
    // ══════════════════════════════════════════════════════════════════════════
    private void runTradeAnimation(Player sender, Player receiver,
                                   EyeType sEye, EyeType rEye) {

        // ── Freeze both players ───────────────────────────────────────────────
        sender.setWalkSpeed(0f);
        receiver.setWalkSpeed(0f);
        sender.addPotionEffect(new PotionEffect(
                PotionEffectType.JUMP_BOOST, 120, 128, false, false, false)); // cancel jumping
        receiver.addPotionEffect(new PotionEffect(
                PotionEffectType.JUMP_BOOST, 120, 128, false, false, false));

        // ── Levitate 2 blocks ─────────────────────────────────────────────────
        sender.addPotionEffect(new PotionEffect(
                PotionEffectType.LEVITATION, 5, 1, false, false, false));
        receiver.addPotionEffect(new PotionEffect(
                PotionEffectType.LEVITATION, 5, 1, false, false, false));

        // Title for both
        sender.sendTitle("§5§l⬡ EYE TRADE ⬡",
                "§fBinding Connection...", 10, 200, 10);
        receiver.sendTitle("§5§l⬡ EYE TRADE ⬡",
                "§fBinding Connection...", 10, 200, 10);

        // Sounds
        sender.playSound(sender.getLocation(), Sound.ENTITY_ENDER_EYE_LAUNCH,   1f, 0.5f);
        receiver.playSound(receiver.getLocation(), Sound.ENTITY_ENDER_EYE_LAUNCH, 1f, 0.5f);

        final Color sColor = sEye.color != null ? sEye.color : Color.AQUA;
        final Color rColor = rEye.color != null ? rEye.color : Color.PURPLE;

        new BukkitRunnable() {
            int ticks = 0;
            double shieldAngle = 0;

            @Override
            public void run() {

                // Safety: one of them left
                if (!sender.isOnline() || !receiver.isOnline()) {
                    cleanupTrade(sender, receiver);
                    this.cancel();
                    return;
                }

                shieldAngle += 0.2;

                Location sEyeLoc = sender.getEyeLocation();
                Location rEyeLoc = receiver.getEyeLocation();

                // ── PURPLE CONNECTING LINE between eyes ───────────────────────
                drawLine(sEyeLoc, rEyeLoc, ticks);

                // ── AURA around each player ───────────────────────────────────
                spawnAura(sender, sColor, shieldAngle,  1.0);
                spawnAura(receiver, rColor, -shieldAngle, 1.0);

                // ── SHIELD DOME around each player ────────────────────────────
                spawnShieldDome(sender,   sColor, shieldAngle);
                spawnShieldDome(receiver, rColor, shieldAngle + Math.PI);

                // ── Pulse sound ───────────────────────────────────────────────
                if (ticks % 15 == 0) {
                    float pitch = 0.8f + ticks * 0.005f;
                    sender.playSound(sender.getLocation(),
                            Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.6f, pitch);
                    receiver.playSound(receiver.getLocation(),
                            Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.6f, pitch);
                }

                // ── Countdown titles ──────────────────────────────────────────
                if (ticks % 20 == 0 && ticks < 100) {
                    int secLeft = (100 - ticks) / 20;
                    String countStr = secLeft > 0 ? "§e" + secLeft + "s..." : "§a§lNow!";
                    sender.sendTitle("§5§l⬡ EYE TRADE ⬡", countStr, 0, 25, 0);
                    receiver.sendTitle("§5§l⬡ EYE TRADE ⬡", countStr, 0, 25, 0);
                }

                // ── 100 ticks = 5 seconds — SWAP ─────────────────────────────
                if (ticks >= 100) {
                    this.cancel();
                    performSwap(sender, receiver, sEye, rEye);
                    return;
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    // ── Line: lerp particles from sEyeLoc to rEyeLoc ─────────────────────────
    private void drawLine(Location a, Location b, int ticks) {
        int steps = 20;
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            // Slight sine wave on Y for dramatic look
            double yWave = Math.sin(ticks * 0.15 + t * Math.PI) * 0.15;
            Location point = a.clone().add(
                    (b.getX() - a.getX()) * t,
                    (b.getY() - a.getY()) * t + yWave,
                    (b.getZ() - a.getZ()) * t);

            // Alternate purple and white
            Particle pType = (i % 3 == 0) ? Particle.END_ROD : Particle.DRAGON_BREATH;
            a.getWorld().spawnParticle(pType, point, 1, 0, 0, 0, 0);
        }
    }

    // ── Aura: fast rotating ring around player ────────────────────────────────
    private void spawnAura(Player p, Color col, double angle, double radius) {
        Particle.DustOptions dust = new Particle.DustOptions(col, 1.2f);
        int points = 12;
        for (int i = 0; i < points; i++) {
            double a = angle + Math.toRadians(i * (360.0 / points));
            Location loc = p.getLocation().add(
                    Math.cos(a) * radius, 1.0, Math.sin(a) * radius);
            p.getWorld().spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, dust);
        }
        // Eye-level inner ring
        for (int i = 0; i < 8; i++) {
            double a = -angle + Math.toRadians(i * 45);
            Location loc = p.getEyeLocation().add(
                    Math.cos(a) * 0.55, 0, Math.sin(a) * 0.55);
            p.getWorld().spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, dust);
        }
    }

    // ── Shield dome: hemisphere of particles around player ────────────────────
    private void spawnShieldDome(Player p, Color col, double offset) {
        Particle.DustOptions dust = new Particle.DustOptions(col, 0.9f);
        double r = 1.4;
        int latitudes = 3, longitudes = 10;
        for (int lat = 0; lat < latitudes; lat++) {
            double phi = Math.PI / 4 * lat; // 0°, 45°, 90°
            double yr  = Math.sin(phi) * r;
            double xzR = Math.cos(phi) * r;
            for (int lon = 0; lon < longitudes; lon++) {
                double theta = offset + Math.toRadians(lon * (360.0 / longitudes));
                Location loc = p.getLocation().add(
                        Math.cos(theta) * xzR, 0.5 + yr, Math.sin(theta) * xzR);
                p.getWorld().spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, dust);
            }
        }
        // Bottom ring
        for (int i = 0; i < 10; i++) {
            double a = offset + Math.toRadians(i * 36);
            p.getWorld().spawnParticle(Particle.DUST,
                    p.getLocation().add(Math.cos(a) * r, 0.15, Math.sin(a) * r),
                    1, 0, 0, 0, dust);
        }
    }

    // ── Final swap: particles burst + sound + give eyes ──────────────────────
    private void performSwap(Player sender, Player receiver,
                             EyeType sEye, EyeType rEye) {
        plugin.getPlayerData().setEye(sender, rEye, true);
        plugin.getPlayerData().setEye(receiver, sEye, true);

        cleanupTrade(sender, receiver);

        // Burst at each player
        for (Player p : new Player[]{sender, receiver}) {
            if (!p.isOnline()) continue;
            p.getWorld().strikeLightningEffect(p.getLocation());
            for (int i = 0; i < 30; i++) {
                p.getWorld().spawnParticle(Particle.END_ROD,
                        p.getLocation().add(0, 1, 0),
                        1,
                        (Math.random() - 0.5) * 2,
                        Math.random() * 2,
                        (Math.random() - 0.5) * 2, 0.1);
                p.getWorld().spawnParticle(Particle.DRAGON_BREATH,
                        p.getLocation().add(0, 1, 0),
                        1,
                        (Math.random() - 0.5) * 1.5,
                        Math.random() * 1.5,
                        (Math.random() - 0.5) * 1.5, 0.05);
            }
            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.2f);
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE,  1f, 1.0f);
            p.sendTitle("§5§l✨ Trade Complete!",
                    "§fTumhara new eye: §e§l" + plugin.getPlayerData().getEye(p).name(),
                    10, 80, 20);
        }
    }

    // ── Unfreeze both players ─────────────────────────────────────────────────
    private void cleanupTrade(Player sender, Player receiver) {
        for (Player p : new Player[]{sender, receiver}) {
            if (!p.isOnline()) continue;
            p.setWalkSpeed(0.2f);
            p.removePotionEffect(PotionEffectType.JUMP_BOOST);
            p.removePotionEffect(PotionEffectType.LEVITATION);
        }
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
