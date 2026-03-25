package com.ancienteye;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.GameMode;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.bukkit.block.BlockState;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.*;

public class AbilityLogic implements Listener {

    private final AncientEyePlugin plugin;
    private static final int INF = -1;

    private final AbilityPrimary primary;
    private final AbilitySecondary secondary;

    public AbilityLogic(AncientEyePlugin plugin) {
        this.plugin    = plugin;
        this.primary   = new AbilityPrimary(plugin, this);
        this.secondary = new AbilitySecondary(plugin, this);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GUI LOCK
    // ══════════════════════════════════════════════════════════════════════
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals("§8Your Ancient Eye Status")) e.setCancelled(true);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PASSIVE COMBAT
    // ══════════════════════════════════════════════════════════════════════
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player victim) {
            EyeType ve = plugin.getPlayerData().getEye(victim);
            if (ve == EyeType.VOID && Math.random() < 0.15) {
                e.setCancelled(true);
                victim.sendMessage("§5§l✦ Void Dodge!");
                victim.getWorld().spawnParticle(Particle.PORTAL, victim.getLocation(), 30, 0.5, 1, 0.5, 0.1);
                return;
            }
            if (ve == EyeType.MIRAGE && Math.random() < 0.15) {
                e.setCancelled(true);
                victim.sendMessage("§e§l✦ Mirage! Attack missed!");
                victim.getWorld().spawnParticle(Particle.CLOUD, victim.getLocation(), 20, 0.4, 0.8, 0.4, 0.04);
                return;
            }
            if (ve == EyeType.GUARDIAN) e.setDamage(e.getDamage() * 0.65);
        }
        if (e.getDamager() instanceof Player atk) {
            EyeType ae = plugin.getPlayerData().getEye(atk);
            if (ae == EyeType.RAGE) {
                double hp  = atk.getHealth();
                double max = atk.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
                if (hp / max < 0.35) {
                    e.setDamage(e.getDamage() * 1.75);
                    atk.getWorld().spawnParticle(Particle.DUST, atk.getLocation(), 10, 0.3, 0.6, 0.3, 0,
                        new Particle.DustOptions(Color.RED, 1.5f));
                }
            }
            if (ae == EyeType.ECLIPSE) {
                long t = atk.getWorld().getTime();
                if (t > 13000 && t < 23000) e.setDamage(e.getDamage() * 1.30);
            }
        }
        // Hunter mark — extra damage
        if (e.getEntity() instanceof LivingEntity victim2) {
            if (victim2.hasMetadata("hunterMarked")) {
                e.setDamage(e.getDamage() * 1.5);
            }
        }
    }

    @EventHandler
    public void onTitanDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player p) {
            if (p.hasMetadata("TitanMode")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
public void onMinionDamage(EntityDamageByEntityEvent event) {

    if (event.getDamager() instanceof Warden warden &&
        event.getEntity() instanceof Player p) {

        if (warden.hasMetadata("MinionOf")) {

            String ownerUUID = warden.getMetadata("MinionOf").get(0).asString();

            if (p.getUniqueId().toString().equals(ownerUUID)) {
                event.setCancelled(true);
            }
        }
    }
}
    @EventHandler
    public void onPlayerKillXP(org.bukkit.event.entity.PlayerDeathEvent e) {
        if (e.getEntity().getKiller() == null) return;
        Player killer = e.getEntity().getKiller();
        if (plugin.getPlayerData().getEye(killer) == EyeType.NONE) return;
        plugin.getPlayerData().addXp(killer, plugin.getConfig().getInt("settings.xp-per-kill", 1));
    }

    @EventHandler
    public void onFootstep(org.bukkit.event.player.PlayerMoveEvent e) {
        Player mover = e.getPlayer();
        if (!mover.isOnGround()) return;
        if (mover.getTicksLived() % 10 != 0) return;
        for (Player owner : Bukkit.getOnlinePlayers()) {
            if (owner.getUniqueId().equals(mover.getUniqueId())) continue;
            EyeType ownerEye = plugin.getPlayerData().getEye(owner);
            if (ownerEye != EyeType.ECHO) continue;
            double distSq = owner.getLocation().distanceSquared(mover.getLocation());
            if (distSq > 20 * 20) continue;
            double dist = Math.sqrt(distSq);
            String dir  = getDirectionLabel(owner, mover);
            owner.sendActionBar("§3👣 §f" + mover.getName() + " §7footstep §b" + (int)dist + "§7 blocks §3" + dir);
            owner.spawnParticle(Particle.SONIC_BOOM, mover.getLocation().add(0, 0.1, 0), 1, 0, 0, 0, 0);
            if (mover instanceof LivingEntity le) {
                EyeType moverEye = plugin.getPlayerData().getEye(mover);
                if (moverEye != EyeType.ECHO) le.damage(0.5, owner);
            }
        }
    }

    private String getDirectionLabel(Player from, Player to) {
        double dx    = to.getLocation().getX() - from.getLocation().getX();
        double dz    = to.getLocation().getZ() - from.getLocation().getZ();
        double angle = Math.toDegrees(Math.atan2(dz, dx));
        double yaw   = (from.getLocation().getYaw() + 90) % 360;
        double rel   = ((angle - yaw) % 360 + 360) % 360;
        if (rel < 45 || rel >= 315) return "▲ Front";
        if (rel < 135)              return "▶ Right";
        if (rel < 225)              return "▼ Behind";
        return                             "◀ Left";
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DELEGATE TO PRIMARY / SECONDARY
    // ══════════════════════════════════════════════════════════════════════
    public void activatePrimary(Player p, EyeType eye) {
        primary.activate(p, eye);
    }

    public void activateSecondary(Player p, EyeType eye) {
        secondary.activate(p, eye);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PASSIVE EFFECTS
    // ══════════════════════════════════════════════════════════════════════
    public void applyPassiveEffects(Player p, EyeType eye) {
        for (PotionEffect e : p.getActivePotionEffects()) p.removePotionEffect(e.getType());
        if (eye == null || eye == EyeType.NONE) return;
        switch (eye) {
            case VOID    -> p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION,    INF, 0, false, false));
            case PHANTOM -> p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION,    INF, 0, false, false));
            case STORM   -> p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,           INF, 1, false, false));
            case FROST   -> p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, INF, 0, false, false));
            case FLAME   -> p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, INF, 0, false, false));
            case SHADOW  -> { p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,         INF,0,false,false)); p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION,INF,0,false,false)); }
            case TITAN   -> p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,      INF, 1, false, false));
            case HUNTER  -> p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,           INF, 1, false, false));
            case GRAVITY -> p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST,      INF, 2, false, false));
            case WIND    -> p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,           INF, 1, false, false));
            case POISON  -> p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,    INF, 0, false, false));
            case LIGHT   -> p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION,    INF, 0, false, false));
            case EARTH   -> p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,      INF, 0, false, false));
            case CRYSTAL -> p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,      INF, 0, false, false));
            case ECHO    -> { p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION,  INF,0,false,false)); p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,INF,0,false,false)); }
            case RAGE    -> p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH,        INF, 0, false, false));
            case SPIRIT  -> p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,    INF, 1, false, false));
            case TIME    -> p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE,           INF, 1, false, false));
            case WARRIOR -> p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH,        INF, 1, false, false));
            case METEOR  -> { p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE,INF,0,false,false)); p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,INF,0,false,false)); }
            case MIRAGE  -> p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,           INF, 2, false, false));
            case OCEAN   -> { p.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING,INF,0,false,false)); p.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER,INF,0,false,false)); }
            case ECLIPSE -> { p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION,  INF,0,false,false)); p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE,INF,0,false,false)); }
            case GUARDIAN-> { p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,    INF,2,false,false)); p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION,INF,1,false,false)); }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GUI
    // ══════════════════════════════════════════════════════════════════════
    public void openEyeGUI(Player p) {
        Inventory gui = Bukkit.createInventory(null, 54, "§8Your Ancient Eye Status");
        ItemStack border = pane(Material.PURPLE_STAINED_GLASS_PANE);
        ItemStack fill   = pane(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < 54; i++)
            gui.setItem(i, (i<9||i>=45||i%9==0||i%9==8) ? border : fill);
        gui.setItem(22, buildEyeItem(p));
        p.openInventory(gui);
        p.playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1.2f);

        new BukkitRunnable() {
            public void run() {
                if (!p.isOnline()) { cancel(); return; }
                Inventory top = p.getOpenInventory().getTopInventory();
                if (top == null || !p.getOpenInventory().getTitle().equals("§8Your Ancient Eye Status")) {
                    cancel(); return;
                }
                top.setItem(22, buildEyeItem(p));
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private ItemStack buildEyeItem(Player p) {
        EyeType type     = plugin.getPlayerData().getEye(p);
        int     level    = plugin.getPlayerData().getLevel(p);
        int     kills    = plugin.getPlayerData().getXP(p);
        int     maxKills = plugin.getPlayerData().getMaxXPForLevel(level);
        String  bar      = progressBar(kills, maxKills);
        String  col      = level==3?"§e§l":level==2?"§b§l":"§7§l";

        ItemStack eye = new ItemStack(Material.ENDER_EYE);
        ItemMeta  m   = eye.getItemMeta();
        m.setDisplayName(col + type.name().replace("_"," ") + " EYE");
        List<String> lore = new ArrayList<>();
        lore.add("§8━━━━━━━━━━━━━━━━━━");
        lore.add("§7Level    §f"+level+" §8/ §f3");
        lore.add("§7Kills: "+bar+" §8(§c"+kills+"§8/§f"+maxKills+"§8)");
        lore.add("§8━━━━━━━━━━━━━━━━━━");
        lore.add("§6§lStats:");
        lore.add("§e- §7Damage Bonus:  §c+"+(int)((getDmg(p)-1)*100)+"%");
        lore.add("§e- §7Duration Mul:  §b"+(int)(getDur(p)*100)+"% of base");
        lore.add("§e- §7Cooldown Red:  §a-"+((level-1)*2)+"s per ability");
        lore.add("§8━━━━━━━━━━━━━━━━━━");
        lore.add("§b§lAbilities:");
        lore.add("§f  SHIFT+F  §7->  §bPrimary");
        lore.add("§f  SHIFT+Q  §7->  §bSecondary");
        lore.add("§8━━━━━━━━━━━━━━━━━━");
        lore.add("§5§oThe power is bound to your soul.");
        m.setLore(lore);
        eye.setItemMeta(m);
        return eye;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  HELPERS (package-private so Primary/Secondary can use them)
    // ══════════════════════════════════════════════════════════════════════
    double getDmg(Player p) {
        int l = plugin.getPlayerData().getLevel(p);
        return l == 3 ? 1.5 : l == 2 ? 1.2 : 1.0;
    }

    double getDur(Player p) {
        int l = plugin.getPlayerData().getLevel(p);
        return l == 3 ? 2.0 : l == 2 ? 1.5 : 1.0;
    }

    int ticks(int base, double durMul) {
        return (int)(base * durMul);
    }

    int getCd(Player p, EyeType eye, String type) {
        String key = "cooldowns." + eye.name().toLowerCase() + "." + type;
        int base = plugin.getConfig().getInt(key, 12);
        int lvl  = plugin.getPlayerData().getLevel(p);
        return Math.max(2, base - (lvl - 1) * 2);
    }

    Location safe(Location l) {
        if (l.getBlock().getType().isSolid()) l.add(0,1,0);
        if (l.getBlock().getType().isSolid()) l.add(0,1,0);
        return l;
    }

    LivingEntity aim(Player p, double range) {
        RayTraceResult r = p.getWorld().rayTraceEntities(p.getEyeLocation(),
            p.getEyeLocation().getDirection(), range, 1.5, e -> e != p);
        return (r != null && r.getHitEntity() instanceof LivingEntity le) ? le : null;
    }

    void crystalAura(Player p) {
        World w = p.getWorld(); Location l = p.getLocation();
        Particle.DustOptions d = new Particle.DustOptions(Color.fromRGB(100,220,255), 2f);
        for (double a = 0; a < Math.PI*2; a += Math.PI/10) {
            double rx = Math.cos(a)*2, rz = Math.sin(a)*2;
            w.spawnParticle(Particle.DUST, l.clone().add(rx,1,rz), 3, 0, 0.4, 0, 0, d);
            w.spawnParticle(Particle.END_ROD, l.clone().add(rx,1,rz), 2, 0, 0.3, 0, 0.01);
        }
        w.spawnParticle(Particle.END_ROD, l, 40, 0.5, 1.2, 0.5, 0.04);
    }

    void voidRift(World w, Location l) {
        w.spawnParticle(Particle.PORTAL,         l, 70, 0.6, 0.9, 0.6, 0.28);
        w.spawnParticle(Particle.REVERSE_PORTAL, l, 45, 0.4, 0.7, 0.4, 0.16);
        w.spawnParticle(Particle.DRAGON_BREATH,  l, 30, 0.3, 0.5, 0.3, 0.06);
        for (int i = 0; i < 14; i++) { double a = Math.toRadians(i*(360.0/14));
            w.spawnParticle(Particle.PORTAL, l.clone().add(Math.cos(a)*1.4,0.5,Math.sin(a)*1.4), 3, 0, 0, 0, 0.09);
        }
    }

    double ecfg(String eye, String key, double def) {
        return plugin.getConfig().getDouble("event-eyes."+eye+"."+key, def);
    }

    ItemStack pane(Material mat) {
        ItemStack i = new ItemStack(mat);
        ItemMeta  m = i.getItemMeta();
        m.setDisplayName("§r");
        i.setItemMeta(m);
        return i;
    }

    void whiteLightScreen(Player ep, int durationTicks, AncientEyePlugin plug) {
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (!ep.isOnline() || t++ >= durationTicks) { cancel(); return; }
                Location eyeFront = ep.getEyeLocation().clone()
                    .add(ep.getEyeLocation().getDirection().multiply(0.1));
                ep.spawnParticle(Particle.FLASH, eyeFront, 1, 0, 0, 0, 0);
                for (int i = 0; i < 8; i++) {
                    double a = Math.toRadians(i * 45);
                    ep.spawnParticle(Particle.FLASH,
                        eyeFront.clone().add(Math.cos(a)*0.12, Math.sin(a)*0.12, 0),
                        1, 0, 0, 0, 0);
                }
            }
        }.runTaskTimer(plug, 0, 1);
    }

    String progressBar(int cur, int max) {
        int bars = 12;
        int done = (int)((double)Math.min(cur, max) / max * bars);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bars; i++) sb.append(i < done ? "§a|" : "§7|");
        return sb.toString();
    }

    void restoreOrDrop(Player p, ItemStack stored, Runnable setter, ItemStack current) {
        if (stored == null) return;
        boolean slotEmpty = (current == null || current.getType() == Material.AIR);
        if (slotEmpty) {
            setter.run();
        } else {
            p.getWorld().dropItemNaturally(p.getLocation(), stored);
            p.sendMessage("\u00a77Your hidden item was dropped: \u00a7f"
                + stored.getType().name().toLowerCase().replace("_", " "));
        }
    }

    // Eclipse blast helper
    void doEclipseBlast(Player p, World w, Location blastLoc, double dmg, AncientEyePlugin plug) {
        double localDm = getDmg(p);
        w.spawnParticle(Particle.SQUID_INK,         blastLoc, 60, 0.5, 0.5, 0.5, 0.15);
        w.spawnParticle(Particle.DRAGON_BREATH,     blastLoc, 80, 0.8, 0.8, 0.8, 0.08);
        w.spawnParticle(Particle.EXPLOSION_EMITTER, blastLoc,  2, 0, 0, 0, 0);
        w.spawnParticle(Particle.EXPLOSION,         blastLoc, 20, 1.0, 0.5, 1.0, 0.1);
        w.createExplosion(blastLoc, 17.0f, false, false);
        Bukkit.getScheduler().runTaskLater(plug, () -> {
            w.spawnParticle(Particle.EXPLOSION_EMITTER, blastLoc, 3, 0.3, 0, 0.3, 0);
            w.spawnParticle(Particle.SQUID_INK,         blastLoc, 40, 0.6, 0.6, 0.6, 0.12);
            w.spawnParticle(Particle.DRAGON_BREATH,     blastLoc, 60, 0.7, 0.7, 0.7, 0.07);
            w.playSound(blastLoc, Sound.ENTITY_GENERIC_EXPLODE,    2f, 0.4f);
            w.playSound(blastLoc, Sound.ENTITY_WITHER_BREAK_BLOCK, 1f, 0.5f);
        }, 5L);
        w.playSound(blastLoc, Sound.ENTITY_GENERIC_EXPLODE,         2f, 0.3f);
        w.playSound(blastLoc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1f, 0.3f);
        w.playSound(blastLoc, Sound.ENTITY_WITHER_BREAK_BLOCK,      1f, 0.5f);
        for (int ring = 1; ring <= 5; ring++) {
            final int fr = ring;
            Bukkit.getScheduler().runTaskLater(plug, () -> {
                double rad = fr * 2.0; int pts = 36;
                for (int i = 0; i < pts; i++) {
                    double a = Math.toRadians(i * (360.0 / pts));
                    w.spawnParticle(Particle.SQUID_INK,    blastLoc.clone().add(Math.cos(a)*rad, 0.1, Math.sin(a)*rad), 1, 0,0,0,0);
                    w.spawnParticle(Particle.DRAGON_BREATH,blastLoc.clone().add(Math.cos(a)*rad, 1.0, Math.sin(a)*rad), 1, 0,0,0,0);
                    w.spawnParticle(Particle.SQUID_INK,    blastLoc.clone().add(Math.cos(a)*rad, 2.0, Math.sin(a)*rad), 1, 0,0,0,0);
                }
                w.playSound(blastLoc, Sound.ENTITY_WITHER_AMBIENT, 0.5f, 0.3f + fr * 0.1f);
            }, ring * 2L);
        }
        applySafeDamage(p, blastLoc, 10.0, dmg);
        blastLoc.getWorld().getNearbyEntities(blastLoc, 10, 10, 10).forEach(e -> {
            if (e instanceof LivingEntity le && e != p) {
                Vector vel = e.getLocation().toVector().subtract(blastLoc.toVector())
                    .normalize().multiply(3.5).setY(1.2);
                Bukkit.getScheduler().runTaskLater(plug, () -> { if (e.isValid()) e.setVelocity(vel); }, 1L);
            }
        });
    }

    private void removeBoxGradually(Location center, int size, Map<Location, org.bukkit.block.BlockState> oldBlocks) {
    new BukkitRunnable() {
        final List<Location> locs = new ArrayList<>(oldBlocks.keySet());
        int index = 0;
        int speed = 15; // Ek saath kitne blocks tootenge

        @Override
        public void run() {
            for (int i = 0; i < speed; i++) {
                if (index >= locs.size()) {
                    this.cancel();
                    oldBlocks.clear();
                    return;
                }
                Location l = locs.get(index);
                // Wapas purana block lagao (Air ya jo bhi tha)
                l.getBlock().setType(oldBlocks.get(l).getType());
                l.getWorld().playSound(l, Sound.BLOCK_STONE_BREAK, 0.4f, 0.8f);
                l.getWorld().spawnParticle(Particle.SMOKE_NORMAL, l, 1, 0.05, 0.05, 0.05, 0.02);
                index++;
            }
        }
    }.runTaskTimer(plugin, 0L, 2L);
    }
    

    // ✅ FIX: owner is always excluded
    void applySafeDamage(Player owner, Location loc, double radius, double damage) {
        loc.getWorld().getNearbyEntities(loc, radius, radius, radius).forEach(entity -> {
            if (!(entity instanceof LivingEntity victim)) return;
            if (entity.equals(owner)) return; // Owner ko kuch nahi
            if (entity instanceof Player ep) {
                if (ep.getGameMode() == GameMode.CREATIVE ||
                    ep.getGameMode() == GameMode.SPECTATOR) return;
            }
            victim.damage(damage * getDmg(owner), owner);
            victim.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, victim.getLocation().add(0, 1, 0), 5);
        });
    }
}
