package net.busybee.clearlaggenhanced.modules.entityclearing;

import net.busybee.clearlaggenhanced.core.Module;
import net.busybee.clearlaggenhanced.ClearLaggEnhanced;
import net.busybee.clearlaggenhanced.modules.entityclearing.inventory.EntityClearingGUI;
import net.busybee.clearlaggenhanced.modules.entityclearing.listeners.BreedingListener;
import net.busybee.clearlaggenhanced.modules.entityclearing.models.AdaptiveIntervalSettings;
import net.busybee.clearlaggenhanced.modules.entityclearing.models.EntityManager;
import net.busybee.clearlaggenhanced.modules.entityclearing.models.NotificationManager;
import net.busybee.clearlaggenhanced.modules.entityclearing.models.PerformanceGateSettings;
import net.busybee.clearlaggenhanced.modules.entityclearing.tasks.AutoClearTask;
import net.busybee.clearlaggenhanced.utils.EntityTypeNames;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.HandlerList;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class EntityClearingModule extends Module {
    private final ClearLaggEnhanced plugin;
    @Getter private EntityManager entityManager;
    @Getter private NotificationManager notificationManager;
    @Getter private FileConfiguration entitiesConfig;
    private File entitiesFile;
    private BreedingListener breedingListener;
    private AutoClearTask autoClearTask;

    public EntityClearingModule(ClearLaggEnhanced plugin) {
        super("Entity Clearing", "entity-clearing");
        this.plugin = plugin;
    }

    @Override
    public void onRegister() {
        registerGUI("entity-clearing", "Entity Clearing", "IRON_SWORD", () -> new EntityClearingGUI(plugin, this));
    }

    @Override
    public void onEnable() {
        loadEntitiesConfig();
        notificationManager = new NotificationManager(this);
        entityManager = new EntityManager(plugin, this);
        plugin.getEntityProtectionUtils().refreshSettingsCache();
        warnUnknownWhitelistEntries();

        int clearInterval = getInt("interval", 300);
        if (clearInterval <= 0) {
            clearInterval = 300;
            plugin.getLogger().warning("interval was <= 0; defaulting to 300.");
        }

        AdaptiveIntervalSettings adaptiveIntervalSettings =
                AdaptiveIntervalSettings.fromConfig(getConfig().getConfigurationSection("adaptive-interval"), plugin.getLogger());
        PerformanceGateSettings performanceGateSettings =
                PerformanceGateSettings.fromConfig(getConfig().getConfigurationSection("performance-gate"), plugin.getLogger());

        autoClearTask = new AutoClearTask(
                plugin,
                entityManager,
                notificationManager,
                clearInterval,
                adaptiveIntervalSettings,
                performanceGateSettings,
                getStringList("worlds")
        );

        // The whitelist in memory is the last working copy, which lacks whatever the owner was
        // adding when the mistake went in, so nothing is cleared without someone asking for it.
        if (isConfigOnWorkingCopy()) {
            plugin.getLogger().severe("Automatic entity clearing is on hold until module/entity-clearing/config.yml is fixed and /lagg reload is run. /lagg clear still works.");
        } else {
            autoClearTask.start();
        }

        if (getBoolean("extra-protections.mobs-from-breeding", true)) {
            breedingListener = new BreedingListener(plugin);
            Bukkit.getPluginManager().registerEvents(breedingListener, plugin);
        }
    }

    @Override
    public void onDisable() {
        if (autoClearTask != null) {
            autoClearTask.stop();
        }
        if (entityManager != null) {
            entityManager.shutdown();
        }
        if (notificationManager != null) {
            notificationManager.shutdown();
        }
        if (breedingListener != null) {
            HandlerList.unregisterAll(breedingListener);
        }
    }

    // Entries are matched by name, so one that is neither an entity type nor a material silently protects nothing.
    private void warnUnknownWhitelistEntries() {
        warnUnknownEntries("whitelist");
        warnUnknownEntries("item-whitelist");
    }

    private void warnUnknownEntries(String path) {
        List<String> unknown = new ArrayList<>();
        for (String entry : getStringList(path)) {
            String name = entry == null ? "" : entry.trim().toUpperCase(Locale.ROOT);
            if (name.isEmpty()) {
                continue;
            }

            // Either list accepts both kinds of name (see ProtectionSettings).
            if (EntityTypeNames.resolve(name) == null && Material.getMaterial(name) == null) {
                unknown.add(entry);
            }
        }

        if (!unknown.isEmpty()) {
            plugin.getLogger().warning("Entity clearing " + path + " has entries that are not entity types or materials on this server version and will protect nothing: "
                    + String.join(", ", unknown));
        }
    }

    public void loadEntitiesConfig() {
        File modFolder = new File(new File(plugin.getDataFolder(), "module"), getFolderName());
        if (!modFolder.exists()) {
            modFolder.mkdirs();
        }
        entitiesFile = new File(modFolder, "entities.yml");
        if (!entitiesFile.exists()) {
            plugin.saveResource("module/" + getFolderName() + "/entities.yml", false);
        }
        entitiesConfig = YamlConfiguration.loadConfiguration(entitiesFile);
    }

    public void saveEntitiesConfig() {
        if (entitiesConfig == null || entitiesFile == null) return;
        try {
            entitiesConfig.save(entitiesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save entities config: " + e.getMessage());
        }
    }

    public void onReload() {
        onDisable();
        onEnable();
    }

    @Override
    public boolean canRunOnDefaults() {
        return false;
    }
    
    public long getTimeUntilNextClear() {
        if (autoClearTask == null || autoClearTask.getTask() == null) {
            return -1;
        }
        
        return autoClearTask.getRemainingTime();
    }
    
    public String getFormattedTimeUntilNextClear() {
        long seconds = getTimeUntilNextClear();
        if (seconds == -1) {
            return "Disabled";
        }
        
        long hours = TimeUnit.SECONDS.toHours(seconds);
        long minutes = TimeUnit.SECONDS.toMinutes(seconds % 3600);
        long secs = seconds % 60;
        
        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0) {
            sb.append(minutes).append("m ");
        }
        sb.append(secs).append("s");
        
        return sb.toString().trim();
    }
    
    public int clearEntities() {
        if (entityManager != null) {
            return entityManager.clearEntities();
        }
        return 0;
    }

    public AutoClearTask.StatusSnapshot getStatusSnapshot() {
        return autoClearTask != null ? autoClearTask.getStatusSnapshot() : null;
    }
}
