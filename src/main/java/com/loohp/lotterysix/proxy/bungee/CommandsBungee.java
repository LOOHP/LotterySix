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

package com.loohp.lotterysix.proxy.bungee;

import com.cronutils.model.Cron;
import com.loohp.lotterysix.game.lottery.PlayableLotterySixGame;
import com.loohp.lotterysix.game.player.LotteryPlayer;
import com.loohp.lotterysix.game.objects.PlayerPreferenceKey;
import com.loohp.lotterysix.game.objects.PlayerStatsKey;
import com.loohp.lotterysix.game.objects.WinningNumbers;
import com.loohp.lotterysix.game.objects.betnumbers.BetNumbersType;
import com.loohp.lotterysix.utils.ChatColorUtils;
import com.loohp.lotterysix.utils.CronUtils;
import com.loohp.lotterysix.utils.LotteryUtils;
import com.loohp.lotterysix.utils.StringUtils;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class CommandsBungee extends Command implements TabExecutor {

    public CommandsBungee() {
        super("lotterysix", null, "lottery", "ls");
    }

    @SuppressWarnings("deprecation")
    @Override
    public void execute(CommandSender sender, String[] args) {
        ProxyServer.getInstance().getScheduler().runAsync(LotterySixBungee.plugin, () -> {
            if (args.length == 0) {
                sender.sendMessage(ChatColor.RED + "LotterySix written by LOOHP!");
                sender.sendMessage(ChatColor.GOLD + "You are running LotterySix version: " + LotterySixBungee.plugin.getDescription().getVersion());
                return;
            }

            if (args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("lotterysix.reload")) {
                    LotterySixBungee.getInstance().reloadConfig();
                    LotterySixBungee.getPluginMessageHandler().reloadConfig();
                    sender.sendMessage(LotterySixBungee.getInstance().messageReloaded);
                } else {
                    sender.sendMessage(LotterySixBungee.getInstance().messageNoPermission);
                }
                return;
            } else if (args[0].equalsIgnoreCase("update")) {
                if (sender.hasPermission("lotterysix.update")) {
                    if (sender instanceof ProxiedPlayer) {
                        LotterySixBungee.getPluginMessageHandler().updater((ProxiedPlayer) sender);
                    } else {
                        sender.sendMessage(ChatColor.RED + "Please execute this command on the backend console or as a player.");
                    }
                } else {
                    sender.sendMessage(LotterySixBungee.getInstance().messageNoPermission);
                }
                return;
            } else if (args[0].equalsIgnoreCase("play")) {
                if (sender.hasPermission("lotterysix.play")) {
                    if (LotterySixBungee.getInstance().isGameLocked()) {
                        sender.sendMessage(LotterySixBungee.getInstance().messageGameLocked);
                        return;
                    }
                    if (sender instanceof ProxiedPlayer) {
                        LotterySixBungee.getPluginMessageHandler().openPlayMenu((ProxiedPlayer) sender);
                    } else {
                        sender.sendMessage(LotterySixBungee.getInstance().messageNoConsole);
                    }
                } else {
                    sender.sendMessage(LotterySixBungee.getInstance().messageNoPermission);
                }
                return;
            } else if (args[0].equalsIgnoreCase("start")) {
                if (sender.hasPermission("lotterysix.start")) {
                    if (LotterySixBungee.getInstance().isGameLocked()) {
                        sender.sendMessage(LotterySixBungee.getInstance().messageGameLocked);
                        return;
                    }
                    if (LotterySixBungee.getInstance().getCurrentGame() == null) {
                        if (args.length > 1) {
                            try {
                                LotterySixBungee.getInstance().startNewGame(Long.parseLong(args[1]));
                                sender.sendMessage(LotterySixBungee.getInstance().messageGameStarted);
                            } catch (NumberFormatException e) {
                                sender.sendMessage(LotterySixBungee.getInstance().messageInvalidUsage);
                            }
                        } else {
                            LotterySixBungee.getInstance().startNewGame();
                            sender.sendMessage(LotterySixBungee.getInstance().messageGameStarted);
                        }
                    } else {
                        sender.sendMessage(LotterySixBungee.getInstance().messageGameAlreadyRunning);
                    }
                } else {
                    sender.sendMessage(LotterySixBungee.getInstance().messageNoPermission);
                }
                return;
            } else if (args[0].equalsIgnoreCase("run")) {
                if (sender.hasPermission("lotterysix.run")) {
                    if (LotterySixBungee.getInstance().isGameLocked()) {
                        sender.sendMessage(LotterySixBungee.getInstance().messageGameLocked);
                        return;
                    }
                    if (LotterySixBungee.getInstance().getCurrentGame() == null) {
                        sender.sendMessage(LotterySixBungee.getInstance().messageNoGameRunning);
                    } else {
                        if (args.length > 1) {
                            if (sender.hasPermission("lotterysix.run.setnumbers")) {
                                WinningNumbers winningNumbers = WinningNumbers.fromString(String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
                                if (winningNumbers == null) {
                                    sender.sendMessage(LotterySixBungee.getInstance().messageInvalidUsage);
                                    return;
                                }
                                LotterySixBungee.getInstance().setNextWinningNumbers(winningNumbers);
                            } else {
                                sender.sendMessage(LotterySixBungee.getInstance().messageNoPermission);
                                return;
                            }
                        }
                        LotterySixBungee.getInstance().getCurrentGame().setScheduledDateTime(System.currentTimeMillis());
                    }
                } else {
                    sender.sendMessage(LotterySixBungee.getInstance().messageNoPermission);
                }
                return;
            } else if (args[0].equalsIgnoreCase("cancel")) {
                if (sender.hasPermission("lotterysix.cancel")) {
                    if (LotterySixBungee.getInstance().isGameLocked()) {
                        sender.sendMessage(LotterySixBungee.getInstance().messageGameLocked);
                        return;
                    }
                    if (LotterySixBungee.getInstance().getCurrentGame() == null) {
                        sender.sendMessage(LotterySixBungee.getInstance().messageNoGameRunning);
                    } else {
                        LotterySixBungee.getInstance().cancelCurrentGame();
                    }
                } else {
                    sender.sendMessage(LotterySixBungee.getInstance().messageNoPermission);
                }
                return;
            } else if (args[0].equalsIgnoreCase("preference")) {
                if (sender.hasPermission("lotterysix.preference")) {
                    if (args.length > 2) {
                        UUID uuid;
                        if (sender instanceof ProxiedPlayer) {
                            if (sender.hasPermission("lotterysix.preference.others")) {
                                if (args.length > 3) {
                                    ProxiedPlayer player = ProxyServer.getInstance().getPlayer(args[3]);
                                    if (player == null) {
                                        sender.sendMessage(LotterySixBungee.getInstance().messagePlayerNotFound);
                                        return;
                                    }
                                    uuid = player.getUniqueId();
                                } else {
                                    uuid = ((ProxiedPlayer) sender).getUniqueId();
                                }
                            } else {
                                uuid = ((ProxiedPlayer) sender).getUniqueId();
                            }
                        } else {
                            if (args.length > 3) {
                                ProxiedPlayer player = ProxyServer.getInstance().getPlayer(args[3]);
                                if (player == null) {
                                    sender.sendMessage(LotterySixBungee.getInstance().messagePlayerNotFound);
                                    return;
                                }
                                uuid = player.getUniqueId();
                            } else {
                                sender.sendMessage(LotterySixBungee.getInstance().messageNoConsole);
                                return;
                            }
                        }
                        PlayerPreferenceKey preferenceKey = PlayerPreferenceKey.fromKey(args[1]);
                        if (preferenceKey == null) {
                            sender.sendMessage(LotterySixBungee.getInstance().messageInvalidUsage);
                        } else {
                            String valueStr = args[2];
                            Object value = preferenceKey.getReader(valueStr);
                            if (value == null) {
                                sender.sendMessage(LotterySixBungee.getInstance().messageInvalidUsage);
                            } else {
                                LotterySixBungee.getInstance().getPlayerPreferenceManager().getLotteryPlayer(uuid).setPreference(preferenceKey, value);
                                sender.sendMessage(LotterySixBungee.getInstance().messagePreferenceUpdated);
                            }
                        }
                    } else {
                        sender.sendMessage(LotterySixBungee.getInstance().messageInvalidUsage);
                    }
                } else {
                    sender.sendMessage(LotterySixBungee.getInstance().messageNoPermission);
                }
                return;
            } else if (args[0].equalsIgnoreCase("settopprizefund")) {
                if (sender.hasPermission("lotterysix.settopprizefund")) {
                    if (args.length > 1) {
                        try {
                            long amount = Long.parseLong(args[1]);
                            PlayableLotterySixGame game = LotterySixBungee.getInstance().getCurrentGame();
                            if (game == null) {
                                sender.sendMessage(LotterySixBungee.getInstance().messageNoGameRunning);
                            } else {
                                game.setLowestTopPlacesPrize(amount);
                                sender.sendMessage(LotterySixBungee.getInstance().messageGameSettingsUpdated);
                            }
                        } catch (NumberFormatException e) {
                            sender.sendMessage(LotterySixBungee.getInstance().messageInvalidUsage);
                        }
                    } else {
                        sender.sendMessage(LotterySixBungee.getInstance().messageInvalidUsage);
                    }
                } else {
                    sender.sendMessage(LotterySixBungee.getInstance().messageNoPermission);
                }
                return;
            } else if (args[0].equalsIgnoreCase("setdrawtime")) {
                if (sender.hasPermission("lotterysix.setdrawtime")) {
                    if (args.length > 1) {
                        try {
                            long time = Long.parseLong(args[1]);
                            PlayableLotterySixGame game = LotterySixBungee.getInstance().getCurrentGame();
                            if (game == null) {
                                sender.sendMessage(LotterySixBungee.getInstance().messageNoGameRunning);
                            } else {
                                game.setScheduledDateTime(time);
                                sender.sendMessage(LotterySixBungee.getInstance().messageGameSettingsUpdated);
                            }
                        } catch (NumberFormatException e) {
                            sender.sendMessage(LotterySixBungee.getInstance().messageInvalidUsage);
                        }
                    } else {
                        sender.sendMessage(LotterySixBungee.getInstance().messageInvalidUsage);
                    }
                } else {
                    sender.sendMessage(LotterySixBungee.getInstance().messageNoPermission);
                }
                return;
            } else if (args[0].equalsIgnoreCase("setspecialname")) {
                if (sender.hasPermission("lotterysix.setspecialname")) {
                    if (args.length > 1) {
                        try {
                            String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                            PlayableLotterySixGame game = LotterySixBungee.getInstance().getCurrentGame();
                            if (game == null) {
                                sender.sendMessage(LotterySixBungee.getInstance().messageNoGameRunning);
                            } else {
                                if (name.equalsIgnoreCase("clear")) {
                                    game.setSpecialName(null);
                                } else {
                                    game.setSpecialName(ChatColorUtils.translateAlternateColorCodes('&', name));
                                }
                                sender.sendMessage(LotterySixBungee.getInstance().messageGameSettingsUpdated);
                            }
                        } catch (NumberFormatException e) {
                            sender.sendMessage(LotterySixBungee.getInstance().messageInvalidUsage);
                        }
                    } else {
                        sender.sendMessage(LotterySixBungee.getInstance().messageInvalidUsage);
                    }
                } else {
                    sender.sendMessage(LotterySixBungee.getInstance().messageNoPermission);
                }
                return;
            } else if (args[0].equalsIgnoreCase("setcarryoverfund")) {
                if (sender.hasPermission("lotterysix.setcarryoverfund")) {
                    if (args.length > 1) {
                        try {
                            long amount = Long.parseLong(args[1]);
                            PlayableLotterySixGame game = LotterySixBungee.getInstance().getCurrentGame();
                            if (game == null) {
                                sender.sendMessage(LotterySixBungee.getInstance().messageNoGameRunning);
                            } else {
                                game.setCarryOverFund(amount);
                                sender.sendMessage(LotterySixBungee.getInstance().messageGameSettingsUpdated);
                            }
                        } catch (NumberFormatException e) {
                            sender.sendMessage(LotterySixBungee.getInstance().messageInvalidUsage);
                        }
                    } else {
                        sender.sendMessage(LotterySixBungee.getInstance().messageInvalidUsage);
                    }
                } else {
                    sender.sendMessage(LotterySixBungee.getInstance().messageNoPermission);
                }
                return;
            } else if (args[0].equalsIgnoreCase("admininfo")) {
                if (sender.hasPermission("lotterysix.admininfo")) {
                    if (args.length > 1) {
                        ProxiedPlayer player;
                        try {
                            player = ProxyServer.getInstance().getPlayer(UUID.fromString(args[1]));
                        } catch (IllegalArgumentException e) {
                            player = ProxyServer.getInstance().getPlayer(args[1]);
                        }
                        if (player == null) {
                            sender.sendMessage(LotterySixBungee.getInstance().messagePlayerNotFound);
                            return;
                        }
                        int maxPastGames = 1;
                        if (args.length > 2) {
                            try {
                                maxPastGames = Integer.parseInt(args[2]);
                            } catch (NumberFormatException ignore) {
                            }
                        }
                        DebugBungee.debugLotteryPlayer(sender, player, maxPastGames);
                    } else {
                        sender.sendMessage(LotterySixBungee.getInstance().messageInvalidUsage);
                    }
                } else {
                    sender.sendMessage(LotterySixBungee.getInstance().messageNoPermission);
                }
                return;
            } else if (args[0].equalsIgnoreCase("invalidatebets")) {
                if (sender.hasPermission("lotterysix.invalidatebets")) {
                    if (args.length > 1) {
                        if (LotterySixBungee.getInstance().getCurrentGame() == null) {
                            sender.sendMessage(LotterySixBungee.getInstance().messageNoGameRunning);
                        } else {
                            UUID uuid;
                            try {
                                uuid = UUID.fromString(args[1]);
                            } catch (IllegalArgumentException e) {
                                uuid = ProxyServer.getInstance().getPlayer(args[1]).getUniqueId();
                            }
                            UUID finalUuid = uuid;
                            LotterySixBungee.getInstance().getCurrentGame().invalidateBetsIf(bet -> bet.getPlayer().equals(finalUuid));
                            sender.sendMessage(LotterySixBungee.getInstance().messageGameSettingsUpdated);
                        }
                    } else {
                        sender.sendMessage(LotterySixBungee.getInstance().messageInvalidUsage);
                    }
                }
                return;
            } else if (args[0].equalsIgnoreCase("pendingtransaction")) {
                if (sender instanceof ProxiedPlayer) {
                    ProxiedPlayer player = (ProxiedPlayer) sender;
                    if (player.getServer() != null) {
                        LotteryPlayer lotteryPlayer = LotterySixBungee.getInstance().getPlayerPreferenceManager().getLotteryPlayer(player.getUniqueId());
                        Long money = lotteryPlayer.updateStats(PlayerStatsKey.PENDING_TRANSACTION, long.class, i -> 0L);
                        if (money != null && money > 0) {
                            player.sendMessage(LotterySixBungee.getInstance().messagePendingClaimed.replace("{Money}", StringUtils.formatComma(money)));
                            LotterySixBungee.getPluginMessageHandler().giveMoney(player, money);
                        }
                    }
                } else {
                    sender.sendMessage(LotterySixBungee.getInstance().messageNoConsole);
                }
                return;
            }
        });
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
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
                if (sender.hasPermission("lotterysix.setspecialname")) {
                    tab.add("setspecialname");
                }
                if (sender.hasPermission("lotterysix.setcarryoverfund")) {
                    tab.add("setcarryoverfund");
                }
                if (sender.hasPermission("lotterysix.invalidatebets")) {
                    tab.add("invalidatebets");
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
                if (sender.hasPermission("lotterysix.setspecialname")) {
                    if ("setspecialname".startsWith(args[0].toLowerCase())) {
                        tab.add("setspecialname");
                    }
                }
                if (sender.hasPermission("lotterysix.setcarryoverfund")) {
                    if ("setcarryoverfund".startsWith(args[0].toLowerCase())) {
                        tab.add("setcarryoverfund");
                    }
                }
                if (sender.hasPermission("lotterysix.invalidatebets")) {
                    if ("invalidatebets".startsWith(args[0].toLowerCase())) {
                        tab.add("invalidatebets");
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
                        long max = LotteryUtils.calculatePrice(LotterySixBungee.getInstance().numberOfChoices, 0, LotterySixBungee.getInstance().pricePerBet);
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
                        Cron cron = LotterySixBungee.getInstance().runInterval;
                        if (cron != null) {
                            long duration = LotterySixBungee.getInstance().betsAcceptDuration;
                            ZonedDateTime dateTime;
                            if (duration < 0) {
                                dateTime = CronUtils.getNextExecution(cron, CronUtils.getNow(LotterySixBungee.getInstance().timezone));
                            } else {
                                dateTime = CronUtils.getLastExecution(cron, CronUtils.getNow(LotterySixBungee.getInstance().timezone));
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
                if (sender.hasPermission("lotterysix.setspecialname")) {
                    if ("setspecialname".equalsIgnoreCase(args[0])) {
                        if ("clear".startsWith(args[1].toLowerCase())) {
                            tab.add("clear");
                        }
                    }
                }
                if (sender.hasPermission("lotterysix.setcarryoverfund")) {
                    if ("setcarryoverfund".equalsIgnoreCase(args[0])) {
                        String str = (LotterySixBungee.getInstance().lowestTopPlacesPrize * 10) + "";
                        if (str.startsWith(args[1].toLowerCase())) {
                            tab.add(str);
                        }
                        if ("0".startsWith(args[1].toLowerCase())) {
                            tab.add("0");
                        }
                    }
                }
                if (sender.hasPermission("lotterysix.invalidatebets")) {
                    if ("invalidatebets".equalsIgnoreCase(args[0])) {
                        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
                            if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                                tab.add(player.getName());
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
                        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
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
