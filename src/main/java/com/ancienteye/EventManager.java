package com.ancienteye;

import org.bukkit.*;
import org.bukkit.boss.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

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
    //  BROADCAST — message + title on screen simultaneously
    // ══════════════════════════════════════════════════════════════════════════
    private void broadcastWithTitle(String title, String subtitle, String chatLine1, String chatLine2) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            // Title on screen
            p.sendTitle(title, subtitle, 10, 80, 20);
            // Same info in chat
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

        // Chat announcement
        List<String> chatLines = Arrays.asList(
            "§8§m================================",
            "§6§l  ★  EYE EVENT: §e§l" + eye.name() + " EYE",
            "§f  Complete 3 tasks shown on your scoreboard.",
            "§f  ⏰ Time: §e" + formatTimeDisplay(durationMillis),
            "§8§m================================"
        );

        // Screen title for all + chat
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

            // Title on screen
            p.sendTitle("§c§lEVENT STOPPED", "§7Cancelled by Admin.", 10, 60, 10);
            // Chat message
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

            // TIME UP
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
                    // Title on screen
                    p.sendTitle("§c§l⏰ TIME IS UP!", "§7No winner this time...", 10, 100, 20);
                    p.playSound(p.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.8f, 0.7f);
                    // Chat
                    for (String line : lines) p.sendMessage(line);
                }
                return;
            }

            // UPDATE SCOREBOARDS every second
            if (winAnimating) return;

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!playerTasks.containsKey(p.getUniqueId())) continue;
                String t = getCurrentTaskName(p);
                if (t != null && !t.isEmpty()) {
                    updateScoreboard(p, t, remaining);
                }
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
    @EventHandler
    public void onKill(PlayerDeathEvent e) {
        if (!active || e.getEntity().getKiller() == null) return;
        handleTaskCompletion(e.getEntity().getKiller(), "Kill 1 Player");
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (!active) return;
        Player p = e.getPlayer();
        if (!playerTasks.containsKey(p.getUniqueId())) return;

        String task = getCurrentTaskName(p);
        if (task == null || task.isEmpty()) return;

        if (task.contains("Diamonds") &&
                (e.getBlock().getType() == Material.DIAMOND_ORE ||
                 e.getBlock().getType() == Material.DEEPSLATE_DIAMOND_ORE)) {
            int c = blockCount.getOrDefault(p.getUniqueId(), 0) + 1;
            blockCount.put(p.getUniqueId(), c);
            p.sendActionBar("§b💎 Diamonds: §f" + c + " §7/ §f10");
            if (c >= 10) handleTaskCompletion(p, task);
        }

        if (task.contains("Iron") &&
                (e.getBlock().getType() == Material.IRON_ORE ||
                 e.getBlock().getType() == Material.DEEPSLATE_IRON_ORE)) {
            int c = blockCount.getOrDefault(p.getUniqueId(), 0) + 1;
            blockCount.put(p.getUniqueId(), c);
            p.sendActionBar("§7⛏ Iron: §f" + c + " §7/ §f64");
            if (c >= 64) handleTaskCompletion(p, task);
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
        if (current != null && current.equals(goal)) advanceTask(p);
    }

    private void advanceTask(Player p) {
        if (!playerTasks.containsKey(p.getUniqueId())) return;
        int cur = taskProgress.getOrDefault(p.getUniqueId(), 1);
        blockCount.put(p.getUniqueId(), 0);

        if (cur >= 3) {
            taskProgress.put(p.getUniqueId(), 4);
            win(p);
            return;
        }

        int next = cur + 1;
        taskProgress.put(p.getUniqueId(), next);
        String nextTask = playerTasks.get(p.getUniqueId()).get(next - 1);

        // Title on screen
        p.sendTitle("§a§l✓ TASK " + cur + " COMPLETE!", "§eNext: §f" + nextTask, 10, 60, 10);
        // Chat message
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
                if (cur2 != null && cur2.equals(nextTask)) advanceTask(p);
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
    //  WIN SEQUENCE
    // ══════════════════════════════════════════════════════════════════════════
    private void win(Player winner) {
        if (!active || winAnimating) return;
        active       = false;
        winAnimating = true;

        if (timerTask != null) { timerTask.cancel(); timerTask = null; }

        List<String> chatLines = Arrays.asList(
            "§8§m================================",
            "§6§l  🏆 " + winner.getName() + " §ehas completed all tasks!",
            "§f  Binding §5§l" + reward.name() + " §fEye...",
            "§8§m================================"
        );

        for (Player p : Bukkit.getOnlinePlayers()) {
            // Title on screen
            p.sendTitle(
                "§6§l🏆 " + winner.getName() + " WON!",
                "§fBinding the §5§l" + reward.name() + "§f Eye...",
                10, 80, 20
            );
            // Chat
            for (String line : chatLines) p.sendMessage(line);

            if (!p.getUniqueId().equals(winner.getUniqueId())) {
                p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            }
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

            // Screen title at animation start
            winner.sendTitle(
                "§5§l✨ BINDING...",
                "§7Your Eye is being bound to your soul",
                5, 220, 10
            );
            // Chat
            winner.sendMessage("§5§l[AncientEye] §fBinding process started! Do not leave...");
        }

        final int[]    tick  = {0};
        final double[] angle = {0.0};
        final BukkitTask[] ref = {null};

        ref[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            tick[0]++;

            if (!winner.isOnline()) {
                bar.removeAll();
                if (ref[0] != null) ref[0].cancel();
                finishWinReward(winner);
                return;
            }

            // Boss bar progress
            double prog = Math.min(tick[0] / 200.0, 1.0);
            bar.setProgress(prog);
            int pct = (int)(prog * 100);
            bar.setTitle("§5§lBinding §6" + reward.name() + "§5§l... §e" + pct + "%");

            // Action bar progress line for winner
            if (tick[0] % 2 == 0) {
                int filled  = (int)(prog * 20);
                StringBuilder pb = new StringBuilder("§5§l[");
                for (int i = 0; i < 20; i++) pb.append(i < filled ? "§e█" : "§8█");
                pb.append("§5§l] §e").append(pct).append("%");
                winner.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent("§5§l✨ Binding " + reward.name() + "  " + pb));
            }

            // Ring Particles
            angle[0] += 12;
            Location base = winner.getLocation().add(0, 1.8, 0);
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

            // Purple beam from sky to player (every 3 ticks)
            if (tick[0] % 3 == 0) {
                Location loc = winner.getLocation();
                for (int y = 0; y <= 22; y++) {
                    winner.getWorld().spawnParticle(Particle.DRAGON_BREATH,
                            loc.clone().add(0, y, 0), 2, 0.12, 0, 0.12, 0);
                    winner.getWorld().spawnParticle(Particle.PORTAL,
                            loc.clone().add(0, y, 0), 1, 0.08, 0, 0.08, 0);
                }
            }

            // Sound effects
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

            // ANIMATION COMPLETE (200 ticks = 10 seconds)
            if (tick[0] >= 200) {
                ref[0].cancel();
                bar.removeAll();

                winner.removePotionEffect(PotionEffectType.LEVITATION);

                winner.getWorld().strikeLightningEffect(winner.getLocation());
                winner.getWorld().playSound(winner.getLocation(),
                        Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 0.5f);
                winner.getWorld().playSound(winner.getLocation(),
                        Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);

                Location fLoc = winner.getLocation().add(0, 1, 0);
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
            }

        }, 1L, 1L);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FINISH WIN
    // ══════════════════════════════════════════════════════════════════════════
    private void finishWinReward(Player winner) {
        plugin.getPlayerData().setEye(winner, reward, false);

        if (winner.isOnline()) {
            winner.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }

        List<String> chatLines = Arrays.asList(
            "§8§m================================",
            "§5§l  ✨  BINDING SUCCESSFUL!",
            "§e  " + winner.getName() + " §fhas successfully bound the §5§l" + reward.name() + " §fEye!",
            "§8§m================================"
        );

        for (Player p : Bukkit.getOnlinePlayers()) {
            // Title on screen
            p.sendTitle(
                "§5§l✨ BINDING SUCCESSFUL!",
                "§6§l" + reward.name() + " §fEye bound by §e" + winner.getName(),
                10, 120, 30
            );
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1f);
            // Chat
            for (String line : chatLines) p.sendMessage(line);
        }

        winAnimating = false;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PUBLIC UTILS
    // ══════════════════════════════════════════════════════════════════════════
    public boolean isActive()  { return active; }
    public EyeType getReward() { return reward; }
}
