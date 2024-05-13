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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ChatColorUtils {

    public static char COLOR_CHAR = '\u00a7';
    public static Set<Character> VALID_COLORS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'k', 'l', 'm', 'n', 'o', 'r')));

    public static String RED = COLOR_CHAR + "c";
    public static String AQUA = COLOR_CHAR + "b";
    public static String GREEN = COLOR_CHAR + "a";

    private static final String[] NUMBER_COLOR = new String[] {
            RED, RED, AQUA, AQUA, GREEN, GREEN, RED, RED, AQUA,
            GREEN, RED, RED, AQUA, AQUA, GREEN, GREEN, RED, RED,
            GREEN, GREEN, RED, RED, AQUA, AQUA, GREEN, GREEN, RED,
            AQUA, GREEN, GREEN, RED, RED, AQUA, AQUA, GREEN, GREEN,
            AQUA, AQUA, GREEN, GREEN, RED, RED, AQUA, AQUA, GREEN,
            RED, AQUA, AQUA, GREEN, GREEN, RED, RED, AQUA, AQUA
    };

    public static String applyNumberColor(int i) {
        return getNumberColor(i) + i;
    }

    public static String getNumberColor(int i) {
        return NUMBER_COLOR[(i - 1) % NUMBER_COLOR.length];
    }

    public static String translateAlternateColorCodes(char code, String text) {
        if (text == null) {
            return null;
        }

        if (text.length() < 2) {
            return text;
        }

        for (int i = 0; i < text.length() - 1; i++) {
            if (text.charAt(i) == code) {
                if (text.charAt(i + 1) == 'x' && text.length() > (i + 14)) {
                    String section = text.substring(i, i + 14);
                    String translated = section.replace(code, COLOR_CHAR);
                    text = text.replace(section, translated);
                } else if (VALID_COLORS.contains(text.charAt(i + 1))) {
                    text = text.substring(0, i) + COLOR_CHAR + text.substring(i + 1);
                }
            }
        }

        return text;
    }

}
