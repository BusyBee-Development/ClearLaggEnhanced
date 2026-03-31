package com.clearlagenhanced.utils;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.hooks.ModernShowcaseHook;
import com.clearlagenhanced.managers.ConfigManager;
import com.clearlagenhanced.managers.StackerManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EntityProtectionUtils {

    public static final NamespacedKey BRED_KEY = new NamespacedKey(ClearLaggEnhanced.getInstance(), "cle_bred");
    
    private final ClearLaggEnhanced plugin;
    private final ConfigManager config;
    private final ModernShowcaseHook modernShowcaseHook;
    private final StackerManager stackerManager;

    public EntityProtectionUtils(ClearLaggEnhanced plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.modernShowcaseHook = new ModernShowcaseHook();
        this.stackerManager = plugin.getStackerManager();
    }

    public boolean isProtected(@NotNull Entity entity) {
        if (entity instanceof Player) return true;

        // Modern Showcase Protection
        if (config.getBoolean("entity-clearing.extra-protections.modern-showcase", true)) {
            if (modernShowcaseHook.isShowcaseEntity(entity)) return true;
        }

        // Vehicle Check (Mobs in Boats)
        if (config.getBoolean("entity-clearing.extra-protections.mobs-in-boats", true)) {
            if (entity.getVehicle() instanceof Boat) return true;
        }

        // Named Entity Protection
        if (config.getBoolean("entity-clearing.protect-named-entities", true)) {
            if (entity.getCustomName() != null && !entity.getCustomName().isEmpty()) return true;
            
            if (entity instanceof Item item) {
                ItemStack stack = item.getItemStack();
                if (stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()) return true;
            }
        }

        // Tamed Entity Protection
        if (config.getBoolean("entity-clearing.protect-tamed-entities", true) && entity instanceof Tameable tameable) {
            if (tameable.isTamed()) return true;
        }

        // Breeding Protection
        if (config.getBoolean("entity-clearing.extra-protections.mobs-from-breeding", true)) {
            if (entity.getPersistentDataContainer().has(BRED_KEY, PersistentDataType.BYTE)) return true;
        }

        // Pets Module Protection (Metadata check for common pet plugins)
        if (config.getBoolean("entity-clearing.extra-protections.pets-module", true)) {
            if (entity.hasMetadata("Pet") || entity.hasMetadata("isPet") || entity.hasMetadata("MyPet")) return true;
        }

        // Player Heads Protection
        if (config.getBoolean("entity-clearing.extra-protections.player-heads", true) && entity instanceof Item item) {
            if (item.getItemStack().getType() == Material.PLAYER_HEAD) return true;
        }

        // Armored Entity Protection
        if (config.getBoolean("entity-clearing.protect-armored-entities", false) && entity instanceof LivingEntity living) {
            if (hasArmor(living)) return true;
        }

        // Whitelist Check
        List<String> whitelist = config.getStringList("entity-clearing.whitelist");
        if (whitelist.contains(entity.getType().name())) return true;

        // Item Whitelist Check
        if (entity instanceof Item item) {
            List<String> itemWhitelist = config.getStringList("entity-clearing.item-whitelist");
            if (itemWhitelist.contains(item.getItemStack().getType().name())) return true;
        }

        // Whitelist All Mobs Check
        if (config.getBoolean("entity-clearing.whitelist-all-mobs", false) && entity instanceof LivingEntity) return true;

        // Stacked Entity Protection
        if (config.getBoolean("entity-clearing.protect-stacked-entities", false)) {
            if (stackerManager.isStacked(entity)) return true;
        }

        return false;
    }

    private boolean hasArmor(LivingEntity living) {
        EntityEquipment equipment = living.getEquipment();
        if (equipment == null) return false;
        for (ItemStack armor : equipment.getArmorContents()) {
            if (armor != null && armor.getType() != Material.AIR) return true;
        }
        return false;
    }
}
