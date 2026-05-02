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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerJoinListener implements Listener {

    private final AncientEyePlugin plugin;
    private final String guiTitle = "§8Choose Your Path";
    
    // Naya Fix: Ye un players ko track karega jo Power choose kar chuke hain
    // Taaki close event GUI ko galti se dobara na khol de.
    private final Set<UUID> pendingSpin = new HashSet<>();

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

        // Delay added: 5 ticks
        Bukkit.getScheduler().runTaskLater(plugin, () -> openLifeChoiceGUI(p), 1200L);
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

        gui.setItem(2, peaceful);
        gui.setItem(6, power);

        p.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals(guiTitle)) return;
        
        // 100% Item nikalna block
        e.setCancelled(true);
        
        if (!(e.getWhoClicked() instanceof Player p)) return;

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (clicked.getType() == Material.LILY_OF_THE_VALLEY) {
            plugin.getPlayerData().setPeaceful(p);
            p.closeInventory();
            
            // Chat message hataya, Screen par Title lagaya
            p.sendTitle("§a§lPEACEFUL LIFE🤡", "§7lol You chose a normal life 🤞", 10, 60, 10);
            
        } else if (clicked.getType() == Material.BLAZE_ROD) {
            // Player ko pending list mein daal diya taaki GUI wapas na khule
            pendingSpin.add(p.getUniqueId());
            p.closeInventory();
            
            // Chat message hataya, Screen par Title lagaya
            p.sendTitle("§c§lPOWER LIFE💀", "§7The ritual begins...", 10, 60, 10);
            
            // 3 Seconds (60 Ticks) ka delay, uske baad Spin start!
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                plugin.getTradeManager().startSmpRitual(p);
                pendingSpin.remove(p.getUniqueId()); // Spin start hone par list se hata diya
            }, 60L);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!e.getView().getTitle().equals(guiTitle)) return;
        
        if (e.getPlayer() instanceof Player player) {
            // MAIN FIX: Agar player Spin start hone ka wait kar raha hai, toh GUI wapas MAT kholo
            if (pendingSpin.contains(player.getUniqueId())) return;

            // Agar player ne escape dabakar bina kuch choose kiye band kiya, tabhi wapas open hoga
            if (!plugin.getPlayerData().isPeaceful(player) && plugin.getPlayerData().getEye(player) == EyeType.NONE) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> openLifeChoiceGUI(player), 5L);
            }
        }
    }
}
