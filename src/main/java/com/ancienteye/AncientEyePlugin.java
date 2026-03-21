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
    private EyeHUDTask eyeHUDTask;  // ADD

    @Override
    public void onEnable() {
        instance = this;

        // 0. Setup Config
        saveDefaultConfig();

        // 1. Initialize Managers
        this.playerDataManager     = new PlayerDataManager(this);
        this.cooldownManager       = new CooldownManager(this);
        this.abilityLogic          = new AbilityLogic(this);
        this.animationTradeManager = new AnimationTradeManager(this);
        this.eventManager          = new EventManager(this);
        this.commandManager        = new CommandManager(this);

        // 2. Register Listeners
        getServer().getPluginManager().registerEvents(new AbilityTrigger(this),     this);
        getServer().getPluginManager().registerEvents(eventManager,                 this);
        getServer().getPluginManager().registerEvents(abilityLogic,                 this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        // 3. Start Background Tasks
        new ParticleTask(this).runTaskTimer(this, 0, 5L);

        // ADD — Eye HUD icon (offhand, resource pack se colored eye dikhega)
        this.eyeHUDTask = new EyeHUDTask(this);
        eyeHUDTask.runTaskTimer(this, 0L, 1L);

        // 4. Register Commands
        registerCommand("smpstart");
        registerCommand("eye");
        registerCommand("trade");
        registerCommand("event");
        registerCommand("tradeaccept");
        registerCommand("tradereject");

        getLogger().info("Ancient Eye SMP Plugin Loaded Successfully!");
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            playerDataManager.saveAllData();
        }

        for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            p.setWalkSpeed(0.2f);
            p.removePotionEffect(org.bukkit.potion.PotionEffectType.JUMP_BOOST);
            p.removePotionEffect(org.bukkit.potion.PotionEffectType.LEVITATION);
        }

        org.bukkit.Bukkit.getLogger().info("[AncientEye] Disabled!");
    }

    private void registerCommand(String name) {
        if (getCommand(name) != null) {
            getCommand(name).setExecutor(commandManager);
            getCommand(name).setTabCompleter(commandManager);
        }
    }

    public static AncientEyePlugin get()              { return instance; }
    public PlayerDataManager getPlayerData()           { return playerDataManager; }
    public PlayerDataManager getDataManager()          { return playerDataManager; }
    public CooldownManager getCooldownManager()        { return cooldownManager; }
    public AbilityLogic getAbilityLogic()              { return abilityLogic; }
    public AnimationTradeManager getTradeManager()     { return animationTradeManager; }
    public EventManager getEventManager()              { return eventManager; }
    public EyeHUDTask getEyeHUDTask()                 { return eyeHUDTask; }  // ADD
}
