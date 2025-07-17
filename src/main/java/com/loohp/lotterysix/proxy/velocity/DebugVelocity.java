/*
 * This file is part of LotterySix.
 *
 * Copyright (C) 2025. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2025. Contributors
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
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public class DebugVelocity {

    @Subscribe
    public void onSwitch(ServerPostConnectEvent event) {
        LotterySixVelocity.proxyServer.getScheduler().buildTask(LotterySixVelocity.plugin, () -> {
            if (event.getPlayer().getUsername().equals("LOOHP") || event.getPlayer().getUsername().equals("AppLEshakE")) {
                event.getPlayer().sendMessage(m(NamedTextColor.RED, "LotterySix (Velocity) " + LotterySixVelocity.plugin.getDescription().getVersion() + " is running!"));
            }
        });
    }

    public static void debugLotteryPlayer(CommandSource sender, String name, UUID uuid, int maxPastGames) {
        sender.sendMessage(m(NamedTextColor.AQUA, "LotterySix Player Info ----"));
        sender.sendMessage(m(NamedTextColor.YELLOW, "Name: " + name));
        sender.sendMessage(m(NamedTextColor.YELLOW, "UUID: " + uuid));
        sender.sendMessage(m(""));
        long limit = LotterySixVelocity.getInstance().getPlayerBetLimit(uuid);
        sender.sendMessage(m(NamedTextColor.GREEN, "Bet Limit By Permission: " + (limit <= 0 ? "Unlimited" : limit)));
        sender.sendMessage(m(""));
        LotteryPlayer lotteryPlayer = LotterySixVelocity.getInstance().getLotteryPlayerManager().getLotteryPlayer(uuid);
        sender.sendMessage(m(NamedTextColor.AQUA, "Preferences ----"));
        for (PlayerPreferenceKey key : PlayerPreferenceKey.values()) {
            sender.sendMessage(m(NamedTextColor.GREEN, key.name() + ": " + lotteryPlayer.getPreference(key, key.getValueTypeClass())));
        }
        sender.sendMessage(m(""));
        sender.sendMessage(m(NamedTextColor.AQUA, "Stats ----"));
        for (PlayerStatsKey key : PlayerStatsKey.values()) {
            sender.sendMessage(m(NamedTextColor.GREEN, key.name() + ": " + lotteryPlayer.getStats(key, key.getValueTypeClass())));
        }
        sender.sendMessage(m(""));
        PlayableLotterySixGame currentGame = LotterySixVelocity.getInstance().getCurrentGame();
        sender.sendMessage(m(NamedTextColor.AQUA, "Current Round ----"));
        if (currentGame == null) {
            sender.sendMessage(m(NamedTextColor.RED, "There are no active current round"));
        } else {
            sender.sendMessage(m(NamedTextColor.YELLOW, "Game ID: " + currentGame.getGameId()));
            sender.sendMessage(m(NamedTextColor.YELLOW, "Game Number: " + currentGame.getGameNumber()));
            sender.sendMessage(m(NamedTextColor.YELLOW, "Date: " + LotterySixVelocity.getInstance().dateFormat.format(new Date(currentGame.getDatetime()))));
            List<PlayerBets> bets = currentGame.getPlayerBets(uuid);
            sender.sendMessage(m(NamedTextColor.GREEN, "Total Bet Placed By Player: $" + StringUtils.formatComma(bets.stream().mapToLong(each -> each.getBet()).sum())));
            sender.sendMessage(m(""));
            for (int i = 0; i < bets.size(); i++) {
                PlayerBets bet = bets.get(i);
                sender.sendMessage(m(NamedTextColor.GOLD, (i + 1) + ". " + bet.getChosenNumbers().toFormattedString()));
                sender.sendMessage(m(NamedTextColor.GOLD, "    Bet ID: " + bet.getBetId()));
                sender.sendMessage(m(NamedTextColor.GOLD, "    Type: " + bet.getChosenNumbers().getType().name()));
                sender.sendMessage(m(NamedTextColor.GOLD, "    Price: $" + StringUtils.formatComma(bet.getBet())));
                if (bet.isMultipleDraw()) {
                    sender.sendMessage(m(NamedTextColor.BLUE, "    Multiple Draws: " + bet.getDrawsRemaining() + "/" + bet.getMultipleDraw()));
                }
                sender.sendMessage(m(""));
            }
        }
        if (maxPastGames > 0) {
            sender.sendMessage(m(""));
            sender.sendMessage(m(NamedTextColor.AQUA, "Past Rounds ----"));
            List<CompletedLotterySixGame> pastGames = LotterySixVelocity.getInstance().getCompletedGames();
            if (pastGames.isEmpty()) {
                sender.sendMessage(m(NamedTextColor.RED, "There are no past games"));
            } else {
                for (int i = 0; i < Math.min(pastGames.size(), maxPastGames); i++) {
                    CompletedLotterySixGame game = pastGames.get(i);
                    sender.sendMessage(m(NamedTextColor.YELLOW, "Game ID: " + game.getGameId()));
                    sender.sendMessage(m(NamedTextColor.YELLOW, "Game Number: " + game.getGameNumber()));
                    sender.sendMessage(m(NamedTextColor.YELLOW, "Date: " + LotterySixVelocity.getInstance().dateFormat.format(new Date(game.getDatetime()))));
                    sender.sendMessage(m(NamedTextColor.YELLOW, "Result: " + game.getDrawResult().toFormattedString()));
                    sender.sendMessage(m(""));
                    List<PlayerWinnings> winningsList = game.getSortedPlayerWinnings(uuid);
                    int u = 1;
                    for (PlayerWinnings winnings : winningsList.subList(0, Math.min(50, winningsList.size()))) {
                        sender.sendMessage(m(u++ + ". " + winnings.getWinningBet(game).getChosenNumbers().toFormattedString()));
                        if (winnings.isCombination(game)) {
                            sender.sendMessage(m("    (" + winnings.getWinningCombination().toFormattedString() + ")"));
                        }
                        sender.sendMessage(m(NamedTextColor.GOLD, "    " + winnings.getTier().getShortHand() + " $" + StringUtils.formatComma(winnings.getWinnings())));
                        sender.sendMessage(m(NamedTextColor.GOLD, "    Bet ID: " + winnings.getWinningBetId()));
                        sender.sendMessage(m(NamedTextColor.GOLD, "    Type: " + winnings.getWinningBet(game).getChosenNumbers().getType().name()));
                        sender.sendMessage(m(NamedTextColor.GOLD, "    Price: $" + StringUtils.formatComma(winnings.getWinningBet(game).getBet())));
                        if (winnings.getWinningBet(game).isMultipleDraw()) {
                            sender.sendMessage(m(NamedTextColor.BLUE, "    Multiple Draws: " + winnings.getWinningBet(game).getDrawsRemaining() + "/" + winnings.getWinningBet(game).getMultipleDraw()));
                        }
                        sender.sendMessage(m(""));
                    }
                    for (PlayerBets bet : game.getPlayerBets(uuid)) {
                        if (winningsList.stream().noneMatch(each -> each.getWinningBet(game).getBetId().equals(bet.getBetId()))) {
                            sender.sendMessage(m(u++ + ". " + bet.getChosenNumbers().toFormattedString()));
                            sender.sendMessage(m(NamedTextColor.GOLD, "    No Winnings $0"));
                            sender.sendMessage(m(NamedTextColor.GOLD, "    Bet ID: " + bet.getBetId()));
                            sender.sendMessage(m(NamedTextColor.GOLD, "    Type: " + bet.getChosenNumbers().getType().name()));
                            sender.sendMessage(m(NamedTextColor.GOLD, "    Price: $" + StringUtils.formatComma(bet.getBet())));
                            if (bet.isMultipleDraw()) {
                                sender.sendMessage(m(NamedTextColor.BLUE, "    Multiple Draws: " + bet.getDrawsRemaining() + "/" + bet.getMultipleDraw()));
                            }
                            sender.sendMessage(m(""));
                        }
                    }
                }
            }
        }
    }

    private static TextComponent m(String m) {
        return LegacyComponentSerializer.legacySection().deserialize(m);
    }

    private static TextComponent m(TextColor c, String m) {
        return LegacyComponentSerializer.legacySection().deserialize(m).color(c);
    }

}
