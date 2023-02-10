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

package com.loohp.lotterysix.utils;

public class MathUtils {

    public static long followRound(long follow, long value) {
        return followRound(follow, value, true);
    }

    private static long followRound(long follow, long value, boolean tryAgain) {
        int signNum = value < 0 ? -1 : 1;
        value = Math.abs(value);
        String str = Long.toString(Math.abs(follow));
        int i;
        for (i = str.length() - 1; i >= 0; i--) {
            if (str.charAt(i) != '0') {
                break;
            }
        }
        double d = Math.pow(10, str.length() - i - 1);
        if (value < d) {
            if (!tryAgain) {
                new RuntimeException("Please report this to the LotterySix developer! (" + value + ")").printStackTrace();
                return value;
            }
            int valurStrLength = Long.toString(value).length();
            if (valurStrLength <= 1) {
                return value * signNum;
            }
            return followRound((long) Math.pow(10, valurStrLength - 1), value * signNum, false);
        }
        long result = (long) (Math.round(value / d) * d);
        return (result <= 0 ? value : result) * signNum;
    }

    public static long followRoundDown(long follow, long value) {
        return followRoundDown(follow, value, true);
    }

    private static long followRoundDown(long follow, long value, boolean tryAgain) {
        int signNum = value < 0 ? -1 : 1;
        value = Math.abs(value);
        String str = Long.toString(Math.abs(follow));
        int i;
        for (i = str.length() - 1; i >= 0; i--) {
            if (str.charAt(i) != '0') {
                break;
            }
        }
        double d = Math.pow(10, str.length() - i - 1);
        if (value < d) {
            if (!tryAgain) {
                new RuntimeException("Please report this to the LotterySix developer! (" + value + ")").printStackTrace();
                return value;
            }
            int valurStrLength = Long.toString(value).length();
            if (valurStrLength <= 1) {
                return value * signNum;
            }
            return followRoundDown((long) Math.pow(10, valurStrLength - 1), value * signNum, false);
        }
        long result = (long) (Math.floor(value / d) * d);
        return (result <= 0 ? value : result) * signNum;
    }

}
