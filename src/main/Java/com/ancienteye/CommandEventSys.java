package com.ancienteye;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.util.*;

public class CommandEventSys implements CommandExecutor, TabCompleter, Listener {
    private final AncientEyePlugin plugin;
    private final Map<UUID, UUID> pendingTrades = new HashMap<>();
    
    // Event Data
    private boolean eventActive = false;
    private EyeManager.Eye eventReward = null;
    private final List<String> eventTasks = Arrays.asList("KILL_PLAYER", "MINE_BLOCKS", "SURVIVE");
    private final Map<UUID, Integer> eventProgress = new HashMap<>();

    public CommandEventSys(AncientEyePlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;

        if (cmd.getName().equalsIgnoreCase("smpstart")) {
            if (p.getPersistentDataContainer().has(plugin.getEyeManager().smpStartKey, PersistentDataType.BYTE)) {
                p.sendMessage(ChatColor.RED + "You already have an Eye!");
                return true;
            }
            p.getPersistentDataContainer().set(plugin.getEyeManager().smpStartKey, PersistentDataType.BYTE, (byte) 1);
            plugin.getEyeManager().setEye(p, EyeManager.Eye.getRandomBase(), false);
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("eye")) {
            if (args.length == 0 || args[0].equalsIgnoreCase("gui")) { plugin.getEyeManager().openGUI(p); return true; }
            if (p.isOp() && args.length >= 2) {
                Player t = Bukkit.getPlayer(args[1]);
                if (args[0].equalsIgnoreCase("give") && args.length == 3 && t != null) {
                    try { plugin.getEyeManager().setEye(t, EyeManager.Eye.valueOf(args[2].toUpperCase()), false); } catch(Exception ignored){}
                } else if (args[0].equalsIgnoreCase("reset") && t != null) {
                    t.getPersistentDataContainer().remove(plugin.getEyeManager().eyeKey);
                    t.getPersistentDataContainer().remove(plugin.getEyeManager().smpStartKey);
                    t.sendMessage(ChatColor.RED + "Your Eye was reset by Admin.");
                } else if (args[0].equalsIgnoreCase("reload")) {
                    plugin.reloadConfig();
                    p.sendMessage(ChatColor.GREEN + "Config Reloaded!");
                }
            }
            return true;
        }

        // Trade System
        if (cmd.getName().equalsIgnoreCase("trade") && args.length == 1) {
            Player t = Bukkit.getPlayer(args[0]);
            if (t == null || t == p || !plugin.getEyeManager().hasEye(p) || !plugin.getEyeManager().hasEye(t)) return true;
            pendingTrades.put(t.getUniqueId(), p.getUniqueId());
            p.sendMessage(ChatColor.YELLOW + "Trade request sent.");
            t.sendMessage(ChatColor.GOLD + p.getName() + " wants to trade Eyes. Type /tradeaccept " + p.getName());
            return true;
        }
        if (cmd.getName().equalsIgnoreCase("tradeaccept") && args.length == 1) {
            Player req = Bukkit.getPlayer(args[0]);
            if (req != null && pendingTrades.get(p.getUniqueId()) != null && pendingTrades.get(p.getUniqueId()).equals(req.getUniqueId())) {
                executeTradeRitual(p, req);
                pendingTrades.remove(p.getUniqueId());
            }
            return true;
        }

        // Event System
        if (cmd.getName().equalsIgnoreCase("event") && p.isOp()) {
            if (args[0].equalsIgnoreCase("start") && args.length == 2) {
                try {
                    eventReward = EyeManager.Eye.valueOf(args[1].toUpperCase());
                    eventActive = true;
                    eventProgress.clear();
                    Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "⚡ " + eventReward.name() + " Event Started!");
                    for(Player op : Bukkit.getOnlinePlayers()) { op.sendTitle("EVENT STARTED", eventReward.name() + " Eye", 10, 70, 20); updateBoard(op); }
                } catch(Exception ignored){}
            } else if (args[0].equalsIgnoreCase("stop")) {
                eventActive = false;
                Bukkit.broadcastMessage(ChatColor.RED + "Event Stopped.");
                for(Player op : Bukkit.getOnlinePlayers()) op.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            }
            return true;
        }
        return false;
    }

    private void executeTradeRitual(Player p1, Player p2) {
        p1.setWalkSpeed(0f); p2.setWalkSpeed(0f);
        p1.playSound(p1.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1, 1);
        p2.playSound(p2.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1, 1);
        
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks++ >= 40) { // 2s wait
                    EyeManager.Eye e1 = plugin.getEyeManager().getEye(p1);
                    EyeManager.Eye e2 = plugin.getEyeManager().getEye(p2);
                    plugin.getEyeManager().setEye(p1, e2, true); // Silent swap
                    plugin.getEyeManager().setEye(p2, e1, true);
                    p1.sendMessage(ChatColor.GREEN + "Trade complete.");
                    p2.sendMessage(ChatColor.GREEN + "Trade complete.");
                    p1.setWalkSpeed(0.2f); p2.setWalkSpeed(0.2f);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // Event Tasks Trackers
    @EventHandler
    public void onKill(PlayerDeathEvent e) {
        if(e.getEntity().getKiller() != null) {
            Player killer = e.getEntity().getKiller();
            plugin.getEyeManager().addXp(killer, 10);
            if(eventActive && eventTasks.get(eventProgress.getOrDefault(killer.getUniqueId(), 0)).equals("KILL_PLAYER")) advanceTask(killer);
        }
    }
    
    @EventHandler
    public void onMine(BlockBreakEvent e) {
        if(eventActive && eventTasks.get(eventProgress.getOrDefault(e.getPlayer().getUniqueId(), 0)).equals("MINE_BLOCKS")) advanceTask(e.getPlayer());
    }

    private void advanceTask(Player p) {
        int t = eventProgress.getOrDefault(p.getUniqueId(), 0) + 1;
        if (t >= 3) {
            Bukkit.broadcastMessage(ChatColor.GOLD + "🏆 " + p.getName() + " won the " + eventReward.name() + " Eye!");
            plugin.getEyeManager().setEye(p, eventReward, false);
            eventActive = false;
            for(Player op : Bukkit.getOnlinePlayers()) op.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        } else {
            eventProgress.put(p.getUniqueId(), t);
            p.sendMessage(ChatColor.AQUA + "Task complete! Next task unlocked.");
            updateBoard(p);
        }
    }

    private void updateBoard(Player p) {
        if (!eventActive) return;
        Scoreboard b = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective o = b.registerNewObjective("evt", "dummy", ChatColor.GOLD + eventReward.name() + " Event");
        o.setDisplaySlot(DisplaySlot.SIDEBAR);
        int t = eventProgress.getOrDefault(p.getUniqueId(), 0);
        for (int i = 0; i < 3; i++) {
            String s = (i < t) ? "§a✔ Done" : (i == t) ? "§e" + eventTasks.get(i) : "§7???";
            o.getScore("Task " + (i+1) + ": " + s).setScore(3-i);
        }
        p.setScoreboard(b);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if(cmd.getName().equalsIgnoreCase("eye") && args.length == 1) return Arrays.asList("give", "reset", "gui", "reload");
        if(cmd.getName().equalsIgnoreCase("event") && args.length == 1) return Arrays.asList("start", "stop");
        if(cmd.getName().equalsIgnoreCase("event") && args.length == 2) return Arrays.stream(EyeManager.Eye.values()).map(Enum::name).toList();
        return null;
    }
                          }
              
