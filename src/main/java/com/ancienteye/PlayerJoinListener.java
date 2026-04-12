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
import org.bukkit.metadata.FixedMetadataValue;

import java.util.Arrays;

public class PlayerJoinListener implements Listener {

    private final AncientEyePlugin plugin;

    public PlayerJoinListener(AncientEyePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        
        // Agar player ke paas already eye hai to passive effect laga do aur return
        EyeType eye = plugin.getPlayerData().getEye(p);
        if (eye != EyeType.NONE) {
            plugin.getAbilityLogic().applyPassiveEffects(p, eye);
            return;
        }
        
        // Agar player ne pehle choice kar li hai (Peaceful) to kuch mat karo
        if (p.hasMetadata("lifeChoiceMade")) {
            return;
        }
        
        // Nahi to GUI dikhao
        openLifeChoiceGUI(p);
    }
    
    private void openLifeChoiceGUI(Player p) {
        Inventory gui = Bukkit.createInventory(null, 9, "§8Choose Your Path");
        
        // Peaceful Life (Lily of the Valley – pink/white flower)
        ItemStack peaceful = new ItemStack(Material.LILY_OF_THE_VALLEY);
        ItemMeta peacefulMeta = peaceful.getItemMeta();
        peacefulMeta.setDisplayName("§a§lPEACEFUL LIFE");
        peacefulMeta.setLore(Arrays.asList("§7No powers, no eyes.", "§7Just a normal life."));
        peaceful.setItemMeta(peacefulMeta);
        
        // Power Life (Blaze Rod – power symbol)
        ItemStack power = new ItemStack(Material.BLAZE_ROD);
        ItemMeta powerMeta = power.getItemMeta();
        powerMeta.setDisplayName("§c§lPOWER LIFE");
        powerMeta.setLore(Arrays.asList("§7Get an Ancient Eye", "§7and unlock special powers!"));
        power.setItemMeta(powerMeta);
        
        gui.setItem(2, peaceful);
        gui.setItem(6, power);
        
        p.openInventory(gui);
        
        // Click handler
        plugin.getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @Override
            public void onInventoryClick(InventoryClickEvent e) {
                if (!e.getInventory().equals(gui)) return;
                e.setCancelled(true); // Koi item nahi nikal sakta
                
                if (e.getCurrentItem() == null) return;
                ItemStack clicked = e.getCurrentItem();
                
                if (clicked.getType() == Material.LILY_OF_THE_VALLEY) {
                    // Peaceful Life
                    p.closeInventory();
                    p.setMetadata("lifeChoiceMade", new FixedMetadataValue(plugin, true));
                    p.sendMessage("§aLol, you chose Peaceful Life. Good luck! 🤞");
                    org.bukkit.event.HandlerList.unregister(this);
                    
                } else if (clicked.getType() == Material.BLAZE_ROD) {
                    // Power Life
                    p.closeInventory();
                    p.setMetadata("lifeChoiceMade", new FixedMetadataValue(plugin, true));
                    p.sendMessage("§cYou chose Power Life! The ritual begins...");
                    plugin.getTradeManager().startSmpRitual(p); // Spin animation
                    org.bukkit.event.HandlerList.unregister(this);
                }
            }
            
            @EventHandler
            public void onInventoryClose(InventoryCloseEvent e) {
                if (!e.getInventory().equals(gui)) return;
                if (e.getPlayer() instanceof Player player) {
                    if (!player.hasMetadata("lifeChoiceMade")) {
                        // Choice nahi kiya to GUI dubara khol do
                        Bukkit.getScheduler().runTaskLater(plugin, () -> openLifeChoiceGUI(player), 1L);
                    }
                }
            }
        }, plugin);
    }
}
