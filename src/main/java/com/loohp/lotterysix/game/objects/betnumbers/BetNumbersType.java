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

package com.loohp.lotterysix.game.objects.betnumbers;

public enum BetNumbersType {

    SINGLE(false, false, false),
    MULTIPLE(true, false, false),
    BANKER(true, true, false),
    RANDOM(false, false, true),
    MULTIPLE_RANDOM(true, false, true),
    BANKER_RANDOM(true, true, true);

    private final boolean multipleCombination;
    private final boolean banker;
    private final boolean random;

    BetNumbersType(boolean multipleCombination, boolean banker, boolean random) {
        this.multipleCombination = multipleCombination;
        this.banker = banker;
        this.random = random;
    }

    public boolean isMultipleCombination() {
        return multipleCombination;
    }

    public boolean isBanker() {
        return banker;
    }

    public boolean isRandom() {
        return random;
    }
}
