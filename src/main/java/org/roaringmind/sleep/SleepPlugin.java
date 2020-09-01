package org.roaringmind.sleep;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

public class SleepPlugin extends JavaPlugin implements Listener {
    @Override
    public void onDisable() {
        getLogger().info("Bye...");
    }

    @Override
    public void onEnable() {
        // Register event listener
        getServer().getPluginManager().registerEvents(this, this);

        // Command handlers
        getCommand("sleepy").setExecutor(this);

        PluginDescriptionFile pluginDescription = this.getDescription();
        getLogger().info("The " + pluginDescription.getName() + " version " + pluginDescription.getVersion() + " salutes you!");
    }

    private void shout(String message) {
        Bukkit.broadcastMessage("SleepPlugin: " + message);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        shout("" + sender + " sent " + command.getName());
        return super.onCommand(sender, command, label, args);
    }

    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        shout("" + event.getPlayer().getName() + " entered bed");
    }

}
