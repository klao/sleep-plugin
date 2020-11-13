package org.roaringmind.sleep;

import java.util.Collection;
import java.util.HashMap;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class ProgressBar extends BukkitRunnable {
    private Runnable callback;
    private HashMap<Player, Float> origXP;
    private float currentProgress = 1F;
    private float step;
    public ProgressBar(Collection<? extends Player> players, Plugin plugin, Runnable callback, int duration) {
        this.callback = callback;
        origXP = new HashMap<>();
        for (var p : players) {
            origXP.put(p, p.getExp());
        }
        step = 1F/(float)duration / 4F;
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
            currentProgress -= step;
        } else {
            this.cancel();
            callback.run();
        }
    }
}
