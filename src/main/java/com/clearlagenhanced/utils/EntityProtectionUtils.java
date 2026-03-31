package com.clearlagenhanced.utils;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.core.module.Module;
import com.clearlagenhanced.managers.StackerManager;
import com.clearlagenhanced.modules.integrations.modernshowcase.ModernShowcaseHook;
import com.clearlagenhanced.modules.integrations.modernshowcase.ModernShowcaseIntegration;
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
    private final StackerManager stackerManager;

    public EntityProtectionUtils(ClearLaggEnhanced plugin) {
        this.plugin = plugin;
        this.stackerManager = plugin.getStackerManager();
    }

    public boolean isProtected(@NotNull Entity entity) {
        if (entity instanceof Player) return true;

        Module module = plugin.getModuleManager().getModule("entity-clearing");
        if (module == null || !module.isEnabled()) return false;
        org.bukkit.configuration.file.FileConfiguration config = module.getConfig();

        if (config.getBoolean("extra-protections.modern-showcase", true)) {
            Module msModule = plugin.getModuleManager().getModule("modern-showcase");
            if (msModule != null && msModule.isEnabled()) {
                ModernShowcaseIntegration integration = (ModernShowcaseIntegration) msModule;
                ModernShowcaseHook hook = integration.getHook();
                if (hook != null && hook.isShowcaseEntity(entity)) return true;
            }
        }

        if (config.getBoolean("extra-protections.mobs-in-boats", true)) {
            if (entity.getVehicle() instanceof Boat) return true;
        }

        if (config.getBoolean("protect-named-entities", true)) {
            if (entity.getCustomName() != null && !entity.getCustomName().isEmpty()) return true;
            
            if (entity instanceof Item item) {
                ItemStack stack = item.getItemStack();
                if (stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()) return true;
            }
        }

        if (config.getBoolean("protect-tamed-entities", true) && entity instanceof Tameable tameable) {
            if (tameable.isTamed()) return true;
        }

        if (config.getBoolean("extra-protections.mobs-from-breeding", true)) {
            if (entity.getPersistentDataContainer().has(BRED_KEY, PersistentDataType.BYTE)) return true;
        }

        if (config.getBoolean("extra-protections.pets-module", true)) {
            if (entity.hasMetadata("Pet") || entity.hasMetadata("isPet") || entity.hasMetadata("MyPet")) return true;
        }

        if (config.getBoolean("extra-protections.player-heads", true) && entity instanceof Item item) {
            if (item.getItemStack().getType() == Material.PLAYER_HEAD) return true;
        }

        if (config.getBoolean("protect-armored-entities", false) && entity instanceof LivingEntity living) {
            if (hasArmor(living)) return true;
        }

        List<String> whitelist = config.getStringList("whitelist");
        if (whitelist.contains(entity.getType().name())) return true;

        if (entity instanceof Item item) {
            List<String> itemWhitelist = config.getStringList("item-whitelist");
            if (itemWhitelist.contains(item.getItemStack().getType().name())) return true;
        }

        if (config.getBoolean("whitelist-all-mobs", false) && entity instanceof LivingEntity) return true;

        if (config.getBoolean("protect-stacked-entities", false)) {
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