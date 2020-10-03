package org.roaringmind.sleep;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class SleepState {

    private Player player;
    private GameMode gameMode;
    private float walkSpeed;
    private float flySpeed;
    private Location location;

    public SleepState(Player player) {
        this.player = player;
        gameMode = player.getGameMode();
        walkSpeed = player.getWalkSpeed();
        flySpeed = player.getFlySpeed();
        location = player.getLocation();
    }

    public void freezeForSleep() {
        player.setGameMode(GameMode.SPECTATOR);
        player.setWalkSpeed(0);
        player.setFlySpeed(0);
    }

    public void restore() {
        player.setGameMode(gameMode);
        player.setWalkSpeed(walkSpeed);
        player.setFlySpeed(flySpeed);
        player.teleport(location);
    }

    public Player getPlayer() {
        return player;
    }
}
