package com.ancienteye;

import org.bukkit.*;
import org.bukkit.boss.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * EventManager — Fully fixed & enhanced.
 *
 * COMMAND HANDLER CHANGE REQUIRED:
 *   Old call:  eventManager.startEvent(EyeType.FIRE);
 *   New call:  eventManager.startEvent(EyeType.FIRE, EventManager.parseTime("10m"));
 *
 * Example command handler snippet:
 *   if (args[0].equalsIgnoreCase("start") && args.length >= 3) {
 *       EyeType eye = EyeType.valueOf(args[1].toUpperCase());
 *       long duration = EventManager.parseTime(args[2]);   // e.g. "1d", "30m", "1h30m"
 *       if (duration < 0) { sender.sendMessage("§cInvalid time!"); return true; }
 *       plugin.getEventManager().startEvent(eye, duration);
 *   }
 *
 * ── CONFIG TASK FORMAT ───────────────────────────────────────────────────────
 *  Unlimited tasks can be added. Supported formats:
 *
 *  KILL tasks:
 *    "Kill 1 Player"         → kill any 1 player
 *    "Kill 3 Players"        → kill any 3 players
 *    "Kill NotEasyOk"        → must kill the player named  NotEasyOk  specifically
 *    "Kill 2 NotEasyOk"      → must kill NotEasyOk 2 times
 *
 *  BREAK / MINE tasks:
 *    "Break 10 Diamonds"     → break 10 diamond ore blocks
 *    "Break 64 Iron"         → break 64 iron ore blocks
 *    "Break 5 Gold"          → break 5 gold ore blocks
 *    "Break 20 Coal"         → break 20 coal ore blocks
 *    "Break 3 Emerald"       → break 3 emerald ore blocks
 *    (any material keyword that appears in the Bukkit Material enum name works)
 *
 *  SURVIVE tasks:
 *    "Survive 2 Minutes"     → stay alive for 2 minutes
 *    "Survive 5 Minutes"     → stay alive for 5 minutes
 *
 *  Example config.yml section:
 *    event-settings:
 *      tasks:
 *        - "Kill 1 Player"
 *        - "Kill NotEasyOk"
 *        - "Break 10 Diamonds"
 *        - "Break 64 Iron"
 *        - "Survive 2 Minutes"
 */
public class EventManager implements Listener {

    private final AncientEyePlugin plugin;

    private boolean   active       = false;
    private boolean   winAnimating = false;
    private EyeType   reward       = EyeType.NONE;

    // Timer
    private long       eventDurationMillis = 0;
    private long       eventStartTime      = 0;
    private BukkitTask timerTask           = null;

    // Player data (never cleared on death/disconnect)
    private final Map<UUID, Integer>      taskProgress = new HashMap<>();
    private final Map<UUID, List<String>> playerTasks  = new HashMap<>();
    private final Map<UUID, Integer>      blockCount   = new HashMap<>();

    public EventManager(AncientEyePlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TIME PARSER  — "1d", "2h30m", "90s", etc.
    // ══════════════════════════════════════════════════════════════════════════
    public static long parseTime(String input) {
        if (input == null || input.isEmpty()) return -1;
        long total = 0;
        int i = 0;
        while (i < input.length()) {
            int start = i;
            while (i < input.length() && Character.isDigit(input.charAt(i))) i++;
            if (i == start || i >= input.length()) break;
            long val = Long.parseLong(input.substring(start, i));
            switch (Character.toLowerCase(input.charAt(i++))) {
                case 'd': total += val * 86_400_000L; break;
                case 'h': total += val *  3_600_000L; break;
                case 'm': total += val *     60_000L; break;
                case 's': total += val *      1_000L; break;
                default: return -1;
            }
        }
        return total > 0 ? total : -1;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  BROADCAST
    // ══════════════════════════════════════════════════════════════════════════
    private void broadcastWithTitle(String title, String subtitle, String chatLine1, String chatLine2) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(title, subtitle, 10, 80, 20);
            p.sendMessage(chatLine1);
            if (chatLine2 != null && !chatLine2.isEmpty()) p.sendMessage(chatLine2);
        }
    }

    private void broadcastWithTitleChat(String title, String subtitle, List<String> chatLines) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(title, subtitle, 10, 80, 20);
            for (String line : chatLines) p.sendMessage(line);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  START EVENT
    // ══════════════════════════════════════════════════════════════════════════
    public void startEvent(EyeType eye, long durationMillis) {
        this.active              = true;
        this.winAnimating        = false;
        this.reward              = eye;
        this.eventDurationMillis = durationMillis;
        this.eventStartTime      = System.currentTimeMillis();

        if (timerTask != null) { timerTask.cancel(); timerTask = null; }
        taskProgress.clear();
        playerTasks.clear();
        blockCount.clear();

        List<String> allTasks = plugin.getConfig().getStringList("event-settings.tasks");
        if (allTasks.isEmpty()) {
            Bukkit.getLogger().warning("[AncientEye] No tasks found in config! Using defaults.");
            allTasks = Arrays.asList("Kill 1 Player", "Break 10 Diamonds", "Survive 2 Minutes");
        }
        final List<String> tasks = new ArrayList<>(allTasks);

        List<String> chatLines = Arrays.asList(
            "§8§m================================",
            "§6§l  ★  EYE EVENT: §e§l" + eye.name() + " EYE",
            "§f  Complete 3 tasks shown on your scoreboard.",
            "§f  ⏰ Time: §e" + formatTimeDisplay(durationMillis),
            "§8§m================================"
        );

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(
                "§6§l★ EYE EVENT STARTED! ★",
                "§e" + eye.name() + " EYE §f— Complete 3 Tasks!",
                10, 100, 20
            );
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1.2f);
            for (String line : chatLines) p.sendMessage(line);
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            assignTasksToPlayer(p, tasks);
        }

        startTimerTask(tasks);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  STOP EVENT
    // ══════════════════════════════════════════════════════════════════════════
    public void stopEvent() {
        if (!active) return;

        active = false;

        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }

        taskProgress.clear();
        playerTasks.clear();
        blockCount.clear();

        for (Player p : Bukkit.getOnlinePlayers()) {
            Scoreboard board = p.getScoreboard();
            Objective obj = board.getObjective("event");
            if (obj != null) obj.unregister();
            p.sendTitle("§c§lEVENT STOPPED", "§7Cancelled by Admin.", 10, 60, 10);
            p.sendMessage("§c§l[AncientEye] §7Event has been cancelled by Admin.");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TIMER TASK  — runs every second
    // ══════════════════════════════════════════════════════════════════════════
    private void startTimerTask(List<String> allTasks) {
        timerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {

            if (!active) {
                timerTask.cancel();
                timerTask = null;
                return;
            }

            long remaining = getRemainingMillis();

            if (remaining <= 0) {
                active = false;
                timerTask.cancel();
                timerTask = null;

                List<String> lines = Arrays.asList(
                    "§8§m================================",
                    "§c§l  ⏰  TIME IS UP!",
                    "§7  No one completed all tasks in time.",
                    "§8§m================================"
                );

                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
                    p.sendTitle("§c§l⏰ TIME IS UP!", "§7No winner this time...", 10, 100, 20);
                    p.playSound(p.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.8f, 0.7f);
                    for (String line : lines) p.sendMessage(line);
                }
                return;
            }

            if (winAnimating) return;

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!playerTasks.containsKey(p.getUniqueId())) continue;
                String t = getCurrentTaskName(p);
                if (t != null && !t.isEmpty()) updateScoreboard(p, t, remaining);
            }

        }, 20L, 20L);
    }

    private long getRemainingMillis() {
        return eventDurationMillis - (System.currentTimeMillis() - eventStartTime);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TASK ASSIGNMENT
    // ══════════════════════════════════════════════════════════════════════════
    private void assignTasksToPlayer(Player p, List<String> allTasks) {
        List<String> copy = new ArrayList<>(allTasks);
        Collections.shuffle(copy);
        List<String> selected = new ArrayList<>(copy.subList(0, Math.min(3, copy.size())));

        playerTasks.put(p.getUniqueId(), selected);
        taskProgress.put(p.getUniqueId(), 1);
        blockCount.put(p.getUniqueId(), 0);

        updateScoreboard(p, selected.get(0), getRemainingMillis());

        // FIX: Agar pehla task survive hai toh abhi se timer start karo
        String firstTask = selected.get(0);
        if (firstTask.toLowerCase().contains("survive")) {
            int mins = parseSurviveMins(firstTask);
            final int expectedProgress = 1;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!active || !p.isOnline()) return;
                Integer prog = taskProgress.get(p.getUniqueId());
                if (prog == null || prog != expectedProgress) return;
                String cur = getCurrentTaskName(p);
                if (cur != null && cur.trim().equalsIgnoreCase(firstTask.trim())) advanceTask(p);
            }, (long) mins * 1200L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SCOREBOARD
    // ══════════════════════════════════════════════════════════════════════════
    private void updateScoreboard(Player p, String taskDesc, long remainMillis) {
        ScoreboardManager sm = Bukkit.getScoreboardManager();
        if (sm == null) return;

        Scoreboard board = sm.getNewScoreboard();
        Objective obj = board.registerNewObjective("event", "dummy",
                "§6§l" + reward.name() + " EVENT");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        int taskNum = taskProgress.getOrDefault(p.getUniqueId(), 1);
        String desc = taskDesc.length() > 22 ? taskDesc.substring(0, 20) + ".." : taskDesc;

        obj.getScore("§7§m——————————————").setScore(7);
        obj.getScore(" §e⏰ §f" + formatTimeScoreboard(remainMillis)).setScore(6);
        obj.getScore("§r ").setScore(5);
        obj.getScore(" §fTask §6" + taskNum + "§f/§e3").setScore(4);
        obj.getScore(" §a▶ §f" + desc).setScore(3);
        obj.getScore("§r  ").setScore(2);
        obj.getScore("§7§m——————————————§r ").setScore(1);

        p.setScoreboard(board);
    }

    private String formatTimeScoreboard(long ms) {
        if (ms <= 0) return "§c00:00";
        long s = ms / 1000;
        long d = s / 86400, h = (s % 86400) / 3600, m = (s % 3600) / 60, sec = s % 60;
        if (d > 0) return String.format("§e%dd §f%02d:%02d:%02d", d, h, m, sec);
        if (h > 0) return String.format("§f%02dh %02dm", h, m);
        if (s < 60) return String.format("§c%02d:%02d", m, sec);
        return String.format("§f%02d:%02d", m, sec);
    }

    private String formatTimeDisplay(long ms) {
        long s = ms / 1000;
        long d = s / 86400, h = (s % 86400) / 3600, m = (s % 3600) / 60, sec = s % 60;
        StringBuilder sb = new StringBuilder();
        if (d > 0) sb.append(d).append("d ");
        if (h > 0) sb.append(h).append("h ");
        if (m > 0) sb.append(m).append("m ");
        if (sec > 0) sb.append(sec).append("s");
        return sb.toString().trim();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  EVENTS
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * KILL task handler — fully config-driven.
     *
     * Supported task formats (from config):
     *   "Kill 1 Player"     → kill any 1 player
     *   "Kill 3 Players"    → kill any 3 players
     *   "Kill NotEasyOk"    → must kill the player named  NotEasyOk  specifically
     *   "Kill 2 NotEasyOk"  → must kill NotEasyOk exactly 2 times
     *
     * How it works:
     *   After the word "Kill", we look at the next word.
     *   • If it is a pure number  → generic kill task (any player counts).
     *     The word after the number ("Player" / "Players" / anything) is ignored.
     *   • If it is NOT a number  → it is a specific player name.
     *     Only killing that exact player (case-insensitive) advances the task.
     */
    private final java.util.Set<UUID> killProcessing = new java.util.HashSet<>();

    @EventHandler
    public void onKill(PlayerDeathEvent e) {
        if (!active || e.getEntity().getKiller() == null) return;

        Player killer = e.getEntity().getKiller();
        Player victim = e.getEntity();
        UUID kuid = killer.getUniqueId();

        if (!playerTasks.containsKey(kuid)) return;

        // Double registration guard
        if (killProcessing.contains(kuid)) return;
        killProcessing.add(kuid);
        Bukkit.getScheduler().runTaskLater(plugin, () -> killProcessing.remove(kuid), 1L);

        String task = getCurrentTaskName(killer);
        if (task == null || task.isEmpty()) return;

        String trimmed = task.trim();
        if (!trimmed.toLowerCase().startsWith("kill")) return;

        // Words after "Kill"
        String[] parts = trimmed.substring(4).trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) return;

        // Is the first word a number?
        boolean firstIsNumber;
        int firstNumber = 1;
        try {
            firstNumber   = Integer.parseInt(parts[0]);
            firstIsNumber = true;
        } catch (NumberFormatException ex) {
            firstIsNumber = false;
        }

        if (!firstIsNumber) {
            // CASE 1: "Kill NotEasyOk" — specific player, only 1 kill needed
            // parts[0] is the player name
            String targetName = parts[0];
            if (victim.getName().equalsIgnoreCase(targetName)) {
                killer.sendActionBar("§c⚔ §fTask complete! §7Killed §c" + targetName);
                handleTaskCompletion(killer, task);
            }
            return;
        }

        // parts[0] is a number
        if (parts.length == 1) {
            // "Kill 3" — no label, generic
            int c = blockCount.getOrDefault(killer.getUniqueId(), 0) + 1;
            blockCount.put(killer.getUniqueId(), c);
            killer.sendActionBar("§c⚔ Kills: §f" + c + " §7/ §f" + firstNumber);
            if (c >= firstNumber) handleTaskCompletion(killer, task);
            return;
        }

        String secondWord = parts[1];
        boolean isGenericLabel = secondWord.equalsIgnoreCase("player")
                              || secondWord.equalsIgnoreCase("players");

        if (isGenericLabel) {
            // CASE 2: "Kill 3 Players" — any player kill counts
            int c = blockCount.getOrDefault(killer.getUniqueId(), 0) + 1;
            blockCount.put(killer.getUniqueId(), c);
            killer.sendActionBar("§c⚔ Kills: §f" + c + " §7/ §f" + firstNumber);
            if (c >= firstNumber) {
                blockCount.put(killer.getUniqueId(), 0); // FIX: reset before advancing
                handleTaskCompletion(killer, task);
            }
        } else {
            // CASE 3: "Kill 2 NotEasyOk" — must kill that specific player N times
            String targetName = secondWord;
            if (victim.getName().equalsIgnoreCase(targetName)) {
                int c = blockCount.getOrDefault(killer.getUniqueId(), 0) + 1;
                blockCount.put(killer.getUniqueId(), c);
                killer.sendActionBar("§c⚔ " + targetName + ": §f" + c + " §7/ §f" + firstNumber);
                if (c >= firstNumber) {
                    blockCount.put(killer.getUniqueId(), 0); // FIX: reset before advancing
                    handleTaskCompletion(killer, task);
                }
            }
        }
    }

        /**
     * CRAFT task handler — config-driven.
     * Works exactly like Break/Kill.
     * Example: "Craft 5 Golden_Apple" or "Craft 1 Diamond_Sword"
     */
    @EventHandler
    public void onCraft(CraftItemEvent e) {
        if (!active) return;
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!playerTasks.containsKey(p.getUniqueId())) return;

        String task = getCurrentTaskName(p);
        if (task == null || task.isEmpty()) return;

        String lower = task.toLowerCase().trim();
        if (!lower.startsWith("craft")) return;

        // Aapke purane helpers use ho rahe hain
        int required = parseTaskCount(task);
        String itemKeyword = parseTaskMaterial(task);

        if (itemKeyword.isEmpty()) return;

        // Check if the crafted item matches the keyword
        String craftedItemName = e.getRecipe().getResult().getType().name().toLowerCase();
        if (craftedItemName.contains(itemKeyword.toLowerCase())) {
            
            // Default amount crafted
            int amount = e.getRecipe().getResult().getAmount();
            
            // Shift-click support: Check how many can actually be crafted
            if (e.isShiftClick()) {
                int maxCraftable = 64; 
                for (ItemStack item : e.getInventory().getMatrix()) {
                    if (item != null && item.getType() != Material.AIR) {
                        maxCraftable = Math.min(maxCraftable, item.getAmount());
                    }
                }
                amount *= maxCraftable;
            }

            int c = blockCount.getOrDefault(p.getUniqueId(), 0) + amount;
            blockCount.put(p.getUniqueId(), c);

            // Action bar: show live progress like onBreak
            p.sendActionBar("§6🔨 Craft " + capitalise(itemKeyword) + ": §f" + c + " §7/ §f" + required);

            if (c >= required) {
                // Counter reset aur task complete
                blockCount.put(p.getUniqueId(), 0);
                handleTaskCompletion(p, task);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  MOB KILL TASK (Fix for 'Kill 20 Zombie')
    // ══════════════════════════════════════════════════════════════════════════
    @EventHandler
    public void onMobKill(org.bukkit.event.entity.EntityDeathEvent e) {
        if (!active || e.getEntity().getKiller() == null) return;
        
        Player killer = e.getEntity().getKiller();
        String task = getCurrentTaskName(killer);
        
        // Check if task is "Kill 20 Zombie"
        if (task != null && task.toLowerCase().contains("kill")) {
            String[] parts = task.split(" ");
            if (parts.length < 3) return;

            int req = Integer.parseInt(parts[1]); // "20"
            String target = parts[2]; // "Zombie"

            if (e.getEntityType().name().equalsIgnoreCase(target)) {
                int c = blockCount.getOrDefault(killer.getUniqueId(), 0) + 1;
                blockCount.put(killer.getUniqueId(), c);
                
                killer.spigot().sendMessage(ChatMessageType.ACTION_BAR, 
                    new TextComponent("§c⚔ " + target.toUpperCase() + ": §f" + c + " §7/ §f" + req));
                
                if (c >= req) {
                    blockCount.put(killer.getUniqueId(), 0);
                    handleTaskCompletion(killer, task);
                }
            }
        }
    }
    
    

    /**
     * BREAK / MINE task handler — fully config-driven.
     *
     * Supported task formats (from config):
     *   "Break 10 Diamonds"   → break 10 diamond ore blocks
     *   "Break 64 Iron"       → break 64 iron ore blocks
     *   "Break 5 Gold"        → break 5 gold ore blocks
     *   "Break 3 Emerald"     → break 3 emerald ore blocks
     *   "Break 20 Coal"       → break 20 coal ore blocks
     *
     * How it works:
     *   Parses the number from the task string (first integer found).
     *   Parses the material keyword (first non-number, non-verb word).
     *   Checks if the broken block's Material name contains that keyword (case-insensitive).
     *   Increments player's blockCount and marks task complete when count >= required.
     */
    private final java.util.Set<UUID> breakProcessing = new java.util.HashSet<>();

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (!active) return;
        Player p = e.getPlayer();
        UUID uid = p.getUniqueId();
        if (!playerTasks.containsKey(uid)) return;

        // Double registration guard
        if (breakProcessing.contains(uid)) return;
        breakProcessing.add(uid);
        Bukkit.getScheduler().runTaskLater(plugin, () -> breakProcessing.remove(uid), 1L);

        String task = getCurrentTaskName(p);
        if (task == null || task.isEmpty()) return;

        String lower = task.toLowerCase().trim();
        if (!lower.startsWith("break") && !lower.startsWith("mine")) return;

        int    required        = parseTaskCount(task);
        String materialKeyword = parseTaskMaterial(task);
        if (materialKeyword.isEmpty()) return;

        String blockName = e.getBlock().getType().name().toLowerCase();
        if (blockName.contains(materialKeyword.toLowerCase())) {
            int c = blockCount.getOrDefault(uid, 0) + 1;
            blockCount.put(uid, c);
            p.sendActionBar("§e⛏ " + capitalise(materialKeyword) + ": §f" + c + " §7/ §f" + required);
            if (c >= required) {
                blockCount.put(uid, 0);
                handleTaskCompletion(p, task);
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (!active) return;
        Player p = e.getPlayer();
        if (!playerTasks.containsKey(p.getUniqueId())) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline() || !active || winAnimating) return;
            String t = getCurrentTaskName(p);
            if (t != null && !t.isEmpty()) updateScoreboard(p, t, getRemainingMillis());
        }, 5L);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        if (!active) return;
        Player p = e.getPlayer();
        if (!playerTasks.containsKey(p.getUniqueId())) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline() || !active || winAnimating) return;
            String t = getCurrentTaskName(p);
            if (t != null && !t.isEmpty()) updateScoreboard(p, t, getRemainingMillis());
        }, 2L);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TASK LOGIC
    // ══════════════════════════════════════════════════════════════════════════
    private void handleTaskCompletion(Player p, String goal) {
        String current = getCurrentTaskName(p);
        // FIX: trim() + equalsIgnoreCase — whitespace/case mismatch se task miss nahi hoga
        if (current != null && current.trim().equalsIgnoreCase(goal.trim())) advanceTask(p);
    }

    private void advanceTask(Player p) {
        if (!playerTasks.containsKey(p.getUniqueId())) return;
        int cur = taskProgress.getOrDefault(p.getUniqueId(), 1);
        blockCount.put(p.getUniqueId(), 0); // reset counter for next task

        if (cur >= 3) {
            taskProgress.put(p.getUniqueId(), 4);
            win(p);
            return;
        }

        int next = cur + 1;
        taskProgress.put(p.getUniqueId(), next);
        String nextTask = playerTasks.get(p.getUniqueId()).get(next - 1);

        p.sendTitle("§a§l✓ TASK " + cur + " COMPLETE!", "§eNext: §f" + nextTask, 10, 60, 10);
        p.sendMessage("§a§l✓ §7Task " + cur + " complete! §eNext task: §f" + nextTask);
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
        updateScoreboard(p, nextTask, getRemainingMillis());

        if (nextTask.toLowerCase().contains("survive")) {
            int mins = parseSurviveMins(nextTask);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!active) return;
                if (!p.isOnline()) return;
                Integer prog = taskProgress.get(p.getUniqueId());
                if (prog == null || prog != next) return;
                String cur2 = getCurrentTaskName(p);
                if (cur2 != null && cur2.trim().equalsIgnoreCase(nextTask.trim())) advanceTask(p);
            }, (long) mins * 1200L);
        }
    }

    private int parseSurviveMins(String task) {
        try {
            String[] parts = task.split(" ");
            for (int i = 0; i < parts.length - 1; i++) {
                if (parts[i].equalsIgnoreCase("Survive")) {
                    return Integer.parseInt(parts[i + 1]);
                }
            }
        } catch (NumberFormatException ignored) {}
        return 2;
    }

    private String getCurrentTaskName(Player p) {
        List<String> tasks = playerTasks.get(p.getUniqueId());
        if (tasks == null || tasks.isEmpty()) return null;
        int prog = taskProgress.getOrDefault(p.getUniqueId(), 1);
        if (prog < 1 || prog > tasks.size()) return null;
        return tasks.get(prog - 1);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TASK PARSING HELPERS  (config-driven)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Extracts the first integer found in a task string.
     * "Break 10 Diamonds"  → 10
     * "Kill 3 Players"     → 3
     * "Survive 2 Minutes"  → 2
     * "Kill NotEasyOk"     → 1  (no number → default 1)
     */
    private int parseTaskCount(String task) {
        for (String part : task.split("\\s+")) {
            try { return Integer.parseInt(part); }
            catch (NumberFormatException ignored) {}
        }
        return 1;
    }

    /**
     * Extracts the material keyword from a break/mine task string.
     * Skips the first word (verb: Break/Mine) and any pure-number words.
     * Returns the first remaining word as the keyword.
     *
     * "Break 10 Diamonds"  → "Diamonds"
     * "Break 64 Iron"      → "Iron"
     * "Mine 5 Gold"        → "Gold"
     * "Break 20 Coal"      → "Coal"
     *
     * Matching is done via  blockMaterialName.contains(keyword.toLowerCase())
     * so "Diamond" matches both DIAMOND_ORE and DEEPSLATE_DIAMOND_ORE automatically.
     */
    private String parseTaskMaterial(String task) {
        String[] parts = task.split("\\s+");
        for (int i = 0; i < parts.length; i++) {
            if (i == 0) continue;                    // skip verb (Break / Mine)
            try { Integer.parseInt(parts[i]); continue; } // skip numbers
            catch (NumberFormatException ignored) {}
            return parts[i];                         // first non-number, non-verb word
        }
        return "";
    }

    /** Capitalises the first letter of a word for display. */
    private String capitalise(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  WIN SEQUENCE
    // ══════════════════════════════════════════════════════════════════════════
    private void win(Player winner) {
        if (!active || winAnimating) return;
        active       = false;
        winAnimating = true;

        if (timerTask != null) { timerTask.cancel(); timerTask = null; }

        List<String> chatLines = Arrays.asList(
            "§8§m================================",
            "§6§l  ** EVENT WINNER **",
            "§f  " + winner.getName() + " §ehas completed all tasks!",
            "§f  Binding §5§l" + reward.name() + " §fEye...",
            "§8§m================================"
        );

        for (Player p : Bukkit.getOnlinePlayers()) {
            // Title — sab online players ko dikhe
            p.sendTitle(
                "§6§l" + winner.getName() + " WON!",
                "§fBinding the §5§l" + reward.name() + " §fEye...",
                10, 100, 25
            );
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            for (String line : chatLines) p.sendMessage(line);
            // Scoreboard remove
            p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }

        startWinAnimation(winner);
    }
   // ══════════════════════════════════════════════════════════════════════════
    //  WIN ANIMATION  (10 seconds = 200 ticks)
    // ══════════════════════════════════════════════════════════════════════════
    private void startWinAnimation(Player winner) {

        BossBar bar = Bukkit.createBossBar(
                "§5§lBinding §6" + reward.name() + "§5§l... §e0%",
                BarColor.PURPLE, BarStyle.SEGMENTED_10);
        bar.setProgress(0.0);
        if (winner.isOnline()) bar.addPlayer(winner);

        if (winner.isOnline()) {
            winner.addPotionEffect(
                    new PotionEffect(PotionEffectType.LEVITATION, 220, 2, false, false, false));
            winner.addPotionEffect(
                    new PotionEffect(PotionEffectType.SLOW_FALLING, 280, 0, false, false, false));

            winner.getWorld().strikeLightningEffect(winner.getLocation());
            winner.getWorld().playSound(winner.getLocation(),
                    Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 0.5f);

            winner.sendTitle(
                "§5§l✨ BINDING...",
                "§7Your Eye is being bound to your soul",
                5, 220, 10
            );
            winner.sendMessage("§5§l[AncientEye] §fBinding process started! Do not leave...");
        }

        final int[]    tick  = {0};
        final double[] angle = {0.0};
        final BukkitTask[] ref = {null};

        // Schedule the win-animation task
        ref[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            tick[0]++;

            // If player logs out mid-animation, cancel and give reward
            if (!winner.isOnline()) {
                if (ref[0] != null) ref[0].cancel();  // [FIX] stop animation task
                cleanupWin(winner, bar);             // [FIX] cleanup boss bar, action bar, and potion
                finishWinReward(winner);
                return;
            }

            double prog = Math.min(tick[0] / 200.0, 1.0);
            bar.setProgress(prog);
            int pct = (int)(prog * 100);
            bar.setTitle("§5§lBinding §6" + reward.name() + "§5§l... §e" + pct + "%");

            if (tick[0] % 2 == 0) {
                int filled  = (int)(prog * 20);
                StringBuilder pb = new StringBuilder("§5§l[");
                for (int i = 0; i < 20; i++) pb.append(i < filled ? "§e█" : "§8█");
                pb.append("§5§l] §e").append(pct).append("%");
                winner.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent("§5§l✨ Binding " + reward.name() + "  " + pb));
            }

            angle[0] += 12;
            Location base = winner.getLocation().clone().add(0, 1.8, 0);  // [FIX] clone location
            double radius = 1.5;

            for (int i = 0; i < 16; i++) {
                double a = Math.toRadians(angle[0] + i * 22.5);
                winner.getWorld().spawnParticle(Particle.END_ROD,
                        base.clone().add(Math.cos(a) * radius, 0, Math.sin(a) * radius),
                        1, 0, 0, 0, 0);
            }

            double rOff = Math.toRadians(angle[0] * 1.4);
            for (int i = 0; i < 16; i++) {
                double a = Math.toRadians(angle[0] + i * 22.5);
                winner.getWorld().spawnParticle(Particle.DRAGON_BREATH,
                        base.clone().add(Math.cos(rOff) * radius,
                                Math.sin(a) * radius,
                                Math.sin(rOff) * radius),
                        1, 0, 0, 0, 0);
            }

            double rOff2 = Math.toRadians(-angle[0] * 0.9);
            for (int i = 0; i < 12; i++) {
                double a = Math.toRadians(angle[0] + i * 30);
                winner.getWorld().spawnParticle(Particle.PORTAL,
                        base.clone().add(
                                Math.cos(a) * radius,
                                Math.sin(a) * radius * 0.5,
                                Math.sin(rOff2) * radius),
                        1, 0, 0, 0, 0);
            }

            if (tick[0] % 3 == 0) {
                Location loc = winner.getLocation();
                for (int y = 0; y <= 22; y++) {
                    winner.getWorld().spawnParticle(Particle.DRAGON_BREATH,
                            loc.clone().add(0, y, 0), 2, 0.12, 0, 0.12, 0);
                    winner.getWorld().spawnParticle(Particle.PORTAL,
                            loc.clone().add(0, y, 0), 1, 0.08, 0, 0.08, 0);
                }
            }

            if (tick[0] % 20 == 0) {
                float pitch = 0.6f + (tick[0] / 400f);
                winner.getWorld().playSound(winner.getLocation(),
                        Sound.BLOCK_BEACON_AMBIENT, 1f, pitch);
                winner.getWorld().playSound(winner.getLocation(),
                        Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.4f, 1.5f);
            }
            if (tick[0] % 60 == 0) {
                winner.getWorld().playSound(winner.getLocation(),
                        Sound.ENTITY_ENDER_EYE_LAUNCH, 0.8f, 0.5f);
            }

            if (tick[0] >= 200) {
                if (ref[0] != null) ref[0].cancel(); // Task pehle band karo
                cleanupWin(winner, bar);            // [FIX] cleanup boss bar, action bar, and potion

                winner.getWorld().strikeLightningEffect(winner.getLocation());
                winner.getWorld().playSound(winner.getLocation(),
                        Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 0.5f);
                winner.getWorld().playSound(winner.getLocation(),
                        Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);

                Location fLoc = winner.getLocation().clone().add(0, 1, 0);  // [FIX] clone location
                Random rng = new Random();
                for (int i = 0; i < 40; i++) {
                    winner.getWorld().spawnParticle(Particle.END_ROD, fLoc, 1,
                            (rng.nextDouble() - 0.5) * 2,
                            rng.nextDouble() * 2,
                            (rng.nextDouble() - 0.5) * 2, 0.15);
                    winner.getWorld().spawnParticle(Particle.DRAGON_BREATH, fLoc, 1,
                            (rng.nextDouble() - 0.5) * 2,
                            rng.nextDouble() * 2,
                            (rng.nextDouble() - 0.5) * 2, 0.05);
                }

                finishWinReward(winner);
                return;
            }

        }, 1L, 1L);

        // [ADD] Failsafe: ensure reward is given even if animation stalls
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (winAnimating) {
                cleanupWin(winner, bar);
                finishWinReward(winner);
            }
        }, 220L);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FINISH WIN
    // ══════════════════════════════════════════════════════════════════════════
    private void finishWinReward(Player winner) {
        this.winAnimating = false;
        this.active = false;

        try {
            // (Original reward code that gives the eye and announces winner)
            plugin.getPlayerData().setEye(winner, reward, false);
            plugin.getPlayerData().saveData(winner);
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.getScoreboard().clearSlot(DisplaySlot.SIDEBAR);
                p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            }
            // (Announcements and sounds ...)
            for (String line : Arrays.asList(
                    "§8§m================================",
                    "§5§l  BINDING SUCCESSFUL!",
                    "§e  " + winner.getName() + " §fhas successfully bound the §5§l" + reward.name() + " §fEye!",
                    "§7  The Ancient Eye is now bound to their soul.",
                    "§8§m================================"
                )) {
                Bukkit.broadcastMessage(line);
            }
            winner.playSound(winner.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1f);
            winner.playSound(winner.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.4f, 1.2f);
            winner.removePotionEffect(PotionEffectType.LEVITATION);
            Bukkit.getLogger().info("[AncientEye] " + winner.getName() + " won and received " + reward.name() + " Eye.");
        } catch (Exception ex) {
            plugin.getLogger().severe("[AncientEye] finishWinReward error: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            // No additional cleanup needed here
        }
    }

    /** 
     * [ADDED] Clean up after win animation: remove bossbar, action bar, and potion effect.
     */
    private void cleanupWin(Player winner, BossBar bar) {
        try {
            if (bar != null) {
                bar.removeAll();
                bar.setVisible(false);
            }
            if (winner.isOnline()) {
                // Clear action bar message and remove levitation
                winner.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
                winner.removePotionEffect(PotionEffectType.LEVITATION);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PUBLIC UTILS
    // ══════════════════════════════════════════════════════════════════════════
    public boolean isActive()  { return active; }
    public EyeType getReward() { return reward; }
}
