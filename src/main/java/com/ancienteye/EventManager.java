package com.ancienteye;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scoreboard.*;

import java.util.*;

public class EventManager implements Listener {
    private final AncientEyePlugin plugin;
    private boolean active = false;
    private EyeType reward = EyeType.NONE;

    // Data Storage
    private final Map<UUID, Integer> taskProgress = new HashMap<>(); // 1, 2, or 3
    private final Map<UUID, List<String>> playerTasks = new HashMap<>(); // Player's unique 3 tasks
    private final Map<UUID, Integer> blockCount = new HashMap<>();

    public EventManager(AncientEyePlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void startEvent(EyeType eye) {
        this.active = true;
        this.reward = eye;
        taskProgress.clear();
        playerTasks.clear();
        blockCount.clear();

        // Config se tasks load karna
        List<String> allTasks = plugin.getConfig().getStringList("event-settings.tasks");
        if (allTasks.isEmpty()) {
            Bukkit.getLogger().warning("Config mein tasks nahi mile! Default use kar raha hoon.");
            allTasks = Arrays.asList("Kill 1 Player", "Break 10 Diamonds", "Survive 2 Minutes");
        }

        Bukkit.broadcastMessage("§8§m---------------------------------");
        Bukkit.broadcastMessage("§6§lEYE EVENT: §e§l" + eye.name());
        Bukkit.broadcastMessage("§fHar player ko 3 random tasks mile hain! Check Scoreboard.");
        Bukkit.broadcastMessage("§8§m---------------------------------");

        for (Player p : Bukkit.getOnlinePlayers()) {
            assignTasksToPlayer(p, allTasks);
        }
    }

    private void assignTasksToPlayer(Player p, List<String> allTasks) {
        List<String> copy = new ArrayList<>(allTasks);
        Collections.shuffle(copy);
        
        // 3 Random tasks select karna
        List<String> selected = copy.subList(0, Math.min(3, copy.size()));
        
        playerTasks.put(p.getUniqueId(), selected);
        taskProgress.put(p.getUniqueId(), 1);
        updateScoreboard(p, selected.get(0));
    }

    private void updateScoreboard(Player p, String taskDesc) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = manager.getNewScoreboard();
        Objective obj = board.registerNewObjective("event", "dummy", "§6§l" + reward.name() + " EVENT");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        obj.getScore("§7----------------").setScore(4);
        obj.getScore("§fTask " + taskProgress.getOrDefault(p.getUniqueId(), 1) + "/3:").setScore(3);
        obj.getScore("§e" + taskDesc).setScore(2);
        obj.getScore("§7-----------------").setScore(1);

        p.setScoreboard(board);
    }

    @EventHandler
    public void onKill(PlayerDeathEvent e) {
        if (!active || e.getEntity().getKiller() == null) return;
        handleTaskCompletion(e.getEntity().getKiller(), "Kill 1 Player");
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (!active) return;
        Player p = e.getPlayer();
        String currentTask = getCurrentTaskName(p);

        // Diamond Task Logic
        if (currentTask.contains("Diamonds") && (e.getBlock().getType() == Material.DIAMOND_ORE || e.getBlock().getType() == Material.DEEPSLATE_DIAMOND_ORE)) {
            int count = blockCount.getOrDefault(p.getUniqueId(), 0) + 1;
            blockCount.put(p.getUniqueId(), count);
            if (count >= 10) handleTaskCompletion(p, currentTask);
        }
        
        // Iron Task Logic
        if (currentTask.contains("Iron Ore") && (e.getBlock().getType() == Material.IRON_ORE || e.getBlock().getType() == Material.DEEPSLATE_IRON_ORE)) {
            int count = blockCount.getOrDefault(p.getUniqueId(), 0) + 1;
            blockCount.put(p.getUniqueId(), count);
            if (count >= 64) handleTaskCompletion(p, currentTask);
        }
    }

    private void handleTaskCompletion(Player p, String goal) {
        if (getCurrentTaskName(p).equals(goal)) {
            advanceTask(p);
        }
    }

    private void advanceTask(Player p) {
        int current = taskProgress.get(p.getUniqueId());
        blockCount.put(p.getUniqueId(), 0); // Reset for next task

        if (current >= 3) {
            win(p);
        } else {
            taskProgress.put(p.getUniqueId(), current + 1);
            String nextTask = playerTasks.get(p.getUniqueId()).get(current);
            
            p.sendTitle("§aTASK COMPLETE", "§fNext: " + nextTask, 10, 40, 10);
            updateScoreboard(p, nextTask);

            // Special Survival Task Logic
            if (nextTask.contains("Survive")) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (active && p.isOnline() && getCurrentTaskName(p).equals(nextTask)) {
                        advanceTask(p);
                    }
                }, 2400L);
            }
        }
    }

    private String getCurrentTaskName(Player p) {
        if (!playerTasks.containsKey(p.getUniqueId())) return "";
        return playerTasks.get(p.getUniqueId()).get(taskProgress.get(p.getUniqueId()) - 1);
    }

    private void win(Player p) {
        if (!active) return;
        active = false;
        Bukkit.broadcastMessage("§6§l🏆 " + p.getName() + " has won the " + reward.name() + " Event!");
        plugin.getPlayerData().setEye(p, reward, false);
        for (Player online : Bukkit.getOnlinePlayers()) online.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (active && playerTasks.containsKey(p.getUniqueId())) {
            updateScoreboard(p, getCurrentTaskName(p));
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        if (active && playerTasks.containsKey(p.getUniqueId())) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline()) updateScoreboard(p, getCurrentTaskName(p));
            }, 1L);
        }
    }
}
