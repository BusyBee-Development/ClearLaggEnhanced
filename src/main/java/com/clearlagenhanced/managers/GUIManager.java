package com.clearlagenhanced.managers;

import com.clearlagenhanced.ClearLaggEnhanced;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class GUIManager implements Listener {

    private final ClearLaggEnhanced plugin;
    private final ConfigManager configManager;
    private final PerformanceManager performanceManager;
    private final Map<UUID, String> openGUIs;
    private final Map<UUID, String> awaitingInput;
    private final Map<UUID, String> inputPaths;
    private BukkitTask performanceUpdateTask;

    public GUIManager(ClearLaggEnhanced plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.performanceManager = plugin.getPerformanceManager();
        this.openGUIs = new HashMap<>();
        this.awaitingInput = new HashMap<>();
        this.inputPaths = new HashMap<>();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        startPerformanceUpdateTask();
    }

    private void startPerformanceUpdateTask() {
        performanceUpdateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updatePerformanceDisplays();
            }
        }.runTaskTimer(plugin, 0L, 40L);
    }

    private void updatePerformanceDisplays() {
        for (Map.Entry<UUID, String> entry : new HashMap<>(openGUIs).entrySet()) {
            if ("main".equals(entry.getValue())) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null && player.getOpenInventory().getTopInventory() != null) {
                    updatePerformanceItem(player.getOpenInventory().getTopInventory());
                }
            }
        }
    }

    public void openMainGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, Component.text("ClearLaggEnhanced Admin Panel").color(NamedTextColor.DARK_GREEN));

        gui.setItem(10, createPerformanceItem());

        ItemStack entityItem = new ItemStack(Material.IRON_SWORD);
        ItemMeta entityMeta = entityItem.getItemMeta();
        entityMeta.displayName(Component.text("Entity Clearing").color(NamedTextColor.RED));
        entityMeta.lore(Arrays.asList(
                Component.text("Configure entity clearing settings").color(NamedTextColor.GRAY),
                Component.text("• Intervals and timings").color(NamedTextColor.YELLOW),
                Component.text("• Entity whitelist/blacklist").color(NamedTextColor.YELLOW),
                Component.text("• World configurations").color(NamedTextColor.YELLOW),
                Component.empty(),
                Component.text("Click to open settings").color(NamedTextColor.GREEN)
        ));
        entityItem.setItemMeta(entityMeta);
        gui.setItem(12, entityItem);

        ItemStack lagItem = new ItemStack(Material.REDSTONE);
        ItemMeta lagMeta = lagItem.getItemMeta();
        lagMeta.displayName(Component.text("Lag Prevention").color(NamedTextColor.GOLD));
        lagMeta.lore(Arrays.asList(
                Component.text("Configure lag prevention modules").color(NamedTextColor.GRAY),
                Component.text("• Mob Limiter").color(NamedTextColor.YELLOW),
                Component.text("• Redstone Limiter").color(NamedTextColor.YELLOW),
                Component.text("• Hopper Limiter").color(NamedTextColor.YELLOW),
                Component.text("• Spawner Limiter").color(NamedTextColor.YELLOW),
                Component.empty(),
                Component.text("Click to open settings").color(NamedTextColor.GREEN)
        ));
        lagItem.setItemMeta(lagMeta);
        gui.setItem(14, lagItem);

        ItemStack reloadItem = new ItemStack(Material.COMMAND_BLOCK);
        ItemMeta reloadMeta = reloadItem.getItemMeta();
        reloadMeta.displayName(Component.text("Reload Config").color(NamedTextColor.AQUA));
        reloadMeta.lore(Arrays.asList(
                Component.text("Reload configuration from file").color(NamedTextColor.GRAY),
                Component.empty(),
                Component.text("Click to reload").color(NamedTextColor.GREEN)
        ));
        reloadItem.setItemMeta(reloadMeta);
        gui.setItem(16, reloadItem);

        openGUIs.put(player.getUniqueId(), "main");
        player.openInventory(gui);
    }

    private ItemStack createPerformanceItem() {
        ItemStack performanceItem = new ItemStack(Material.CLOCK);
        updatePerformanceItemMeta(performanceItem);
        return performanceItem;
    }

    private void updatePerformanceItemMeta(ItemStack performanceItem) {
        ItemMeta performanceMeta = performanceItem.getItemMeta();
        double tps = performanceManager.getTPS();
        String memoryUsage = performanceManager.getFormattedMemoryUsage();
        double memoryPercent = performanceManager.getMemoryUsagePercentage();
        int totalEntities = performanceManager.getTotalEntities();
        NamedTextColor tpsColor = tps >= 18.0 ? NamedTextColor.GREEN : tps >= 15.0 ? NamedTextColor.YELLOW : NamedTextColor.RED;
        NamedTextColor memoryColor = memoryPercent < 70.0 ? NamedTextColor.GREEN : memoryPercent < 85.0 ? NamedTextColor.YELLOW : NamedTextColor.RED;

        performanceMeta.displayName(Component.text("Performance Monitoring").color(NamedTextColor.BLUE));
        performanceMeta.lore(Arrays.asList(
                Component.text("Real-time server statistics").color(NamedTextColor.GRAY),
                Component.empty(),
                Component.text("TPS: " + String.format("%.2f", tps)).color(tpsColor),
                Component.text("Memory: " + memoryUsage + " (" + String.format("%.1f", memoryPercent) + "%)").color(memoryColor),
                Component.text("Entities: " + totalEntities).color(NamedTextColor.YELLOW),
                Component.empty(),
                Component.text("Updates every 2 seconds").color(NamedTextColor.DARK_GRAY)
        ));
        performanceItem.setItemMeta(performanceMeta);
    }

    private void updatePerformanceItem(Inventory inventory) {
        ItemStack performanceItem = inventory.getItem(10);
        if (performanceItem != null && performanceItem.getType() == Material.CLOCK) {
            updatePerformanceItemMeta(performanceItem);
        }
    }

    public void openEntityClearingGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 36, Component.text("Entity Clearing Settings").color(NamedTextColor.RED));
        boolean enabled = configManager.getBoolean("entity-clearing.enabled", true);
        int interval = configManager.getInt("entity-clearing.interval", 300);
        boolean protectNamed = configManager.getBoolean("entity-clearing.protect-named-entities", true);
        boolean protectTamed = configManager.getBoolean("entity-clearing.protect-tamed-entities", true);

        ItemStack toggleItem = new ItemStack(enabled ? Material.GREEN_WOOL : Material.RED_WOOL);
        ItemMeta toggleMeta = toggleItem.getItemMeta();
        toggleMeta.displayName(Component.text("Entity Clearing: " + (enabled ? "Enabled" : "Disabled")).color(enabled ? NamedTextColor.GREEN : NamedTextColor.RED));
        toggleMeta.lore(Collections.singletonList(Component.text("Click to " + (enabled ? "disable" : "enable")).color(NamedTextColor.GRAY)));
        toggleItem.setItemMeta(toggleMeta);
        gui.setItem(10, toggleItem);

        ItemStack intervalItem = new ItemStack(Material.CLOCK);
        ItemMeta intervalMeta = intervalItem.getItemMeta();
        intervalMeta.displayName(Component.text("Clearing Interval: " + interval + "s").color(NamedTextColor.YELLOW));
        intervalMeta.lore(Arrays.asList(Component.text("Current interval: " + interval + " seconds").color(NamedTextColor.GRAY), Component.empty(), Component.text("Click to change").color(NamedTextColor.GREEN)));
        intervalItem.setItemMeta(intervalMeta);
        gui.setItem(12, intervalItem);

        ItemStack namedItem = new ItemStack(protectNamed ? Material.NAME_TAG : Material.PAPER);
        ItemMeta namedMeta = namedItem.getItemMeta();
        namedMeta.displayName(Component.text("Protect Named: " + (protectNamed ? "Yes" : "No")).color(protectNamed ? NamedTextColor.GREEN : NamedTextColor.RED));
        namedMeta.lore(Collections.singletonList(Component.text("Click to " + (protectNamed ? "disable" : "enable")).color(NamedTextColor.GRAY)));
        namedItem.setItemMeta(namedMeta);
        gui.setItem(14, namedItem);

        ItemStack tamedItem = new ItemStack(protectTamed ? Material.BONE : Material.STICK);
        ItemMeta tamedMeta = tamedItem.getItemMeta();
        tamedMeta.displayName(Component.text("Protect Tamed: " + (protectTamed ? "Yes" : "No")).color(protectTamed ? NamedTextColor.GREEN : NamedTextColor.RED));
        tamedMeta.lore(Collections.singletonList(Component.text("Click to " + (protectTamed ? "disable" : "enable")).color(NamedTextColor.GRAY)));
        tamedItem.setItemMeta(tamedMeta);
        gui.setItem(16, tamedItem);

        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.displayName(Component.text("Back to Main Menu").color(NamedTextColor.WHITE));
        backItem.setItemMeta(backMeta);
        gui.setItem(31, backItem);

        openGUIs.put(player.getUniqueId(), "entity-clearing");
        player.openInventory(gui);
    }

    public void openLagPreventionGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 36, Component.text("Lag Prevention Modules").color(NamedTextColor.GOLD));
        boolean mobLimiter = configManager.getBoolean("lag-prevention.mob-limiter.enabled", true);
        boolean redstoneLimiter = configManager.getBoolean("lag-prevention.redstone-limiter.enabled", true);
        boolean hopperLimiter = configManager.getBoolean("lag-prevention.hopper-limiter.enabled", true);
        boolean spawnerLimiter = configManager.getBoolean("lag-prevention.spawner-limiter.enabled", true);
        int maxMobs = configManager.getInt("lag-prevention.mob-limiter.max-mobs-per-chunk", 50);

        ItemStack mobItem = new ItemStack(mobLimiter ? Material.GREEN_WOOL : Material.RED_WOOL);
        ItemMeta mobMeta = mobItem.getItemMeta();
        mobMeta.displayName(Component.text("Mob Limiter: " + (mobLimiter ? "Enabled" : "Disabled")).color(mobLimiter ? NamedTextColor.GREEN : NamedTextColor.RED));
        mobMeta.lore(Arrays.asList(Component.text("Max mobs per chunk: " + maxMobs).color(NamedTextColor.GRAY), Component.empty(), Component.text("Click to " + (mobLimiter ? "disable" : "enable")).color(NamedTextColor.YELLOW)));
        mobItem.setItemMeta(mobMeta);
        gui.setItem(10, mobItem);

        ItemStack redstoneItem = new ItemStack(redstoneLimiter ? Material.GREEN_WOOL : Material.RED_WOOL);
        ItemMeta redstoneMeta = redstoneItem.getItemMeta();
        redstoneMeta.displayName(Component.text("Redstone Limiter: " + (redstoneLimiter ? "Enabled" : "Disabled")).color(redstoneLimiter ? NamedTextColor.GREEN : NamedTextColor.RED));
        redstoneMeta.lore(Collections.singletonList(Component.text("Click to " + (redstoneLimiter ? "disable" : "enable")).color(NamedTextColor.YELLOW)));
        redstoneItem.setItemMeta(redstoneMeta);
        gui.setItem(12, redstoneItem);

        ItemStack hopperItem = new ItemStack(hopperLimiter ? Material.GREEN_WOOL : Material.RED_WOOL);
        ItemMeta hopperMeta = hopperItem.getItemMeta();
        hopperMeta.displayName(Component.text("Hopper Limiter: " + (hopperLimiter ? "Enabled" : "Disabled")).color(hopperLimiter ? NamedTextColor.GREEN : NamedTextColor.RED));
        hopperMeta.lore(Collections.singletonList(Component.text("Click to " + (hopperLimiter ? "disable" : "enable")).color(NamedTextColor.YELLOW)));
        hopperItem.setItemMeta(hopperMeta);
        gui.setItem(14, hopperItem);

        ItemStack spawnerItem = new ItemStack(spawnerLimiter ? Material.GREEN_WOOL : Material.RED_WOOL);
        ItemMeta spawnerMeta = spawnerItem.getItemMeta();
        spawnerMeta.displayName(Component.text("Spawner Limiter: " + (spawnerLimiter ? "Enabled" : "Disabled")).color(spawnerLimiter ? NamedTextColor.GREEN : NamedTextColor.RED));
        spawnerMeta.lore(Collections.singletonList(Component.text("Click to " + (spawnerLimiter ? "disable" : "enable")).color(NamedTextColor.YELLOW)));
        spawnerItem.setItemMeta(spawnerMeta);
        gui.setItem(16, spawnerItem);

        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.displayName(Component.text("Back to Main Menu").color(NamedTextColor.WHITE));
        backItem.setItemMeta(backMeta);
        gui.setItem(31, backItem);

        openGUIs.put(player.getUniqueId(), "lag-prevention");
        player.openInventory(gui);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        if (!openGUIs.containsKey(player.getUniqueId())) return;

        event.setCancelled(true);

        int topSize = event.getView().getTopInventory().getSize();
        if (event.getRawSlot() < 0 || event.getRawSlot() >= topSize) return;
        if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) return;

        String currentGUI = openGUIs.get(player.getUniqueId());
        int slot = event.getRawSlot();

        switch (currentGUI) {
            case "main":
                handleMainGUIClick(player, slot);
                break;
            case "entity-clearing":
                handleEntityClearingClick(player, slot);
                break;
            case "lag-prevention":
                handleLagPreventionClick(player, slot);
                break;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (openGUIs.containsKey(event.getWhoClicked().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private void handleMainGUIClick(Player player, int slot) {
        switch (slot) {
            case 12:
                openEntityClearingGUI(player);
                break;
            case 14:
                openLagPreventionGUI(player);
                break;
            case 16:
                configManager.reload();
                player.sendMessage(Component.text("Configuration reloaded successfully!").color(NamedTextColor.GREEN));
                break;
        }
    }

    private void handleEntityClearingClick(Player player, int slot) {
        UUID playerId = player.getUniqueId();
        switch (slot) {
            case 10:
                boolean currentEnabled = configManager.getBoolean("entity-clearing.enabled", true);
                configManager.set("entity-clearing.enabled", !currentEnabled);
                plugin.saveConfig();
                openEntityClearingGUI(player);
                break;
            case 12:
                player.closeInventory();
                awaitingInput.put(playerId, "entity-clearing-interval");
                inputPaths.put(playerId, "entity-clearing.interval");
                player.sendMessage(Component.text("Enter new clearing interval in seconds:").color(NamedTextColor.YELLOW));
                player.sendMessage(Component.text("Type 'cancel' to cancel.").color(NamedTextColor.GRAY));
                break;
            case 14:
                boolean currentNamed = configManager.getBoolean("entity-clearing.protect-named-entities", true);
                configManager.set("entity-clearing.protect-named-entities", !currentNamed);
                plugin.saveConfig();
                openEntityClearingGUI(player);
                break;
            case 16:
                boolean currentTamed = configManager.getBoolean("entity-clearing.protect-tamed-entities", true);
                configManager.set("entity-clearing.protect-tamed-entities", !currentTamed);
                plugin.saveConfig();
                openEntityClearingGUI(player);
                break;
            case 31:
                openMainGUI(player);
                break;
        }
    }

    private void handleLagPreventionClick(Player player, int slot) {
        switch (slot) {
            case 10:
                boolean mobLimiter = configManager.getBoolean("lag-prevention.mob-limiter.enabled", true);
                configManager.set("lag-prevention.mob-limiter.enabled", !mobLimiter);
                plugin.saveConfig();
                openLagPreventionGUI(player);
                break;
            case 12:
                boolean redstoneLimiter = configManager.getBoolean("lag-prevention.redstone-limiter.enabled", true);
                configManager.set("lag-prevention.redstone-limiter.enabled", !redstoneLimiter);
                plugin.saveConfig();
                openLagPreventionGUI(player);
                break;
            case 14:
                boolean hopperLimiter = configManager.getBoolean("lag-prevention.hopper-limiter.enabled", true);
                configManager.set("lag-prevention.hopper-limiter.enabled", !hopperLimiter);
                plugin.saveConfig();
                openLagPreventionGUI(player);
                break;
            case 16:
                boolean spawnerLimiter = configManager.getBoolean("lag-prevention.spawner-limiter.enabled", true);
                configManager.set("lag-prevention.spawner-limiter.enabled", !spawnerLimiter);
                plugin.saveConfig();
                openLagPreventionGUI(player);
                break;
            case 31:
                openMainGUI(player);
                break;
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();

        if (awaitingInput.containsKey(playerId)) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!awaitingInput.containsKey(playerId) && !openGUIs.containsKey(playerId)) {
                openGUIs.remove(playerId); // harmless if absent
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (!awaitingInput.containsKey(playerId)) return;

        event.setCancelled(true);
        String input = event.getMessage().trim();
        String inputType = awaitingInput.get(playerId);
        String configPath = inputPaths.get(playerId);

        if (input.equalsIgnoreCase("cancel")) {
            awaitingInput.remove(playerId);
            inputPaths.remove(playerId);
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage(Component.text("Input cancelled.").color(NamedTextColor.GRAY));
                if ("entity-clearing-interval".equals(inputType)) {
                    openEntityClearingGUI(player);
                }
            });
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                if ("entity-clearing-interval".equals(inputType)) {
                    int interval = Integer.parseInt(input);
                    if (interval < 10) {
                        player.sendMessage(Component.text("Interval must be at least 10 seconds!").color(NamedTextColor.RED));
                        return;
                    }
                    configManager.set(configPath, interval);
                    plugin.saveConfig();
                    player.sendMessage(Component.text("Entity clearing interval set to " + interval + " seconds!").color(NamedTextColor.GREEN));
                }
                awaitingInput.remove(playerId);
                inputPaths.remove(playerId);
                if ("entity-clearing-interval".equals(inputType)) {
                    openEntityClearingGUI(player);
                }
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("Invalid number! Please enter a valid integer.").color(NamedTextColor.RED));
            }
        });
    }

    public void shutdown() {
        if (performanceUpdateTask != null) {
            performanceUpdateTask.cancel();
        }
        openGUIs.clear();
        awaitingInput.clear();
        inputPaths.clear();
    }
}
