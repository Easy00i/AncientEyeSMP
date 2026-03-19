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
        
        // 1. Initialize Managers (Pass 'this' to satisfy the constructors)
        this.playerDataManager = new PlayerDataManager(this);
        this.cooldownManager = new CooldownManager(this);
        this.abilityLogic = new AbilityLogic(this);
        this.animationTradeManager = new AnimationTradeManager(this);
        this.eventManager = new EventManager(this);
        
        // CommandManager usually needs the plugin instance too
        this.commandManager = new CommandManager(this);

        // 2. Register Listeners (Pass necessary instances to the triggers)
        getServer().getPluginManager().registerEvents(new AbilityTrigger(this, abilityLogic), this);
        getServer().getPluginManager().registerEvents(eventManager, this);
        getServer().getPluginManager().registerEvents(abilityLogic, this); 

        // 3. Register Commands
        getCommand("smpstart").setExecutor(commandManager);
        getCommand("eye").setExecutor(commandManager);
        getCommand("trade").setExecutor(commandManager);
        getCommand("event").setExecutor(commandManager);

        getLogger().info("Ancient Eye SMP Plugin Loaded Successfully!");
    }

    public static AncientEyePlugin get() { return instance; }
    public PlayerDataManager getPlayerData() { return playerDataManager; }
    
    // Compatibility method if other classes use 'getDataManager'
    public PlayerDataManager getDataManager() { return playerDataManager; }
    
    public CooldownManager getCooldownManager() { return cooldownManager; }
    public AbilityLogic getAbilityLogic() { return abilityLogic; }
    public AnimationTradeManager getTradeManager() { return animationTradeManager; }
    public EventManager getEventManager() { return eventManager; }
}
