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
import com.loohp.lotterysix.game.lottery.PlayableLotterySixGame;
import com.loohp.lotterysix.game.objects.PlayerBets;
import com.loohp.lotterysix.game.objects.PlayerWinnings;
import com.loohp.lotterysix.game.objects.betnumbers.BetNumbers;
import com.loohp.lotterysix.game.objects.betnumbers.BetNumbersBuilder;
import com.loohp.lotterysix.game.objects.betnumbers.BetNumbersType;
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
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
                    Bukkit.getScheduler().runTaskLater(plugin, () -> click.getGui().close(click.getWhoClicked(), true), 1);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> ((Player) click.getWhoClicked()).openBook(getPlacedBets((Player) click.getWhoClicked())), 2);
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
                    Bukkit.getScheduler().runTaskLater(plugin, () -> getBetTypeChooser((Player) click.getWhoClicked(), LotterySixPlugin.getInstance().getCurrentGame()).show(click.getWhoClicked()), 1);
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

    public ItemStack getPlacedBets(Player player) {
        PlayableLotterySixGame game = LotterySixPlugin.getInstance().getCurrentGame();
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
                pages.add(title + "\n\n" + bet.getChosenNumbers().toColoredString());
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
        InventoryGui gui = new InventoryGui(plugin, LotteryUtils.formatPlaceholders(null, instance.guiMainMenuTitle, instance), guiSetup);
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
            Bukkit.getScheduler().runTaskLater(plugin, () -> getSingleNumberConfirm((Player) click.getWhoClicked(), game, BetNumbersBuilder.random(1, instance.numberOfChoices).build()).show(click.getWhoClicked()), 1);
            return true;
        }, LotteryUtils.formatPlaceholders(null, instance.guiSelectNewBetTypeRandom, instance)));
        gui.setSilent(true);
        return gui;
    }

    public InventoryGui getNumberChooser(Player player, PlayableLotterySixGame game, BetNumbersBuilder builder) {
        int num = LotterySixPlugin.getInstance().numberOfChoices;
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
                                        Bukkit.getScheduler().runTaskLater(plugin, () -> change.getWhoClicked().closeInventory(), 1);
                                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                            BetNumbers betNumbers = builder.build();
                                            if (betNumbers.getType().equals(BetNumbersType.SINGLE)) {
                                                getSingleNumberConfirm((Player) change.getWhoClicked(), game, betNumbers).show(change.getWhoClicked());
                                            } else {
                                                getNumberConfirm((Player) change.getWhoClicked(), game, betNumbers).show(change.getWhoClicked());
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
                                    } else if (builder.completed() || (isBanker && !((BetNumbersBuilder.BankerBuilder) builder).getBankers().isEmpty())) {
                                        gui.removeElement('\0');
                                        gui.addElement(new StaticGuiElement('\0', isBanker && !((BetNumbersBuilder.BankerBuilder) builder).inSelectionPhase() ? XMaterial.EMERALD_BLOCK.parseItem() : XMaterial.GOLD_BLOCK.parseItem(), click -> {
                                            if (builder.completed()) {
                                                Bukkit.getScheduler().runTaskLater(plugin, () -> click.getWhoClicked().closeInventory(), 1);
                                                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                                    BetNumbers betNumbers = builder.build();
                                                    if (betNumbers.getType().equals(BetNumbersType.SINGLE)) {
                                                        getSingleNumberConfirm((Player) click.getWhoClicked(), game, betNumbers).show(click.getWhoClicked());
                                                    } else {
                                                        getNumberConfirm((Player) click.getWhoClicked(), game, betNumbers).show(click.getWhoClicked());
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

    public InventoryGui getNumberConfirm(Player player, PlayableLotterySixGame game, BetNumbers betNumbers) {
        String[] guiSetup = {
                "         ",
                "   a g   ",
                "         ",
                "  h   i  ",
                "         "
        };
        InventoryGui gui = new InventoryGui(plugin, LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetTitle, instance, game), guiSetup);
        gui.addElement(new StaticGuiElement('a', XMaterial.GOLD_BLOCK.parseItem(), StringUtils.wrapAtSpace(betNumbers.toColoredString(), 6)
                .replace("{Price}", LotteryUtils.calculatePrice(betNumbers, instance) + "")));
        gui.addElement(new StaticGuiElement('g', XMaterial.BEACON.parseItem(), Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetLotteryInfo, instance, game))
                .map(each -> each.replace("{Price}", LotteryUtils.calculatePrice(betNumbers, instance) + "")).toArray(String[]::new)));
        gui.addElement(new StaticGuiElement('h', XMaterial.GREEN_WOOL.parseItem(), click -> {
            if (game != null && game.isValid()) {
                if (game.addBet(player.getUniqueId(), LotterySixPlugin.getInstance().pricePerBet, betNumbers)) {
                    player.sendMessage(instance.messageBetPlaced.replace("{Price}", LotteryUtils.calculatePrice(betNumbers, instance) + ""));
                } else {
                    player.sendMessage(instance.messageNotEnoughMoney.replace("{Price}", LotteryUtils.calculatePrice(betNumbers, instance) + ""));
                }
                Bukkit.getScheduler().runTaskLater(plugin, () -> click.getGui().close(click.getWhoClicked(), true), 1);
            }
            return true;
        }, Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetConfirm, instance, game))
                .map(each -> each.replace("{Price}", LotteryUtils.calculatePrice(betNumbers, instance) + "")).toArray(String[]::new)));
        gui.addElement(new StaticGuiElement('i', XMaterial.BARRIER.parseItem(), click -> {
            Bukkit.getScheduler().runTaskLater(plugin, () -> click.getWhoClicked().closeInventory(), 1);
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
        InventoryGui gui = new InventoryGui(plugin, LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetTitle, instance, game), guiSetup);
        char c = 'a';
        for (int i : betNumbers.getNumbers()) {
            gui.addElement(new StaticGuiElement(c++, getNumberItem(i), getNumberColor(i) + "" + i));
        }
        gui.addElement(new StaticGuiElement('g', XMaterial.BEACON.parseItem(), Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetLotteryInfo, instance, game))
                .map(each -> each.replace("{Price}", LotteryUtils.calculatePrice(betNumbers, instance) + "")).toArray(String[]::new)));
        gui.addElement(new StaticGuiElement('h', XMaterial.GREEN_WOOL.parseItem(), click -> {
            if (game != null && game.isValid()) {
                if (game.addBet(player.getUniqueId(), LotterySixPlugin.getInstance().pricePerBet, betNumbers)) {
                    player.sendMessage(instance.messageBetPlaced.replace("{Price}", instance.pricePerBet + ""));
                    Bukkit.getScheduler().runTaskLater(plugin, () -> click.getWhoClicked().closeInventory(), 1);
                } else {
                    player.sendMessage(instance.messageNotEnoughMoney.replace("{Price}", instance.pricePerBet + ""));
                    Bukkit.getScheduler().runTaskLater(plugin, () -> click.getGui().close(click.getWhoClicked(), true), 1);
                }
            }
            return true;
        }, Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetConfirm, instance, game))
                .map(each -> each.replace("{Price}", LotteryUtils.calculatePrice(betNumbers, instance) + "")).toArray(String[]::new)));
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
                    "  h   i  ",
                    "         "
            };
            gui = new InventoryGui(plugin, LotteryUtils.formatPlaceholders(player, instance.guiLastResultsTitle, instance, game), guiSetup);
            char c = 'a';
            for (int i : game.getDrawResult().getNumbersOrdered()) {
                gui.addElement(new StaticGuiElement(c++, getNumberItem(i), getNumberColor(i) + "" + i));
            }
            int specialNumber = game.getDrawResult().getSpecialNumber();
            gui.addElement(new StaticGuiElement(c, getNumberItem(specialNumber), getNumberColor(specialNumber) + "" + specialNumber));
            gui.addElement(new StaticGuiElement('h', XMaterial.GOLD_BLOCK.parseItem(), LotteryUtils.formatPlaceholders(player, instance.guiLastResultsLotteryInfo, instance, game)));

            List<String> pages = new ArrayList<>();
            List<PlayerWinnings> winningsList = game.getPlayerWinnings(player.getUniqueId());
            for (PlayerWinnings winnings : winningsList) {
                String str = winnings.getWinningBet().getChosenNumbers().toColoredString() + "\n"
                        + ChatColor.GOLD + "" + winnings.getTier().getShortHand() + " $" + winnings.getWinnings();
                pages.add(str);
            }
            for (PlayerBets bets : game.getPlayerBets(player.getUniqueId())) {
                if (winningsList.stream().noneMatch(each -> each.getWinningBet().getBetId().equals(bets.getBetId()))) {
                    String str = bets.getChosenNumbers().toColoredString() + "\n"
                            + LotteryUtils.formatPlaceholders(player, instance.guiLastResultsNoWinnings, instance, game) + " $0";
                    pages.add(str);
                }
            }
            ItemStack itemStack = XMaterial.WRITTEN_BOOK.parseItem();
            BookMeta meta = (BookMeta) itemStack.getItemMeta();
            meta.setAuthor("LotterySix");
            meta.setTitle("LotterySix");
            meta.setPages(pages);
            itemStack.setItemMeta(meta);

            gui.addElement(new StaticGuiElement('i', XMaterial.GREEN_WOOL.parseItem(), click -> {
                Bukkit.getScheduler().runTaskLater(plugin, () -> click.getGui().close(click.getWhoClicked(), true), 1);
                Bukkit.getScheduler().runTaskLater(plugin, () -> ((Player) click.getWhoClicked()).openBook(itemStack), 2);
                return true;
            }, LotteryUtils.formatPlaceholders(player, instance.guiLastResultsYourBets, instance, game)));
        }

        return gui;
    }

}
