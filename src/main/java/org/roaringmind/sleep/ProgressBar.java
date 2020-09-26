package org.roaringmind.sleep;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class ProgressBar extends BukkitRunnable {
    private Player player;
    private Plugin plugin;
    private float origXP;
    private float currentProgress = 1;

    public ProgressBar(Player player, Plugin plugin) {
        this.player = player;
        this.plugin = plugin;
        origXP = player.getExp();
        // 5 tikkenkent levesz 5 szazalekot
        this.runTaskTimer(plugin, 1, 5);
    }

    @Override
    public void run() {
        if (currentProgress > 0) {
            player.sendExperienceChange(currentProgress);
            currentProgress -= 0.01;
        } else {
            player.sendExperienceChange(origXP);
            this.cancel();
        }
    }
}
