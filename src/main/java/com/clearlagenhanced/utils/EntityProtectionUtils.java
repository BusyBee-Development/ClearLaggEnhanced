package com.clearlagenhanced.utils;

import com.clearlagenhanced.ClearLaggEnhanced;
<<<<<<< HEAD
import com.clearlagenhanced.hooks.ModernShowcaseHook;
import com.clearlagenhanced.managers.ConfigManager;
import com.clearlagenhanced.managers.StackerManager;
=======
import com.clearlagenhanced.core.module.Module;
import com.clearlagenhanced.managers.StackerManager;
import com.clearlagenhanced.modules.integrations.modernshowcase.ModernShowcaseHook;
import com.clearlagenhanced.modules.integrations.modernshowcase.ModernShowcaseIntegration;
>>>>>>> dev
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
<<<<<<< HEAD

import java.util.List;
=======
import org.jetbrains.annotations.Nullable;
>>>>>>> dev

public class EntityProtectionUtils {

    public static final NamespacedKey BRED_KEY = new NamespacedKey(ClearLaggEnhanced.getInstance(), "cle_bred");
    
    private final ClearLaggEnhanced plugin;
<<<<<<< HEAD
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
=======
    private final StackerManager stackerManager;

    public EntityProtectionUtils(ClearLaggEnhanced plugin, StackerManager stackerManager) {
        this.plugin = plugin;
        this.stackerManager = stackerManager;
    }

    public boolean isProtected(@NotNull Entity entity) {
        Module module = plugin.getModuleManager().getModule("entity-clearing");
        if (module == null || !module.isEnabled()) return false;
        
        ProtectionSettings settings = ProtectionSettings.fromConfig(module.getConfig());
        
        ModernShowcaseHook msHook = null;
        if (settings.modernShowcase()) {
            Module msModule = plugin.getModuleManager().getModule("modernshowcase");
            if (msModule != null && msModule.isEnabled()) {
                msHook = ((ModernShowcaseIntegration) msModule).getHook();
            }
        }
        
        return isProtected(entity, settings, msHook);
    }

    public boolean isProtected(@NotNull Entity entity, @NotNull ProtectionSettings settings, @Nullable ModernShowcaseHook msHook) {
        if (entity instanceof Player) return true;

        if (settings.modernShowcase() && msHook != null) {
            if (msHook.isShowcaseEntity(entity)) return true;
        }

        if (settings.mobsInBoats()) {
            if (entity.getVehicle() instanceof Boat) return true;
        }

        if (settings.protectNamed()) {
>>>>>>> dev
            if (entity.getCustomName() != null && !entity.getCustomName().isEmpty()) return true;
            
            if (entity instanceof Item item) {
                ItemStack stack = item.getItemStack();
                if (stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()) return true;
            }
        }

<<<<<<< HEAD
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
=======
        if (settings.protectTamed() && entity instanceof Tameable tameable) {
            if (tameable.isTamed()) return true;
        }

        if (settings.mobsFromBreeding()) {
            if (entity.getPersistentDataContainer().has(BRED_KEY, PersistentDataType.BYTE)) return true;
        }

        if (settings.petsModule()) {
            if (entity.hasMetadata("Pet") || entity.hasMetadata("isPet") || entity.hasMetadata("MyPet")) return true;
        }

        if (settings.playerHeads() && entity instanceof Item item) {
            if (item.getItemStack().getType() == Material.PLAYER_HEAD) return true;
        }

        if (settings.protectArmored() && entity instanceof LivingEntity living) {
            if (hasArmor(living)) return true;
        }

        if (settings.whitelist().contains(entity.getType().name())) return true;

        if (entity instanceof Item item) {
            if (settings.itemWhitelist().contains(item.getItemStack().getType().name())) return true;
        }

        if (settings.whitelistAllMobs() && entity instanceof LivingEntity) return true;

        if (settings.protectStacked()) {
>>>>>>> dev
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
