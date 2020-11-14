package org.roaringmind.sleep;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.World.Environment;
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
    private ArrayList<SleepState> frozenPlayers;

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

        this.saveDefaultConfig();
    }

    public void shout(String message) {
        Bukkit.broadcastMessage("SleepPlugin: " + message);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        //shout("" + sender + " sent " + command.getName() + " " + String.join(" ", args));
        boolean isOp;
        if (!(sender instanceof Player)) {
            getLogger().warning("Command is not from a player");
            return true;
        }
        Player player = (Player) sender;

        if (player.isOp()) {
            isOp = true;
        } else {
            isOp = false;
        }

        if (args[0].equals("start") && isOp) {
            startSleep();
            return true;
        }

        if (args[0].equals("end") && isOp) {
            endSleep();
            return true;
        }

        if (args[0].equals("speed") && isOp) {
            player.setWalkSpeed(Float.parseFloat(args[1]));
            return true;
        }

        if (args[0].equals("fspeed") && isOp) {
            player.setFlySpeed(Float.parseFloat(args[1]));
            return true;
        }

        if (args[0].equals("f") && state == State.VOTING && isOp) {
            shout("Starting sleep (forced)!");
            countdown.cancel();
            startSleep();
            return true;
        }

        if (args[0].equals("config") && isOp) {
            shout("SleepPercent: " + this.getConfig().getInt("SleepPercent"));
            shout("SleepWhenTimerRunsOut: " + this.getConfig().getBoolean("SleepWhenTimerRunsOut"));
            shout("TimerLength: " + this.getConfig().getInt("TimerLength"));
            return true;
        }

        if (state != State.VOTING) {
            getLogger().info("ERROR\n-Either command doesn't exist\n-Or it can only be used while Voting is in process");
            return true;
        }

        if (args[0].equals("votes")) {
            int yes = 0;
            int no = 0;
            for (var vote : playerVotes.values()) {
                if (vote == VoteState.NO) {
                    ++no;
                } else {
                    ++yes;
                }
            }

            getServer().getPlayer(sender.getName()).sendMessage("Yes: " + yes + "  No: " + no);
        }

        playerVote(player, args[0].equalsIgnoreCase("yes"));
        return true;
    }

    private List<Player> overworldPlayers() {
        return getServer().getOnlinePlayers().stream()
                .filter((p) -> p.getLocation().getWorld().getEnvironment() == Environment.NORMAL)
                .collect(Collectors.toList());
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
        int oPlayers = overworldPlayers().size();
        for (var vote : playerVotes.keySet()) {
            if (playerVotes.get(vote) == VoteState.NO) {
                ++no;
            } else{
                for (var o : overworldPlayers()) {
                    if (o == getServer().getPlayer(vote)) {
                        ++yes;
                    }
                }

            }
        }
        //shout("There are currently " + yes + " votes for sleeping, and " + no + " votes against");
        // TODO: check and actually start the sleep here if the counts are OK
        int limit = Math.round((float) oPlayers / 100F * (float) this.getConfig().getInt("SleepPercent"));
        //shout("" + limit + " " + (yes >= limit) + " " + yes);
        if (no > 1) {
            countdown.cancel();
            shout("too many voted no");
        }
        if (yes >= limit) {
            shout("Starting sleep!");
            countdown.cancel();
            startSleep();
        }
    }

    void startSleep() {
        state = State.SLEEPING;

        frozenPlayers = new ArrayList<>();
        for (var p : overworldPlayers()) {
            if (p.isSleeping()) {
                // azért hogy aki ágyban van maradjon survival mode-ban és rendesen aludjon
                continue;
            }

            //shout("Freezing: " + p.getName());
            var playerSleep = new SleepState(p);
            playerSleep.freezeForSleep();
            frozenPlayers.add(playerSleep);
        }
    }

    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {

        //shout("bed enter " + event.getPlayer().getName() + " result: " +
        // event.getBedEnterResult());

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

    public void startVoting(Player initiator) {
        var players = overworldPlayers();
        if (players.size() <= 1) {
            // Csak egy ember van online, nem kell csinálni semmit.
            // TODO: nethert és endet kideríteni
            shout("Sleep tight, " + initiator.getName());
            return;
        }

        shout("" + initiator.getName() + " wants to sleep");
        state = State.VOTING;

        var msg = new ComponentBuilder("OK to sleep?  ").append("[Yes]").color(ChatColor.DARK_GREEN).bold(true)
                .event(new ClickEvent(Action.RUN_COMMAND, "/sleepy yes")).append("  ").append("[No]")
                .color(ChatColor.DARK_RED).bold(true).event(new ClickEvent(Action.RUN_COMMAND, "/sleepy no")).create();
        // getServer().spigot().broadcast(msg);

        for (var p : players) {
            if (p == initiator)
                continue;
            p.spigot().sendMessage(msg);
        }

        countdown = new ProgressBar(players, this, this::voteTimeout, this.getConfig().getInt("TimerLength"));

        playerVotes = new HashMap<>();
        playerVotes.put(initiator.getUniqueId(), VoteState.INITIATOR);
        countVotes();
    }

    private void endSleep() {
        state = State.NORMAL;

        for (var frozenPlayer : frozenPlayers) {
            //shout("Unfreezing: " + frozenPlayer.getPlayer().getName());
            frozenPlayer.restore();
        }
        frozenPlayers = null;
    }

    @EventHandler
    public void onPlayerBedLeave(PlayerBedLeaveEvent event) {
        Player player = event.getPlayer();
        //shout(player.getName() + " left the bed, state is " + state);

        if (state == State.SLEEPING) {
            endSleep();
            return;
        }

        // TODO: ellenőrizni, hogy ha a kezdeményező szállt ki, akkor cancelálni az
        // egészet vagy nem engedni vagy valami
        if (state == State.VOTING && playerVotes.get(player.getUniqueId()) == VoteState.INITIATOR) {
            shout(player.getName() + " doesn't want to sleep after all.");
            cancelVote();
        }
    }

    private void cancelVote() {
        state = State.NORMAL;
        countdown.cancel();
    }

    private void voteTimeout() {
        shout("Time is up");
        if (this.getConfig().getBoolean("SleepWhenTimerRunsOut")) {
            startSleep();
            return;
        }
        state = State.NORMAL;
    }
}
