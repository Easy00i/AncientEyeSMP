package com.ancienteye;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandManager implements CommandExecutor, TabCompleter {

    private final AncientEyePlugin plugin;

    public CommandManager(AncientEyePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player p)) return true;

        switch (cmd.getName().toLowerCase()) {
            case "smpstart" -> { // PUBLIC
                if (plugin.getPlayerData().getEye(p) != EyeType.NONE) {
                    p.sendMessage("§cYou already have an Eye!");
                    return true;
                }
                plugin.getTradeManager().startSmpRitual(p);
            }
            case "trade" -> { // PUBLIC
                if (args.length == 0) { p.sendMessage("§cUsage: /trade <player>"); return true; }
                Player target = Bukkit.getPlayer(args[0]);
                if (target != null && target != p) {
                    plugin.getTradeManager().sendTradeRequest(p, target);
                }
            }
            case "tradeaccept" -> { // PUBLIC
                if (args.length > 0) {
                    Player senderPlayer = Bukkit.getPlayer(args[0]);
                    if (senderPlayer != null) {
                        plugin.getTradeManager().executeTrade(senderPlayer, p);
                    }
                }
            }
            case "tradereject" -> p.sendMessage("§cTrade rejected."); // PUBLIC
            
            case "eye" -> {
                if (args.length == 1 && args[0].equalsIgnoreCase("gui")) { // PUBLIC
                    plugin.getAbilityLogic().openEyeGUI(p);
                    return true;
                }

                // ADMIN CHECK START (Baaki sab eye commands admin ke liye)
                if (!p.hasPermission("eye.admin")) {
                    p.sendMessage("§cNo permission to use admin commands!");
                    return true;
                }
                
                if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                    plugin.reloadConfig();
                    p.sendMessage("§a[AncientEye] Config reloaded successfully!");
                    return true;
                }

                if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) { p.sendMessage("§cPlayer not found!"); return true; }
                    try {
                        EyeType eye = EyeType.valueOf(args[2].toUpperCase());
                        plugin.getPlayerData().setEye(target, eye, false);
                        p.sendMessage("§aGave " + eye.name() + " to " + target.getName());
                    } catch (Exception e) { p.sendMessage("§cInvalid Eye Type!"); }
                }
                
                if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target != null) {
                        plugin.getPlayerData().setEye(target, EyeType.NONE, false);
                        p.sendMessage("§aEye reset for " + target.getName());
                    }
                }
            }
            case "event" -> { // ADMIN ONLY
                if (!p.hasPermission("eye.admin")) {
                    p.sendMessage("§cNo permission!");
                    return true;
                }
                
                if (args.length >= 1) {
                    if (args[0].equalsIgnoreCase("start")) {
                        if (args.length >= 3) {
                            try {
                                EyeType eventEye = EyeType.valueOf(args[1].toUpperCase());
                                long duration = EventManager.parseTime(args[2]);

                                if (duration <= 0) {
                                    p.sendMessage("§cInvalid time! Use: 10m, 30m, 1h, etc.");
                                    return true;
                                }
                                plugin.getEventManager().startEvent(eventEye, duration);
                            } catch (Exception e) { p.sendMessage("§cInvalid Event Eye Type!"); }
                        } else {
                            p.sendMessage("§cUsage: /event start <EyeType> <Time>");
                        }
                    } else if (args[0].equalsIgnoreCase("stop")) {
                        plugin.getEventManager().stopEvent();
                        p.sendMessage("§a[AncientEye] Current event stopped.");
                    }
                }
            }
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        String cmdName = cmd.getName().toLowerCase();

        if (cmdName.equals("eye")) {
            if (args.length == 1) {
                completions.add("gui");
                if (sender.hasPermission("eye.admin")) {
                    completions.add("give");
                    completions.add("reset");
                    completions.add("reload");
                }
            } else if (args.length == 2 && sender.hasPermission("eye.admin")) {
                if (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("reset")) {
                    return null; // Player names suggest karega
                }
            } else if (args.length == 3 && args[0].equalsIgnoreCase("give") && sender.hasPermission("eye.admin")) {
                return Arrays.stream(EyeType.values())
                        .map(Enum::name)
                        .filter(name -> !name.equals("NONE"))
                        .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (cmdName.equals("event") && sender.hasPermission("eye.admin")) {
            if (args.length == 1) {
                completions.add("start");
                completions.add("stop");
            } else if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
                return Arrays.stream(EyeType.values())
                        .filter(EyeType::isEventEye)
                        .map(Enum::name)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args.length == 3 && args[0].equalsIgnoreCase("start")) {
                completions.addAll(Arrays.asList("5m", "10m", "30m", "1h"));
            }
        }

        // Trade aur TradeAccept dono ke liye player names suggest honge
        if (cmdName.equals("trade") || cmdName.equals("tradeaccept")) {
            if (args.length == 1) return null; // NULL return karne par Bukkit online players suggest karta hai
        }

        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length-1].toLowerCase()))
                .collect(Collectors.toList());
    }
}
