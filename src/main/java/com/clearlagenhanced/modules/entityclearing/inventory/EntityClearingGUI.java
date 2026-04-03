package com.clearlagenhanced.modules.entityclearing.inventory;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.core.module.EntityClearingModule;
import com.clearlagenhanced.core.module.Module;
import com.clearlagenhanced.inventory.InventoryButton;
import com.clearlagenhanced.inventory.InventoryGUI;
import com.clearlagenhanced.modules.entityclearing.models.AdaptiveIntervalSettings;
import com.clearlagenhanced.modules.entityclearing.tasks.AutoClearTask;
import com.clearlagenhanced.utils.MessageUtils;
import com.cryptomorin.xseries.XMaterial;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EntityClearingGUI extends InventoryGUI {
    private final ClearLaggEnhanced plugin;
    private final EntityClearingModule module;
    private WrappedTask refreshTask;
    
    public EntityClearingGUI(ClearLaggEnhanced plugin, Module module) {
        this(plugin, (EntityClearingModule) module);
    }

    public EntityClearingGUI(ClearLaggEnhanced plugin, EntityClearingModule module) {
        this.plugin = plugin;
        this.module = module;
    }
    
    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 36, ChatColor.translateAlternateColorCodes('&', "&c&lEntity Clearing"));
    }
    
    @Override
    public void decorate(Player player) {
        boolean enabled = module.isEnabled();
        int fallbackInterval = module.getConfig().getInt("interval", 300);
        boolean adaptiveIntervalEnabled = module.getConfig().getBoolean("adaptive-interval.enabled", false);
        AutoClearTask.StatusSnapshot statusSnapshot = module.getStatusSnapshot();
        boolean protectNamed = module.getConfig().getBoolean("protect-named-entities", true);
        boolean protectTamed = module.getConfig().getBoolean("protect-tamed-entities", true);
        boolean protectStacked = module.getConfig().getBoolean("protect-stacked-entities", true);
        AdaptiveIntervalSettings.Metric adaptiveMetric = AdaptiveIntervalSettings.Metric.fromName(
                module.getConfig().getString("adaptive-interval.metric")
        );
        List<AdaptiveIntervalSettings.Tier> adaptiveTiers = readAdaptiveTiers();
        
        addButton(10, new InventoryButton()
            .creator(p -> createToggleItem(enabled))
            .consumer(event -> {
                plugin.getModuleManager().setModuleEnabled(module, !enabled);
                plugin.getGuiManager().openGUI(new EntityClearingGUI(plugin, module), (Player) event.getWhoClicked());
            })
        );
        
        addButton(12, new InventoryButton()
            .creator(p -> createIntervalItem(fallbackInterval, adaptiveIntervalEnabled, statusSnapshot))
            .consumer(event -> {
                Player clicker = (Player) event.getWhoClicked();
                MessageUtils.sendMessage(clicker, "gui.enter-interval");
                clicker.closeInventory();

                plugin.getChatInputManager().requestInput(clicker, input -> {
                    if (input != null) {
                        try {
                            int newVal = Integer.parseInt(input);
                            if (newVal < 10) {
                                MessageUtils.sendMessage(clicker, "gui.interval-min", "min", "10");
                            } else {
                                module.getConfig().set("interval", newVal);
                                module.saveConfig();
                                module.onReload(); // This refreshes the auto-clear task
                                MessageUtils.sendMessage(clicker, "gui.interval-set", "interval", input);
                            }
                        } catch (NumberFormatException e) {
                            MessageUtils.sendMessage(clicker, "gui.invalid-number");
                        }
                    }
                    // Re-open GUI regardless of success/fail/cancel
                    plugin.getGuiManager().openGUI(new EntityClearingGUI(plugin, module), clicker);
                });
            })
        );

        addButton(13, new InventoryButton()
            .creator(p -> createAdaptiveItem(adaptiveIntervalEnabled, adaptiveMetric, adaptiveTiers, statusSnapshot))
            .consumer(event -> {
                Player clicker = (Player) event.getWhoClicked();

                if (event.isShiftClick()) {
                    MessageUtils.sendMessage(clicker, "gui.enter-adaptive-thresholds");
                    MessageUtils.sendMessage(clicker, "gui.adaptive-thresholds-format");
                    clicker.closeInventory();

                    plugin.getChatInputManager().requestInput(clicker, input -> {
                        if (input != null) {
                            try {
                                List<AdaptiveIntervalSettings.Tier> parsedTiers = parseTierInput(input);
                                saveAdaptiveTiers(parsedTiers);
                                module.onReload();
                                MessageUtils.sendMessage(clicker, "gui.adaptive-thresholds-set");
                            } catch (IllegalArgumentException exception) {
                                MessageUtils.sendMessage(clicker, "gui.adaptive-thresholds-invalid", "reason", exception.getMessage());
                            }
                        }

                        plugin.getGuiManager().openGUI(new EntityClearingGUI(plugin, module), clicker);
                    });
                    return;
                }

                if (event.isRightClick()) {
                    AdaptiveIntervalSettings.Metric nextMetric = adaptiveMetric == AdaptiveIntervalSettings.Metric.ENTITY_COUNT
                            ? AdaptiveIntervalSettings.Metric.PLAYER_COUNT
                            : AdaptiveIntervalSettings.Metric.ENTITY_COUNT;
                    module.getConfig().set("adaptive-interval.metric", nextMetric.name());
                    module.saveConfig();
                    module.onReload();
                    MessageUtils.sendMessage(clicker, "gui.adaptive-metric-set", "metric", nextMetric.name());
                    plugin.getGuiManager().openGUI(new EntityClearingGUI(plugin, module), clicker);
                    return;
                }

                module.getConfig().set("adaptive-interval.enabled", !adaptiveIntervalEnabled);
                module.saveConfig();
                module.onReload();
                MessageUtils.sendMessage(clicker, "gui.adaptive-enabled-set", "state", (!adaptiveIntervalEnabled) ? "enabled" : "disabled");
                plugin.getGuiManager().openGUI(new EntityClearingGUI(plugin, module), clicker);
            })
        );
        
        addButton(14, new InventoryButton()
            .creator(p -> createProtectionItem("Named Entities", protectNamed))
            .consumer(event -> {
                module.getConfig().set("protect-named-entities", !protectNamed);
                module.saveConfig();
                plugin.getEntityProtectionUtils().refreshSettingsCache();
                plugin.getGuiManager().openGUI(new EntityClearingGUI(plugin, module), (Player) event.getWhoClicked());
            })
        );
        
        addButton(16, new InventoryButton()
            .creator(p -> createProtectionItem("Tamed Entities", protectTamed))
            .consumer(event -> {
                module.getConfig().set("protect-tamed-entities", !protectTamed);
                module.saveConfig();
                plugin.getEntityProtectionUtils().refreshSettingsCache();
                plugin.getGuiManager().openGUI(new EntityClearingGUI(plugin, module), (Player) event.getWhoClicked());
            })
        );
        
        addButton(20, new InventoryButton()
            .creator(p -> createProtectionItem("Stacked Entities", protectStacked))
            .consumer(event -> {
                module.getConfig().set("protect-stacked-entities", !protectStacked);
                module.saveConfig();
                plugin.getEntityProtectionUtils().refreshSettingsCache();
                plugin.getGuiManager().openGUI(new EntityClearingGUI(plugin, module), (Player) event.getWhoClicked());
            })
        );
        
        addButton(31, new InventoryButton()
            .creator(p -> createBackItem())
            .consumer(event -> {
                Player clicker = (Player) event.getWhoClicked();
                clicker.closeInventory();
                clicker.performCommand("lagg admin");
            })
        );
        
        super.decorate(player);
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);

        Player player = (Player) event.getPlayer();
        stopRefreshTask();
        refreshTask = ClearLaggEnhanced.scheduler().runTimer(() -> {
            if (!player.isOnline() || player.getOpenInventory().getTopInventory() != getInventory()) {
                stopRefreshTask();
                return;
            }

            decorate(player);
            player.updateInventory();
        }, 20L, 20L);
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        stopRefreshTask();
    }
    
    private ItemStack createToggleItem(boolean enabled) {
        ItemStack item = enabled ? XMaterial.GREEN_WOOL.parseItem() : XMaterial.RED_WOOL.parseItem();
        if (item != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', enabled ? "&aEnabled" : "&cDisabled"));
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Click to " + (enabled ? "disable" : "enable")));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
        }
        return item;
    }
    
    private ItemStack createIntervalItem(int fallbackInterval, boolean adaptiveIntervalEnabled, AutoClearTask.StatusSnapshot statusSnapshot) {
        ItemStack item = XMaterial.CLOCK.parseItem();
        if (item != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                int displayedInterval = statusSnapshot != null ? statusSnapshot.activeIntervalSeconds() : fallbackInterval;
                String label = adaptiveIntervalEnabled ? "Current Interval" : "Interval";
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&e" + label + ": &f" + displayedInterval + "s"));
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Current: " + displayedInterval + " seconds"));
                if (adaptiveIntervalEnabled) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&7Fallback: " + fallbackInterval + " seconds"));
                    if (statusSnapshot != null && statusSnapshot.sampledMetricValue() >= 0) {
                        lore.add(ChatColor.translateAlternateColorCodes('&',
                                "&7Detected: " + statusSnapshot.sampledMetricValue() + " " + metricLabel(statusSnapshot.metric())));
                    }
                }
                lore.add("");
                lore.add(ChatColor.translateAlternateColorCodes('&', "&aClick to change fallback interval"));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
        }
        return item;
    }

    private ItemStack createAdaptiveItem(
            boolean enabled,
            AdaptiveIntervalSettings.Metric metric,
            List<AdaptiveIntervalSettings.Tier> tiers,
            AutoClearTask.StatusSnapshot statusSnapshot
    ) {
        ItemStack item = XMaterial.REPEATER.parseItem();
        if (item == null) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                enabled ? "&bAdaptive Interval: &aEnabled" : "&bAdaptive Interval: &cDisabled"));

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Metric: " + metric.name()));
        if (statusSnapshot != null && enabled) {
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Current interval: " + statusSnapshot.activeIntervalSeconds() + " seconds"));
            if (statusSnapshot.sampledMetricValue() >= 0) {
                lore.add(ChatColor.translateAlternateColorCodes('&',
                        "&7Detected: " + statusSnapshot.sampledMetricValue() + " " + metricLabel(statusSnapshot.metric())));
            }
        }
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Tiers:"));
        if (tiers.isEmpty()) {
            lore.add(ChatColor.translateAlternateColorCodes('&', "&8- none configured"));
        } else {
            int limit = Math.min(tiers.size(), 4);
            for (int i = 0; i < limit; i++) {
                AdaptiveIntervalSettings.Tier tier = tiers.get(i);
                lore.add(ChatColor.translateAlternateColorCodes('&',
                        "&8- &f" + tier.threshold() + "+ &7-> &f" + tier.interval() + "s"));
            }
            if (tiers.size() > limit) {
                lore.add(ChatColor.translateAlternateColorCodes('&', "&8... and " + (tiers.size() - limit) + " more"));
            }
        }
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&aLeft-click: toggle adaptive mode"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&eRight-click: switch metric"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&bShift-click: edit thresholds"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createProtectionItem(String name, boolean enabled) {
        ItemStack item = enabled ? XMaterial.GREEN_WOOL.parseItem() : XMaterial.RED_WOOL.parseItem();
        if (item != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&eProtect " + name + ": " + (enabled ? "&aYes" : "&cNo")));
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Click to toggle"));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
        }
        return item;
    }

    private List<AdaptiveIntervalSettings.Tier> readAdaptiveTiers() {
        List<Map<?, ?>> tierMaps = module.getConfig().getMapList("adaptive-interval.tiers");
        List<AdaptiveIntervalSettings.Tier> tiers = new ArrayList<>();
        for (Map<?, ?> tierMap : tierMaps) {
            Integer threshold = parseInt(tierMap.get("threshold"));
            Integer interval = parseInt(tierMap.get("interval"));
            if (threshold == null || threshold < 0 || interval == null || interval < 10) {
                continue;
            }
            tiers.add(new AdaptiveIntervalSettings.Tier(threshold, interval));
        }

        tiers.sort(Comparator.comparingInt(AdaptiveIntervalSettings.Tier::threshold));
        return tiers;
    }

    private List<AdaptiveIntervalSettings.Tier> parseTierInput(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Please provide at least one threshold.");
        }

        String[] entries = input.split(",");
        List<AdaptiveIntervalSettings.Tier> tiers = new ArrayList<>();
        java.util.Set<Integer> seenThresholds = new java.util.HashSet<>();

        for (String entry : entries) {
            String trimmedEntry = entry.trim();
            if (trimmedEntry.isEmpty()) {
                continue;
            }

            String[] pair = trimmedEntry.split(":");
            if (pair.length != 2) {
                throw new IllegalArgumentException("Use threshold:interval pairs separated by commas.");
            }

            int threshold;
            int interval;
            try {
                threshold = Integer.parseInt(pair[0].trim());
                interval = Integer.parseInt(pair[1].trim());
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Thresholds and intervals must be whole numbers.");
            }

            if (threshold < 0) {
                throw new IllegalArgumentException("Thresholds cannot be negative.");
            }

            if (interval < 10) {
                throw new IllegalArgumentException("Intervals must be at least 10 seconds.");
            }

            if (!seenThresholds.add(threshold)) {
                throw new IllegalArgumentException("Duplicate thresholds are not allowed.");
            }

            tiers.add(new AdaptiveIntervalSettings.Tier(threshold, interval));
        }

        if (tiers.isEmpty()) {
            throw new IllegalArgumentException("Please provide at least one threshold.");
        }

        tiers.sort(Comparator.comparingInt(AdaptiveIntervalSettings.Tier::threshold));
        return tiers;
    }

    private void saveAdaptiveTiers(List<AdaptiveIntervalSettings.Tier> tiers) {
        List<Map<String, Object>> serializedTiers = new ArrayList<>();
        for (AdaptiveIntervalSettings.Tier tier : tiers) {
            serializedTiers.add(Map.of(
                    "threshold", tier.threshold(),
                    "interval", tier.interval()
            ));
        }
        module.getConfig().set("adaptive-interval.tiers", serializedTiers);
        module.saveConfig();
    }

    private Integer parseInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }

        if (value instanceof String string) {
            try {
                return Integer.parseInt(string.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return null;
    }

    private String metricLabel(AdaptiveIntervalSettings.Metric metric) {
        return switch (metric) {
            case ENTITY_COUNT -> "entities";
            case PLAYER_COUNT -> "players";
        };
    }

    private void stopRefreshTask() {
        if (refreshTask != null) {
            ClearLaggEnhanced.scheduler().cancelTask(refreshTask);
            refreshTask = null;
        }
    }
    
    private ItemStack createBackItem() {
        ItemStack item = XMaterial.ARROW.parseItem();
        if (item != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&fBack to Main Menu"));
                item.setItemMeta(meta);
            }
        }
        return item;
    }
}
