package org.roaringmind.sleep;

import java.util.Collection;
import java.util.HashMap;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class ProgressBar extends BukkitRunnable {
    private Runnable callback;
    private HashMap<Player, Float> origXP;
    private float currentProgress = 0;
    public ProgressBar(Collection<? extends Player> players, Plugin plugin, Runnable callback, int length) {
        this.callback = callback;
        currentProgress = length / 25;
        origXP = new HashMap<>();
        for (var p : players) {
            origXP.put(p, p.getExp());
        }

        // 5 tikkenkent fut
        this.runTaskTimer(plugin, 1, 5);
    }

    @Override
    public synchronized void cancel() {
        for (var e : origXP.entrySet()) {
            e.getKey().sendExperienceChange(e.getValue());
        }
        super.cancel();
    }

    @Override
    public void run() {
        if (currentProgress > 0) {
            for (var p : origXP.keySet()) {
                p.sendExperienceChange(currentProgress);
            }
            currentProgress -= 0.01;
        } else {
            this.cancel();
            callback.run();
        }
    }
}
