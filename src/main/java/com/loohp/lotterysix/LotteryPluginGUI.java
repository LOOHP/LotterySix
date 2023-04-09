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
import com.loohp.lotterysix.game.LotteryRegistry;
import com.loohp.lotterysix.game.LotterySix;
import com.loohp.lotterysix.game.lottery.CompletedLotterySixGame;
import com.loohp.lotterysix.game.lottery.CompletedLotterySixGameIndex;
import com.loohp.lotterysix.game.lottery.GameNumber;
import com.loohp.lotterysix.game.lottery.LazyCompletedLotterySixGameList;
import com.loohp.lotterysix.game.lottery.PlayableLotterySixGame;
import com.loohp.lotterysix.game.objects.AddBetResult;
import com.loohp.lotterysix.game.objects.BetUnitType;
import com.loohp.lotterysix.game.objects.IntObjectConsumer;
import com.loohp.lotterysix.game.objects.NumberStatistics;
import com.loohp.lotterysix.game.objects.PlayerBets;
import com.loohp.lotterysix.game.objects.PlayerPreferenceKey;
import com.loohp.lotterysix.game.objects.PlayerStatsKey;
import com.loohp.lotterysix.game.objects.PlayerWinnings;
import com.loohp.lotterysix.game.objects.PrizeTier;
import com.loohp.lotterysix.game.objects.WinningNumbers;
import com.loohp.lotterysix.game.objects.betnumbers.BetNumbers;
import com.loohp.lotterysix.game.objects.betnumbers.BetNumbersBuilder;
import com.loohp.lotterysix.game.objects.betnumbers.BetNumbersType;
import com.loohp.lotterysix.game.player.LotteryPlayer;
import com.loohp.lotterysix.objects.ScheduledRunnable;
import com.loohp.lotterysix.objects.Scheduler;
import com.loohp.lotterysix.utils.BookUtils;
import com.loohp.lotterysix.utils.ChatColorUtils;
import com.loohp.lotterysix.utils.LotteryUtils;
import com.loohp.lotterysix.utils.SkinUtils;
import com.loohp.lotterysix.utils.StringUtils;
import de.themoep.inventorygui.DynamicGuiElement;
import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.GuiStateElement;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import io.github.bananapuncher714.nbteditor.NBTEditor;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
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
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

public class LotteryPluginGUI implements Listener {

    private static String multipleDrawStr(int multipleDraw) {
        return multipleDraw <= 1 ? "-" : StringUtils.formatComma(multipleDraw);
    }

    private static String[] fillChars(int arrays) {
        return fillChars(arrays, 0);
    }

    private static String[] fillChars(int arrays, int trail) {
        String[] strings = new String[arrays + trail];
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

    private static ItemStack getNumberItem(int number, NumberSelectedState selectedState) {
        return getNumberItem(number, false, selectedState);
    }

    private static ItemStack getNumberItem(int number, boolean enchanted, NumberSelectedState selectedState) {
        XMaterial material;
        if (LotterySixPlugin.getInstance().numberItemsType == null) {
            if (number == 0) {
                if (selectedState.equals(NumberSelectedState.NOT_SELECTED)) {
                    material = XMaterial.ORANGE_WOOL;
                } else {
                    material = XMaterial.GRAY_WOOL;
                }
            } else {
                switch (selectedState) {
                    case NOT_SELECTED: {
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
                        break;
                    }
                    case SELECTED: {
                        material = XMaterial.LIGHT_GRAY_WOOL;
                        break;
                    }
                    case SELECTED_BANKER: {
                        material = XMaterial.YELLOW_WOOL;
                        break;
                    }
                    default: {
                        material = XMaterial.GRAY_WOOL;
                        break;
                    }
                }
            }
        } else {
            material = LotterySixPlugin.getInstance().numberItemsType;
        }
        ItemStack itemStack = material.parseItem();
        if (selectedState.shouldSetStack() && LotterySixPlugin.getInstance().numberItemsSetStackSize) {
            itemStack.setAmount(Math.max(1, number));
        }
        if (enchanted) {
            itemStack = setEnchanted(itemStack);
        }
        int itemModelData = LotterySixPlugin.getInstance().numberItemsCustomModelData + selectedState.getCustomModelDataOffset() + number;
        itemStack = NBTEditor.set(itemStack, itemModelData, "CustomModelData");
        return NBTEditor.set(itemStack, number, "LotterySixNumber");
    }

    private static ItemStack setItemSize(ItemStack item, int amount) {
        if (item == null) {
            return null;
        }
        item.setAmount(amount);
        return item;
    }

    private static ItemStack setEnchanted(ItemStack item) {
        if (item == null) {
            return null;
        }
        item.addUnsafeEnchantment(Enchantment.LUCK, 10);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(itemMeta);
        return item;
    }

    private static ChatColor getNumberColor(int number) {
        return ChatColorUtils.getNumberColor(number);
    }

    private final LotterySixPlugin plugin;
    private final LotterySix instance;
    private final Map<Player, Long> lastGuiClick;
    private final Map<Player, AtomicInteger> tickCounter;

    public LotteryPluginGUI(LotterySixPlugin plugin) {
        this.plugin = plugin;
        this.instance = LotterySixPlugin.getInstance();
        this.lastGuiClick = new HashMap<>();
        this.tickCounter = new ConcurrentHashMap<>();
    }

    public void forceClose(Player player) {
        InventoryGui gui = InventoryGui.getOpen(player);
        if (gui != null) {
            gui.close(player, true);
        }
    }

    private void handleClick(InventoryInteractEvent event) {
        Player player = (Player) event.getWhoClicked();
        long gameTick = tickCounter.get(player).get();
        Long lastClick = lastGuiClick.get(player);
        if (lastClick == null || gameTick != lastClick) {
            lastGuiClick.put(player, gameTick);
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        AtomicInteger counter = new AtomicInteger();
        tickCounter.put(player, counter);
        Scheduler.runTaskTimer(plugin, () -> counter.incrementAndGet(), 0, 1, player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        lastGuiClick.remove(player);
        Deque<InventoryGui> guis = InventoryGui.clearHistory(player);
        Scheduler.runTaskLater(plugin, () -> {
            InventoryGui gui;
            while ((gui = guis.poll()) != null) {
                gui.destroy();
            }
        }, 1, player);
        tickCounter.remove(player);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryClickLow(InventoryClickEvent event) {
        Inventory inventory = event.getView().getTopInventory();
        if (inventory.getHolder() instanceof InventoryGui.Holder) {
            handleClick(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getView().getTopInventory();
        if (inventory.getHolder() instanceof InventoryGui.Holder) {
            if (InventoryGui.getOpen(event.getWhoClicked()) == null) {
                event.setCancelled(true);
                Scheduler.runTaskLater(plugin, () -> {
                    InventoryGui.clearHistory(event.getWhoClicked());
                    event.getWhoClicked().closeInventory();
                }, 1, event.getWhoClicked());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory inventory = event.getView().getTopInventory();
        if (inventory.getHolder() instanceof InventoryGui.Holder) {
            handleClick(event);
            if (InventoryGui.getOpen(event.getWhoClicked()) == null) {
                event.setCancelled(true);
                Scheduler.runTaskLater(plugin, () -> {
                    InventoryGui.clearHistory(event.getWhoClicked());
                    event.getWhoClicked().closeInventory();
                }, 1, event.getWhoClicked());
            }
        }
    }

    public void close(HumanEntity player, InventoryGui inventoryGui, boolean back) {
        inventoryGui.close(player, !back);
        Scheduler.runTaskLater(plugin, () -> inventoryGui.destroy(), 1, player);
    }

    public void removeSecondLast(HumanEntity player) {
        Deque<InventoryGui> history = InventoryGui.getHistory(player);
        if (history.size() <= 1) {
            return;
        }
        Iterator<InventoryGui> itr = history.descendingIterator();
        itr.next();
        InventoryGui inventoryGui = itr.next();
        itr.remove();
        Scheduler.runTaskLater(plugin, () -> inventoryGui.destroy(), 1, player);
    }

    public void checkReopen(HumanEntity player) {
        if (instance.getLotteryPlayerManager().getLotteryPlayer(player.getUniqueId()).getPreference(PlayerPreferenceKey.REOPEN_MENU_ON_PURCHASE, boolean.class)) {
            if (InventoryGui.getHistory(player).isEmpty()) {
                getMainMenu((Player) player).show(player);
            }
        }
    }

    public InventoryGui getMainMenu(Player player) {
        String[] guiSetup = {
                "         ",
                "fyaaaaaaa",
                "$ybacadae",
                "zyaaaaaaa",
                "         "
        };
        InventoryGui gui = new InventoryGui(plugin, LotteryUtils.formatPlaceholders(player, instance.guiMainMenuTitle, instance), guiSetup);
        gui.setFiller(XMaterial.YELLOW_STAINED_GLASS_PANE.parseItem());
        gui.addElement(new StaticGuiElement('a', new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));
        gui.addElement(new StaticGuiElement('y', XMaterial.ORANGE_STAINED_GLASS_PANE.parseItem(), ChatColor.LIGHT_PURPLE.toString()));

        CompletedLotterySixGame completedGame = instance.getCompletedGames().isEmpty() ? null : instance.getCompletedGames().get(0);
        gui.addElement(new StaticGuiElement('b', completedGame != null && completedGame.hasPlayerWinnings(player.getUniqueId()) ? setEnchanted(XMaterial.CLOCK.parseItem()) : XMaterial.CLOCK.parseItem(), click -> {
            Scheduler.runTaskLater(plugin, () -> close(click.getWhoClicked(), gui, false), 1, player);
            Scheduler.runTaskLater(plugin, () -> getPastResults((Player) click.getWhoClicked(), completedGame).show(click.getWhoClicked()), 2, player);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.guiMainMenuCheckPastResults, instance)));
        gui.addElement(new DynamicGuiElement('c', viewer -> {
            PlayableLotterySixGame currentGame = instance.getCurrentGame();
            if (currentGame == null) {
                return new StaticGuiElement('c', XMaterial.BARRIER.parseItem(), LotteryUtils.formatPlaceholders(player, instance.guiMainMenuNoLotteryGamesScheduled, instance));
            } else {
                return new StaticGuiElement('c', XMaterial.PAPER.parseItem(), click -> {
                    Scheduler.runTaskLater(plugin, () -> {
                        close(click.getWhoClicked(), click.getGui(), true);

                        Scheduler.runTaskLaterAsynchronously(plugin, () -> {
                            ItemStack itemStack = getPlacedBets((Player) click.getWhoClicked());
                            Scheduler.runTaskLater(plugin, () -> BookUtils.openBook((Player) click.getWhoClicked(), itemStack), 1, player);
                        }, 1);
                    }, 1, player);
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
                    Scheduler.runTaskLater(plugin, () -> getBetTypeChooser((Player) click.getWhoClicked(), instance.getCurrentGame()).show(click.getWhoClicked()), 1, player);
                    return true;
                }, LotteryUtils.formatPlaceholders(player, instance.guiMainMenuPlaceNewBets, instance, currentGame));
            }
        }));
        gui.addElement(new StaticGuiElement('e', XMaterial.OAK_SIGN.parseItem(), click -> {
            Scheduler.runTaskLater(plugin, () -> getNumberStatistics((Player) click.getWhoClicked(), instance.getCompletedGames().isEmpty() ? null : instance.getCompletedGames().get(0)).show(click.getWhoClicked()), 1, player);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.guiMainMenuStatistics, instance)));
        gui.addElement(new StaticGuiElement('z', XMaterial.COMPASS.parseItem(), click -> {
            TextComponent message = new TextComponent(LotteryUtils.formatPlaceholders(player, instance.explanationMessage, instance));
            message.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, instance.explanationURL));
            click.getWhoClicked().spigot().sendMessage(message);
            Scheduler.runTaskLater(plugin, () -> close(click.getWhoClicked(), click.getGui(), true), 1, player);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.explanationGUIItem, instance)));
        gui.addElement(new StaticGuiElement('f', SkinUtils.getSkull(player.getUniqueId()), click -> {
            Scheduler.runTaskLater(plugin, () -> getBettingAccount((Player) click.getWhoClicked()).show(click.getWhoClicked()), 1, player);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.guiMainMenuBettingAccount, instance)));
        gui.addElement(new StaticGuiElement('$', XMaterial.EMERALD.parseItem(), click -> {
            Scheduler.runTaskLater(plugin, () -> getTransactionMenu((Player) click.getWhoClicked()).show(click.getWhoClicked()), 1, player);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.guiMainMenuAccountFundTransfer, instance)));
        gui.setCloseAction(close -> false);
        Set<GuiElement> elements = Collections.singleton(gui.getElement('c'));
        new ScheduledRunnable() {
            @Override
            public void run() {
                Deque<InventoryGui> history = InventoryGui.getHistory(player);
                if (!history.contains(gui)) {
                    cancel();
                    return;
                }
                if (Objects.equals(history.peekLast(), gui)) {
                    InventoryGui.updateElements(player, elements);
                    gui.draw(player, false);
                }
            }
        }.runTaskTimer(plugin, 10, 10, player);
        return gui;
    }

    public InventoryGui getBettingAccount(Player player) {
        String[] guiSetup = {
                "         ",
                "bzacadaea",
                "$zaaaaaaa",
                "azaafagaa",
                "         "
        };
        InventoryGui gui = new InventoryGui(plugin, LotteryUtils.formatPlaceholders(player, instance.guiBettingAccountTitle, instance), guiSetup);
        gui.setFiller(XMaterial.BLUE_STAINED_GLASS_PANE.parseItem());
        gui.addElement(new StaticGuiElement('a', new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));
        gui.addElement(new StaticGuiElement('z', XMaterial.LIGHT_BLUE_STAINED_GLASS_PANE.parseItem(), ChatColor.LIGHT_PURPLE.toString()));
        gui.addElement(new StaticGuiElement('b', SkinUtils.getSkull(player.getUniqueId()), LotteryUtils.formatPlaceholders(player, instance.guiBettingAccountProfile, instance)));

        LotteryPlayer lotteryPlayer = instance.getLotteryPlayerManager().getLotteryPlayer(player.getUniqueId());
        gui.addElement(new DynamicGuiElement('c', () -> {
            boolean value = lotteryPlayer.getPreference(PlayerPreferenceKey.HIDE_TITLES, boolean.class);
            String display = value ? instance.trueFormat : instance.falseFormat;
            if (value) {
                return new StaticGuiElement('c', XMaterial.LIME_DYE.parseItem(), click -> {
                    if (instance.backendBungeecordMode) {
                        LotterySixPlugin.getPluginMessageHandler().updatePlayerPreference(lotteryPlayer, PlayerPreferenceKey.HIDE_TITLES, false);
                    } else {
                        lotteryPlayer.setPreference(PlayerPreferenceKey.HIDE_TITLES, false);
                    }
                    return true;
                }, Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiBettingAccountToggleHideTitles, instance)).map(e -> e.replace("{Status}", display)).toArray(String[]::new));
            } else {
                return new StaticGuiElement('c', XMaterial.GRAY_DYE.parseItem(), click -> {
                    if (instance.backendBungeecordMode) {
                        LotterySixPlugin.getPluginMessageHandler().updatePlayerPreference(lotteryPlayer, PlayerPreferenceKey.HIDE_TITLES, true);
                    } else {
                        lotteryPlayer.setPreference(PlayerPreferenceKey.HIDE_TITLES, true);
                    }
                    return true;
                }, Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiBettingAccountToggleHideTitles, instance)).map(e -> e.replace("{Status}", display)).toArray(String[]::new));
            }
        }));
        gui.addElement(new DynamicGuiElement('d', () -> {
            boolean value = lotteryPlayer.getPreference(PlayerPreferenceKey.HIDE_PERIODIC_ANNOUNCEMENTS, boolean.class);
            String display = value ? instance.trueFormat : instance.falseFormat;
            if (value) {
                return new StaticGuiElement('d', XMaterial.LIME_DYE.parseItem(), click -> {
                    if (instance.backendBungeecordMode) {
                        LotterySixPlugin.getPluginMessageHandler().updatePlayerPreference(lotteryPlayer, PlayerPreferenceKey.HIDE_PERIODIC_ANNOUNCEMENTS, false);
                    } else {
                        lotteryPlayer.setPreference(PlayerPreferenceKey.HIDE_PERIODIC_ANNOUNCEMENTS, false);
                    }
                    return true;
                }, Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiBettingAccountToggleHidePeriodicAnnouncements, instance)).map(e -> e.replace("{Status}", display)).toArray(String[]::new));
            } else {
                return new StaticGuiElement('d', XMaterial.GRAY_DYE.parseItem(), click -> {
                    if (instance.backendBungeecordMode) {
                        LotterySixPlugin.getPluginMessageHandler().updatePlayerPreference(lotteryPlayer, PlayerPreferenceKey.HIDE_PERIODIC_ANNOUNCEMENTS, true);
                    } else {
                        lotteryPlayer.setPreference(PlayerPreferenceKey.HIDE_PERIODIC_ANNOUNCEMENTS, true);
                    }
                    return true;
                }, Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiBettingAccountToggleHidePeriodicAnnouncements, instance)).map(e -> e.replace("{Status}", display)).toArray(String[]::new));
            }
        }));
        gui.addElement(new DynamicGuiElement('e', () -> {
            boolean value = lotteryPlayer.getPreference(PlayerPreferenceKey.REOPEN_MENU_ON_PURCHASE, boolean.class);
            String display = value ? instance.trueFormat : instance.falseFormat;
            if (value) {
                return new StaticGuiElement('e', XMaterial.LIME_DYE.parseItem(), click -> {
                    if (instance.backendBungeecordMode) {
                        LotterySixPlugin.getPluginMessageHandler().updatePlayerPreference(lotteryPlayer, PlayerPreferenceKey.REOPEN_MENU_ON_PURCHASE, false);
                    } else {
                        lotteryPlayer.setPreference(PlayerPreferenceKey.REOPEN_MENU_ON_PURCHASE, false);
                    }
                    return true;
                }, Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiBettingAccountToggleReopenMenu, instance)).map(e -> e.replace("{Status}", display)).toArray(String[]::new));
            } else {
                return new StaticGuiElement('e', XMaterial.GRAY_DYE.parseItem(), click -> {
                    if (instance.backendBungeecordMode) {
                        LotterySixPlugin.getPluginMessageHandler().updatePlayerPreference(lotteryPlayer, PlayerPreferenceKey.REOPEN_MENU_ON_PURCHASE, true);
                    } else {
                        lotteryPlayer.setPreference(PlayerPreferenceKey.REOPEN_MENU_ON_PURCHASE, true);
                    }
                    return true;
                }, Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiBettingAccountToggleReopenMenu, instance)).map(e -> e.replace("{Status}", display)).toArray(String[]::new));
            }
        }));
        gui.addElement(new DynamicGuiElement('f', () -> {
            long realValue = lotteryPlayer.getPreference(PlayerPreferenceKey.BET_LIMIT_PER_ROUND, long.class);
            boolean active = lotteryPlayer.isPreferenceSet(PlayerPreferenceKey.BET_LIMIT_PER_ROUND);
            String value = active ? StringUtils.formatComma(realValue) : "-";
            ItemStack left = XMaterial.PAPER.parseItem();
            ItemMeta leftMeta = left.getItemMeta();
            leftMeta.setDisplayName(Math.max(0, realValue) + "");
            left.setItemMeta(leftMeta);
            return new StaticGuiElement('g', active ? setEnchanted(XMaterial.OAK_FENCE_GATE.parseItem()) : XMaterial.OAK_FENCE_GATE.parseItem(), click -> {
                if (click.getType().isLeftClick()) {
                    Scheduler.runTaskLater(plugin, () -> close(click.getWhoClicked(), click.getGui(), false), 1, player);
                    Scheduler.runTaskLater(plugin, () -> new AnvilGUI.Builder().plugin(plugin)
                            .title(LotteryUtils.formatPlaceholders(player, instance.guiBettingAccountSetBetLimitPerRoundTitle, instance))
                            .itemLeft(left)
                            .onComplete(completion -> {
                                Scheduler.runTaskAsynchronously(plugin, () -> {
                                    String input = completion.getText().trim();
                                    try {
                                        long newValue = Math.max(0, Long.parseLong(input));
                                        if (instance.backendBungeecordMode) {
                                            LotterySixPlugin.getPluginMessageHandler().updatePlayerPreference(lotteryPlayer, PlayerPreferenceKey.BET_LIMIT_PER_ROUND, newValue);
                                        } else {
                                            lotteryPlayer.setPreference(PlayerPreferenceKey.BET_LIMIT_PER_ROUND, newValue);
                                        }
                                        Scheduler.runTaskLater(plugin, () -> getMainMenu(player).show(player), 2, player);
                                        Scheduler.runTaskLater(plugin, () -> getBettingAccount(player).show(player), 3, player);
                                    } catch (Exception e) {
                                        player.sendMessage(instance.messageInvalidUsage);
                                    }
                                });
                                return Collections.singletonList(AnvilGUI.ResponseAction.close());
                            })
                            .open(player), 2, player);
                } else if (click.getType().isRightClick()) {
                    if (instance.backendBungeecordMode) {
                        LotterySixPlugin.getPluginMessageHandler().resetPlayerPreference(lotteryPlayer, PlayerPreferenceKey.BET_LIMIT_PER_ROUND);
                    } else {
                        lotteryPlayer.resetPreference(PlayerPreferenceKey.BET_LIMIT_PER_ROUND);
                    }
                }
                return true;
            }, Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiBettingAccountSetBetLimitPerRound, instance)).map(s -> s.replace("{Value}", value)).toArray(String[]::new));
        }));
        gui.addElement(new DynamicGuiElement('g', () -> {
            long realValue = lotteryPlayer.getPreference(PlayerPreferenceKey.SUSPEND_ACCOUNT_UNTIL, long.class);
            boolean active = System.currentTimeMillis() < realValue;
            String value = active ? instance.dateFormat.format(realValue) : "-";
            return new StaticGuiElement('f', active ? setEnchanted(XMaterial.ANVIL.parseItem()) : XMaterial.ANVIL.parseItem(), click -> {
                if (click.getType().isLeftClick()) {
                    long newValue = Math.max(System.currentTimeMillis(), realValue) + 604800000;
                    if (instance.backendBungeecordMode) {
                        LotterySixPlugin.getPluginMessageHandler().updatePlayerPreference(lotteryPlayer, PlayerPreferenceKey.SUSPEND_ACCOUNT_UNTIL, newValue);
                    } else {
                        lotteryPlayer.setPreference(PlayerPreferenceKey.SUSPEND_ACCOUNT_UNTIL, newValue);
                    }
                } else if (click.getType().isRightClick()) {
                    if (instance.backendBungeecordMode) {
                        LotterySixPlugin.getPluginMessageHandler().resetPlayerPreference(lotteryPlayer, PlayerPreferenceKey.SUSPEND_ACCOUNT_UNTIL);
                    } else {
                        lotteryPlayer.resetPreference(PlayerPreferenceKey.SUSPEND_ACCOUNT_UNTIL);
                    }
                }
                return true;
            }, Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiBettingAccountSuspendAccountForAWeek, instance)).map(s -> s.replace("{Value}", value)).toArray(String[]::new));
        }));
        gui.addElement(new StaticGuiElement('$', XMaterial.EMERALD.parseItem(), click -> {
            Scheduler.runTaskLater(plugin, () -> getTransactionMenu((Player) click.getWhoClicked()).show(click.getWhoClicked()), 1, player);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.guiMainMenuAccountFundTransfer, instance)));
        gui.setSilent(true);
        new ScheduledRunnable() {
            @Override
            public void run() {
                Deque<InventoryGui> history = InventoryGui.getHistory(player);
                if (!history.contains(gui)) {
                    cancel();
                    return;
                }
                if (Objects.equals(history.peekLast(), gui)) {
                    gui.draw(player);
                }
            }
        }.runTaskTimer(plugin, 10, 10, player);
        return gui;
    }

    public InventoryGui getTransactionMenu(Player player) {
        String[] guiSetup = {
                "         ",
                "bzaaacaaa",
                "azaaaaaaa",
                "azadaeafa",
                "         "
        };
        InventoryGui gui = new InventoryGui(plugin, LotteryUtils.formatPlaceholders(player, instance.guiAccountFundTransferTitle, instance), guiSetup);
        gui.setFiller(XMaterial.BLUE_STAINED_GLASS_PANE.parseItem());
        gui.addElement(new StaticGuiElement('a', new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));
        gui.addElement(new StaticGuiElement('z', XMaterial.ORANGE_STAINED_GLASS_PANE.parseItem(), ChatColor.LIGHT_PURPLE.toString()));
        gui.addElement(new StaticGuiElement('b', SkinUtils.getSkull(player.getUniqueId()), LotteryUtils.formatPlaceholders(player, instance.guiBettingAccountProfile, instance)));

        LotteryPlayer lotteryPlayer = instance.getLotteryPlayerManager().getLotteryPlayer(player.getUniqueId());
        long money = lotteryPlayer.getStats(PlayerStatsKey.ACCOUNT_BALANCE, long.class);
        gui.addElement(new StaticGuiElement('c', XMaterial.GOLD_INGOT.parseItem(), Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiAccountFundTransferCurrentBalance, instance)).map(s -> s.replace("{Amount}", StringUtils.formatComma(money))).toArray(String[]::new)));

        long time = lotteryPlayer.getPreference(PlayerPreferenceKey.SUSPEND_ACCOUNT_UNTIL, long.class);
        if (System.currentTimeMillis() < time) {
            gui.addElement(new StaticGuiElement('d', new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));
            gui.addElement(new StaticGuiElement('e', new ItemStack(Material.BARRIER), click -> {
                player.sendMessage(instance.messageBettingAccountSuspended.replace("{Date}", instance.dateFormat.format(new Date(time))).replace("{Price}", "0"));
                Scheduler.runTaskLater(plugin, () -> close(player, gui, false), 1, player);
                return true;
            }, ChatColor.RED + "-"));
            gui.addElement(new StaticGuiElement('f', new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));
        } else {
            gui.addElement(new StaticGuiElement(money > 0 ? 'd' : 'e', XMaterial.EMERALD.parseItem(), click -> {
                Scheduler.runTaskLater(plugin, () -> close(player, gui, false), 1, player);
                Scheduler.runTaskLater(plugin, () -> getTransactionInput(player, AccountTransactionMode.DEPOSIT).open(player), 2, player);
                return true;
            }, Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiAccountFundTransferDeposit, instance)).map(s -> s.replace("{Amount}", StringUtils.formatComma(money))).toArray(String[]::new)));
            if (money > 0) {
                gui.addElement(new StaticGuiElement('e', XMaterial.RED_DYE.parseItem(), click -> {
                    Scheduler.runTaskLater(plugin, () -> close(player, gui, false), 1, player);
                    Scheduler.runTaskLater(plugin, () -> getTransactionInput(player, AccountTransactionMode.WITHDRAW).open(player), 2, player);
                    return true;
                }, Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiAccountFundTransferWithdraw, instance)).map(s -> s.replace("{Amount}", StringUtils.formatComma(money))).toArray(String[]::new)));
                gui.addElement(new StaticGuiElement('f', setEnchanted(XMaterial.RED_DYE.parseItem()), click -> {
                    Scheduler.runTaskLater(plugin, () -> close(player, gui, false), 1, player);
                    Scheduler.runTaskAsynchronously(plugin, () -> {
                        long total = lotteryPlayer.getStats(PlayerStatsKey.ACCOUNT_BALANCE, long.class);
                        if (instance.giveMoney(player.getUniqueId(), total)) {
                            if (instance.backendBungeecordMode) {
                                long current = lotteryPlayer.getStats(PlayerStatsKey.ACCOUNT_BALANCE, long.class);
                                LotterySixPlugin.getPluginMessageHandler().updatePlayerStats(lotteryPlayer, PlayerStatsKey.ACCOUNT_BALANCE, current - total);
                            } else {
                                lotteryPlayer.updateStats(PlayerStatsKey.ACCOUNT_BALANCE, long.class, i -> i - total);
                            }
                            player.sendMessage(instance.messageWithdrawSuccess.replace("{Amount}", StringUtils.formatComma(total)));
                            Scheduler.runTaskLater(plugin, () -> checkReopen(player), 5, player);
                        } else {
                            player.sendMessage(instance.messageWithdrawFailed);
                        }
                    });
                    return true;
                }, Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiAccountFundTransferWithdrawAll, instance)).map(s -> s.replace("{Amount}", StringUtils.formatComma(money))).toArray(String[]::new)));
            } else {
                gui.addElement(new StaticGuiElement('d', new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));
                gui.addElement(new StaticGuiElement('f', new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));
            }
        }

        gui.setSilent(true);
        return gui;
    }

    public AnvilGUI.Builder getTransactionInput(Player player, AccountTransactionMode transactionMode) {
        ItemStack left = XMaterial.GOLD_INGOT.parseItem();
        ItemMeta leftMeta = left.getItemMeta();
        leftMeta.setDisplayName("0");
        left.setItemMeta(leftMeta);
        return new AnvilGUI.Builder().plugin(plugin)
                .title(LotteryUtils.formatPlaceholders(player, transactionMode.equals(AccountTransactionMode.WITHDRAW) ? instance.guiAccountFundTransferWithdrawInputTitle : instance.guiAccountFundTransferDepositInputTitle, instance))
                .itemLeft(left)
                .onComplete(completion -> {
                    Scheduler.runTaskAsynchronously(plugin, () -> {
                        String input = completion.getText().trim();
                        try {
                            long amount = Long.parseLong(input);
                            Scheduler.runTaskAsynchronously(plugin, () -> {
                                if (amount <= 0) {
                                    player.sendMessage(instance.messageInvalidNumber);
                                } else {
                                    LotteryPlayer lotteryPlayer = instance.getLotteryPlayerManager().getLotteryPlayer(player.getUniqueId());
                                    long money = lotteryPlayer.getStats(PlayerStatsKey.ACCOUNT_BALANCE, long.class);
                                    if (transactionMode.equals(AccountTransactionMode.WITHDRAW)) {
                                        if (money < amount) {
                                            player.sendMessage(instance.messageNotEnoughMoney.replace("{Price}", StringUtils.formatComma(amount)));
                                        } else if (instance.giveMoney(player.getUniqueId(), amount)) {
                                            if (instance.backendBungeecordMode) {
                                                long current = lotteryPlayer.getStats(PlayerStatsKey.ACCOUNT_BALANCE, long.class);
                                                LotterySixPlugin.getPluginMessageHandler().updatePlayerStats(lotteryPlayer, PlayerStatsKey.ACCOUNT_BALANCE, current - amount);
                                            } else {
                                                lotteryPlayer.updateStats(PlayerStatsKey.ACCOUNT_BALANCE, long.class, i -> i - amount);
                                            }
                                            player.sendMessage(instance.messageWithdrawSuccess.replace("{Amount}", StringUtils.formatComma(amount)));
                                            Scheduler.runTaskLater(plugin, () -> checkReopen(player), 5, player);
                                        } else {
                                            player.sendMessage(instance.messageWithdrawFailed);
                                        }
                                    } else {
                                        if (instance.takeMoney(player.getUniqueId(), amount)) {
                                            if (instance.backendBungeecordMode) {
                                                long current = lotteryPlayer.getStats(PlayerStatsKey.ACCOUNT_BALANCE, long.class);
                                                LotterySixPlugin.getPluginMessageHandler().updatePlayerStats(lotteryPlayer, PlayerStatsKey.ACCOUNT_BALANCE, current + amount);
                                            } else {
                                                lotteryPlayer.updateStats(PlayerStatsKey.ACCOUNT_BALANCE, long.class, i -> i + amount);
                                            }
                                            player.sendMessage(instance.messageDepositSuccess.replace("{Amount}", StringUtils.formatComma(amount)));
                                            Scheduler.runTaskLater(plugin, () -> checkReopen(player), 5, player);
                                        } else {
                                            player.sendMessage(instance.messageNotEnoughMoney.replace("{Price}", StringUtils.formatComma(amount)));
                                        }
                                    }
                                }
                            });
                        } catch (NumberFormatException e) {
                            player.sendMessage(instance.messageInvalidNumber);
                        }
                    });
                    return Collections.singletonList(AnvilGUI.ResponseAction.close());
                });
    }

    public void checkFixedTransactionDeposit(Player player, long amount, Runnable onSuccess) {
        LotteryPlayer lotteryPlayer = instance.getLotteryPlayerManager().getLotteryPlayer(player.getUniqueId());
        long time = lotteryPlayer.getPreference(PlayerPreferenceKey.SUSPEND_ACCOUNT_UNTIL, long.class);
        if (System.currentTimeMillis() < time) {
            player.sendMessage(instance.messageBettingAccountSuspended.replace("{Date}", instance.dateFormat.format(new Date(time))).replace("{Price}", StringUtils.formatComma(amount)));
            InventoryGui gui = InventoryGui.getOpen(player);
            if (gui != null) {
                Scheduler.runTaskLater(plugin, () -> close(player, gui, false), 1, player);
            }
            return;
        }
        long money = lotteryPlayer.getStats(PlayerStatsKey.ACCOUNT_BALANCE, long.class);
        if (amount > money) {
            long amountNeeded = amount - money;
            String[] guiSetup = {
                    "         ",
                    " aaaaaaa ",
                    " aabacaa ",
                    " aaaaaaa ",
                    "         "
            };
            InventoryGui gui = new InventoryGui(plugin, LotteryUtils.formatPlaceholders(player, instance.guiAccountFundTransferPlacingBetTitle, instance), guiSetup);
            gui.setFiller(XMaterial.RED_STAINED_GLASS_PANE.parseItem());
            gui.addElement(new StaticGuiElement('a', new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));
            gui.addElement(new StaticGuiElement('b', XMaterial.EMERALD.parseItem(), click -> {
                Scheduler.runTaskAsynchronously(plugin, () -> {
                    if (instance.takeMoney(player.getUniqueId(), amountNeeded)) {
                        if (instance.backendBungeecordMode) {
                            long current = lotteryPlayer.getStats(PlayerStatsKey.ACCOUNT_BALANCE, long.class);
                            LotterySixPlugin.getPluginMessageHandler().updatePlayerStats(lotteryPlayer, PlayerStatsKey.ACCOUNT_BALANCE, current + amountNeeded);
                        } else {
                            lotteryPlayer.updateStats(PlayerStatsKey.ACCOUNT_BALANCE, long.class, i -> i + amountNeeded);
                        }
                        player.sendMessage(instance.messageDepositSuccess.replace("{Amount}", StringUtils.formatComma(amount)));
                        new ScheduledRunnable() {
                            private int counter = 0;
                            @Override
                            public void run() {
                                long current = lotteryPlayer.getStats(PlayerStatsKey.ACCOUNT_BALANCE, long.class);
                                if (amount <= current) {
                                    cancel();
                                    onSuccess.run();
                                }
                                if (counter++ > 40) {
                                    cancel();
                                }
                            }
                        }.runTaskTimer(plugin, 2, 1, player);
                    } else {
                        player.sendMessage(instance.messageNotEnoughMoney.replace("{Price}", StringUtils.formatComma(amountNeeded)));
                    }
                    Scheduler.runTaskLater(plugin, () -> close(player, gui, false), 1, player);
                });
                return true;
            }, Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiAccountFundTransferPlacingBetConfirm, instance)).map(s -> s
                    .replace("{Amount}", StringUtils.formatComma(amountNeeded))).toArray(String[]::new)));
            gui.addElement(new StaticGuiElement('c', XMaterial.BARRIER.parseItem(), click -> {
                Scheduler.runTaskLater(plugin, () -> close(player, gui, false), 1, player);
                Scheduler.runTaskLater(plugin, () -> checkReopen(player), 5, player);
                return true;
            }, LotteryUtils.formatPlaceholders(player, instance.guiAccountFundTransferPlacingBetCancel, instance)));
            gui.show(player);
        } else {
            onSuccess.run();
        }
    }

    public ItemStack getPlacedBets(Player player) {
        PlayableLotterySixGame game = instance.getCurrentGame();
        List<PlayerBets> bets = game.getPlayerBets(player.getUniqueId());

        ItemStack itemStack = XMaterial.WRITTEN_BOOK.parseItem();
        BookMeta meta = (BookMeta) itemStack.getItemMeta();
        meta.setAuthor("LotterySix");
        meta.setTitle("LotterySix");
        itemStack.setItemMeta(meta);

        List<String> pages = new ArrayList<>();

        String title = LotteryUtils.formatPlaceholders(player, instance.guiYourBetsTitle, instance, game);
        if (bets.isEmpty()) {
            pages.add(title + "\n\n" + String.join("\n", LotteryUtils.formatPlaceholders(player, instance.guiYourBetsNothing, instance, game)));
        } else {
            for (PlayerBets bet : bets) {
                if (pages.size() > 100) {
                    break;
                }
                String str = title + "\n\n" + bet.getChosenNumbers().toFormattedString().replace("/ ", "/\n") + "\n";
                if (bet.isMultipleDraw()) {
                    str += instance.ticketDescriptionMultipleDraw
                            .replace("{Price}", StringUtils.formatComma(bet.getBet()))
                            .replace("{UnitPrice}", StringUtils.formatComma(instance.pricePerBet / bet.getType().getDivisor()))
                            .replace("{DrawsRemaining}", StringUtils.formatComma(bet.getDrawsRemaining()))
                            .replace("{MultipleDraw}", StringUtils.formatComma(bet.getMultipleDraw()));
                } else {
                    str += instance.ticketDescription
                            .replace("{Price}", StringUtils.formatComma(bet.getBet()))
                            .replace("{UnitPrice}", StringUtils.formatComma(instance.pricePerBet / bet.getType().getDivisor()));
                }
                pages.add(str);
            }
        }

        return BookUtils.setPages(itemStack, pages);
    }

    public InventoryGui getBetTypeChooser(Player player, PlayableLotterySixGame game) {
        String[] guiSetup = {
                "         ",
                " aaaaaaa ",
                " bacadae ",
                " aaaaaaa ",
                "         "
        };
        InventoryGui gui = new InventoryGui(plugin, LotteryUtils.formatPlaceholders(player, instance.guiSelectNewBetTypeTitle, instance), guiSetup);
        gui.setFiller(XMaterial.ORANGE_STAINED_GLASS_PANE.parseItem());
        gui.addElement(new StaticGuiElement('a', new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));
        gui.addElement(new StaticGuiElement('b', XMaterial.BRICK.parseItem(), click -> {
            Scheduler.runTaskLater(plugin, () -> getNumberChooser((Player) click.getWhoClicked(), game, BetNumbersBuilder.single(1, instance.numberOfChoices)).show(click.getWhoClicked()), 1, player);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.guiSelectNewBetTypeSingle, instance)));
        gui.addElement(new StaticGuiElement('c', XMaterial.IRON_INGOT.parseItem(), click -> {
            Scheduler.runTaskLater(plugin, () -> getNumberChooser((Player) click.getWhoClicked(), game, BetNumbersBuilder.multiple(1, instance.numberOfChoices)).show(click.getWhoClicked()), 1, player);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.guiSelectNewBetTypeMultiple, instance)));
        gui.addElement(new StaticGuiElement('d', XMaterial.GOLD_INGOT.parseItem(), click -> {
            Scheduler.runTaskLater(plugin, () -> getNumberChooser((Player) click.getWhoClicked(), game, BetNumbersBuilder.banker(1, instance.numberOfChoices)).show(click.getWhoClicked()), 1, player);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.guiSelectNewBetTypeBanker, instance)));
        gui.addElement(new StaticGuiElement('e', XMaterial.REDSTONE.parseItem(), click -> {
            Scheduler.runTaskLater(plugin, () -> getRandomEntryChooser((Player) click.getWhoClicked(), game, BetNumbersType.RANDOM).show(click.getWhoClicked()), 1, player);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.guiSelectNewBetTypeRandom, instance)));
        gui.setSilent(true);
        return gui;
    }

    public InventoryGui getRandomEntryChooser(Player player, PlayableLotterySixGame game, BetNumbersType selectedType) {
        BetNumbersType type = selectedType.isRandom() ? selectedType : BetNumbersType.RANDOM;
        String[] guiSetup = {
                "         ",
                " abacada ",
                " efghijk ",
                " lamanao ",
                "         "
        };
        InventoryGui gui = new InventoryGui(plugin, LotteryUtils.formatPlaceholders(player, instance.guiRandomEntryTitle, instance), guiSetup);
        gui.setFiller(XMaterial.RED_STAINED_GLASS_PANE.parseItem());
        gui.addElement(new StaticGuiElement('a', new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));

        gui.addElement(new StaticGuiElement('b', type.equals(BetNumbersType.RANDOM) ? setEnchanted(XMaterial.BRICK.parseItem()) : XMaterial.BRICK.parseItem(), click -> {
            Scheduler.runTaskLater(plugin, () -> {
                getRandomEntryChooser((Player) click.getWhoClicked(), game, BetNumbersType.RANDOM).show(click.getWhoClicked());
                Scheduler.runTaskLater(plugin, () -> removeSecondLast(player), 2, player);
            }, 1, player);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.guiRandomEntrySingleTab, instance)));
        gui.addElement(new StaticGuiElement('c', type.equals(BetNumbersType.MULTIPLE_RANDOM) ? setEnchanted(XMaterial.IRON_INGOT.parseItem()) : XMaterial.IRON_INGOT.parseItem(), click -> {
            Scheduler.runTaskLater(plugin, () -> {
                getRandomEntryChooser((Player) click.getWhoClicked(), game, BetNumbersType.MULTIPLE_RANDOM).show(click.getWhoClicked());
                Scheduler.runTaskLater(plugin, () -> removeSecondLast(player), 2, player);
            }, 1, player);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.guiRandomEntryMultipleTab, instance)));
        gui.addElement(new StaticGuiElement('d', type.equals(BetNumbersType.BANKER_RANDOM) ? setEnchanted(XMaterial.GOLD_INGOT.parseItem()) : XMaterial.GOLD_INGOT.parseItem(), click -> {
            Scheduler.runTaskLater(plugin, () -> {
                getRandomEntryChooser((Player) click.getWhoClicked(), game, BetNumbersType.BANKER_RANDOM).show(click.getWhoClicked());
                Scheduler.runTaskLater(plugin, () -> removeSecondLast(player), 2, player);
            }, 1, player);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.guiRandomEntryBankerTab, instance)));

        if (type.equals(BetNumbersType.RANDOM)) {
            gui.addElement(new StaticGuiElement('f', new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));
            gui.addElement(new StaticGuiElement('h', new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));
            gui.addElement(new StaticGuiElement('j', new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));

            gui.addElement(new StaticGuiElement('e', setItemSize(XMaterial.PAPER.parseItem(), 1), click -> {
                Scheduler.runTaskLater(plugin, () -> getSingleBulkNumberConfirm((Player) click.getWhoClicked(), game, BetNumbersBuilder.random(1, instance.numberOfChoices, 1).map(each -> each.build()).collect(Collectors.toList())).show(click.getWhoClicked()), 2, player);
                return true;
            }, Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiRandomEntryBetCountValueSimple, instance)).map(each -> each.replace("{Count}", "1").replace("{BetUnits}", "1").replace("{Price}", StringUtils.formatComma(instance.pricePerBet))).toArray(String[]::new)));
            gui.addElement(new StaticGuiElement('g', setItemSize(XMaterial.PAPER.parseItem(), 2), click -> {
                Scheduler.runTaskLater(plugin, () -> getSingleBulkNumberConfirm((Player) click.getWhoClicked(), game, BetNumbersBuilder.random(1, instance.numberOfChoices, 2).map(each -> each.build()).collect(Collectors.toList())).show(click.getWhoClicked()), 2, player);
                return true;
            }, Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiRandomEntryBetCountValueSimple, instance)).map(each -> each.replace("{Count}", "2").replace("{BetUnits}", "2").replace("{Price}", StringUtils.formatComma(instance.pricePerBet * 2))).toArray(String[]::new)));
            gui.addElement(new StaticGuiElement('i', setItemSize(XMaterial.PAPER.parseItem(), 5), click -> {
                Scheduler.runTaskLater(plugin, () -> getSingleBulkNumberConfirm((Player) click.getWhoClicked(), game, BetNumbersBuilder.random(1, instance.numberOfChoices, 5).map(each -> each.build()).collect(Collectors.toList())).show(click.getWhoClicked()), 2, player);
                return true;
            }, Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiRandomEntryBetCountValueSimple, instance)).map(each -> each.replace("{Count}", "5").replace("{BetUnits}", "5").replace("{Price}", StringUtils.formatComma(instance.pricePerBet * 5))).toArray(String[]::new)));
            gui.addElement(new StaticGuiElement('k', setItemSize(XMaterial.PAPER.parseItem(), 10), click -> {
                Scheduler.runTaskLater(plugin, () -> getSingleBulkNumberConfirm((Player) click.getWhoClicked(), game, BetNumbersBuilder.random(1, instance.numberOfChoices, 10).map(each -> each.build()).collect(Collectors.toList())).show(click.getWhoClicked()), 2, player);
                return true;
            }, Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiRandomEntryBetCountValueSimple, instance)).map(each -> each.replace("{Count}", "10").replace("{BetUnits}", "10").replace("{Price}", StringUtils.formatComma(instance.pricePerBet * 10))).toArray(String[]::new)));
            gui.addElement(new StaticGuiElement('l', setItemSize(XMaterial.PAPER.parseItem(), 20), click -> {
                Scheduler.runTaskLater(plugin, () -> getSingleBulkNumberConfirm((Player) click.getWhoClicked(), game, BetNumbersBuilder.random(1, instance.numberOfChoices, 20).map(each -> each.build()).collect(Collectors.toList())).show(click.getWhoClicked()), 2, player);
                return true;
            }, Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiRandomEntryBetCountValueSimple, instance)).map(each -> each.replace("{Count}", "20").replace("{BetUnits}", "20").replace("{Price}", StringUtils.formatComma(instance.pricePerBet * 20))).toArray(String[]::new)));
            gui.addElement(new StaticGuiElement('m', setItemSize(XMaterial.PAPER.parseItem(), 40), click -> {
                Scheduler.runTaskLater(plugin, () -> getSingleBulkNumberConfirm((Player) click.getWhoClicked(), game, BetNumbersBuilder.random(1, instance.numberOfChoices, 40).map(each -> each.build()).collect(Collectors.toList())).show(click.getWhoClicked()), 2, player);
                return true;
            }, Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiRandomEntryBetCountValueSimple, instance)).map(each -> each.replace("{Count}", "40").replace("{BetUnits}", "40").replace("{Price}", StringUtils.formatComma(instance.pricePerBet * 40))).toArray(String[]::new)));
            gui.addElement(new StaticGuiElement('n', setItemSize(XMaterial.PAPER.parseItem(), 50), click -> {
                Scheduler.runTaskLater(plugin, () -> getSingleBulkNumberConfirm((Player) click.getWhoClicked(), game, BetNumbersBuilder.random(1, instance.numberOfChoices, 50).map(each -> each.build()).collect(Collectors.toList())).show(click.getWhoClicked()), 2, player);
                return true;
            }, Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiRandomEntryBetCountValueSimple, instance)).map(each -> each.replace("{Count}", "50").replace("{BetUnits}", "50").replace("{Price}", StringUtils.formatComma(instance.pricePerBet * 50))).toArray(String[]::new)));
            gui.addElement(new StaticGuiElement('o', setEnchanted(XMaterial.PAPER.parseItem()), click -> {
                Scheduler.runTaskLater(plugin, () -> getSingleBulkNumberConfirm((Player) click.getWhoClicked(), game, BetNumbersBuilder.random(1, instance.numberOfChoices, 100).map(each -> each.build()).collect(Collectors.toList())).show(click.getWhoClicked()), 2, player);
                return true;
            }, Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiRandomEntryBetCountValueSimple, instance)).map(each -> each.replace("{Count}", "100").replace("{BetUnits}", "100").replace("{Price}", StringUtils.formatComma(instance.pricePerBet * 100))).toArray(String[]::new)));
        } else if (type.equals(BetNumbersType.MULTIPLE_RANDOM)) {
            gui.addElement(new StaticGuiElement('e', new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));
            gui.addElement(new StaticGuiElement('f', new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));
            gui.addElement(new StaticGuiElement('j', new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));
            gui.addElement(new StaticGuiElement('k', new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));

            AtomicInteger size = new AtomicInteger(LotteryRegistry.NUMBERS_PER_BET + 1);
            gui.addElement(new StaticGuiElement('g', XMaterial.ARROW.parseItem(), click -> {
                int decrement = click.getType().isRightClick() ? 10 : 1;
                size.updateAndGet(i -> Math.max(LotteryRegistry.NUMBERS_PER_BET + 1, i - decrement));
                gui.draw(player);
                return true;
            }, LotteryUtils.formatPlaceholders(player, instance.guiRandomEntryDecrementButton, instance)));
            gui.addElement(new StaticGuiElement('i', XMaterial.ARROW.parseItem(), click -> {
                int increment = click.getType().isRightClick() ? 10 : 1;
                size.updateAndGet(i -> Math.min(instance.numberOfChoices, i + increment));
                gui.draw(player);
                return true;
            }, LotteryUtils.formatPlaceholders(player, instance.guiRandomEntryIncrementButton, instance)));

            gui.addElement(new DynamicGuiElement('h', () -> new StaticGuiElement('g', getNumberItem(size.get(), NumberSelectedState.NOT_SELECTED), Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiRandomEntryMultipleSizeValue, instance)).map(each -> each.replace("{Count}", size.get() + "")).toArray(String[]::new))));

            gui.addElement(new DynamicGuiElement('l', () -> {
                long price = LotteryUtils.calculatePrice(size.get(), 0, instance.pricePerBet);
                return new StaticGuiElement('l', setItemSize(XMaterial.PAPER.parseItem(), 1), click -> {
                    Scheduler.runTaskLater(plugin, () -> getComplexBulkNumberConfirm((Player) click.getWhoClicked(), game, BetNumbersBuilder.multipleRandom(1, instance.numberOfChoices, size.get(), 1).map(each -> each.build()).collect(Collectors.toList())).show(click.getWhoClicked()), 2, player);
                    return true;
                }, Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiRandomEntryBetCountValueComplex, instance)).map(each -> each
                        .replace("{Count}", "1")
                        .replace("{BetUnits}", StringUtils.formatComma(price / instance.pricePerBet))
                        .replace("{Price}", StringUtils.formatComma(price))
                        .replace("{PricePartial}", StringUtils.formatComma(price / BetUnitType.PARTIAL.getDivisor()))).toArray(String[]::new));
            }));
            gui.addElement(new DynamicGuiElement('m', () -> {
                long price = LotteryUtils.calculatePrice(size.get(), 0, instance.pricePerBet);
                return new StaticGuiElement('m', setItemSize(XMaterial.PAPER.parseItem(), 2), click -> {
                    Scheduler.runTaskLater(plugin, () -> getComplexBulkNumberConfirm((Player) click.getWhoClicked(), game, BetNumbersBuilder.multipleRandom(1, instance.numberOfChoices, size.get(), 2).map(each -> each.build()).collect(Collectors.toList())).show(click.getWhoClicked()), 2, player);
                    return true;
                }, Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiRandomEntryBetCountValueComplex, instance)).map(each -> each
                        .replace("{Count}", "2")
                        .replace("{BetUnits}", StringUtils.formatComma(price * 2 / instance.pricePerBet))
                        .replace("{Price}", StringUtils.formatComma(price * 2))
                        .replace("{PricePartial}", StringUtils.formatComma(price * 2 / BetUnitType.PARTIAL.getDivisor()))).toArray(String[]::new));
            }));
            gui.addElement(new DynamicGuiElement('n', () -> {
                long price = LotteryUtils.calculatePrice(size.get(), 0, instance.pricePerBet);
                return new StaticGuiElement('n', setItemSize(XMaterial.PAPER.parseItem(), 5), click -> {
                    Scheduler.runTaskLater(plugin, () -> getComplexBulkNumberConfirm((Player) click.getWhoClicked(), game, BetNumbersBuilder.multipleRandom(1, instance.numberOfChoices, size.get(), 5).map(each -> each.build()).collect(Collectors.toList())).show(click.getWhoClicked()), 2, player);
                    return true;
                }, Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiRandomEntryBetCountValueComplex, instance)).map(each -> each
                        .replace("{Count}", "5")
                        .replace("{BetUnits}", StringUtils.formatComma(price * 5 / instance.pricePerBet))
                        .replace("{Price}", StringUtils.formatComma(price * 5))
                        .replace("{PricePartial}", StringUtils.formatComma(price * 5 / BetUnitType.PARTIAL.getDivisor()))).toArray(String[]::new));
            }));
            gui.addElement(new DynamicGuiElement('o', () -> {
                long price = LotteryUtils.calculatePrice(size.get(), 0, instance.pricePerBet);
                return new StaticGuiElement('o', setItemSize(XMaterial.PAPER.parseItem(), 10), click -> {
                    Scheduler.runTaskLater(plugin, () -> getComplexBulkNumberConfirm((Player) click.getWhoClicked(), game, BetNumbersBuilder.multipleRandom(1, instance.numberOfChoices, size.get(), 10).map(each -> each.build()).collect(Collectors.toList())).show(click.getWhoClicked()), 2, player);
                    return true;
                }, Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiRandomEntryBetCountValueComplex, instance)).map(each -> each
                        .replace("{Count}", "10")
                        .replace("{BetUnits}", StringUtils.formatComma(price * 10 / instance.pricePerBet))
                        .replace("{Price}", StringUtils.formatComma(price * 10))
                        .replace("{PricePartial}", StringUtils.formatComma(price * 10 / BetUnitType.PARTIAL.getDivisor()))).toArray(String[]::new));
            }));
        } else if (type.equals(BetNumbersType.BANKER_RANDOM)) {
            gui.addElement(new StaticGuiElement('h', new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));

            AtomicInteger bankerSize = new AtomicInteger(1);
            AtomicInteger selectionSize = new AtomicInteger(LotteryRegistry.NUMBERS_PER_BET);

            gui.addElement(new StaticGuiElement('e', XMaterial.ARROW.parseItem(), click -> {
                int decrement = click.getType().isRightClick() ? 10 : 1;
                bankerSize.updateAndGet(i -> Math.max(1, i - decrement));
                selectionSize.updateAndGet(i -> Math.max(LotteryRegistry.NUMBERS_PER_BET + 1 - bankerSize.get(), i));
                gui.draw(player);
                return true;
            }, LotteryUtils.formatPlaceholders(player, instance.guiRandomEntryDecrementButton, instance)));
            gui.addElement(new StaticGuiElement('g', XMaterial.ARROW.parseItem(), click -> {
                int increment = click.getType().isRightClick() ? 10 : 1;
                bankerSize.updateAndGet(i -> Math.min(LotteryRegistry.NUMBERS_PER_BET - 1, i + increment));
                selectionSize.updateAndGet(i -> Math.min(instance.numberOfChoices - bankerSize.get(), i));
                gui.draw(player);
                return true;
            }, LotteryUtils.formatPlaceholders(player, instance.guiRandomEntryIncrementButton, instance)));

            gui.addElement(new StaticGuiElement('i', XMaterial.ARROW.parseItem(), click -> {
                int decrement = click.getType().isRightClick() ? 10 : 1;
                selectionSize.updateAndGet(i -> Math.max(LotteryRegistry.NUMBERS_PER_BET + 1 - bankerSize.get(), i - decrement));
                gui.draw(player);
                return true;
            }, LotteryUtils.formatPlaceholders(player, instance.guiRandomEntryDecrementButton, instance)));
            gui.addElement(new StaticGuiElement('k', XMaterial.ARROW.parseItem(), click -> {
                int increment = click.getType().isRightClick() ? 10 : 1;
                selectionSize.updateAndGet(i -> Math.min(instance.numberOfChoices - bankerSize.get(), i + increment));
                gui.draw(player);
                return true;
            }, LotteryUtils.formatPlaceholders(player, instance.guiRandomEntryIncrementButton, instance)));

            gui.addElement(new DynamicGuiElement('f', () -> new StaticGuiElement('f', getNumberItem(bankerSize.get(), NumberSelectedState.NOT_SELECTED), Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiRandomEntryBankerBankersValue, instance)).map(each -> each.replace("{Count}", bankerSize.get() + "")).toArray(String[]::new))));
            gui.addElement(new DynamicGuiElement('j', () -> new StaticGuiElement('j', getNumberItem(selectionSize.get(), NumberSelectedState.NOT_SELECTED), Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiRandomEntryBankerSelectionsValue, instance)).map(each -> each.replace("{Count}", selectionSize.get() + "")).toArray(String[]::new))));


            gui.addElement(new DynamicGuiElement('l', () -> {
                long price = LotteryUtils.calculatePrice(selectionSize.get(), bankerSize.get(), instance.pricePerBet);
                return new StaticGuiElement('l', setItemSize(XMaterial.PAPER.parseItem(), 1), click -> {
                    Scheduler.runTaskLater(plugin, () -> getComplexBulkNumberConfirm((Player) click.getWhoClicked(), game, BetNumbersBuilder.bankerRandom(1, instance.numberOfChoices, bankerSize.get(), selectionSize.get(), 1).map(each -> each.build()).collect(Collectors.toList())).show(click.getWhoClicked()), 2, player);
                    return true;
                }, Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiRandomEntryBetCountValueComplex, instance)).map(each -> each
                        .replace("{Count}", "1")
                        .replace("{BetUnits}", StringUtils.formatComma(price / instance.pricePerBet))
                        .replace("{Price}", StringUtils.formatComma(price))
                        .replace("{PricePartial}", StringUtils.formatComma(price / BetUnitType.PARTIAL.getDivisor()))).toArray(String[]::new));
            }));
            gui.addElement(new DynamicGuiElement('m', () -> {
                long price = LotteryUtils.calculatePrice(selectionSize.get(), bankerSize.get(), instance.pricePerBet);
                return new StaticGuiElement('m', setItemSize(XMaterial.PAPER.parseItem(), 2), click -> {
                    Scheduler.runTaskLater(plugin, () -> getComplexBulkNumberConfirm((Player) click.getWhoClicked(), game, BetNumbersBuilder.bankerRandom(1, instance.numberOfChoices, bankerSize.get(), selectionSize.get(), 2).map(each -> each.build()).collect(Collectors.toList())).show(click.getWhoClicked()), 2, player);
                    return true;
                }, Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiRandomEntryBetCountValueComplex, instance)).map(each -> each
                        .replace("{Count}", "2")
                        .replace("{BetUnits}", StringUtils.formatComma(price * 2 / instance.pricePerBet))
                        .replace("{Price}", StringUtils.formatComma(price * 2))
                        .replace("{PricePartial}", StringUtils.formatComma(price * 2 / BetUnitType.PARTIAL.getDivisor()))).toArray(String[]::new));
            }));
            gui.addElement(new DynamicGuiElement('n', () -> {
                long price = LotteryUtils.calculatePrice(selectionSize.get(), bankerSize.get(), instance.pricePerBet);
                return new StaticGuiElement('n', setItemSize(XMaterial.PAPER.parseItem(), 5), click -> {
                    Scheduler.runTaskLater(plugin, () -> getComplexBulkNumberConfirm((Player) click.getWhoClicked(), game, BetNumbersBuilder.bankerRandom(1, instance.numberOfChoices, bankerSize.get(), selectionSize.get(), 5).map(each -> each.build()).collect(Collectors.toList())).show(click.getWhoClicked()), 2, player);
                    return true;
                }, Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiRandomEntryBetCountValueComplex, instance)).map(each -> each
                        .replace("{Count}", "5")
                        .replace("{BetUnits}", StringUtils.formatComma(price * 5 / instance.pricePerBet))
                        .replace("{Price}", StringUtils.formatComma(price * 5))
                        .replace("{PricePartial}", StringUtils.formatComma(price * 5 / BetUnitType.PARTIAL.getDivisor()))).toArray(String[]::new));
            }));
            gui.addElement(new DynamicGuiElement('o', () -> {
                long price = LotteryUtils.calculatePrice(selectionSize.get(), bankerSize.get(), instance.pricePerBet);
                return new StaticGuiElement('o', setItemSize(XMaterial.PAPER.parseItem(), 10), click -> {
                    Scheduler.runTaskLater(plugin, () -> getComplexBulkNumberConfirm((Player) click.getWhoClicked(), game, BetNumbersBuilder.bankerRandom(1, instance.numberOfChoices, bankerSize.get(), selectionSize.get(), 10).map(each -> each.build()).collect(Collectors.toList())).show(click.getWhoClicked()), 2, player);
                    return true;
                }, Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiRandomEntryBetCountValueComplex, instance)).map(each -> each
                        .replace("{Count}", "10")
                        .replace("{BetUnits}", StringUtils.formatComma(price * 10 / instance.pricePerBet))
                        .replace("{Price}", StringUtils.formatComma(price * 10))
                        .replace("{PricePartial}", StringUtils.formatComma(price * 10 / BetUnitType.PARTIAL.getDivisor()))).toArray(String[]::new));
            }));
        }

        gui.setSilent(true);
        return gui;
    }

    public InventoryGui getNumberChooser(Player player, PlayableLotterySixGame game, BetNumbersBuilder builder) {
        int num = instance.numberOfChoices;
        String[] guiSetup = fillChars((num + (builder.getType().isMultipleCombination() ? 2 : 1)) / 9 + 1);
        String last = guiSetup[guiSetup.length - 1];
        guiSetup[guiSetup.length - 1] = last.substring(0, last.length() - 2) + "\1\0";
        String title;
        boolean isBanker = builder.getType().isBanker();
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
        InventoryGui gui = new InventoryGui(plugin, LotteryUtils.formatPlaceholders(player, title.replace("{Price}", StringUtils.formatComma(LotteryUtils.calculatePrice(builder, instance))).replace("{MinSelection}", ((isBanker ? ((BetNumbersBuilder.BankerBuilder) builder).getMinSelectionsNeeded() : LotteryRegistry.NUMBERS_PER_BET) - builder.size()) + ""), instance, game), guiSetup);
        char c = 'a';
        IntObjectConsumer<Player> handleClick = (number, clicker) -> {
            if (builder.completed() || (isBanker && !((BetNumbersBuilder.BankerBuilder) builder).getBankers().isEmpty())) {
                if (isBanker && !((BetNumbersBuilder.BankerBuilder) builder).inSelectionPhase() && ((BetNumbersBuilder.BankerBuilder) builder).bankerCompleted()) {
                    ((BetNumbersBuilder.BankerBuilder) builder).finishBankers();
                    gui.removeElement('\0');
                    gui.addElement(new StaticGuiElement('\0', XMaterial.GRAY_DYE.parseItem(), LotteryUtils.formatPlaceholders(player, instance.guiNewBetNotYetFinish, instance, game)));
                    for (int banker : ((BetNumbersBuilder.BankerBuilder) builder).getBankers()) {
                        char bankerC = (char) ('a' + banker - 1);
                        gui.removeElement(bankerC);
                        gui.addElement(new StaticGuiElement(bankerC, getNumberItem(banker, NumberSelectedState.SELECTED_BANKER), ChatColor.YELLOW + "" + banker));
                    }
                } else if (builder.completed() || (isBanker && !((BetNumbersBuilder.BankerBuilder) builder).inSelectionPhase() && !((BetNumbersBuilder.BankerBuilder) builder).getBankers().isEmpty())) {
                    long price = LotteryUtils.calculatePrice(builder, instance);
                    gui.removeElement('\0');
                    gui.addElement(new StaticGuiElement('\0', isBanker && !((BetNumbersBuilder.BankerBuilder) builder).inSelectionPhase() ? XMaterial.EMERALD.parseItem() : XMaterial.GOLD_INGOT.parseItem(), click -> {
                        if (builder.completed()) {
                            Scheduler.runTaskLater(plugin, () -> {
                                BetNumbers betNumbers = builder.build();
                                if (betNumbers.getType().equals(BetNumbersType.SINGLE)) {
                                    getSingleNumberConfirm((Player) click.getWhoClicked(), game, betNumbers).show(click.getWhoClicked());
                                } else {
                                    getComplexNumberConfirm((Player) click.getWhoClicked(), game, betNumbers).show(click.getWhoClicked());
                                }
                            }, 2, player);
                        } else if (isBanker && !((BetNumbersBuilder.BankerBuilder) builder).inSelectionPhase() && !((BetNumbersBuilder.BankerBuilder) builder).getBankers().isEmpty()) {
                            ((BetNumbersBuilder.BankerBuilder) builder).finishBankers();
                            gui.removeElement('\0');
                            gui.addElement(new StaticGuiElement('\0', XMaterial.GRAY_DYE.parseItem(), LotteryUtils.formatPlaceholders(player, instance.guiNewBetNotYetFinish, instance, game)));
                            for (int banker : ((BetNumbersBuilder.BankerBuilder) builder).getBankers()) {
                                char bankerC = (char) ('a' + banker - 1);
                                gui.removeElement(bankerC);
                                gui.addElement(new StaticGuiElement(bankerC, getNumberItem(banker, NumberSelectedState.SELECTED_BANKER), ChatColor.YELLOW + "" + banker));
                            }
                            gui.draw(clicker);
                        }
                        return true;
                    }, LotteryUtils.formatPlaceholders(player, Arrays.stream(isBanker && !((BetNumbersBuilder.BankerBuilder) builder).inSelectionPhase() ? instance.guiNewBetFinishBankers : (builder.getType().isMultipleCombination() ? instance.guiNewBetFinishComplex : instance.guiNewBetFinishSimple))
                            .map(each -> each
                                    .replace("{BetUnits}", StringUtils.formatComma(price / instance.pricePerBet))
                                    .replace("{Price}", StringUtils.formatComma(price))
                                    .replace("{PricePartial}", StringUtils.formatComma(price / BetUnitType.PARTIAL.getDivisor()))).toArray(String[]::new), instance, game)));
                }
            } else {
                gui.removeElement('\0');
                gui.addElement(new StaticGuiElement('\0', XMaterial.GRAY_DYE.parseItem(), LotteryUtils.formatPlaceholders(player, instance.guiNewBetNotYetFinish, instance, game)));
            }
        };
        for (int i = 0; i < num; i++) {
            int number = i + 1;
            GuiStateElement element = new GuiStateElement(c++,
                    new GuiStateElement.State(
                            change -> {
                                if (builder.canAdd()) {
                                    builder.addNumber(number);
                                    handleClick.accept(number, (Player) change.getWhoClicked());
                                } else {
                                    ((GuiStateElement) change.getElement()).nextState();
                                }
                                gui.draw(change.getWhoClicked());
                            },
                            "true",
                            getNumberItem(number, NumberSelectedState.SELECTED),
                            ChatColor.GRAY + "" + number
                    ),
                    new GuiStateElement.State(
                            change -> {
                                builder.removeNumber(number);
                                gui.draw(player);
                                GuiElement guiElement =  gui.getElement('\0');
                                if (guiElement != null) {
                                    if (isBanker) {
                                        BetNumbersBuilder.BankerBuilder bankerBuilder = (BetNumbersBuilder.BankerBuilder) builder;
                                        if (bankerBuilder.inSelectionPhase()) {
                                            if (!bankerBuilder.completed()) {
                                                gui.removeElement('\0');
                                                gui.addElement(new StaticGuiElement('\0', XMaterial.GRAY_DYE.parseItem(), LotteryUtils.formatPlaceholders(player, instance.guiNewBetNotYetFinish, instance, game)));
                                                return;
                                            }
                                        } else {
                                            if (bankerBuilder.getBankers().isEmpty()) {
                                                gui.removeElement('\0');
                                                gui.addElement(new StaticGuiElement('\0', XMaterial.GRAY_DYE.parseItem(), LotteryUtils.formatPlaceholders(player, instance.guiNewBetNotYetFinish, instance, game)));
                                                return;
                                            }
                                        }
                                    } else if (!builder.completed()) {
                                        gui.removeElement('\0');
                                        gui.addElement(new StaticGuiElement('\0', XMaterial.GRAY_DYE.parseItem(), LotteryUtils.formatPlaceholders(player, instance.guiNewBetNotYetFinish, instance, game)));
                                        return;
                                    }
                                    long price = LotteryUtils.calculatePrice(builder, instance);
                                    ((StaticGuiElement) gui.getElement('\0')).setText(LotteryUtils.formatPlaceholders(player, Arrays.stream(isBanker ? instance.guiNewBetFinishBankers : (builder.getType().isMultipleCombination() ? instance.guiNewBetFinishComplex : instance.guiNewBetFinishSimple))
                                            .map(each -> each
                                                    .replace("{BetUnits}", StringUtils.formatComma(price / instance.pricePerBet))
                                                    .replace("{Price}", StringUtils.formatComma(price))
                                                    .replace("{PricePartial}", StringUtils.formatComma(price / BetUnitType.PARTIAL.getDivisor()))).toArray(String[]::new), instance, game));
                                }
                            },
                            "false",
                            getNumberItem(number, NumberSelectedState.NOT_SELECTED),
                            getNumberColor(number) + "" + number
                    )
            );
            element.setState("false");
            element.setSilent(true);
            gui.addElement(element);
        }
        if (builder.getType().isMultipleCombination()) {
            char allChar = c;
            gui.addElement(new DynamicGuiElement(allChar, viewer -> {
                if (builder.canAdd()) {
                    if (!isBanker || ((BetNumbersBuilder.BankerBuilder) builder).inSelectionPhase()) {
                        return new StaticGuiElement(allChar, getNumberItem(0, NumberSelectedState.NOT_SELECTED), click -> {
                            for (int number = 1; number <= num; number++) {
                                if (!builder.contains(number)) {
                                    builder.addNumber(number);
                                    GuiStateElement element = (GuiStateElement) gui.getElement((char) ('a' + number - 1));
                                    element.nextState();
                                    handleClick.accept(number, (Player) click.getWhoClicked());
                                }
                            }
                            gui.draw(click.getWhoClicked());
                            return true;
                        }, ChatColor.GOLD + instance.guiNewBetSelectAll);
                    } else {
                        return new StaticGuiElement(allChar, new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString());
                    }
                } else {
                    return new StaticGuiElement(allChar, getNumberItem(0, NumberSelectedState.SELECTED), ChatColor.DARK_GRAY + instance.guiNewBetSelectAll);
                }
            }));
        }
        gui.addElement(new StaticGuiElement('\0', XMaterial.GRAY_DYE.parseItem(), LotteryUtils.formatPlaceholders(player, instance.guiNewBetNotYetFinish, instance, game)));
        gui.addElement(new DynamicGuiElement('\1', viewer -> {
            if (builder.canAdd()) {
                return new StaticGuiElement('\1', XMaterial.REDSTONE.parseItem(), click -> {
                    int number = builder.addRandomNumber().getFirstInt();
                    GuiStateElement element = (GuiStateElement) gui.getElement((char) ('a' + number - 1));
                    element.nextState();
                    handleClick.accept(number, (Player) click.getWhoClicked());
                    gui.draw(click.getWhoClicked());
                    return true;
                }, LotteryUtils.formatPlaceholders(player, instance.guiNewBetAddRandom, instance, game));
            } else {
                return new StaticGuiElement('\1', new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString());
            }
        }));

        return gui;
    }

    public InventoryGui getNumberConfirm(Player player, PlayableLotterySixGame game, Collection<BetNumbers> betNumbers, BetNumbersType type) {
        if (betNumbers.isEmpty()) {
            throw new IllegalArgumentException("betNumbers cannot be empty");
        }
        switch (type) {
            case SINGLE:
                return getSingleNumberConfirm(player, game, betNumbers.iterator().next());
            case MULTIPLE:
            case BANKER:
                return getComplexNumberConfirm(player, game, betNumbers.iterator().next());
            case RANDOM:
                return getSingleBulkNumberConfirm(player, game, betNumbers);
            case MULTIPLE_RANDOM:
            case BANKER_RANDOM:
                return getComplexBulkNumberConfirm(player, game, betNumbers);
            default:
                throw new IllegalArgumentException("Unknown BetNumbersType \"" + type + "\"");
        }
    }

    public InventoryGui getSingleNumberConfirm(Player player, PlayableLotterySixGame game, BetNumbers betNumbers) {
        String[] guiSetup = {
                "         ",
                " abcdefg ",
                " zzklmzz ",
                " zhzzziz ",
                "         "
        };
        InventoryGui gui = new InventoryGui(plugin, LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetTitle, instance, game), guiSetup);
        gui.setFiller(XMaterial.LIME_STAINED_GLASS_PANE.parseItem());
        gui.addElement(new StaticGuiElement('z', new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));

        char c = 'a';
        for (int i : betNumbers.getNumbers()) {
            gui.addElement(new StaticGuiElement(c++, getNumberItem(i, NumberSelectedState.NOT_SELECTED), getNumberColor(i) + "" + i));
        }

        AtomicInteger multipleDraw = new AtomicInteger(1);
        gui.addElement(new StaticGuiElement('k', XMaterial.ARROW.parseItem(), click -> {
            int decrement = click.getType().isRightClick() ? 5 : 1;
            multipleDraw.updateAndGet(i -> Math.max(1, i - decrement));
            gui.draw(player);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetDecrementButton, instance)));
        gui.addElement(new StaticGuiElement('m', XMaterial.ARROW.parseItem(), click -> {
            int increment = click.getType().isRightClick() ? 5 : 1;
            multipleDraw.updateAndGet(i -> Math.min(20, i + increment));
            gui.draw(player);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetIncrementButton, instance)));

        gui.addElement(new DynamicGuiElement('l', () -> new StaticGuiElement('l', getNumberItem(multipleDraw.get(), NumberSelectedState.NOT_SELECTED), Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetMultipleDrawValue, instance)).map(each -> each.replace("{Count}", multipleDrawStr(multipleDraw.get()))).toArray(String[]::new))));

        LongSupplier priceProvider = () -> LotteryUtils.calculatePrice(betNumbers, instance) * multipleDraw.get();
        gui.addElement(new DynamicGuiElement('g', () -> new StaticGuiElement('g', XMaterial.DIAMOND.parseItem(), Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetLotteryInfo, instance, game))
                .map(each -> each.replace("{Price}", StringUtils.formatComma(priceProvider.getAsLong()))).toArray(String[]::new))));

        gui.addElement(new DynamicGuiElement('h', () -> {
            long price = priceProvider.getAsLong();
            return new StaticGuiElement('h', XMaterial.GOLD_INGOT.parseItem(), click -> {
                checkFixedTransactionDeposit(player, price, () -> {
                    if (game != null && game.isValid()) {
                        if (instance.backendBungeecordMode) {
                            LotterySixPlugin.getPluginMessageHandler().requestAddBet(player.getName(), player.getUniqueId(), instance.pricePerBet, BetUnitType.FULL, betNumbers, multipleDraw.get());
                        } else {
                            AddBetResult result = game.addBet(player.getName(), player.getUniqueId(), instance.pricePerBet, BetUnitType.FULL, betNumbers, multipleDraw.get());
                            switch (result) {
                                case SUCCESS: {
                                    player.sendMessage(instance.messageBetPlaced.replace("{Price}", StringUtils.formatComma(price)));
                                    break;
                                }
                                case GAME_LOCKED: {
                                    player.sendMessage(instance.messageGameLocked.replace("{Price}", StringUtils.formatComma(price)));
                                    break;
                                }
                                case NOT_ENOUGH_MONEY: {
                                    player.sendMessage(instance.messageNotEnoughMoney.replace("{Price}", StringUtils.formatComma(price)));
                                    break;
                                }
                                case LIMIT_SELF: {
                                    player.sendMessage(instance.messageBetLimitReachedSelf.replace("{Price}", StringUtils.formatComma(price)));
                                    break;
                                }
                                case LIMIT_PERMISSION: {
                                    player.sendMessage(instance.messageBetLimitReachedPermission.replace("{Price}", StringUtils.formatComma(price)));
                                    break;
                                }
                                case LIMIT_CHANCE_PER_SELECTION: {
                                    player.sendMessage(instance.messageBetLimitMaximumChancePerSelection.replace("{Price}", StringUtils.formatComma(price)));
                                    break;
                                }
                                case ACCOUNT_SUSPENDED: {
                                    long time = instance.getLotteryPlayerManager().getLotteryPlayer(player.getUniqueId()).getPreference(PlayerPreferenceKey.SUSPEND_ACCOUNT_UNTIL, long.class);
                                    player.sendMessage(instance.messageBettingAccountSuspended.replace("{Date}", instance.dateFormat.format(new Date(time))).replace("{Price}", StringUtils.formatComma(price)));
                                    break;
                                }
                            }
                            if (result.isSuccess()) {
                                Scheduler.runTaskLater(plugin, () -> checkReopen(click.getWhoClicked()), 5, player);
                            }
                        }
                        Scheduler.runTaskLater(plugin, () -> close(click.getWhoClicked(), click.getGui(), false), 1, player);
                    }
                });
                return true;
            }, Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetUnitInvestmentConfirm, instance, game))
                    .map(each -> each.replace("{Price}", StringUtils.formatComma(price))).toArray(String[]::new));
        }));
        gui.addElement(new StaticGuiElement('i', XMaterial.BARRIER.parseItem(), click -> {
            Scheduler.runTaskLater(plugin, () -> close(click.getWhoClicked(), click.getGui(), false), 1, player);
            Scheduler.runTaskLater(plugin, () -> checkReopen(click.getWhoClicked()), 5, player);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetCancel, instance, game)));

        return gui;
    }

    public InventoryGui getComplexNumberConfirm(Player player, PlayableLotterySixGame game, BetNumbers betNumbers) {
        String[] guiSetup = {
                "         ",
                " zzazgzz ",
                " zzklmzz ",
                " zhzizjz ",
                "         "
        };
        InventoryGui gui = new InventoryGui(plugin, LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetTitle, instance, game), guiSetup);
        gui.setFiller(XMaterial.LIME_STAINED_GLASS_PANE.parseItem());
        gui.addElement(new StaticGuiElement('z', new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));

        AtomicInteger multipleDraw = new AtomicInteger(1);
        gui.addElement(new StaticGuiElement('k', XMaterial.ARROW.parseItem(), click -> {
            int decrement = click.getType().isRightClick() ? 5 : 1;
            multipleDraw.updateAndGet(i -> Math.max(1, i - decrement));
            gui.draw(player);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetDecrementButton, instance)));
        gui.addElement(new StaticGuiElement('m', XMaterial.ARROW.parseItem(), click -> {
            int increment = click.getType().isRightClick() ? 5 : 1;
            multipleDraw.updateAndGet(i -> Math.min(20, i + increment));
            gui.draw(player);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetIncrementButton, instance)));

        gui.addElement(new DynamicGuiElement('l', () -> new StaticGuiElement('l', getNumberItem(multipleDraw.get(), NumberSelectedState.NOT_SELECTED), Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetMultipleDrawValue, instance)).map(each -> each.replace("{Count}", multipleDrawStr(multipleDraw.get()))).toArray(String[]::new))));

        LongSupplier priceProvider = () -> LotteryUtils.calculatePrice(betNumbers, instance) * multipleDraw.get();

        gui.addElement(new DynamicGuiElement('a', () -> {
            long price = priceProvider.getAsLong();
            long partial = price / BetUnitType.PARTIAL.getDivisor();
            return new StaticGuiElement('a', XMaterial.PAPER.parseItem(), StringUtils.wrapAtSpace(betNumbers.toFormattedString(), 6)
                    .replace("{Price}", StringUtils.formatComma(price)).replace("{PricePartial}", StringUtils.formatComma(partial)));
        }));
        gui.addElement(new DynamicGuiElement('g', () -> {
            long price = priceProvider.getAsLong();
            long partial = price / BetUnitType.PARTIAL.getDivisor();
            return new StaticGuiElement('g', XMaterial.DIAMOND.parseItem(), Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetLotteryInfo, instance, game))
                    .map(each -> each.replace("{Price}", StringUtils.formatComma(price)).replace("{PricePartial}", StringUtils.formatComma(partial))).toArray(String[]::new));
        }));
        gui.addElement(new DynamicGuiElement('h', () -> {
            long price = priceProvider.getAsLong();
            long partial = price / BetUnitType.PARTIAL.getDivisor();
            return new StaticGuiElement('h', XMaterial.GOLD_NUGGET.parseItem(), click -> {
                checkFixedTransactionDeposit(player, partial, () -> {
                    if (game != null && game.isValid()) {
                        if (instance.backendBungeecordMode) {
                            LotterySixPlugin.getPluginMessageHandler().requestAddBet(player.getName(), player.getUniqueId(), partial / multipleDraw.get(), BetUnitType.PARTIAL, betNumbers, multipleDraw.get());
                        } else {
                            AddBetResult result = game.addBet(player.getName(), player.getUniqueId(), partial / multipleDraw.get(), BetUnitType.PARTIAL, betNumbers, multipleDraw.get());
                            switch (result) {
                                case SUCCESS: {
                                    player.sendMessage(instance.messageBetPlaced.replace("{Price}", StringUtils.formatComma(partial)));
                                    break;
                                }
                                case GAME_LOCKED: {
                                    player.sendMessage(instance.messageGameLocked.replace("{Price}", StringUtils.formatComma(partial)));
                                    break;
                                }
                                case NOT_ENOUGH_MONEY: {
                                    player.sendMessage(instance.messageNotEnoughMoney.replace("{Price}", StringUtils.formatComma(partial)));
                                    break;
                                }
                                case LIMIT_SELF: {
                                    player.sendMessage(instance.messageBetLimitReachedSelf.replace("{Price}", StringUtils.formatComma(partial)));
                                    break;
                                }
                                case LIMIT_PERMISSION: {
                                    player.sendMessage(instance.messageBetLimitReachedPermission.replace("{Price}", StringUtils.formatComma(partial)));
                                    break;
                                }
                                case LIMIT_CHANCE_PER_SELECTION: {
                                    player.sendMessage(instance.messageBetLimitMaximumChancePerSelection.replace("{Price}", StringUtils.formatComma(partial)));
                                    break;
                                }
                                case ACCOUNT_SUSPENDED: {
                                    long time = instance.getLotteryPlayerManager().getLotteryPlayer(player.getUniqueId()).getPreference(PlayerPreferenceKey.SUSPEND_ACCOUNT_UNTIL, long.class);
                                    player.sendMessage(instance.messageBettingAccountSuspended.replace("{Date}", instance.dateFormat.format(new Date(time))).replace("{Price}", StringUtils.formatComma(partial)));
                                    break;
                                }
                            }
                            if (result.isSuccess()) {
                                Scheduler.runTaskLater(plugin, () -> checkReopen(click.getWhoClicked()), 5, player);
                            }
                        }
                        Scheduler.runTaskLater(plugin, () -> close(click.getWhoClicked(), click.getGui(), false), 1, player);
                    }
                });
                return true;
            }, Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetPartialInvestmentConfirm, instance, game))
                    .map(each -> each.replace("{Price}", StringUtils.formatComma(price)).replace("{PricePartial}", StringUtils.formatComma(partial))).toArray(String[]::new));
        }));
        gui.addElement(new DynamicGuiElement('i', () -> {
            long price = priceProvider.getAsLong();
            long partial = price / BetUnitType.PARTIAL.getDivisor();
            return new StaticGuiElement('i', XMaterial.GOLD_INGOT.parseItem(), click -> {
                checkFixedTransactionDeposit(player, price, () -> {
                    if (game != null && game.isValid()) {
                        if (instance.backendBungeecordMode) {
                            LotterySixPlugin.getPluginMessageHandler().requestAddBet(player.getName(), player.getUniqueId(), price / multipleDraw.get(), BetUnitType.FULL, betNumbers, multipleDraw.get());
                        } else {
                            AddBetResult result = game.addBet(player.getName(), player.getUniqueId(), price / multipleDraw.get(), BetUnitType.FULL, betNumbers, multipleDraw.get());
                            switch (result) {
                                case SUCCESS: {
                                    player.sendMessage(instance.messageBetPlaced.replace("{Price}", StringUtils.formatComma(price)));
                                    break;
                                }
                                case GAME_LOCKED: {
                                    player.sendMessage(instance.messageGameLocked.replace("{Price}", StringUtils.formatComma(price)));
                                    break;
                                }
                                case NOT_ENOUGH_MONEY: {
                                    player.sendMessage(instance.messageNotEnoughMoney.replace("{Price}", StringUtils.formatComma(price)));
                                    break;
                                }
                                case LIMIT_SELF: {
                                    player.sendMessage(instance.messageBetLimitReachedSelf.replace("{Price}", StringUtils.formatComma(price)));
                                    break;
                                }
                                case LIMIT_PERMISSION: {
                                    player.sendMessage(instance.messageBetLimitReachedPermission.replace("{Price}", StringUtils.formatComma(price)));
                                    break;
                                }
                                case LIMIT_CHANCE_PER_SELECTION: {
                                    player.sendMessage(instance.messageBetLimitMaximumChancePerSelection.replace("{Price}", StringUtils.formatComma(price)));
                                    break;
                                }
                                case ACCOUNT_SUSPENDED: {
                                    long time = instance.getLotteryPlayerManager().getLotteryPlayer(player.getUniqueId()).getPreference(PlayerPreferenceKey.SUSPEND_ACCOUNT_UNTIL, long.class);
                                    player.sendMessage(instance.messageBettingAccountSuspended.replace("{Date}", instance.dateFormat.format(new Date(time))).replace("{Price}", StringUtils.formatComma(price)));
                                    break;
                                }
                            }
                            if (result.isSuccess()) {
                                Scheduler.runTaskLater(plugin, () -> checkReopen(click.getWhoClicked()), 5, player);
                            }
                        }
                        Scheduler.runTaskLater(plugin, () -> close(click.getWhoClicked(), click.getGui(), false), 1, player);
                    }
                });
                return true;
            }, Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetUnitInvestmentConfirm, instance, game))
                    .map(each -> each.replace("{Price}", StringUtils.formatComma(price)).replace("{PricePartial}", StringUtils.formatComma(partial))).toArray(String[]::new));
        }));
        gui.addElement(new StaticGuiElement('j', XMaterial.BARRIER.parseItem(), click -> {
            Scheduler.runTaskLater(plugin, () -> close(click.getWhoClicked(), click.getGui(), false), 1, player);
            Scheduler.runTaskLater(plugin, () -> checkReopen(click.getWhoClicked()), 5, player);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetCancel, instance, game)));

        return gui;
    }

    public InventoryGui getSingleBulkNumberConfirm(Player player, PlayableLotterySixGame game, Collection<BetNumbers> betNumbers) {
        String[] guiSetup = {
                "         ",
                " zzazgzz ",
                " zzklmzz ",
                " zhzzziz ",
                "         "
        };
        int entriesTotal = betNumbers.stream().mapToInt(each -> each.getSetsSize()).sum();
        InventoryGui gui = new InventoryGui(plugin, LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetTitle, instance, game), guiSetup);
        gui.setFiller(XMaterial.LIME_STAINED_GLASS_PANE.parseItem());
        gui.addElement(new StaticGuiElement('z', new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));

        AtomicInteger multipleDraw = new AtomicInteger(1);
        gui.addElement(new StaticGuiElement('k', XMaterial.ARROW.parseItem(), click -> {
            int decrement = click.getType().isRightClick() ? 5 : 1;
            multipleDraw.updateAndGet(i -> Math.max(1, i - decrement));
            gui.draw(player);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetDecrementButton, instance)));
        gui.addElement(new StaticGuiElement('m', XMaterial.ARROW.parseItem(), click -> {
            int increment = click.getType().isRightClick() ? 5 : 1;
            multipleDraw.updateAndGet(i -> Math.min(20, i + increment));
            gui.draw(player);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetIncrementButton, instance)));

        gui.addElement(new DynamicGuiElement('l', () -> new StaticGuiElement('l', getNumberItem(multipleDraw.get(), NumberSelectedState.NOT_SELECTED), Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetMultipleDrawValue, instance)).map(each -> each.replace("{Count}", multipleDrawStr(multipleDraw.get()))).toArray(String[]::new))));

        LongSupplier priceProvider = () -> betNumbers.stream().mapToLong(each -> LotteryUtils.calculatePrice(each, instance)).sum() * multipleDraw.get();

        gui.addElement(new StaticGuiElement('a', setItemSize(XMaterial.PAPER.parseItem(), betNumbers.size()), Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetBulkRandom, instance, game))
                .map(each -> each.replace("{EntriesTotal}", entriesTotal + "")).toArray(String[]::new)));
        gui.addElement(new DynamicGuiElement('g', () -> new StaticGuiElement('g', XMaterial.DIAMOND.parseItem(), Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetLotteryInfo, instance, game))
                .map(each -> each.replace("{Price}", StringUtils.formatComma(priceProvider.getAsLong()))).toArray(String[]::new))));
        gui.addElement(new DynamicGuiElement('h', () -> {
            long price = priceProvider.getAsLong();
            return new StaticGuiElement('h', XMaterial.GOLD_INGOT.parseItem(), click -> {
                checkFixedTransactionDeposit(player, price, () -> {
                    if (game != null && game.isValid()) {
                        if (instance.backendBungeecordMode) {
                            LotterySixPlugin.getPluginMessageHandler().requestAddBet(player.getName(), player.getUniqueId(), price / multipleDraw.get() / betNumbers.size(), BetUnitType.FULL, betNumbers, multipleDraw.get());
                        } else {
                            AddBetResult result = game.addBet(player.getName(), player.getUniqueId(), price / multipleDraw.get() / betNumbers.size(), BetUnitType.FULL, betNumbers, multipleDraw.get());
                            switch (result) {
                                case SUCCESS: {
                                    player.sendMessage(instance.messageBetPlaced.replace("{Price}", StringUtils.formatComma(price)));
                                    break;
                                }
                                case GAME_LOCKED: {
                                    player.sendMessage(instance.messageGameLocked.replace("{Price}", StringUtils.formatComma(price)));
                                    break;
                                }
                                case NOT_ENOUGH_MONEY: {
                                    player.sendMessage(instance.messageNotEnoughMoney.replace("{Price}", StringUtils.formatComma(price)));
                                    break;
                                }
                                case LIMIT_SELF: {
                                    player.sendMessage(instance.messageBetLimitReachedSelf.replace("{Price}", StringUtils.formatComma(price)));
                                    break;
                                }
                                case LIMIT_PERMISSION: {
                                    player.sendMessage(instance.messageBetLimitReachedPermission.replace("{Price}", StringUtils.formatComma(price)));
                                    break;
                                }
                                case LIMIT_CHANCE_PER_SELECTION: {
                                    player.sendMessage(instance.messageBetLimitMaximumChancePerSelection.replace("{Price}", StringUtils.formatComma(price)));
                                    break;
                                }
                                case ACCOUNT_SUSPENDED: {
                                    long time = instance.getLotteryPlayerManager().getLotteryPlayer(player.getUniqueId()).getPreference(PlayerPreferenceKey.SUSPEND_ACCOUNT_UNTIL, long.class);
                                    player.sendMessage(instance.messageBettingAccountSuspended.replace("{Date}", instance.dateFormat.format(new Date(time))).replace("{Price}", StringUtils.formatComma(price)));
                                    break;
                                }
                            }
                            if (result.isSuccess()) {
                                Scheduler.runTaskLater(plugin, () -> checkReopen(click.getWhoClicked()), 5, player);
                            }
                        }
                        Scheduler.runTaskLater(plugin, () -> close(click.getWhoClicked(), click.getGui(), false), 1, player);
                    }
                });
                return true;
            }, Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetUnitInvestmentConfirm, instance, game))
                    .map(each -> each.replace("{Price}", StringUtils.formatComma(price))).toArray(String[]::new));
        }));
        gui.addElement(new StaticGuiElement('i', XMaterial.BARRIER.parseItem(), click -> {
            Scheduler.runTaskLater(plugin, () -> close(click.getWhoClicked(), click.getGui(), false), 1, player);
            Scheduler.runTaskLater(plugin, () -> checkReopen(click.getWhoClicked()), 5, player);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetCancel, instance, game)));

        return gui;
    }

    public InventoryGui getComplexBulkNumberConfirm(Player player, PlayableLotterySixGame game, Collection<BetNumbers> betNumbers) {
        String[] guiSetup = {
                "         ",
                " zzazgzz ",
                " zzklmzz ",
                " zhzizjz ",
                "         "
        };
        int entriesTotal = betNumbers.stream().mapToInt(each -> each.getSetsSize()).sum();
        InventoryGui gui = new InventoryGui(plugin, LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetTitle, instance, game), guiSetup);
        gui.setFiller(XMaterial.LIME_STAINED_GLASS_PANE.parseItem());
        gui.addElement(new StaticGuiElement('z', new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));

        AtomicInteger multipleDraw = new AtomicInteger(1);
        gui.addElement(new StaticGuiElement('k', XMaterial.ARROW.parseItem(), click -> {
            int decrement = click.getType().isRightClick() ? 5 : 1;
            multipleDraw.updateAndGet(i -> Math.max(1, i - decrement));
            gui.draw(player);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetDecrementButton, instance)));
        gui.addElement(new StaticGuiElement('m', XMaterial.ARROW.parseItem(), click -> {
            int increment = click.getType().isRightClick() ? 5 : 1;
            multipleDraw.updateAndGet(i -> Math.min(20, i + increment));
            gui.draw(player);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetIncrementButton, instance)));

        gui.addElement(new DynamicGuiElement('l', () -> new StaticGuiElement('l', getNumberItem(multipleDraw.get(), NumberSelectedState.NOT_SELECTED), Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetMultipleDrawValue, instance)).map(each -> each.replace("{Count}", multipleDrawStr(multipleDraw.get()))).toArray(String[]::new))));

        LongSupplier priceProvider = () -> betNumbers.stream().mapToLong(each -> LotteryUtils.calculatePrice(each, instance)).sum() * multipleDraw.get();

        gui.addElement(new StaticGuiElement('a', setItemSize(XMaterial.PAPER.parseItem(), betNumbers.size()), Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetBulkRandom, instance, game))
                .map(each -> each.replace("{EntriesTotal}", entriesTotal + "")).toArray(String[]::new)));
        gui.addElement(new DynamicGuiElement('g', () -> {
            long price = priceProvider.getAsLong();
            long partial = price / BetUnitType.PARTIAL.getDivisor();
            return new StaticGuiElement('g', XMaterial.DIAMOND.parseItem(), Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetLotteryInfo, instance, game))
                    .map(each -> each.replace("{Price}", StringUtils.formatComma(price)).replace("{PricePartial}", StringUtils.formatComma(partial))).toArray(String[]::new));
        }));
        gui.addElement(new DynamicGuiElement('h', () -> {
            long price = priceProvider.getAsLong();
            long partial = price / BetUnitType.PARTIAL.getDivisor();
            return new StaticGuiElement('h', XMaterial.GOLD_NUGGET.parseItem(), click -> {
                checkFixedTransactionDeposit(player, partial, () -> {
                    if (game != null && game.isValid()) {
                        if (instance.backendBungeecordMode) {
                            LotterySixPlugin.getPluginMessageHandler().requestAddBet(player.getName(), player.getUniqueId(), partial / multipleDraw.get() / betNumbers.size(), BetUnitType.PARTIAL, betNumbers, multipleDraw.get());
                        } else {
                            AddBetResult result = game.addBet(player.getName(), player.getUniqueId(), partial / multipleDraw.get() / betNumbers.size(), BetUnitType.PARTIAL, betNumbers, multipleDraw.get());
                            switch (result) {
                                case SUCCESS: {
                                    player.sendMessage(instance.messageBetPlaced.replace("{Price}", StringUtils.formatComma(partial)));
                                    break;
                                }
                                case GAME_LOCKED: {
                                    player.sendMessage(instance.messageGameLocked.replace("{Price}", StringUtils.formatComma(partial)));
                                    break;
                                }
                                case NOT_ENOUGH_MONEY: {
                                    player.sendMessage(instance.messageNotEnoughMoney.replace("{Price}", StringUtils.formatComma(partial)));
                                    break;
                                }
                                case LIMIT_SELF: {
                                    player.sendMessage(instance.messageBetLimitReachedSelf.replace("{Price}", StringUtils.formatComma(partial)));
                                    break;
                                }
                                case LIMIT_PERMISSION: {
                                    player.sendMessage(instance.messageBetLimitReachedPermission.replace("{Price}", StringUtils.formatComma(partial)));
                                    break;
                                }
                                case LIMIT_CHANCE_PER_SELECTION: {
                                    player.sendMessage(instance.messageBetLimitMaximumChancePerSelection.replace("{Price}", StringUtils.formatComma(partial)));
                                    break;
                                }
                                case ACCOUNT_SUSPENDED: {
                                    long time = instance.getLotteryPlayerManager().getLotteryPlayer(player.getUniqueId()).getPreference(PlayerPreferenceKey.SUSPEND_ACCOUNT_UNTIL, long.class);
                                    player.sendMessage(instance.messageBettingAccountSuspended.replace("{Date}", instance.dateFormat.format(new Date(time))).replace("{Price}", StringUtils.formatComma(partial)));
                                    break;
                                }
                            }
                            if (result.isSuccess()) {
                                Scheduler.runTaskLater(plugin, () -> checkReopen(click.getWhoClicked()), 5, player);
                            }
                        }
                        Scheduler.runTaskLater(plugin, () -> close(click.getWhoClicked(), click.getGui(), false), 1, player);
                    }
                });
                return true;
            }, Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetPartialInvestmentConfirm, instance, game))
                    .map(each -> each.replace("{Price}", StringUtils.formatComma(price)).replace("{PricePartial}", StringUtils.formatComma(partial))).toArray(String[]::new));
        }));
        gui.addElement(new DynamicGuiElement('i', () -> {
            long price = priceProvider.getAsLong();
            long partial = price / BetUnitType.PARTIAL.getDivisor();
            return new StaticGuiElement('i', XMaterial.GOLD_INGOT.parseItem(), click -> {
                checkFixedTransactionDeposit(player, price, () -> {
                    if (game != null && game.isValid()) {
                        if (instance.backendBungeecordMode) {
                            LotterySixPlugin.getPluginMessageHandler().requestAddBet(player.getName(), player.getUniqueId(), price / multipleDraw.get() / betNumbers.size(), BetUnitType.FULL, betNumbers, multipleDraw.get());
                        } else {
                            AddBetResult result = game.addBet(player.getName(), player.getUniqueId(), price / multipleDraw.get() / betNumbers.size(), BetUnitType.FULL, betNumbers, multipleDraw.get());
                            switch (result) {
                                case SUCCESS: {
                                    player.sendMessage(instance.messageBetPlaced.replace("{Price}", StringUtils.formatComma(price)));
                                    break;
                                }
                                case GAME_LOCKED: {
                                    player.sendMessage(instance.messageGameLocked.replace("{Price}", StringUtils.formatComma(price)));
                                    break;
                                }
                                case NOT_ENOUGH_MONEY: {
                                    player.sendMessage(instance.messageNotEnoughMoney.replace("{Price}", StringUtils.formatComma(price)));
                                    break;
                                }
                                case LIMIT_SELF: {
                                    player.sendMessage(instance.messageBetLimitReachedSelf.replace("{Price}", StringUtils.formatComma(price)));
                                    break;
                                }
                                case LIMIT_PERMISSION: {
                                    player.sendMessage(instance.messageBetLimitReachedPermission.replace("{Price}", StringUtils.formatComma(price)));
                                    break;
                                }
                                case LIMIT_CHANCE_PER_SELECTION: {
                                    player.sendMessage(instance.messageBetLimitMaximumChancePerSelection.replace("{Price}", StringUtils.formatComma(price)));
                                    break;
                                }
                                case ACCOUNT_SUSPENDED: {
                                    long time = instance.getLotteryPlayerManager().getLotteryPlayer(player.getUniqueId()).getPreference(PlayerPreferenceKey.SUSPEND_ACCOUNT_UNTIL, long.class);
                                    player.sendMessage(instance.messageBettingAccountSuspended.replace("{Date}", instance.dateFormat.format(new Date(time))).replace("{Price}", StringUtils.formatComma(price)));
                                    break;
                                }
                            }
                            if (result.isSuccess()) {
                                Scheduler.runTaskLater(plugin, () -> checkReopen(click.getWhoClicked()), 5, player);
                            }
                        }
                        Scheduler.runTaskLater(plugin, () -> close(click.getWhoClicked(), click.getGui(), false), 1, player);
                    }
                });
                return true;
            }, Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetUnitInvestmentConfirm, instance, game))
                    .map(each -> each.replace("{Price}", StringUtils.formatComma(price)).replace("{PricePartial}", StringUtils.formatComma(partial))).toArray(String[]::new));
        }));
        gui.addElement(new StaticGuiElement('j', XMaterial.BARRIER.parseItem(), click -> {
            Scheduler.runTaskLater(plugin, () -> close(click.getWhoClicked(), click.getGui(), false), 1, player);
            Scheduler.runTaskLater(plugin, () -> checkReopen(click.getWhoClicked()), 5, player);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetCancel, instance, game)));

        return gui;
    }

    public InventoryGui getPastResults(Player player, CompletedLotterySixGame game) {
        InventoryGui gui;
        if (game == null) {
            String[] guiSetup = {
                    "         ",
                    " zzzzzzz ",
                    " zzzazzz ",
                    " zzzzzzz ",
                    "         "
            };
            gui = new InventoryGui(plugin, LotteryUtils.formatPlaceholders(player, instance.guiLastResultsTitle, instance), guiSetup);
            gui.addElement(new StaticGuiElement('a', XMaterial.BARRIER.parseItem(), ChatColor.RED + "No games have been played yet."));
        } else {
            String[] guiSetup = {
                    "         ",
                    " abcdefg ",
                    " zzzzzzz ",
                    " zhzizjz ",
                    "         "
            };
            gui = new InventoryGui(plugin, LotteryUtils.formatPlaceholders(player, instance.guiLastResultsTitle, instance, game), guiSetup);
            char c = 'a';
            for (int i : game.getDrawResult().getNumbersOrdered()) {
                gui.addElement(new StaticGuiElement(c++, getNumberItem(i, NumberSelectedState.NOT_SELECTED), getNumberColor(i) + "" + i));
            }
            int specialNumber = game.getDrawResult().getSpecialNumber();
            gui.addElement(new StaticGuiElement(c, getNumberItem(specialNumber, true, NumberSelectedState.NOT_SELECTED), getNumberColor(specialNumber) + "" + specialNumber));
            gui.addElement(new StaticGuiElement('h', XMaterial.CLOCK.parseItem(), LotteryUtils.formatPlaceholders(player, instance.guiLastResultsLotteryInfo, instance, game)));

            gui.addElement(new StaticGuiElement('i', game.hasPlayerWinnings(player.getUniqueId()) ? setEnchanted(XMaterial.GOLD_INGOT.parseItem()) : XMaterial.GOLD_INGOT.parseItem(), click -> {
                Scheduler.runTaskLater(plugin, () -> {
                    close(click.getWhoClicked(), click.getGui(), false);

                    Scheduler.runTaskLaterAsynchronously(plugin, () -> {
                        String winningNumberStr = game.getDrawResult().toFormattedString();

                        List<BaseComponent> pages = new ArrayList<>();
                        List<PlayerWinnings> winningsList = game.getSortedPlayerWinnings(player.getUniqueId());
                        Set<UUID> displayedBets = new HashSet<>();
                        for (PlayerWinnings winnings : winningsList) {
                            if (pages.size() > 100) {
                                break;
                            }
                            UUID betId = winnings.getWinningBetId();
                            if (!displayedBets.contains(betId)) {
                                displayedBets.add(betId);
                                if (winnings.isBulk(game)) {
                                    List<PlayerWinnings> winningsForBet = game.getPlayerWinningsByBet(betId).values().stream().flatMap(each -> each.stream()).collect(Collectors.toList());
                                    TextComponent text = new TextComponent(winningNumberStr + "\n\n");
                                    PlayerBets bets = winnings.getWinningBet(game);
                                    BetNumbers betNumbers = bets.getChosenNumbers();
                                    String[] numberStrings = betNumbers.toFormattedString().replace("/ ", "/\n\0").split("\0");
                                    int i = 0;
                                    for (String numbers : numberStrings) {
                                        TextComponent numbersText = new TextComponent(numbers);
                                        int finalI = i;
                                        Optional<PlayerWinnings> optWinnings = winningsForBet.stream().filter(each -> each.getWinningCombination().getNumbers().equals(betNumbers.getSet(finalI))).findFirst();
                                        if (optWinnings.isPresent()) {
                                            PlayerWinnings localWinnings = optWinnings.get();
                                            numbersText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[]{
                                                    new TextComponent(instance.winningsDescription
                                                            .replace("{Tier}", instance.tierNames.get(localWinnings.getTier()))
                                                            .replace("{Winnings}", StringUtils.formatComma(localWinnings.getWinnings()))
                                                            .replace("{UnitPrice}", StringUtils.formatComma(game.getPricePerBet(localWinnings.getWinningBet(game).getType()))))
                                            }));
                                        } else {
                                            numbersText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[]{
                                                    new TextComponent(instance.winningsDescription
                                                            .replace("{Tier}", LotteryUtils.formatPlaceholders(player, instance.guiLastResultsNoWinnings, instance, game))
                                                            .replace("{Winnings}", StringUtils.formatComma(0))
                                                            .replace("{UnitPrice}", StringUtils.formatComma(game.getPricePerBet(bets.getType()))))
                                            }));
                                        }
                                        text.addExtra(numbersText);
                                        i++;
                                    }
                                    text.addExtra("\n");
                                    text.addExtra(instance.bulkWinningsDescription
                                            .replace("{HighestTier}", instance.tierNames.get(winnings.getTier()))
                                            .replace("{Winnings}", StringUtils.formatComma(winningsForBet.stream().mapToLong(each -> each.getWinnings()).sum()))
                                            .replace("{UnitPrice}", StringUtils.formatComma(game.getPricePerBet(winnings.getWinningBet(game).getType()))));
                                    pages.add(text);
                                } else if (winnings.isCombination(game)) {
                                    Map<PrizeTier, List<PlayerWinnings>> winningsForBet = game.getPlayerWinningsByBet(betId);
                                    TextComponent text = new TextComponent(winningNumberStr + "\n\n");
                                    TextComponent numbersText = new TextComponent(winnings.getWinningBet(game).getChosenNumbers().toFormattedString().replace("/ ", "/\n"));

                                    List<BaseComponent> hoverComponents = new ArrayList<>(LotteryRegistry.NUMBERS_PER_BET + 1);
                                    for (PrizeTier prizeTier : PrizeTier.values()) {
                                        List<PlayerWinnings> playerWinnings = winningsForBet.get(prizeTier);
                                        if (playerWinnings != null && !playerWinnings.isEmpty()) {
                                            hoverComponents.add(new TextComponent(instance.multipleWinningsDescription
                                                    .replace("{Tier}", instance.tierNames.get(prizeTier))
                                                    .replace("{Times}", String.valueOf(playerWinnings.size()))
                                                    .replace("{Winnings}", StringUtils.formatComma(playerWinnings.get(0).getWinnings()))
                                                    .replace("{UnitPrice}", StringUtils.formatComma(game.getPricePerBet(winnings.getWinningBet(game).getType())))));
                                        }
                                    }
                                    for (int i = 0; i < hoverComponents.size() - 1; i++) {
                                        hoverComponents.get(i).addExtra("\n");
                                    }

                                    numbersText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverComponents.toArray(new BaseComponent[0])));
                                    text.addExtra(numbersText);
                                    text.addExtra("\n");
                                    text.addExtra(instance.combinationWinningsDescription
                                            .replace("{HighestTier}", instance.tierNames.get(winnings.getTier()))
                                            .replace("{Winnings}", StringUtils.formatComma(winningsForBet.values().stream().flatMap(each -> each.stream()).mapToLong(each -> each.getWinnings()).sum()))
                                            .replace("{UnitPrice}", StringUtils.formatComma(game.getPricePerBet(winnings.getWinningBet(game).getType()))));
                                    pages.add(text);
                                } else {
                                    String str = winningNumberStr + "\n\n" + winnings.getWinningBet(game).getChosenNumbers().toFormattedString().replace("/ ", "/\n") + "\n";
                                    str += instance.winningsDescription
                                            .replace("{Tier}", instance.tierNames.get(winnings.getTier()))
                                            .replace("{Winnings}", StringUtils.formatComma(winnings.getWinnings()))
                                            .replace("{UnitPrice}", StringUtils.formatComma(game.getPricePerBet(winnings.getWinningBet(game).getType())));
                                    pages.add(new TextComponent(str));
                                }
                            }
                        }
                        for (PlayerBets bets : game.getPlayerBets(player.getUniqueId())) {
                            if (pages.size() > 100) {
                                break;
                            }
                            if (!displayedBets.contains(bets.getBetId())) {
                                String str = winningNumberStr + "\n\n" + bets.getChosenNumbers().toFormattedString().replace("/ ", "/\n") + "\n";
                                str += instance.winningsDescription
                                        .replace("{Tier}", LotteryUtils.formatPlaceholders(player, instance.guiLastResultsNoWinnings, instance, game))
                                        .replace("{Winnings}", StringUtils.formatComma(0))
                                        .replace("{UnitPrice}", StringUtils.formatComma(game.getPricePerBet(bets.getType())));
                                pages.add(new TextComponent(str));
                            }
                        }
                        if (pages.isEmpty()) {
                            pages.add(new TextComponent(winningNumberStr + "\n\n" + String.join("\n", LotteryUtils.formatPlaceholders(player, instance.guiLastResultsNothing, instance, game)) + "\n"));
                        }
                        ItemStack itemStack = XMaterial.WRITTEN_BOOK.parseItem();
                        BookMeta meta = (BookMeta) itemStack.getItemMeta();
                        meta.setAuthor("LotterySix");
                        meta.setTitle("LotterySix");
                        itemStack.setItemMeta(meta);
                        ItemStack book = BookUtils.setPagesComponent(itemStack, pages);
                        Scheduler.runTaskLater(plugin, () -> BookUtils.openBook((Player) click.getWhoClicked(), book), 1, player);
                    }, 1);
                }, 1, player);
                return true;
            }, LotteryUtils.formatPlaceholders(player, instance.guiLastResultsYourBets, instance, game)));
            gui.addElement(new StaticGuiElement('j', XMaterial.MAP.parseItem(), click -> {
                Scheduler.runTaskLater(plugin, () -> close(click.getWhoClicked(), click.getGui(), false), 1, player);
                Scheduler.runTaskLater(plugin, () -> getPastResultsList(player, game).show(player), 2, player);
                return true;
            }, LotteryUtils.formatPlaceholders(player, instance.guiLastResultsListHistoricGames, instance, game)));
        }
        gui.setFiller(XMaterial.YELLOW_STAINED_GLASS_PANE.parseItem());
        gui.addElement(new StaticGuiElement('z', new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));
        gui.setCloseAction(action -> {
            Scheduler.runTaskLater(plugin, () -> checkReopen(action.getPlayer()), 5, player);
            return false;
        });

        return gui;
    }

    public InventoryGui getPastResultsList(Player player, CompletedLotterySixGame lastSelectedGame) {
        return getPastResultsList(player, instance.getCompletedGames().indexOf(lastSelectedGame), lastSelectedGame);
    }

    public InventoryGui getPastResultsList(Player player, int currentPosition, CompletedLotterySixGame lastSelectedGame) {
        LazyCompletedLotterySixGameList completedGames = instance.getCompletedGames();
        LinkedList<CompletedLotterySixGameIndex> list = new LinkedList<>();
        Map<CompletedLotterySixGameIndex, Integer> position = new HashMap<>();
        int startPosition = Math.max(0, currentPosition - currentPosition % 5);
        for (int i = startPosition; i < startPosition + 5 && i < completedGames.size(); i++) {
            CompletedLotterySixGameIndex gameIndex = completedGames.getIndex(i);
            list.add(gameIndex);
            position.put(gameIndex, i);
        }
        String[] guiSetup = fillChars(5, 1);
        char lastChar = guiSetup[guiSetup.length - 1].charAt(8);
        guiSetup[5] = " \0  \1  \2 ";
        InventoryGui gui = new InventoryGui(plugin, LotteryUtils.formatPlaceholders(player, instance.guiLastResultsHistoricGameListTitle
                .replace("{FromGameNumber}", list.getFirst().getGameNumber() + "").replace("{ToGameNumber}", list.getLast().getGameNumber() + ""), instance, lastSelectedGame), guiSetup);
        gui.setFiller(XMaterial.BLACK_STAINED_GLASS_PANE.parseItem());
        if (startPosition >= 5) {
            gui.addElement(new StaticGuiElement('\0', XMaterial.ARROW.parseItem(), click -> {
                Scheduler.runTaskLater(plugin, () -> {
                    getPastResultsList(player, currentPosition - 5, lastSelectedGame).show(player);
                    removeSecondLast(player);
                }, 1, player);
                return true;
            }, LotteryUtils.formatPlaceholders(player, instance.guiLastResultsHistoricNewerGames, instance, lastSelectedGame)));
        } else {
            gui.addElement(new StaticGuiElement('\0', XMaterial.BLACK_STAINED_GLASS_PANE.parseItem(), ChatColor.LIGHT_PURPLE.toString()));
        }
        if (startPosition + 5 < completedGames.size()) {
            gui.addElement(new StaticGuiElement('\2', XMaterial.ARROW.parseItem(), click -> {
                Scheduler.runTaskLater(plugin, () -> {
                    getPastResultsList(player, currentPosition + 5, lastSelectedGame).show(player);
                    removeSecondLast(player);
                }, 1, player);
                return true;
            }, LotteryUtils.formatPlaceholders(player, instance.guiLastResultsHistoricOlderGames, instance, lastSelectedGame)));
        } else {
            gui.addElement(new StaticGuiElement('\2', XMaterial.BLACK_STAINED_GLASS_PANE.parseItem(), ChatColor.LIGHT_PURPLE.toString()));
        }
        char c = 'a';
        for (CompletedLotterySixGameIndex gameIndex : list) {
            gui.addElement(new StaticGuiElement(c++, XMaterial.PAPER.parseItem(), click -> {
                Scheduler.runTaskLater(plugin, () -> close(player, gui, false), 1, player);
                Scheduler.runTaskLater(plugin, () -> getPastResults(player, completedGames.get(position.get(gameIndex))).show(player), 2, player);
                return true;
            }, LotteryUtils.formatPlaceholders(player, instance.guiLastResultsHistoricGameListInfo, instance, gameIndex)));
            if (gameIndex.hasSpecialName()) {
                gui.addElement(new StaticGuiElement(c++, setEnchanted(XMaterial.GOLD_INGOT.parseItem()), LotteryUtils.formatPlaceholders(player, instance.guiLastResultsHistoricGameListSpecialName, instance, gameIndex)));
            } else {
                gui.addElement(new StaticGuiElement(c++, new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));
            }
            WinningNumbers winningNumbers = gameIndex.getDrawResult();
            for (int i : winningNumbers.getNumbersOrdered()) {
                gui.addElement(new StaticGuiElement(c++, getNumberItem(i, NumberSelectedState.NOT_SELECTED), getNumberColor(i) + "" + i));
            }
            int specialNumber = winningNumbers.getSpecialNumber();
            gui.addElement(new StaticGuiElement(c++, getNumberItem(specialNumber, true, NumberSelectedState.NOT_SELECTED), getNumberColor(specialNumber) + "" + specialNumber));
        }
        while (c <= lastChar) {
            gui.addElement(new StaticGuiElement(c++, new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));
        }

        ItemStack left = XMaterial.PAPER.parseItem();
        ItemMeta leftMeta = left.getItemMeta();
        leftMeta.setDisplayName(lastSelectedGame.getGameNumber() == null ? " " : lastSelectedGame.getGameNumber().toString());
        left.setItemMeta(leftMeta);
        gui.addElement(new StaticGuiElement('\1', XMaterial.MAP.parseItem(), click -> {
            Scheduler.runTaskLater(plugin, () -> close(click.getWhoClicked(), click.getGui(), false), 1, player);
            Scheduler.runTaskLater(plugin, () -> new AnvilGUI.Builder().plugin(plugin)
                    .title(LotteryUtils.formatPlaceholders(player, instance.guiGameNumberInputTitle, instance, lastSelectedGame))
                    .itemLeft(left)
                    .onComplete(completion -> {
                        Scheduler.runTaskAsynchronously(plugin, () -> {
                            String input = completion.getText().trim();
                            try {
                                GameNumber gameNumber = GameNumber.fromString(input);
                                CompletedLotterySixGame targetGame = instance.getCompletedGames().get(gameNumber);
                                if (targetGame == null) {
                                    player.sendMessage(instance.messageGameNumberNotFound);
                                } else {
                                    Scheduler.runTaskLater(plugin, () -> getPastResults(player, targetGame).show(player), 2, player);
                                }
                            } catch (Exception e) {
                                player.sendMessage(instance.messageGameNumberNotFound);
                            }
                        });
                        return Collections.singletonList(AnvilGUI.ResponseAction.close());
                    })
                    .open(player), 2, player);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.guiLastResultsLookupHistoricGames, instance, lastSelectedGame)));
        gui.setCloseAction(action -> {
            Scheduler.runTaskLater(plugin, () -> checkReopen(action.getPlayer()), 5, player);
            return false;
        });

        return gui;
    }

    public InventoryGui getNumberStatistics(Player player, CompletedLotterySixGame game) {
        int num = instance.numberOfChoices;
        String[] guiSetup = fillChars((num + 1) / 9 + 1);
        String last = guiSetup[guiSetup.length - 1];
        guiSetup[guiSetup.length - 1] = last.substring(0, last.length() - 1) + "\0";
        InventoryGui gui = new InventoryGui(plugin, LotteryUtils.formatPlaceholders(player, instance.guiNumberStatisticsTitle, instance, game), guiSetup);
        char c = 'a';
        for (int i = 0; i < num; i++) {
            int number = i + 1;
            NumberStatistics stats = game == null ? NumberStatistics.NOT_EVER_DRAWN : game.getNumberStatistics(number);
            gui.addElement(new StaticGuiElement(c++, getNumberItem(number, stats.getLastDrawn() == 0, NumberSelectedState.NOT_SELECTED),
                    getNumberColor(number) + "" + number,
                    LotteryUtils.formatPlaceholders(player, instance.guiNumberStatisticsLastDrawn.replace("{Number}", number + ""), instance, game),
                    LotteryUtils.formatPlaceholders(player, instance.guiNumberStatisticsTimesDrawn.replace("{Number}", number + ""), instance, game)
            ));
        }
        gui.addElement(new StaticGuiElement('\0', XMaterial.OAK_SIGN.parseItem(), LotteryUtils.formatPlaceholders(player, instance.guiNumberStatisticsNote, instance, game)));

        return gui;
    }

    public enum NumberSelectedState {

        NOT_SELECTED(0, true),
        SELECTED(100, false),
        SELECTED_BANKER(200, false);

        private final int customModelDataOffset;
        private final boolean shouldSetStack;

        NumberSelectedState(int customModelDataOffset, boolean shouldSetStack) {
            this.customModelDataOffset = customModelDataOffset;
            this.shouldSetStack = shouldSetStack;
        }

        public int getCustomModelDataOffset() {
            return customModelDataOffset;
        }

        public boolean shouldSetStack() {
            return shouldSetStack;
        }
    }

    public enum AccountTransactionMode {

        WITHDRAW, DEPOSIT;

    }

}
