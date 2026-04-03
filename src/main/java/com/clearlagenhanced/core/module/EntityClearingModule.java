package com.clearlagenhanced.core.module;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.modules.entityclearing.inventory.EntityClearingGUI;
import com.clearlagenhanced.modules.entityclearing.listeners.BreedingListener;
import com.clearlagenhanced.modules.entityclearing.models.AdaptiveIntervalSettings;
import com.clearlagenhanced.modules.entityclearing.models.EntityManager;
import com.clearlagenhanced.modules.entityclearing.models.NotificationManager;
import com.clearlagenhanced.modules.entityclearing.tasks.AutoClearTask;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class EntityClearingModule extends Module {
    private final ClearLaggEnhanced plugin;
    @Getter private EntityManager entityManager;
    @Getter private NotificationManager notificationManager;
    private BreedingListener breedingListener;
    private AutoClearTask autoClearTask;

    public EntityClearingModule(ClearLaggEnhanced plugin) {
        super("Entity Clearing", "entity-clearing");
        this.plugin = plugin;
    }

    @Override
    public void onEnable() {
        notificationManager = new NotificationManager(plugin, this);
        entityManager = new EntityManager(plugin, this);
        plugin.getEntityProtectionUtils().refreshSettingsCache();

        int clearInterval = getInt("interval", 300);
        if (clearInterval <= 0) {
            clearInterval = 300;
            plugin.getLogger().warning("interval was <= 0; defaulting to 300.");
        }

        AdaptiveIntervalSettings adaptiveIntervalSettings =
                AdaptiveIntervalSettings.fromConfig(getConfig().getConfigurationSection("adaptive-interval"), plugin.getLogger());

        autoClearTask = new AutoClearTask(
                plugin,
                entityManager,
                notificationManager,
                clearInterval,
                adaptiveIntervalSettings,
                getStringList("worlds")
        );
        autoClearTask.start();

        if (getBoolean("extra-protections.mobs-from-breeding", true)) {
            breedingListener = new BreedingListener(plugin);
            Bukkit.getPluginManager().registerEvents(breedingListener, plugin);
        }
        
        registerGUI("entity-clearing", "Entity Clearing", "IRON_SWORD", () -> new EntityClearingGUI(plugin, this));
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
        
        unregisterGUI("entity-clearing");
    }

    @Override
    public void onReload() {
        onDisable();
        onEnable();
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
