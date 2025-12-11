package com.clearlagenhanced.managers;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.utils.MessageUtils;
import com.tcoded.folialib.impl.PlatformScheduler;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GUIManager implements Listener {

    private static class GUIHolder implements InventoryHolder {

        private final String id;

        GUIHolder(String id) {
            this.id = id;
        }

        String id() {
            return id;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private final ClearLaggEnhanced plugin;
    private final ConfigManager configManager;
    private final PerformanceManager performanceManager;
    private final PlatformScheduler scheduler;
    private final Map<UUID, String> openGUIs;
    private final Map<UUID, String> awaitingInput;
    private final Map<UUID, String> inputPaths;
    private WrappedTask performanceUpdateTask;

    public GUIManager(@NotNull ClearLaggEnhanced plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.performanceManager = plugin.getPerformanceManager();
        this.scheduler = ClearLaggEnhanced.scheduler();
        this.openGUIs = new ConcurrentHashMap<>();
        this.awaitingInput = new ConcurrentHashMap<>();
        this.inputPaths = new ConcurrentHashMap<>();
        startPerformanceUpdateTask();
    }

    private void startPerformanceUpdateTask() {
        performanceUpdateTask = scheduler.runTimer(this::updatePerformanceDisplays, 1L, 40L);
    }

    private void updatePerformanceDisplays() {
        for (Map.Entry<UUID, String> entry : new ConcurrentHashMap<>(openGUIs).entrySet()) {
            if ("main".equals(entry.getValue())) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null) {
                    if (player.getOpenInventory().getTopInventory().getHolder() instanceof GUIHolder) {
                        updatePerformanceItem(player.getOpenInventory().getTopInventory());
                    }
                }
            }
        }
    }

    private Component lore(String text, NamedTextColor color) {
        return Component.text(text).color(color).decoration(TextDecoration.ITALIC, false);
    }

    public void openMainGUI(@NotNull Player player) {
        Inventory gui = Bukkit.createInventory(new GUIHolder("main"), 27, Component.text("ClearLaggEnhanced Admin Panel").color(NamedTextColor.DARK_GREEN));

        gui.setItem(10, createPerformanceItem());

        ItemStack entityItem = new ItemStack(Material.IRON_SWORD);
        ItemMeta entityMeta = entityItem.getItemMeta();
        entityMeta.displayName(Component.text("Entity Clearing").color(NamedTextColor.RED));
        entityMeta.lore(Arrays.asList(
                lore("Configure entity clearing settings", NamedTextColor.GRAY),
                lore("• Intervals and timings", NamedTextColor.YELLOW),
                lore("• Entity whitelist/blacklist", NamedTextColor.YELLOW),
                lore("• World configurations", NamedTextColor.YELLOW),
                Component.empty(),
                lore("Click to open settings", NamedTextColor.GREEN)
        ));
        entityItem.setItemMeta(entityMeta);
        gui.setItem(12, entityItem);

        ItemStack lagItem = new ItemStack(Material.REDSTONE);
        ItemMeta lagMeta = lagItem.getItemMeta();
        lagMeta.displayName(Component.text("Lag Prevention").color(NamedTextColor.GOLD));
        lagMeta.lore(Arrays.asList(
                lore("Configure lag prevention modules", NamedTextColor.GRAY),
                lore("• Mob Limiter (per-type limits)", NamedTextColor.YELLOW),
                lore("• Spawner Limiter", NamedTextColor.YELLOW),
                Component.empty(),
                lore("Click to open settings", NamedTextColor.GREEN)
        ));
        lagItem.setItemMeta(lagMeta);
        gui.setItem(14, lagItem);

        ItemStack reloadItem = new ItemStack(Material.COMMAND_BLOCK);
        ItemMeta reloadMeta = reloadItem.getItemMeta();
        reloadMeta.displayName(Component.text("Reload Config").color(NamedTextColor.AQUA));
        reloadMeta.lore(Arrays.asList(
                lore("Reload all plugin configurations", NamedTextColor.GRAY),
                Component.empty(),
                lore("Click to reload", NamedTextColor.GREEN)
        ));
        reloadItem.setItemMeta(reloadMeta);
        gui.setItem(16, reloadItem);

        scheduler.runAtEntity(player, task -> player.openInventory(gui));
        openGUIs.put(player.getUniqueId(), "main");
    }

    private ItemStack createPerformanceItem() {
        ItemStack performanceItem = new ItemStack(Material.CLOCK);
        updatePerformanceItemMeta(performanceItem);
        return performanceItem;
    }

    private void updatePerformanceItemMeta(@NotNull ItemStack performanceItem) {
        ItemMeta performanceMeta = performanceItem.getItemMeta();
        double tps = performanceManager.getTPS();
        String memoryUsage = performanceManager.getFormattedMemoryUsage();
        double memoryPercent = performanceManager.getMemoryUsagePercentage();
        int totalEntities = performanceManager.getTotalEntities();
        NamedTextColor tpsColor = tps >= 18.0 ? NamedTextColor.GREEN : tps >= 15.0 ? NamedTextColor.YELLOW : NamedTextColor.RED;
        NamedTextColor memoryColor = memoryPercent < 70.0 ? NamedTextColor.GREEN : memoryPercent < 85.0 ? NamedTextColor.YELLOW : NamedTextColor.RED;

        performanceMeta.displayName(Component.text("Performance Monitoring").color(NamedTextColor.BLUE));
        performanceMeta.lore(Arrays.asList(
                lore("Real-time server statistics", NamedTextColor.GRAY),
                Component.empty(),
                lore("TPS: " + String.format("%.2f", tps), tpsColor),
                lore("Memory: " + memoryUsage + " (" + String.format("%.1f", memoryPercent) + "%)", memoryColor),
                lore("Entities: " + totalEntities, NamedTextColor.YELLOW),
                Component.empty(),
                lore("Updates every 2 seconds", NamedTextColor.DARK_GRAY)
        ));

        performanceItem.setItemMeta(performanceMeta);
    }

    private void updatePerformanceItem(@NotNull Inventory inventory) {
        ItemStack performanceItem = inventory.getItem(10);
        if (performanceItem != null && performanceItem.getType() == Material.CLOCK) {
            updatePerformanceItemMeta(performanceItem);
        }
    }

    public void openEntityClearingGUI(@NotNull Player player) {
        Inventory gui = Bukkit.createInventory(new GUIHolder("entity-clearing"), 36, Component.text("Entity Clearing Settings").color(NamedTextColor.RED));
        boolean enabled = configManager.getBoolean("entity-clearing.enabled", true);
        int interval = configManager.getInt("entity-clearing.interval", 300);
        boolean protectNamed = configManager.getBoolean("entity-clearing.protect-named-entities", true);
        boolean protectTamed = configManager.getBoolean("entity-clearing.protect-tamed-entities", true);
        boolean protectStacked = configManager.getBoolean("entity-clearing.protect-stacked-entities", true);

        ItemStack toggleItem = new ItemStack(enabled ? Material.GREEN_WOOL : Material.RED_WOOL);
        ItemMeta toggleMeta = toggleItem.getItemMeta();
        toggleMeta.displayName(Component.text("Entity Clearing: " + (enabled ? "Enabled" : "Disabled")).color(enabled ? NamedTextColor.GREEN : NamedTextColor.RED));
        toggleMeta.lore(Collections.singletonList(lore("Click to " + (enabled ? "disable" : "enable"), NamedTextColor.GRAY)));
        toggleItem.setItemMeta(toggleMeta);
        gui.setItem(10, toggleItem);

        ItemStack intervalItem = new ItemStack(Material.CLOCK);
        ItemMeta intervalMeta = intervalItem.getItemMeta();
        intervalMeta.displayName(Component.text("Clearing Interval: " + interval + "s").color(NamedTextColor.YELLOW));
        intervalMeta.lore(Arrays.asList(lore("Current interval: " + interval + " seconds", NamedTextColor.GRAY), Component.empty(), lore("Click to change", NamedTextColor.GREEN)));
        intervalItem.setItemMeta(intervalMeta);
        gui.setItem(12, intervalItem);

        ItemStack namedItem = new ItemStack(protectNamed ? Material.NAME_TAG : Material.PAPER);
        ItemMeta namedMeta = namedItem.getItemMeta();
        namedMeta.displayName(Component.text("Protect Named: " + (protectNamed ? "Yes" : "No")).color(protectNamed ? NamedTextColor.GREEN : NamedTextColor.RED));
        namedMeta.lore(Collections.singletonList(lore("Click to " + (protectNamed ? "disable" : "enable"), NamedTextColor.GRAY)));
        namedItem.setItemMeta(namedMeta);
        gui.setItem(14, namedItem);

        ItemStack tamedItem = new ItemStack(protectTamed ? Material.BONE : Material.STICK);
        ItemMeta tamedMeta = tamedItem.getItemMeta();
        tamedMeta.displayName(Component.text("Protect Tamed: " + (protectTamed ? "Yes" : "No")).color(protectTamed ? NamedTextColor.GREEN : NamedTextColor.RED));
        tamedMeta.lore(Collections.singletonList(lore("Click to " + (protectTamed ? "disable" : "enable"), NamedTextColor.GRAY)));
        tamedItem.setItemMeta(tamedMeta);
        gui.setItem(16, tamedItem);

        ItemStack stackedItem = new ItemStack(protectStacked ? Material.CHEST : Material.ENDER_CHEST);
        ItemMeta stackedMeta = stackedItem.getItemMeta();
        stackedMeta.displayName(Component.text("Protect Stacked: " + (protectStacked ? "Yes" : "No")).color(protectStacked ? NamedTextColor.GREEN : NamedTextColor.RED));
        stackedMeta.lore(Collections.singletonList(lore("Click to " + (protectStacked ? "disable" : "enable"), NamedTextColor.GRAY)));
        stackedItem.setItemMeta(stackedMeta);
        gui.setItem(20, stackedItem);

        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.displayName(Component.text("Back to Main Menu").color(NamedTextColor.WHITE));
        backItem.setItemMeta(backMeta);
        gui.setItem(31, backItem);

        scheduler.runAtEntity(player, task -> player.openInventory(gui));
        openGUIs.put(player.getUniqueId(), "entity-clearing");
    }

    public void openLagPreventionGUI(@NotNull Player player) {
        Inventory gui = Bukkit.createInventory(new GUIHolder("lag-prevention"), 36, Component.text("Lag Prevention Modules").color(NamedTextColor.GOLD));
        boolean mobLimiter = configManager.getBoolean("lag-prevention.mob-limiter.enabled", true);
        boolean spawnerLimiter = configManager.getBoolean("lag-prevention.spawner-limiter.enabled", true);
        int maxMobs = configManager.getInt("lag-prevention.mob-limiter.max-mobs-per-chunk", 50);

        ItemStack mobItem = new ItemStack(mobLimiter ? Material.GREEN_WOOL : Material.RED_WOOL);
        ItemMeta mobMeta = mobItem.getItemMeta();
        mobMeta.displayName(Component.text("Mob Limiter: " + (mobLimiter ? "Enabled" : "Disabled")).color(mobLimiter ? NamedTextColor.GREEN : NamedTextColor.RED));
        mobMeta.lore(Arrays.asList(
                lore("Limits mob spawns per chunk", NamedTextColor.GRAY),
                lore("Global: " + maxMobs + " mobs/chunk", NamedTextColor.GRAY),
                lore("Per-type limits in config.yml", NamedTextColor.DARK_GRAY),
                Component.empty(),
                lore("Click to " + (mobLimiter ? "disable" : "enable"), NamedTextColor.YELLOW)));
        mobItem.setItemMeta(mobMeta);
        gui.setItem(10, mobItem);

        ItemStack spawnerItem = new ItemStack(spawnerLimiter ? Material.GREEN_WOOL : Material.RED_WOOL);
        ItemMeta spawnerMeta = spawnerItem.getItemMeta();
        spawnerMeta.displayName(Component.text("Spawner Limiter: " + (spawnerLimiter ? "Enabled" : "Disabled")).color(spawnerLimiter ? NamedTextColor.GREEN : NamedTextColor.RED));
        spawnerMeta.lore(Collections.singletonList(lore("Click to " + (spawnerLimiter ? "disable" : "enable"), NamedTextColor.YELLOW)));
        spawnerItem.setItemMeta(spawnerMeta);
        gui.setItem(12, spawnerItem);

        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.displayName(Component.text("Back to Main Menu").color(NamedTextColor.WHITE));
        backItem.setItemMeta(backMeta);
        gui.setItem(31, backItem);

        scheduler.runAtEntity(player, task -> player.openInventory(gui));
        openGUIs.put(player.getUniqueId(), "lag-prevention");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        InventoryHolder holder = top.getHolder();
        if (!(holder instanceof GUIHolder guiHolder)) {
            return;
        }

        int topSize = top.getSize();
        int raw = event.getRawSlot();
        boolean clickedTop = raw >= 0 && raw < topSize;

        if (clickedTop) {
            event.setCancelled(true);

            switch (guiHolder.id()) {
                case "main" -> handleMainGUIClick(player, raw);
                case "entity-clearing" -> handleEntityClearingClick(player, raw);
                case "lag-prevention" -> handleLagPreventionClick(player, raw);
            }

            return;
        }

        InventoryAction action = event.getAction();

        if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            event.setCancelled(true);
            return;
        }

        if (action == InventoryAction.COLLECT_TO_CURSOR) {
            ItemStack cursor = event.getCursor();
            if (!cursor.getType().isAir()) {
                for (ItemStack it : top.getContents()) {
                    if (it != null && it.isSimilar(cursor)) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(@NotNull InventoryDragEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof GUIHolder)) {
            return;
        }

        int topSize = event.getView().getTopInventory().getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= 0 && rawSlot < topSize) {
                event.setCancelled(true);
                break;
            }
        }
    }

    private void handleMainGUIClick(@NotNull Player player, int slot) {
        switch (slot) {
            case 12 -> openEntityClearingGUI(player);
            case 14 -> openLagPreventionGUI(player);
            case 16 -> {
                player.closeInventory();
                plugin.reloadAll(player);
            }
        }
    }

    private void handleEntityClearingClick(@NotNull Player player, int slot) {
        UUID playerId = player.getUniqueId();
        switch (slot) {
            case 10 -> {
                toggleAndRefresh(player, "entity-clearing.enabled");
            }
            case 12 -> {
                scheduler.runAtEntity(player, task -> player.closeInventory());
                awaitingInput.put(playerId, "entity-clearing-interval");
                inputPaths.put(playerId, "entity-clearing.interval");
                MessageUtils.sendMessage(player, "gui.enter-interval");
                MessageUtils.sendMessage(player, "gui.type-cancel");
            }
            case 14 -> {
                toggleAndRefresh(player, "entity-clearing.protect-named-entities");
            }
            case 16 -> {
                toggleAndRefresh(player, "entity-clearing.protect-tamed-entities");
            }
            case 20 -> {
                toggleAndRefresh(player, "entity-clearing.protect-stacked-entities");
            }
            case 31 -> openMainGUI(player);
        }
    }

    private void handleLagPreventionClick(@NotNull Player player, int slot) {
        switch (slot) {
            case 10 -> {
                toggleAndRefresh(player, "lag-prevention.mob-limiter.enabled");
            }
            case 12 -> {
                toggleAndRefresh(player, "lag-prevention.spawner-limiter.enabled");
            }
            case 31 -> openMainGUI(player);
        }
    }

    private void refreshEntityClearingGUI(@NotNull Player player) {
        Inventory gui = player.getOpenInventory().getTopInventory();
        if (!(gui.getHolder() instanceof GUIHolder holder) || !"entity-clearing".equals(holder.id())) {
            return;
        }

        boolean enabled = configManager.getBoolean("entity-clearing.enabled", true);
        int interval = configManager.getInt("entity-clearing.interval", 300);
        boolean protectNamed = configManager.getBoolean("entity-clearing.protect-named-entities", true);
        boolean protectTamed = configManager.getBoolean("entity-clearing.protect-tamed-entities", true);
        boolean protectStacked = configManager.getBoolean("entity-clearing.protect-stacked-entities", true);

        ItemStack toggleItem = new ItemStack(enabled ? Material.GREEN_WOOL : Material.RED_WOOL);
        ItemMeta toggleMeta = toggleItem.getItemMeta();
        toggleMeta.displayName(Component.text("Entity Clearing: " + (enabled ? "Enabled" : "Disabled")).color(enabled ? NamedTextColor.GREEN : NamedTextColor.RED));
        toggleMeta.lore(Collections.singletonList(lore("Click to " + (enabled ? "disable" : "enable"), NamedTextColor.GRAY)));
        toggleItem.setItemMeta(toggleMeta);
        gui.setItem(10, toggleItem);

        ItemStack intervalItem = new ItemStack(Material.CLOCK);
        ItemMeta intervalMeta = intervalItem.getItemMeta();
        intervalMeta.displayName(Component.text("Clearing Interval: " + interval + "s").color(NamedTextColor.YELLOW));
        intervalMeta.lore(Arrays.asList(lore("Current interval: " + interval + " seconds", NamedTextColor.GRAY), Component.empty(), lore("Click to change", NamedTextColor.GREEN)));
        intervalItem.setItemMeta(intervalMeta);
        gui.setItem(12, intervalItem);

        ItemStack namedItem = new ItemStack(protectNamed ? Material.NAME_TAG : Material.PAPER);
        ItemMeta namedMeta = namedItem.getItemMeta();
        namedMeta.displayName(Component.text("Protect Named: " + (protectNamed ? "Yes" : "No")).color(protectNamed ? NamedTextColor.GREEN : NamedTextColor.RED));
        namedMeta.lore(Collections.singletonList(lore("Click to " + (protectNamed ? "disable" : "enable"), NamedTextColor.GRAY)));
        namedItem.setItemMeta(namedMeta);
        gui.setItem(14, namedItem);

        ItemStack tamedItem = new ItemStack(protectTamed ? Material.BONE : Material.STICK);
        ItemMeta tamedMeta = tamedItem.getItemMeta();
        tamedMeta.displayName(Component.text("Protect Tamed: " + (protectTamed ? "Yes" : "No")).color(protectTamed ? NamedTextColor.GREEN : NamedTextColor.RED));
        tamedMeta.lore(Collections.singletonList(lore("Click to " + (protectTamed ? "disable" : "enable"), NamedTextColor.GRAY)));
        tamedItem.setItemMeta(tamedMeta);
        gui.setItem(16, tamedItem);

        ItemStack stackedItem = new ItemStack(protectStacked ? Material.CHEST : Material.ENDER_CHEST);
        ItemMeta stackedMeta = stackedItem.getItemMeta();
        stackedMeta.displayName(Component.text("Protect Stacked: " + (protectStacked ? "Yes" : "No")).color(protectStacked ? NamedTextColor.GREEN : NamedTextColor.RED));
        stackedMeta.lore(Collections.singletonList(lore("Click to " + (protectStacked ? "disable" : "enable"), NamedTextColor.GRAY)));
        stackedItem.setItemMeta(stackedMeta);
        gui.setItem(20, stackedItem);
    }

    private void refreshLagPreventionGUI(@NotNull Player player) {
        Inventory gui = player.getOpenInventory().getTopInventory();
        if (!(gui.getHolder() instanceof GUIHolder holder) || !"lag-prevention".equals(holder.id())) {
            return;
        }

        boolean mobLimiter = configManager.getBoolean("lag-prevention.mob-limiter.enabled", true);
        boolean spawnerLimiter = configManager.getBoolean("lag-prevention.spawner-limiter.enabled", true);
        int maxMobs = configManager.getInt("lag-prevention.mob-limiter.max-mobs-per-chunk", 50);

        ItemStack mobItem = new ItemStack(mobLimiter ? Material.GREEN_WOOL : Material.RED_WOOL);
        ItemMeta mobMeta = mobItem.getItemMeta();
        mobMeta.displayName(Component.text("Mob Limiter: " + (mobLimiter ? "Enabled" : "Disabled")).color(mobLimiter ? NamedTextColor.GREEN : NamedTextColor.RED));
        mobMeta.lore(Arrays.asList(
                lore("Limits mob spawns per chunk", NamedTextColor.GRAY),
                lore("Global: " + maxMobs + " mobs/chunk", NamedTextColor.GRAY),
                lore("Per-type limits in config.yml", NamedTextColor.DARK_GRAY),
                Component.empty(),
                lore("Click to " + (mobLimiter ? "disable" : "enable"), NamedTextColor.YELLOW)));
        mobItem.setItemMeta(mobMeta);
        gui.setItem(10, mobItem);

        ItemStack spawnerItem = new ItemStack(spawnerLimiter ? Material.GREEN_WOOL : Material.RED_WOOL);
        ItemMeta spawnerMeta = spawnerItem.getItemMeta();
        spawnerMeta.displayName(Component.text("Spawner Limiter: " + (spawnerLimiter ? "Enabled" : "Disabled")).color(spawnerLimiter ? NamedTextColor.GREEN : NamedTextColor.RED));
        spawnerMeta.lore(Collections.singletonList(lore("Click to " + (spawnerLimiter ? "disable" : "enable"), NamedTextColor.YELLOW)));
        spawnerItem.setItemMeta(spawnerMeta);
        gui.setItem(12, spawnerItem);
    }

    private void toggleAndRefresh(Player player, String path) {
        boolean currentValue = configManager.getBoolean(path, true);
        configManager.set(path, !currentValue);
        configManager.save();

        String guiId = openGUIs.get(player.getUniqueId());
        scheduler.runAtEntity(player, task -> {
            if ("entity-clearing".equals(guiId)) {
                refreshEntityClearingGUI(player);
            } else if ("lag-prevention".equals(guiId)) {
                refreshLagPreventionGUI(player);
            }
        });
    }

    @EventHandler
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        openGUIs.remove(playerId);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncChat(@NotNull AsyncChatEvent event) {
        final Player player = event.getPlayer();
        final UUID playerId = player.getUniqueId();
        if (!awaitingInput.containsKey(playerId)) {
            return;
        }

        final String input = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        event.setCancelled(true);
        processChatInput(player, input);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncPlayerChat(@NotNull AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();
        final UUID playerId = player.getUniqueId();
        if (!awaitingInput.containsKey(playerId)) return;

        event.setCancelled(true);
        final String input = event.getMessage().trim();
        processChatInput(player, input);
    }

    private void processChatInput(@NotNull Player player, @NotNull String input) {
        final UUID playerId = player.getUniqueId();
        final String inputType = awaitingInput.get(playerId);
        final String configPath = inputPaths.get(playerId);

        if (input.equalsIgnoreCase("cancel")) {
            awaitingInput.remove(playerId);
            inputPaths.remove(playerId);
            scheduler.runNextTick(task -> {
                MessageUtils.sendMessage(player, "gui.input-cancelled");
                if ("entity-clearing-interval".equals(inputType)) {
                    openEntityClearingGUI(player);
                }
            });

            return;
        }

        scheduler.runNextTick(task -> {
            try {
                if ("entity-clearing-interval".equals(inputType)) {
                    int interval = Integer.parseInt(input);
                    if (interval < 10) {
                        Map<String, String> ph = new ConcurrentHashMap<>();
                        ph.put("min", String.valueOf(10));
                        MessageUtils.sendMessage(player, "gui.interval-min", ph);
                        return;
                    }

                    configManager.set(configPath, interval);
                    configManager.save();
                    configManager.reload();
                    Map<String, String> ph2 = new ConcurrentHashMap<>();
                    ph2.put("interval", String.valueOf(interval));
                    MessageUtils.sendMessage(player, "gui.interval-set", ph2);
                }

                awaitingInput.remove(playerId);
                inputPaths.remove(playerId);
                if ("entity-clearing-interval".equals(inputType)) {
                    openEntityClearingGUI(player);
                }
            } catch (NumberFormatException e) {
                MessageUtils.sendMessage(player, "gui.invalid-number");
            }
        });
    }

    public void shutdown() {
        if (performanceUpdateTask != null) {
            scheduler.cancelTask(performanceUpdateTask);
        }

        openGUIs.clear();
        awaitingInput.clear();
        inputPaths.clear();
    }
}
