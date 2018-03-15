package net.minespree.thimble.state;

import net.minespree.cartographer.maps.ThimbleGameMap;
import net.minespree.rise.RisePlugin;
import net.minespree.rise.states.BaseGameState;
import net.minespree.rise.states.GameState;
import net.minespree.thimble.ThimblePlugin;
import net.minespree.thimble.ThimbleScoreboardFeature;
import net.minespree.thimble.extension.ColourExtension;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nonnull;

public abstract class ThimbleState extends BaseGameState {

    protected ThimbleGameMap map;
    ThimbleScoreboardFeature scoreboard;
    protected ColourExtension extension;

    private BukkitTask tick;

    ThimbleState() {
        super();

        map = (ThimbleGameMap) mapManager.getCurrentMap();
        scoreboard = new ThimbleScoreboardFeature(ThimblePlugin.getPlugin());
    }

    void initialize() {
        tick = Bukkit.getScheduler().runTaskTimer(RisePlugin.getPlugin(), this::tick, 1L, 1L);
    }

    @Override
    public abstract void onStart(GameState previous);
    protected abstract void tick();

    @Override
    public void onStop(@Nonnull GameState next) {
        tick.cancel();
    }
}
