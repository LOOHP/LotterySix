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

import com.loohp.lotterysix.game.LotterySix;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public enum PlayerPreferenceKey {

    HIDE_TITLES(boolean.class, (i, p) -> false, () -> Arrays.asList("true", "false"), s -> Boolean.parseBoolean(s), false),
    HIDE_PERIODIC_ANNOUNCEMENTS(boolean.class, (i, p) -> false, () -> Arrays.asList("true", "false"), s -> Boolean.parseBoolean(s), false),
    REOPEN_MENU_ON_PURCHASE(boolean.class, (i, p) -> true, () -> Arrays.asList("true", "false"), s -> Boolean.parseBoolean(s), false),
    BET_LIMIT_PER_ROUND(long.class, (i, p) -> i == null || p == null ? Long.MAX_VALUE : i.getPlayerBetLimit(p), () -> Arrays.asList("0", "100", "1000", "100000", "10000000"), s -> Long.parseLong(s), true),
    SUSPEND_ACCOUNT_UNTIL(long.class, (i, p) -> 0L, () -> Arrays.asList("0", Long.toString(System.currentTimeMillis() + 604800000)), s -> Long.parseLong(s), false);

    private final Class<?> valueTypeClass;
    private final BiFunction<LotterySix, UUID, ?> defaultValue;
    private final Supplier<List<String>> suggestedValues;
    private final Function<String, ?> reader;
    private final boolean isMonetaryValue;

    <T> PlayerPreferenceKey(Class<T> valueTypeClass, BiFunction<LotterySix, UUID, T> defaultValue, Supplier<List<String>> suggestedValues, Function<String, T> reader, boolean isMonetaryValue) {
        this.valueTypeClass = valueTypeClass;
        this.defaultValue = defaultValue;
        this.suggestedValues = suggestedValues;
        this.reader = reader;
        this.isMonetaryValue = isMonetaryValue;
    }

    public static PlayerPreferenceKey fromKey(String key) {
        for (PlayerPreferenceKey preferenceKey : values()) {
            if (key.equalsIgnoreCase(preferenceKey.name())) {
                return preferenceKey;
            }
        }
        return null;
    }

    public Class<?> getValueTypeClass() {
        return valueTypeClass;
    }

    public Object getDefaultValue(LotterySix instance, UUID player) {
        return defaultValue.apply(instance, player);
    }

    public List<String> getSuggestedValues() {
        return suggestedValues.get();
    }

    public Object getReader(String input) {
        return reader.apply(input);
    }

    public boolean isMonetaryValue() {
        return isMonetaryValue;
    }
}
