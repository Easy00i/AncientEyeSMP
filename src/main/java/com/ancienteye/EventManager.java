package com.ancienteye;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EventManager implements Listener {
    private boolean isEventActive = false;
    private EyeType eventReward = EyeType.NONE;
    
    // UUID -> Current Task Number (1, 2, or 3)
    private final Map<UUID, Integer> playerTaskProgress = new HashMap<>();

    public void startEvent(EyeType reward) {
        isEventActive = true;
        eventReward = reward;
        playerTaskProgress.clear();
        
        Bukkit.broadcastMessage("§8[§b§lEVENT§8] §e" + reward.name() + " Eye Event has started!");
        Bukkit.getOnlinePlayers().forEach(p -> {
            p.sendTitle("§6§l" + reward.name() + " EVENT", "§fTask 1: Kill a Player!", 10, 60, 10);
            playerTaskProgress.put(p.getUniqueId(), 1); // Everyone starts at Task 1
        });
    }

    public void completeTask(Player p, int completedTaskNumber) {
        if (!isEventActive) return;
        int currentTask = playerTaskProgress.getOrDefault(p.getUniqueId(), 1);
        
        if (currentTask == completedTaskNumber) {
            if (currentTask == 3) {
                // WON THE EVENT
                isEventActive = false;
                Bukkit.broadcastMessage("§8[§b§lEVENT§8] §a§l🏆 " + p.getName() + " has won the " + eventReward.name() + " Event!");
                AncientEyePlugin.get().getPlayerData().setEye(p, eventReward, false);
            } else {
                // MOVE TO NEXT TASK
                playerTaskProgress.put(p.getUniqueId(), currentTask + 1);
                String nextTask = currentTask == 1 ? "Task 2: Break 10 Diamond Ores!" : "Task 3: Survive for 5 mins (Not dying)!";
                p.sendMessage("§aTask " + currentTask + " Complete! Next: " + nextTask);
                p.sendTitle("§a§lTASK COMPLETE", "§e" + nextTask, 10, 50, 10);
            }
        }
    }

    // --- TASK TRIGGERS ---
    @EventHandler
    public void onPlayerKill(PlayerDeathEvent e) {
        if (!isEventActive) return;
        if (e.getEntity().getKiller() != null) {
            completeTask(e.getEntity().getKiller(), 1); // Task 1 is killing a player
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (!isEventActive) return;
        // Logic to track broken blocks for Task 2 can go here
        // If broken 10 diamonds -> completeTask(e.getPlayer(), 2);
    }
}
