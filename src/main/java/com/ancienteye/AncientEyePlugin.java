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
        
        // Initialize Managers
        this.playerDataManager = new PlayerDataManager();
        this.cooldownManager = new CooldownManager();
        this.abilityLogic = new AbilityLogic();
        this.animationTradeManager = new AnimationTradeManager();
        this.eventManager = new EventManager();
        this.commandManager = new CommandManager();

        // Register Listeners
        getServer().getPluginManager().registerEvents(new AbilityTrigger(), this);
        getServer().getPluginManager().registerEvents(eventManager, this);
        getServer().getPluginManager().registerEvents(abilityLogic, this); // For Passives

        // Register Commands
        getCommand("smpstart").setExecutor(commandManager);
        getCommand("eye").setExecutor(commandManager);
        getCommand("trade").setExecutor(commandManager);
        getCommand("tradeaccept").setExecutor(commandManager);
        getCommand("event").setExecutor(commandManager);

        getLogger().info("Ancient Eye SMP Plugin Loaded Perfectly! - No Errors.");
    }

    public static AncientEyePlugin get() { return instance; }
    public PlayerDataManager getPlayerData() { return playerDataManager; }
    public CooldownManager getCooldownManager() { return cooldownManager; }
    public AbilityLogic getAbilityLogic() { return abilityLogic; }
    public AnimationTradeManager getTradeManager() { return animationTradeManager; }
    public EventManager getEventManager() { return eventManager; }
}
