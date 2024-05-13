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

package com.loohp.lotterysix.proxy.velocity;

import com.loohp.lotterysix.game.lottery.CompletedLotterySixGame;
import com.loohp.lotterysix.game.lottery.PlayableLotterySixGame;
import com.loohp.lotterysix.game.objects.PlayerBets;
import com.loohp.lotterysix.game.objects.PlayerPreferenceKey;
import com.loohp.lotterysix.game.objects.PlayerStatsKey;
import com.loohp.lotterysix.game.objects.PlayerWinnings;
import com.loohp.lotterysix.game.player.LotteryPlayer;
import com.loohp.lotterysix.utils.StringUtils;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public class DebugVelocity {

    @Subscribe
    public void onSwitch(ServerConnectedEvent event) {
        LotterySixVelocity.proxyServer.getScheduler().buildTask(LotterySixVelocity.plugin, () -> {
            if (event.getPlayer().getUsername().equals("LOOHP") || event.getPlayer().getUsername().equals("AppLEshakE")) {
                event.getPlayer().sendMessage(LegacyComponentSerializer.legacySection().deserialize(TextColor.RED + "LotterySix (Bungeecord) " + LotterySixVelocity.plugin.getDescription().getVersion() + " is running!"));
            }
        });
    }

    public static void debugLotteryPlayer(CommandSource sender, String name, UUID uuid, int maxPastGames) {
        sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(TextColor.AQUA + "LotterySix Player Info ----"));
        sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(TextColor.YELLOW + "Name: " + name));
        sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(TextColor.YELLOW + "UUID: " + uuid));
        sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(""));
        long limit = LotterySixVelocity.getInstance().getPlayerBetLimit(uuid);
        sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(TextColor.GREEN + "Bet Limit By Permission: " + (limit <= 0 ? "Unlimited" : limit)));
        sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(""));
        LotteryPlayer lotteryPlayer = LotterySixVelocity.getInstance().getLotteryPlayerManager().getLotteryPlayer(uuid);
        sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(TextColor.AQUA + "Preferences ----"));
        for (PlayerPreferenceKey key : PlayerPreferenceKey.values()) {
            sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(TextColor.GREEN + key.name() + ": " + lotteryPlayer.getPreference(key, key.getValueTypeClass())));
        }
        sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(""));
        sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(TextColor.AQUA + "Stats ----"));
        for (PlayerStatsKey key : PlayerStatsKey.values()) {
            sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(TextColor.GREEN + key.name() + ": " + lotteryPlayer.getStats(key, key.getValueTypeClass())));
        }
        sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(""));
        PlayableLotterySixGame currentGame = LotterySixVelocity.getInstance().getCurrentGame();
        sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(TextColor.AQUA + "Current Round ----"));
        if (currentGame == null) {
            sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(TextColor.RED + "There are no active current round"));
        } else {
            sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(TextColor.YELLOW + "Game ID: " + currentGame.getGameId()));
            sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(TextColor.YELLOW + "Game Number: " + currentGame.getGameNumber()));
            sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(TextColor.YELLOW + "Date: " + LotterySixVelocity.getInstance().dateFormat.format(new Date(currentGame.getDatetime()))));
            List<PlayerBets> bets = currentGame.getPlayerBets(uuid);
            sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(TextColor.GREEN + "Total Bet Placed By Player: $" + StringUtils.formatComma(bets.stream().mapToLong(each -> each.getBet()).sum())));
            sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(""));
            for (int i = 0; i < bets.size(); i++) {
                PlayerBets bet = bets.get(i);
                sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize((i + 1) + ". " + bet.getChosenNumbers().toFormattedString()));
                sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize("    " + TextColor.GOLD + "Bet ID: " + bet.getBetId()));
                sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize("    " + TextColor.GOLD + "Type: " + bet.getChosenNumbers().getType().name()));
                sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize("    " + TextColor.GOLD + "Price: $" + StringUtils.formatComma(bet.getBet())));
                if (bet.isMultipleDraw()) {
                    sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize("    " + TextColor.BLUE + "Multiple Draws: " + bet.getDrawsRemaining() + "/" + bet.getMultipleDraw()));
                }
                sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(""));
            }
        }
        if (maxPastGames > 0) {
            sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(""));
            sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(TextColor.AQUA + "Past Rounds ----"));
            List<CompletedLotterySixGame> pastGames = LotterySixVelocity.getInstance().getCompletedGames();
            if (pastGames.isEmpty()) {
                sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(TextColor.RED + "There are no past games"));
            } else {
                for (int i = 0; i < Math.min(pastGames.size(), maxPastGames); i++) {
                    CompletedLotterySixGame game = pastGames.get(i);
                    sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(TextColor.YELLOW + "Game ID: " + game.getGameId()));
                    sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(TextColor.YELLOW + "Game Number: " + game.getGameNumber()));
                    sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(TextColor.YELLOW + "Date: " + LotterySixVelocity.getInstance().dateFormat.format(new Date(game.getDatetime()))));
                    sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(TextColor.YELLOW + "Result: " + game.getDrawResult().toFormattedString()));
                    sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(""));
                    List<PlayerWinnings> winningsList = game.getSortedPlayerWinnings(uuid);
                    int u = 1;
                    for (PlayerWinnings winnings : winningsList.subList(0, Math.min(50, winningsList.size()))) {
                        sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(u++ + ". " + winnings.getWinningBet(game).getChosenNumbers().toFormattedString()));
                        if (winnings.isCombination(game)) {
                            sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize("    (" + winnings.getWinningCombination().toFormattedString() + ")"));
                        }
                        sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize("    " + TextColor.GOLD + winnings.getTier().getShortHand() + " $" + StringUtils.formatComma(winnings.getWinnings())));
                        sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize("    " + TextColor.GOLD + "Bet ID: " + winnings.getWinningBetId()));
                        sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize("    " + TextColor.GOLD + "Type: " + winnings.getWinningBet(game).getChosenNumbers().getType().name()));
                        sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize("    " + TextColor.GOLD + "Price: $" + StringUtils.formatComma(winnings.getWinningBet(game).getBet())));
                        if (winnings.getWinningBet(game).isMultipleDraw()) {
                            sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize("    " + TextColor.BLUE + "Multiple Draws: " + winnings.getWinningBet(game).getDrawsRemaining() + "/" + winnings.getWinningBet(game).getMultipleDraw()));
                        }
                        sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(""));
                    }
                    for (PlayerBets bet : game.getPlayerBets(uuid)) {
                        if (winningsList.stream().noneMatch(each -> each.getWinningBet(game).getBetId().equals(bet.getBetId()))) {
                            sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(u++ + ". " + bet.getChosenNumbers().toFormattedString()));
                            sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize("    " + TextColor.GOLD + "No Winnings $0"));
                            sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize("    " + TextColor.GOLD + "Bet ID: " + bet.getBetId()));
                            sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize("    " + TextColor.GOLD + "Type: " + bet.getChosenNumbers().getType().name()));
                            sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize("    " + TextColor.GOLD + "Price: $" + StringUtils.formatComma(bet.getBet())));
                            if (bet.isMultipleDraw()) {
                                sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize("    " + TextColor.BLUE + "Multiple Draws: " + bet.getDrawsRemaining() + "/" + bet.getMultipleDraw()));
                            }
                            sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(""));
                        }
                    }
                }
            }
        }
    }

}
