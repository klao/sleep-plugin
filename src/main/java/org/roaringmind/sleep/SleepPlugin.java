package org.roaringmind.sleep;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerBedEnterEvent.BedEnterResult;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

// TODO: "borrow" features from:
// https://github.com/Nuytemans-Dieter/BetterSleeping
// https://github.com/JoelGodOfwar/SinglePlayerSleep

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
        if (event.getBedEnterResult() != BedEnterResult.OK) {
            return;
        }
        shout("" + event.getPlayer().getName() + " wants to sleep");
        for (var p : getServer().getOnlinePlayers()) {
            shout("Online:" + p.getName());
            
            if (p == event.getPlayer()) {
                continue;
            

            p.setGameMode(GameMode.SPECTATOR);
            p.setWalkSpeed(0);
            p.setFlySpeed(0);
            // var bed = p.getBedSpawnLocation();
            // shout(p.getName() + " " + bed);
            // if (!p.sleep(p.getLocation(), true)) {
            //     shout(p.getName() + " " + "not succesfull");
            // }
        }

    }

    @EventHandler
    public void onPlayerBedLeave(PlayerBedLeaveEvent event) {
        for (var p : getServer().getOnlinePlayers()) {
            
            if (p == event.getPlayer()) {
                continue;
            }
            

            p.setGameMode(GameMode.SURVIVAL);
            p.setWalkSpeed(1);
            p.setFlySpeed(0);
            // var bed = p.getBedSpawnLocation();
            // shout(p.getName() + " " + bed);
            // if (!p.sleep(p.getLocation(), true)) {
            //     shout(p.getName() + " " + "not succesfull");
            // }
        }
    }
}
