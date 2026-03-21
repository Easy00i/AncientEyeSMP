package com.ancienteye;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * EyeHUDTask — Action Bar + Font trick
 *
 * Hunger bar ke UPAR eye icon dikhata hai.
 * Offhand bilkul free rehta hai — shield, torch sab use kar sakte ho.
 *
 * Kaise kaam karta hai:
 *   1. Resource pack mein hud.json font define hai
 *   2. \uF664 = +100px positive space → text center se right side shift hota hai
 *   3. Eye char (U+E000+) = colored eye PNG, ascent=-8 → hunger bar level par aa jaata hai
 *   4. Action bar mein yeh string send karte hain har tick
 *
 * Position:
 *   ❤❤❤❤❤  🍗🍗🍗🍗🍗
 *                        [👁]  ← yahan dikhega (hunger bar ke right upar)
 *
 * Requires: AncientEye_Final_Pack.zip resource pack
 */
public class EyeHUDTask extends BukkitRunnable {

    private final AncientEyePlugin plugin;

    // ── Space chars from hud.json font ────────────────────────────────────────
    // \uF680 = +128px  \uF640 = +64px  \uF620 = +32px  \uF610 = +16px
    // Hunger bar right side ke liye ~+100px chahiye GUI scale 2 par
    // Tune karo agar position off ho:
    //   Zyada right chahiye → \uF680\uF640  (+192px)
    //   Thoda kam          → \uF640\uF620   (+96px)
    //   Abhi default       → \uF664         (+100px approx via 64+32+4)
    private static final String PUSH = "\uF640\uF620\uF610\uF608";  // ~100px right

    // ── Tiny negative space after icon to reset cursor ────────────────────────
    private static final String RESET = "\uF701";  // -1px

    public EyeHUDTask(AncientEyePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            EyeType eye = plugin.getPlayerData().getEye(p);
            if (eye == null || eye == EyeType.NONE) continue;

            // PUSH + eye icon char
            // Font ascent=-8 automatically places it at hunger bar level
            String hud = PUSH + getEyeChar(eye) + RESET;

            p.spigot().sendMessage(
                ChatMessageType.ACTION_BAR,
                new TextComponent(hud)
            );
        }
    }

    // ── Eye → Unicode char (matches hud.json font) ────────────────────────────
    private String getEyeChar(EyeType eye) {
        return switch (eye) {
            case VOID     -> "\uE000";
            case PHANTOM  -> "\uE001";
            case STORM    -> "\uE002";
            case FROST    -> "\uE003";
            case FLAME    -> "\uE004";
            case SHADOW   -> "\uE005";
            case TITAN    -> "\uE006";
            case HUNTER   -> "\uE007";
            case GRAVITY  -> "\uE008";
            case WIND     -> "\uE009";
            case POISON   -> "\uE00A";
            case LIGHT    -> "\uE00B";
            case EARTH    -> "\uE00C";
            case CRYSTAL  -> "\uE00D";
            case ECHO     -> "\uE00E";
            case RAGE     -> "\uE00F";
            case SPIRIT   -> "\uE010";
            case TIME     -> "\uE011";
            case WARRIOR  -> "\uE012";
            case METEOR   -> "\uE013";
            case MIRAGE   -> "\uE014";
            case OCEAN    -> "\uE015";
            case ECLIPSE  -> "\uE016";
            case GUARDIAN -> "\uE017";
            default       -> "";
        };
    }
}
