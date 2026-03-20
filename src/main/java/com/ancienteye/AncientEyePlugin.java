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
        
        // 0. Setup Config (Very Important!)
        saveDefaultConfig();
        
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
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        
        // 3. Start Background Tasks (Particles)
        // Har 5 ticks (0.25s) mein particles spawn honge
        new ParticleTask(this).runTaskTimer(this, 0, 5L);

        // 4. Register Commands
        registerCommand("smpstart");
        registerCommand("eye");
        registerCommand("trade");
        registerCommand("event");
        
        // Trade buttons ke liye ye do extra commands chahiye
        registerCommand("tradeaccept");
        registerCommand("tradereject");

        getLogger().info("Ancient Eye SMP Plugin Loaded Successfully!");
    }

    @Override
public void onDisable() {
    // 1. Sabhi online players ki speed aur effects reset karo
    for (Player p : Bukkit.getOnlinePlayers()) {
        // Agar koi trade ke beech mein freeze tha, toh use wapas normal karo
        p.setWalkSpeed(0.2f); 
        p.removePotionEffect(PotionEffectType.JUMP_BOOST);
        p.removePotionEffect(PotionEffectType.LEVITATION);
        
        // 2. Event scoreboard bhi saaf kar do taaki reload ke baad glitch na ho
        if (getEventManager() != null && getEventManager().isActive()) {
             p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
    }
    
    Bukkit.getLogger().info("[AncientEye] Plugin disabled and players reset!");
}

    // Helper method to save space
    private void registerCommand(String name) {
        if (getCommand(name) != null) {
            getCommand(name).setExecutor(commandManager);
            getCommand(name).setTabCompleter(commandManager);
        }
    }

    public static AncientEyePlugin get() { return instance; }
    public PlayerDataManager getPlayerData() { return playerDataManager; }
    public PlayerDataManager getDataManager() { return playerDataManager; }
    public CooldownManager getCooldownManager() { return cooldownManager; }
    public AbilityLogic getAbilityLogic() { return abilityLogic; }
    public AnimationTradeManager getTradeManager() { return animationTradeManager; }
    public EventManager getEventManager() { return eventManager; }
}
