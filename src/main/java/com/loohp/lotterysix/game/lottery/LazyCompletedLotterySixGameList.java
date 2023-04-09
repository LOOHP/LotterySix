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

package com.loohp.lotterysix.game.lottery;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterator;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LazyCompletedLotterySixGameList extends AbstractList<CompletedLotterySixGame> {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static CompletedLotterySixGame loadFromFile(File file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8))) {
            return GSON.fromJson(reader, CompletedLotterySixGame.class);
        } catch (IOException e) {
            throw new IllegalStateException("Do not remove LotterySix game data from the file system while the server is running, please restart the server now", e);
        }
    }

    private final File lotteryDataFolder;
    private final List<CompletedLotterySixGameIndex> gameIndexes;
    private final Map<UUID, CompletedLotterySixGame> cachedGames;
    private final Map<UUID, CompletedLotterySixGame> dirtyGames;

    public LazyCompletedLotterySixGameList(File lotteryDataFolder) {
        this.lotteryDataFolder = lotteryDataFolder;
        this.gameIndexes = Collections.synchronizedList(new ArrayList<>());
        Cache<UUID, CompletedLotterySixGame> cache = CacheBuilder.newBuilder().maximumSize(20).build();
        this.cachedGames = cache.asMap();
        this.dirtyGames = new ConcurrentHashMap<>();
    }

    public Object getIterateLock() {
        return gameIndexes;
    }
    
    private CompletedLotterySixGame lookForCached(UUID gameId) {
        CompletedLotterySixGame game = dirtyGames.get(gameId);
        if (game != null) {
            return game;
        }
        return cachedGames.get(gameId);
    }

    public void setGameDirty(UUID gameId) {
        CompletedLotterySixGame game = lookForCached(gameId);
        if (game != null) {
            cachedGames.remove(gameId);
            dirtyGames.put(gameId, game);
        }
    }

    public Iterator<CompletedLotterySixGame> dirtyGamesIterator() {
        return dirtyGames.values().iterator();
    }

    public Iterable<CompletedLotterySixGame> dirtyGamesIterable() {
        return () -> dirtyGamesIterator();
    }

    @Override
    public CompletedLotterySixGame get(int index) {
        CompletedLotterySixGameIndex gameIndex = gameIndexes.get(index);
        CompletedLotterySixGame game = lookForCached(gameIndex.getGameId());
        if (game != null) {
            return game;
        }
        game = loadFromFile(new File(lotteryDataFolder, gameIndex.getDataFileName()));
        cachedGames.put(gameIndex.getGameId(), game);
        return game;
    }

    public CompletedLotterySixGame get(CompletedLotterySixGameIndex gameIndex) {
        CompletedLotterySixGame game = lookForCached(gameIndex.getGameId());
        if (game != null) {
            return game;
        }
        return get(gameIndexes.indexOf(gameIndex));
    }

    public CompletedLotterySixGame get(GameNumber gameNumber) {
        int i = 0;
        synchronized (getIterateLock()) {
            for (CompletedLotterySixGameIndex gameIndex : gameIndexes) {
                GameNumber number = gameIndex.getGameNumber();
                if (Objects.equals(number, gameNumber)) {
                    break;
                }
                i++;
            }
        }
        return get(i);
    }

    public CompletedLotterySixGameIndex getIndex(int index) {
        return gameIndexes.get(index);
    }

    @Override
    public int size() {
        return gameIndexes.size();
    }

    @Override
    public CompletedLotterySixGame set(int index, CompletedLotterySixGame element) {
        CompletedLotterySixGame lastGame = get(index);
        cachedGames.remove(lastGame.getGameId());
        CompletedLotterySixGameIndex gameIndex = element.toGameIndex();
        gameIndexes.set(index, gameIndex);
        dirtyGames.put(gameIndex.getGameId(), element);
        return lastGame;
    }

    public CompletedLotterySixGame set(int index, CompletedLotterySixGameIndex gameIndex) {
        CompletedLotterySixGame lastGame = get(index);
        cachedGames.remove(lastGame.getGameId());
        gameIndexes.set(index, gameIndex);
        return lastGame;
    }

    @Override
    public void add(int index, CompletedLotterySixGame element) {
        CompletedLotterySixGameIndex gameIndex = element.toGameIndex();
        gameIndexes.add(index, gameIndex);
        dirtyGames.put(gameIndex.getGameId(), element);
    }

    public void add(CompletedLotterySixGameIndex gameIndex) {
        add(size(), gameIndex);
    }

    public void add(int index, CompletedLotterySixGameIndex gameIndex) {
        gameIndexes.add(index, gameIndex);
    }

    @Override
    public CompletedLotterySixGame remove(int index) {
        CompletedLotterySixGame game = get(index);
        CompletedLotterySixGameIndex gameIndex = gameIndexes.remove(index);
        cachedGames.remove(gameIndex.getGameId());
        return game;
    }

    public Iterator<CompletedLotterySixGameIndex> indexIterator() {
        return new Iterator<CompletedLotterySixGameIndex>() {
            int index = -1;

            @Override
            public boolean hasNext() {
                return (index + 1) < size();
            }

            @Override
            public CompletedLotterySixGameIndex next() {
                return gameIndexes.get(++index);
            }

            @Override
            public void remove() {
                LazyCompletedLotterySixGameList.this.remove(index);
            }
        };
    }

    public Iterable<CompletedLotterySixGameIndex> indexIterable() {
        return () -> indexIterator();
    }

    public Spliterator<CompletedLotterySixGameIndex> indexSpliterator() {
        return gameIndexes.spliterator();
    }

    public Stream<CompletedLotterySixGameIndex> indexStream() {
        return gameIndexes.stream();
    }

    public Stream<CompletedLotterySixGameIndex> indexParallelStream() {
        return gameIndexes.parallelStream();
    }

    public int indexOf(CompletedLotterySixGameIndex gameIndex) {
        return gameIndexes.indexOf(gameIndex);
    }

    public List<CompletedLotterySixGameIndex> query(CompletedLotteryGamesQuery query, TimeZone timeZone) {
        return query(query, -1, timeZone);
    }

    public List<CompletedLotterySixGameIndex> query(CompletedLotteryGamesQuery query, int numberOfGames, TimeZone timeZone) {
        Stream<CompletedLotterySixGameIndex> stream = queryStream(query, timeZone);
        if (numberOfGames >= 0) {
            stream = stream.limit(numberOfGames);
        }
        synchronized (getIterateLock()) {
            return stream.collect(Collectors.toList());
        }
    }

    public Stream<CompletedLotterySixGameIndex> queryStream(CompletedLotteryGamesQuery query, TimeZone timeZone) {
        Predicate<CompletedLotterySixGameIndex> predicate = query.getQueryPredicate(this, timeZone);
        return indexStream().filter(predicate);
    }

    @Deprecated
    @Override
    public void sort(Comparator<? super CompletedLotterySixGame> comparator) {
        super.sort(comparator);
    }

    public void indexSort(Comparator<? super CompletedLotterySixGameIndex> comparator) {
        gameIndexes.sort(comparator);
    }

}
