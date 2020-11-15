package org.roaringmind.sleep;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

public class SleepState {

    private Player player;
    private GameMode gameMode;
    private float walkSpeed;
    private float flySpeed;
    private Location location;
    private int insomnia;
    private boolean sleepignore;

    public SleepState(Player player) {
        this.player = player;
        gameMode = player.getGameMode();
        walkSpeed = player.getWalkSpeed();
        flySpeed = player.getFlySpeed();
        location = player.getLocation();
        insomnia = player.getStatistic(Statistic.TIME_SINCE_REST);
        sleepignore = player.isSleepingIgnored();
    }

    public void freezeForSleep() {
        player.setSleepingIgnored(true);
    }

    public void restore() {
        player.setSleepingIgnored(sleepignore);
        //player.setGameMode(gameMode);
        //player.setWalkSpeed(walkSpeed);
        //player.setFlySpeed(flySpeed);
        //player.teleport(location);
        //player.setStatistic(Statistic.TIME_SINCE_REST, insomnia);
    }

    public Player getPlayer() {
        return player;
    }
}
