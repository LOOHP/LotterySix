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

package com.loohp.lotterysix.game.lottery;

import java.util.Comparator;
import java.util.UUID;

public interface ILotterySixGame extends PersistentGame, Comparable<ILotterySixGame> {

    Comparator<ILotterySixGame> COMPARATOR = Comparator.comparing(ILotterySixGame::getDatetime).thenComparing(ILotterySixGame::getGameNumber).thenComparing(ILotterySixGame::getGameId);

    UUID getGameId();

    GameNumber getGameNumber();

    long getDatetime();

    boolean hasSpecialName();

    String getSpecialName();

    @Override
    default int compareTo(ILotterySixGame o) {
        return COMPARATOR.compare(this, o);
    }
}
