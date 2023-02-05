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

import com.cronutils.model.Cron;
import com.loohp.lotterysix.debug.Debug;
import com.loohp.lotterysix.game.lottery.PlayableLotterySixGame;
import com.loohp.lotterysix.game.objects.LotteryPlayer;
import com.loohp.lotterysix.game.objects.PlayerPreferenceKey;
import com.loohp.lotterysix.game.objects.PlayerStatsKey;
import com.loohp.lotterysix.game.objects.betnumbers.BetNumbersType;
import com.loohp.lotterysix.updater.Updater;
import com.loohp.lotterysix.utils.ArrayUtils;
import com.loohp.lotterysix.utils.ChatColorUtils;
import com.loohp.lotterysix.utils.CronUtils;
import com.loohp.lotterysix.utils.LotteryUtils;
import com.loohp.lotterysix.utils.StringUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class Commands implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "LotterySix written by LOOHP!");
            sender.sendMessage(ChatColor.GOLD + "You are running LotterySix version: " + LotterySixPlugin.plugin.getDescription().getVersion());
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (sender.hasPermission("lotterysix.reload")) {
                LotterySixPlugin.getInstance().reloadConfig();
                if (LotterySixPlugin.discordSRVHook != null) {
                    LotterySixPlugin.discordSRVHook.reload();
                }
                sender.sendMessage(LotterySixPlugin.getInstance().messageReloaded);
            } else {
                sender.sendMessage(LotterySixPlugin.getInstance().messageNoPermission);
            }
            return true;
        } else if (args[0].equalsIgnoreCase("update")) {
            if (sender.hasPermission("lotterysix.update")) {
                sender.sendMessage(ChatColor.AQUA + "[LotterySix] LotterySix written by LOOHP!");
                sender.sendMessage(ChatColor.GOLD + "[LotterySix] You are running LotterySix version: " + LotterySixPlugin.plugin.getDescription().getVersion());
                Bukkit.getScheduler().runTaskAsynchronously(LotterySixPlugin.plugin, () -> {
                    Updater.UpdaterResponse version = Updater.checkUpdate();
                    if (version.getResult().equals("latest")) {
                        if (version.isDevBuildLatest()) {
                            sender.sendMessage(ChatColor.GREEN + "[LotterySix] You are running the latest version!");
                        } else {
                            Updater.sendUpdateMessage(sender, version.getResult(), version.getSpigotPluginId(), true);
                        }
                    } else {
                        Updater.sendUpdateMessage(sender, version.getResult(), version.getSpigotPluginId());
                    }
                });
            } else {
                sender.sendMessage(LotterySixPlugin.getInstance().messageNoPermission);
            }
            return true;
        }

        if (LotterySixPlugin.getInstance().backendBungeecordMode) {
            sender.sendMessage(ChatColor.RED + "LotterySix written by LOOHP!");
            sender.sendMessage(ChatColor.GOLD + "You are running LotterySix version: " + LotterySixPlugin.plugin.getDescription().getVersion());
            return true;
        }

        if (args[0].equalsIgnoreCase("play")) {
            if (sender.hasPermission("lotterysix.play")) {
                if (LotterySixPlugin.getInstance().isGameLocked()) {
                    sender.sendMessage(LotterySixPlugin.getInstance().messageGameLocked);
                    return true;
                }
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    LotterySixPlugin.getGuiProvider().getMainMenu(player).show(player);
                } else {
                    sender.sendMessage(LotterySixPlugin.getInstance().messageNoConsole);
                }
            } else {
                sender.sendMessage(LotterySixPlugin.getInstance().messageNoPermission);
            }
            return true;
        } else if (args[0].equalsIgnoreCase("start")) {
            if (sender.hasPermission("lotterysix.start")) {
                if (LotterySixPlugin.getInstance().isGameLocked()) {
                    sender.sendMessage(LotterySixPlugin.getInstance().messageGameLocked);
                    return true;
                }
                if (LotterySixPlugin.getInstance().getCurrentGame() == null) {
                    if (args.length > 1) {
                        try {
                            LotterySixPlugin.getInstance().startNewGame(Long.parseLong(args[1]));
                            sender.sendMessage(LotterySixPlugin.getInstance().messageGameStarted);
                        } catch (NumberFormatException e) {
                            sender.sendMessage(LotterySixPlugin.getInstance().messageInvalidUsage);
                        }
                    } else {
                        LotterySixPlugin.getInstance().startNewGame();
                        sender.sendMessage(LotterySixPlugin.getInstance().messageGameStarted);
                    }
                } else {
                    sender.sendMessage(LotterySixPlugin.getInstance().messageGameAlreadyRunning);
                }
            } else {
                sender.sendMessage(LotterySixPlugin.getInstance().messageNoPermission);
            }
            return true;
        } else if (args[0].equalsIgnoreCase("run")) {
            if (sender.hasPermission("lotterysix.run")) {
                if (LotterySixPlugin.getInstance().isGameLocked()) {
                    sender.sendMessage(LotterySixPlugin.getInstance().messageGameLocked);
                    return true;
                }
                if (LotterySixPlugin.getInstance().getCurrentGame() == null) {
                    sender.sendMessage(LotterySixPlugin.getInstance().messageNoGameRunning);
                } else {
                    LotterySixPlugin.getInstance().getCurrentGame().setScheduledDateTime(System.currentTimeMillis());
                }
            } else {
                sender.sendMessage(LotterySixPlugin.getInstance().messageNoPermission);
            }
            return true;
        } else if (args[0].equalsIgnoreCase("cancel")) {
            if (sender.hasPermission("lotterysix.cancel")) {
                if (LotterySixPlugin.getInstance().isGameLocked()) {
                    sender.sendMessage(LotterySixPlugin.getInstance().messageGameLocked);
                    return true;
                }
                if (LotterySixPlugin.getInstance().getCurrentGame() == null) {
                    sender.sendMessage(LotterySixPlugin.getInstance().messageNoGameRunning);
                } else {
                    LotterySixPlugin.getInstance().cancelCurrentGame();
                }
            } else {
                sender.sendMessage(LotterySixPlugin.getInstance().messageNoPermission);
            }
            return true;
        } else if (args[0].equalsIgnoreCase("preference")) {
            if (sender.hasPermission("lotterysix.preference")) {
                if (args.length > 2) {
                    UUID uuid;
                    if (sender instanceof Player) {
                        if (sender.hasPermission("lotterysix.preference.others")) {
                            uuid = ArrayUtils.getOptional(args, 3).map(s -> Bukkit.getOfflinePlayer(s)).orElseGet(() -> (Player) sender).getUniqueId();
                        } else {
                            uuid = ((Player) sender).getUniqueId();
                        }
                    } else {
                        if (args.length > 3) {
                            uuid = Bukkit.getOfflinePlayer(args[3]).getUniqueId();
                        } else {
                            sender.sendMessage(LotterySixPlugin.getInstance().messageNoConsole);
                            return true;
                        }
                    }
                    PlayerPreferenceKey preferenceKey = PlayerPreferenceKey.fromKey(args[1]);
                    if (preferenceKey == null) {
                        sender.sendMessage(LotterySixPlugin.getInstance().messageInvalidUsage);
                    } else {
                        String valueStr = args[2];
                        Object value = preferenceKey.getReader(valueStr);
                        if (value == null) {
                            sender.sendMessage(LotterySixPlugin.getInstance().messageInvalidUsage);
                        } else {
                            LotterySixPlugin.getInstance().getPlayerPreferenceManager().getLotteryPlayer(uuid).setPreference(preferenceKey, value);
                            sender.sendMessage(LotterySixPlugin.getInstance().messagePreferenceUpdated);
                        }
                    }
                } else {
                    sender.sendMessage(LotterySixPlugin.getInstance().messageInvalidUsage);
                }
            } else {
                sender.sendMessage(LotterySixPlugin.getInstance().messageNoPermission);
            }
            return true;
        } else if (args[0].equalsIgnoreCase("settopprizefund")) {
            if (sender.hasPermission("lotterysix.settopprizefund")) {
                if (args.length > 1) {
                    try {
                        long amount = Long.parseLong(args[1]);
                        PlayableLotterySixGame game = LotterySixPlugin.getInstance().getCurrentGame();
                        if (game == null) {
                            sender.sendMessage(LotterySixPlugin.getInstance().messageNoGameRunning);
                        } else {
                            game.setLowestTopPlacesPrize(amount);
                            sender.sendMessage(LotterySixPlugin.getInstance().messageGameSettingsUpdated);
                        }
                    } catch (NumberFormatException e) {
                        sender.sendMessage(LotterySixPlugin.getInstance().messageInvalidUsage);
                    }
                } else {
                    sender.sendMessage(LotterySixPlugin.getInstance().messageInvalidUsage);
                }
            } else {
                sender.sendMessage(LotterySixPlugin.getInstance().messageNoPermission);
            }
            return true;
        } else if (args[0].equalsIgnoreCase("setdrawtime")) {
            if (sender.hasPermission("lotterysix.setdrawtime")) {
                if (args.length > 1) {
                    try {
                        long time = Long.parseLong(args[1]);
                        PlayableLotterySixGame game = LotterySixPlugin.getInstance().getCurrentGame();
                        if (game == null) {
                            sender.sendMessage(LotterySixPlugin.getInstance().messageNoGameRunning);
                        } else {
                            game.setScheduledDateTime(time);
                            sender.sendMessage(LotterySixPlugin.getInstance().messageGameSettingsUpdated);
                        }
                    } catch (NumberFormatException e) {
                        sender.sendMessage(LotterySixPlugin.getInstance().messageInvalidUsage);
                    }
                } else {
                    sender.sendMessage(LotterySixPlugin.getInstance().messageInvalidUsage);
                }
            } else {
                sender.sendMessage(LotterySixPlugin.getInstance().messageNoPermission);
            }
            return true;
        } else if (args[0].equalsIgnoreCase("admininfo")) {
            if (sender.hasPermission("lotterysix.admininfo")) {
                if (args.length > 1) {
                    OfflinePlayer player;
                    try {
                        player = Bukkit.getOfflinePlayer(UUID.fromString(args[1]));
                    } catch (IllegalArgumentException e) {
                        player = Bukkit.getOfflinePlayer(args[1]);
                    }
                    int maxPastGames = 1;
                    if (args.length > 2) {
                        try {
                            maxPastGames = Integer.parseInt(args[2]);
                        } catch (NumberFormatException ignore) {
                        }
                    }
                    Debug.debugLotteryPlayer(sender, player, maxPastGames);
                } else {
                    sender.sendMessage(LotterySixPlugin.getInstance().messageInvalidUsage);
                }
            } else {
                sender.sendMessage(LotterySixPlugin.getInstance().messageNoPermission);
            }
            return true;
        } else if (args[0].equalsIgnoreCase("pendingtransaction")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                LotteryPlayer lotteryPlayer = LotterySixPlugin.getInstance().getPlayerPreferenceManager().getLotteryPlayer(player.getUniqueId());
                Long money = lotteryPlayer.updateStats(PlayerStatsKey.PENDING_TRANSACTION, long.class, i -> 0L);
                if (money != null && money > 0) {
                    player.sendMessage(LotterySixPlugin.getInstance().messagePendingClaimed.replace("{Money}", StringUtils.formatComma(money)));
                    LotterySixPlugin.giveMoneyNow(player.getUniqueId(), money);
                }
            } else {
                sender.sendMessage(LotterySixPlugin.getInstance().messageNoConsole);
            }
            return true;
        }

        sender.sendMessage(ChatColorUtils.translateAlternateColorCodes('&', Bukkit.spigot().getConfig().getString("messages.unknown-command")));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> tab = new LinkedList<>();

        switch (args.length) {
            case 0:
                if (sender.hasPermission("lotterysix.reload")) {
                    tab.add("reload");
                }
                if (sender.hasPermission("lotterysix.update")) {
                    tab.add("update");
                }
                if (sender.hasPermission("lotterysix.play")) {
                    tab.add("play");
                }
                if (sender.hasPermission("lotterysix.start")) {
                    tab.add("start");
                }
                if (sender.hasPermission("lotterysix.run")) {
                    tab.add("run");
                }
                if (sender.hasPermission("lotterysix.cancel")) {
                    tab.add("cancel");
                }
                if (sender.hasPermission("lotterysix.preference")) {
                    tab.add("preference");
                }
                if (sender.hasPermission("lotterysix.settopprizefund")) {
                    tab.add("settopprizefund");
                }
                if (sender.hasPermission("lotterysix.setdrawtime")) {
                    tab.add("setdrawtime");
                }
                return tab;
            case 1:
                if (sender.hasPermission("lotterysix.reload")) {
                    if ("reload".startsWith(args[0].toLowerCase())) {
                        tab.add("reload");
                    }
                }
                if (sender.hasPermission("lotterysix.update")) {
                    if ("update".startsWith(args[0].toLowerCase())) {
                        tab.add("update");
                    }
                }
                if (sender.hasPermission("lotterysix.play")) {
                    if ("play".startsWith(args[0].toLowerCase())) {
                        tab.add("play");
                    }
                }
                if (sender.hasPermission("lotterysix.start")) {
                    if ("start".startsWith(args[0].toLowerCase())) {
                        tab.add("start");
                    }
                }
                if (sender.hasPermission("lotterysix.run")) {
                    if ("run".startsWith(args[0].toLowerCase())) {
                        tab.add("run");
                    }
                }
                if (sender.hasPermission("lotterysix.cancel")) {
                    if ("cancel".startsWith(args[0].toLowerCase())) {
                        tab.add("cancel");
                    }
                }
                if (sender.hasPermission("lotterysix.preference")) {
                    if ("preference".startsWith(args[0].toLowerCase())) {
                        tab.add("preference");
                    }
                }
                if (sender.hasPermission("lotterysix.settopprizefund")) {
                    if ("settopprizefund".startsWith(args[0].toLowerCase())) {
                        tab.add("settopprizefund");
                    }
                }
                if (sender.hasPermission("lotterysix.setdrawtime")) {
                    if ("setdrawtime".startsWith(args[0].toLowerCase())) {
                        tab.add("setdrawtime");
                    }
                }
                return tab;
            case 2:
                if (sender.hasPermission("lotterysix.preference")) {
                    if ("preference".equalsIgnoreCase(args[0])) {
                        for (PlayerPreferenceKey key : PlayerPreferenceKey.values()) {
                            String name = key.name().toLowerCase();
                            if (name.startsWith(args[1].toLowerCase())) {
                                tab.add(name);
                            }
                        }
                    }
                }
                if (sender.hasPermission("lotterysix.settopprizefund")) {
                    if ("settopprizefund".equalsIgnoreCase(args[0])) {
                        long max = LotteryUtils.calculatePrice(BetNumbersType.MULTIPLE, LotterySixPlugin.getInstance().numberOfChoices, 0, LotterySixPlugin.getInstance().pricePerBet);
                        long high = Math.round(max / 0.5);
                        long low = Math.round(high * 0.125);
                        String highStr = LotteryUtils.oneSignificantFigure(high);
                        String lowStr = LotteryUtils.oneSignificantFigure(low);
                        if (highStr.startsWith(args[1].toLowerCase())) {
                            tab.add(highStr);
                        }
                        if (lowStr.startsWith(args[1].toLowerCase())) {
                            tab.add(lowStr);
                        }
                    }
                }
                if (sender.hasPermission("lotterysix.setdrawtime")) {
                    if ("setdrawtime".equalsIgnoreCase(args[0])) {
                        Cron cron = LotterySixPlugin.getInstance().runInterval;
                        if (cron != null) {
                            long duration = LotterySixPlugin.getInstance().betsAcceptDuration;
                            ZonedDateTime dateTime;
                            if (duration < 0) {
                                dateTime = CronUtils.getNextExecution(cron, CronUtils.getNow(LotterySixPlugin.getInstance().timezone));
                            } else {
                                dateTime = CronUtils.getLastExecution(cron, CronUtils.getNow(LotterySixPlugin.getInstance().timezone));
                            }
                            if (dateTime != null) {
                                long time = dateTime.toInstant().toEpochMilli() + duration;
                                if (Long.toString(time).startsWith(args[1].toLowerCase())) {
                                    tab.add(Long.toString(time));
                                }
                            }
                        }
                    }
                }
                return tab;
            case 3:
                if (sender.hasPermission("lotterysix.preference")) {
                    if ("preference".equalsIgnoreCase(args[0])) {
                        PlayerPreferenceKey key = PlayerPreferenceKey.fromKey(args[1]);
                        if (key != null) {
                            for (String value : key.getSuggestedValues()) {
                                if (value.toLowerCase().startsWith(args[2].toLowerCase())) {
                                    tab.add(value);
                                }
                            }
                        }
                    }
                }
                return tab;
            case 4:
                if (sender.hasPermission("lotterysix.preference.others")) {
                    if ("preference".equalsIgnoreCase(args[0])) {
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            if (player.getName().toLowerCase().startsWith(args[3].toLowerCase())) {
                                tab.add(player.getName());
                            }
                        }
                    }
                }
                return tab;
            default:
                return tab;
        }
    }

}
