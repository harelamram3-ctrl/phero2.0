package com.harel.superheroes;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

/** Handles the timed Infinity Gauntlet world event and its admin controls. */
public final class GauntletManager implements Listener, CommandExecutor, TabCompleter {
    private final SuperHeroesPlugin plugin;
    private final NamespacedKey gauntletKey;
    private final Random random = new Random();
    private BukkitTask timerTask;
    private BukkitTask effectsTask;
    private Item activeItem;
    private Location activeLocation;
    private boolean autoEnabled;
    private long nextSpawnAt;

    public GauntletManager(SuperHeroesPlugin plugin) {
        this.plugin = plugin;
        this.gauntletKey = new NamespacedKey(plugin, "infinity_gauntlet");
        this.autoEnabled = plugin.getConfig().getBoolean("gauntlet.enabled", true);
        startTimer();
    }

    public void shutdown() {
        if (timerTask != null) timerTask.cancel();
        if (effectsTask != null) effectsTask.cancel();
        removeActiveItem();
    }

    private long intervalTicks() {
        return Math.max(1, plugin.getConfig().getLong("gauntlet.interval-minutes", 50)) * 60L * 20L;
    }

    private void startTimer() {
        if (timerTask != null) timerTask.cancel();
        nextSpawnAt = System.currentTimeMillis() + intervalTicks() * 50L;
        timerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!autoEnabled || activeItem != null) return;
            if (System.currentTimeMillis() >= nextSpawnAt) spawnAtRandomLocation();
        }, 20L, 20L);
    }

    public boolean spawnAtRandomLocation() {
        if (activeItem != null && activeItem.isValid()) return false;
        List<World> worlds = Bukkit.getWorlds().stream()
                .filter(w -> !plugin.getConfig().getStringList("gauntlet.disabled-worlds").contains(w.getName()))
                .toList();
        if (worlds.isEmpty()) {
            Bukkit.broadcastMessage(color("&c[כפפת האינסוף] אין עולם זמין לזימון הכפפה."));
            nextSpawnAt = System.currentTimeMillis() + intervalTicks() * 50L;
            return false;
        }
        World world = worlds.get(random.nextInt(worlds.size()));
        Location spawn = world.getSpawnLocation();
        int radius = Math.max(100, plugin.getConfig().getInt("gauntlet.spawn-radius", 1500));
        Location chosen = null;
        for (int attempt = 0; attempt < 40; attempt++) {
            int x = spawn.getBlockX() + random.nextInt(radius * 2 + 1) - radius;
            int z = spawn.getBlockZ() + random.nextInt(radius * 2 + 1) - radius;
            int y = world.getHighestBlockYAt(x, z) + 1;
            if (y <= world.getMinHeight() || y >= world.getMaxHeight() - 2) continue;
            Block floor = world.getBlockAt(x, y - 1, z);
            Block feet = world.getBlockAt(x, y, z);
            Block head = world.getBlockAt(x, y + 1, z);
            if (floor.getType().isSolid() && feet.isPassable() && head.isPassable()
                    && !floor.isLiquid() && !feet.isLiquid()) {
                chosen = new Location(world, x + .5, y, z + .5);
                break;
            }
        }
        if (chosen == null) chosen = spawn.clone().add(0, 1, 0);
        spawnItem(chosen);
        return true;
    }

    private void spawnItem(Location location) {
        removeActiveItem();
        ItemStack stack = createGauntlet();
        activeItem = location.getWorld().dropItem(location, stack);
        activeItem.setUnlimitedLifetime(true);
        activeItem.setGlowing(true);
        activeItem.setVelocity(new Vector(0, .12, 0));
        activeLocation = location.clone();
        location.getWorld().playSound(location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 3f, .65f);
        location.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, location.clone().add(0, 1, 0), 100, 1.2, 1.5, 1.2, .08);
        String message = plugin.getConfig().getString("gauntlet.messages.spawn", "&6&lכפפת האינסוף הופיעה! &eעיינו במיקום: &f{x}, {y}, {z} &7בעולם {world}");
        Bukkit.broadcastMessage(color(message.replace("{x}", String.valueOf(location.getBlockX()))
                .replace("{y}", String.valueOf(location.getBlockY())).replace("{z}", String.valueOf(location.getBlockZ()))
                .replace("{world}", location.getWorld().getName())));
        if (effectsTask != null) effectsTask.cancel();
        effectsTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (activeItem == null || !activeItem.isValid()) { effectsTask.cancel(); return; }
            Location at = activeItem.getLocation();
            at.getWorld().spawnParticle(Particle.END_ROD, at.clone().add(0, .4, 0), 5, .25, .35, .25, .015);
            at.getWorld().spawnParticle(Particle.PORTAL, at.clone().add(0, .5, 0), 12, .35, .5, .35, .2);
            if (plugin.getConfig().getBoolean("gauntlet.light-beam", true) && at.getWorld().getTime() % 10 == 0) {
                for (int y = 1; y <= 12; y++) at.getWorld().spawnParticle(Particle.END_ROD, at.clone().add(0, y, 0), 1, 0, 0, 0, 0);
            }
        }, 1L, 10L);
        nextSpawnAt = System.currentTimeMillis() + intervalTicks() * 50L;
    }

    public ItemStack createGauntlet() {
        ItemStack item = new ItemStack(Material.NETHERITE_INGOT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color("&6&lכפפת האינסוף"));
        meta.setLore(List.of(color("&7חפץ אגדי בעל כוח בלתי נתפס"), color("&eשש אבני האינסוף מחכות למחזיק."), color("&8SuperHeroes")));
        meta.setCustomModelData(31001);
        meta.getPersistentDataContainer().set(gauntletKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isGauntlet(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(gauntletKey, PersistentDataType.BYTE);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player) || !isGauntlet(event.getItem().getItemStack())) return;
        if (activeItem != null && event.getItem().getUniqueId().equals(activeItem.getUniqueId())) {
            Bukkit.broadcastMessage(color("&6&l" + player.getName() + " השיג/ה את כפפת האינסוף!"));
            player.sendTitle(color("&6&lכפפת האינסוף"), color("&eהכוח בידיים שלך!"), 10, 60, 20);
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 2f, 1f);
            activeItem = null;
            activeLocation = null;
            if (effectsTask != null) effectsTask.cancel();
            nextSpawnAt = System.currentTimeMillis() + intervalTicks() * 50L;
        }
    }

    private void removeActiveItem() {
        if (activeItem != null && activeItem.isValid()) activeItem.remove();
        activeItem = null;
        activeLocation = null;
        if (effectsTask != null) { effectsTask.cancel(); effectsTask = null; }
    }

    private void cancelEvent(CommandSender sender) {
        boolean hadEvent = activeItem != null && activeItem.isValid();
        removeActiveItem();
        nextSpawnAt = System.currentTimeMillis() + intervalTicks() * 50L;
        sender.sendMessage(color(hadEvent ? "&aזימון הכפפה הפעיל בוטל והחפץ הוסר." : "&eאין כרגע זימון פעיל. הטיימר אופס."));
        Bukkit.broadcastMessage(color("&cזימון כפפת האינסוף בוטל על ידי צוות השרת."));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(color("&6&lSuperHeroes - Gauntlet"));
            sender.sendMessage(color("&e/gauntlet status &7- מצב המערכת"));
            sender.sendMessage(color("&e/gauntlet cancel &7- ביטול הזימון הפעיל ואיפוס הטיימר"));
            sender.sendMessage(color("&e/gauntlet disable &7- כיבוי זימונים אוטומטיים"));
            sender.sendMessage(color("&e/gauntlet enable &7- הפעלת זימונים אוטומטיים מחדש"));
            sender.sendMessage(color("&e/gauntlet spawn &7- זימון ידני"));
            sender.sendMessage(color("&e/gauntlet give <player> &7- מתן כפפה לשחקן"));
            return true;
        }
        if (!sender.hasPermission("superheroes.gauntlet.admin")) { sender.sendMessage(color("&cאין לך הרשאה לפקודה הזאת.")); return true; }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "status" -> {
                String state = autoEnabled ? "&aפעילה" : "&cכבויה";
                String event = activeItem != null && activeItem.isValid() ? "&aכפפה נמצאת בעולם" : "&7אין כפפה פעילה";
                sender.sendMessage(color("&6מערכת זימון: " + state + " &8| " + event));
                if (activeLocation != null) sender.sendMessage(color("&7מיקום: &f" + activeLocation.getWorld().getName() + " " + activeLocation.getBlockX() + ", " + activeLocation.getBlockY() + ", " + activeLocation.getBlockZ()));
            }
            case "cancel", "stop" -> cancelEvent(sender);
            case "disable", "off" -> { autoEnabled = false; cancelEvent(sender); sender.sendMessage(color("&cזימונים אוטומטיים כובו. השתמשו /gauntlet enable להפעלה.")); }
            case "enable", "on" -> { autoEnabled = true; startTimer(); sender.sendMessage(color("&aזימונים אוטומטיים הופעלו. הזימון הבא בעוד " + plugin.getConfig().getLong("gauntlet.interval-minutes", 50) + " דקות.")); }
            case "spawn" -> { if (spawnAtRandomLocation()) sender.sendMessage(color("&aהכפפה זומנה בהצלחה.")); else sender.sendMessage(color("&cכבר קיימת כפפה פעילה.")); }
            case "give" -> {
                if (args.length < 2) { sender.sendMessage(color("&cשימוש: /gauntlet give <player>")); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { sender.sendMessage(color("&cהשחקן לא מחובר.")); return true; }
                target.getInventory().addItem(createGauntlet()).values().forEach(drop -> target.getWorld().dropItemNaturally(target.getLocation(), drop));
                sender.sendMessage(color("&aנתת כפפה ל-" + target.getName() + "."));
                target.sendMessage(color("&6קיבלת את כפפת האינסוף!"));
            }
            default -> sender.sendMessage(color("&cפקודה לא מוכרת. /gauntlet help"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return filter(List.of("help", "status", "cancel", "disable", "enable", "spawn", "give"), args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String prefix) {
        return options.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))).toList();
    }
    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s); }
}
