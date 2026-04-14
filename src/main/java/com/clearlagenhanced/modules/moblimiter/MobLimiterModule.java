package com.clearlagenhanced.modules.moblimiter;
import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.core.module.Module;
import com.clearlagenhanced.modules.moblimiter.inventory.MobLimiterGUI;
import com.clearlagenhanced.modules.moblimiter.listeners.MobLimiterListener;
import com.clearlagenhanced.modules.moblimiter.models.LagPreventionManager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.event.HandlerList;

public class MobLimiterModule extends Module {
    private final ClearLaggEnhanced plugin;
    @Getter private LagPreventionManager lagPreventionManager;
    private MobLimiterListener mobLimiterListener;

    public MobLimiterModule(ClearLaggEnhanced plugin) {
        super("Mob Limiter", "mob-limiter");
        this.plugin = plugin;
    }

    @Override
    public void onRegister() {
        registerGUI("mob-limiter", "Mob Limiter", "ZOMBIE_HEAD", () -> new MobLimiterGUI(plugin, this));
    }

    @Override
    public void onEnable() {
        lagPreventionManager = new LagPreventionManager(plugin, this);
        mobLimiterListener = new MobLimiterListener(plugin, this);
        Bukkit.getPluginManager().registerEvents(mobLimiterListener, plugin);
    }

    @Override
    public void onDisable() {
        if (mobLimiterListener != null) {
            HandlerList.unregisterAll(mobLimiterListener);
            mobLimiterListener = null;
        }
        lagPreventionManager = null;
    }

    @Override
    public void onReload() {
        onDisable();
        onEnable();
    }

    public boolean isMobLimitReached(Chunk chunk) {
        return isEnabled() && lagPreventionManager != null && lagPreventionManager.isMobLimitReached(chunk);
    }
}
