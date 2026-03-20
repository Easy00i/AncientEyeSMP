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

    // ── Timer ─────────────────────────────────────────────────────────────────
    private long       eventDurationMillis = 0;
    private long       eventStartTime      = 0;
    private BukkitTask timerTask           = null;

    // ── Player data (never cleared on death/disconnect) ───────────────────────
    private final Map<UUID, Integer>      taskProgress = new HashMap<>();
    private final Map<UUID, List<String>> playerTasks  = new HashMap<>();
    private final Map<UUID, Integer>      blockCount   = new HashMap<>();

    // ─────────────────────────────────────────────────────────────────────────
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
            Bukkit.getLogger().warning("[AncientEye] Config mein tasks nahi mile! Default use ho raha hai.");
            allTasks = Arrays.asList("Kill 1 Player", "Break 10 Diamonds", "Survive 2 Minutes");
        }
        final List<String> tasks = new ArrayList<>(allTasks);

        Bukkit.broadcastMessage("§8§m================================");
        Bukkit.broadcastMessage("§6§l  👁  EYE EVENT: §e§l" + eye.name());
        Bukkit.broadcastMessage("§f  You will get 3 tasks, look at the scoreboard.");
        Bukkit.broadcastMessage("§f  ⏰ Time: §e" + formatTimeDisplay(durationMillis));
        Bukkit.broadcastMessage("§8§m================================");

        for (Player p : Bukkit.getOnlinePlayers()) {
            assignTasksToPlayer(p, tasks);
        }

        startTimerTask(tasks);
    }

        // --- STOP EVENT METHOD ---
    public void stopEvent() {
    // 1. Agar event active nahi hai toh kuch mat karo
    if (!active) return;

    // 2. Sabse pehle sari processing band karo
    active = false;
    
    if (timerTask != null) {
        timerTask.cancel();
        timerTask = null;
    }

    // 3. Clear all stored data (Taki koi purana task baki na rahe)
    taskProgress.clear();
    playerTasks.clear();
    blockCount.clear();

    // 4. Sabka SCOREBOARD aur SIDEBAR remove karo (Normal condition mein wapas)
    for (Player p : Bukkit.getOnlinePlayers()) {
        Scoreboard board = p.getScoreboard();
        // Humne startEvent mein objective ka naam "event" rakha tha
        Objective obj = board.getObjective("event"); 
        
        if (obj != null) {
            obj.unregister(); // Ye sidebar ko screen se bilkul delete kar dega
        }
        
        p.sendTitle("§c§lEVENT STOPPED", "§7SAdmin.", 10, 40, 10);
    }

    Bukkit.broadcastMessage("§c§l[AncientEye] §7Event cancel by Admin .");
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

            // ── TIME UP ──────────────────────────────────────────────────────
            if (remaining <= 0) {
                active = false;
                timerTask.cancel();
                timerTask = null;

                Bukkit.broadcastMessage("§8§m================================");
                Bukkit.broadcastMessage("§c§l  ⏰  TIME UP!");
                Bukkit.broadcastMessage("§7  Fhahhhhhhhh.");
                Bukkit.broadcastMessage("§8§m================================");

                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
                    p.sendTitle("§c§l⏰ TIME UP!", "§7Koi winner nahi...", 10, 80, 20);
                    p.playSound(p.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.8f, 0.7f);
                }
                return;
            }

            // ── UPDATE SCOREBOARDS ────────────────────────────────────────────
            if (winAnimating) return;

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!playerTasks.containsKey(p.getUniqueId())) continue;
                String t = getCurrentTaskName(p);
                if (t != null && !t.isEmpty()) {
                    updateScoreboard(p, t, remaining);
                }
            }

        }, 20L, 20L); // every 1 second
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

        // Unique strings per line (extra spaces to avoid duplicate score bugs)
        obj.getScore("§7§m——————————————").setScore(7);
        obj.getScore(" §e⏰ §f" + formatTimeScoreboard(remainMillis)).setScore(6);
        obj.getScore("§r ").setScore(5);
        obj.getScore(" §fTask §6" + taskNum + "§f/§e3").setScore(4);
        obj.getScore(" §a▶ §f" + desc).setScore(3);
        obj.getScore("§r  ").setScore(2);
        obj.getScore("§7§m——————————————§r ").setScore(1);

        p.setScoreboard(board);
    }

    // Scoreboard time: compact
    private String formatTimeScoreboard(long ms) {
        if (ms <= 0) return "§c00:00";
        long s = ms / 1000;
        long d = s / 86400, h = (s % 86400) / 3600, m = (s % 3600) / 60, sec = s % 60;
        if (d > 0) return String.format("§e%dd §f%02d:%02d:%02d", d, h, m, sec);
        if (h > 0) return String.format("§f%02dh %02dm", h, m);
        if (s < 60) return String.format("§c%02d:%02d", m, sec);
        return String.format("§f%02d:%02d", m, sec);
    }

    // Broadcast time: readable
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

        // ── Diamond task ──────────────────────────────────────────────────────
        if (task.contains("Diamonds") &&
                (e.getBlock().getType() == Material.DIAMOND_ORE ||
                 e.getBlock().getType() == Material.DEEPSLATE_DIAMOND_ORE)) {
            int c = blockCount.getOrDefault(p.getUniqueId(), 0) + 1;
            blockCount.put(p.getUniqueId(), c);
            p.sendActionBar("§b💎 Diamonds: §f" + c + " §7/ §f10");
            if (c >= 10) handleTaskCompletion(p, task);
        }

        // ── Iron task ─────────────────────────────────────────────────────────
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

        // Restore scoreboard after reconnect (delayed to ensure client is ready)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline() || !active || winAnimating) return;
            String t = getCurrentTaskName(p);
            if (t != null && !t.isEmpty()) {
                updateScoreboard(p, t, getRemainingMillis());
            }
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
            if (t != null && !t.isEmpty()) {
                updateScoreboard(p, t, getRemainingMillis());
            }
        }, 2L);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TASK LOGIC
    // ══════════════════════════════════════════════════════════════════════════
    private void handleTaskCompletion(Player p, String goal) {
        String current = getCurrentTaskName(p);
        if (current != null && current.equals(goal)) {
            advanceTask(p);
        }
    }

    private void advanceTask(Player p) {
        if (!playerTasks.containsKey(p.getUniqueId())) return;
        int cur = taskProgress.getOrDefault(p.getUniqueId(), 1);
        blockCount.put(p.getUniqueId(), 0); // reset per-task counter only

        if (cur >= 3) {
            // All 3 tasks done!
            taskProgress.put(p.getUniqueId(), 4); // mark complete, prevents re-trigger
            win(p);
            return;
        }

        int next = cur + 1;
        taskProgress.put(p.getUniqueId(), next);
        String nextTask = playerTasks.get(p.getUniqueId()).get(next - 1);

        p.sendTitle("§a§l✓ TASK " + cur + " DONE!", "§eNext: §f" + nextTask, 10, 50, 10);
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
        updateScoreboard(p, nextTask, getRemainingMillis());

        // ── Survive task: schedule auto-complete ─────────────────────────────
        if (nextTask.toLowerCase().contains("survive")) {
            int mins = parseSurviveMins(nextTask);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!active) return;
                if (!p.isOnline()) return;
                Integer prog = taskProgress.get(p.getUniqueId());
                if (prog == null || prog != next) return; // guard: already advanced
                String cur2 = getCurrentTaskName(p);
                if (cur2 != null && cur2.equals(nextTask)) advanceTask(p);
            }, (long) mins * 1200L); // mins * 60s * 20tps
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

        // Stop timer
        if (timerTask != null) { timerTask.cancel(); timerTask = null; }

        // Broadcast
        Bukkit.broadcastMessage("§8§m================================");
        Bukkit.broadcastMessage("§6§l  🏆 " + winner.getName() + " §ene sab tasks complete kiye!");
        Bukkit.broadcastMessage("§f  §5§l" + reward.name() + " §fko bind kar raha/rahi hai...");
        Bukkit.broadcastMessage("§8§m================================");

        // Title to all, remove scoreboard from everyone except winner
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle("§6§l" + winner.getName(),
                    "§fis Binding the §5§l" + reward.name() + "§f!", 10, 70, 20);
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

        // Progress bar (BossBar) — only for winner
        BossBar bar = Bukkit.createBossBar(
                "§5§lBinding §6" + reward.name() + "§5§l... §e0%",
                BarColor.PURPLE, BarStyle.SEGMENTED_10);
        bar.setProgress(0.0);
        if (winner.isOnline()) bar.addPlayer(winner);

        // Initial effects
        if (winner.isOnline()) {
            // Levitate ~4 blocks (amplifier 2 ≈ 4 block rise over 10 s)
            winner.addPotionEffect(
                    new PotionEffect(PotionEffectType.LEVITATION, 220, 2, false, false, false));
            // Slow fall after levitation ends
            winner.addPotionEffect(
                    new PotionEffect(PotionEffectType.SLOW_FALLING, 280, 0, false, false, false));

            winner.getWorld().strikeLightningEffect(winner.getLocation());
            winner.getWorld().playSound(winner.getLocation(),
                    Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 0.5f);
        }

        final int[]    tick  = {0};
        final double[] angle = {0.0};
        final BukkitTask[] ref = {null};

        ref[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            tick[0]++;

            // ── Winner disconnected mid-animation ──────────────────────────
            if (!winner.isOnline()) {
                bar.removeAll();
                if (ref[0] != null) ref[0].cancel();
                finishWinReward(winner);
                return;
            }

            // ── Boss bar progress ──────────────────────────────────────────
            double prog = Math.min(tick[0] / 200.0, 1.0);
            bar.setProgress(prog);
            int pct = (int)(prog * 100);
            bar.setTitle("§5§lBinding §6" + reward.name() + "§5§l... §e" + pct + "%");

            // ── Ring Particles (Doctor Strange orbit style) ────────────────
            angle[0] += 12; // degrees per tick
            Location base = winner.getLocation().add(0, 1.8, 0); // near eye level
            double radius = 1.5;

            // Ring 1: horizontal (XZ) — END_ROD (bright white)
            for (int i = 0; i < 16; i++) {
                double a = Math.toRadians(angle[0] + i * 22.5);
                winner.getWorld().spawnParticle(Particle.END_ROD,
                        base.clone().add(Math.cos(a) * radius, 0, Math.sin(a) * radius),
                        1, 0, 0, 0, 0);
            }

            // Ring 2: vertical (XY) — DRAGON_BREATH (purple)
            double rOff = Math.toRadians(angle[0] * 1.4);
            for (int i = 0; i < 16; i++) {
                double a = Math.toRadians(angle[0] + i * 22.5);
                winner.getWorld().spawnParticle(Particle.DRAGON_BREATH,
                        base.clone().add(Math.cos(rOff) * radius,
                                Math.sin(a) * radius,
                                Math.sin(rOff) * radius),
                        1, 0, 0, 0, 0);
            }

            
            // Ring 3: diagonal — PORTAL (swirling purple)
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

            // ── Purple beam from sky to player (every 3 ticks) ────────────
            if (tick[0] % 3 == 0) {
                Location loc = winner.getLocation();
                for (int y = 0; y <= 22; y++) {
                    winner.getWorld().spawnParticle(Particle.DRAGON_BREATH,
                            loc.clone().add(0, y, 0), 2, 0.12, 0, 0.12, 0);
                    winner.getWorld().spawnParticle(Particle.PORTAL,
                            loc.clone().add(0, y, 0), 1, 0.08, 0, 0.08, 0);
                }
            }

            // ── Sound effects ─────────────────────────────────────────────
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

            // ── ANIMATION COMPLETE (200 ticks = 10 seconds) ───────────────
            if (tick[0] >= 200) {
                ref[0].cancel();
                bar.removeAll();

                winner.removePotionEffect(PotionEffectType.LEVITATION);

                // Final burst
                winner.getWorld().strikeLightningEffect(winner.getLocation());
                winner.getWorld().playSound(winner.getLocation(),
                        Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 0.5f);
                winner.getWorld().playSound(winner.getLocation(),
                        Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);

                // Explosion of END_ROD + DRAGON_BREATH
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

        }, 1L, 1L); // every tick
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FINISH WIN  — give eye + announcements
    // ══════════════════════════════════════════════════════════════════════════
    private void finishWinReward(Player winner) {
        // Give the eye
        plugin.getPlayerData().setEye(winner, reward, false);

        // Clear winner's scoreboard
        if (winner.isOnline()) {
            winner.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }

        // Broadcast
        Bukkit.broadcastMessage("§8§m================================");
        Bukkit.broadcastMessage("§5§l  ✨  SUCCESSFUL BOUND!");
        Bukkit.broadcastMessage("§e  " + winner.getName() +
                " §fne §5§l" + reward.name() + " §fsuccessfully bound kiya!");
        Bukkit.broadcastMessage("§8§m================================");

        // Title + sound for all
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(
                    "§5§l✨ Successful Bound!",
                    "§6§l" + reward.name() + " §fbound by §e" + winner.getName(),
                    10, 100, 30);
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1f);
        }

        winAnimating = false;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PUBLIC UTILS
    // ══════════════════════════════════════════════════════════════════════════
    public boolean isActive()  { return active; }
    public EyeType getReward() { return reward; }
}
