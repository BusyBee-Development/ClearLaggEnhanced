package net.busybee.clearlaggenhanced.managers;

import net.busybee.clearlaggenhanced.ClearLaggEnhanced;
import net.busybee.clearlaggenhanced.core.Module;
import net.busybee.clearlaggenhanced.models.ProtectionSettings;
import net.busybee.clearlaggenhanced.modules.entityclearing.EntityClearingModule;
import net.busybee.clearlaggenhanced.modules.integrations.itemsadder.ItemsAdderHook;
import net.busybee.clearlaggenhanced.modules.integrations.modernshowcase.ModernShowcaseHook;
import net.busybee.clearlaggenhanced.modules.integrations.modernshowcase.ModernShowcaseIntegration;
import net.busybee.clearlaggenhanced.modules.integrations.griefprevention3d.GriefPrevention3DHook;
import net.busybee.clearlaggenhanced.modules.integrations.griefprevention3d.GriefPrevention3DIntegration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;
import io.papermc.paper.entity.Bucketable;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EntityProtectionUtils {
    public static final NamespacedKey BRED_KEY = new NamespacedKey("clearlaggenhanced", "bred");
    public static final String PROTECTED_TAG = "CLE_PROTECTED";
    private static final List<EquipmentSlot> SADDLE_SLOTS = resolveSaddleSlots();

    private final ClearLaggEnhanced plugin;
    private final StackerManager stackerManager;
    private final ItemsAdderHook itemsAdderHook;
    private ProtectionSettings cachedSettings = ProtectionSettings.DEFAULTS;

    public EntityProtectionUtils(ClearLaggEnhanced plugin, StackerManager stackerManager) {
        this.plugin = plugin;
        this.stackerManager = stackerManager;
        this.itemsAdderHook = new ItemsAdderHook(plugin.getLogger());
    }

    public ProtectionContext createProtectionContext() {
        ProtectionSettings settings = cachedSettings;
        ModernShowcaseHook msHook = null;
        GriefPrevention3DHook gp3dHook = null;

        ModuleManager moduleManager = plugin.getModuleManager();
        if (moduleManager == null) {
            return new ProtectionContext(settings, null, null);
        }

        if (settings.modernShowcase()) {
            Module msModule = moduleManager.getModule("modernshowcase");
            if (msModule != null && msModule.isEnabled()) {
                msHook = ((ModernShowcaseIntegration) msModule).getHook();
            }
        }

        if (settings.griefPrevention3D()) {
            Module gpModule = moduleManager.getModule("griefprevention3d");
            if (gpModule != null && gpModule.isEnabled()) {
                gp3dHook = ((GriefPrevention3DIntegration) gpModule).getHook();
            }
        }

        return new ProtectionContext(settings, msHook, gp3dHook);
    }

    public void refreshSettingsCache() {
        ModuleManager moduleManager = plugin.getModuleManager();
        if (moduleManager == null) {
            cachedSettings = ProtectionSettings.DEFAULTS;
            return;
        }

        Module module = moduleManager.getModule("entity-clearing");
        if (!(module instanceof EntityClearingModule ecModule)) {
            cachedSettings = ProtectionSettings.DEFAULTS;
            return;
        }

        // The module's config.yml is loaded whether or not the module is enabled, and the mob and misc
        // limiters read these toggles too, so the owner's settings apply with entity clearing off.
        if (ecModule.getConfig() == null) {
            cachedSettings = ProtectionSettings.DEFAULTS;
            return;
        }

        cachedSettings = ProtectionSettings.fromConfig(ecModule.getConfig());
    }

    public boolean isProtected(@NotNull Entity entity, @NotNull ProtectionContext context) {
        return isProtected(entity, context.settings(), context.modernShowcaseHook(), context.griefPrevention3DHook());
    }

    public boolean isProtected(@NotNull Entity entity, @NotNull ProtectionSettings settings, @Nullable ModernShowcaseHook msHook, @Nullable GriefPrevention3DHook gp3dHook) {
        if (entity instanceof Player) return true;

        try {
            if (plugin.getApi().getProtections().isProtected(entity)) {
                return true;
            }

            boolean isStacked = stackerManager.isStacked(entity);
            if (isStacked && settings.protectStacked()) {
                return true;
            }

            if (hasProtectedTag(entity, settings.protectedEntityTags())) {
                return true;
            }

            // With their toggle off, plugin mobs still fall through so player-ownership checks
            // (tamed, leashed, ridden, ...) and the explicit whitelist apply; only the name check and
            // the broad passive/all-mobs toggles are skipped, since these plugins give their mobs
            // custom names of their own.
            boolean unprotectedPluginMob = false;
            if (isMythicMob(entity)) {
                if (settings.mythicMobs()) return true;
                unprotectedPluginMob = true;
            }

            if (isInfernalMob(entity)) {
                if (settings.infernalMobs()) return true;
                unprotectedPluginMob = true;
            }

            if (settings.modernShowcase() && msHook != null) {
                if (msHook.isShowcaseEntity(entity)) return true;
            }

            if (settings.griefPrevention3D() && gp3dHook != null) {
                if (isPeacefulMob(entity) && gp3dHook.isInsideClaim(entity)) return true;
            }

            if (settings.oraxen() && isOraxen(entity)) return true;
            if (settings.nexo() && isNexo(entity)) return true;
            if (settings.itemsAdder() && itemsAdderHook.isItemsAdderEntity(entity)) return true;

            if (isKeptByPlayer(entity, settings, isStacked, unprotectedPluginMob)) return true;

            if (settings.protectNamed() && !isStacked && !unprotectedPluginMob && entity instanceof Item item) {
                ItemStack stack = item.getItemStack();
                if (stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()) return true;
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

            // An explicit whitelist entry always wins, plugin mobs included. The set already
            // holds each entry under every name it has across versions (LEASH_HITCH/LEASH_KNOT, ...).
            if (settings.whitelist().contains(entity.getType().name())) return true;

            if (!unprotectedPluginMob && settings.protectPassiveMobs() && isPeacefulMob(entity)) return true;

            if (entity instanceof Item item) {
                if (settings.itemWhitelist().contains(item.getItemStack().getType().name())) return true;
            }

            if (settings.whitelistAllMobs() && entity instanceof LivingEntity && !unprotectedPluginMob) return true;
        } catch (Exception e) {
            // Safety fallback for Folia/multi-threaded ConcurrentModificationExceptions
            return true;
        }

        return false;
    }

    /**
     * Mobs clears keep because a player owns or uses them: riders of boats and minecarts, leashed,
     * ridden, saddled, bucket, named, tamed, bred and armored mobs. The whitelist and the passive or
     * all-mobs toggles are not part of this; they protect by type, not by what a player did.
     */
    private boolean isKeptByPlayer(@NotNull Entity entity, @NotNull ProtectionSettings settings,
                                   boolean isStacked, boolean unprotectedPluginMob) {
        if (settings.mobsInBoats() && entity.getVehicle() instanceof Boat) return true;
        if (settings.mobsInMinecarts() && entity.getVehicle() instanceof Minecart) return true;
        if (settings.leashedMobs() && entity instanceof LivingEntity living && living.isLeashed()) return true;
        if (settings.riddenMobs() && hasPlayerPassenger(entity)) return true;
        if (settings.saddledMobs() && entity instanceof LivingEntity living && hasSaddleOrHarness(living)) return true;
        if (settings.bucketMobs() && entity instanceof Bucketable bucketable && bucketable.isFromBucket()) return true;

        // Plugin mobs and stacks carry names of their own, so a name only means "kept" on ordinary mobs.
        if (settings.protectNamed() && !isStacked && !unprotectedPluginMob && entity.customName() != null
                && (!settings.protectPersistentNamedOnly() || entity.isPersistent())) return true;

        if (settings.protectTamed() && entity instanceof Tameable tameable && tameable.isTamed()) return true;
        if (settings.mobsFromBreeding() && entity.getPersistentDataContainer().has(BRED_KEY, PersistentDataType.BYTE)) return true;
        return settings.protectArmored() && entity instanceof LivingEntity living && hasArmor(living);
    }

    public boolean isExemptFromMobLimits(@NotNull Entity entity, @NotNull ProtectionContext context) {
        return isExemptFromMobLimits(entity, context, true);
    }

    /**
     * Whether the mob limiter leaves this entity out of its per-chunk counts (and lets it spawn).
     * Entities other plugins control are always left out: players, registered protections that don't
     * count, protection tags, stacks and plugin mobs, NPCs and pets. With {@code countKeptMobs} (the
     * default) mobs that clears only keep for a player (bred, tamed, named, leashed...) still count,
     * so breeding and spawning stay capped; without it they are left out too, as before 26.10.2.
     * Whitelisted and passive mobs always count. A check that fails counts the entity, so an error can
     * never switch the limit off.
     */
    public boolean isExemptFromMobLimits(@NotNull Entity entity, @NotNull ProtectionContext context, boolean countKeptMobs) {
        if (entity instanceof Player) return true;
        ProtectionSettings settings = context.settings();

        try {
            if (plugin.getApi().getProtections().isExemptFromMobLimits(entity)) return true;
            boolean isStacked = stackerManager.isStacked(entity);
            if (settings.protectStacked() && isStacked) return true;
            if (hasProtectedTag(entity, settings.protectedEntityTags())) return true;
            boolean mythic = isMythicMob(entity);
            if (settings.mythicMobs() && mythic) return true;
            boolean infernal = isInfernalMob(entity);
            if (settings.infernalMobs() && infernal) return true;
            if (settings.citizensSupport() && entity.hasMetadata("NPC")) return true;
            if (settings.petsModule()
                    && (entity.hasMetadata("Pet") || entity.hasMetadata("isPet") || entity.hasMetadata("MyPet"))) return true;
            if (settings.modernShowcase() && context.modernShowcaseHook() != null
                    && context.modernShowcaseHook().isShowcaseEntity(entity)) return true;
            if (settings.itemsAdder() && itemsAdderHook.isItemsAdderEntity(entity, false)) return true;
            if (settings.oraxen() && isOraxen(entity)) return true;
            if (settings.nexo() && isNexo(entity)) return true;
            if (!countKeptMobs && isKeptByPlayer(entity, settings, isStacked, mythic || infernal)) return true;
        } catch (Exception e) {
            // Unlike clears, which keep what they cannot check, the limiter counts it: an error
            // that exempted every mob would switch the limit off.
            return false;
        }
        return false;
    }

    // "MythicMob" metadata is MythicMobs' own documented cross-plugin compatibility marker.
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

    // "infernalMetadata" is the actual key InfernalMobs sets; the other two are legacy/defensive fallbacks.
    private boolean isInfernalMob(@NotNull Entity entity) {
        try {
            return entity.hasMetadata("infernalMetadata") ||
                   entity.hasMetadata("InfernalMob") ||
                   entity.hasMetadata("infernalMob");
        } catch (Exception ignored) {}
        return false;
    }

    private boolean hasProtectedTag(@NotNull Entity entity, @NotNull Set<String> protectedEntityTags) {
        try {
            Set<String> tags = entity.getScoreboardTags();
            if (tags.isEmpty()) return false;

            // CLE_PROTECTED is honoured regardless of config, matching the misc entity limiter's default.
            if (tags.contains(PROTECTED_TAG)) return true;

            for (String tag : tags) {
                if (protectedEntityTags.contains(tag)) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private boolean hasPlayerPassenger(@NotNull Entity entity) {
        for (Entity passenger : entity.getPassengers()) {
            if (passenger instanceof Player) return true;
        }
        return false;
    }

    private boolean hasSaddleOrHarness(@NotNull LivingEntity living) {
        if (living instanceof Steerable steerable && steerable.hasSaddle()) return true;
        if (living instanceof AbstractHorse horse) {
            ItemStack saddle = horse.getInventory().getSaddle();
            if (saddle != null && saddle.getType() == Material.SADDLE) return true;
        }

        EntityEquipment equipment = living.getEquipment();
        if (equipment == null) return false;

        // SADDLE (1.21.5+) and BODY (1.20.5+, where the happy ghast harness goes) are resolved by name
        // because the API we compile against predates them.
        for (EquipmentSlot slot : SADDLE_SLOTS) {
            try {
                ItemStack item = equipment.getItem(slot);
                if (item == null) continue;

                String type = item.getType().name();
                if (type.equals("SADDLE") || type.endsWith("_HARNESS")) return true;
            } catch (IllegalArgumentException | UnsupportedOperationException ignored) {
                // Entity type does not support this slot.
            }
        }
        return false;
    }

    private static @NotNull List<EquipmentSlot> resolveSaddleSlots() {
        List<EquipmentSlot> slots = new ArrayList<>();
        for (String name : new String[]{"SADDLE", "BODY"}) {
            try {
                slots.add(EquipmentSlot.valueOf(name));
            } catch (IllegalArgumentException ignored) {}
        }
        return List.copyOf(slots);
    }

    private boolean hasArmor(@NotNull LivingEntity living) {
        if (living.getEquipment() == null) return false;
        for (ItemStack armor : living.getEquipment().getArmorContents()) {
            if (armor != null && armor.getType() != Material.AIR) return true;
        }
        return false;
    }

    // Enemy excludes hostile types that share a peaceful supertype (Hoglin is Animals, Shulker is Golem).
    private boolean isPeacefulMob(@NotNull Entity entity) {
        if (entity instanceof Enemy) return false;
        return entity instanceof Animals ||
               entity instanceof NPC ||
               entity instanceof Ambient ||
               entity instanceof WaterMob ||
               entity instanceof AbstractVillager ||
               entity instanceof Golem ||
               entity instanceof Allay;
    }

    // Best-effort PDC key guesses, unverified against current plugin internals — not a confirmed
    // API convention like isMythicMob/isInfernalMob. Most Oraxen/Nexo furniture now uses Display
    // entities anyway, which are already protected globally via the whitelist. ItemsAdder is asked
    // through its own API instead (see ItemsAdderHook).
    private boolean isOraxen(@NotNull Entity entity) {
        try {
            return entity.getPersistentDataContainer().has(new NamespacedKey("oraxen", "id"), PersistentDataType.STRING);
        } catch (Exception ignored) {}
        return false;
    }

    private boolean isNexo(@NotNull Entity entity) {
        try {
            return entity.getPersistentDataContainer().has(new NamespacedKey("nexo", "id"), PersistentDataType.STRING);
        } catch (Exception ignored) {}
        return false;
    }

    public record ProtectionContext(ProtectionSettings settings, @Nullable ModernShowcaseHook modernShowcaseHook, @Nullable GriefPrevention3DHook griefPrevention3DHook) {}
}
