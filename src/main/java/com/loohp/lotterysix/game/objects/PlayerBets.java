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

import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;

public class PlayerBets implements Comparable<PlayerBets> {

    public static final Comparator<PlayerBets> COMPARATOR = Comparator.comparing(PlayerBets::getTimePlaced).thenComparing(PlayerBets::getNanoTime).thenComparing(PlayerBets::getBetId);

    private final UUID betId;
    private final String name;
    private final UUID player;
    private final long timePlaced;
    private final long nanoTime;
    private final long bet;
    private final BetUnitType type;
    private final BetNumbers chosenNumbers;
    private final int multipleDraw;
    private final int drawsRemaining;

    private PlayerBets(String name, UUID player, long timePlaced, long nanoTime, long bet, BetUnitType type, BetNumbers chosenNumbers, int multipleDraw, int drawsRemaining) {
        this.name = name;
        this.timePlaced = timePlaced;
        this.nanoTime = nanoTime;
        this.multipleDraw = multipleDraw;
        this.drawsRemaining = drawsRemaining;
        this.betId = UUID.randomUUID();
        this.player = player;
        this.bet = bet;
        this.type = type;
        this.chosenNumbers = chosenNumbers;
    }

    public PlayerBets(String name, UUID player, long timePlaced, long nanoTime, long bet, BetUnitType type, BetNumbers chosenNumbers, int multipleDraw) {
        this(name, player, timePlaced, nanoTime, bet, type, chosenNumbers, multipleDraw, multipleDraw);
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

    public long getNanoTime() {
        return nanoTime;
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

    public boolean isMultipleDraw() {
        return multipleDraw > 1;
    }

    public int getMultipleDraw() {
        return isMultipleDraw() ? multipleDraw : 1;
    }

    public int getDrawsRemaining() {
        return isMultipleDraw() ? drawsRemaining : 1;
    }

    public PlayerBets decrementDrawsRemaining() {
        if (isMultipleDraw()) {
            int decremented = Math.max(0, drawsRemaining - 1);
            if (decremented < drawsRemaining) {
                return new PlayerBets(name, player, timePlaced, nanoTime, bet, type, chosenNumbers, multipleDraw, decremented);
            }
        }
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerBets that = (PlayerBets) o;
        return timePlaced == that.timePlaced && nanoTime == that.nanoTime && bet == that.bet && multipleDraw == that.multipleDraw && drawsRemaining == that.drawsRemaining && Objects.equals(betId, that.betId) && Objects.equals(name, that.name) && Objects.equals(player, that.player) && type == that.type && Objects.equals(chosenNumbers, that.chosenNumbers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(betId, name, player, timePlaced, nanoTime, bet, type, chosenNumbers, multipleDraw, drawsRemaining);
    }

    @Override
    public int compareTo(PlayerBets o) {
        return COMPARATOR.compare(this, o);
    }
}
