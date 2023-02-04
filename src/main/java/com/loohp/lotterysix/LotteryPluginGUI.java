/*
 * This file is part of LotterySix.
 *
 * Copyright (C) 2023. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2023. Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.loohp.lotterysix;

import com.cryptomorin.xseries.XMaterial;
import com.loohp.lotterysix.game.LotterySix;
import com.loohp.lotterysix.game.lottery.CompletedLotterySixGame;
import com.loohp.lotterysix.game.lottery.GameNumber;
import com.loohp.lotterysix.game.lottery.PlayableLotterySixGame;
import com.loohp.lotterysix.game.objects.AddBetResult;
import com.loohp.lotterysix.game.objects.BetUnitType;
import com.loohp.lotterysix.game.objects.PlayerBets;
import com.loohp.lotterysix.game.objects.PlayerPreferenceKey;
import com.loohp.lotterysix.game.objects.PlayerWinnings;
import com.loohp.lotterysix.game.objects.betnumbers.BetNumbers;
import com.loohp.lotterysix.game.objects.betnumbers.BetNumbersBuilder;
import com.loohp.lotterysix.game.objects.betnumbers.BetNumbersType;
import com.loohp.lotterysix.utils.BookUtils;
import com.loohp.lotterysix.utils.ChatColorUtils;
import com.loohp.lotterysix.utils.LotteryUtils;
import com.loohp.lotterysix.utils.StringUtils;
import de.themoep.inventorygui.DynamicGuiElement;
import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.GuiStateElement;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import io.github.bananapuncher714.nbteditor.NBTEditor;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

public class LotteryPluginGUI implements Listener {

    private static String[] fillChars(int arrays) {
        String[] strings = new String[arrays];
        char c = 'a';
        for (int i = 0; i < strings.length; i++) {
            StringBuilder sb = new StringBuilder(9);
            for (int u = 0; u < 9; u++) {
                sb.append(c++);
            }
            strings[i] = sb.toString();
        }
        return strings;
    }

    private static ItemStack getNumberItem(int number) {
        return getNumberItem(number, false);
    }

    private static ItemStack getNumberItem(int number, boolean enchanted) {
        XMaterial material;
        ChatColor color = ChatColorUtils.getNumberColor(number);
        if (color.equals(ChatColor.RED)) {
            material = XMaterial.RED_WOOL;
        } else if (color.equals(ChatColor.AQUA)) {
            material = XMaterial.LIGHT_BLUE_WOOL;
        } else if (color.equals(ChatColor.GREEN)) {
            material = XMaterial.LIME_WOOL;
        } else {
            material = XMaterial.RED_WOOL;
        }
        ItemStack itemStack = material.parseItem();
        itemStack.setAmount(number);
        if (enchanted) {
            itemStack.addUnsafeEnchantment(Enchantment.LUCK, 10);
            ItemMeta itemMeta = itemStack.getItemMeta();
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            itemStack.setItemMeta(itemMeta);
        }
        return NBTEditor.set(itemStack, number, "LotterySixNumber");
    }

    private static ItemStack setItemSize(ItemStack item, int amount) {
        if (item == null) {
            return null;
        }
        item.setAmount(amount);
        return item;
    }

    private static ChatColor getNumberColor(int number) {
        return ChatColorUtils.getNumberColor(number);
    }

    private final LotterySixPlugin plugin;
    private final LotterySix instance;

    public LotteryPluginGUI(LotterySixPlugin plugin) {
        this.plugin = plugin;
        this.instance = LotterySixPlugin.getInstance();
    }

    public void forceClose(Player player) {
        InventoryGui gui = InventoryGui.getOpen(player);
        if (gui != null) {
            gui.close(player, true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Deque<InventoryGui> guis = InventoryGui.clearHistory(event.getPlayer());
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            InventoryGui gui;
            while ((gui = guis.poll()) != null) {
                gui.destroy();
            }
        }, 1);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getView().getTopInventory();
        if (inventory.getHolder() instanceof InventoryGui.Holder && InventoryGui.getOpen(event.getWhoClicked()) == null) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                InventoryGui.clearHistory(event.getWhoClicked());
                event.getWhoClicked().closeInventory();
            }, 1);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory inventory = event.getView().getTopInventory();
        if (inventory.getHolder() instanceof InventoryGui.Holder && InventoryGui.getOpen(event.getWhoClicked()) == null) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                InventoryGui.clearHistory(event.getWhoClicked());
                event.getWhoClicked().closeInventory();
            }, 1);
        }
    }

    public void close(HumanEntity player, InventoryGui inventoryGui, boolean back) {
        inventoryGui.close(player, !back);
        Bukkit.getScheduler().runTaskLater(plugin, () -> inventoryGui.destroy(), 1);
    }

    public void checkReopen(HumanEntity player) {
        if (instance.getPlayerPreferenceManager().getLotteryPlayer(player.getUniqueId()).getPreference(PlayerPreferenceKey.REOPEN_MENU_ON_PURCHASE, boolean.class)) {
            getMainMenu((Player) player).show(player);
        }
    }

    public InventoryGui getMainMenu(Player player) {
        String[] guiSetup = {
                "         ",
                " aaaaaaa ",
                " abacada ",
                " aaaaaaa ",
                "        z"
        };
        InventoryGui gui = new InventoryGui(plugin, LotteryUtils.formatPlaceholders(player, instance.guiMainMenuTitle, instance), guiSetup);
        gui.setFiller(XMaterial.YELLOW_STAINED_GLASS_PANE.parseItem());
        gui.addElement(new StaticGuiElement('a', new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));
        gui.addElement(new StaticGuiElement('b', XMaterial.CLOCK.parseItem(), click -> {
            Bukkit.getScheduler().runTaskLater(plugin, () -> getPastResults((Player) click.getWhoClicked(), instance.getCompletedGames().isEmpty() ? null : instance.getCompletedGames().get(0)).show(click.getWhoClicked()), 1);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.guiMainMenuCheckPastResults, instance)));
        gui.addElement(new DynamicGuiElement('c', viewer -> {
            PlayableLotterySixGame currentGame = instance.getCurrentGame();
            if (currentGame == null) {
                return new StaticGuiElement('c', XMaterial.BARRIER.parseItem(), LotteryUtils.formatPlaceholders(player, instance.guiMainMenuNoLotteryGamesScheduled, instance));
            } else {
                return new StaticGuiElement('c', XMaterial.PAPER.parseItem(), click -> {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        close(click.getWhoClicked(), click.getGui(), true);

                        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                            ItemStack itemStack = getPlacedBets((Player) click.getWhoClicked());
                            Bukkit.getScheduler().runTaskLater(plugin, () -> BookUtils.openBook((Player) click.getWhoClicked(), itemStack), 1);
                        }, 1);
                    }, 1);
                    return true;
                }, LotteryUtils.formatPlaceholders(player, instance.guiMainMenuCheckOwnBets, instance, currentGame));
            }
        }));
        gui.addElement(new DynamicGuiElement('d', viewer -> {
            PlayableLotterySixGame currentGame = instance.getCurrentGame();
            if (currentGame == null) {
                return new StaticGuiElement('d', XMaterial.RED_WOOL.parseItem(), LotteryUtils.formatPlaceholders(player, instance.guiMainMenuNoLotteryGamesScheduled, instance));
            } else {
                return new StaticGuiElement('d', XMaterial.GOLD_INGOT.parseItem(), click -> {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> getBetTypeChooser((Player) click.getWhoClicked(), instance.getCurrentGame()).show(click.getWhoClicked()), 1);
                    return true;
                }, LotteryUtils.formatPlaceholders(player, instance.guiMainMenuPlaceNewBets, instance, currentGame));
            }
        }));
        gui.addElement(new StaticGuiElement('z', XMaterial.LIME_STAINED_GLASS_PANE.parseItem(), click -> {
            TextComponent message = new TextComponent(LotteryUtils.formatPlaceholders(player, instance.explanationMessage, instance));
            message.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, instance.explanationURL));
            click.getWhoClicked().spigot().sendMessage(message);
            Bukkit.getScheduler().runTaskLater(plugin, () -> close(click.getWhoClicked(), click.getGui(), true), 1);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.explanationGUIItem, instance)));
        gui.setCloseAction(close -> false);
        return gui;
    }

    public ItemStack getPlacedBets(Player player) {
        PlayableLotterySixGame game = instance.getCurrentGame();
        List<PlayerBets> bets = game.getPlayerBets(player.getUniqueId());

        ItemStack itemStack = XMaterial.WRITTEN_BOOK.parseItem();
        BookMeta meta = (BookMeta) itemStack.getItemMeta();
        meta.setAuthor("LotterySix");
        meta.setTitle("LotterySix");
        List<String> pages = new ArrayList<>();

        String title = LotteryUtils.formatPlaceholders(player, instance.guiYourBetsTitle, instance, game);
        if (bets.isEmpty()) {
            pages.add(title + "\n\n" + String.join("\n", LotteryUtils.formatPlaceholders(null, instance.guiYourBetsNothing, instance, game)));
        } else {
            for (PlayerBets bet : bets) {
                pages.add(title + "\n\n" + bet.getChosenNumbers().toColoredString() + "\n" + ChatColor.GOLD + "$" + bet.getBet() + " ($" + (instance.pricePerBet / bet.getType().getDivisor()) + ")");
            }
        }

        meta.setPages(pages);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public InventoryGui getBetTypeChooser(Player player, PlayableLotterySixGame game) {
        String[] guiSetup = {
                "         ",
                " aaaaaaa ",
                " bacadae ",
                " aaaaaaa ",
                "         "
        };
        InventoryGui gui = new InventoryGui(plugin, LotteryUtils.formatPlaceholders(null, instance.guiSelectNewBetTypeTitle, instance), guiSetup);
        gui.setFiller(XMaterial.ORANGE_STAINED_GLASS_PANE.parseItem());
        gui.addElement(new StaticGuiElement('a', new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));
        gui.addElement(new StaticGuiElement('b', XMaterial.BRICK.parseItem(), click -> {
            Bukkit.getScheduler().runTaskLater(plugin, () -> getNumberChooser((Player) click.getWhoClicked(), game, BetNumbersBuilder.single(1, instance.numberOfChoices)).show(click.getWhoClicked()), 1);
            return true;
        }, LotteryUtils.formatPlaceholders(null, instance.guiSelectNewBetTypeSingle, instance)));
        gui.addElement(new StaticGuiElement('c', XMaterial.IRON_INGOT.parseItem(), click -> {
            Bukkit.getScheduler().runTaskLater(plugin, () -> getNumberChooser((Player) click.getWhoClicked(), game, BetNumbersBuilder.multiple(1, instance.numberOfChoices)).show(click.getWhoClicked()), 1);
            return true;
        }, LotteryUtils.formatPlaceholders(null, instance.guiSelectNewBetTypeMultiple, instance)));
        gui.addElement(new StaticGuiElement('d', XMaterial.GOLD_INGOT.parseItem(), click -> {
            Bukkit.getScheduler().runTaskLater(plugin, () -> getNumberChooser((Player) click.getWhoClicked(), game, BetNumbersBuilder.banker(1, instance.numberOfChoices)).show(click.getWhoClicked()), 1);
            return true;
        }, LotteryUtils.formatPlaceholders(null, instance.guiSelectNewBetTypeBanker, instance)));
        gui.addElement(new StaticGuiElement('e', XMaterial.REDSTONE.parseItem(), click -> {
            Bukkit.getScheduler().runTaskLater(plugin, () -> getRandomEntryCountChooser((Player) click.getWhoClicked(), game).show(click.getWhoClicked()), 1);
            return true;
        }, LotteryUtils.formatPlaceholders(null, instance.guiSelectNewBetTypeRandom, instance)));
        gui.setSilent(true);
        return gui;
    }

    public InventoryGui getRandomEntryCountChooser(Player player, PlayableLotterySixGame game) {
        String[] guiSetup = {
                "         ",
                " aaaaaaa ",
                " bacadae ",
                " aaaaaaa ",
                "         "
        };
        InventoryGui gui = new InventoryGui(plugin, LotteryUtils.formatPlaceholders(null, instance.guiRandomEntryCountTitle, instance), guiSetup);
        gui.setFiller(XMaterial.RED_STAINED_GLASS_PANE.parseItem());
        gui.addElement(new StaticGuiElement('a', new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));
        gui.addElement(new StaticGuiElement('b', setItemSize(XMaterial.PAPER.parseItem(), 1), click -> {
            Bukkit.getScheduler().runTaskLater(plugin, () -> close(click.getWhoClicked(), click.getGui(), false), 1);
            Bukkit.getScheduler().runTaskLater(plugin, () -> getSingleNumberConfirm((Player) click.getWhoClicked(), game, BetNumbersBuilder.random(1, instance.numberOfChoices).build()).show(click.getWhoClicked()), 2);
            return true;
        }, LotteryUtils.formatPlaceholders(null, instance.guiRandomEntryCountValue.replace("{Count}", "1"), instance)));
        gui.addElement(new StaticGuiElement('c', setItemSize(XMaterial.PAPER.parseItem(), 2), click -> {
            Bukkit.getScheduler().runTaskLater(plugin, () -> close(click.getWhoClicked(), click.getGui(), false), 1);
            Bukkit.getScheduler().runTaskLater(plugin, () -> getBulkNumberConfirm((Player) click.getWhoClicked(), game, BetNumbersBuilder.buildBulkRandom(1, instance.numberOfChoices, 2)).show(click.getWhoClicked()), 2);
            return true;
        }, LotteryUtils.formatPlaceholders(null, instance.guiRandomEntryCountValue.replace("{Count}", "2"), instance)));
        gui.addElement(new StaticGuiElement('d', setItemSize(XMaterial.PAPER.parseItem(), 5), click -> {
            Bukkit.getScheduler().runTaskLater(plugin, () -> close(click.getWhoClicked(), click.getGui(), false), 1);
            Bukkit.getScheduler().runTaskLater(plugin, () -> getBulkNumberConfirm((Player) click.getWhoClicked(), game, BetNumbersBuilder.buildBulkRandom(1, instance.numberOfChoices, 5)).show(click.getWhoClicked()), 2);
            return true;
        }, LotteryUtils.formatPlaceholders(null, instance.guiRandomEntryCountValue.replace("{Count}", "5"), instance)));
        gui.addElement(new StaticGuiElement('e', setItemSize(XMaterial.PAPER.parseItem(), 10), click -> {
            Bukkit.getScheduler().runTaskLater(plugin, () -> close(click.getWhoClicked(), click.getGui(), false), 1);
            Bukkit.getScheduler().runTaskLater(plugin, () -> getBulkNumberConfirm((Player) click.getWhoClicked(), game, BetNumbersBuilder.buildBulkRandom(1, instance.numberOfChoices, 10)).show(click.getWhoClicked()), 2);
            return true;
        }, LotteryUtils.formatPlaceholders(null, instance.guiRandomEntryCountValue.replace("{Count}", "10"), instance)));
        gui.setSilent(true);
        return gui;
    }

    public InventoryGui getNumberChooser(Player player, PlayableLotterySixGame game, BetNumbersBuilder builder) {
        int num = instance.numberOfChoices;
        String[] guiSetup = fillChars((num + 1) / 9 + 1);
        String last = guiSetup[guiSetup.length - 1];
        guiSetup[guiSetup.length - 1] = last.substring(0, last.length() - 1) + "\0";
        String title;
        boolean isBanker = builder.getType().equals(BetNumbersType.BANKER);
        boolean isFixed = builder.getType().equals(BetNumbersType.SINGLE);
        switch (builder.getType()) {
            case SINGLE: {
                title = instance.guiNewBetSingleTitle;
                break;
            }
            case MULTIPLE: {
                title = instance.guiNewBetMultipleTitle;
                break;
            }
            case BANKER: {
                title = instance.guiNewBetBankerTitle;
                break;
            }
            default: {
                throw new RuntimeException(builder.getType() + " type does not need choosing");
            }
        }
        InventoryGui gui = new InventoryGui(plugin, LotteryUtils.formatPlaceholders(player, title.replace("{Price}", LotteryUtils.calculatePrice(builder, instance) + "").replace("{MinSelection}", ((isBanker ? ((BetNumbersBuilder.BankerBuilder) builder).getMinSelectionsNeeded() : 6) - builder.size()) + ""), instance, game), guiSetup);
        char c = 'a';
        for (int i = 0; i < num; i++) {
            int number = i + 1;
            GuiStateElement element = new GuiStateElement(c++,
                    new GuiStateElement.State(
                            change -> {
                                builder.addNumber(number);
                                if (builder.completed() || (isBanker && !((BetNumbersBuilder.BankerBuilder) builder).getBankers().isEmpty())) {
                                    if (isFixed) {
                                        Bukkit.getScheduler().runTaskLater(plugin, () -> close(change.getWhoClicked(), change.getGui(), false), 1);
                                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                            BetNumbers betNumbers = builder.build();
                                            if (betNumbers.getType().equals(BetNumbersType.SINGLE)) {
                                                getSingleNumberConfirm((Player) change.getWhoClicked(), game, betNumbers).show(change.getWhoClicked());
                                            } else {
                                                getComplexNumberConfirm((Player) change.getWhoClicked(), game, betNumbers).show(change.getWhoClicked());
                                            }
                                        }, 2);
                                    } else if (isBanker && !((BetNumbersBuilder.BankerBuilder) builder).inSelectionPhase() && ((BetNumbersBuilder.BankerBuilder) builder).bankerCompleted()) {
                                        ((BetNumbersBuilder.BankerBuilder) builder).finishBankers();
                                        gui.removeElement('\0');
                                        gui.addElement(new StaticGuiElement('\0', new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));
                                        for (int banker : ((BetNumbersBuilder.BankerBuilder) builder).getBankers()) {
                                            char bankerC = (char) ('a' + banker - 1);
                                            gui.removeElement(bankerC);
                                            gui.addElement(new StaticGuiElement(bankerC, XMaterial.MAGENTA_WOOL.parseItem(), ChatColor.LIGHT_PURPLE + "" + number));
                                        }
                                    } else if (builder.completed() || (isBanker && !((BetNumbersBuilder.BankerBuilder) builder).inSelectionPhase() && !((BetNumbersBuilder.BankerBuilder) builder).getBankers().isEmpty())) {
                                        gui.removeElement('\0');
                                        gui.addElement(new StaticGuiElement('\0', isBanker && !((BetNumbersBuilder.BankerBuilder) builder).inSelectionPhase() ? XMaterial.EMERALD_BLOCK.parseItem() : XMaterial.GOLD_BLOCK.parseItem(), click -> {
                                            if (builder.completed()) {
                                                Bukkit.getScheduler().runTaskLater(plugin, () -> close(click.getWhoClicked(), click.getGui(), false), 1);
                                                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                                    BetNumbers betNumbers = builder.build();
                                                    if (betNumbers.getType().equals(BetNumbersType.SINGLE)) {
                                                        getSingleNumberConfirm((Player) click.getWhoClicked(), game, betNumbers).show(click.getWhoClicked());
                                                    } else {
                                                        getComplexNumberConfirm((Player) click.getWhoClicked(), game, betNumbers).show(click.getWhoClicked());
                                                    }
                                                }, 2);
                                            } else if (isBanker && !((BetNumbersBuilder.BankerBuilder) builder).inSelectionPhase() && !((BetNumbersBuilder.BankerBuilder) builder).getBankers().isEmpty()) {
                                                ((BetNumbersBuilder.BankerBuilder) builder).finishBankers();
                                                gui.removeElement('\0');
                                                gui.addElement(new StaticGuiElement('\0', new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));
                                                for (int banker : ((BetNumbersBuilder.BankerBuilder) builder).getBankers()) {
                                                    char bankerC = (char) ('a' + banker - 1);
                                                    gui.removeElement(bankerC);
                                                    gui.addElement(new StaticGuiElement(bankerC, XMaterial.MAGENTA_WOOL.parseItem(), ChatColor.LIGHT_PURPLE + "" + number));
                                                }
                                                Bukkit.getScheduler().runTaskLater(plugin, () -> gui.close(click.getWhoClicked()), 1);
                                                Bukkit.getScheduler().runTaskLater(plugin, () -> gui.show(click.getWhoClicked()), 2);
                                            }
                                            return true;
                                        }, LotteryUtils.formatPlaceholders(player, Arrays.stream(isBanker && !((BetNumbersBuilder.BankerBuilder) builder).inSelectionPhase() ? instance.guiNewBetFinishBankers : instance.guiNewBetFinish)
                                                .map(each -> each.replace("{Price}", LotteryUtils.calculatePrice(builder, instance) + "")).toArray(String[]::new), instance, game)));
                                    }
                                } else {
                                    gui.removeElement('\0');
                                    gui.addElement(new StaticGuiElement('\0', new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));
                                }
                            },
                            "true",
                            XMaterial.LIGHT_GRAY_WOOL.parseItem(),
                            ChatColor.GRAY + "" + number
                    ),
                    new GuiStateElement.State(
                            change -> {
                                builder.removeNumber(number);
                                GuiElement guiElement =  gui.getElement('\0');
                                if (guiElement != null) {
                                    if (isBanker) {
                                        BetNumbersBuilder.BankerBuilder bankerBuilder = (BetNumbersBuilder.BankerBuilder) builder;
                                        if (bankerBuilder.inSelectionPhase()) {
                                            if (!bankerBuilder.completed()) {
                                                gui.removeElement('\0');
                                                gui.addElement(new StaticGuiElement('\0', new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));
                                                return;
                                            }
                                        } else {
                                            if (bankerBuilder.getBankers().isEmpty()) {
                                                gui.removeElement('\0');
                                                gui.addElement(new StaticGuiElement('\0', new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));
                                                return;
                                            }
                                        }
                                    } else if (!builder.completed()) {
                                        gui.removeElement('\0');
                                        gui.addElement(new StaticGuiElement('\0', new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));
                                        return;
                                    }
                                    ((StaticGuiElement) gui.getElement('\0')).setText(LotteryUtils.formatPlaceholders(player, Arrays.stream(isBanker ? instance.guiNewBetFinishBankers : instance.guiNewBetFinish)
                                            .map(each -> each.replace("{Price}", LotteryUtils.calculatePrice(builder, instance) + "")).toArray(String[]::new), instance, game));
                                }
                            },
                            "false",
                            getNumberItem(number),
                            getNumberColor(number) + "" + number
                    )
            );
            element.setState("false");
            element.setSilent(true);
            gui.addElement(element);
        }
        gui.addElement(new StaticGuiElement('\0', new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));

        return gui;
    }

    public InventoryGui getComplexNumberConfirm(Player player, PlayableLotterySixGame game, BetNumbers betNumbers) {
        String[] guiSetup = {
                "         ",
                "   a g   ",
                "         ",
                "  h i j  ",
                "         "
        };
        long price = LotteryUtils.calculatePrice(betNumbers, instance);
        long partial = price / BetUnitType.PARTIAL.getDivisor();
        InventoryGui gui = new InventoryGui(plugin, LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetTitle, instance, game), guiSetup);
        gui.addElement(new StaticGuiElement('a', XMaterial.GOLD_BLOCK.parseItem(), StringUtils.wrapAtSpace(betNumbers.toColoredString(), 6)
                .replace("{Price}", price + "").replace("{PricePartial}", partial + "")));
        gui.addElement(new StaticGuiElement('g', XMaterial.BEACON.parseItem(), Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetLotteryInfo, instance, game))
                .map(each -> each.replace("{Price}", price + "").replace("{PricePartial}", partial + "")).toArray(String[]::new)));
        gui.addElement(new StaticGuiElement('h', XMaterial.YELLOW_WOOL.parseItem(), click -> {
            if (game != null && game.isValid()) {
                if (instance.backendBungeecordMode) {
                    LotterySixPlugin.getPluginMessageHandler().requestAddBet(player.getName(), player.getUniqueId(), partial, BetUnitType.PARTIAL, betNumbers);
                } else {
                    AddBetResult result = game.addBet(player.getName(), player.getUniqueId(), partial, BetUnitType.PARTIAL, betNumbers);
                    switch (result) {
                        case SUCCESS: {
                            player.sendMessage(instance.messageBetPlaced.replace("{Price}", partial + ""));
                            break;
                        }
                        case GAME_LOCKED: {
                            player.sendMessage(instance.messageGameLocked.replace("{Price}", partial + ""));
                            break;
                        }
                        case NOT_ENOUGH_MONEY: {
                            player.sendMessage(instance.messageNotEnoughMoney.replace("{Price}", partial + ""));
                            break;
                        }
                        case LIMIT_SELF: {
                            player.sendMessage(instance.messageBetLimitReachedSelf.replace("{Price}", partial + ""));
                            break;
                        }
                        case LIMIT_PERMISSION: {
                            player.sendMessage(instance.messageBetLimitReachedPermission.replace("{Price}", partial + ""));
                            break;
                        }
                    }
                    if (result.isSuccess()) {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> checkReopen(click.getWhoClicked()), 5);
                    }
                }
                Bukkit.getScheduler().runTaskLater(plugin, () -> close(click.getWhoClicked(), click.getGui(), false), 1);
            }
            return true;
        }, Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetPartialConfirm, instance, game))
                .map(each -> each.replace("{Price}", price + "").replace("{PricePartial}", partial + "")).toArray(String[]::new)));
        gui.addElement(new StaticGuiElement('i', XMaterial.GREEN_WOOL.parseItem(), click -> {
            if (game != null && game.isValid()) {
                if (instance.backendBungeecordMode) {
                    LotterySixPlugin.getPluginMessageHandler().requestAddBet(player.getName(), player.getUniqueId(), price, BetUnitType.FULL, betNumbers);
                } else {
                    AddBetResult result = game.addBet(player.getName(), player.getUniqueId(), price, BetUnitType.FULL, betNumbers);
                    switch (result) {
                        case SUCCESS: {
                            player.sendMessage(instance.messageBetPlaced.replace("{Price}", price + ""));
                            break;
                        }
                        case GAME_LOCKED: {
                            player.sendMessage(instance.messageGameLocked.replace("{Price}", price + ""));
                            break;
                        }
                        case NOT_ENOUGH_MONEY: {
                            player.sendMessage(instance.messageNotEnoughMoney.replace("{Price}", price + ""));
                            break;
                        }
                        case LIMIT_SELF: {
                            player.sendMessage(instance.messageBetLimitReachedSelf.replace("{Price}", price + ""));
                            break;
                        }
                        case LIMIT_PERMISSION: {
                            player.sendMessage(instance.messageBetLimitReachedPermission.replace("{Price}", price + ""));
                            break;
                        }
                    }
                    if (result.isSuccess()) {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> checkReopen(click.getWhoClicked()), 5);
                    }
                }
                Bukkit.getScheduler().runTaskLater(plugin, () -> close(click.getWhoClicked(), click.getGui(), false), 1);
            }
            return true;
        }, Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetConfirm, instance, game))
                .map(each -> each.replace("{Price}", price + "").replace("{PricePartial}", partial + "")).toArray(String[]::new)));
        gui.addElement(new StaticGuiElement('j', XMaterial.BARRIER.parseItem(), click -> {
            Bukkit.getScheduler().runTaskLater(plugin, () -> close(click.getWhoClicked(), click.getGui(), false), 1);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetCancel, instance, game)));

        return gui;
    }

    public InventoryGui getSingleNumberConfirm(Player player, PlayableLotterySixGame game, BetNumbers betNumbers) {
        String[] guiSetup = {
                "         ",
                " abcdefg ",
                "         ",
                "  h   i  ",
                "         "
        };
        long price = LotteryUtils.calculatePrice(betNumbers, instance);
        InventoryGui gui = new InventoryGui(plugin, LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetTitle, instance, game), guiSetup);
        char c = 'a';
        for (int i : betNumbers.getNumbers()) {
            gui.addElement(new StaticGuiElement(c++, getNumberItem(i), getNumberColor(i) + "" + i));
        }
        gui.addElement(new StaticGuiElement('g', XMaterial.BEACON.parseItem(), Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetLotteryInfo, instance, game))
                .map(each -> each.replace("{Price}", price + "")).toArray(String[]::new)));
        gui.addElement(new StaticGuiElement('h', XMaterial.GREEN_WOOL.parseItem(), click -> {
            if (game != null && game.isValid()) {
                if (instance.backendBungeecordMode) {
                    LotterySixPlugin.getPluginMessageHandler().requestAddBet(player.getName(), player.getUniqueId(), instance.pricePerBet, BetUnitType.FULL, betNumbers);
                } else {
                    AddBetResult result = game.addBet(player.getName(), player.getUniqueId(), instance.pricePerBet, BetUnitType.FULL, betNumbers);
                    switch (result) {
                        case SUCCESS: {
                            player.sendMessage(instance.messageBetPlaced.replace("{Price}", price + ""));
                            break;
                        }
                        case GAME_LOCKED: {
                            player.sendMessage(instance.messageGameLocked.replace("{Price}", price + ""));
                            break;
                        }
                        case NOT_ENOUGH_MONEY: {
                            player.sendMessage(instance.messageNotEnoughMoney.replace("{Price}", price + ""));
                            break;
                        }
                        case LIMIT_SELF: {
                            player.sendMessage(instance.messageBetLimitReachedSelf.replace("{Price}", price + ""));
                            break;
                        }
                        case LIMIT_PERMISSION: {
                            player.sendMessage(instance.messageBetLimitReachedPermission.replace("{Price}", price + ""));
                            break;
                        }
                    }
                    if (result.isSuccess()) {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> checkReopen(click.getWhoClicked()), 5);
                    }
                }
                Bukkit.getScheduler().runTaskLater(plugin, () -> close(click.getWhoClicked(), click.getGui(), false), 1);
            }
            return true;
        }, Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetConfirm, instance, game))
                .map(each -> each.replace("{Price}", price + "")).toArray(String[]::new)));
        gui.addElement(new StaticGuiElement('i', XMaterial.BARRIER.parseItem(), click -> {
            Bukkit.getScheduler().runTaskLater(plugin, () -> close(click.getWhoClicked(), click.getGui(), false), 1);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetCancel, instance, game)));

        return gui;
    }

    public InventoryGui getBulkNumberConfirm(Player player, PlayableLotterySixGame game, Collection<BetNumbers> betNumbers) {
        String[] guiSetup = {
                "         ",
                "   a g   ",
                "         ",
                "  h   i  ",
                "         "
        };
        long price = betNumbers.stream().mapToLong(each -> LotteryUtils.calculatePrice(each, instance)).sum();
        InventoryGui gui = new InventoryGui(plugin, LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetTitle, instance, game), guiSetup);
        String[] numbersStr = betNumbers.stream().map(each -> StringUtils.wrapAtSpace(each.toColoredString(), 6)
                .replace("{Price}", price + "")).toArray(String[]::new);
        gui.addElement(new StaticGuiElement('a', XMaterial.GOLD_BLOCK.parseItem(), numbersStr));
        gui.addElement(new StaticGuiElement('g', XMaterial.BEACON.parseItem(), Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetLotteryInfo, instance, game))
                .map(each -> each.replace("{Price}", price + "")).toArray(String[]::new)));
        gui.addElement(new StaticGuiElement('h', XMaterial.GREEN_WOOL.parseItem(), click -> {
            if (game != null && game.isValid()) {
                if (instance.backendBungeecordMode) {
                    LotterySixPlugin.getPluginMessageHandler().requestAddBet(player.getName(), player.getUniqueId(), instance.pricePerBet, BetUnitType.FULL, betNumbers);
                } else {
                    AddBetResult result = game.addBet(player.getName(), player.getUniqueId(), instance.pricePerBet, BetUnitType.FULL, betNumbers);
                    switch (result) {
                        case SUCCESS: {
                            player.sendMessage(instance.messageBetPlaced.replace("{Price}", price + ""));
                            break;
                        }
                        case GAME_LOCKED: {
                            player.sendMessage(instance.messageGameLocked.replace("{Price}", price + ""));
                            break;
                        }
                        case NOT_ENOUGH_MONEY: {
                            player.sendMessage(instance.messageNotEnoughMoney.replace("{Price}", price + ""));
                            break;
                        }
                        case LIMIT_SELF: {
                            player.sendMessage(instance.messageBetLimitReachedSelf.replace("{Price}", price + ""));
                            break;
                        }
                        case LIMIT_PERMISSION: {
                            player.sendMessage(instance.messageBetLimitReachedPermission.replace("{Price}", price + ""));
                            break;
                        }
                    }
                    if (result.isSuccess()) {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> checkReopen(click.getWhoClicked()), 5);
                    }
                }
                Bukkit.getScheduler().runTaskLater(plugin, () -> close(click.getWhoClicked(), click.getGui(), false), 1);
            }
            return true;
        }, Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetConfirm, instance, game))
                .map(each -> each.replace("{Price}", price + "")).toArray(String[]::new)));
        gui.addElement(new StaticGuiElement('i', XMaterial.BARRIER.parseItem(), click -> {
            Bukkit.getScheduler().runTaskLater(plugin, () -> close(click.getWhoClicked(), click.getGui(), false), 1);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetCancel, instance, game)));

        return gui;
    }

    public InventoryGui getPastResults(Player player, CompletedLotterySixGame game) {
        InventoryGui gui;
        if (game == null) {
            String[] guiSetup = {
                    "         ",
                    "    a    ",
                    "         "
            };
            gui = new InventoryGui(plugin, LotteryUtils.formatPlaceholders(player, instance.guiLastResultsTitle, instance), guiSetup);
            gui.addElement(new StaticGuiElement('a', XMaterial.BARRIER.parseItem(), ChatColor.RED + "No games have been played yet."));
        } else {
            String[] guiSetup = {
                    "         ",
                    " abcdefg ",
                    "         ",
                    "  h i j  ",
                    "         "
            };
            gui = new InventoryGui(plugin, LotteryUtils.formatPlaceholders(player, instance.guiLastResultsTitle, instance, game), guiSetup);
            char c = 'a';
            for (int i : game.getDrawResult().getNumbersOrdered()) {
                gui.addElement(new StaticGuiElement(c++, getNumberItem(i), getNumberColor(i) + "" + i));
            }
            int specialNumber = game.getDrawResult().getSpecialNumber();
            gui.addElement(new StaticGuiElement(c, getNumberItem(specialNumber, true), getNumberColor(specialNumber) + "" + specialNumber));
            gui.addElement(new StaticGuiElement('h', XMaterial.GOLD_BLOCK.parseItem(), LotteryUtils.formatPlaceholders(player, instance.guiLastResultsLotteryInfo, instance, game)));

            gui.addElement(new StaticGuiElement('i', XMaterial.GREEN_WOOL.parseItem(), click -> {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    close(click.getWhoClicked(), click.getGui(), false);

                    Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                        String winningNumberStr = game.getDrawResult().toColoredString();

                        List<String> pages = new ArrayList<>();
                        List<PlayerWinnings> winningsList = game.getSortedPlayerWinnings(player.getUniqueId());
                        for (PlayerWinnings winnings : winningsList) {
                            if (pages.size() > 100) {
                                break;
                            }
                            String str = winningNumberStr + "\n\n" + winnings.getWinningBet(game).getChosenNumbers().toColoredString() + "\n";
                            if (winnings.isCombination(game)) {
                                str += ChatColor.BLACK + "(" + winnings.getWinningCombination().toColoredString() + ChatColor.BLACK + ")\n";
                            }
                            str += ChatColor.GOLD + "" + winnings.getTier().getShortHand() + " $" + winnings.getWinnings() + " ($" + game.getPricePerBet(winnings.getWinningBet(game).getType()) + ")";
                            pages.add(str);
                        }
                        for (PlayerBets bets : game.getPlayerBets(player.getUniqueId())) {
                            if (pages.size() > 100) {
                                break;
                            }
                            if (winningsList.stream().noneMatch(each -> each.getWinningBet(game).getBetId().equals(bets.getBetId()))) {
                                String str = winningNumberStr + "\n\n" + bets.getChosenNumbers().toColoredString() + "\n"
                                        + LotteryUtils.formatPlaceholders(player, instance.guiLastResultsNoWinnings, instance, game) + " $0 ($" + game.getPricePerBet(bets.getType()) + ")";
                                pages.add(str);
                            }
                        }
                        if (pages.isEmpty()) {
                            pages.add(winningNumberStr + "\n\n" + String.join("\n", LotteryUtils.formatPlaceholders(null, instance.guiLastResultsNothing, instance, game)) + "\n");
                        }
                        ItemStack itemStack = XMaterial.WRITTEN_BOOK.parseItem();
                        BookMeta meta = (BookMeta) itemStack.getItemMeta();
                        meta.setAuthor("LotterySix");
                        meta.setTitle("LotterySix");
                        meta.setPages(pages);
                        itemStack.setItemMeta(meta);

                        Bukkit.getScheduler().runTaskLater(plugin, () -> BookUtils.openBook((Player) click.getWhoClicked(), itemStack), 1);
                    }, 1);
                }, 1);
                return true;
            }, LotteryUtils.formatPlaceholders(player, instance.guiLastResultsYourBets, instance, game)));

            ItemStack left = XMaterial.PAPER.parseItem();
            ItemMeta leftMeta = left.getItemMeta();
            leftMeta.setDisplayName(game.getGameNumber() == null ? " " : game.getGameNumber().toString());
            left.setItemMeta(leftMeta);
            gui.addElement(new StaticGuiElement('j', XMaterial.MAP.parseItem(), click -> {
                Bukkit.getScheduler().runTaskLater(plugin, () -> close(click.getWhoClicked(), click.getGui(), false), 1);
                Bukkit.getScheduler().runTaskLater(plugin, () -> new AnvilGUI.Builder().plugin(plugin)
                        .title(LotteryUtils.formatPlaceholders(player, instance.guiGameNumberInputTitle, instance, game))
                        .itemLeft(left)
                        .onComplete(completion -> {
                            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                                String input = completion.getText().trim();
                                try {
                                    GameNumber gameNumber = GameNumber.fromString(input);
                                    CompletedLotterySixGame targetGame = instance.getCompletedGames().get(gameNumber);
                                    if (targetGame == null) {
                                        player.sendMessage(instance.messageGameNumberNotFound);
                                    } else {
                                        Bukkit.getScheduler().runTaskLater(plugin, () -> getPastResults(player, targetGame).show(player), 2);
                                    }
                                } catch (Exception e) {
                                    player.sendMessage(instance.messageGameNumberNotFound);
                                }
                            });
                            return Collections.singletonList(AnvilGUI.ResponseAction.close());
                        })
                        .open(player), 2);
                return true;
            }, LotteryUtils.formatPlaceholders(player, instance.guiLastResultsLookupHistoricGames, instance, game)));
        }

        return gui;
    }

}
