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
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public enum PlayerPreferenceKey {

    HIDE_TITLES(boolean.class, false, Arrays.asList("true", "false"), s -> Boolean.parseBoolean(s)),
    HIDE_PERIODIC_ANNOUNCEMENTS(boolean.class, false, Arrays.asList("true", "false"), s -> Boolean.parseBoolean(s)),
    BET_LIMIT_PER_ROUND(long.class, 100000L, Arrays.asList("0", "100", "1000", "100000", "10000000"), s -> Long.parseLong(s));

    private final Class<?> valueTypeClass;
    private final Object defaultValue;
    private final List<String> suggestedValues;
    private final Function<String, ?> reader;

    <T> PlayerPreferenceKey(Class<T> valueTypeClass, T defaultValue, List<String> suggestedValues, Function<String, T> reader) {
        this.valueTypeClass = valueTypeClass;
        this.defaultValue = defaultValue;
        this.suggestedValues = Collections.unmodifiableList(suggestedValues);
        this.reader = reader;
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

    public Object getDefaultValue() {
        return defaultValue;
    }

    public List<String> getSuggestedValues() {
        return suggestedValues;
    }

    public Function<String, ?> getReader() {
        return reader;
    }
}