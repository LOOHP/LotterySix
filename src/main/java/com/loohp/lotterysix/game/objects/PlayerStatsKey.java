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

public enum PlayerStatsKey {

    TOTAL_BETS_PLACED(long.class, 0L, true),
    TOTAL_ROUNDS_PARTICIPATED(long.class, 0L, false),
    TOTAL_WINNINGS(long.class, 0L, true),
    HIGHEST_WON_TIER(PrizeTier.class, null, false),
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    PENDING_TRANSACTION(long.class, 0L, true),
    NOTIFY_BALANCE_CHANGE(long.class, 0L, false),
    ACCOUNT_BALANCE(long.class, 0L, true);

    private final Class<?> valueTypeClass;
    private final Object defaultValue;
    private final boolean isMonetaryValue;

    <T> PlayerStatsKey(Class<T> valueTypeClass, T defaultValue, boolean isMonetaryValue) {
        this.valueTypeClass = valueTypeClass;
        this.defaultValue = defaultValue;
        this.isMonetaryValue = isMonetaryValue;
    }

    public static PlayerStatsKey fromKey(String key) {
        for (PlayerStatsKey statsKey : values()) {
            if (key.equalsIgnoreCase(statsKey.name())) {
                return statsKey;
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

    public boolean isMonetaryValue() {
        return isMonetaryValue;
    }
}
