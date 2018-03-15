package net.minespree.thimble.state;

import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.minespree.babel.Babel;
import net.minespree.babel.BabelMessage;
import net.minespree.babel.ComplexBabelMessage;
import net.minespree.cartographer.util.GameArea;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.pirate.cosmetics.Cosmetic;
import net.minespree.pirate.cosmetics.CosmeticManager;
import net.minespree.pirate.cosmetics.CosmeticType;
import net.minespree.pirate.cosmetics.games.thimble.JumpTrailCosmetic;
import net.minespree.pirate.cosmetics.games.thimble.LandBlockCosmetic;
import net.minespree.pirate.cosmetics.games.thimble.LandCosmetic;
import net.minespree.pirate.cosmetics.games.thimble.MarkerCosmetic;
import net.minespree.rise.RisePlugin;
import net.minespree.rise.states.GameState;
import net.minespree.thimble.ThimblePlugin;
import net.minespree.thimble.extension.ColourExtension;
import net.minespree.wizard.util.FireworkUtil;
import net.minespree.wizard.util.MessageUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

import java.util.*;

public class ThimblePlayState extends ThimbleState {

    private static final BabelMessage TH_HITBLOCK = Babel.translate("th_hitblock");
    private static final BabelMessage TH_LANDED = Babel.translate("th_landed");
    private static final BabelMessage TH_NEXTJUMPER = Babel.translate("th_nextjumper");
    private static final BabelMessage TH_JUMPSOON = Babel.translate("th_jumpsoon");
    private static final BabelMessage TH_NEXTROUND = Babel.translate("th_nextround");
    private static final BabelMessage TH_WINNER = Babel.translate("th_winner");
    private static final BabelMessage TH_JUMPMISSED = Babel.translate("th_jumpmissed");
    private static final BabelMessage TH_THIMBLE = Babel.translate("th_thimble");
    private static final BabelMessage TH_GOT_THIMBLE = Babel.translate("th_got_thimble");

    private final BlockFace[] relativeChecks = new BlockFace[] {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};

    private List<UUID> orderedJumpers = new ArrayList<>();
    private List<UUID> newJumpers = new ArrayList<>();
    @Getter
    private List<PlayerData> points = new ArrayList<>();

    private Player jumper;
    private Location lastLocation;

    private int tick, currentIdx, jumperY;
    @Getter
    private int round;
    private boolean check, stopMoving, firstRoundStarted = false;

    @Override
    public void onStart(GameState previous) {
        extension = ColourExtension.getInstance();
        scoreboard.onStart();
        Bukkit.getOnlinePlayers().forEach(player -> {
            orderedJumpers.add(player.getUniqueId());
            getData(player);
            player.teleport(map.getWaitingLocation().toLocation());
            player.setGameMode(GameMode.ADVENTURE);
            player.getInventory().clear();
            player.getInventory().setHelmet(extension.getUsing().get(player.getUniqueId()).build());
        });

        currentIdx = 0;
        round = 1;

        initialize();
        scoreboard.updateLeaderboard();
        Bukkit.getScheduler().runTaskLater(RisePlugin.getPlugin(), this::nextJumper, 100L);
    }

    @Override
    public void onJoin(Player player) {
        getData(player).setPlayer(player); // add to points if not already exists
        player.getInventory().clear();

        Bukkit.getOnlinePlayers().forEach(p -> {
            p.hidePlayer(player);
            p.showPlayer(player);
            player.hidePlayer(p);
            player.showPlayer(p);
        });

        player.setGameMode(GameMode.ADVENTURE);

        scoreboard.onStart(player);

        scoreboard.updateLeaderboard();

        newJumpers.add(player.getUniqueId());

        extension.select(player);
        player.getInventory().setHelmet(extension.getUsing().get(player.getUniqueId()).build());

        player.teleport(map.getWaitingLocation().toLocation());
    }

    @Override
    public void onQuit(Player player) {
        newJumpers.remove(player.getUniqueId());
        int jumperIdx = orderedJumpers.indexOf(player.getUniqueId());
        if(jumperIdx != -1) {
            if (jumperIdx <= currentIdx) {
                // jumper was in line before the current player, or is currently jumping
                currentIdx--;
            }
            orderedJumpers.remove(jumperIdx);
            if(jumper == player && Bukkit.getOnlinePlayers().size() - 1 > 1) {
                nextJumper();
            }
            scoreboard.onStop(player);
        }
        if(Bukkit.getOnlinePlayers().size() - 1 <= 1) {
            check = false;
            end();
        }
    }

    @Override
    protected void tick() {
        if(!check)
            return;
        if(tick % 20 == 0) {
            jumper.setLevel(15 - (tick / 20));
        }
        if(tick >= 300) {
            TH_JUMPMISSED.broadcast(jumper.getName());
            nextJumper();
        } else if(tick >= 200 && tick % 20 == 0) {
            MessageUtil.sendActionBar(jumper, TH_JUMPSOON);
            jumper.playSound(jumper.getLocation(), Sound.NOTE_PLING, 1F, 1F);
        }
        tick++;
    }

    private void nextJumper() {
        check = false;
        tick = 0;
        if(jumper != null && jumper.isOnline()) {
            jumper.teleport(lastLocation);
            jumper.setLevel(0);
            game.changeStatistic(jumper, "jumps", 1);
            if(waterFull()) {
                end();
                return;
            }
            jumper = null;
        } else {
            TH_NEXTROUND.broadcast(round);
        }
        if(currentIdx + 1 >= orderedJumpers.size()) {
            orderedJumpers.addAll(newJumpers);
            newJumpers.clear();
            Collections.shuffle(orderedJumpers);
            currentIdx = 0;
            round++;
            scoreboard.updateRounds(round);
            if(round > 10) {
                end();
                return;
            }
            TH_NEXTROUND.broadcast(round);
        } else {
            if (firstRoundStarted) {
                currentIdx++;
            } else {
                firstRoundStarted = true;
            }
        }
        Bukkit.getScheduler().runTaskLater(RisePlugin.getPlugin(), () -> {
            // rare edge case, but it's a weird one
            if (currentIdx < 0) {
                if (Bukkit.getOnlinePlayers().size() == 1) {
                    end();
                    return;
                }
                currentIdx = 0;
            }
            jumper = Bukkit.getPlayer(orderedJumpers.get(currentIdx));
            lastLocation = jumper.getLocation();
            Bukkit.getScheduler().runTaskLater(ThimblePlugin.getPlugin(), () -> {
                jumper.setFallDistance(0.0f);
                jumper.teleport(map.getJumpLocation().toLocation());
                jumper.playSound(jumper.getLocation(), Sound.BLAZE_HIT, 1F, 1.5F);
                CosmeticManager.getCosmeticManager().getSelectedCosmetic(jumper, CosmeticType.TH_JUMP_TRAIL)
                        .ifPresent(cosmetic -> ((JumpTrailCosmetic) cosmetic).use(jumper));
                stopMoving = true;
                Bukkit.getScheduler().runTaskLater(ThimblePlugin.getPlugin(), () -> stopMoving = false, 20L);
                jumperY = jumper.getLocation().getBlockY();
                TH_NEXTJUMPER.broadcast(NetworkPlayer.of(jumper).getName());
                check = true;
            }, 1L);
        }, 60L);
    }

    public void end() {
        Player winner =  getTop5().get(0).getPlayer();
        game.changeStatistic(winner, "win", 1);
        Bukkit.getOnlinePlayers().stream().filter(player -> !player.equals(winner)).forEach(player -> game.changeStatistic(player, "loss", 1));
        game.endGame(Bukkit.getOnlinePlayers().size() - 1 == 0 ? null : new ComplexBabelMessage().append(TH_WINNER, NetworkPlayer.of(winner).getName()), winner.getName());
    }

    public List<PlayerData> getTop5() {
        List<PlayerData> sortedList = new ArrayList<>(points);
        sortedList.sort((o1, o2) -> o2.getPoints().compareTo(o1.getPoints()));
        for (int i = 0; i < sortedList.size(); i++) {
            sortedList.get(i).setPosition(i);
        }
        return sortedList;
    }

    private boolean waterFull() {
        GameArea waterArea = map.getWaterArea();
        for(double y = waterArea.getYMin(); y <= waterArea.getYMax(); y++) {
            for(double x = waterArea.getXMin(); x <= waterArea.getXMax(); x++) {
                for(double z = waterArea.getZMin(); z <= waterArea.getZMax(); z++) {
                    if(jumper != null) {
                        Material type = jumper.getWorld().getBlockAt((int) x, (int) y, (int) z).getType();
                        if(type == Material.WATER || type == Material.STATIONARY_WATER) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    public PlayerData getData(Player player) {
        for (PlayerData point : points) {
            if(point.getPlayer().getUniqueId().equals(player.getUniqueId()))
                return point;
        }
        PlayerData data = new PlayerData(player);
        points.add(data);
        return data;
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if(entity instanceof Player && entity == jumper && event.getCause() == EntityDamageEvent.DamageCause.FALL && check && entity.getLocation().getBlockY() < jumperY) {
            check = false;
            TH_HITBLOCK.broadcast(NetworkPlayer.of((Player) entity).getName(), getData((Player) event.getEntity()).getPoints());
            game.changeStatistic((Player) entity, "hitblock", 1);
            Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1F, 1F));
            nextJumper();
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if(event.getTo().getBlockY() <= 0) {
            event.getPlayer().teleport(map.getWaitingLocation().toLocation());
            return;
        }
        if(jumper == event.getPlayer() && check) {
            if(stopMoving && event.getTo().distance(event.getFrom()) != 0.0) {
                event.setTo(event.getFrom());
                return;
            }
            Block block = event.getTo().getBlock();
            if((block.getType() == Material.WATER || block.getType() == Material.STATIONARY_WATER) && event.getPlayer().getLocation().getBlockY() < jumperY) {
                check = false;
                Player player = event.getPlayer();
                int points = 1;
                for (BlockFace blockFace : relativeChecks) {
                    Block relative = block.getRelative(blockFace);
                    if(relative.getType() != Material.WATER && relative.getType() != Material.STATIONARY_WATER) {
                        points++;
                    }
                }
                boolean thimble = false;
                if(points == 5) {
                    thimble = true;
                    game.changeStatistic(player, "thimble", 1);
                }
                game.changeStatistic(player, "successful", 1, 4 + points + (thimble ? 1 : 0));
                LandBlockCosmetic c = extension.getUsing().get(player.getUniqueId());
                c.build(block.getLocation());
                getData(player).setPoints(getData(player).getPoints() + points);
                scoreboard.updateScore(player, getData(player).getPoints());
                scoreboard.updateLeaderboard();
                ComplexBabelMessage landMessage = new ComplexBabelMessage().append(TH_LANDED, NetworkPlayer.of(player).getName(), points, getData(player).getPoints());
                if(thimble) {
                    landMessage.append(TH_GOT_THIMBLE);
                }
                landMessage.broadcast();
                Location jumperLocation = jumper.getEyeLocation().clone();
                nextJumper();
                Optional<Cosmetic> succLanding = CosmeticManager.getCosmeticManager().getSelectedCosmetic(player, CosmeticType.TH_SUCCESSFUL_LAND);
                if(thimble) {
                    CosmeticManager.getCosmeticManager().getSelectedCosmetic(player, CosmeticType.TH_MARKER)
                            .ifPresent(cosmetic -> ((MarkerCosmetic) cosmetic).build(player, block.getLocation().add(0, 1, 0)));
                }
                CosmeticManager.getCosmeticManager().getSelectedCosmetic(player, CosmeticType.TH_JUMP_TRAIL)
                        .ifPresent(cosmetic -> ((JumpTrailCosmetic) cosmetic).stopUsing(player));
                if(succLanding.isPresent()) {
                    ((LandCosmetic) succLanding.get()).land(player, block.getLocation());
                } else {
                    FireworkUtil.randomFirework(jumperLocation, 3, 1);
                    Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), Sound.LEVEL_UP, 1F, 1F));
                }
            }
        }
    }

    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onItemPickup(PlayerPickupItemEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        if (event.toWeatherState()) event.setCancelled(true);
    }

    @Data @Getter
    public class PlayerData {

        @Setter @NonNull
        private Player player;
        @Setter
        private Integer points = 0;
        @Setter
        private int position;

    }
}
