package com.clearlagenhanced.modules.miscentitylimiter;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.core.module.Module;
import com.clearlagenhanced.modules.miscentitylimiter.inventory.MiscEntityLimiterGUI;
import com.clearlagenhanced.modules.miscentitylimiter.listeners.MiscEntityLimiterListener;
import com.clearlagenhanced.modules.miscentitylimiter.tasks.MiscEntitySweepService;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;

public class MiscEntityLimiterModule extends Module {
    private final ClearLaggEnhanced plugin;
    @Getter private MiscEntitySweepService miscSweepService;
    private MiscEntityLimiterListener miscEntityLimiterListener;

    public MiscEntityLimiterModule(ClearLaggEnhanced plugin) {
        super("Misc Entity Limiter", "misc-entity-limiter");
        this.plugin = plugin;
    }

    @Override
    public void onEnable() {
        miscSweepService = new MiscEntitySweepService(plugin, this);
        miscSweepService.start();
        
        miscEntityLimiterListener = new MiscEntityLimiterListener(plugin, miscSweepService, this);
        Bukkit.getPluginManager().registerEvents(miscEntityLimiterListener, plugin);
        
        registerGUI("misc-entity-limiter", "Misc Entity Limiter", "ARMOR_STAND", () -> new MiscEntityLimiterGUI(plugin, this));
    }

    @Override
    public void onDisable() {
        if (miscSweepService != null) {
            miscSweepService.shutdown();
        }
        if (miscEntityLimiterListener != null) {
            HandlerList.unregisterAll(miscEntityLimiterListener);
        }
        
        unregisterGUI("misc-entity-limiter");
    }

    @Override
    public void onReload() {
        onDisable();
        onEnable();
    }
}