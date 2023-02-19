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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class LotteryPlayer {

    private transient LotteryPlayerManager manager;

    private final UUID player;
    private final ConcurrentHashMap<PlayerPreferenceKey, Object> preferences;
    private final ConcurrentHashMap<PlayerStatsKey, Object> stats;

    public LotteryPlayer(LotteryPlayerManager manager, UUID player) {
        this.manager = manager;
        this.player = player;
        this.preferences = new ConcurrentHashMap<>();
        this.stats = new ConcurrentHashMap<>();
    }

    public LotteryPlayer(LotteryPlayerManager manager, UUID player, Map<PlayerPreferenceKey, Object> preferences, Map<PlayerStatsKey, Object> stats) {
        this.manager = manager;
        this.player = player;
        this.preferences = new ConcurrentHashMap<>(preferences);
        this.stats = new ConcurrentHashMap<>(stats);
    }

    public LotteryPlayerManager getManager() {
        return manager;
    }

    public void setManager(LotteryPlayerManager manager) {
        this.manager = manager;
    }

    public void save() {
        if (!manager.getInstance().backendBungeecordMode) {
            manager.getInstance().getLotteryPlayerUpdateListener().accept(this);
        }
        new Thread(() -> manager.saveLotteryPlayer(player)).start();
    }

    public UUID getPlayer() {
        return player;
    }

    public void bulkSet(Map<PlayerPreferenceKey, Object> preferences, Map<PlayerStatsKey, Object> stats) {
        for (PlayerPreferenceKey key : PlayerPreferenceKey.values()) {
            this.preferences.compute(key, (k, v) -> preferences.get(key));
        }
        for (PlayerStatsKey key : PlayerStatsKey.values()) {
            this.stats.compute(key, (k, v) -> stats.get(key));
        }
        save();
    }

    public Object getPreference(PlayerPreferenceKey key) {
        return preferences.getOrDefault(key, key.getDefaultValue(manager.getInstance(), player));
    }

    @SuppressWarnings("unchecked")
    public <T> T getPreference(PlayerPreferenceKey key, Class<T> type) {
        return (T) getPreference(key);
    }

    public void setPreference(PlayerPreferenceKey key, Object value) {
        preferences.put(key, value);
        save();
    }

    public boolean isPreferenceSet(PlayerPreferenceKey key) {
        return preferences.containsKey(key);
    }

    public void resetPreference(PlayerPreferenceKey key) {
        preferences.remove(key);
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

    public <T> T updateStats(PlayerStatsKey key, Class<T> type, Predicate<T> predicate, T newValue) {
        return updateStats(key, type, t -> predicate.test(t) ? newValue : t);
    }

    @SuppressWarnings("unchecked")
    public <T> T updateStats(PlayerStatsKey key, Class<T> type, UnaryOperator<T> updateFunction) {
        AtomicReference<T> ref = new AtomicReference<>(null);
        T defaultValue = (T) key.getDefaultValue();
        stats.compute(key, (k, v) -> {
            T t = (T) v;
            ref.set(t);
            return updateFunction.apply(v == null ? defaultValue : t);
        });
        save();
        T t = ref.get();
        return t == null ? defaultValue : t;
    }
}
