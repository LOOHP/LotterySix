/*
 * This file is part of LotterySix.
 *
 * Copyright (C) 2024. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2024. Contributors
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

package com.loohp.lotterysix.proxy.velocity;

import com.cronutils.model.Cron;
import com.loohp.lotterysix.game.lottery.PlayableLotterySixGame;
import com.loohp.lotterysix.game.objects.PlayerBets;
import com.loohp.lotterysix.game.objects.PlayerPreferenceKey;
import com.loohp.lotterysix.game.objects.PlayerStatsKey;
import com.loohp.lotterysix.game.objects.WinningNumbers;
import com.loohp.lotterysix.game.player.LotteryPlayer;
import com.loohp.lotterysix.proxy.bungee.LotterySixBungee;
import com.loohp.lotterysix.proxy.velocity.utils.PlayerUtilsVelocity;
import com.loohp.lotterysix.utils.ChatColorUtils;
import com.loohp.lotterysix.utils.CronUtils;
import com.loohp.lotterysix.utils.LotteryUtils;
import com.loohp.lotterysix.utils.StringUtils;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.bukkit.Bukkit;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CommandsVelocity implements SimpleCommand {

    @Override
    public void execute(final Invocation invocation) {
        CommandSource sender = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0) {
            sender.sendMessage(m(TextColor.RED + "LotterySix written by LOOHP!"));
            sender.sendMessage(m(TextColor.GOLD + "You are running LotterySix version: " + LotterySixVelocity.plugin.getDescription().getVersion()));
            return;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (sender.hasPermission("lotterysix.reload")) {
                LotterySixVelocity.getInstance().reloadConfig();
                LotterySixVelocity.getPluginMessageHandler().reloadConfig();
                sender.sendMessage(m(LotterySixVelocity.getInstance().messageReloaded));
            } else {
                sender.sendMessage(m(LotterySixVelocity.getInstance().messageNoPermission));
            }
            return;
        } else if (args[0].equalsIgnoreCase("update")) {
            if (sender.hasPermission("lotterysix.update")) {
                if (sender instanceof Player) {
                    LotterySixVelocity.getPluginMessageHandler().updater((Player) sender);
                } else {
                    sender.sendMessage(m(TextColor.RED + "Please execute this command on the backend console or as a player."));
                }
            } else {
                sender.sendMessage(m(LotterySixVelocity.getInstance().messageNoPermission));
            }
            return;
        } else if (args[0].equalsIgnoreCase("play")) {
            if (sender.hasPermission("lotterysix.play")) {
                if (LotterySixVelocity.getInstance().isGameLocked()) {
                    sender.sendMessage(m(LotterySixVelocity.getInstance().messageGameLocked));
                    return;
                }
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    CompletableFuture<Boolean> future;
                    if (args.length > 1) {
                        String input = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                        PlayableLotterySixGame game = LotterySixVelocity.getInstance().getCurrentGame();
                        if (game != null) {
                            future = LotterySixVelocity.getPluginMessageHandler().openPlayMenu(player, input);
                        } else {
                            future = null;
                            player.sendMessage(m(LotterySixVelocity.getInstance().messageNoGameRunning));
                        }
                    } else {
                        future = LotterySixVelocity.getPluginMessageHandler().openPlayMenu(player);
                    }
                    if (future != null) {
                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                if (!future.getNow(false)) {
                                    player.sendMessage(m(LotterySixVelocity.getInstance().messageLotterySixNotOnCurrentBackend));
                                }
                            }
                        }, 2000);
                    }
                } else {
                    sender.sendMessage(m(LotterySixVelocity.getInstance().messageNoConsole));
                }
            } else {
                sender.sendMessage(m(LotterySixVelocity.getInstance().messageNoPermission));
            }
            return;
        } else if (args[0].equalsIgnoreCase("balance")) {
            if (sender.hasPermission("lotterysix.balance")) {
                if (LotterySixVelocity.getInstance().isGameLocked()) {
                    sender.sendMessage(m(LotterySixVelocity.getInstance().messageGameLocked));
                    return;
                }
                if (args.length > 2) {
                    UUID uuid = PlayerUtilsVelocity.getPlayerUUID(args[1]);
                    LotteryPlayer lotteryPlayer = LotterySixVelocity.getInstance().getLotteryPlayerManager().getLotteryPlayer(uuid);
                    long money = lotteryPlayer.getStats(PlayerStatsKey.ACCOUNT_BALANCE, long.class);
                    if (args[2].equalsIgnoreCase("get")) {
                        sender.sendMessage(m(LotterySixVelocity.getInstance().messagePlayerBalance.replace("{Player}", args[1]).replace("{Balance}", StringUtils.formatComma(money))));
                    } else if (args.length > 3) {
                        try {
                            long newMoney = Long.parseLong(args[3]);
                            if (args[2].equalsIgnoreCase("set")) {
                                lotteryPlayer.setStats(PlayerStatsKey.ACCOUNT_BALANCE, newMoney);
                                LotterySixVelocity.getPluginMessageHandler().syncPlayerData(lotteryPlayer);
                                sender.sendMessage(m(LotterySixVelocity.getInstance().messagePlayerBalance.replace("{Player}", args[1]).replace("{Balance}", StringUtils.formatComma(newMoney))));
                            } else if (args[2].equalsIgnoreCase("add")) {
                                lotteryPlayer.updateStats(PlayerStatsKey.ACCOUNT_BALANCE, long.class, i -> i + newMoney);
                                long updated = lotteryPlayer.getStats(PlayerStatsKey.ACCOUNT_BALANCE, long.class);
                                LotterySixVelocity.getPluginMessageHandler().syncPlayerData(lotteryPlayer);
                                sender.sendMessage(m(LotterySixVelocity.getInstance().messagePlayerBalance.replace("{Player}", args[1]).replace("{Balance}", StringUtils.formatComma(updated))));
                            } else {
                                sender.sendMessage(m(LotterySixVelocity.getInstance().messageInvalidUsage));
                            }
                        } catch (NumberFormatException e) {
                            sender.sendMessage(m(LotterySixVelocity.getInstance().messageInvalidNumber));
                            return;
                        }
                    } else {
                        sender.sendMessage(m(LotterySixVelocity.getInstance().messageInvalidUsage));
                    }
                } else {
                    sender.sendMessage(m(LotterySixVelocity.getInstance().messageInvalidUsage));
                }
            } else {
                sender.sendMessage(m(LotterySixVelocity.getInstance().messageNoPermission));
            }
            return;
        } else if (args[0].equalsIgnoreCase("start")) {
            if (sender.hasPermission("lotterysix.start")) {
                if (LotterySixVelocity.getInstance().isGameLocked()) {
                    sender.sendMessage(m(LotterySixVelocity.getInstance().messageGameLocked));
                    return;
                }
                if (LotterySixVelocity.getInstance().getCurrentGame() == null) {
                    if (args.length > 1) {
                        try {
                            LotterySixVelocity.getInstance().startNewGame(Long.parseLong(args[1]));
                            sender.sendMessage(m(LotterySixVelocity.getInstance().messageGameStarted));
                        } catch (NumberFormatException e) {
                            sender.sendMessage(m(LotterySixVelocity.getInstance().messageInvalidUsage));
                        }
                    } else {
                        LotterySixVelocity.getInstance().startNewGame();
                        sender.sendMessage(m(LotterySixVelocity.getInstance().messageGameStarted));
                    }
                } else {
                    sender.sendMessage(m(LotterySixVelocity.getInstance().messageGameAlreadyRunning));
                }
            } else {
                sender.sendMessage(m(LotterySixVelocity.getInstance().messageNoPermission));
            }
            return;
        } else if (args[0].equalsIgnoreCase("run")) {
            if (sender.hasPermission("lotterysix.run")) {
                if (LotterySixVelocity.getInstance().isGameLocked()) {
                    sender.sendMessage(m(LotterySixVelocity.getInstance().messageGameLocked));
                    return;
                }
                if (LotterySixVelocity.getInstance().getCurrentGame() == null) {
                    sender.sendMessage(m(LotterySixVelocity.getInstance().messageNoGameRunning));
                } else {
                    if (args.length > 1) {
                        if (sender.hasPermission("lotterysix.run.setnumbers")) {
                            WinningNumbers winningNumbers = WinningNumbers.fromString(String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
                            if (winningNumbers == null) {
                                sender.sendMessage(m(LotterySixVelocity.getInstance().messageInvalidUsage));
                                return;
                            }
                            LotterySixVelocity.getInstance().setNextWinningNumbers(winningNumbers);
                        } else {
                            sender.sendMessage(m(LotterySixVelocity.getInstance().messageNoPermission));
                            return;
                        }
                    }
                    long time = System.currentTimeMillis();
                    LotterySixVelocity.getInstance().getCurrentGame().setDatetime(time, LotterySixVelocity.getInstance().dateToGameNumber(time));
                }
            } else {
                sender.sendMessage(m(LotterySixVelocity.getInstance().messageNoPermission));
            }
            return;
        } else if (args[0].equalsIgnoreCase("cancel")) {
            if (sender.hasPermission("lotterysix.cancel")) {
                if (LotterySixVelocity.getInstance().isGameLocked()) {
                    sender.sendMessage(m(LotterySixVelocity.getInstance().messageGameLocked));
                    return;
                }
                if (LotterySixVelocity.getInstance().getCurrentGame() == null) {
                    sender.sendMessage(m(LotterySixVelocity.getInstance().messageNoGameRunning));
                } else {
                    LotterySixVelocity.getInstance().cancelCurrentGame();
                }
            } else {
                sender.sendMessage(m(LotterySixVelocity.getInstance().messageNoPermission));
            }
            return;
        } else if (args[0].equalsIgnoreCase("preference")) {
            if (sender.hasPermission("lotterysix.preference")) {
                if (args.length > 2) {
                    UUID uuid;
                    if (sender instanceof Player) {
                        if (sender.hasPermission("lotterysix.preference.others")) {
                            if (args.length > 3) {
                                uuid = PlayerUtilsVelocity.getPlayerUUID(args[3]);
                                if (uuid == null) {
                                    sender.sendMessage(m(LotterySixVelocity.getInstance().messagePlayerNotFound));
                                    return;
                                }
                            } else {
                                uuid = ((Player) sender).getUniqueId();
                            }
                        } else {
                            uuid = ((Player) sender).getUniqueId();
                        }
                    } else {
                        if (args.length > 3) {
                            uuid = PlayerUtilsVelocity.getPlayerUUID(args[3]);
                            if (uuid == null) {
                                sender.sendMessage(m(LotterySixVelocity.getInstance().messagePlayerNotFound));
                                return;
                            }
                        } else {
                            sender.sendMessage(m(LotterySixVelocity.getInstance().messageNoConsole));
                            return;
                        }
                    }
                    PlayerPreferenceKey preferenceKey = PlayerPreferenceKey.fromKey(args[1]);
                    if (preferenceKey == null) {
                        sender.sendMessage(m(LotterySixVelocity.getInstance().messageInvalidUsage));
                    } else {
                        String valueStr = args[2];
                        Object value = preferenceKey.getReader(valueStr);
                        if (value == null) {
                            sender.sendMessage(m(LotterySixVelocity.getInstance().messageInvalidUsage));
                        } else {
                            LotterySixVelocity.getInstance().getLotteryPlayerManager().getLotteryPlayer(uuid).setPreference(preferenceKey, value);
                            sender.sendMessage(m(LotterySixVelocity.getInstance().messagePreferenceUpdated));
                        }
                    }
                } else {
                    sender.sendMessage(m(LotterySixVelocity.getInstance().messageInvalidUsage));
                }
            } else {
                sender.sendMessage(m(LotterySixVelocity.getInstance().messageNoPermission));
            }
            return;
        } else if (args[0].equalsIgnoreCase("settopprizefund")) {
            if (sender.hasPermission("lotterysix.settopprizefund")) {
                if (args.length > 1) {
                    try {
                        long amount = Long.parseLong(args[1]);
                        PlayableLotterySixGame game = LotterySixVelocity.getInstance().getCurrentGame();
                        if (game == null) {
                            sender.sendMessage(m(LotterySixVelocity.getInstance().messageNoGameRunning));
                        } else {
                            game.setLowestTopPlacesPrize(amount);
                            sender.sendMessage(m(LotterySixVelocity.getInstance().messageGameSettingsUpdated));
                        }
                    } catch (NumberFormatException e) {
                        sender.sendMessage(m(LotterySixVelocity.getInstance().messageInvalidUsage));
                    }
                } else {
                    sender.sendMessage(m(LotterySixVelocity.getInstance().messageInvalidUsage));
                }
            } else {
                sender.sendMessage(m(LotterySixVelocity.getInstance().messageNoPermission));
            }
            return;
        } else if (args[0].equalsIgnoreCase("setdrawtime")) {
            if (sender.hasPermission("lotterysix.setdrawtime")) {
                if (args.length > 1) {
                    try {
                        long time = Long.parseLong(args[1]);
                        PlayableLotterySixGame game = LotterySixVelocity.getInstance().getCurrentGame();
                        if (game == null) {
                            sender.sendMessage(m(LotterySixVelocity.getInstance().messageNoGameRunning));
                        } else {
                            game.setDatetime(time, LotterySixVelocity.getInstance().dateToGameNumber(time));
                            sender.sendMessage(m(LotterySixVelocity.getInstance().messageGameSettingsUpdated));
                        }
                    } catch (NumberFormatException e) {
                        sender.sendMessage(m(LotterySixVelocity.getInstance().messageInvalidUsage));
                    }
                } else {
                    sender.sendMessage(m(LotterySixVelocity.getInstance().messageInvalidUsage));
                }
            } else {
                sender.sendMessage(m(LotterySixVelocity.getInstance().messageNoPermission));
            }
            return;
        } else if (args[0].equalsIgnoreCase("setspecialname")) {
            if (sender.hasPermission("lotterysix.setspecialname")) {
                if (args.length > 1) {
                    try {
                        String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                        PlayableLotterySixGame game = LotterySixVelocity.getInstance().getCurrentGame();
                        if (game == null) {
                            sender.sendMessage(m(LotterySixVelocity.getInstance().messageNoGameRunning));
                        } else {
                            if (name.equalsIgnoreCase("clear")) {
                                game.setSpecialName(null);
                            } else {
                                game.setSpecialName(ChatColorUtils.translateAlternateColorCodes('&', name));
                            }
                            sender.sendMessage(m(LotterySixVelocity.getInstance().messageGameSettingsUpdated));
                        }
                    } catch (NumberFormatException e) {
                        sender.sendMessage(m(LotterySixVelocity.getInstance().messageInvalidUsage));
                    }
                } else {
                    sender.sendMessage(m(LotterySixVelocity.getInstance().messageInvalidUsage));
                }
            } else {
                sender.sendMessage(m(LotterySixVelocity.getInstance().messageNoPermission));
            }
            return;
        } else if (args[0].equalsIgnoreCase("setcarryoverfund")) {
            if (sender.hasPermission("lotterysix.setcarryoverfund")) {
                if (args.length > 1) {
                    try {
                        long amount = Long.parseLong(args[1]);
                        PlayableLotterySixGame game = LotterySixVelocity.getInstance().getCurrentGame();
                        if (game == null) {
                            sender.sendMessage(m(LotterySixVelocity.getInstance().messageNoGameRunning));
                        } else {
                            game.setCarryOverFund(amount);
                            sender.sendMessage(m(LotterySixVelocity.getInstance().messageGameSettingsUpdated));
                        }
                    } catch (NumberFormatException e) {
                        sender.sendMessage(m(LotterySixVelocity.getInstance().messageInvalidUsage));
                    }
                } else {
                    sender.sendMessage(m(LotterySixVelocity.getInstance().messageInvalidUsage));
                }
            } else {
                sender.sendMessage(m(LotterySixVelocity.getInstance().messageNoPermission));
            }
            return;
        } else if (args[0].equalsIgnoreCase("admininfo")) {
            if (sender.hasPermission("lotterysix.admininfo")) {
                if (args.length > 1) {
                    UUID uuid;
                    try {
                        uuid = UUID.fromString(args[1]);
                    } catch (IllegalArgumentException e) {
                        uuid = PlayerUtilsVelocity.getPlayerUUID(args[1]);
                    }
                    if (uuid == null) {
                        sender.sendMessage(m(LotterySixVelocity.getInstance().messagePlayerNotFound));
                        return;
                    }
                    int maxPastGames = 1;
                    if (args.length > 2) {
                        try {
                            maxPastGames = Integer.parseInt(args[2]);
                        } catch (NumberFormatException ignore) {
                        }
                    }
                    DebugVelocity.debugLotteryPlayer(sender, PlayerUtilsVelocity.getPlayerName(uuid), uuid, maxPastGames);
                } else {
                    sender.sendMessage(m(LotterySixVelocity.getInstance().messageInvalidUsage));
                }
            } else {
                sender.sendMessage(m(LotterySixVelocity.getInstance().messageNoPermission));
            }
            return;
        } else if (args[0].equalsIgnoreCase("invalidatebets")) {
            if (sender.hasPermission("lotterysix.invalidatebets")) {
                if (args.length > 2) {
                    if (LotterySixVelocity.getInstance().getCurrentGame() == null) {
                        sender.sendMessage(m(LotterySixVelocity.getInstance().messageNoGameRunning));
                    } else {
                        if (args[1].equalsIgnoreCase("player")) {
                            if (args[2].equals("*")) {
                                LotterySixVelocity.getInstance().getCurrentGame().invalidateBetsIf(bet -> true, Boolean.parseBoolean(args[3]));
                            } else {
                                UUID uuid;
                                try {
                                    uuid = UUID.fromString(args[2]);
                                } catch (IllegalArgumentException e) {
                                    uuid = PlayerUtilsVelocity.getPlayerUUID(args[2]);
                                }
                                UUID finalUuid = uuid;
                                LotterySixVelocity.getInstance().getCurrentGame().invalidateBetsIf(bet -> bet.getPlayer().equals(finalUuid), Boolean.parseBoolean(args[3]));
                            }
                            sender.sendMessage(m(LotterySixVelocity.getInstance().messageGameSettingsUpdated));
                        } else if (args[1].equalsIgnoreCase("bet")) {
                            try {
                                UUID uuid = UUID.fromString(args[2]);
                                LotterySixVelocity.getInstance().getCurrentGame().invalidateBetsIf(bet -> bet.getBetId().equals(uuid), Boolean.parseBoolean(args[3]));
                            } catch (IllegalArgumentException e) {
                                sender.sendMessage(m(LotterySixVelocity.getInstance().messageInvalidUsage));
                            }
                        } else {
                            sender.sendMessage(m(LotterySixVelocity.getInstance().messageInvalidUsage));
                        }
                    }
                } else {
                    sender.sendMessage(m(LotterySixVelocity.getInstance().messageInvalidUsage));
                }
            }
            return;
        }
    }

    @Override
    public List<String> suggest(final Invocation invocation) {
        CommandSource sender = invocation.source();
        String[] args = invocation.arguments();
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
                if (sender.hasPermission("lotterysix.balance")) {
                    tab.add("balance");
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
                if (sender.hasPermission("lotterysix.balance")) {
                    if ("balance".startsWith(args[0].toLowerCase())) {
                        tab.add("balance");
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
                if (sender.hasPermission("lotterysix.balance")) {
                    if ("balance".equalsIgnoreCase(args[0])) {
                        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
                            if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                                tab.add(player.getName());
                            }
                        }
                    }
                }
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
                        if ("player".startsWith(args[1].toLowerCase())) {
                            tab.add("player");
                        }
                        if ("bet".startsWith(args[1].toLowerCase())) {
                            tab.add("bet");
                        }
                    }
                }
                return tab;
            case 3:
                if (sender.hasPermission("lotterysix.balance")) {
                    if ("balance".equalsIgnoreCase(args[0])) {
                        if ("get".startsWith(args[2].toLowerCase())) {
                            tab.add("get");
                        }
                        if ("set".startsWith(args[2].toLowerCase())) {
                            tab.add("set");
                        }
                        if ("add".startsWith(args[2].toLowerCase())) {
                            tab.add("add");
                        }
                    }
                }
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
                if (sender.hasPermission("lotterysix.invalidatebets")) {
                    if ("invalidatebets".equalsIgnoreCase(args[0])) {
                        if ("player".equalsIgnoreCase(args[1])) {
                            for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
                                if (player.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                                    tab.add(player.getName());
                                }
                            }
                            if ("*".startsWith(args[2].toLowerCase())) {
                                tab.add("*");
                            }
                        } else if ("bet".equalsIgnoreCase(args[1])) {
                            PlayableLotterySixGame game = LotterySixBungee.getInstance().getCurrentGame();
                            if (game == null) {
                                tab.add("<bet-id>");
                            } else {
                                try {
                                    UUID betId = UUID.fromString(args[2]);
                                    PlayerBets bet = game.getBet(betId);
                                    if (bet == null) {
                                        tab.add("<bet-id>");
                                    } else {
                                        tab.add(bet.getName() + " > " + LotterySixBungee.getInstance().betNumbersTypeNames.get(bet.getChosenNumbers().getType()));
                                    }
                                } catch (IllegalArgumentException e) {
                                    tab.add("<bet-id>");
                                }
                            }
                        }
                    }
                }
                return tab;
            case 4:
                if (sender.hasPermission("lotterysix.balance")) {
                    if ("balance".equalsIgnoreCase(args[0]) && ("set".equalsIgnoreCase(args[2]) || "add".equalsIgnoreCase(args[2]))) {
                        tab.add("<value>");
                    }
                }
                if (sender.hasPermission("lotterysix.preference.others")) {
                    if ("preference".equalsIgnoreCase(args[0])) {
                        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
                            if (player.getName().toLowerCase().startsWith(args[3].toLowerCase())) {
                                tab.add(player.getName());
                            }
                        }
                    }
                }
                if (sender.hasPermission("lotterysix.invalidatebets")) {
                    if ("invalidatebets".equalsIgnoreCase(args[0])) {
                        if ("true".startsWith(args[3].toLowerCase())) {
                            tab.add("true");
                        }
                        if ("false".startsWith(args[3].toLowerCase())) {
                            tab.add("false");
                        }
                    }
                }
                return tab;
            default:
                return tab;
        }
    }

    private static TextComponent m(String m) {
        return LegacyComponentSerializer.legacySection().deserialize(m);
    }

}
