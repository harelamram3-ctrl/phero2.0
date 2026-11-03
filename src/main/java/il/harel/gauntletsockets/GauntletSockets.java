package il.harel.gauntletsockets;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class GauntletSockets extends JavaPlugin implements Listener {
    private NamespacedKey gauntletKey;
    private NamespacedKey stoneKey;
    private final String[] stones = {"space", "mind", "reality", "power", "time", "soul"};
    private final int[] socketSlots = {10, 11, 12, 14, 15, 16};
    private int snapSlot = 13;
    private Plugin infinityPlugin;
    private Method stoneItemMethod;
    private Method useMethod;
    private Method snapMethod;
    private Class<?> stoneEnum;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        gauntletKey = new NamespacedKey("superheroes", "infinity_gauntlet");
        stoneKey = new NamespacedKey("infinitystones", "stone");
        List<Integer> configured = getConfig().getIntegerList("sockets");
        if (configured.size() == 6) {
            for (int i = 0; i < 6; i++) socketSlots[i] = configured.get(i);
        }
        snapSlot = getConfig().getInt("snap-slot", 13);
        hookInfinityStones();
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("Gauntlet sockets enabled. Right-click the Infinity Gauntlet to open its GUI.");
    }

    private void hookInfinityStones() {
        infinityPlugin = Bukkit.getPluginManager().getPlugin("InfinityStones");
        if (infinityPlugin == null || !infinityPlugin.isEnabled()) {
            getLogger().severe("InfinityStones is missing or disabled. Stone abilities will not work.");
            return;
        }
        try {
            ClassLoader loader = infinityPlugin.getClass().getClassLoader();
            stoneEnum = Class.forName("il.harel.infinitystones.Stone", true, loader);
            stoneItemMethod = infinityPlugin.getClass().getMethod("stoneItem", stoneEnum);
            useMethod = infinityPlugin.getClass().getDeclaredMethod("use", Player.class, stoneEnum);
            useMethod.setAccessible(true);
            snapMethod = infinityPlugin.getClass().getDeclaredMethod("snap", Player.class);
            snapMethod.setAccessible(true);
        } catch (ReflectiveOperationException ex) {
            getLogger().severe("Could not connect to InfinityStones API: " + ex.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == null || event.getHand().name().equals("OFF_HAND")) return;
        ItemStack item = event.getItem();
        if (!isGauntlet(item)) return;
        event.setCancelled(true);
        if (!event.getPlayer().hasPermission("gauntletsockets.use")) return;
        openGui(event.getPlayer(), item, false);
    }

    private boolean isGauntlet(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        Byte marker = item.getItemMeta().getPersistentDataContainer().get(gauntletKey, PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }

    private void openGui(Player player, ItemStack gauntlet, boolean offhand) {
        GauntletView holder = new GauntletView(gauntlet.clone(), offhand);
        Inventory inv = Bukkit.createInventory(holder, 27, ChatColor.DARK_PURPLE + "Infinity Gauntlet");
        holder.setInventory(inv);
        render(holder);
        player.openInventory(inv);
        player.sendMessage(color(msg("opened")));
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.5f);
    }

    private void render(GauntletView holder) {
        Inventory inv = holder.getInventory();
        inv.clear();
        ItemStack filler = named(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) inv.setItem(i, filler);
        for (int i = 0; i < stones.length; i++) {
            String stored = readSocket(holder.gauntlet, stones[i]);
            inv.setItem(socketSlots[i], stored == null ? named(Material.GRAY_STAINED_GLASS_PANE,
                    "&7" + pretty(stones[i]) + " Stone &8- &fריק") : createStone(stored));
        }
        boolean complete = true;
        for (String stone : stones) if (readSocket(holder.gauntlet, stone) == null) complete = false;
        inv.setItem(snapSlot, named(complete ? Material.NETHER_STAR : Material.BARRIER,
                complete ? "&6&lINFINITY SNAP" : "&cחסרות אבנים ל-Snap",
                complete ? "&7לחץ להפעלת הכוח" : "&7הכנס את כל שש האבנים"));
        inv.setItem(4, named(Material.GOLD_BLOCK, "&6&lכפפת האינסוף", "&7הכנס אבנים בלחיצה עליהן במלאי שלך", "&7לחץ על אבן בכפפה כדי להפעיל אותה", "&7קליק ימני על אבן כדי להוציא אותה"));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof GauntletView holder)) return;
        event.setCancelled(true);
        int raw = event.getRawSlot();
        if (raw < 0) return;

        if (raw >= event.getView().getTopInventory().getSize()) {
            ItemStack clicked = event.getCurrentItem();
            String stone = getStoneKey(clicked);
            if (stone == null) return;
            int index = indexOf(stone);
            if (index < 0) return;
            if (readSocket(holder.gauntlet, stone) != null) {
                player.sendMessage(color(msg("full")));
                return;
            }
            writeSocket(holder.gauntlet, stone, stone);
            if (clicked.getAmount() <= 1) event.setCurrentItem(null);
            else {
                ItemStack reduced = clicked.clone();
                reduced.setAmount(reduced.getAmount() - 1);
                event.setCurrentItem(reduced);
            }
            render(holder);
            player.sendMessage(color(msg("inserted").replace("{stone}", pretty(stone))));
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.4f);
            return;
        }

        int socketIndex = indexOfSlot(raw);
        if (socketIndex >= 0) {
            String stone = stones[socketIndex];
            if (readSocket(holder.gauntlet, stone) == null) return;
            if (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT) {
                ItemStack returned = createStone(stone);
                if (returned != null) {
                    var leftovers = player.getInventory().addItem(returned);
                    leftovers.values().forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
                    writeSocket(holder.gauntlet, stone, null);
                    render(holder);
                    player.sendMessage(color(msg("removed").replace("{stone}", pretty(stone))));
                }
                return;
            }
            player.closeInventory();
            activateStone(player, stone);
            return;
        }

        if (raw == snapSlot) {
            if (!hasAll(holder.gauntlet)) {
                player.sendMessage(color(msg("missing")));
                return;
            }
            player.closeInventory();
            activateSnap(player);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof GauntletView holder)) return;
        if (holder.offhand) player.getInventory().setItemInOffHand(holder.gauntlet);
        else player.getInventory().setItemInMainHand(holder.gauntlet);
    }

    private void activateStone(Player player, String key) {
        if (infinityPlugin == null || useMethod == null || stoneEnum == null) {
            player.sendMessage(color(msg("no-plugin")));
            return;
        }
        try {
            Object stone = Enum.valueOf((Class) stoneEnum, key.toUpperCase(Locale.ROOT));
            useMethod.invoke(infinityPlugin, player, stone);
        } catch (ReflectiveOperationException ex) {
            getLogger().warning("Could not activate " + key + ": " + ex.getMessage());
            player.sendMessage(ChatColor.RED + "לא ניתן להפעיל את האבן כרגע.");
        }
    }

    private void activateSnap(Player player) {
        if (infinityPlugin == null || snapMethod == null || stoneItemMethod == null || stoneEnum == null) {
            player.sendMessage(color(msg("no-plugin")));
            return;
        }
        PlayerInventory inventory = player.getInventory();
        ItemStack[] old = cloneItems(inventory.getStorageContents());
        try {
            ItemStack[] temporary = new ItemStack[old.length];
            for (int i = 0; i < stones.length; i++) {
                Object stone = Enum.valueOf((Class) stoneEnum, stones[i].toUpperCase(Locale.ROOT));
                temporary[i] = (ItemStack) stoneItemMethod.invoke(infinityPlugin, stone);
            }
            inventory.setStorageContents(temporary);
            snapMethod.invoke(infinityPlugin, player);
        } catch (ReflectiveOperationException ex) {
            getLogger().warning("Could not activate Snap: " + ex.getMessage());
            player.sendMessage(ChatColor.RED + "לא ניתן להפעיל Snap כרגע.");
        } finally {
            inventory.setStorageContents(old);
        }
        player.sendTitle(color(msg("snap")), ChatColor.LIGHT_PURPLE + "The power of all six stones", 10, 50, 20);
    }

    private ItemStack[] cloneItems(ItemStack[] source) {
        ItemStack[] copy = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) copy[i] = source[i] == null ? null : source[i].clone();
        return copy;
    }

    private String getStoneKey(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(stoneKey, PersistentDataType.STRING);
    }

    private ItemStack createStone(String key) {
        if (infinityPlugin != null && stoneItemMethod != null && stoneEnum != null) {
            try {
                Object stone = Enum.valueOf((Class) stoneEnum, key.toUpperCase(Locale.ROOT));
                return (ItemStack) stoneItemMethod.invoke(infinityPlugin, stone);
            } catch (ReflectiveOperationException ignored) { }
        }
        return named(Material.AMETHYST_SHARD, "&d" + pretty(key) + " Stone");
    }

    private boolean hasAll(ItemStack gauntlet) {
        for (String stone : stones) if (readSocket(gauntlet, stone) == null) return false;
        return true;
    }

    private String readSocket(ItemStack item, String stone) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(socketKey(stone), PersistentDataType.STRING);
    }

    private void writeSocket(ItemStack item, String stone, String value) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (value == null) pdc.remove(socketKey(stone));
        else pdc.set(socketKey(stone), PersistentDataType.STRING, value);
        List<String> lore = new ArrayList<>();
        lore.add(color("&7אבני אינסוף בכפפה:"));
        for (String s : stones) lore.add(color((readSocketFrom(pdc, s) == null ? "&8[ ] " : "&a[✓] ") + pretty(s)));
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    private String readSocketFrom(PersistentDataContainer pdc, String stone) {
        return pdc.get(socketKey(stone), PersistentDataType.STRING);
    }

    private NamespacedKey socketKey(String stone) { return new NamespacedKey(this, "socket_" + stone); }
    private int indexOf(String stone) { for (int i = 0; i < stones.length; i++) if (stones[i].equalsIgnoreCase(stone)) return i; return -1; }
    private int indexOfSlot(int slot) { for (int i = 0; i < socketSlots.length; i++) if (socketSlots[i] == slot) return i; return -1; }
    private String pretty(String key) { return key.substring(0, 1).toUpperCase(Locale.ROOT) + key.substring(1).toLowerCase(Locale.ROOT); }
    private String msg(String key) { return getConfig().getString("messages." + key, ""); }
    private String color(String text) { return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text); }

    private ItemStack named(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(name));
        if (lore.length > 0) {
            List<String> lines = new ArrayList<>();
            for (String line : lore) lines.add(color(line));
            meta.setLore(lines);
        }
        item.setItemMeta(meta);
        return item;
    }

    private final class GauntletView implements InventoryHolder {
        private final ItemStack gauntlet;
        private final boolean offhand;
        private Inventory inventory;
        private GauntletView(ItemStack gauntlet, boolean offhand) { this.gauntlet = gauntlet; this.offhand = offhand; }
        private void setInventory(Inventory inventory) { this.inventory = inventory; }
        @Override public Inventory getInventory() { return inventory; }
    }
}
