package com.ancienteye;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class PlayerJoinListener implements Listener {

    private final AncientEyePlugin plugin;
    private final String guiTitle = "§8Choose Your Path";

    public PlayerJoinListener(AncientEyePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        EyeType eye = plugin.getPlayerData().getEye(p);
        if (eye != EyeType.NONE) {
            plugin.getAbilityLogic().applyPassiveEffects(p, eye);
            return;
        }

        if (plugin.getPlayerData().isPeaceful(p)) {
            return;
        }

        // Delay added: 5 ticks taaki join process finish ho jaye aur GUI open ho sake
        Bukkit.getScheduler().runTaskLater(plugin, () -> openLifeChoiceGUI(p), 5L);
    }

    public void openLifeChoiceGUI(Player p) {
        Inventory gui = Bukkit.createInventory(null, 9, guiTitle);

        ItemStack peaceful = new ItemStack(Material.LILY_OF_THE_VALLEY);
        ItemMeta peacefulMeta = peaceful.getItemMeta();
        peacefulMeta.setDisplayName("§a§lPEACEFUL LIFE");
        peacefulMeta.setLore(Arrays.asList("§7No powers, no eyes.", "§7Just a normal life."));
        peaceful.setItemMeta(peacefulMeta);

        ItemStack power = new ItemStack(Material.BLAZE_ROD);
        ItemMeta powerMeta = power.getItemMeta();
        powerMeta.setDisplayName("§c§lPOWER LIFE");
        powerMeta.setLore(Arrays.asList("§7Get an Ancient Eye", "§7and unlock special powers!"));
        power.setItemMeta(powerMeta);

        // Peaceful Left (Slot 2) aur Power Right (Slot 6) par rakha hai
        gui.setItem(2, peaceful);
        gui.setItem(6, power);

        p.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals(guiTitle)) return;
        
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (clicked.getType() == Material.LILY_OF_THE_VALLEY) {
            p.closeInventory();
            plugin.getPlayerData().setPeaceful(p);
            p.sendMessage("§aLol, you chose Peaceful Life. Good luck! 🤞");
        } else if (clicked.getType() == Material.BLAZE_ROD) {
            p.closeInventory();
            p.sendMessage("§cYou chose Power Life! The ritual begins...");
            plugin.getTradeManager().startSmpRitual(p);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!e.getView().getTitle().equals(guiTitle)) return;
        
        if (e.getPlayer() instanceof Player player) {
            // Agar player ne choose nahi kiya aur band kiya, toh wapas open hoga
            if (!plugin.getPlayerData().isPeaceful(player) && plugin.getPlayerData().getEye(player) == EyeType.NONE) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> openLifeChoiceGUI(player), 5L);
            }
        }
    }
}
