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

import com.loohp.lotterysix.game.objects.WinningNumbers;

import java.util.Objects;
import java.util.UUID;

public class CompletedLotterySixGameIndex implements ILotterySixGame {

    private transient WinningNumbers drawResultCache;

    private final UUID gameId;
    private final long datetime;
    private final GameNumber gameNumber;
    private final String drawResult;
    private final String specialName;

    public CompletedLotterySixGameIndex(UUID gameId, long datetime, GameNumber gameNumber, WinningNumbers drawResult, String specialName) {
        this.gameId = gameId;
        this.datetime = datetime;
        this.gameNumber = gameNumber;
        this.drawResultCache = drawResult;
        this.drawResult = drawResult.toString();
        this.specialName = specialName;
    }

    @Override
    public UUID getGameId() {
        return gameId;
    }

    @Override
    public GameNumber getGameNumber() {
        return gameNumber;
    }

    @Override
    public boolean hasSpecialName() {
        return specialName != null && !specialName.isEmpty();
    }

    public long getDatetime() {
        return datetime;
    }

    @Override
    public String getDataFileName() {
        return gameNumber.toString().replace("/", "_") + "_" + datetime + ".json";
    }

    public WinningNumbers getDrawResult() {
        if (drawResultCache != null) {
            return drawResultCache;
        }
        return drawResultCache = WinningNumbers.fromString(drawResult);
    }

    @Override
    public String getSpecialName() {
        return specialName;
    }

    public boolean isDetailsComplete() {
        return gameId != null && gameNumber != null && drawResult != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompletedLotterySixGameIndex gameIndex = (CompletedLotterySixGameIndex) o;
        return datetime == gameIndex.datetime && Objects.equals(gameId, gameIndex.gameId) && Objects.equals(gameNumber, gameIndex.gameNumber) && Objects.equals(drawResult, gameIndex.drawResult) && Objects.equals(specialName, gameIndex.specialName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameId, datetime, gameNumber, drawResult, specialName);
    }
}
