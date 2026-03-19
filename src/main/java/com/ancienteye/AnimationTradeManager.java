package com.ancienteye;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;

public class AnimationTradeManager {
    private final Map<UUID, UUID> pendingTrades = new HashMap<>();
    private final AncientEyePlugin plugin;

    public AnimationTradeManager(AncientEyePlugin plugin) {
        this.plugin = plugin;
    }

    // --- /smpstart Ritual Animation ---
    public void startSmpRitual(Player p) {
        // Config se duration uthao (Default: 6 seconds = 120 ticks)
        int duration = 120; 

        new BukkitRunnable() {
            int ticks = 0;
            final EyeType[] allBasicEyes = getBasicEyes();
            final Random random = new Random();

            @Override
            public void run() {
                if (ticks >= duration) {
                    // Final Eye Reward
                    EyeType finalEye = EyeType.getRandomStartEye();
                    plugin.getPlayerData().setEye(p, finalEye, false);
                    
                    p.sendTitle("§6§l" + finalEye.name(), "§aPower Awakened!", 10, 40, 10);
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1.2f);
                    this.cancel();
                    return;
                }

                // 1. WAVE PARTICLE: Pair se Sar tak (Leg to Head)
                double yOffset = (ticks % 20) * 0.1; // 0 se 2 blocks tak jayega cycle mein
                double angle = ticks * 0.4;
                double x = Math.cos(angle) * 0.6;
                double z = Math.sin(angle) * 0.6;

                // Blue & White Particles
                p.getWorld().spawnParticle(Particle.DUST, p.getLocation().add(x, yOffset, z), 2, 0, 0, 0, new Particle.DustOptions(org.bukkit.Color.AQUA, 1));
                p.getWorld().spawnParticle(Particle.SNOWFLAKE, p.getLocation().add(-x, yOffset, -z), 1, 0, 0, 0, 0.01);

                // 2. SPINNING TITLE: Random names flashing
                if (ticks % 3 == 0) {
                    EyeType displayEye = allBasicEyes[random.nextInt(allBasicEyes.length)];
                    p.sendTitle("§b§l? ? ?", "§fDestiny: §7" + displayEye.name(), 0, 7, 0);
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 2f);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    // Helper to get only basic eyes for the spin
    private EyeType[] getBasicEyes() {
        List<EyeType> list = new ArrayList<>();
        for (EyeType t : EyeType.values()) {
            if (t != EyeType.NONE && !t.isEventEye()) list.add(t);
        }
        return list.toArray(new EyeType[0]);
    }

    // --- TRADE LOGIC (Rest of your code remains same) ---
    public void sendTradeRequest(Player sender, Player receiver) {
        pendingTrades.put(sender.getUniqueId(), receiver.getUniqueId());
        
        TextComponent msg = new TextComponent("§d" + sender.getName() + " wants to trade Eyes! ");
        TextComponent accept = new TextComponent("§a§l[ACCEPT] ");
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tradeaccept " + sender.getName()));
        
        TextComponent reject = new TextComponent("§c§l[REJECT]");
        reject.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tradereject " + sender.getName()));

        msg.addExtra(accept); msg.addExtra(reject);
        receiver.spigot().sendMessage(msg);
        Bukkit.getScheduler().runTaskLater(plugin, () -> pendingTrades.remove(sender.getUniqueId()), 600L);
    }

    public void executeTrade(Player sender, Player receiver) {
        if (!pendingTrades.containsKey(sender.getUniqueId())) return;
        pendingTrades.remove(sender.getUniqueId());

        EyeType sEye = plugin.getPlayerData().getEye(sender);
        EyeType rEye = plugin.getPlayerData().getEye(receiver);

        sender.setWalkSpeed(0); receiver.setWalkSpeed(0);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getPlayerData().setEye(sender, rEye, true);
                plugin.getPlayerData().setEye(receiver, sEye, true);
                sender.setWalkSpeed(0.2f); receiver.setWalkSpeed(0.2f);
                sender.playSound(sender.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1, 1);
                receiver.playSound(receiver.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1, 1);
            }
        }.runTaskLater(plugin, 40L);
    }
}
