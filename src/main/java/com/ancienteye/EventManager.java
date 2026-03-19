package com.ancienteye;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scoreboard.*;

import java.util.*;

public class EventManager implements Listener {
    private final AncientEyePlugin plugin;
    private boolean active = false;
    private EyeType reward = EyeType.NONE;
    private final Map<UUID, Integer> taskProgress = new HashMap<>();
    private final Map<UUID, Integer> blockCount = new HashMap<>();

    public EventManager(AncientEyePlugin plugin) { this.plugin = plugin; plugin.getServer().getPluginManager().registerEvents(this, plugin); }

    public void startEvent(EyeType eye) {
        this.active = true;
        this.reward = eye;
        taskProgress.clear();
        blockCount.clear();

        Bukkit.broadcastMessage("§8§m---------------------------------");
        Bukkit.broadcastMessage("§6§lEYE EVENT: §e§l" + eye.name());
        Bukkit.broadcastMessage("§fComplete 3 tasks to win the Eye!");
        Bukkit.broadcastMessage("§8§m---------------------------------");

        for (Player p : Bukkit.getOnlinePlayers()) {
            taskProgress.put(p.getUniqueId(), 1);
            updateScoreboard(p, "Kill 1 Player");
        }
    }

    private void updateScoreboard(Player p, String taskDesc) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = manager.getNewScoreboard();
        Objective obj = board.registerNewObjective("event", "dummy", "§6§l" + reward.name() + " EVENT");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        obj.getScore("§7----------------").setScore(4);
        obj.getScore("§fCurrent Task:").setScore(3);
        obj.getScore("§e" + taskDesc).setScore(2);
        obj.getScore("§7Progress: §a" + taskProgress.get(p.getUniqueId()) + "/3").setScore(1);

        p.setScoreboard(board);
    }

    @EventHandler
    public void onKill(PlayerDeathEvent e) {
        if (!active || e.getEntity().getKiller() == null) return;
        Player killer = e.getEntity().getKiller();
        if (taskProgress.getOrDefault(killer.getUniqueId(), 0) == 1) {
            advanceTask(killer, "Break 10 Diamonds");
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (!active) return;
        Player p = e.getPlayer();
        if (taskProgress.getOrDefault(p.getUniqueId(), 0) == 2 && e.getBlock().getType() == Material.DIAMOND_ORE) {
            int count = blockCount.getOrDefault(p.getUniqueId(), 0) + 1;
            blockCount.put(p.getUniqueId(), count);
            if (count >= 10) advanceTask(p, "Survive 2 Minutes");
        }
    }

    private void advanceTask(Player p, String nextTask) {
        int current = taskProgress.get(p.getUniqueId());
        if (current == 3) {
            win(p);
        } else {
            taskProgress.put(p.getUniqueId(), current + 1);
            p.sendTitle("§aTASK COMPLETE", "§fNext: " + nextTask, 10, 40, 10);
            updateScoreboard(p, nextTask);
            
            if (current + 1 == 3) { // Survival Task
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (active && taskProgress.get(p.getUniqueId()) == 3) win(p);
                }, 2400L); // 2 Minutes
            }
        }
    }

    private void win(Player p) {
        if (!active) return;
        active = false;
        Bukkit.broadcastMessage("§6§l🏆 " + p.getName() + " has won the " + reward.name() + " Event!");
        plugin.getDataManager().setEye(p, reward, false);
        for (Player online : Bukkit.getOnlinePlayers()) online.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
    }
}
