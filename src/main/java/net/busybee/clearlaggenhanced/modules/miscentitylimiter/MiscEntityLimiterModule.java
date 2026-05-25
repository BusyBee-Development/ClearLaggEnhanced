package net.busybee.clearlaggenhanced.modules.miscentitylimiter;
import net.busybee.clearlaggenhanced.ClearLaggEnhanced;
import net.busybee.clearlaggenhanced.core.Module;
import net.busybee.clearlaggenhanced.modules.miscentitylimiter.inventory.MiscEntityLimiterGUI;
import net.busybee.clearlaggenhanced.modules.miscentitylimiter.listeners.MiscEntityLimiterListener;
import net.busybee.clearlaggenhanced.modules.miscentitylimiter.tasks.MiscEntitySweepService;
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
    public void onRegister() {
        registerGUI("misc-entity-limiter", "Misc Entity Limiter", "ARMOR_STAND", () -> new MiscEntityLimiterGUI(plugin, this));
    }

    @Override
    public void onEnable() {
        miscSweepService = new MiscEntitySweepService(plugin, this);
        miscSweepService.start();

        miscEntityLimiterListener = new MiscEntityLimiterListener(plugin, miscSweepService, this);
        Bukkit.getPluginManager().registerEvents(miscEntityLimiterListener, plugin);
    }

    @Override
    public void onDisable() {
        if (miscSweepService != null) {
            miscSweepService.shutdown();
        }
        if (miscEntityLimiterListener != null) {
            HandlerList.unregisterAll(miscEntityLimiterListener);
        }
    }

    @Override
    public void onReload() {
        onDisable();
        onEnable();
    }
}
