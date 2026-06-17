package net.busybee.clearlaggenhanced.managers;

import net.busybee.clearlaggenhanced.ClearLaggEnhanced;
import net.busybee.clearlaggenhanced.core.Module;
import net.busybee.clearlaggenhanced.models.ProtectionSettings;
import net.busybee.clearlaggenhanced.modules.integrations.modernshowcase.ModernShowcaseHook;
import net.busybee.clearlaggenhanced.modules.integrations.modernshowcase.ModernShowcaseIntegration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class EntityProtectionUtils {
    public static final NamespacedKey BRED_KEY = new NamespacedKey("clearlaggenhanced", "bred");

    private final ClearLaggEnhanced plugin;
    private final StackerManager stackerManager;
    private ProtectionSettings cachedSettings = ProtectionSettings.DEFAULTS;

    public EntityProtectionUtils(ClearLaggEnhanced plugin, StackerManager stackerManager) {
        this.plugin = plugin;
        this.stackerManager = stackerManager;
    }

    public ProtectionContext createProtectionContext() {
        ProtectionSettings settings = cachedSettings;
        ModernShowcaseHook msHook = null;

        ModuleManager moduleManager = plugin.getModuleManager();
        if (moduleManager == null) {
            return new ProtectionContext(settings, null);
        }

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
        if (!(module instanceof net.busybee.clearlaggenhanced.modules.entityclearing.EntityClearingModule ecModule)) {
            cachedSettings = ProtectionSettings.DEFAULTS;
            return;
        }

        if (ecModule.getConfig() == null || ecModule.getEntitiesConfig() == null) {
            cachedSettings = ProtectionSettings.DEFAULTS;
            return;
        }

        cachedSettings = ProtectionSettings.fromConfig(ecModule.getConfig(), ecModule.getEntitiesConfig());
    }

    public boolean isProtected(@NotNull Entity entity, @NotNull ProtectionContext context) {
        return isProtected(entity, context.settings(), context.modernShowcaseHook(), true);
    }

    public boolean isProtected(@NotNull Entity entity, @NotNull ProtectionContext context, boolean checkWhitelist) {
        return isProtected(entity, context.settings(), context.modernShowcaseHook(), checkWhitelist);
    }

    public boolean isProtected(@NotNull Entity entity, @NotNull ProtectionSettings settings, @Nullable ModernShowcaseHook msHook) {
        return isProtected(entity, settings, msHook, true);
    }

    public boolean isProtected(@NotNull Entity entity, @NotNull ProtectionSettings settings, @Nullable ModernShowcaseHook msHook, boolean checkWhitelist) {
        if (entity instanceof Player) return true;

        try {
            boolean isStacked = stackerManager.isStacked(entity);
            if (isStacked && settings.protectStacked()) {
                return true;
            }

            if (hasProtectedTag(entity, settings.protectedEntityTags())) {
                return true;
            }

            if (isMythicMob(entity)) {
                return settings.mythicMobs();
            }

            if (isInfernalMob(entity)) {
                return settings.infernalMobs();
            }

            if (isModDexMob(entity)) {
                return settings.modDex();
            }

            if (settings.modernShowcase() && msHook != null) {
                if (msHook.isShowcaseEntity(entity)) return true;
            }

            if (settings.mobsInBoats()) {
                if (entity.getVehicle() instanceof Boat) return true;
            }

            if (settings.protectNamed() && !isStacked) {
                if (entity.customName() != null) {
                    if (!settings.protectPersistentNamedOnly() || entity.isPersistent()) {
                        return true;
                    }
                }

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

            if (checkWhitelist && settings.whitelist().contains(entity.getType().name())) return true;

            if (entity instanceof Item item) {
                if (settings.itemWhitelist().contains(item.getItemStack().getType().name())) return true;
            }

            if (settings.whitelistAllMobs() && entity instanceof LivingEntity) return true;
        } catch (Exception e) {
            // Safety fallback for Folia/multi-threaded ConcurrentModificationExceptions
            return true;
        }

        return false;
    }

    private boolean isMythicMob(@NotNull Entity entity) {
        try {
            if (entity.hasMetadata("MythicMob")) {
                for (MetadataValue value : entity.getMetadata("MythicMob")) {
                    if (value.asBoolean()) return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private boolean isInfernalMob(@NotNull Entity entity) {
        try {
            return entity.hasMetadata("infernalMetadata") || 
                   entity.hasMetadata("InfernalMob") || 
                   entity.hasMetadata("infernalMob");
        } catch (Exception ignored) {}
        return false;
    }

    private boolean isModDexMob(@NotNull Entity entity) {
        try {
            if (entity.hasMetadata("moddex") || entity.hasMetadata("ModDex") ||
                entity.hasMetadata("RareMob") || entity.hasMetadata("RareMobDiscovered")) {
                return true;
            }

            for (NamespacedKey key : entity.getPersistentDataContainer().getKeys()) {
                if (key.getNamespace().equalsIgnoreCase("moddex") ||
                    key.getNamespace().equalsIgnoreCase("mobdex")) {
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private boolean hasProtectedTag(@NotNull Entity entity, @NotNull Set<String> protectedEntityTags) {
        if (protectedEntityTags.isEmpty()) return false;
        try {
            Set<String> tags = entity.getScoreboardTags();
            if (tags.isEmpty()) return false;

            for (String tag : tags) {
                if (protectedEntityTags.contains(tag)) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private boolean hasArmor(@NotNull LivingEntity living) {
        if (living.getEquipment() == null) return false;
        for (ItemStack armor : living.getEquipment().getArmorContents()) {
            if (armor != null && armor.getType() != Material.AIR) return true;
        }
        return false;
    }

    public record ProtectionContext(ProtectionSettings settings, @Nullable ModernShowcaseHook modernShowcaseHook) {}
}
