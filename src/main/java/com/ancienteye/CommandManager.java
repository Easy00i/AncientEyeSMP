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
            case "smpstart" -> {
                if (plugin.getPlayerData().getEye(p) != EyeType.NONE) {
                    p.sendMessage("§cYou already have an Eye!");
                    return true;
                }
                plugin.getTradeManager().startSmpRitual(p);
            }
            case "trade" -> {
                if (args.length == 0) { p.sendMessage("§cUsage: /trade <player>"); return true; }
                Player target = Bukkit.getPlayer(args[0]);
                if (target != null && target != p) {
                    plugin.getTradeManager().sendTradeRequest(p, target);
                }
            }
            case "tradeaccept" -> {
                if (args.length > 0) {
                    Player senderPlayer = Bukkit.getPlayer(args[0]);
                    if (senderPlayer != null) {
                        plugin.getTradeManager().executeTrade(senderPlayer, p);
                    }
                }
            }
            case "tradereject" -> p.sendMessage("§cTrade rejected.");
            
            case "eye" -> {
                if (!p.hasPermission("eye.admin")) return true;
                if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) return true;
                    try {
                        EyeType eye = EyeType.valueOf(args[2].toUpperCase());
                        plugin.getPlayerData().setEye(target, eye, false);
                    } catch (Exception e) { p.sendMessage("§cInvalid Eye!"); }
                }
                if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target != null) plugin.getPlayerData().resetEye(target);
                }
            }
            case "event" -> {
                if (!p.hasPermission("eye.admin")) return true;
                if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
                    try {
                        EyeType eventEye = EyeType.valueOf(args[1].toUpperCase());
                        plugin.getEventManager().startEvent(eventEye);
                    } catch (Exception e) { p.sendMessage("§cInvalid Event Eye!"); }
                }
            }
        }
        return true;
    }

    // --- TAB COMPLETER LOGIC ---
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (cmd.getName().equalsIgnoreCase("eye")) {
            if (args.length == 1) {
                completions.add("give");
                completions.add("reset");
            } else if (args.length == 2) {
                // Sugget all online players
                return null; // Returning null defaults to online players list
            } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
                // Suggest all EyeTypes from the Enum
                return Arrays.stream(EyeType.values())
                        .map(Enum::name)
                        .filter(name -> !name.equals("NONE"))
                        .collect(Collectors.toList());
            }
        }

        if (cmd.getName().equalsIgnoreCase("event")) {
            if (args.length == 1) {
                completions.add("start");
                completions.add("stop");
            } else if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
                // Suggest only Event Eyes (Optional: or all Eyes)
                return Arrays.stream(EyeType.values())
                        .map(Enum::name)
                        .filter(name -> !name.equals("NONE"))
                        .collect(Collectors.toList());
            }
        }

        if (cmd.getName().equalsIgnoreCase("trade")) {
            if (args.length == 1) return null; // Player suggestions
        }

        return completions;
    }
}
