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
import com.loohp.lotterysix.game.completed.CompletedLotterySixGame;
import com.loohp.lotterysix.game.objects.BetNumbers;
import com.loohp.lotterysix.game.objects.PlayerBets;
import com.loohp.lotterysix.game.objects.PlayerWinnings;
import com.loohp.lotterysix.game.playable.PlayableLotterySixGame;
import com.loohp.lotterysix.utils.ChatColorUtils;
import com.loohp.lotterysix.utils.LotteryUtils;
import de.themoep.inventorygui.DynamicGuiElement;
import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.GuiPageElement;
import de.themoep.inventorygui.GuiStateElement;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import io.github.bananapuncher714.nbteditor.NBTEditor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LotteryPluginGUI {

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
        return NBTEditor.set(itemStack, number, "LotterySixNumber");
    }

    private static ChatColor getNumberColor(int number) {
        return ChatColorUtils.getNumberColor(number);
    }

    private static String coloredString(BetNumbers betNumbers) {
        return betNumbers.getNumbers().stream().map(each -> getNumberColor(each) + each.toString()).collect(Collectors.joining(" "));
    }

    private final LotterySixPlugin plugin;
    private final LotterySix instance;
    private final InventoryGui mainMenu;

    public LotteryPluginGUI(LotterySixPlugin plugin) {
        this.plugin = plugin;
        this.instance = LotterySixPlugin.getInstance();
        String[] guiSetup = {
                "         ",
                " aaaaaaa ",
                " abacada ",
                " aaaaaaa ",
                "         "
        };
        mainMenu = new InventoryGui(plugin, LotteryUtils.formatPlaceholders(null, instance.guiMainMenuTitle, instance), guiSetup);
        mainMenu.setFiller(XMaterial.YELLOW_STAINED_GLASS_PANE.parseItem());
        mainMenu.addElement(new StaticGuiElement('a', new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));
        mainMenu.addElement(new StaticGuiElement('b', XMaterial.CLOCK.parseItem(), click -> {
            Bukkit.getScheduler().runTaskLater(plugin, () -> getPastResults((Player) click.getWhoClicked()).show(click.getWhoClicked()), 1);
            return true;
        }, LotteryUtils.formatPlaceholders(null, instance.guiMainMenuCheckPastResults, instance)));
        mainMenu.addElement(new DynamicGuiElement('c', player -> {
            PlayableLotterySixGame currentGame = LotterySixPlugin.getInstance().getCurrentGame();
            if (currentGame == null) {
                return new StaticGuiElement('c', XMaterial.BARRIER.parseItem(), LotteryUtils.formatPlaceholders(null, instance.guiMainMenuNoLotteryGamesScheduled, instance));
            } else {
                return new StaticGuiElement('c', XMaterial.PAPER.parseItem(), click -> {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> getPlacedBets((Player) click.getWhoClicked()).show(click.getWhoClicked()), 1);
                    return true;
                }, LotteryUtils.formatPlaceholders(null, instance.guiMainMenuCheckOwnBets, instance, currentGame));
            }
        }));
        mainMenu.addElement(new DynamicGuiElement('d', player -> {
            PlayableLotterySixGame currentGame = LotterySixPlugin.getInstance().getCurrentGame();
            if (currentGame == null) {
                return new StaticGuiElement('d', XMaterial.RED_WOOL.parseItem(), LotteryUtils.formatPlaceholders(null, instance.guiMainMenuNoLotteryGamesScheduled, instance));
            } else {
                return new StaticGuiElement('d', XMaterial.GOLD_INGOT.parseItem(), click -> {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> getNumberChooser((Player) click.getWhoClicked(), LotterySixPlugin.getInstance().getCurrentGame(), BetNumbers.Builder.builder()).show(click.getWhoClicked()), 1);
                    return true;
                }, LotteryUtils.formatPlaceholders(null, instance.guiMainMenuPlaceNewBets, instance, currentGame));
            }
        }));
    }

    public void forceClose(Player player) {
        InventoryGui gui = InventoryGui.getOpen(player);
        if (gui != null) {
            gui.close(player, true);
        }
    }

    public InventoryGui getMainMenu() {
        return mainMenu;
    }

    public InventoryGui getPlacedBets(Player player) {
        PlayableLotterySixGame game = LotterySixPlugin.getInstance().getCurrentGame();
        List<PlayerBets> bets = game.getPlayerBets(player.getUniqueId());
        InventoryGui gui;
        if (bets.isEmpty()) {
            String[] guiSetup = {
                    "         ",
                    "    a    ",
                    "         "
            };
            gui = new InventoryGui(plugin, LotteryUtils.formatPlaceholders(player, instance.guiYourBetsTitle, instance, game), guiSetup);
            gui.addElement(new StaticGuiElement('a', XMaterial.BARRIER.parseItem(), LotteryUtils.formatPlaceholders(null, instance.guiYourBetsNothing, instance, game)));
        } else {
            String[] guiSetup = {
                    "         ",
                    " abcdefg ",
                    "         ",
                    "  h   i  ",
                    "         "
            };
            gui = new InventoryGui(plugin, LotteryUtils.formatPlaceholders(player, instance.guiYourBetsTitle, instance, game), guiSetup);
            gui.addElement(new StaticGuiElement('g', XMaterial.GOLD_BLOCK.parseItem(), LotteryUtils.formatPlaceholders(player, instance.guiYourBetsLotteryInfo, instance, game)));
            for (char c = 'a'; c <= 'f'; c++) {
                int i = c - 'a';
                GuiElementGroup group = new GuiElementGroup(c);
                for (PlayerBets bet : bets) {
                    int number = bet.getChosenNumbers().getNumber(i);
                    group.addElement((new StaticGuiElement(c, getNumberItem(number), getNumberColor(number) + "" + number)));
                }
                gui.addElement(group);
            }
            gui.addElement(new GuiPageElement('h', XMaterial.REDSTONE_BLOCK.parseItem(), GuiPageElement.PageAction.PREVIOUS, LotteryUtils.formatPlaceholders(player, instance.guiYourBetsPreviousPage, instance, game)));
            gui.addElement(new GuiPageElement('i', XMaterial.EMERALD_BLOCK.parseItem(), GuiPageElement.PageAction.NEXT, LotteryUtils.formatPlaceholders(player, instance.guiYourBetsNextPage, instance, game)));
            gui.setSilent(true);
        }
        return gui;
    }

    public InventoryGui getNumberChooser(Player player, PlayableLotterySixGame game, BetNumbers.Builder builder) {
        int num = LotterySixPlugin.getInstance().numberOfChoices;
        InventoryGui gui = new InventoryGui(plugin, LotteryUtils.formatPlaceholders(player, instance.guiNewBetTitle, instance, game), fillChars(num / 9 + 1));
        char c = 'a';
        for (int i = 0; i < num; i++) {
            int number = i + 1;
            GuiStateElement element = new GuiStateElement(c++,
                    new GuiStateElement.State(
                            change -> {
                                builder.addNumber(number);
                                if (builder.completed()) {
                                    Bukkit.getScheduler().runTaskLater(plugin, () -> change.getWhoClicked().closeInventory(), 1);
                                    Bukkit.getScheduler().runTaskLater(plugin, () -> getNumberConfirm((Player) change.getWhoClicked(), game, builder.build()).show(change.getWhoClicked()), 2);
                                }
                            },
                            "true",
                            XMaterial.LIGHT_GRAY_WOOL.parseItem(),
                            ChatColor.GRAY + "" + number
                    ),
                    new GuiStateElement.State(
                            change -> {
                                builder.removeNumber(number);
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
        return gui;
    }

    public InventoryGui getNumberConfirm(Player player, PlayableLotterySixGame game, BetNumbers betNumbers) {
        String[] guiSetup = {
                "         ",
                " abcdefg ",
                "         ",
                "  h   i  ",
                "         "
        };
        InventoryGui gui = new InventoryGui(plugin, LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetTitle, instance, game), guiSetup);
        char c = 'a';
        for (int i : betNumbers) {
            gui.addElement(new StaticGuiElement(c++, getNumberItem(i), getNumberColor(i) + "" + i));
        }
        gui.addElement(new StaticGuiElement('g', XMaterial.BEACON.parseItem(), LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetLotteryInfo, instance, game)));
        gui.addElement(new StaticGuiElement('h', XMaterial.GREEN_WOOL.parseItem(), click -> {
            if (game != null && game.isValid()) {
                if (game.addBet(player.getUniqueId(), LotterySixPlugin.getInstance().pricePerBet, betNumbers)) {
                    player.sendMessage(instance.messageBidPlaced.replace("{Price}", instance.pricePerBet + ""));
                    Bukkit.getScheduler().runTaskLater(plugin, () -> click.getWhoClicked().closeInventory(), 1);
                } else {
                    player.sendMessage(instance.messageNotEnoughMoney.replace("{Price}", instance.pricePerBet + ""));
                    Bukkit.getScheduler().runTaskLater(plugin, () -> click.getGui().close(click.getWhoClicked(), true), 1);
                }
            }
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetConfirm, instance, game)));
        gui.addElement(new StaticGuiElement('i', XMaterial.BARRIER.parseItem(), click -> {
            Bukkit.getScheduler().runTaskLater(plugin, () -> click.getWhoClicked().closeInventory(), 1);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetCancel, instance, game)));

        return gui;
    }

    public InventoryGui getPastResults(Player player) {
        List<CompletedLotterySixGame> games = LotterySixPlugin.getInstance().getCompletedGames();
        InventoryGui gui;
        if (games.isEmpty()) {
            String[] guiSetup = {
                    "         ",
                    "    a    ",
                    "         "
            };
            gui = new InventoryGui(plugin, LotteryUtils.formatPlaceholders(player, instance.guiLastResultsTitle, instance), guiSetup);
            gui.addElement(new StaticGuiElement('a', XMaterial.BARRIER.parseItem(), ChatColor.RED + "No games have been played yet."));
        } else {
            CompletedLotterySixGame game = games.get(0);
            String[] guiSetup = {
                    "         ",
                    " abcdefg ",
                    "         ",
                    "    h    ",
                    "         "
            };
            gui = new InventoryGui(plugin, LotteryUtils.formatPlaceholders(player, instance.guiLastResultsTitle, instance, game), guiSetup);
            char c = 'a';
            for (int i : game.getDrawResult()) {
                gui.addElement(new StaticGuiElement(c++, getNumberItem(i), getNumberColor(i) + "" + i));
            }
            gui.addElement(new StaticGuiElement('g', XMaterial.GOLD_BLOCK.parseItem(), LotteryUtils.formatPlaceholders(player, instance.guiLastResultsLotteryInfo, instance, game)));
            List<String> strings = new ArrayList<>(Arrays.asList(LotteryUtils.formatPlaceholders(player, instance.guiLastResultsYourBets, instance, game)));
            List<PlayerWinnings> winningsList = game.getPlayerWinnings(player.getUniqueId());
            for (PlayerWinnings winnings : winningsList) {
                strings.add(coloredString(winnings.getWinningBet().getChosenNumbers()));
                strings.add(ChatColor.GOLD + "" + winnings.getTier() + " $" + winnings.getWinnings());
                strings.add("");
            }
            for (PlayerBets bets : game.getPlayerBets(player.getUniqueId())) {
                if (winningsList.stream().noneMatch(each -> each.getWinningBet().getBetId().equals(bets.getBetId()))) {
                    strings.add(coloredString(bets.getChosenNumbers()));
                    strings.add(LotteryUtils.formatPlaceholders(player, instance.guiLastResultsNoWinnings, instance, game) + " $0");
                    strings.add("");
                }
            }
            if (strings.size() > 1) {
                strings.remove(strings.size() - 1);
            }

            gui.addElement(new StaticGuiElement('h', XMaterial.GREEN_WOOL.parseItem(), strings.toArray(new String[0])));
        }

        return gui;
    }

}
