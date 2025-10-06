package com.clearlagenhanced;

import com.clearlagenhanced.commands.LaggCommand;
import com.clearlagenhanced.database.DatabaseManager;
import com.clearlagenhanced.listeners.HopperLimiterListener;
import com.clearlagenhanced.listeners.MiscEntityLimiterListener;
import com.clearlagenhanced.listeners.MobLimiterListener;
import com.clearlagenhanced.listeners.RedstoneLimiterListener;
import com.clearlagenhanced.listeners.SpawnerLimiterListener;
import com.clearlagenhanced.managers.ConfigManager;
import com.clearlagenhanced.managers.EntityManager;
import com.clearlagenhanced.managers.GUIManager;
import com.clearlagenhanced.managers.LagPreventionManager;
import com.clearlagenhanced.managers.MessageManager;
import com.clearlagenhanced.managers.MiscEntitySweepService;
import com.clearlagenhanced.managers.NotificationManager;
import com.clearlagenhanced.managers.PerformanceManager;
import com.clearlagenhanced.utils.MessageUtils;
import com.clearlagenhanced.utils.VersionCheck;
import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.impl.PlatformScheduler;
import lombok.Getter;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

public class ClearLaggEnhanced extends JavaPlugin {

    @Getter
    private static ClearLaggEnhanced instance;

    private static PlatformScheduler scheduler;

    @Getter private DatabaseManager databaseManager;
    @Getter private ConfigManager configManager;
    @Getter private MessageManager messageManager;
    @Getter private EntityManager entityManager;
    @Getter private LagPreventionManager lagPreventionManager;
    @Getter private PerformanceManager performanceManager;
    @Getter private NotificationManager notificationManager;
    private GUIManager guiManager;

    private MiscEntitySweepService miscSweep;

    public static PlatformScheduler scheduler() {
      return scheduler;
    }

    @Override
    public void onEnable() {
        instance = this;

        FoliaLib foliaLib = new FoliaLib(this);
        scheduler = foliaLib.getScheduler();

        saveDefaultConfig();
        initializeManagers();
        registerCommands();
        registerListeners();
        startMiscLimiterIfEnabled();

        getLogger().info("ClearLaggEnhanced has been enabled!");
    }

    @Override
    public void onDisable() {
        closeQuietlyDatabase();
        shutdown(entityManager);
        shutdown(guiManager);
        stopMiscLimiterIfRunning();

        getLogger().info("ClearLaggEnhanced has been disabled!");
    }

    public GUIManager getGUIManager() {
        return guiManager;
    }

    public void reloadAll() {
        HandlerList.unregisterAll(this);

        shutdown(entityManager);
        shutdown(guiManager);
        stopMiscLimiterIfRunning();

        if (configManager != null) {
            configManager.reload();
        }

        if (messageManager == null) {
            messageManager = new MessageManager(this);
        } else {
            messageManager.reload();
        }

        MessageUtils.initialize(messageManager);

        entityManager = new EntityManager(this);
        lagPreventionManager = new LagPreventionManager(this);
        performanceManager = new PerformanceManager(this);
        notificationManager = new NotificationManager(this);
        guiManager = new GUIManager(this);

        registerListeners();
        final HopperLimiterListener hopperListener = new HopperLimiterListener(this);
        getServer().getPluginManager().registerEvents(hopperListener, this);
        try {
            hopperListener.rescanLoadedChunks();
        } catch (NoSuchMethodError | Exception ignored) {
        }

        startMiscLimiterIfEnabled();

        getServer().getPluginManager().registerEvents(new VersionCheck(this), this);
    }

    private void initializeManagers() {
        configManager = new ConfigManager(this);
        messageManager = new MessageManager(this);
        databaseManager = new DatabaseManager(this);
        entityManager = new EntityManager(this);
        lagPreventionManager = new LagPreventionManager(this);
        performanceManager = new PerformanceManager(this);
        notificationManager = new NotificationManager(this);
        guiManager = new GUIManager(this);

        MessageUtils.initialize(messageManager);

        getLogger().info("All managers initialized successfully!");
    }

    private void registerCommands() {
        final PluginCommand lagg = getCommand("lagg");
        if (lagg != null) {
            lagg.setExecutor(new LaggCommand());
            lagg.setTabCompleter(new LaggCommand());
        } else {
            getLogger().severe("Command 'lagg' is not defined in plugin.yml!");
        }
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new MobLimiterListener(this), this);
        getServer().getPluginManager().registerEvents(new RedstoneLimiterListener(this), this);
        getServer().getPluginManager().registerEvents(new HopperLimiterListener(this), this);
        getServer().getPluginManager().registerEvents(new SpawnerLimiterListener(this), this);
        getServer().getPluginManager().registerEvents(new VersionCheck(this), this);
    }

    private void startMiscLimiterIfEnabled() {
        final boolean miscEnabled = getConfigManager().getBoolean("lag-prevention.misc-entity-limiter.enabled", true);
        if (!miscEnabled) {
            return;
        }

        miscSweep = new MiscEntitySweepService(this, getConfigManager());
        miscSweep.start();

        getServer().getPluginManager().registerEvents(new MiscEntityLimiterListener(this, miscSweep), this);
    }

    private void stopMiscLimiterIfRunning() {
        if (miscSweep != null) {
            miscSweep.shutdown();
            miscSweep = null;
        }
    }

    private void closeQuietlyDatabase() {
        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    private static void shutdown(Object o) {
        if (o instanceof GUIManager gm) {
            gm.shutdown();
        } else if (o instanceof EntityManager em) {
            em.shutdown();
        }
    }
}
