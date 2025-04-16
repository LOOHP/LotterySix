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

package com.loohp.lotterysix.game.player;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.loohp.lotterysix.game.LotterySix;
import com.loohp.lotterysix.game.objects.PlayerBets;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LotteryPlayerManager {

    public static final Gson GSON = LotterySix.GSON;

    private final LotterySix instance;
    private final Map<UUID, WeakReference<LotteryPlayer>> loadedPlayers;
    private final Set<LotteryPlayer> persistentReferences;

    public LotteryPlayerManager(LotterySix instance) {
        this.instance = instance;
        this.loadedPlayers = new ConcurrentHashMap<>();
        this.persistentReferences = ConcurrentHashMap.newKeySet();
    }

    public LotterySix getInstance() {
        return instance;
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

    public Collection<UUID> getAllLotteryPlayerUUIDs() {
        File playerFolder = new File(instance.getDataFolder(), "player");
        playerFolder.mkdirs();
        File[] files = playerFolder.listFiles();
        Set<UUID> lotteryPlayers = new HashSet<>(files.length + loadedPlayers.size());
        lotteryPlayers.addAll(loadedPlayers.keySet());
        for (File file : files) {
            String fileName = file.getName();
            if (fileName.endsWith(".json")) {
                try {
                    lotteryPlayers.add(UUID.fromString(fileName.substring(0, fileName.lastIndexOf("."))));
                } catch (IllegalArgumentException ignore) {
                }
            }
        }
        return lotteryPlayers;
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
        File file = new File(playerFolder, player + ".json");
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8))) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);

                Map<PlayerPreferenceKey, Object> preferences = new EnumMap<>(PlayerPreferenceKey.class);
                JsonObject preferencesJson = json.getAsJsonObject("preferences");

                for (PlayerPreferenceKey key : PlayerPreferenceKey.values()) {
                    JsonElement element = preferencesJson.get(key.name());
                    if (element != null) {
                        preferences.put(key, GSON.fromJson(element, key.getValueTypeClass()));
                    }
                }

                JsonObject statsJson = json.getAsJsonObject("stats");
                Map<PlayerStatsKey, Object> stats = new EnumMap<>(PlayerStatsKey.class);

                for (PlayerStatsKey key : PlayerStatsKey.values()) {
                    JsonElement element = statsJson.get(key.name());
                    if (element != null) {
                        stats.put(key, GSON.fromJson(element, key.getValueTypeClass()));
                    }
                }

                List<PlayerBets> multipleDrawPlayerBets = GSON.fromJson(json.getAsJsonArray("multipleDrawPlayerBets"), new TypeToken<ArrayList<PlayerBets>>(){}.getType());

                LotteryPlayer lotteryPlayer = new LotteryPlayer(this, UUID.fromString(json.get("player").getAsString()), preferences, stats, multipleDrawPlayerBets == null ? Collections.emptyList() : multipleDrawPlayerBets);

                lotteryPlayer.setManager(this);
                loadedPlayers.put(player, new WeakReference<>(lotteryPlayer));
                if (persist) {
                    persistentReferences.add(lotteryPlayer);
                }
                return lotteryPlayer;
            } catch (Exception e) {
                new RuntimeException("Error while reading data for lottery player " + player, e).printStackTrace();
                try {
                    Files.copy(file.toPath(), new File(playerFolder, player + ".json.bak." + System.currentTimeMillis()).toPath());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        LotteryPlayer lotteryPlayer = new LotteryPlayer(this, player);
        loadedPlayers.put(player, new WeakReference<>(lotteryPlayer));
        if (persist) {
            persistentReferences.add(lotteryPlayer);
        }
        saveLotteryPlayer(player);
        return lotteryPlayer;
    }

    public synchronized void saveLotteryPlayer(UUID player) {
        LotteryPlayer lotteryPlayer = getLotteryPlayer(player, false);
        if (lotteryPlayer != null) {
            File playerFolder = new File(instance.getDataFolder(), "player");
            playerFolder.mkdirs();
            File file = new File(playerFolder, player + ".json");
            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8))) {
                pw.println(GSON.toJson(lotteryPlayer));
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
