package net.minespree.thimble;

import com.google.common.collect.ImmutableList;
import lombok.Getter;
import net.minespree.babel.Babel;
import net.minespree.feather.data.gamedata.GameRegistry;
import net.minespree.feather.player.stats.local.SessionStatRegistry;
import net.minespree.feather.player.stats.local.StatType;
import net.minespree.rise.RisePlugin;
import net.minespree.rise.control.Gamemode;
import net.minespree.rise.util.InformationBook;
import net.minespree.thimble.extension.ColourExtension;
import net.minespree.thimble.state.ThimblePlayState;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;

public class ThimblePlugin extends JavaPlugin {

    @Getter
    private static ThimblePlugin plugin;

    public void onEnable() {
        plugin = this;

        RisePlugin.getPlugin().getGameManager().setGamemode(Gamemode.builder()
                .plugin(this)
                .initialGameState(ThimblePlayState::new)
                .spawnHandler(() -> (player, spawnReason) -> player.teleport(RisePlugin.getPlugin().getConfiguration().getLobbyLocation()))
                .features(ImmutableList.of())
                .waitingStateExtensions(ImmutableList.of(ColourExtension.getInstance()))
                .statisticSize(9)
                .game(GameRegistry.Type.THIMBLE)
                .statisticMap(new LinkedHashMap<String, StatType>() {
                    {put("jumps", new StatType("th_jumps", SessionStatRegistry.Sorter.HIGHEST_SCORE, true, 0, 0, GameRegistry.Type.THIMBLE));}
                    {put("successful", new StatType("th_successful", SessionStatRegistry.Sorter.HIGHEST_SCORE, false, 5, 0, GameRegistry.Type.THIMBLE));}
                    {put("hitblock", new StatType("th_hitblock", SessionStatRegistry.Sorter.HIGHEST_SCORE, false, 0, 0, (p, o) ->
                            p.getPersistentStats().getLocationStatistics(GameRegistry.Type.THIMBLE).push("th_hitblock", p.getPlayer().getLocation())));}
                    {put("thimble", new StatType("th_thimble", SessionStatRegistry.Sorter.HIGHEST_SCORE, false, 0, 0, GameRegistry.Type.THIMBLE));}
                    {put("win", new StatType("th_win", SessionStatRegistry.Sorter.HIGHEST_SCORE, true, 25, 0, GameRegistry.Type.THIMBLE));}
                    {put("loss", new StatType("th_loss", SessionStatRegistry.Sorter.HIGHEST_SCORE, true, 0, 0, GameRegistry.Type.THIMBLE));}
                    {put("gamesPlayed", new StatType("th_gamesplayed", SessionStatRegistry.Sorter.HIGHEST_SCORE, true, 10, 0, GameRegistry.Type.THIMBLE));}
                    {put("timePlayed", new StatType("th_timeplayed", SessionStatRegistry.Sorter.HIGHEST_SCORE,true, 0, 0,
                            (p, o) -> p.getPersistentStats().getLongStatistics(GameRegistry.Type.THIMBLE).increment("th_timeplayed", (Long) o)));}
                })
                .informationBook(new InformationBook(Babel.translate("thimble_information_title"), "thimble_information_page1", "thimble_information_page2"))
                .build());
    }

}
