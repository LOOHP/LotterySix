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

import com.loohp.lotterysix.game.player.LotteryPlayerManager;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class LotteryPlayer {

    private transient LotteryPlayerManager manager;

    private final UUID player;
    private final Map<PlayerPreferenceKey, Object> preferences;
    private final Map<PlayerStatsKey, Object> stats;

    public LotteryPlayer(LotteryPlayerManager manager, UUID player) {
        this.manager = manager;
        this.player = player;
        this.preferences = Collections.synchronizedMap(new EnumMap<>(PlayerPreferenceKey.class));
        this.stats = Collections.synchronizedMap(new EnumMap<>(PlayerStatsKey.class));
    }

    public LotteryPlayer(LotteryPlayerManager manager, UUID player, Map<PlayerPreferenceKey, Object> preferences, Map<PlayerStatsKey, Object> stats) {
        this.manager = manager;
        this.player = player;
        this.preferences = Collections.synchronizedMap(new EnumMap<>(preferences));
        this.stats = Collections.synchronizedMap(new EnumMap<>(stats));
    }

    public LotteryPlayerManager getManager() {
        return manager;
    }

    public void setManager(LotteryPlayerManager manager) {
        this.manager = manager;
    }

    public void save() {
        manager.saveLotteryPlayer(player);
    }

    public UUID getPlayer() {
        return player;
    }

    public Object getPreference(PlayerPreferenceKey key) {
        return preferences.getOrDefault(key, key.getDefaultValue());
    }

    @SuppressWarnings("unchecked")
    public <T> T getPreference(PlayerPreferenceKey key, Class<T> type) {
        return (T) getPreference(key);
    }

    public void setPreference(PlayerPreferenceKey key, Object value) {
        preferences.put(key, value);
        save();
    }

    public Object getStats(PlayerStatsKey key) {
        return stats.getOrDefault(key, key.getDefaultValue());
    }

    @SuppressWarnings("unchecked")
    public <T> T getStats(PlayerStatsKey key, Class<T> type) {
        return (T) getStats(key);
    }

    public void setStats(PlayerStatsKey key, Object value) {
        stats.put(key, value);
        save();
    }

    public <T> void updateStats(PlayerStatsKey key, Class<T> type, Predicate<T> predicate, T newValue) {
        updateStats(key, type, t -> predicate.test(t) ? newValue : t);
    }

    public synchronized <T> void updateStats(PlayerStatsKey key, Class<T> type, UnaryOperator<T> updateFunction) {
        T t = getStats(key, type);
        setStats(key, updateFunction.apply(t));
    }
}
