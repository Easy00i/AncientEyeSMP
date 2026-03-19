package com.ancienteye;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandManager implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;

        switch (cmd.getName().toLowerCase()) {
            case "smpstart" -> {
                if (AncientEyePlugin.get().getPlayerData().getEye(p) != EyeType.NONE) {
                    p.sendMessage("§cYou already have an Eye!");
                    return true;
                }
                AncientEyePlugin.get().getTradeManager().startSmpRitual(p);
            }
            case "trade" -> {
                if (args.length == 0) { p.sendMessage("§cUsage: /trade <player>"); return true; }
                Player target = Bukkit.getPlayer(args[0]);
                if (target != null && target != p) {
                    AncientEyePlugin.get().getTradeManager().sendTradeRequest(p, target);
                }
            }
            case "tradeaccept" -> {
                if (args.length > 0) {
                    Player senderPlayer = Bukkit.getPlayer(args[0]);
                    if (senderPlayer != null) {
                        AncientEyePlugin.get().getTradeManager().executeTrade(senderPlayer, p);
                    }
                }
            }
            case "tradereject" -> p.sendMessage("§cTrade rejected.");
            
            case "eye" -> {
                if (!p.hasPermission("eye.admin")) return true;
                if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
                    Player target = Bukkit.getPlayer(args[1]);
                    try {
                        EyeType eye = EyeType.valueOf(args[2].toUpperCase());
                        AncientEyePlugin.get().getPlayerData().setEye(target, eye, false);
                    } catch (Exception e) { p.sendMessage("§cInvalid Eye!"); }
                }
                if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
                    Player target = Bukkit.getPlayer(args[1]);
                    AncientEyePlugin.get().getPlayerData().resetEye(target);
                }
            }
            case "event" -> {
                if (!p.hasPermission("eye.admin")) return true;
                if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
                    try {
                        EyeType eventEye = EyeType.valueOf(args[1].toUpperCase());
                        AncientEyePlugin.get().getEventManager().startEvent(eventEye);
                    } catch (Exception e) { p.sendMessage("§cInvalid Event Eye!"); }
                }
            }
        }
        return true;
    }
}

