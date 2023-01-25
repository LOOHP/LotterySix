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

package com.loohp.lotterysix.game.player;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.loohp.lotterysix.game.LotterySix;
import com.loohp.lotterysix.game.objects.LotteryPlayer;
import com.loohp.lotterysix.game.objects.PlayerPreferenceKey;
import com.loohp.lotterysix.game.objects.PlayerStatsKey;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LotteryPlayerManager {

    private final LotterySix instance;
    private final Map<UUID, WeakReference<LotteryPlayer>> loadedPlayers;
    private final Set<LotteryPlayer> persistentReferences;

    public LotteryPlayerManager(LotterySix instance) {
        this.instance = instance;
        this.loadedPlayers = new ConcurrentHashMap<>();
        this.persistentReferences = ConcurrentHashMap.newKeySet();
    }

    public LotteryPlayer getLotteryPlayer(UUID player) {
        return getLotteryPlayer(player, true);
    }

    public LotteryPlayer getLotteryPlayer(UUID player, boolean loadIfNeeded) {
        WeakReference<LotteryPlayer> preference = loadedPlayers.get(player);
        if (preference == null || preference.get() == null) {
            if (loadIfNeeded) {
                return loadLotteryPlayer(player, false);
            }
            loadedPlayers.remove(player);
            return null;
        }
        return preference.get();
    }

    public boolean isLotteryPlayerLoaded(UUID player) {
        LotteryPlayer lotteryPlayer = getLotteryPlayer(player, false);
        if (lotteryPlayer == null) {
            return false;
        }
        return persistentReferences.contains(lotteryPlayer);
    }

    public synchronized LotteryPlayer loadLotteryPlayer(UUID player, boolean persist) {
        LotteryPlayer loaded = getLotteryPlayer(player, false);
        if (loaded != null) {
            if (persist) {
                persistentReferences.add(loaded);
            }
            return loaded;
        }
        File playerFolder = new File(instance.getDataFolder(), "player");
        playerFolder.mkdirs();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        File file = new File(playerFolder, player + ".json");
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8))) {
                JsonObject json = gson.fromJson(reader, JsonObject.class);

                Map<PlayerPreferenceKey, Object> preferences = new EnumMap<>(PlayerPreferenceKey.class);
                JsonObject preferencesJson = json.getAsJsonObject("preferences");

                for (PlayerPreferenceKey key : PlayerPreferenceKey.values()) {
                    JsonElement element = preferencesJson.get(key.name());
                    if (element != null) {
                        preferences.put(key, gson.fromJson(element, key.getValueTypeClass()));
                    }
                }

                JsonObject statsJson = json.getAsJsonObject("stats");
                Map<PlayerStatsKey, Object> stats = new EnumMap<>(PlayerStatsKey.class);

                for (PlayerStatsKey key : PlayerStatsKey.values()) {
                    JsonElement element = statsJson.get(key.name());
                    if (element != null) {
                        stats.put(key, gson.fromJson(element, key.getValueTypeClass()));
                    }
                }

                LotteryPlayer preference = new LotteryPlayer(this, UUID.fromString(json.get("player").getAsString()), preferences, stats);

                preference.setManager(this);
                loadedPlayers.put(player, new WeakReference<>(preference));
                if (persist) {
                    persistentReferences.add(preference);
                }
                return preference;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            LotteryPlayer preference = new LotteryPlayer(this, player);
            loadedPlayers.put(player, new WeakReference<>(preference));
            if (persist) {
                persistentReferences.add(preference);
            }
            saveLotteryPlayer(player);
            return preference;
        }
        return null;
    }

    public synchronized void saveLotteryPlayer(UUID player) {
        LotteryPlayer preference = getLotteryPlayer(player, false);
        if (preference != null) {
            File playerFolder = new File(instance.getDataFolder(), "player");
            playerFolder.mkdirs();
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            File file = new File(playerFolder, player + ".json");
            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8))) {
                pw.println(gson.toJson(preference));
                pw.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void unloadLotteryPlayer(UUID player, boolean save) {
        if (save) {
            saveLotteryPlayer(player);
        }
        LotteryPlayer lotteryPlayer = getLotteryPlayer(player, false);
        if (lotteryPlayer != null) {
            persistentReferences.remove(lotteryPlayer);
        }
        loadedPlayers.remove(player);
    }

}
