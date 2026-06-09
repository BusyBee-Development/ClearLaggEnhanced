package net.busybee.clearlaggenhanced.modules.entityclearing.inventory;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import net.busybee.clearlaggenhanced.ClearLaggEnhanced;
import net.busybee.clearlaggenhanced.gui.base.InventoryGUI;
import net.busybee.clearlaggenhanced.modules.entityclearing.EntityClearingModule;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

public class MobWhitelistGUI extends InventoryGUI {

    private final ClearLaggEnhanced plugin;
    private final EntityClearingModule module;
    private final int page;
    private final String selectedCategory;
    private final List<String> allItems;

    public MobWhitelistGUI(ClearLaggEnhanced plugin, EntityClearingModule module, int page) {
        this(plugin, module, page, null);
    }

    public MobWhitelistGUI(ClearLaggEnhanced plugin, EntityClearingModule module, int page, String selectedCategory) {
        super(54, ChatColor.translateAlternateColorCodes('&', 
                (selectedCategory == null ? "&2&lWhitelists" : 
                selectedCategory.equals("dynamic_custom") ? "&5&lDynamic / Custom" :
                module.getEntitiesConfig().getString("categories." + selectedCategory + ".display-name", "&2&lWhitelist"))
                + " &8- Page " + (page + 1)));
        this.plugin = plugin;
        this.module = module;
        this.page = page;
        this.selectedCategory = selectedCategory;

        module.loadEntitiesConfig();

        if (selectedCategory == null) {
            ConfigurationSection catSection = module.getEntitiesConfig().getConfigurationSection("categories");
            if (catSection != null) {
                this.allItems = new ArrayList<>(catSection.getKeys(false));
            } else {
                this.allItems = new ArrayList<>();
            }

            if (!getDynamicItems().isEmpty()) {
                this.allItems.add("dynamic_custom");
            }
        } else if (selectedCategory.equals("dynamic_custom")) {
            this.allItems = getDynamicItems();
            this.allItems.sort(String::compareTo);
        } else {
            ConfigurationSection entriesSection = module.getEntitiesConfig().getConfigurationSection("categories." + selectedCategory + ".entries");
            if (entriesSection != null) {
                this.allItems = new ArrayList<>(entriesSection.getKeys(false));
            } else {
                this.allItems = new ArrayList<>();
            }
            this.allItems.sort(String::compareTo);
        }
    }

    @Override
    public void decorate(Player player) {
        int start = page * 45;
        int end = Math.min(start + 45, allItems.size());

        if (selectedCategory == null) {
            decorateCategories(player, start, end);

            setItem(48, createNavigationItem("&b&l+ Add Custom Entry", XMaterial.ANVIL), event -> {
                player.closeInventory();
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aType the technical name of the Mob or Item you want to add (e.g., &fDIAMOND_ORE &aor &fPIG&a):"));
                plugin.getChatInputManager().requestInput(player, input -> {
                    if (input == null) return;
                    
                    String technicalName = input.toUpperCase().replace(" ", "_");
                    boolean isValid = false;
                    String configPath = "";

                    try {
                        EntityType type = EntityType.valueOf(technicalName);
                        if (type != EntityType.UNKNOWN) {
                            isValid = true;
                            configPath = "whitelist";
                        }
                    } catch (Exception ignored) {}

                    if (!isValid) {
                        Material mat = Material.matchMaterial(technicalName);
                        if (mat != null) {
                            isValid = true;
                            configPath = "item-whitelist";
                        }
                    }

                    if (isValid) {
                        List<String> whitelist = module.getConfig().getStringList(configPath);
                        if (!whitelist.contains(technicalName)) {
                            whitelist.add(technicalName);
                            module.getConfig().set(configPath, whitelist);
                            module.saveConfig();
                            plugin.getEntityProtectionUtils().refreshSettingsCache();
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aSuccessfully added &f" + technicalName + " &ato the whitelist!"));
                        } else {
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c" + technicalName + " is already in the whitelist!"));
                        }
                    } else {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cInvalid Mob or Item name: " + technicalName));
                    }

                    new MobWhitelistGUI(plugin, module, 0).open(player);
                });
            });
        } else {
            decorateEntries(player, start, end);
        }

        if (page > 0) {
            setItem(45, createNavigationItem("&aPrevious Page", XMaterial.ARROW), event -> {
                new MobWhitelistGUI(plugin, module, page - 1, selectedCategory).open(player);
            });
        }

        if (selectedCategory != null) {
            setItem(49, createNavigationItem("&eBack to Categories", XMaterial.BOOKSHELF), event -> {
                new MobWhitelistGUI(plugin, module, 0, null).open(player);
            });
        } else {
            setItem(49, createNavigationItem("&fBack to Entity Clearing", XMaterial.IRON_SWORD), event -> {
                new EntityClearingGUI(plugin, module).open(player);
            });
        }

        if (end < allItems.size()) {
            setItem(53, createNavigationItem("&aNext Page", XMaterial.ARROW), event -> {
                new MobWhitelistGUI(plugin, module, page + 1, selectedCategory).open(player);
            });
        }
    }

    private void decorateCategories(Player player, int start, int end) {
        for (int i = start; i < end; i++) {
            String catKey = allItems.get(i);
            String displayName;
            String iconName;
            
            if (catKey.equals("dynamic_custom")) {
                displayName = "&5Dynamic / Custom";
                iconName = "MAP";
            } else {
                displayName = module.getEntitiesConfig().getString("categories." + catKey + ".display-name", catKey);
                iconName = module.getEntitiesConfig().getString("categories." + catKey + ".icon", "PAPER");
            }
            
            ItemStack item = XMaterial.matchXMaterial(iconName).map(XMaterial::parseItem).orElse(XMaterial.PAPER.parseItem());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add(ChatColor.translateAlternateColorCodes('&', "&eClick to view entries"));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            
            setItem(i - start, item, event -> {
                new MobWhitelistGUI(plugin, module, 0, catKey).open(player);
            });
        }
    }

    private void decorateEntries(Player player, int start, int end) {
        List<String> entityWhitelist = module.getConfig().getStringList("whitelist");
        List<String> itemWhitelist = module.getConfig().getStringList("item-whitelist");

        for (int i = start; i < end; i++) {
            String name = allItems.get(i);
            
            boolean isEntity = false;
            try { 
                EntityType type = EntityType.valueOf(name); 
                if (type != EntityType.UNKNOWN) isEntity = true;
            } catch (Exception ignored) {}
            
            String configPath = isEntity ? "whitelist" : "item-whitelist";
            boolean isWhitelisted = (isEntity ? entityWhitelist : itemWhitelist).contains(name);
            
            setItem(i - start, createItemIcon(name, isWhitelisted), event -> {
                List<String> currentWhitelist = module.getConfig().getStringList(configPath);
                if (currentWhitelist.contains(name)) {
                    currentWhitelist.remove(name);
                    XSound.BLOCK_NOTE_BLOCK_PLING.play(player, 1.0f, 0.5f);
                } else {
                    currentWhitelist.add(name);
                    XSound.BLOCK_NOTE_BLOCK_PLING.play(player, 1.0f, 2.0f);
                }
                module.getConfig().set(configPath, currentWhitelist);
                module.saveConfig();
                plugin.getEntityProtectionUtils().refreshSettingsCache();
                decorate(player);
            });
        }
    }

    private ItemStack createItemIcon(String name, boolean whitelisted) {
        ItemStack item = null;
        String displayName = formatName(name);

        String customIconName = module.getEntitiesConfig().getString("categories." + selectedCategory + ".entries." + name);
        if (customIconName != null) {
            item = XMaterial.matchXMaterial(customIconName).map(XMaterial::parseItem).orElse(null);
        }

        if (item == null) {
            try {
                EntityType type = EntityType.valueOf(name);
                String materialName = type.name() + "_SPAWN_EGG";
                item = XMaterial.matchXMaterial(materialName).map(XMaterial::parseItem)
                        .orElseGet(() -> {
                            String typeName = type.name();
                            if (typeName.contains("DISPLAY")) return XMaterial.GLOW_ITEM_FRAME.parseItem();
                            if (typeName.equals("ARMOR_STAND")) return XMaterial.ARMOR_STAND.parseItem();
                            if (typeName.equals("IRON_GOLEM")) return XMaterial.IRON_BLOCK.parseItem();
                            if (typeName.equals("SNOWMAN") || typeName.equals("SNOW_GOLEM")) return XMaterial.SNOW_BLOCK.parseItem();
                            if (typeName.equals("WITHER")) return XMaterial.NETHER_STAR.parseItem();
                            if (typeName.equals("ENDER_DRAGON")) return XMaterial.DRAGON_EGG.parseItem();
                            if (typeName.equals("MOOSHROOM") || typeName.equals("MUSHROOM_COW")) return XMaterial.MOOSHROOM_SPAWN_EGG.parseItem();
                            if (typeName.equals("DROPPED_ITEM")) return XMaterial.CHEST.parseItem();
                            if (typeName.equals("EXPERIENCE_ORB")) return XMaterial.EXPERIENCE_BOTTLE.parseItem();
                            return XMaterial.PAPER.parseItem();
                        });
            } catch (Exception e) {
                item = XMaterial.matchXMaterial(name).map(XMaterial::parseItem).orElse(null);
            }
        }
        
        if (item == null) item = XMaterial.PAPER.parseItem();
        
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', (whitelisted ? "&a" : "&c") + displayName));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Whitelisted: " + (whitelisted ? "&aYes" : "&cNo")));
            lore.add("");
            lore.add(ChatColor.translateAlternateColorCodes('&', "&eClick to toggle"));
            meta.setLore(lore);
            
            if (whitelisted) {
                meta.addEnchant(Enchantment.DURABILITY, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createNavigationItem(String name, XMaterial material) {
        ItemStack item = material.parseItem();
        if (item != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
                item.setItemMeta(meta);
            }
        }
        return item;
    }

    private String formatName(String name) {
        return Arrays.stream(name.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    private Set<String> getCategorizedItems() {
        Set<String> categorized = new HashSet<>();
        ConfigurationSection categories = module.getEntitiesConfig().getConfigurationSection("categories");
        if (categories != null) {
            for (String catKey : categories.getKeys(false)) {
                ConfigurationSection entries = categories.getConfigurationSection(catKey + ".entries");
                if (entries != null) {
                    categorized.addAll(entries.getKeys(false));
                }
            }
        }
        return categorized;
    }

    private List<String> getDynamicItems() {
        List<String> entityWhitelist = module.getConfig().getStringList("whitelist");
        List<String> itemWhitelist = module.getConfig().getStringList("item-whitelist");
        Set<String> categorized = getCategorizedItems();

        List<String> dynamic = new ArrayList<>();
        for (String item : entityWhitelist) {
            if (!categorized.contains(item)) dynamic.add(item);
        }
        for (String item : itemWhitelist) {
            if (!categorized.contains(item)) dynamic.add(item);
        }
        return dynamic;
    }
}
