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

package com.loohp.lotterysix.game.objects.betnumbers;

import com.google.common.collect.Sets;
import com.loohp.lotterysix.utils.ChatColorUtils;
import org.paukov.combinatorics3.Generator;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class BetNumbers {

    private final Set<Integer> bankers;
    private final Set<Integer> numbers;
    private final BetNumbersType type;

    BetNumbers(Collection<Integer> numbers, BetNumbersType type) {
        this.bankers = null;
        this.numbers = Collections.unmodifiableSet(new TreeSet<>(numbers));
        this.type = type;
    }

    BetNumbers(Collection<Integer> bankers, Collection<Integer> numbers) {
        this.bankers = Collections.unmodifiableSet(new TreeSet<>(bankers));
        this.numbers = Collections.unmodifiableSet(new TreeSet<>(numbers));
        this.type = BetNumbersType.BANKER;
    }

    public Iterator<Integer> iterator() {
        return new Iterator<Integer>() {
            private final Iterator<Integer> itr = numbers.iterator();

            @Override
            public boolean hasNext() {
                return itr.hasNext();
            }

            @Override
            public Integer next() {
                return itr.next();
            }
        };
    }

    public Stream<List<Integer>> combinations() {
        if (hasNoBankers()) {
            return Generator.combination(numbers).simple(6).stream();
        } else {
            return Generator.combination(numbers).simple(6 - bankers.size()).stream().peek(list -> list.addAll(bankers));
        }
    }

    public IntStream stream() {
        return numbers.stream().mapToInt(i -> i);
    }

    public Set<Integer> getNumbers() {
        return numbers;
    }

    public int getNumber(int index) {
        Iterator<Integer> itr = iterator();
        int value = itr.next();
        for (int i = 0; i < index; i++) {
            value = itr.next();
        }
        return value;
    }

    public boolean hasBankers() {
        return !hasNoBankers();
    }

    public boolean hasNoBankers() {
        return bankers == null || bankers.isEmpty();
    }

    public Iterator<Integer> bankersIterator() {
        if (hasNoBankers()) {
            return Collections.emptyIterator();
        }
        return new Iterator<Integer>() {
            private final Iterator<Integer> itr = bankers.iterator();

            @Override
            public boolean hasNext() {
                return itr.hasNext();
            }

            @Override
            public Integer next() {
                return itr.next();
            }
        };
    }

    public IntStream bankersStream() {
        return hasNoBankers() ? IntStream.empty() : bankers.stream().mapToInt(i -> i);
    }

    public Set<Integer> getBankersNumbers() {
        return hasNoBankers() ? Collections.emptySet() : bankers;
    }

    public int getBankerNumber(int index) {
        Iterator<Integer> itr = bankersIterator();
        int value = itr.next();
        for (int i = 0; i < index; i++) {
            value = itr.next();
        }
        return value;
    }

    public Set<Integer> getAllNumbers() {
        return hasNoBankers() ? numbers : Sets.union(bankers, numbers);
    }

    public BetNumbersType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BetNumbers numbers1 = (BetNumbers) o;
        return Objects.equals(bankers, numbers1.bankers) && numbers.equals(numbers1.numbers) && type == numbers1.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bankers, numbers, type);
    }

    @Override
    public String toString() {
        String numbersString = numbers.stream().map(each -> each.toString()).collect(Collectors.joining(" "));
        if (hasNoBankers()) {
            return numbersString;
        }
        return bankers.stream().map(each -> each.toString()).collect(Collectors.joining(" ")) + " > " + numbersString;
    }

    public String toColoredString() {
        String numbersString = numbers.stream().map(each -> ChatColorUtils.getNumberColor(each) + each.toString()).collect(Collectors.joining(" "));
        if (hasNoBankers()) {
            return numbersString;
        }
        return bankers.stream().map(each -> ChatColorUtils.getNumberColor(each) + each.toString()).collect(Collectors.joining(" ")) + " \u00a75> " + numbersString;
    }

}
