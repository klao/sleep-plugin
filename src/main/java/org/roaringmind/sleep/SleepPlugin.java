package org.roaringmind.sleep;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedEnterEvent.BedEnterResult;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.api.chat.ComponentBuilder;

// We can "borrow" features from:
// https://github.com/Nuytemans-Dieter/BetterSleeping
// https://github.com/JoelGodOfwar/SinglePlayerSleep

public class SleepPlugin extends JavaPlugin implements Listener {

    enum State {
        NORMAL, // Normal state, waiting till someone gets in bed
        VOTING, // After someone got in bed; waiting for vote results and counting down
        SLEEPING, // Vote was positive, we put everyone to sleep and waiting for morning
    }

    private State state = State.NORMAL;
    private HashMap<UUID, VoteState> playerVotes;
    private ProgressBar countdown;

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
        getLogger().info(
                "The " + pluginDescription.getName() + " version " + pluginDescription.getVersion() + " salutes you!");
    }

    private void shout(String message) {
        Bukkit.broadcastMessage("SleepPlugin: " + message);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        shout("" + sender + " sent " + command.getName() + " " + String.join(" ", args));

        if (!(sender instanceof Player)) {
            getLogger().warning("Command is not from a player");
            return true;
        }
        Player player = (Player) sender;

        if (state != State.VOTING) {
            getLogger().info("Command received while no voting in progress");
            return true;
        }

        playerVote(player, args[0].equals("Yes"));
        return true;
    }

    private void playerVote(Player player, boolean vote) {
        if (playerVotes.get(player.getUniqueId()) == VoteState.INITIATOR) {
            getLogger().info("Initiator trying to change their vote");
            // TODO: reply to the user that they cannot change their vote
            return;
        }

        shout(player.getName() + " voted " + (vote ? "for" : "against") + " sleeping");
        playerVotes.put(player.getUniqueId(), vote ? VoteState.YES : VoteState.NO);

        countVotes();
    }

    private void countVotes() {
        int yes = 0;
        int no = 0;
        for (var vote : playerVotes.values()) {
            if (vote == VoteState.NO) {
                ++no;
            } else {
                ++yes;
            }
        }
        shout("There are currently " + yes + " votes for sleeping, and " + no + " votes against");
        // TODO: check and actually start the sleep here if the counts are OK
        if (yes > 1) {
            shout("Starting sleep!");
            countdown.cancel();
            // startSleep();
        }
    }

    void startSleep() {
        state = State.SLEEPING;

        for (var p : getServer().getOnlinePlayers()) {
            if (p.isSleeping()) {
                // azért hogy aki ágyban van maradjon survival mode-ban és rendesen aludjon
                continue;
            }

            p.setGameMode(GameMode.SPECTATOR);
            p.setWalkSpeed(0);
            p.setFlySpeed(0);
        }
    }

    public void startVoting(Player initiator) {
        shout("" + initiator.getName() + " wants to sleep");
        state = State.VOTING;

        var msg = new ComponentBuilder("OK to sleep?  ").append("[Yes]").color(ChatColor.DARK_GREEN).bold(true)
                .event(new ClickEvent(Action.RUN_COMMAND, "/sleepy yes")).append("  ").append("[No]")
                .color(ChatColor.DARK_RED).bold(true).event(new ClickEvent(Action.RUN_COMMAND, "/sleepy no")).create();
        getServer().spigot().broadcast(msg);

        countdown = new ProgressBar(getServer().getOnlinePlayers(), this, () -> shout("Time is up!"));

        playerVotes = new HashMap<>();
        playerVotes.put(initiator.getUniqueId(), VoteState.INITIATOR);
        countVotes();
    }

    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        if (event.getBedEnterResult() != BedEnterResult.OK)
            return;

        if (state == State.VOTING) {
            // lehessen ágybafeküdni a voteoláshoz
            playerVote(event.getPlayer(), true);
            return;
        }

        if (state == State.SLEEPING) {
            // Hajaj, valaki valahogy ágyba feküdt alvás közben :D
            getLogger().warning("BedEnter while SLEEPING");
            return;
        }

        startVoting(event.getPlayer());
    }

    private void endSleep() {
        state = State.NORMAL;

        // TODO!
        for (var p : getServer().getOnlinePlayers()) {
            // if (p == event.getPlayer())
            // continue;

            p.setGameMode(GameMode.SURVIVAL);
            p.setWalkSpeed(1);
            p.setFlySpeed(0);
        }

    }

    @EventHandler
    public void onPlayerBedLeave(PlayerBedLeaveEvent event) {
        if (state == State.SLEEPING) {
            endSleep();
            return;
        }

        if (state == State.VOTING) {
            // TODO: ellenőrizni, hogy ha a kezdeményező szállt ki, akkor cancelálni az
            // egészet vagy nem engedni vagy valami
        }
    }
}
