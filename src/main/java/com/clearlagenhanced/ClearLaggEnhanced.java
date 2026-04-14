package com.clearlagenhanced;
import com.clearlagenhanced.commands.LaggCommand;
import com.clearlagenhanced.core.gui.ModuleGUIRegistry;
import com.clearlagenhanced.core.module.*;
import com.clearlagenhanced.database.DatabaseManager;
import com.clearlagenhanced.database.DatabaseSettings;
import com.clearlagenhanced.hooks.ClearLaggEnhancedExpansion;
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
import com.clearlagenhanced.utils.EntityProtectionUtils;
import com.clearlagenhanced.utils.MessageUtils;
import com.clearlagenhanced.utils.VersionCheck;
import com.tcoded.folialib.FoliaLib;
import fr.mrmicky.fastinv.FastInvManager;
import com.tcoded.folialib.impl.PlatformScheduler;
import lombok.Getter;
import org.bstats.bukkit.Metrics;
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

    public static PlatformScheduler scheduler() {
        return scheduler;
    }

    @Override
    public void onEnable() {
        instance = this;

        FoliaLib foliaLib = new FoliaLib(this);
        scheduler = foliaLib.getScheduler();

        new Metrics(this, 26743);

        saveDefaultConfig();
        FastInvManager.register(this);
        initializeCore();
        initializeModules();
        registerCommands();
        registerListeners();
        registerPlaceholders();

        getLogger().info("ClearLaggEnhanced has been enabled!");
    }

    @Override
    public void onDisable() {
        if (moduleManager != null) {
            moduleManager.disableAll();
        }

        shutdownCore();

        getLogger().info("ClearLaggEnhanced has been disabled!");
    }

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

        if (moduleManager != null) {
            moduleManager.disableAll();
        }
        HandlerList.unregisterAll(this);
        shutdownCore();
        applyCoreServices(newCoreServices);
        initializeModules();
        registerListeners();
        registerPlaceholders();

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
        getServer().getPluginManager().registerEvents(new VersionCheck(this), this);
        if (chatInputManager != null) {
            getServer().getPluginManager().registerEvents(chatInputManager, this);
        }
    }

    private void registerPlaceholders() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ClearLaggEnhancedExpansion(this).register();
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
        ChatInputManager newChatInputManager = new ChatInputManager(this);

        return new CoreServices(
                newConfigManager,
                newMessageManager,
                newDatabaseManager,
                newStackerManager,
                newEntityProtectionUtils,
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
            ChatInputManager chatInputManager
    ) {
    }
}
