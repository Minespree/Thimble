package net.minespree.thimble.extension;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Getter;
import net.minespree.babel.Babel;
import net.minespree.babel.BabelMessage;
import net.minespree.feather.player.NetworkPlayer;
import net.minespree.pirate.cosmetics.Cosmetic;
import net.minespree.pirate.cosmetics.CosmeticManager;
import net.minespree.pirate.cosmetics.CosmeticSortType;
import net.minespree.pirate.cosmetics.CosmeticType;
import net.minespree.pirate.cosmetics.games.thimble.LandBlockCosmetic;
import net.minespree.rise.RisePlugin;
import net.minespree.rise.states.WaitingState;
import net.minespree.wizard.gui.MultiPageGUI;
import net.minespree.wizard.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ColourExtension implements WaitingState.Extension, Listener {

    private static final BabelMessage TH_USING = Babel.translate("th_using");

    @Getter
    private static final ColourExtension instance = new ColourExtension();

    @Getter
    private Map<UUID, LandBlockCosmetic> using = Maps.newHashMap();
    private MultiPageGUI blockGui;
    private ItemBuilder colourBuilder;

    @Override
    public void onWaitingStateStart() {
        using.clear();
        blockGui = new MultiPageGUI(Babel.translate("th_landblocks"), MultiPageGUI.PageFormat.RECTANGLE3, 45, 44, 36);
        for (Cosmetic cosmetic : CosmeticManager.getCosmeticManager().getCosmeticsByType(CosmeticType.TH_LAND_BLOCK)) {
            blockGui.addItem(player -> {
                ItemStack item = cosmetic.getItemBuilder().build(player);
                if(!cosmetic.has(player)) {
                    item.setType(Material.INK_SACK);
                    item.setDurability((short) 8);
                }
                ItemMeta meta = item.getItemMeta();
                List<String> lore = meta.hasLore() ? meta.getLore() : Lists.newArrayList();
                for (UUID uuid : using.keySet()) {
                    if(using.get(uuid) == cosmetic) {
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                        meta.addEnchant(Enchantment.DURABILITY, 1, true);
                        lore.add(" ");
                        if(Bukkit.getPlayer(uuid) != null && Bukkit.getPlayer(uuid).isOnline()) {
                            lore.add(TH_USING.toString(player, NetworkPlayer.of(Bukkit.getPlayer(uuid)).getName()));
                        }
                        break;
                    }
                }
                meta.setLore(lore);
                item.setItemMeta(meta);
                return item;
            }, cosmetic, (player, type) -> {
                if(using.values().contains(cosmetic)) {
                    if(using.containsKey(player.getUniqueId()) && using.get(player.getUniqueId()) == cosmetic) {
                        using.remove(player.getUniqueId());
                        blockGui.refresh();
                    }
                } else if(cosmetic.has(player)) {
                    using.put(player.getUniqueId(), (LandBlockCosmetic) cosmetic);
                    blockGui.refresh();
                }
            });
        }
        colourBuilder = new ItemBuilder(Material.NETHER_STAR)
                .displayName(Babel.translate("th_colourselector"));

        Bukkit.getPluginManager().registerEvents(this, RisePlugin.getPlugin());
    }

    @Override
    public void onWaitingStateStop() {
        HandlerList.unregisterAll(this);

        Bukkit.getOnlinePlayers().forEach(player -> {
            if(!using.containsKey(player.getUniqueId())) {
                select(player);
            }
        });
        blockGui.unregister();
    }

    private void selectDefault(Player player) {
        Optional<Cosmetic> cosmetic = CosmeticManager.getCosmeticManager().getSelectedCosmetic(player, CosmeticType.TH_LAND_BLOCK);
        cosmetic.ifPresent(c -> {
            if(!using.values().contains(c)) {
                using.put(player.getUniqueId(), (LandBlockCosmetic) c);
                blockGui.refresh();
            }
        });
    }

    public void select(Player player) {
        selectDefault(player);
        if(!using.containsKey(player.getUniqueId())) {
            for (Cosmetic c : CosmeticManager.getCosmeticManager().getOwnedCosmeticsByType(player, CosmeticType.TH_LAND_BLOCK)) {
                if(!using.values().contains(c)) {
                    using.put(player.getUniqueId(), (LandBlockCosmetic) c);
                    blockGui.refresh();
                    break;
                }
            }
        }
    }

    @Override
    public void onJoin(Player player) {
        player.getInventory().setItem(0, colourBuilder.build(player));

        selectDefault(player);
    }

    @Override
    public void onLeave(Player player) {
        if(using.containsKey(player.getUniqueId())) {
            using.remove(player.getUniqueId());
            blockGui.refresh();
        }
    }

    @Override
    public void onCountdownStart() {

    }

    @Override
    public void onCountdownAbort() {

    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (event.getPlayer().getInventory().getHeldItemSlot() == 0) {
                    blockGui.open(event.getPlayer(), CosmeticSortType.ALPHABETICAL_OWNED.getMethod());
            }
        }
    }

}
