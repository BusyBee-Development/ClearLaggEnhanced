package com.clearlagenhanced;

import com.clearlagenhanced.commands.LaggCommand;
<<<<<<< HEAD
import com.clearlagenhanced.database.DatabaseManager;
import com.clearlagenhanced.hooks.ClearLaggEnhancedExpansion;
import com.clearlagenhanced.listeners.BreedingListener;
import com.clearlagenhanced.listeners.MiscEntityLimiterListener;
import com.clearlagenhanced.listeners.MobLimiterListener;
import com.clearlagenhanced.listeners.SpawnerLimiterListener;
import com.clearlagenhanced.managers.ConfigManager;
import com.clearlagenhanced.managers.EntityManager;
import com.clearlagenhanced.managers.GUIManager;
import com.clearlagenhanced.managers.LagPreventionManager;
import com.clearlagenhanced.managers.MessageManager;
import com.clearlagenhanced.managers.MiscEntitySweepService;
import com.clearlagenhanced.managers.NotificationManager;
import com.clearlagenhanced.managers.PerformanceManager;
import com.clearlagenhanced.managers.StackerManager;
=======
import com.clearlagenhanced.core.gui.ModuleGUIRegistry;
import com.clearlagenhanced.core.module.*;
import com.clearlagenhanced.database.DatabaseManager;
import com.clearlagenhanced.database.DatabaseSettings;
import com.clearlagenhanced.database.DatabaseType;
import com.clearlagenhanced.hooks.ClearLaggEnhancedExpansion;
import com.clearlagenhanced.inventory.gui.GUIListener;
import com.clearlagenhanced.inventory.gui.GUIManager;
import com.clearlagenhanced.managers.ChatInputManager;
import com.clearlagenhanced.managers.ConfigManager;
import com.clearlagenhanced.managers.MessageManager;
import com.clearlagenhanced.managers.StackerManager;
import com.clearlagenhanced.modules.entityclearing.models.EntityManager;
import com.clearlagenhanced.modules.entityclearing.models.NotificationManager;
import com.clearlagenhanced.modules.integrations.modernshowcase.ModernShowcaseIntegration;
import com.clearlagenhanced.modules.integrations.rosestacker.RoseStackerIntegration;
import com.clearlagenhanced.modules.integrations.wildstacker.WildStackerIntegration;
import com.clearlagenhanced.modules.miscentitylimiter.MiscEntityLimiterModule;
import com.clearlagenhanced.modules.moblimiter.MobLimiterModule;
import com.clearlagenhanced.modules.moblimiter.models.LagPreventionManager;
import com.clearlagenhanced.modules.spawnerlimiter.SpawnerLimiterModule;
>>>>>>> dev
import com.clearlagenhanced.utils.EntityProtectionUtils;
import com.clearlagenhanced.utils.MessageUtils;
import com.clearlagenhanced.utils.VersionCheck;
import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.impl.PlatformScheduler;
import lombok.Getter;
import org.bstats.bukkit.Metrics;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

<<<<<<< HEAD
=======
import java.util.Map;

>>>>>>> dev
public class ClearLaggEnhanced extends JavaPlugin {

    @Getter
    private static ClearLaggEnhanced instance;

    private static PlatformScheduler scheduler;

    @Getter private DatabaseManager databaseManager;
    @Getter private ConfigManager configManager;
    @Getter private MessageManager messageManager;
    @Getter private StackerManager stackerManager;
<<<<<<< HEAD
    @Getter private EntityManager entityManager;
    @Getter private LagPreventionManager lagPreventionManager;
    @Getter private PerformanceManager performanceManager;
    @Getter private NotificationManager notificationManager;
    @Getter private EntityProtectionUtils entityProtectionUtils;
    private GUIManager guiManager;

    private MiscEntitySweepService miscSweep;

    public static PlatformScheduler scheduler() {
      return scheduler;
=======
    @Getter private EntityProtectionUtils entityProtectionUtils;
    @Getter private GUIManager guiManager;
    @Getter private ModuleGUIRegistry guiRegistry;
    @Getter private ModuleManager moduleManager;
    @Getter private ChatInputManager chatInputManager;

    public static PlatformScheduler scheduler() {
        return scheduler;
>>>>>>> dev
    }

    @Override
    public void onEnable() {
        instance = this;

        FoliaLib foliaLib = new FoliaLib(this);
        scheduler = foliaLib.getScheduler();

        new Metrics(this, 26743);

        saveDefaultConfig();
<<<<<<< HEAD
        initializeManagers();
        registerCommands();
        registerListeners();
        startMiscLimiterIfEnabled();
=======
        initializeCore();
        initializeModules();
        registerCommands();
        registerListeners();
>>>>>>> dev
        registerPlaceholders();

        getLogger().info("ClearLaggEnhanced has been enabled!");
    }

    @Override
    public void onDisable() {
<<<<<<< HEAD
        closeQuietlyDatabase();
        shutdown(entityManager);
        shutdown(guiManager);
        shutdown(notificationManager);
        stopMiscLimiterIfRunning();
=======
        if (moduleManager != null) {
            moduleManager.disableAll();
        }

        shutdownCore();
>>>>>>> dev

        getLogger().info("ClearLaggEnhanced has been disabled!");
    }

<<<<<<< HEAD
    public GUIManager getGUIManager() {
        return guiManager;
    }

    public void reloadAll(CommandSender sender) {
        HandlerList.unregisterAll(this);

        shutdown(entityManager);
        shutdown(guiManager);
        shutdown(notificationManager);
        stopMiscLimiterIfRunning();

        initializeManagers();

        registerListeners();
        startMiscLimiterIfEnabled();
=======
    private void initializeCore() {
        applyCoreServices(buildCoreServices());
    }

    private void initializeModules() {
        moduleManager = new ModuleManager(this, guiRegistry);

        moduleManager.registerModule(new EntityClearingModule(this));
        moduleManager.registerModule(new MobLimiterModule(this));
        moduleManager.registerModule(new SpawnerLimiterModule(this));
        moduleManager.registerModule(new MiscEntityLimiterModule(this));
        moduleManager.registerModule(new ChunkFinderModule(this));
        moduleManager.registerModule(new PerformanceModule(this));

        moduleManager.registerModule(new WildStackerIntegration(this));
        moduleManager.registerModule(new RoseStackerIntegration(this));
        moduleManager.registerModule(new ModernShowcaseIntegration(this));

        moduleManager.loadAll();
    }

    public EntityManager getEntityManager() {
        EntityClearingModule module = (EntityClearingModule) moduleManager.getModule("Entity Clearing");
        return module != null ? module.getEntityManager() : null;
    }

    public NotificationManager getNotificationManager() {
        EntityClearingModule module = (EntityClearingModule) moduleManager.getModule("Entity Clearing");
        return module != null ? module.getNotificationManager() : null;
    }

    public com.clearlagenhanced.modules.performance.models.PerformanceManager getPerformanceManager() {
        PerformanceModule module = (PerformanceModule) moduleManager.getModule("Performance");
        return module != null ? module.getPerformanceManager() : null;
    }

    public LagPreventionManager getLagPreventionManager() {
        MobLimiterModule module = (MobLimiterModule) moduleManager.getModule("Mob Limiter");
        return module != null ? module.getLagPreventionManager() : null;
    }

    public void reloadAll(CommandSender sender) {
        CoreServices newCoreServices;
        try {
            newCoreServices = buildCoreServices();
        } catch (RuntimeException exception) {
            getLogger().severe("Failed to reload core services: " + exception.getMessage());
            if (sender != null) {
                MessageUtils.sendMessage(sender, "notifications.reload-failed", Map.of(
                        "reason", reloadFailureReason(exception)
                ));
            }
            return;
        }

        DatabaseType previousDatabaseType = databaseManager != null && databaseManager.isEnabled()
                ? databaseManager.getType()
                : null;
        DatabaseType newDatabaseType = newCoreServices.databaseManager().isEnabled()
                ? newCoreServices.databaseManager().getType()
                : null;

        HandlerList.unregisterAll(this);

        if (moduleManager != null) {
            moduleManager.disableAll();
        }

        shutdownCore();
        applyCoreServices(newCoreServices);

        if (previousDatabaseType != null && newDatabaseType != null && previousDatabaseType != newDatabaseType) {
            getLogger().warning("Database backend changed from " + previousDatabaseType.getConfigValue() + " to " + newDatabaseType.getConfigValue() + ". Existing data is not migrated automatically.");
        }

        moduleManager.reloadAll();

        registerListeners();
>>>>>>> dev

        if (sender != null) {
            MessageUtils.sendMessage(sender, "notifications.reload-complete");
        }
    }

<<<<<<< HEAD
    private void initializeManagers() {
        configManager = new ConfigManager(this);
        configManager.reload();
        messageManager = new MessageManager(this);
        MessageUtils.initialize(messageManager);
        databaseManager = new DatabaseManager(this);

        stackerManager = new StackerManager(this);
        notificationManager = new NotificationManager(this);
        performanceManager = new PerformanceManager(this);
        entityProtectionUtils = new EntityProtectionUtils(this);
        lagPreventionManager = new LagPreventionManager(this);

        entityManager = new EntityManager(this);
        entityManager.startAutoClearTask();

        guiManager = new GUIManager(this);
    }

=======
>>>>>>> dev
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
<<<<<<< HEAD
        getServer().getPluginManager().registerEvents(new MobLimiterListener(this), this);
        getServer().getPluginManager().registerEvents(new SpawnerLimiterListener(this), this);
        getServer().getPluginManager().registerEvents(new BreedingListener(this), this);
        getServer().getPluginManager().registerEvents(new VersionCheck(this), this);
        if (guiManager != null) {
            getServer().getPluginManager().registerEvents(guiManager, this);
=======
        getServer().getPluginManager().registerEvents(new VersionCheck(this), this);
        if (guiManager != null) {
            GUIListener guiListener = new GUIListener(guiManager);
            getServer().getPluginManager().registerEvents(guiListener, this);
        }
        if (chatInputManager != null) {
            getServer().getPluginManager().registerEvents(chatInputManager, this);
>>>>>>> dev
        }
    }

    private void registerPlaceholders() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ClearLaggEnhancedExpansion(this).register();
        }
    }

<<<<<<< HEAD
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
        } else if (o instanceof NotificationManager nm) {
            nm.shutdown();
        }
    }
=======
    private void closeQuietlyDatabase() {
        if (databaseManager != null) {
            databaseManager.close();
            databaseManager = null;
        }
    }

    private void shutdownCore() {
        closeQuietlyDatabase();

        if (guiManager != null) {
            guiManager.shutdown();
            guiManager = null;
        }

        chatInputManager = null;
        entityProtectionUtils = null;
        stackerManager = null;
        messageManager = null;
        configManager = null;
    }

    private CoreServices buildCoreServices() {
        ConfigManager newConfigManager = new ConfigManager(this);
        MessageManager newMessageManager = new MessageManager(this);
        DatabaseSettings databaseSettings = DatabaseSettings.from(newConfigManager.getConfig(), getDataFolder());
        DatabaseManager newDatabaseManager = new DatabaseManager(getName(), getLogger(), databaseSettings);
        StackerManager newStackerManager = new StackerManager(this);
        EntityProtectionUtils newEntityProtectionUtils = new EntityProtectionUtils(this, newStackerManager);
        GUIManager newGuiManager = new GUIManager();
        ChatInputManager newChatInputManager = new ChatInputManager(this);

        return new CoreServices(
                newConfigManager,
                newMessageManager,
                newDatabaseManager,
                newStackerManager,
                newEntityProtectionUtils,
                newGuiManager,
                newChatInputManager
        );
    }

    private void applyCoreServices(CoreServices coreServices) {
        configManager = coreServices.configManager();
        messageManager = coreServices.messageManager();
        MessageUtils.initialize(messageManager);
        databaseManager = coreServices.databaseManager();
        stackerManager = coreServices.stackerManager();
        entityProtectionUtils = coreServices.entityProtectionUtils();
        guiManager = coreServices.guiManager();
        chatInputManager = coreServices.chatInputManager();

        if (guiRegistry == null) {
            guiRegistry = new ModuleGUIRegistry();
        } else {
            guiRegistry.clear();
        }
    }

    private String reloadFailureReason(RuntimeException exception) {
        Throwable cause = exception.getCause();
        if (cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()) {
            return cause.getMessage();
        }
        if (exception.getMessage() != null && !exception.getMessage().isBlank()) {
            return exception.getMessage();
        }
        return "Unknown error";
    }

    public MobLimiterModule getMobLimiterModule() {
        return (MobLimiterModule) moduleManager.getModule("Mob Limiter");
    }

    public ChunkFinderModule getChunkFinderModule() {
        return (ChunkFinderModule) moduleManager.getModule("Chunk Finder");
    }

    private record CoreServices(
            ConfigManager configManager,
            MessageManager messageManager,
            DatabaseManager databaseManager,
            StackerManager stackerManager,
            EntityProtectionUtils entityProtectionUtils,
            GUIManager guiManager,
            ChatInputManager chatInputManager
    ) {
    }
>>>>>>> dev
}
