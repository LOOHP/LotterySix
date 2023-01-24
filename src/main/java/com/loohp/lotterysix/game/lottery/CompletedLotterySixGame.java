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

package com.loohp.lotterysix.game.lottery;

import com.loohp.lotterysix.game.objects.PlayerBets;
import com.loohp.lotterysix.game.objects.PlayerWinnings;
import com.loohp.lotterysix.game.objects.PrizeTier;
import com.loohp.lotterysix.game.objects.WinningNumbers;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class CompletedLotterySixGame {

    private final UUID gameId;
    private final long datetime;
    private final WinningNumbers drawResult;
    private final List<PlayerWinnings> winners;
    private final List<PlayerBets> bets;
    private final long totalPrizes;
    private final long remainingFunds;

    public CompletedLotterySixGame(UUID gameId, long datetime, WinningNumbers drawResult, List<PlayerWinnings> winners, List<PlayerBets> bets, long totalPrizes, long remainingFunds) {
        this.gameId = gameId;
        this.datetime = datetime;
        this.drawResult = drawResult;
        this.winners = Collections.unmodifiableList(winners);
        this.bets = Collections.unmodifiableList(bets);
        this.totalPrizes = totalPrizes;
        this.remainingFunds = remainingFunds;
    }

    public UUID getGameId() {
        return gameId;
    }

    public long getDatetime() {
        return datetime;
    }

    public WinningNumbers getDrawResult() {
        return drawResult;
    }

    public List<PlayerWinnings> getWinnings() {
        return winners;
    }

    public List<PlayerBets> getBets() {
        return bets;
    }

    public long getTotalBets() {
        return bets.stream().mapToLong(each -> each.getBet()).sum();
    }

    public List<PlayerWinnings> getPlayerWinnings(UUID player) {
        return Collections.unmodifiableList(winners.stream().filter(each -> each.getPlayer().equals(player)).collect(Collectors.toList()));
    }

    public List<PlayerBets> getPlayerBets(UUID player) {
        return Collections.unmodifiableList(bets.stream().filter(each -> each.getPlayer().equals(player)).collect(Collectors.toList()));
    }

    public long getTotalPrizes() {
        return totalPrizes;
    }

    public long getPrizeForTier(PrizeTier prizeTier) {
        return winners.stream().filter(each -> each.getTier().equals(prizeTier)).mapToLong(each -> each.getWinnings()).findAny().orElse(0);
    }

    public int getWinnerCountForTier(PrizeTier prizeTier) {
        return (int) winners.stream().filter(each -> each.getTier().equals(prizeTier)).count();
    }

    public long getRemainingFunds() {
        return remainingFunds;
    }

}
