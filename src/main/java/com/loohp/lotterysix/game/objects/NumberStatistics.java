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

package com.loohp.lotterysix.game.objects;

import java.util.Objects;

public class NumberStatistics {

    public static final NumberStatistics NOT_EVER_DRAWN = new NumberStatistics(-1, 0);

    private final int lastDrawn;
    private final int timesDrawn;

    public NumberStatistics(int lastDrawn, int timesDrawn) {
        this.lastDrawn = lastDrawn;
        this.timesDrawn = timesDrawn;
    }

    public NumberStatistics increment(boolean chosen) {
        return chosen ? new NumberStatistics(0, timesDrawn + 1) : new NumberStatistics(isNotEverDrawn() ? -1 : lastDrawn + 1, timesDrawn);
    }

    public int getLastDrawn() {
        return lastDrawn;
    }

    public int getTimesDrawn() {
        return timesDrawn;
    }

    public boolean isNotEverDrawn() {
        return lastDrawn < 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NumberStatistics that = (NumberStatistics) o;
        return lastDrawn == that.lastDrawn && timesDrawn == that.timesDrawn;
    }

    @Override
    public int hashCode() {
        return Objects.hash(lastDrawn, timesDrawn);
    }
}
