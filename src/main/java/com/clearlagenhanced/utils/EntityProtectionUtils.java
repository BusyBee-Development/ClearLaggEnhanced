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
import org.jetbrains.annotations.Nullable;

public class EntityProtectionUtils {

    public static final NamespacedKey BRED_KEY = new NamespacedKey(ClearLaggEnhanced.getInstance(), "cle_bred");
    
    private final ClearLaggEnhanced plugin;
    private final StackerManager stackerManager;

    public EntityProtectionUtils(ClearLaggEnhanced plugin) {
        this.plugin = plugin;
        this.stackerManager = plugin.getStackerManager();
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
            if (entity.getCustomName() != null && !entity.getCustomName().isEmpty()) return true;
            
            if (entity instanceof Item item) {
                ItemStack stack = item.getItemStack();
                if (stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()) return true;
            }
        }

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
