package com.clearlagenhanced.utils;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.core.module.Module;
import com.clearlagenhanced.core.module.ModuleManager;
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

import java.util.Set;

public class EntityProtectionUtils {

    public static final NamespacedKey BRED_KEY = new NamespacedKey(ClearLaggEnhanced.getInstance(), "cle_bred");
    
    private final ClearLaggEnhanced plugin;
    private final StackerManager stackerManager;
    private volatile ProtectionSettings cachedSettings = ProtectionSettings.DEFAULTS;

    public EntityProtectionUtils(ClearLaggEnhanced plugin, StackerManager stackerManager) {
        this.plugin = plugin;
        this.stackerManager = stackerManager;
    }

    public boolean isProtected(@NotNull Entity entity) {
        ProtectionContext context = createProtectionContext();
        return context != null && isProtected(entity, context);
    }

    public @Nullable ProtectionContext createProtectionContext() {
        ModuleManager moduleManager = plugin.getModuleManager();
        if (moduleManager == null) {
            return null;
        }

        Module module = moduleManager.getModule("entity-clearing");
        if (module == null || !module.isEnabled()) {
            return null;
        }

        ProtectionSettings settings = cachedSettings;
        ModernShowcaseHook msHook = null;
        if (settings.modernShowcase()) {
            Module msModule = moduleManager.getModule("modernshowcase");
            if (msModule != null && msModule.isEnabled()) {
                msHook = ((ModernShowcaseIntegration) msModule).getHook();
            }
        }

        return new ProtectionContext(settings, msHook);
    }

    public void refreshSettingsCache() {
        ModuleManager moduleManager = plugin.getModuleManager();
        if (moduleManager == null) {
            cachedSettings = ProtectionSettings.DEFAULTS;
            return;
        }

        Module module = moduleManager.getModule("entity-clearing");
        if (module == null || module.getConfig() == null) {
            cachedSettings = ProtectionSettings.DEFAULTS;
            return;
        }

        cachedSettings = ProtectionSettings.fromConfig(module.getConfig());
    }

    public boolean isProtected(@NotNull Entity entity, @NotNull ProtectionContext context) {
        return isProtected(entity, context.settings(), context.modernShowcaseHook());
    }

    public boolean isProtected(@NotNull Entity entity, @NotNull ProtectionSettings settings, @Nullable ModernShowcaseHook msHook) {
        if (entity instanceof Player) return true;

        if (hasProtectedTag(entity, settings.protectedEntityTags())) {
            return true;
        }

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

        if (settings.citizensSupport()) {
            if (entity.hasMetadata("NPC")) return true;
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

    private boolean hasProtectedTag(@NotNull Entity entity, @NotNull Set<String> protectedEntityTags) {
        if (protectedEntityTags.isEmpty()) {
            return false;
        }

        for (String scoreboardTag : entity.getScoreboardTags()) {
            if (protectedEntityTags.contains(scoreboardTag)) {
                return true;
            }
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

    public record ProtectionContext(@NotNull ProtectionSettings settings, @Nullable ModernShowcaseHook modernShowcaseHook) {
    }
}
