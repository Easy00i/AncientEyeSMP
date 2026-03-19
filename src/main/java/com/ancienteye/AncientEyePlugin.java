package com.ancienteye;

import org.bukkit.plugin.java.JavaPlugin;

public class AncientEyePlugin extends JavaPlugin {
    private static AncientEyePlugin instance;
    
    private PlayerDataManager playerDataManager;
    private CooldownManager cooldownManager;
    private AbilityLogic abilityLogic;
    private AnimationTradeManager animationTradeManager;
    private CommandManager commandManager;
    private EventManager eventManager;

    @Override
    public void onEnable() {
        instance = this;
        
        // 1. Initialize Managers
        this.playerDataManager = new PlayerDataManager(this);
        this.cooldownManager = new CooldownManager(this);
        this.abilityLogic = new AbilityLogic(this);
        this.animationTradeManager = new AnimationTradeManager(this);
        this.eventManager = new EventManager(this);
        this.commandManager = new CommandManager(this);

        // 2. Register Listeners 
        getServer().getPluginManager().registerEvents(new AbilityTrigger(this), this);
        getServer().getPluginManager().registerEvents(eventManager, this);
        getServer().getPluginManager().registerEvents(abilityLogic, this); 

        // 3. Register Commands & Tab Completers
        if (getCommand("smpstart") != null) {
            getCommand("smpstart").setExecutor(commandManager);
            getCommand("smpstart").setTabCompleter(commandManager);
        }
        if (getCommand("eye") != null) {
            getCommand("eye").setExecutor(commandManager);
            getCommand("eye").setTabCompleter(commandManager);
        }
        if (getCommand("trade") != null) {
            getCommand("trade").setExecutor(commandManager);
            getCommand("trade").setTabCompleter(commandManager);
        }
        if (getCommand("event") != null) {
            getCommand("event").setExecutor(commandManager);
            getCommand("event").setTabCompleter(commandManager);
        }

        getLogger().info("Ancient Eye SMP Plugin Loaded Successfully!");
    }

    public static AncientEyePlugin get() { return instance; }
    public PlayerDataManager getPlayerData() { return playerDataManager; }
    public PlayerDataManager getDataManager() { return playerDataManager; }
    public CooldownManager getCooldownManager() { return cooldownManager; }
    public AbilityLogic getAbilityLogic() { return abilityLogic; }
    public AnimationTradeManager getTradeManager() { return animationTradeManager; }
    public EventManager getEventManager() { return eventManager; }
}
