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

import com.loohp.lotterysix.game.objects.PrizeTier;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.Month;
import java.time.Year;
import java.time.ZonedDateTime;
import java.util.TimeZone;
import java.util.function.Predicate;

public class CompletedLotteryGamesQuery {

    private final boolean onlyWithSpecialName;
    private final Year year;
    private final Month month;
    private final DayOfWeek dayOfWeek;
    private final long firstTierPrizeAtLeast;

    public CompletedLotteryGamesQuery(boolean onlyWithSpecialName, Year year, Month month, DayOfWeek dayOfWeek, long firstTierPrizeAtLeast) {
        this.onlyWithSpecialName = onlyWithSpecialName;
        this.year = year;
        this.month = month;
        this.dayOfWeek = dayOfWeek;
        this.firstTierPrizeAtLeast = firstTierPrizeAtLeast;
    }

    public Predicate<CompletedLotterySixGameIndex> getQueryPredicate(LazyCompletedLotterySixGameList games, TimeZone timezone) {
        return index -> {
            if (onlyWithSpecialName && !index.hasSpecialName()) {
                return false;
            }
            ZonedDateTime time = ZonedDateTime.ofInstant(Instant.ofEpochMilli(index.getDatetime()), timezone.toZoneId());
            if (year != null && !Year.of(time.getYear()).equals(year)) {
                return false;
            }
            if (month != null && !time.getMonth().equals(month)) {
                return false;
            }
            if (dayOfWeek != null && !time.getDayOfWeek().equals(dayOfWeek)) {
                return false;
            }
            if (firstTierPrizeAtLeast > 0 && games.get(index).getPrizeForTier(PrizeTier.FIRST) >= firstTierPrizeAtLeast) {
                return false;
            }
            return true;
        };
    }

    public Builder toBuilder() {
        return Builder.builder()
                .onlyWithSpecialName(onlyWithSpecialName)
                .inYear(year)
                .inMonth(month)
                .inDayOfWeek(dayOfWeek)
                .onlyIfFirstTierPrizeAtLeast(firstTierPrizeAtLeast);
    }

    public static class Builder {

        public static Builder builder() {
            return new Builder();
        }

        private boolean onlyWithSpecialName;
        private Year year;
        private Month month;
        private DayOfWeek dayOfWeek;
        private long firstTierPrizeAtLeast;

        private Builder() {
            this.onlyWithSpecialName = false;
            this.year = null;
            this.month = null;
            this.dayOfWeek = null;
            this.firstTierPrizeAtLeast = 0;
        }

        public Builder onlyWithSpecialName(boolean onlyWithSpecialName) {
            this.onlyWithSpecialName = onlyWithSpecialName;
            return this;
        }

        public Builder inYear(Year year) {
            this.year = year;
            return this;
        }

        public Builder inMonth(Month month) {
            this.month = month;
            return this;
        }

        public Builder inDayOfWeek(DayOfWeek dayOfWeek) {
            this.dayOfWeek = dayOfWeek;
            return this;
        }

        public Builder onlyIfFirstTierPrizeAtLeast(long firstTierPrizeAtLeast) {
            this.firstTierPrizeAtLeast = firstTierPrizeAtLeast;
            return this;
        }

        public CompletedLotteryGamesQuery build() {
            return new CompletedLotteryGamesQuery(onlyWithSpecialName, year, month, dayOfWeek, firstTierPrizeAtLeast);
        }

    }

}
