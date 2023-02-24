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

package com.loohp.lotterysix.game.objects;

import com.loohp.lotterysix.game.lottery.CompletedLotterySixGame;
import com.loohp.lotterysix.game.objects.betnumbers.BetNumbersType;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class PlayerWinnings {

    private final String name;
    private final UUID player;
    private final PrizeTier tier;
    private final UUID winningBetId;
    private final WinningCombination winningCombination;
    private final long winnings;

    public PlayerWinnings(String name, UUID player, PrizeTier tier, PlayerBets winningBet, WinningCombination winningCombination, long winnings) {
        this.name = name;
        this.player = player;
        this.tier = tier;
        this.winningBetId = winningBet.getBetId();
        this.winningCombination = winningCombination;
        this.winnings = winnings;
    }

    private PlayerWinnings(String name, UUID player, PrizeTier tier, UUID winningBetId, WinningCombination winningCombination, long winnings) {
        this.name = name;
        this.player = player;
        this.tier = tier;
        this.winningBetId = winningBetId;
        this.winningCombination = winningCombination;
        this.winnings = winnings;
    }

    public String getName() {
        return name;
    }

    public UUID getPlayer() {
        return player;
    }

    public PrizeTier getTier() {
        return tier;
    }

    public UUID getWinningBetId() {
        return winningBetId;
    }

    public PlayerBets getWinningBet(CompletedLotterySixGame game) {
        return game.getBet(winningBetId);
    }

    public PlayerBets getWinningBet(Map<UUID, PlayerBets> bets) {
        return bets.get(winningBetId);
    }

    public WinningCombination getWinningCombination() {
        return winningCombination;
    }

    public boolean isCombination(CompletedLotterySixGame game) {
        return getWinningBet(game).getChosenNumbers().isCombination();
    }

    public long getWinnings() {
        return winnings;
    }

    public PlayerWinnings winnings(long winnings) {
        return new PlayerWinnings(name, player, tier, winningBetId, winningCombination, winnings);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerWinnings that = (PlayerWinnings) o;
        return winnings == that.winnings && name.equals(that.name) && player.equals(that.player) && tier == that.tier && winningBetId.equals(that.winningBetId) && winningCombination.equals(that.winningCombination);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, player, tier, winningBetId, winningCombination, winnings);
    }
}
