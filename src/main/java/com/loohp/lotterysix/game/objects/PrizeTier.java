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

public enum PrizeTier {

    FIRST("1st", 2, true),
    SECOND("2nd", 2, true),
    THIRD("3rd", 2, true),
    FOURTH("4th", 15),
    FIFTH("5th", 2),
    SIXTH("6th", 8),
    SEVENTH("7th", 1);

    private final String shortHand;
    private final int minimumMultiplierFromLast;
    private final boolean isTopTier;

    PrizeTier(String shortHand, int minimumMultiplierFromLast, boolean isTopTier) {
        this.shortHand = shortHand;
        this.minimumMultiplierFromLast = minimumMultiplierFromLast;
        this.isTopTier = isTopTier;
    }

    PrizeTier(String shortHand, int minimumMultiplierFromLast) {
        this(shortHand, minimumMultiplierFromLast, false);
    }

    public String getShortHand() {
        return shortHand;
    }

    public int getMinimumMultiplierFromLast() {
        return minimumMultiplierFromLast;
    }

    public boolean isTopTier() {
        return isTopTier;
    }

    @Override
    public String toString() {
        return getShortHand();
    }

}
