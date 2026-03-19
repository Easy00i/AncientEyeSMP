package com.ancienteye;

import org.bukkit.Color;
import org.bukkit.Particle;

public enum EyeType {
    // --- 20 BASIC EYES (Obtainable via /smpstart) ---
    NONE(null, null),
    VOID(Particle.PORTAL, Color.PURPLE),
    PHANTOM(Particle.SQUID_INK, Color.GRAY),
    STORM(Particle.ELECTRIC_SPARK, Color.AQUA),
    FROST(Particle.SNOWFLAKE, Color.WHITE),
    FLAME(Particle.FLAME, Color.RED),
    SHADOW(Particle.SMOKE, Color.BLACK),
    TITAN(Particle.CAMPFIRE_COSY_SMOKE, Color.ORANGE),
    HUNTER(Particle.SOUL, Color.BLUE),
    GRAVITY(Particle.REVERSE_PORTAL, Color.PURPLE),
    WIND(Particle.CLOUD, Color.WHITE),
    POISON(Particle.SNEEZE, Color.GREEN),
    LIGHT(Particle.GLOW, Color.YELLOW),
    EARTH(Particle.CRITTER, Color.fromRGB(139, 69, 19)),
    CRYSTAL(Particle.INSTANT_EFFECT, Color.FUCHSIA),
    ECHO(Particle.SONIC_BOOM, Color.TEAL),
    RAGE(Particle.ANGRY_VILLAGER, Color.RED),
    SPIRIT(Particle.WITCH, Color.LIME),
    TIME(Particle.NAUTILUS, Color.AQUA),
    WARRIOR(Particle.SWEEP_ATTACK, Color.MAROON), // 20th Eye: Warrior
    
    // --- 5 EVENT-ONLY EYES (Exclusive - High Tier) ---
    METEOR(Particle.LAVA, Color.ORANGE),
    MIRAGE(Particle.WHITE_SMOKE, Color.SILVER),
    OCEAN(Particle.WATER_DROP, Color.BLUE),
    ECLIPSE(Particle.DRAGON_BREATH, Color.PURPLE),
    GUARDIAN(Particle.TOTEM_OF_UNDYING, Color.GOLD);

    public final Particle particle;
    public final Color color;

    // Constructor
    EyeType(Particle p, Color c) {
        this.particle = p;
        this.color = c;
    }

    // Helper to check for Event Eyes
    public boolean isEventEye() {
        return this == METEOR || this == MIRAGE || this == OCEAN || this == ECLIPSE || this == GUARDIAN;
    }

    // Helper to get only startable eyes for /smpstart (Random selection)
    public static EyeType getRandomStartEye() {
        List<EyeType> eyes = new ArrayList<>();
        for (EyeType type : values()) {
            if (type != NONE && !type.isEventEye()) {
                eyes.add(type);
            }
        }
        return eyes.get(new java.util.Random().nextInt(eyes.size()));
    }
}
