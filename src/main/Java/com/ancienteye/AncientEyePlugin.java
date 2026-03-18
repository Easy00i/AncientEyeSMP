package com.ancienteye;

import org.bukkit.plugin.java.JavaPlugin;

public class AncientEyePlugin extends JavaPlugin {
    
    private static AncientEyePlugin instance;
    private EyeManager eyeManager;
    private AbilityListener abilityListener;
    private CommandEventSys commandEventSys;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig(); // config.yml load karega

        this.eyeManager = new EyeManager(this);
        this.abilityListener = new AbilityListener(this);
        this.commandEventSys = new CommandEventSys(this);

        getServer().getPluginManager().registerEvents(abilityListener, this);
        getServer().getPluginManager().registerEvents(commandEventSys, this);
        getServer().getPluginManager().registerEvents(eyeManager, this);

        // Commands register
        getCommand("smpstart").setExecutor(commandEventSys);
        getCommand("eye").setExecutor(commandEventSys);
        getCommand("eye").setTabCompleter(commandEventSys);
        getCommand("trade").setExecutor(commandEventSys);
        getCommand("tradeaccept").setExecutor(commandEventSys);
        getCommand("event").setExecutor(commandEventSys);
        getCommand("event").setTabCompleter(commandEventSys);

        getLogger().info("AncientEyeSMP (1.21.11) enabled successfully!");
    }

    public static AncientEyePlugin get() { return instance; }
    public EyeManager getEyeManager() { return eyeManager; }
    public AbilityListener getAbilityListener() { return abilityListener; }
    public CommandEventSys getCommandEventSys() { return commandEventSys; }
}
