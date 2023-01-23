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

import java.util.Objects;
import java.util.UUID;

public class PlayerWinnings {

    private final UUID player;
    private final PrizeTier tier;
    private final PlayerBets winningBet;
    private final long winnings;

    public PlayerWinnings(UUID player, PrizeTier tier, PlayerBets winningBet, long winnings) {
        this.player = player;
        this.tier = tier;
        this.winningBet = winningBet;
        this.winnings = winnings;
    }

    public UUID getPlayer() {
        return player;
    }

    public PrizeTier getTier() {
        return tier;
    }

    public PlayerBets getWinningBet() {
        return winningBet;
    }

    public long getWinnings() {
        return winnings;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerWinnings that = (PlayerWinnings) o;
        return winnings == that.winnings && player.equals(that.player) && tier == that.tier && winningBet.equals(that.winningBet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(player, tier, winningBet, winnings);
    }
}
