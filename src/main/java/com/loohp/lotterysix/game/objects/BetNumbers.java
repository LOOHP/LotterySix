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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BetNumbers implements Iterable<Integer> {

    private final Set<Integer> numbers;

    private BetNumbers(Collection<Integer> numbers) {
        this.numbers = Collections.unmodifiableSet(new TreeSet<>(numbers));
    }

    public BetNumbers(int number1, int number2, int number3, int number4, int number5, int number6) {
        this(Arrays.asList(number1, number2, number3, number4, number5, number6));
    }

    @Override
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BetNumbers integers = (BetNumbers) o;
        return numbers.equals(integers.numbers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numbers);
    }

    @Override
    public String toString() {
        return numbers.stream().map(each -> each.toString()).collect(Collectors.joining(" "));
    }

    public static class Builder {

        public static Builder builder() {
            return new Builder();
        }

        private final int max;
        private final List<Integer> numbers;

        private Builder() {
            this.max = 6;
            this.numbers = new ArrayList<>(max);
        }

        public synchronized Builder addNumber(int number) {
            if (completed()) {
                throw new IllegalStateException("Lottery Number builder already completed!");
            }
            numbers.add(number);
            return this;
        }

        public synchronized Builder removeNumber(int number) {
            numbers.remove((Object) number);
            return this;
        }

        public boolean contains(int i) {
            return numbers.contains(i);
        }

        public boolean completed() {
            return numbers.size() >= max;
        }

        public BetNumbers build() {
            if (!completed()) {
                throw new IllegalStateException("Lottery Number builder not yet completed!");
            }
            return new BetNumbers(numbers);
        }

    }


}
