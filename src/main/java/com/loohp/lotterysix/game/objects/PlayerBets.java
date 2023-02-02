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

import com.loohp.lotterysix.game.objects.betnumbers.BetNumbers;

import java.util.Objects;
import java.util.UUID;

public class PlayerBets {

    private final UUID betId;
    private final String name;
    private final UUID player;
    private final long timePlaced;
    private final long bet;
    private final BetUnitType type;
    private final BetNumbers chosenNumbers;

    public PlayerBets(String name, UUID player, long timePlaced, long bet, BetUnitType type, BetNumbers chosenNumbers) {
        this.name = name;
        this.timePlaced = timePlaced;
        this.betId = UUID.randomUUID();
        this.player = player;
        this.bet = bet;
        this.type = type;
        this.chosenNumbers = chosenNumbers;
    }

    public UUID getBetId() {
        return betId;
    }

    public String getName() {
        return name;
    }

    public UUID getPlayer() {
        return player;
    }

    public long getTimePlaced() {
        return timePlaced;
    }

    public long getBet() {
        return bet;
    }

    public BetUnitType getType() {
        return type;
    }

    public BetNumbers getChosenNumbers() {
        return chosenNumbers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerBets that = (PlayerBets) o;
        return bet == that.bet && betId.equals(that.betId) && player.equals(that.player) && chosenNumbers.equals(that.chosenNumbers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(betId, player, bet, chosenNumbers);
    }
}
