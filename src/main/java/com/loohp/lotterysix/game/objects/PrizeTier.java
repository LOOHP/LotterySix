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

import com.loohp.lotterysix.game.LotteryRegistry;

import static com.loohp.lotterysix.utils.MathUtils.probabilityFormula;
import static com.loohp.lotterysix.game.LotteryRegistry.NUMBERS_PER_BET;

public enum PrizeTier {

    FIRST("1st", new WinningCriteria(NUMBERS_PER_BET, false), 7680, 2, true),
    SECOND("2nd", new WinningCriteria(NUMBERS_PER_BET - 1, true), 3840, 2, true),
    THIRD("3rd", new WinningCriteria(NUMBERS_PER_BET - 1, false), 1920, 2, true),
    FOURTH("4th", new WinningCriteria(NUMBERS_PER_BET - 2, true), 960, 15),
    FIFTH("5th", new WinningCriteria(NUMBERS_PER_BET - 2, false), 64, 2),
    SIXTH("6th", new WinningCriteria(NUMBERS_PER_BET - 3, true), 32, 8),
    SEVENTH("7th", new WinningCriteria(NUMBERS_PER_BET - 3, false), 4, 1);

    private final String shortHand;
    private final WinningCriteria winningCriteria;
    private int fixedPrizeMultiplier;
    private int minimumMultiplierFromLast;
    private final boolean isVariableTier;

    PrizeTier(String shortHand, WinningCriteria winningCriteria, int fixedPrizeMultiplier, int minimumMultiplierFromLast, boolean isVariableTier) {
        this.shortHand = shortHand;
        this.winningCriteria = winningCriteria;
        this.fixedPrizeMultiplier = fixedPrizeMultiplier;
        this.minimumMultiplierFromLast = minimumMultiplierFromLast;
        this.isVariableTier = isVariableTier;
    }

    PrizeTier(String shortHand, WinningCriteria winningCriteria, int fixedPrizeMultiplier, int minimumMultiplierFromLast) {
        this(shortHand, winningCriteria, fixedPrizeMultiplier, minimumMultiplierFromLast, false);
    }

    public String getShortHand() {
        return shortHand;
    }

    public WinningCriteria getWinningCriteria() {
        return winningCriteria;
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

    public boolean isVariableTier() {
        return isVariableTier;
    }

    @Override
    public String toString() {
        return getShortHand();
    }

    public static class WinningCriteria {

        private final int matchNumbers;
        private final boolean requireSpecialNumber;

        public WinningCriteria(int matchNumbers, boolean requireSpecialNumber) {
            this.matchNumbers = matchNumbers;
            this.requireSpecialNumber = requireSpecialNumber;
        }

        public boolean satisfies(int matches, boolean matchedSpecial) {
            return matches >= matchNumbers && (!requireSpecialNumber || matchedSpecial);
        }

        public double probability(int numberOfChoices) {
            int notMatched = (requireSpecialNumber ? LotteryRegistry.NUMBERS_PER_BET - 1 : LotteryRegistry.NUMBERS_PER_BET) - matchNumbers;
            return probabilityFormula(LotteryRegistry.NUMBERS_PER_BET, matchNumbers) * probabilityFormula(numberOfChoices - LotteryRegistry.NUMBERS_PER_BET - 1, notMatched) / probabilityFormula(numberOfChoices, LotteryRegistry.NUMBERS_PER_BET);
        }

        public double oneOverProbability(int numberOfChoices) {
            return 1.0 / probability(numberOfChoices);
        }

        public int getMatchNumbers() {
            return matchNumbers;
        }

        public boolean requireSpecialNumber() {
            return requireSpecialNumber;
        }

    }

}
