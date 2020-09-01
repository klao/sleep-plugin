package org.roaringmind.sleep;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

public class SleepPlugin extends JavaPlugin {
    @Override
    public void onDisable() {
        getLogger().info("Bye...");
    }

    @Override
    public void onEnable() {
        PluginDescriptionFile pluginDescription = this.getDescription();
        getLogger().info("The " + pluginDescription.getName() + " version " + pluginDescription.getVersion() + " salutes you!");
    }
}
