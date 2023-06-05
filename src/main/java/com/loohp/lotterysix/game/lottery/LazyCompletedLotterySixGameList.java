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

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LazyCompletedLotterySixGameList implements List<CompletedLotterySixGame>, RandomAccess {

    private final Function<CompletedLotterySixGameIndex, CompletedLotterySixGame> gameLoader;
    private final List<CompletedLotterySixGameIndex> gameIndexes;
    private final Map<UUID, CompletedLotterySixGame> cachedGames;
    private final Map<UUID, CompletedLotterySixGame> dirtyGames;
    private final Map<UUID, Object> gameLoadingLock;

    public LazyCompletedLotterySixGameList(Function<CompletedLotterySixGameIndex, CompletedLotterySixGame> gameLoader) {
        this.gameLoader = gameLoader;
        this.gameIndexes = new CopyOnWriteArrayList<>();
        Cache<UUID, CompletedLotterySixGame> cache = CacheBuilder.newBuilder().maximumSize(20).build();
        this.cachedGames = cache.asMap();
        this.dirtyGames = new ConcurrentHashMap<>();
        Cache<UUID, Object> loadingLock = CacheBuilder.newBuilder().weakValues().build();
        this.gameLoadingLock = loadingLock.asMap();
    }

    private LazyCompletedLotterySixGameList(Function<CompletedLotterySixGameIndex, CompletedLotterySixGame> gameLoader, List<CompletedLotterySixGameIndex> gameIndexes, Map<UUID, CompletedLotterySixGame> cachedGames, Map<UUID, CompletedLotterySixGame> dirtyGames, Map<UUID, Object> gameLoadingLock) {
        this.gameLoader = gameLoader;
        this.gameIndexes = gameIndexes;
        this.cachedGames = cachedGames;
        this.dirtyGames = dirtyGames;
        this.gameLoadingLock = gameLoadingLock;
    }

    private Object getGameLoadingLock(UUID gameId) {
        return gameLoadingLock.computeIfAbsent(gameId, k -> new Object());
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

    public CompletedLotterySixGame getLatest() {
        if (gameIndexes.isEmpty()) {
            return null;
        }
        return get(gameIndexes.get(0));
    }

    @Override
    public CompletedLotterySixGame get(int index) {
        CompletedLotterySixGameIndex gameIndex = gameIndexes.get(index);
        if (gameIndex == null) {
            return null;
        }
        return get(gameIndex);
    }

    public CompletedLotterySixGame get(CompletedLotterySixGameIndex gameIndex) {
        UUID gameId = gameIndex.getGameId();
        synchronized (getGameLoadingLock(gameId)) {
            CompletedLotterySixGame game = lookForCached(gameId);
            if (game != null) {
                return game;
            }
            game = gameLoader.apply(gameIndex);
            cachedGames.put(gameIndex.getGameId(), game);
            return game;
        }
    }

    public CompletedLotterySixGame get(GameNumber gameNumber) {
        for (CompletedLotterySixGameIndex gameIndex : gameIndexes) {
            GameNumber number = gameIndex.getGameNumber();
            if (Objects.equals(number, gameNumber)) {
                return get(gameIndex);
            }
        }
        return null;
    }

    public CompletedLotterySixGameIndex getIndex(int index) {
        return gameIndexes.get(index);
    }

    @Override
    public int size() {
        return gameIndexes.size();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean contains(Object o) {
        if (o == null || o instanceof CompletedLotterySixGameIndex) {
            return gameIndexes.contains(o);
        } else if (o instanceof CompletedLotterySixGame) {
            return gameIndexes.contains(((CompletedLotterySixGame) o).toGameIndex());
        } else {
            return false;
        }
    }

    public boolean contains(CompletedLotterySixGameIndex o) {
        return gameIndexes.contains(o);
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
    public boolean add(CompletedLotterySixGame element) {
        CompletedLotterySixGameIndex gameIndex = element.toGameIndex();
        dirtyGames.put(gameIndex.getGameId(), element);
        return gameIndexes.add(gameIndex);
    }

    @Override
    public void add(int index, CompletedLotterySixGame element) {
        CompletedLotterySixGameIndex gameIndex = element.toGameIndex();
        gameIndexes.add(index, gameIndex);
        dirtyGames.put(gameIndex.getGameId(), element);
    }

    public boolean addUnloaded(CompletedLotterySixGameIndex gameIndex) {
        return gameIndexes.add(gameIndex);
    }

    public void addUnloaded(int index, CompletedLotterySixGameIndex gameIndex) {
        gameIndexes.add(index, gameIndex);
    }

    @Override
    public boolean remove(Object o) {
        if (o == null || o instanceof CompletedLotterySixGameIndex) {
            return gameIndexes.remove(o);
        } else if (o instanceof CompletedLotterySixGame) {
            return gameIndexes.remove(((CompletedLotterySixGame) o).toGameIndex());
        } else {
            return false;
        }
    }

    public boolean remove(CompletedLotterySixGameIndex o) {
        return gameIndexes.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return c.stream().allMatch(o -> contains(o));
    }

    @Override
    public boolean addAll(Collection<? extends CompletedLotterySixGame> c) {
        boolean result = false;
        for (CompletedLotterySixGame game : c) {
            result |= add(game);
        }
        return result;
    }

    @Override
    public boolean addAll(int index, Collection<? extends CompletedLotterySixGame> c) {
        boolean result = false;
        for (CompletedLotterySixGame game : c) {
            add(index++, game);
            result = true;
        }
        return result;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean result = false;
        for (Object o : c) {
            result |= remove(o);
        }
        return result;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean result = false;
        Optional<?> optSample = c.stream().filter(o -> o != null).findAny();
        if (!optSample.isPresent()) {
            if (c.isEmpty()) {
                result = !isEmpty();
                clear();
                return result;
            } else {
                return gameIndexes.retainAll(Collections.singleton(null));
            }
        }
        Object sample = optSample.get();
        if (sample instanceof CompletedLotterySixGameIndex) {
            for (CompletedLotterySixGameIndex gameIndex : gameIndexes) {
                if (!c.contains(gameIndex)) {
                    result |= remove(gameIndex);
                }
            }
            return result;
        } else if (sample instanceof CompletedLotterySixGame) {
            c = c.stream().map(g -> ((CompletedLotterySixGame) g).toGameIndex()).collect(Collectors.toSet());
            for (CompletedLotterySixGameIndex gameIndex : gameIndexes) {
                if (!c.contains(gameIndex)) {
                    result |= remove(gameIndex);
                }
            }
            return result;
        } else {
            result = !isEmpty();
            clear();
            return result;
        }
    }

    @Override
    public CompletedLotterySixGame remove(int index) {
        CompletedLotterySixGame game = get(index);
        CompletedLotterySixGameIndex gameIndex = gameIndexes.remove(index);
        cachedGames.remove(gameIndex.getGameId());
        return game;
    }

    @Override
    public int indexOf(Object o) {
        if (o == null || o instanceof CompletedLotterySixGameIndex) {
            return gameIndexes.indexOf(o);
        } else if (o instanceof CompletedLotterySixGame) {
            return gameIndexes.indexOf(((CompletedLotterySixGame) o).toGameIndex());
        } else {
            return -1;
        }
    }

    @Override
    public int lastIndexOf(Object o) {
        if (o == null || o instanceof CompletedLotterySixGameIndex) {
            return gameIndexes.lastIndexOf(o);
        } else if (o instanceof CompletedLotterySixGame) {
            return gameIndexes.lastIndexOf(((CompletedLotterySixGame) o).toGameIndex());
        } else {
            return -1;
        }
    }

    @Override
    public ListIterator<CompletedLotterySixGame> listIterator() {
        return listIterator(0);
    }

    @Override
    public ListIterator<CompletedLotterySixGame> listIterator(int index) {
        return new ListIterator<CompletedLotterySixGame>() {

            private final ListIterator<CompletedLotterySixGameIndex> itr = indexListIterator();

            @Override
            public boolean hasNext() {
                return itr.hasNext();
            }

            @Override
            public CompletedLotterySixGame next() {
                return get(itr.next());
            }

            @Override
            public boolean hasPrevious() {
                return itr.hasPrevious();
            }

            @Override
            public CompletedLotterySixGame previous() {
                return get(itr.previous());
            }

            @Override
            public int nextIndex() {
                return itr.nextIndex();
            }

            @Override
            public int previousIndex() {
                return itr.previousIndex();
            }

            @Override
            public void remove() {
                itr.remove();
            }

            @Override
            public void set(CompletedLotterySixGame completedLotterySixGame) {
                itr.set(completedLotterySixGame.toGameIndex());
            }

            @Override
            public void add(CompletedLotterySixGame completedLotterySixGame) {
                itr.add(completedLotterySixGame.toGameIndex());
            }
        };
    }

    public ListIterator<CompletedLotterySixGameIndex> indexListIterator() {
        return indexListIterator(0);
    }

    public ListIterator<CompletedLotterySixGameIndex> indexListIterator(int index) {
        return gameIndexes.listIterator(index);
    }

    @Override
    public LazyCompletedLotterySixGameList subList(int fromIndex, int toIndex) {
        return new LazyCompletedLotterySixGameList(gameLoader, gameIndexes.subList(fromIndex, toIndex), cachedGames, dirtyGames, gameLoadingLock);
    }

    @Override
    public Iterator<CompletedLotterySixGame> iterator() {
        return new Iterator<CompletedLotterySixGame>() {

            private final Iterator<CompletedLotterySixGameIndex> itr = indexIterator();

            @Override
            public boolean hasNext() {
                return itr.hasNext();
            }

            @Override
            public CompletedLotterySixGame next() {
                return get(itr.next());
            }

            @Override
            public void remove() {
                itr.remove();
            }
        };
    }

    @Deprecated
    @Override
    public Object[] toArray() {
        return indexStream().map(this::get).toArray();
    }

    @SuppressWarnings({"unchecked", "SuspiciousSystemArraycopy"})
    @Deprecated
    @Override
    public <T> T[] toArray(T[] a) {
        Object[] array = toArray();
        if (a.length < array.length) {
            a = (T[]) Array.newInstance(a.getClass().getComponentType(), array.length);
        }
        System.arraycopy(array, 0, a, 0, array.length);
        return a;
    }

    @Override
    public Stream<CompletedLotterySixGame> stream() {
        return indexStream().map(this::get);
    }

    @Override
    public Stream<CompletedLotterySixGame> parallelStream() {
        return indexParallelStream().map(this::get);
    }

    public Iterator<CompletedLotterySixGameIndex> indexIterator() {
        return gameIndexes.iterator();
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

    public List<CompletedLotterySixGameIndex> query(CompletedLotteryGamesQuery query, TimeZone timeZone) {
        return query(query, -1, timeZone);
    }

    public List<CompletedLotterySixGameIndex> query(CompletedLotteryGamesQuery query, int numberOfGames, TimeZone timeZone) {
        Stream<CompletedLotterySixGameIndex> stream = queryStream(query, timeZone);
        if (numberOfGames >= 0) {
            stream = stream.limit(numberOfGames);
        }
        return stream.collect(Collectors.toList());
    }

    public Stream<CompletedLotterySixGameIndex> queryStream(CompletedLotteryGamesQuery query, TimeZone timeZone) {
        Predicate<CompletedLotterySixGameIndex> predicate = query.getQueryPredicate(this, timeZone);
        return indexStream().filter(predicate);
    }

    @Override
    public void clear() {
        for (CompletedLotterySixGameIndex gameIndex : gameIndexes) {
            remove(gameIndex);
        }
    }

    @Deprecated
    @Override
    public void sort(Comparator<? super CompletedLotterySixGame> comparator) {
        indexSort(Comparator.comparing(this::get, comparator));
    }

    public void indexSort(Comparator<? super CompletedLotterySixGameIndex> comparator) {
        gameIndexes.sort(comparator);
    }

}
