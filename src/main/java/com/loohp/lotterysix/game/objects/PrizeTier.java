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

    FIRST("1st", -1, 2, true),
    SECOND("2nd", -1, 2, true),
    THIRD("3rd", -1, 2, true),
    FOURTH("4th", 960, 15),
    FIFTH("5th", 64, 2),
    SIXTH("6th", 32, 8),
    SEVENTH("7th", 4, 1);

    private final String shortHand;
    private int fixedPrizeMultiplier;
    private int minimumMultiplierFromLast;
    private final boolean isTopTier;

    PrizeTier(String shortHand, int fixedPrizeMultiplier, int minimumMultiplierFromLast, boolean isTopTier) {
        this.shortHand = shortHand;
        this.fixedPrizeMultiplier = fixedPrizeMultiplier;
        this.minimumMultiplierFromLast = minimumMultiplierFromLast;
        this.isTopTier = isTopTier;
    }

    PrizeTier(String shortHand, int fixedPrizeMultiplier, int minimumMultiplierFromLast) {
        this(shortHand, fixedPrizeMultiplier, minimumMultiplierFromLast, false);
    }

    public String getShortHand() {
        return shortHand;
    }

    public int getFixedPrizeMultiplier() {
        return fixedPrizeMultiplier;
    }

    @Deprecated
    public void setFixedPrizeMultiplier(int fixedPrizeMultiplier) {
        this.fixedPrizeMultiplier = fixedPrizeMultiplier;
    }

    public int getMinimumMultiplierFromLast() {
        return minimumMultiplierFromLast;
    }

    @Deprecated
    public void setMinimumMultiplierFromLast(int minimumMultiplierFromLast) {
        this.minimumMultiplierFromLast = minimumMultiplierFromLast;
    }

    public boolean isTopTier() {
        return isTopTier;
    }

    @Override
    public String toString() {
        return getShortHand();
    }

}
