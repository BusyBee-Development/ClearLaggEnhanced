package net.busybee.clearlaggenhanced;

import net.busybee.clearlaggenhanced.commands.LaggCommand;
import net.busybee.clearlaggenhanced.core.updater.FoliaUpdateNotifier;
import net.busybee.clearlaggenhanced.core.updater.VersionCheck;
import net.busybee.clearlaggenhanced.database.DatabaseSettings;
import net.busybee.clearlaggenhanced.gui.ModuleGUIRegistry;
import net.busybee.clearlaggenhanced.hooks.ClearLaggEnhancedExpansion;
import net.busybee.clearlaggenhanced.managers.*;
import net.busybee.clearlaggenhanced.modules.chunkfinder.ChunkFinderModule;
import net.busybee.clearlaggenhanced.modules.entityclearing.EntityClearingModule;
import net.busybee.clearlaggenhanced.modules.integrations.modernshowcase.ModernShowcaseIntegration;
import net.busybee.clearlaggenhanced.modules.integrations.rosestacker.RoseStackerIntegration;
import net.busybee.clearlaggenhanced.modules.integrations.wildstacker.WildStackerIntegration;
import net.busybee.clearlaggenhanced.modules.miscentitylimiter.MiscEntityLimiterModule;
import net.busybee.clearlaggenhanced.modules.moblimiter.MobLimiterModule;
import net.busybee.clearlaggenhanced.modules.moblimiter.models.LagPreventionManager;
import net.busybee.clearlaggenhanced.modules.performance.PerformanceModule;
import net.busybee.clearlaggenhanced.modules.performance.models.PerformanceManager;
import net.busybee.clearlaggenhanced.modules.spawnerlimiter.SpawnerLimiterModule;
import net.busybee.clearlaggenhanced.utils.MessageUtils;
import com.tcoded.folialib.FoliaLib;
import fr.mrmicky.fastinv.FastInvManager;
import com.tcoded.folialib.impl.PlatformScheduler;
import lombok.Getter;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public class ClearLaggEnhanced extends JavaPlugin {

    @Getter
    private static ClearLaggEnhanced instance;

    private static PlatformScheduler scheduler;

    @Getter private DatabaseManager databaseManager;
    @Getter private ConfigManager configManager;
    @Getter private MessageManager messageManager;
    @Getter private StackerManager stackerManager;
    @Getter private EntityProtectionUtils entityProtectionUtils;
    @Getter private ModuleGUIRegistry guiRegistry;
    @Getter private ModuleManager moduleManager;
    @Getter private ChatInputManager chatInputManager;
    @Getter private VersionCheck versionCheck;
    @Getter private BStatsManager bStatsManager;
    @Getter private FastStatsManager fastStatsManager;
    private FoliaLib foliaLib;
    private ClearLaggEnhancedExpansion placeholderExpansion;

    public static PlatformScheduler scheduler() {
        return scheduler;
    }

    @Override
    public void onEnable() {
        instance = this;

        silenceLoggers();

        foliaLib = new FoliaLib(this);
        scheduler = foliaLib.getScheduler();

        saveDefaultConfig();
        FastInvManager.register(this);
        initializeCore();
        registerCommands();
        registerListeners();
        registerPlaceholders();

        if (versionCheck != null) {
            new FoliaUpdateNotifier(this, versionCheck).check();
        }

        getLogger().info("ClearLaggEnhanced is ready!");
    }

    private void silenceLoggers() {
        String[] loggers = {
                "com.zaxxer.hikari",
                "com.zaxxer.hikari.HikariConfig",
                "com.zaxxer.hikari.HikariDataSource",
                "com.zaxxer.hikari.pool.HikariPool",
                "com.zaxxer.hikari.pool.PoolBase"
        };

        java.util.logging.Level julLevel = java.util.logging.Level.SEVERE;
        for (String loggerName : loggers) {
            java.util.logging.Logger.getLogger(loggerName).setLevel(julLevel);
        }

        try {
            Class<?> levelClass = Class.forName("org.apache.logging.log4j.Level");
            Object errorLevel = levelClass.getField("ERROR").get(null);
            Class<?> configuratorClass = Class.forName("org.apache.logging.log4j.core.config.Configurator");
            java.lang.reflect.Method setLevelMethod = configuratorClass.getMethod("setLevel", String.class, levelClass);

            for (String loggerName : loggers) {
                setLevelMethod.invoke(null, loggerName, errorLevel);
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> factoryClass = Class.forName("org.slf4j.LoggerFactory");
            Object loggerContext = factoryClass.getMethod("getILoggerFactory").invoke(null);
            Class<?> loggerContextClass = Class.forName("ch.qos.logback.classic.LoggerContext");
            Class<?> loggerClass = Class.forName("ch.qos.logback.classic.Logger");
            Class<?> levelClass = Class.forName("ch.qos.logback.classic.Level");

            java.lang.reflect.Method getLoggerMethod = loggerContextClass.getMethod("getLogger", String.class);
            java.lang.reflect.Method setLevelMethod = loggerClass.getMethod("setLevel", levelClass);
            Object errorLevel = levelClass.getField("ERROR").get(null);

            for (String loggerName : loggers) {
                Object logger = getLoggerMethod.invoke(loggerContext, loggerName);
                setLevelMethod.invoke(logger, errorLevel);
            }
        } catch (Throwable ignored) {}
    }

    @Override
    public void onDisable() {
        if (moduleManager != null) {
            moduleManager.disableAll();
        }

        if (placeholderExpansion != null) {
            placeholderExpansion.unregister();
        }

        if (fastStatsManager != null) {
            fastStatsManager.onDisable();
        }

        shutdownCore();

        getLogger().info("ClearLaggEnhanced has been disabled!");
    }

    private void initializeCore() {
        applyCoreServices(buildCoreServices());
    }

    private void registerModules(ModuleManager moduleManager) {
        moduleManager.registerModule(new EntityClearingModule(this));
        moduleManager.registerModule(new MobLimiterModule(this));
        moduleManager.registerModule(new SpawnerLimiterModule(this));
        moduleManager.registerModule(new MiscEntityLimiterModule(this));
        moduleManager.registerModule(new ChunkFinderModule(this));
        moduleManager.registerModule(new PerformanceModule(this));
        moduleManager.registerModule(new WildStackerIntegration(this));
        moduleManager.registerModule(new RoseStackerIntegration(this));
        moduleManager.registerModule(new ModernShowcaseIntegration(this));
    }

    public PerformanceManager getPerformanceManager() {
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

        if (moduleManager != null) {
            moduleManager.disableAll();
        }

        if (versionCheck != null) {
            HandlerList.unregisterAll(versionCheck);
            versionCheck = null;
        }

        if (chatInputManager != null) {
            HandlerList.unregisterAll(chatInputManager);
        }

        shutdownCore();
        applyCoreServices(newCoreServices);
        registerListeners();
        registerPlaceholders();

        if (versionCheck != null) {
            new FoliaUpdateNotifier(this, versionCheck).check();
        }

        if (sender != null) {
            MessageUtils.sendMessage(sender, "notifications.reload-complete");
        }
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
        versionCheck = new VersionCheck(this);
        getServer().getPluginManager().registerEvents(versionCheck, this);
        if (chatInputManager != null) {
            getServer().getPluginManager().registerEvents(chatInputManager, this);
        }
    }

    private void registerPlaceholders() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            if (placeholderExpansion != null) {
                placeholderExpansion.unregister();
            }
            placeholderExpansion = new ClearLaggEnhancedExpansion(this);
            placeholderExpansion.register();
        }
    }

    private void closeQuietlyDatabase() {
        if (databaseManager != null) {
            databaseManager.close();
            databaseManager = null;
        }
    }

    private void shutdownCore() {
        closeQuietlyDatabase();

        if (fastStatsManager != null) {
            fastStatsManager.onDisable();
        }

        chatInputManager = null;
        entityProtectionUtils = null;
        stackerManager = null;
        messageManager = null;
        configManager = null;
        moduleManager = null;
        guiRegistry = null;
        versionCheck = null;
        bStatsManager = null;
        fastStatsManager = null;
    }

    private CoreServices buildCoreServices() {
        ConfigManager newConfigManager = new ConfigManager(this);
        MessageManager newMessageManager = new MessageManager(this);
        DatabaseSettings databaseSettings = DatabaseSettings.from(newConfigManager.getConfig(), getDataFolder());
        DatabaseManager newDatabaseManager = new DatabaseManager(getName(), getLogger(), databaseSettings);
        StackerManager newStackerManager = new StackerManager(this);
        EntityProtectionUtils newEntityProtectionUtils = new EntityProtectionUtils(this, newStackerManager);
        ChatInputManager newChatInputManager = new ChatInputManager(this);
        ModuleGUIRegistry newGuiRegistry = new ModuleGUIRegistry();
        ModuleManager newModuleManager = new ModuleManager(this, newConfigManager, newGuiRegistry);
        VersionCheck newVersionCheck = new VersionCheck(this);
        BStatsManager newBStatsManager = new BStatsManager(this);
        FastStatsManager newFastStatsManager = new FastStatsManager(this, newModuleManager);

        registerModules(newModuleManager);

        return new CoreServices(
                newConfigManager,
                newMessageManager,
                newDatabaseManager,
                newStackerManager,
                newEntityProtectionUtils,
                newChatInputManager,
                newGuiRegistry,
                newModuleManager,
                newVersionCheck,
                newBStatsManager,
                newFastStatsManager
        );
    }

    private void applyCoreServices(CoreServices coreServices) {
        configManager = coreServices.configManager();
        messageManager = coreServices.messageManager();
        MessageUtils.initialize(messageManager);
        databaseManager = coreServices.databaseManager();
        stackerManager = coreServices.stackerManager();
        entityProtectionUtils = coreServices.entityProtectionUtils();
        chatInputManager = coreServices.chatInputManager();
        guiRegistry = coreServices.guiRegistry();
        moduleManager = coreServices.moduleManager();
        versionCheck = coreServices.versionCheck();
        bStatsManager = coreServices.bStatsManager();
        fastStatsManager = coreServices.fastStatsManager();

        if (fastStatsManager != null) {
            fastStatsManager.onEnable();
        }

        if (moduleManager != null) {
            moduleManager.loadAll();
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

    private record CoreServices(
            ConfigManager configManager,
            MessageManager messageManager,
            DatabaseManager databaseManager,
            StackerManager stackerManager,
            EntityProtectionUtils entityProtectionUtils,
            ChatInputManager chatInputManager,
            ModuleGUIRegistry guiRegistry,
            ModuleManager moduleManager,
            VersionCheck versionCheck,
            BStatsManager bStatsManager,
            FastStatsManager fastStatsManager
    ) {
    }
}
