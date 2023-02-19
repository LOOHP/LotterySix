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
import com.loohp.lotterysix.game.objects.IntObjectConsumer;
import com.loohp.lotterysix.game.objects.LotteryPlayer;
import com.loohp.lotterysix.game.objects.NumberStatistics;
import com.loohp.lotterysix.game.objects.PlayerBets;
import com.loohp.lotterysix.game.objects.PlayerPreferenceKey;
import com.loohp.lotterysix.game.objects.PlayerStatsKey;
import com.loohp.lotterysix.game.objects.PlayerWinnings;
import com.loohp.lotterysix.game.objects.betnumbers.BetNumbers;
import com.loohp.lotterysix.game.objects.betnumbers.BetNumbersBuilder;
import com.loohp.lotterysix.game.objects.betnumbers.BetNumbersType;
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
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
            itemStack = setEnchanted(itemStack);
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
    private final AtomicInteger tickCounter;

    public LotteryPluginGUI(LotterySixPlugin plugin) {
        this.plugin = plugin;
        this.instance = LotterySixPlugin.getInstance();
        this.lastGuiClick = new HashMap<>();
        this.tickCounter = new AtomicInteger();
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            tickCounter.incrementAndGet();
        }, 0, 1);
    }

    public void forceClose(Player player) {
        InventoryGui gui = InventoryGui.getOpen(player);
        if (gui != null) {
            gui.close(player, true);
        }
    }

    private void handleClick(InventoryInteractEvent event) {
        Player player = (Player) event.getWhoClicked();
        long gameTick = tickCounter.get();
        Long lastClick = lastGuiClick.get(player);
        if (lastClick == null || gameTick != lastClick) {
            lastGuiClick.put(player, gameTick);
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        lastGuiClick.remove(player);
        Deque<InventoryGui> guis = InventoryGui.clearHistory(player);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            InventoryGui gui;
            while ((gui = guis.poll()) != null) {
                gui.destroy();
            }
        }, 1);
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
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    InventoryGui.clearHistory(event.getWhoClicked());
                    event.getWhoClicked().closeInventory();
                }, 1);
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
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    InventoryGui.clearHistory(event.getWhoClicked());
                    event.getWhoClicked().closeInventory();
                }, 1);
            }
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
                "fyaaaaaaa",
                "aybacadae",
                "zyaaaaaaa",
                "         "
        };
        InventoryGui gui = new InventoryGui(plugin, LotteryUtils.formatPlaceholders(player, instance.guiMainMenuTitle, instance), guiSetup);
        gui.setFiller(XMaterial.YELLOW_STAINED_GLASS_PANE.parseItem());
        gui.addElement(new StaticGuiElement('a', new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));
        gui.addElement(new StaticGuiElement('y', XMaterial.ORANGE_STAINED_GLASS_PANE.parseItem(), ChatColor.LIGHT_PURPLE.toString()));
        gui.addElement(new StaticGuiElement('b', XMaterial.CLOCK.parseItem(), click -> {
            Bukkit.getScheduler().runTaskLater(plugin, () -> close(click.getWhoClicked(), gui, false), 1);
            Bukkit.getScheduler().runTaskLater(plugin, () -> getPastResults((Player) click.getWhoClicked(), instance.getCompletedGames().isEmpty() ? null : instance.getCompletedGames().get(0)).show(click.getWhoClicked()), 2);
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
            LotteryPlayer lotteryPlayer = instance.getPlayerPreferenceManager().getLotteryPlayer(player.getUniqueId());
            Long money = lotteryPlayer.getStats(PlayerStatsKey.PENDING_TRANSACTION, long.class);
            if (money != null && money > 0) {
                return new StaticGuiElement('d', XMaterial.EMERALD.parseItem(), click -> {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        close(click.getWhoClicked(), click.getGui(), false);
                        LotterySixPlugin.notifyPendingTransactions(lotteryPlayer);
                    }, 1);
                    return true;
                }, LotteryUtils.formatPlaceholders(player, instance.guiMainMenuPendingTransactions, instance));
            } else {
                PlayableLotterySixGame currentGame = instance.getCurrentGame();
                if (currentGame == null) {
                    return new StaticGuiElement('d', XMaterial.RED_WOOL.parseItem(), LotteryUtils.formatPlaceholders(player, instance.guiMainMenuNoLotteryGamesScheduled, instance));
                } else {
                    return new StaticGuiElement('d', XMaterial.GOLD_INGOT.parseItem(), click -> {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> getBetTypeChooser((Player) click.getWhoClicked(), instance.getCurrentGame()).show(click.getWhoClicked()), 1);
                        return true;
                    }, LotteryUtils.formatPlaceholders(player, instance.guiMainMenuPlaceNewBets, instance, currentGame));
                }
            }
        }));
        gui.addElement(new StaticGuiElement('e', XMaterial.OAK_SIGN.parseItem(), click -> {
            Bukkit.getScheduler().runTaskLater(plugin, () -> getNumberStatistics((Player) click.getWhoClicked(), instance.getCompletedGames().isEmpty() ? null : instance.getCompletedGames().get(0)).show(click.getWhoClicked()), 1);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.guiMainMenuStatistics, instance)));
        gui.addElement(new StaticGuiElement('z', XMaterial.COMPASS.parseItem(), click -> {
            TextComponent message = new TextComponent(LotteryUtils.formatPlaceholders(player, instance.explanationMessage, instance));
            message.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, instance.explanationURL));
            click.getWhoClicked().spigot().sendMessage(message);
            Bukkit.getScheduler().runTaskLater(plugin, () -> close(click.getWhoClicked(), click.getGui(), true), 1);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.explanationGUIItem, instance)));
        gui.addElement(new StaticGuiElement('f', SkinUtils.getSkull(player.getUniqueId()), click -> {
            Bukkit.getScheduler().runTaskLater(plugin, () -> getBettingAccount((Player) click.getWhoClicked()).show(click.getWhoClicked()), 1);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.guiMainMenuBettingAccount, instance)));
        gui.setCloseAction(close -> false);
        Set<GuiElement> elements = Collections.singleton(gui.getElement('c'));
        new BukkitRunnable() {
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
        }.runTaskTimer(plugin, 10, 10);
        return gui;
    }

    public InventoryGui getBettingAccount(Player player) {
        String[] guiSetup = {
                "         ",
                "bzacadaea",
                "azaaaaaaa",
                "azaafagaa",
                "         "
        };
        InventoryGui gui = new InventoryGui(plugin, LotteryUtils.formatPlaceholders(player, instance.guiBettingAccountTitle, instance), guiSetup);
        gui.setFiller(XMaterial.BLUE_STAINED_GLASS_PANE.parseItem());
        gui.addElement(new StaticGuiElement('a', new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));
        gui.addElement(new StaticGuiElement('z', XMaterial.LIGHT_BLUE_STAINED_GLASS_PANE.parseItem(), ChatColor.LIGHT_PURPLE.toString()));
        gui.addElement(new StaticGuiElement('b', SkinUtils.getSkull(player.getUniqueId()), LotteryUtils.formatPlaceholders(player, instance.guiBettingAccountProfile, instance)));

        LotteryPlayer lotteryPlayer = instance.getPlayerPreferenceManager().getLotteryPlayer(player.getUniqueId());
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
                    Bukkit.getScheduler().runTaskLater(plugin, () -> close(click.getWhoClicked(), click.getGui(), false), 1);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> new AnvilGUI.Builder().plugin(plugin)
                            .title(LotteryUtils.formatPlaceholders(player, instance.guiBettingAccountSetBetLimitPerRoundTitle, instance))
                            .itemLeft(left)
                            .onComplete(completion -> {
                                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                                    String input = completion.getText().trim();
                                    try {
                                        long newValue = Math.max(0, Long.parseLong(input));
                                        if (instance.backendBungeecordMode) {
                                            LotterySixPlugin.getPluginMessageHandler().updatePlayerPreference(lotteryPlayer, PlayerPreferenceKey.BET_LIMIT_PER_ROUND, newValue);
                                        } else {
                                            lotteryPlayer.setPreference(PlayerPreferenceKey.BET_LIMIT_PER_ROUND, newValue);
                                        }
                                        Bukkit.getScheduler().runTaskLater(plugin, () -> getMainMenu(player).show(player), 2);
                                        Bukkit.getScheduler().runTaskLater(plugin, () -> getBettingAccount(player).show(player), 3);
                                    } catch (Exception e) {
                                        player.sendMessage(instance.messageInvalidUsage);
                                    }
                                });
                                return Collections.singletonList(AnvilGUI.ResponseAction.close());
                            })
                            .open(player), 2);
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
        gui.setSilent(true);
        new BukkitRunnable() {
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
        }.runTaskTimer(plugin, 10, 10);
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
            pages.add(title + "\n\n" + String.join("\n", LotteryUtils.formatPlaceholders(player, instance.guiYourBetsNothing, instance, game)));
        } else {
            for (PlayerBets bet : bets) {
                pages.add(title + "\n\n" + bet.getChosenNumbers().toColoredString() + "\n" + ChatColor.GOLD + "$" + StringUtils.formatComma(bet.getBet()) + " ($" + StringUtils.formatComma(instance.pricePerBet / bet.getType().getDivisor()) + ")");
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
        InventoryGui gui = new InventoryGui(plugin, LotteryUtils.formatPlaceholders(player, instance.guiSelectNewBetTypeTitle, instance), guiSetup);
        gui.setFiller(XMaterial.ORANGE_STAINED_GLASS_PANE.parseItem());
        gui.addElement(new StaticGuiElement('a', new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));
        gui.addElement(new StaticGuiElement('b', XMaterial.BRICK.parseItem(), click -> {
            Bukkit.getScheduler().runTaskLater(plugin, () -> getNumberChooser((Player) click.getWhoClicked(), game, BetNumbersBuilder.single(1, instance.numberOfChoices)).show(click.getWhoClicked()), 1);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.guiSelectNewBetTypeSingle, instance)));
        gui.addElement(new StaticGuiElement('c', XMaterial.IRON_INGOT.parseItem(), click -> {
            Bukkit.getScheduler().runTaskLater(plugin, () -> getNumberChooser((Player) click.getWhoClicked(), game, BetNumbersBuilder.multiple(1, instance.numberOfChoices)).show(click.getWhoClicked()), 1);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.guiSelectNewBetTypeMultiple, instance)));
        gui.addElement(new StaticGuiElement('d', XMaterial.GOLD_INGOT.parseItem(), click -> {
            Bukkit.getScheduler().runTaskLater(plugin, () -> getNumberChooser((Player) click.getWhoClicked(), game, BetNumbersBuilder.banker(1, instance.numberOfChoices)).show(click.getWhoClicked()), 1);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.guiSelectNewBetTypeBanker, instance)));
        gui.addElement(new StaticGuiElement('e', XMaterial.REDSTONE.parseItem(), click -> {
            Bukkit.getScheduler().runTaskLater(plugin, () -> getRandomEntryCountChooser((Player) click.getWhoClicked(), game).show(click.getWhoClicked()), 1);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.guiSelectNewBetTypeRandom, instance)));
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
        InventoryGui gui = new InventoryGui(plugin, LotteryUtils.formatPlaceholders(player, instance.guiRandomEntryCountTitle, instance), guiSetup);
        gui.setFiller(XMaterial.RED_STAINED_GLASS_PANE.parseItem());
        gui.addElement(new StaticGuiElement('a', new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));
        gui.addElement(new StaticGuiElement('b', setItemSize(XMaterial.PAPER.parseItem(), 1), click -> {
            Bukkit.getScheduler().runTaskLater(plugin, () -> close(click.getWhoClicked(), click.getGui(), false), 1);
            Bukkit.getScheduler().runTaskLater(plugin, () -> getSingleNumberConfirm((Player) click.getWhoClicked(), game, BetNumbersBuilder.random(1, instance.numberOfChoices).build()).show(click.getWhoClicked()), 2);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.guiRandomEntryCountValue.replace("{Count}", "1"), instance)));
        gui.addElement(new StaticGuiElement('c', setItemSize(XMaterial.PAPER.parseItem(), 2), click -> {
            Bukkit.getScheduler().runTaskLater(plugin, () -> close(click.getWhoClicked(), click.getGui(), false), 1);
            Bukkit.getScheduler().runTaskLater(plugin, () -> getBulkNumberConfirm((Player) click.getWhoClicked(), game, BetNumbersBuilder.bulkRandom(1, instance.numberOfChoices, 2).map(r -> r.build()).collect(Collectors.toList())).show(click.getWhoClicked()), 2);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.guiRandomEntryCountValue.replace("{Count}", "2"), instance)));
        gui.addElement(new StaticGuiElement('d', setItemSize(XMaterial.PAPER.parseItem(), 5), click -> {
            Bukkit.getScheduler().runTaskLater(plugin, () -> close(click.getWhoClicked(), click.getGui(), false), 1);
            Bukkit.getScheduler().runTaskLater(plugin, () -> getBulkNumberConfirm((Player) click.getWhoClicked(), game, BetNumbersBuilder.bulkRandom(1, instance.numberOfChoices, 5).map(r -> r.build()).collect(Collectors.toList())).show(click.getWhoClicked()), 2);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.guiRandomEntryCountValue.replace("{Count}", "5"), instance)));
        gui.addElement(new StaticGuiElement('e', setItemSize(XMaterial.PAPER.parseItem(), 10), click -> {
            Bukkit.getScheduler().runTaskLater(plugin, () -> close(click.getWhoClicked(), click.getGui(), false), 1);
            Bukkit.getScheduler().runTaskLater(plugin, () -> getBulkNumberConfirm((Player) click.getWhoClicked(), game, BetNumbersBuilder.bulkRandom(1, instance.numberOfChoices, 10).map(r -> r.build()).collect(Collectors.toList())).show(click.getWhoClicked()), 2);
            return true;
        }, LotteryUtils.formatPlaceholders(player, instance.guiRandomEntryCountValue.replace("{Count}", "10"), instance)));
        gui.setSilent(true);
        return gui;
    }

    public InventoryGui getNumberChooser(Player player, PlayableLotterySixGame game, BetNumbersBuilder builder) {
        int num = instance.numberOfChoices;
        String[] guiSetup = fillChars((num + (builder.getType().isMultipleCombination() ? 2 : 1)) / 9 + 1);
        String last = guiSetup[guiSetup.length - 1];
        guiSetup[guiSetup.length - 1] = last.substring(0, last.length() - 2) + "\1\0";
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
        InventoryGui gui = new InventoryGui(plugin, LotteryUtils.formatPlaceholders(player, title.replace("{Price}", StringUtils.formatComma(LotteryUtils.calculatePrice(builder, instance))).replace("{MinSelection}", ((isBanker ? ((BetNumbersBuilder.BankerBuilder) builder).getMinSelectionsNeeded() : 6) - builder.size()) + ""), instance, game), guiSetup);
        char c = 'a';
        IntObjectConsumer<Player> handleClick = (number, clicker) -> {
            if (builder.completed() || (isBanker && !((BetNumbersBuilder.BankerBuilder) builder).getBankers().isEmpty())) {
                if (isFixed) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> close(clicker, gui, false), 1);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        BetNumbers betNumbers = builder.build();
                        if (betNumbers.getType().equals(BetNumbersType.SINGLE)) {
                            getSingleNumberConfirm(clicker, game, betNumbers).show(clicker);
                        } else {
                            getComplexNumberConfirm(clicker, game, betNumbers).show(clicker);
                        }
                    }, 2);
                } else if (isBanker && !((BetNumbersBuilder.BankerBuilder) builder).inSelectionPhase() && ((BetNumbersBuilder.BankerBuilder) builder).bankerCompleted()) {
                    ((BetNumbersBuilder.BankerBuilder) builder).finishBankers();
                    gui.removeElement('\0');
                    gui.addElement(new StaticGuiElement('\0', XMaterial.GRAY_DYE.parseItem(), LotteryUtils.formatPlaceholders(player, instance.guiNewBetNotYetFinish, instance, game)));
                    for (int banker : ((BetNumbersBuilder.BankerBuilder) builder).getBankers()) {
                        char bankerC = (char) ('a' + banker - 1);
                        gui.removeElement(bankerC);
                        gui.addElement(new StaticGuiElement(bankerC, XMaterial.YELLOW_WOOL.parseItem(), ChatColor.YELLOW + "" + banker));
                    }
                } else if (builder.completed() || (isBanker && !((BetNumbersBuilder.BankerBuilder) builder).inSelectionPhase() && !((BetNumbersBuilder.BankerBuilder) builder).getBankers().isEmpty())) {
                    gui.removeElement('\0');
                    gui.addElement(new StaticGuiElement('\0', isBanker && !((BetNumbersBuilder.BankerBuilder) builder).inSelectionPhase() ? XMaterial.EMERALD.parseItem() : XMaterial.GOLD_INGOT.parseItem(), click -> {
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
                            gui.addElement(new StaticGuiElement('\0', XMaterial.GRAY_DYE.parseItem(), LotteryUtils.formatPlaceholders(player, instance.guiNewBetNotYetFinish, instance, game)));
                            for (int banker : ((BetNumbersBuilder.BankerBuilder) builder).getBankers()) {
                                char bankerC = (char) ('a' + banker - 1);
                                gui.removeElement(bankerC);
                                gui.addElement(new StaticGuiElement(bankerC, XMaterial.YELLOW_WOOL.parseItem(), ChatColor.YELLOW + "" + banker));
                            }
                            gui.draw(clicker);
                        }
                        return true;
                    }, LotteryUtils.formatPlaceholders(player, Arrays.stream(isBanker && !((BetNumbersBuilder.BankerBuilder) builder).inSelectionPhase() ? instance.guiNewBetFinishBankers : instance.guiNewBetFinish)
                            .map(each -> each.replace("{Price}", StringUtils.formatComma(LotteryUtils.calculatePrice(builder, instance)))).toArray(String[]::new), instance, game)));
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
                                builder.addNumber(number);
                                handleClick.accept(number, (Player) change.getWhoClicked());
                                gui.draw(change.getWhoClicked());
                            },
                            "true",
                            XMaterial.LIGHT_GRAY_WOOL.parseItem(),
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
                                    ((StaticGuiElement) gui.getElement('\0')).setText(LotteryUtils.formatPlaceholders(player, Arrays.stream(isBanker ? instance.guiNewBetFinishBankers : instance.guiNewBetFinish)
                                            .map(each -> each.replace("{Price}", StringUtils.formatComma(LotteryUtils.calculatePrice(builder, instance)))).toArray(String[]::new), instance, game));
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
        if (builder.getType().isMultipleCombination()) {
            char allChar = c;
            gui.addElement(new DynamicGuiElement(allChar, viewer -> {
                if (builder.canAdd()) {
                    if (!isBanker || ((BetNumbersBuilder.BankerBuilder) builder).inSelectionPhase()) {
                        return new StaticGuiElement(allChar, NBTEditor.set(XMaterial.ORANGE_WOOL.parseItem(), -1, "LotterySixNumber"), click -> {
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
                    return new StaticGuiElement(allChar, XMaterial.GRAY_WOOL.parseItem(), ChatColor.DARK_GRAY + instance.guiNewBetSelectAll);
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
        gui.addElement(new StaticGuiElement('a', XMaterial.PAPER.parseItem(), StringUtils.wrapAtSpace(betNumbers.toColoredString(), 6)
                .replace("{Price}", StringUtils.formatComma(price)).replace("{PricePartial}", StringUtils.formatComma(partial))));
        gui.addElement(new StaticGuiElement('g', XMaterial.DIAMOND.parseItem(), Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetLotteryInfo, instance, game))
                .map(each -> each.replace("{Price}", StringUtils.formatComma(price)).replace("{PricePartial}", StringUtils.formatComma(partial))).toArray(String[]::new)));
        gui.addElement(new StaticGuiElement('h', XMaterial.GOLD_NUGGET.parseItem(), click -> {
            if (game != null && game.isValid()) {
                if (instance.backendBungeecordMode) {
                    LotterySixPlugin.getPluginMessageHandler().requestAddBet(player.getName(), player.getUniqueId(), partial, BetUnitType.PARTIAL, betNumbers);
                } else {
                    AddBetResult result = game.addBet(player.getName(), player.getUniqueId(), partial, BetUnitType.PARTIAL, betNumbers);
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
                        case ACCOUNT_SUSPENDED: {
                            long time = instance.getPlayerPreferenceManager().getLotteryPlayer(player.getUniqueId()).getPreference(PlayerPreferenceKey.SUSPEND_ACCOUNT_UNTIL, long.class);
                            player.sendMessage(instance.messageBettingAccountSuspended.replace("{Date}", instance.dateFormat.format(new Date(time))).replace("{Price}", StringUtils.formatComma(partial)));
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
                .map(each -> each.replace("{Price}", StringUtils.formatComma(price)).replace("{PricePartial}", StringUtils.formatComma(partial))).toArray(String[]::new)));
        gui.addElement(new StaticGuiElement('i', XMaterial.GOLD_INGOT.parseItem(), click -> {
            if (game != null && game.isValid()) {
                if (instance.backendBungeecordMode) {
                    LotterySixPlugin.getPluginMessageHandler().requestAddBet(player.getName(), player.getUniqueId(), price, BetUnitType.FULL, betNumbers);
                } else {
                    AddBetResult result = game.addBet(player.getName(), player.getUniqueId(), price, BetUnitType.FULL, betNumbers);
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
                        case ACCOUNT_SUSPENDED: {
                            long time = instance.getPlayerPreferenceManager().getLotteryPlayer(player.getUniqueId()).getPreference(PlayerPreferenceKey.SUSPEND_ACCOUNT_UNTIL, long.class);
                            player.sendMessage(instance.messageBettingAccountSuspended.replace("{Date}", instance.dateFormat.format(new Date(time))).replace("{Price}", StringUtils.formatComma(price)));
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
                .map(each -> each.replace("{Price}", StringUtils.formatComma(price)).replace("{PricePartial}", StringUtils.formatComma(partial))).toArray(String[]::new)));
        gui.addElement(new StaticGuiElement('j', XMaterial.BARRIER.parseItem(), click -> {
            Bukkit.getScheduler().runTaskLater(plugin, () -> close(click.getWhoClicked(), click.getGui(), false), 1);
            Bukkit.getScheduler().runTaskLater(plugin, () -> checkReopen(click.getWhoClicked()), 5);
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
        gui.addElement(new StaticGuiElement('g', XMaterial.DIAMOND.parseItem(), Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetLotteryInfo, instance, game))
                .map(each -> each.replace("{Price}", StringUtils.formatComma(price))).toArray(String[]::new)));
        gui.addElement(new StaticGuiElement('h', XMaterial.GOLD_INGOT.parseItem(), click -> {
            if (game != null && game.isValid()) {
                if (instance.backendBungeecordMode) {
                    LotterySixPlugin.getPluginMessageHandler().requestAddBet(player.getName(), player.getUniqueId(), instance.pricePerBet, BetUnitType.FULL, betNumbers);
                } else {
                    AddBetResult result = game.addBet(player.getName(), player.getUniqueId(), instance.pricePerBet, BetUnitType.FULL, betNumbers);
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
                        case ACCOUNT_SUSPENDED: {
                            long time = instance.getPlayerPreferenceManager().getLotteryPlayer(player.getUniqueId()).getPreference(PlayerPreferenceKey.SUSPEND_ACCOUNT_UNTIL, long.class);
                            player.sendMessage(instance.messageBettingAccountSuspended.replace("{Date}", instance.dateFormat.format(new Date(time))).replace("{Price}", StringUtils.formatComma(price)));
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
                .map(each -> each.replace("{Price}", StringUtils.formatComma(price))).toArray(String[]::new)));
        gui.addElement(new StaticGuiElement('i', XMaterial.BARRIER.parseItem(), click -> {
            Bukkit.getScheduler().runTaskLater(plugin, () -> close(click.getWhoClicked(), click.getGui(), false), 1);
            Bukkit.getScheduler().runTaskLater(plugin, () -> checkReopen(click.getWhoClicked()), 5);
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
                .replace("{Price}", StringUtils.formatComma(price))).toArray(String[]::new);
        gui.addElement(new StaticGuiElement('a', XMaterial.PAPER.parseItem(), numbersStr));
        gui.addElement(new StaticGuiElement('g', XMaterial.DIAMOND.parseItem(), Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetLotteryInfo, instance, game))
                .map(each -> each.replace("{Price}", StringUtils.formatComma(price))).toArray(String[]::new)));
        gui.addElement(new StaticGuiElement('h', XMaterial.GOLD_INGOT.parseItem(), click -> {
            if (game != null && game.isValid()) {
                if (instance.backendBungeecordMode) {
                    LotterySixPlugin.getPluginMessageHandler().requestAddBet(player.getName(), player.getUniqueId(), instance.pricePerBet, BetUnitType.FULL, betNumbers);
                } else {
                    AddBetResult result = game.addBet(player.getName(), player.getUniqueId(), instance.pricePerBet, BetUnitType.FULL, betNumbers);
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
                        case ACCOUNT_SUSPENDED: {
                            long time = instance.getPlayerPreferenceManager().getLotteryPlayer(player.getUniqueId()).getPreference(PlayerPreferenceKey.SUSPEND_ACCOUNT_UNTIL, long.class);
                            player.sendMessage(instance.messageBettingAccountSuspended.replace("{Date}", instance.dateFormat.format(new Date(time))).replace("{Price}", StringUtils.formatComma(price)));
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
                .map(each -> each.replace("{Price}", StringUtils.formatComma(price))).toArray(String[]::new)));
        gui.addElement(new StaticGuiElement('i', XMaterial.BARRIER.parseItem(), click -> {
            Bukkit.getScheduler().runTaskLater(plugin, () -> close(click.getWhoClicked(), click.getGui(), false), 1);
            Bukkit.getScheduler().runTaskLater(plugin, () -> checkReopen(click.getWhoClicked()), 5);
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
            gui.addElement(new StaticGuiElement('h', XMaterial.CLOCK.parseItem(), LotteryUtils.formatPlaceholders(player, instance.guiLastResultsLotteryInfo, instance, game)));

            gui.addElement(new StaticGuiElement('i', XMaterial.GOLD_INGOT.parseItem(), click -> {
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
                            str += ChatColor.GOLD + "" + winnings.getTier().getShortHand() + " $" + StringUtils.formatComma(winnings.getWinnings()) + " ($" + StringUtils.formatComma(game.getPricePerBet(winnings.getWinningBet(game).getType())) + ")";
                            pages.add(str);
                        }
                        for (PlayerBets bets : game.getPlayerBets(player.getUniqueId())) {
                            if (pages.size() > 100) {
                                break;
                            }
                            if (winningsList.stream().noneMatch(each -> each.getWinningBet(game).getBetId().equals(bets.getBetId()))) {
                                String str = winningNumberStr + "\n\n" + bets.getChosenNumbers().toColoredString() + "\n"
                                        + LotteryUtils.formatPlaceholders(player, instance.guiLastResultsNoWinnings, instance, game) + " $0 ($" + StringUtils.formatComma(game.getPricePerBet(bets.getType())) + ")";
                                pages.add(str);
                            }
                        }
                        if (pages.isEmpty()) {
                            pages.add(winningNumberStr + "\n\n" + String.join("\n", LotteryUtils.formatPlaceholders(player, instance.guiLastResultsNothing, instance, game)) + "\n");
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
        gui.setCloseAction(action -> {
            Bukkit.getScheduler().runTaskLater(plugin, () -> checkReopen(action.getPlayer()), 5);
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
            gui.addElement(new StaticGuiElement(c++, getNumberItem(number, stats.getLastDrawn() == 0),
                    getNumberColor(number) + "" + number,
                    LotteryUtils.formatPlaceholders(player, instance.guiNumberStatisticsLastDrawn.replace("{Number}", number + ""), instance, game),
                    LotteryUtils.formatPlaceholders(player, instance.guiNumberStatisticsTimesDrawn.replace("{Number}", number + ""), instance, game)
            ));
        }
        gui.addElement(new StaticGuiElement('\0', XMaterial.OAK_SIGN.parseItem(), LotteryUtils.formatPlaceholders(player, instance.guiNumberStatisticsNote, instance, game)));

        return gui;
    }

}
