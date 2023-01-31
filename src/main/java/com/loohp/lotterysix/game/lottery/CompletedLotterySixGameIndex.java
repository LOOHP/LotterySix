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

import java.util.Objects;
import java.util.UUID;

public class CompletedLotterySixGameIndex implements IDedGame {

    private final UUID gameId;
    private final long datetime;

    public CompletedLotterySixGameIndex(UUID gameId, long datetime) {
        this.gameId = gameId;
        this.datetime = datetime;
    }

    @Override
    public UUID getGameId() {
        return gameId;
    }

    public long getDatetime() {
        return datetime;
    }

    public String getDataFileName() {
        return datetime + ".json";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompletedLotterySixGameIndex gameIndex = (CompletedLotterySixGameIndex) o;
        return gameId.equals(gameIndex.gameId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameId);
    }
}
