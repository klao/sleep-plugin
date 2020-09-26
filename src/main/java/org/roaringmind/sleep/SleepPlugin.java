package org.roaringmind.sleep;


import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedEnterEvent.BedEnterResult;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.api.chat.ComponentBuilder;

// We can "borrow" features from:
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

    private boolean votingPending = false;  //Ez arra van, hogy ha valaki ágyba fekszik miközben már van votolás, ne kezdödjön elörröl
    private int positiveVotes = 0; //Ezek a vote ok száma
    private int negativeVotes = 0;
    private HashMap <String, Boolean> playersVoted;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        shout("" + sender + " sent " + command.getName() + " " + String.join(" ", args));

        if (args[0].equals("Yes") && !votingPending && !playersVoted.get(sender.getName())) {
            shout(sender.getName() + "voted for sleeping");
            ++positiveVotes;
            playersVoted.put(sender.getName(), true);
            shout("Currently there are" + positiveVotes + "for sleeping, and" + negativeVotes + "against sleeping");
        }

        if (args[0].equals("No") && !votingPending/* && positiveVotes + negativeVotes < getServer().getOnlinePlayers().size()*/) {
            shout(sender.getName() + "voted against sleeping");
            ++positiveVotes;
            playersVoted.put(sender.getName(), true);
            shout("Currently there are" + positiveVotes + "votes for sleeping, and" + negativeVotes + "against sleeping");
        }

        return true;
    }

    private void counter(int i) {
        for (var p : getServer().getOnlinePlayers()) {p.sendExperienceChange(i / 30);}
    }

    public void voting() {


        var msg = new ComponentBuilder("OK to sleep?  ")
            .append("[Yes]").color(ChatColor.DARK_GREEN).bold(true).event(new ClickEvent(Action.RUN_COMMAND, "/sleepy yes"))
            .append("  ")
            .append("[No]").color(ChatColor.DARK_RED).bold(true).event(new ClickEvent(Action.RUN_COMMAND, "/sleepy no"))
            .create();
        // getServer().spigot().broadcast(msg);
        votingPending = true;

        getServer().spigot().broadcast(msg);


        for (int i = 30; i < 0; i = i - 1) {
            if (getServer().getOnlinePlayers().size() / 100 * 90 <= negativeVotes + positiveVotes) {
                break;
            }
            Bukkit.getServer().getScheduler().runTaskLater(this, counter(i), 20);
        }



        //(nem mindenképp pont itt) valami ellenörzés hogy csak egyszer lehessen vote olni
        for (var p : getServer().getOnlinePlayers()) {
            if (p.isSleeping()) continue;

            if (!p.isSleeping()) {  //azért hogy aki ágyban van maradjon survival ba

                p.setGameMode(GameMode.SPECTATOR);
                p.setWalkSpeed(0);
                p.setFlySpeed(0);
            }
        }
        negativeVotes = 0;
        positiveVotes = 0;
        playersVoted.clear();
        votingPending = false;
        return;
    }



    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        if (votingPending) {
            event.getPlayer().chat("/sleepy yes");  //lehessen ágybafeküdni a voteoláshoz
            return;
        }

        if (event.getBedEnterResult() != BedEnterResult.OK) return;

        //if (!voting()) return;
        shout("" + event.getPlayer().getName() + " wants to sleep");
        voting();



    }

    @EventHandler
    public void onPlayerBedLeave(PlayerBedLeaveEvent event) {
        for (var p : getServer().getOnlinePlayers()) {
            if (p == event.getPlayer()) continue;

            p.setGameMode(GameMode.SURVIVAL);
            p.setWalkSpeed(1);
            p.setFlySpeed(0);
        }
    }
}
