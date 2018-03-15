package net.minespree.thimble;

import net.minespree.babel.Babel;
import net.minespree.babel.BabelMessage;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.rise.RisePlugin;
import net.minespree.rise.features.ScoreboardFeature;
import net.minespree.thimble.state.ThimblePlayState;
import net.minespree.wizard.util.Chat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class ThimbleScoreboardFeature extends ScoreboardFeature {

    private final static int LEADERBOARD_SIZE = 5;

    private BabelMessage TH_JUMPSLEFT = Babel.translate("th_jumpsleft");

    public ThimbleScoreboardFeature(Plugin plugin) {
        super(plugin, Chat.AQUA + Chat.BOLD + "Thimble");
    }

    @Override
    public void initialize(Player player) {
        ThimblePlayState state = (ThimblePlayState) RisePlugin.getPlugin().getGameStateManager().getCurrentState();
        setScore(player, "ip", ChatColor.GOLD + "play.minespree.net", 0);
        setScore(player, "gameId", ChatColor.GRAY + Bukkit.getServerName(), 1);
        setScore(player, "blank1", " ", 2);
        setScore("roundLeft", Chat.YELLOW + (11 - (state.getRound() == 0 ? 1 : state.getRound())), 3);
        setScore("rounds", TH_JUMPSLEFT, 4);
        setScore("blank2", "  ", 5);
        setScore(player, "personal", Chat.GREEN + NetworkPlayer.of(player).getName() + " " + Chat.YELLOW + state.getData(player).getPoints(), 6);
        setScore("blank3", "   ", 7);
        setScore("blank4", "     ", 14);

        updateScore(player, 0);
    }

    public void updateScore(Player player, int points) {
        setScore(player, "personal", Chat.GREEN + NetworkPlayer.of(player).getName() + " " + Chat.YELLOW + points, 6);
    }

    public void updateRounds(int rounds) {
        updateScore("roundLeft", Chat.YELLOW + (11 - rounds));
    }

    public void updateLeaderboard() {
        ThimblePlayState state = (ThimblePlayState) RisePlugin.getPlugin().getGameStateManager().getCurrentState();
        List<ThimblePlayState.PlayerData> top5 = state.getTop5();
        for (int i = 0; i < top5.size(); i++) {
            removeScore("lb" + i); // Remove score in-case goes from 5 to 4 players
        }
        for (int i = 0; i < top5.size(); i++) {
            if(i < top5.size()) {
                ThimblePlayState.PlayerData data = top5.get(i);
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if(data.getPlayer() == null || !data.getPlayer().isOnline())
                        continue;
                    if (data.getPosition() < LEADERBOARD_SIZE) {
                        if (player.equals(data.getPlayer())) {
                            removeScore(player, "personal");
                            hide(player, "blank2");
                        }
                        setScore(player, "lb" + data.getPosition(), (player.equals(data.getPlayer()) ? Chat.GREEN : Chat.GRAY) + NetworkPlayer.of(data.getPlayer()).getName() + " " + Chat.YELLOW + data.getPoints() + " ", 7 + (5 - data.getPosition()));
                    } else {
                        if (player.equals(data.getPlayer())) {
                            updateScore(player, data.getPoints());
                            show(player, "blank2");
                        }
                    }
                }
            }
        }
    }

}
