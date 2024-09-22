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

import java.util.Arrays;
import java.util.List;

public enum PrizeCalculationMode {

    UNREALISTIC_FOR_FUN("DEFAULT"),
    HKJC;

    public static final PrizeCalculationMode CONFIG_DEFAULT = UNREALISTIC_FOR_FUN;

    public static PrizeCalculationMode fromName(String name) {
        for (PrizeCalculationMode mode : PrizeCalculationMode.values()) {
            if (mode.name().equals(name) || mode.alternateNames.contains(name)) {
                return mode;
            }
        }
        throw new IllegalArgumentException();
    }

    private final List<String> alternateNames;

    PrizeCalculationMode(String... alternateNames) {
        this.alternateNames = Arrays.asList(alternateNames);
    }
}
