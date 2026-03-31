package com.clearlagenhanced;

import com.clearlagenhanced.commands.LaggCommand;
import com.clearlagenhanced.core.gui.ModuleGUIRegistry;
import com.clearlagenhanced.core.module.*;
import com.clearlagenhanced.database.DatabaseManager;
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

public class ClearLaggEnhanced extends JavaPlugin {

    @Getter
    private static ClearLaggEnhanced instance;

    private static PlatformScheduler scheduler;

    @Getter private DatabaseManager databaseManager;
    @Getter private ConfigManager configManager;
    @Getter private MessageManager messageManager;
    @Getter private StackerManager stackerManager;
    @Getter private EntityProtectionUtils entityProtectionUtils;
    @Getter private GUIManager guiManager;
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

        closeQuietlyDatabase();
        if (guiManager != null) {
            guiManager.shutdown();
        }

        getLogger().info("ClearLaggEnhanced has been disabled!");
    }

    private void initializeCore() {
        configManager = new ConfigManager(this);
        configManager.reload();
        messageManager = new MessageManager(this);
        MessageUtils.initialize(messageManager);
        databaseManager = new DatabaseManager(this);
        stackerManager = new StackerManager(this);
        entityProtectionUtils = new EntityProtectionUtils(this);
        guiManager = new GUIManager();
        chatInputManager = new ChatInputManager(this);
        if (guiRegistry == null) {
            guiRegistry = new ModuleGUIRegistry();
        } else {
            guiRegistry.clear();
        }
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
        HandlerList.unregisterAll(this);

        if (guiManager != null) {
            guiManager.shutdown();
        }

        moduleManager.disableAll();

        initializeCore();

        moduleManager.reloadAll();

        registerListeners();

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
        if (guiManager != null) {
            GUIListener guiListener = new GUIListener(guiManager);
            getServer().getPluginManager().registerEvents(guiListener, this);
        }
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
        }
    }

    public MobLimiterModule getMobLimiterModule() {
        return (MobLimiterModule) moduleManager.getModule("Mob Limiter");
    }

    public ChunkFinderModule getChunkFinderModule() {
        return (ChunkFinderModule) moduleManager.getModule("Chunk Finder");
    }
}
