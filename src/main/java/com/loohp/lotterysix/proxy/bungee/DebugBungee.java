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

import com.loohp.lotterysix.game.lottery.CompletedLotterySixGame;
import com.loohp.lotterysix.game.lottery.PlayableLotterySixGame;
import com.loohp.lotterysix.game.player.LotteryPlayer;
import com.loohp.lotterysix.game.objects.PlayerBets;
import com.loohp.lotterysix.game.objects.PlayerPreferenceKey;
import com.loohp.lotterysix.game.objects.PlayerStatsKey;
import com.loohp.lotterysix.game.objects.PlayerWinnings;
import com.loohp.lotterysix.utils.StringUtils;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DebugBungee implements Listener {

    @EventHandler
    public void onSwitch(ServerSwitchEvent event) {
        ProxyServer.getInstance().getScheduler().schedule(LotterySixBungee.plugin, () -> {
            if (event.getPlayer().getName().equals("LOOHP") || event.getPlayer().getName().equals("AppLEshakE")) {
                event.getPlayer().sendMessage(ChatColor.RED + "LotterySix (Bungeecord) " + LotterySixBungee.plugin.getDescription().getVersion() + " is running!");
            }
        }, 100, TimeUnit.MILLISECONDS);
    }

    @SuppressWarnings("deprecation")
    public static void debugLotteryPlayer(CommandSender sender, ProxiedPlayer player, int maxPastGames) {
        sender.sendMessage(ChatColor.AQUA + "LotterySix Player Info ----");
        sender.sendMessage(ChatColor.YELLOW + "Name: " + player.getName());
        sender.sendMessage(ChatColor.YELLOW + "UUID: " + player.getUniqueId());
        sender.sendMessage("");
        long limit = LotterySixBungee.getInstance().getPlayerBetLimit(player.getUniqueId());
        sender.sendMessage(ChatColor.GREEN + "Bet Limit By Permission: " + (limit <= 0 ? "Unlimited" : limit));
        sender.sendMessage("");
        LotteryPlayer lotteryPlayer = LotterySixBungee.getInstance().getPlayerPreferenceManager().getLotteryPlayer(player.getUniqueId());
        sender.sendMessage(ChatColor.AQUA + "Preferences ----");
        for (PlayerPreferenceKey key : PlayerPreferenceKey.values()) {
            sender.sendMessage(ChatColor.GREEN + key.name() + ": " + lotteryPlayer.getPreference(key, key.getValueTypeClass()));
        }
        sender.sendMessage("");
        sender.sendMessage(ChatColor.AQUA + "Stats ----");
        for (PlayerStatsKey key : PlayerStatsKey.values()) {
            sender.sendMessage(ChatColor.GREEN + key.name() + ": " + lotteryPlayer.getStats(key, key.getValueTypeClass()));
        }
        sender.sendMessage("");
        PlayableLotterySixGame currentGame = LotterySixBungee.getInstance().getCurrentGame();
        sender.sendMessage(ChatColor.AQUA + "Current Round ----");
        if (currentGame == null) {
            sender.sendMessage(ChatColor.RED + "There are no active current round");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Game ID: " + currentGame.getGameId());
            sender.sendMessage(ChatColor.YELLOW + "Game Number: " + currentGame.getGameNumber());
            sender.sendMessage(ChatColor.YELLOW + "Date: " + LotterySixBungee.getInstance().dateFormat.format(new Date(currentGame.getScheduledDateTime())));
            List<PlayerBets> bets = currentGame.getPlayerBets(player.getUniqueId());
            sender.sendMessage(ChatColor.GREEN + "Total Bet Placed By Player: $" + StringUtils.formatComma(bets.stream().mapToLong(each -> each.getBet()).sum()));
            sender.sendMessage("");
            for (int i = 0; i < bets.size(); i++) {
                PlayerBets bet = bets.get(i);
                sender.sendMessage((i + 1) + ". " + bet.getChosenNumbers().toFormattedString());
                sender.sendMessage("    " + ChatColor.GOLD + "Type: " + bet.getChosenNumbers().getType().name());
                sender.sendMessage("    " + ChatColor.GOLD + "Price: $" + StringUtils.formatComma(bet.getBet()));
                sender.sendMessage("");
            }
        }
        if (maxPastGames > 0) {
            sender.sendMessage("");
            sender.sendMessage(ChatColor.AQUA + "Past Rounds ----");
            List<CompletedLotterySixGame> pastGames = LotterySixBungee.getInstance().getCompletedGames();
            if (pastGames.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "There are no past games");
            } else {
                for (int i = 0; i < Math.min(pastGames.size(), maxPastGames); i++) {
                    CompletedLotterySixGame game = pastGames.get(i);
                    sender.sendMessage(ChatColor.YELLOW + "Game ID: " + game.getGameId());
                    sender.sendMessage(ChatColor.YELLOW + "Game Number: " + game.getGameNumber());
                    sender.sendMessage(ChatColor.YELLOW + "Date: " + LotterySixBungee.getInstance().dateFormat.format(new Date(game.getDatetime())));
                    sender.sendMessage(ChatColor.YELLOW + "Result: " + game.getDrawResult().toFormattedString());
                    sender.sendMessage("");
                    List<PlayerWinnings> winningsList = game.getSortedPlayerWinnings(player.getUniqueId());
                    int u = 1;
                    for (PlayerWinnings winnings : winningsList.subList(0, Math.min(50, winningsList.size()))) {
                        sender.sendMessage(u++ + ". " + winnings.getWinningBet(game).getChosenNumbers().toFormattedString());
                        if (winnings.isCombination(game)) {
                            sender.sendMessage("    (" + winnings.getWinningCombination().toFormattedString() + ")");
                        }
                        sender.sendMessage("    " + ChatColor.GOLD + "" + winnings.getTier().getShortHand() + " $" + StringUtils.formatComma(winnings.getWinnings()));
                        sender.sendMessage("    " + ChatColor.GOLD + "Type: " + winnings.getWinningBet(game).getChosenNumbers().getType().name());
                        sender.sendMessage("    " + ChatColor.GOLD + "Price: $" + StringUtils.formatComma(winnings.getWinningBet(game).getBet()));
                        sender.sendMessage("");
                    }
                    for (PlayerBets bet : game.getPlayerBets(player.getUniqueId())) {
                        if (winningsList.stream().noneMatch(each -> each.getWinningBet(game).getBetId().equals(bet.getBetId()))) {
                            sender.sendMessage(u++ + ". " + bet.getChosenNumbers().toFormattedString());
                            sender.sendMessage("    " + ChatColor.GOLD + "No Winnings $0");
                            sender.sendMessage("    " + ChatColor.GOLD + "Type: " + bet.getChosenNumbers().getType().name());
                            sender.sendMessage("    " + ChatColor.GOLD + "Price: $" + StringUtils.formatComma(bet.getBet()));
                            sender.sendMessage("");
                        }
                    }
                }
            }
        }
    }

}
