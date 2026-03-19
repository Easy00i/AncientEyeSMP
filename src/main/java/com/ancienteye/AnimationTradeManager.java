package com.ancienteye;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AnimationTradeManager {
    // Sender -> Receiver
    private final Map<UUID, UUID> pendingTrades = new HashMap<>();

    private final AncientEyePlugin plugin;
public AnimationTradeManager(AncientEyePlugin plugin) {
    this.plugin = plugin;
}

    public void startSmpRitual(Player p) {
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks > 60) {
                    EyeType randomEye = EyeType.values()[new java.util.Random().nextInt(18) + 1]; // Avoid NONE and Events
                    AncientEyePlugin.get().getPlayerData().setEye(p, randomEye, false);
                    this.cancel();
                    return;
                }
                double angle = ticks * 0.5;
                p.spawnParticle(Particle.ENCHANT, p.getLocation().add(Math.cos(angle)*1.5, 1, Math.sin(angle)*1.5), 5, 0,0,0,0);
                p.sendTitle("§5§lAWAKENING", "§7Spinning destiny...", 0, 5, 0);
                ticks++;
            }
        }.runTaskTimer(AncientEyePlugin.get(), 0, 1);
    }

    public void sendTradeRequest(Player sender, Player receiver) {
        pendingTrades.put(sender.getUniqueId(), receiver.getUniqueId());
        sender.sendMessage("§aTrade request sent to " + receiver.getName());

        TextComponent msg = new TextComponent("§d" + sender.getName() + " wants to trade Eyes! ");
        TextComponent accept = new TextComponent("§a§l[ACCEPT] ");
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tradeaccept " + sender.getName()));
        
        TextComponent reject = new TextComponent("§c§l[REJECT]");
        reject.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tradereject " + sender.getName()));

        msg.addExtra(accept);
        msg.addExtra(reject);
        receiver.spigot().sendMessage(msg);

        // 30 Seconds timeout
        Bukkit.getScheduler().runTaskLater(AncientEyePlugin.get(), () -> pendingTrades.remove(sender.getUniqueId()), 600L);
    }

    public void executeTrade(Player sender, Player receiver) {
        if (!pendingTrades.containsKey(sender.getUniqueId()) || !pendingTrades.get(sender.getUniqueId()).equals(receiver.getUniqueId())) return;
        
        pendingTrades.remove(sender.getUniqueId());

        EyeType senderEye = AncientEyePlugin.get().getPlayerData().getEye(sender);
        EyeType receiverEye = AncientEyePlugin.get().getPlayerData().getEye(receiver);

        // Freeze and Animation
        sender.setWalkSpeed(0); receiver.setWalkSpeed(0);
        sender.sendTitle("§e§lSWAPPING", "§fPlease wait...", 10, 40, 10);
        receiver.sendTitle("§e§lSWAPPING", "§fPlease wait...", 10, 40, 10);

        new BukkitRunnable() {
            @Override
            public void run() {
                AncientEyePlugin.get().getPlayerData().setEye(sender, receiverEye, true);
                AncientEyePlugin.get().getPlayerData().setEye(receiver, senderEye, true);
                
                sender.setWalkSpeed(0.2f); receiver.setWalkSpeed(0.2f);
                sender.playSound(sender.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1, 1);
                receiver.playSound(receiver.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1, 1);
            }
        }.runTaskLater(AncientEyePlugin.get(), 40L); // 2 Seconds Freeze
    }
}

