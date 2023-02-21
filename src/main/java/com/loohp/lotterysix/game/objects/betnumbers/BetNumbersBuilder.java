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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public abstract class BetNumbersBuilder {

    public static BetNumbersBuilder fromString(int minNumber, int maxNumber, String input) {
        String[] sections = input.split(" ");
        Set<Integer> bankers = null;
        Set<Integer> numbers = new LinkedHashSet<>();
        for (String section : sections) {
            if (!section.isEmpty()) {
                if (section.equals(">")) {
                    if (numbers.isEmpty() || numbers.size() > 5) {
                        return null;
                    } else {
                        bankers = numbers;
                        numbers = new LinkedHashSet<>();
                    }
                } else {
                    try {
                        int number = Integer.parseInt(section);
                        if (number < minNumber || number > maxNumber) {
                            return null;
                        }
                        if (!numbers.add(number)) {
                            return null;
                        }
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
            }
        }
        if (bankers == null) {
            BetNumbersBuilder builder;
            if (numbers.size() < 6) {
                return null;
            } else if (numbers.size() == 6) {
                builder = new SingleBuilder(minNumber, maxNumber);
            } else {
                builder = new MultipleBuilder(minNumber, maxNumber);
            }
            for (int number : numbers) {
                builder.addNumber(number);
            }
            return builder;
        } else {
            if (numbers.isEmpty() || bankers.size() + numbers.size() < 7) {
                return null;
            }
            BankerBuilder builder = new BankerBuilder(minNumber, maxNumber);
            for (int number : bankers) {
                builder.addNumber(number);
            }
            builder.finishBankers();
            for (int number : numbers) {
                if (builder.contains(number)) {
                    return null;
                }
                builder.addNumber(number);
            }
            return builder;
        }
    }

    public static SingleBuilder single(int minNumber, int maxNumber) {
        return new BetNumbersBuilder.SingleBuilder(minNumber, maxNumber);
    }

    public static MultipleBuilder multiple(int minNumber, int maxNumber) {
        return new BetNumbersBuilder.MultipleBuilder(minNumber, maxNumber);
    }

    public static BankerBuilder banker(int minNumber, int maxNumber) {
        return new BetNumbersBuilder.BankerBuilder(minNumber, maxNumber);
    }

    public static RandomBuilder random(int minNumber, int maxNumber) {
        return new BetNumbersBuilder.RandomBuilder(minNumber, maxNumber);
    }

    public static Stream<RandomBuilder> random(int minNumber, int maxNumber, int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("count cannot be 0 or negative");
        } else if (count % 10 == 0) {
            return IntStream.range(0, count / 10).mapToObj(i -> new BetNumbersBuilder.RandomBuilder(minNumber, maxNumber, 10));
        } else if (count % 5 == 0) {
            return IntStream.range(0, count / 5).mapToObj(i -> new BetNumbersBuilder.RandomBuilder(minNumber, maxNumber, 5));
        } else if (count % 2 == 0) {
            return IntStream.range(0, count / 2).mapToObj(i -> new BetNumbersBuilder.RandomBuilder(minNumber, maxNumber, 2));
        } else {
            return IntStream.range(0, count).mapToObj(i -> new BetNumbersBuilder.RandomBuilder(minNumber, maxNumber, 1));
        }
    }

    protected final int minNumber;
    protected final int maxNumber;
    protected final BetNumbersType type;

    protected BetNumbersBuilder(int minNumber, int maxNumber, BetNumbersType type) {
        this.minNumber = minNumber;
        this.maxNumber = maxNumber;
        this.type = type;
    }

    protected void checkBound(int number) {
        if (number < minNumber || number > maxNumber) {
            throw new IllegalArgumentException("number out of bound");
        }
    }

    public BetNumbersType getType() {
        return type;
    }

    public abstract BetNumbersBuilder addNumber(int number);

    public abstract IntObjectPair<BetNumbersBuilder> addRandomNumber();

    public abstract BetNumbersBuilder removeNumber(int number);

    public abstract int size();

    public abstract boolean contains(int i);

    public boolean canAdd() {
        return size() < maxNumber - minNumber + 1;
    }

    public int setsSize() {
        return 1;
    }

    public abstract boolean completed();

    public abstract BetNumbers build();

    public static class SingleBuilder extends BetNumbersBuilder {

        private final int max;
        private final List<Integer> numbers;

        private SingleBuilder(int minNumber, int maxNumber) {
            super(minNumber, maxNumber, BetNumbersType.SINGLE);
            this.max = 6;
            this.numbers = new ArrayList<>(max);
        }

        @Override
        public synchronized SingleBuilder addNumber(int number) {
            checkBound(number);
            if (completed()) {
                throw new IllegalStateException("Lottery Number builder already completed!");
            }
            numbers.add(number);
            return this;
        }

        @Override
        public IntObjectPair<BetNumbersBuilder> addRandomNumber() {
            if (completed()) {
                throw new IllegalStateException("Lottery Number builder already completed!");
            }
            if (numbers.size() > maxNumber - minNumber) {
                throw new IllegalStateException("Cannot add more random numbers!");
            }
            int number = ThreadLocalRandom.current().ints(minNumber, maxNumber + 1).filter(i -> !numbers.contains(i)).findFirst().orElseThrow(() -> new RuntimeException());
            numbers.add(number);
            return IntObjectPair.of(number, this);
        }

        @Override
        public synchronized SingleBuilder removeNumber(int number) {
            numbers.remove((Object) number);
            return this;
        }

        @Override
        public int size() {
            return numbers.size();
        }

        @Override
        public boolean contains(int i) {
            return numbers.contains(i);
        }

        @Override
        public boolean completed() {
            return numbers.size() >= max;
        }

        @Override
        public BetNumbers build() {
            if (!completed()) {
                throw new IllegalStateException("Lottery Number builder not yet completed!");
            }
            return new BetNumbers(numbers, BetNumbersType.SINGLE);
        }

    }

    public static class MultipleBuilder extends BetNumbersBuilder {

        private final int min;
        private final List<Integer> numbers;

        private MultipleBuilder(int minNumber, int maxNumber) {
            super(minNumber, maxNumber, BetNumbersType.MULTIPLE);
            this.min = 7;
            this.numbers = new ArrayList<>();
        }

        @Override
        public synchronized MultipleBuilder addNumber(int number) {
            checkBound(number);
            numbers.add(number);
            return this;
        }

        @Override
        public IntObjectPair<BetNumbersBuilder> addRandomNumber() {
            if (numbers.size() > maxNumber - minNumber) {
                throw new IllegalStateException("Cannot add more random numbers!");
            }
            int number = ThreadLocalRandom.current().ints(minNumber, maxNumber + 1).filter(i -> !numbers.contains(i)).findFirst().orElseThrow(() -> new RuntimeException());
            numbers.add(number);
            return IntObjectPair.of(number, this);
        }

        @Override
        public synchronized MultipleBuilder removeNumber(int number) {
            numbers.remove((Object) number);
            return this;
        }

        @Override
        public int size() {
            return numbers.size();
        }

        @Override
        public boolean contains(int i) {
            return numbers.contains(i);
        }

        @Override
        public boolean completed() {
            return numbers.size() >= min;
        }

        @Override
        public BetNumbers build() {
            if (!completed()) {
                throw new IllegalStateException("Lottery Number builder not yet completed!");
            }
            return new BetNumbers(numbers, BetNumbersType.MULTIPLE);
        }

    }

    public static class BankerBuilder extends BetNumbersBuilder {

        private final int maxBankers;
        private final List<Integer> bankers;
        private final List<Integer> selections;
        private boolean bankersComplete;

        private BankerBuilder(int minNumber, int maxNumber) {
            super(minNumber, maxNumber, BetNumbersType.BANKER);
            this.maxBankers = 5;
            this.bankers = new ArrayList<>();
            this.selections = new ArrayList<>();
            this.bankersComplete = false;
        }

        @Override
        public synchronized BankerBuilder addNumber(int number) {
            checkBound(number);
            if (bankersComplete) {
                if (!bankers.contains(number)) {
                    selections.add(number);
                }
            } else {
                if (bankerCompleted()) {
                    throw new IllegalStateException("Max numbers of bankers reached!");
                }
                bankers.add(number);
            }
            return this;
        }

        @Override
        public IntObjectPair<BetNumbersBuilder> addRandomNumber() {
            if (bankers.size() + selections.size() > maxNumber - minNumber) {
                throw new IllegalStateException("Cannot add more random numbers!");
            }
            int number = ThreadLocalRandom.current().ints(minNumber, maxNumber + 1).filter(i -> !contains(i)).findFirst().orElseThrow(() -> new RuntimeException());
            if (bankersComplete) {
                if (!bankers.contains(number)) {
                    selections.add(number);
                }
            } else {
                if (bankerCompleted()) {
                    throw new IllegalStateException("Max numbers of bankers reached!");
                }
                bankers.add(number);
            }
            return IntObjectPair.of(number, this);
        }

        @Override
        public synchronized BankerBuilder removeNumber(int number) {
            if (bankersComplete) {
                selections.remove((Object) number);
            } else {
                bankers.remove((Object) number);
            }
            return this;
        }

        @Override
        public int size() {
            return selections.size();
        }

        public int bankerSize() {
            return bankers.size();
        }

        @Override
        public boolean canAdd() {
            return bankerSize() + size() < maxNumber - minNumber + 1;
        }

        public synchronized BankerBuilder finishBankers() {
            if (bankers.isEmpty()) {
                throw new IllegalStateException("At least one banker number must be chosen");
            }
            bankersComplete = true;
            return this;
        }

        public int getMinSelectionsNeeded() {
            return Math.max(1, 6 - bankers.size());
        }

        public List<Integer> getBankers() {
            return Collections.unmodifiableList(bankers);
        }

        public boolean inSelectionPhase() {
            return bankersComplete;
        }

        @Override
        public boolean contains(int i) {
            return bankers.contains(i) || selections.contains(i);
        }

        public boolean bankerCompleted() {
            return bankersComplete || bankers.size() >= maxBankers;
        }

        @Override
        public boolean completed() {
            return (bankers.size() + selections.size()) > 6;
        }

        @Override
        public BetNumbers build() {
            if (!completed()) {
                throw new IllegalStateException("Lottery Number builder not yet completed!");
            }
            return new BetNumbers(bankers, selections);
        }

    }

    public static class RandomBuilder extends BetNumbersBuilder {

        private final int count;

        private RandomBuilder(int minNumber, int maxNumber) {
            this(minNumber, maxNumber, 1);
        }

        private RandomBuilder(int minNumber, int maxNumber, int count) {
            super(minNumber, maxNumber, BetNumbersType.RANDOM);
            this.count = count;
        }

        @Override
        public synchronized RandomBuilder addNumber(int number) {
            throw new UnsupportedOperationException("Cannot add number in random builder");
        }

        @Override
        public IntObjectPair<BetNumbersBuilder> addRandomNumber() {
            throw new UnsupportedOperationException("Cannot add number in random builder");
        }

        @Override
        public synchronized RandomBuilder removeNumber(int number) {
            throw new UnsupportedOperationException("Cannot remove number in random builder");
        }

        @Override
        public int size() {
            return 6;
        }

        @Override
        public boolean contains(int i) {
            throw new UnsupportedOperationException("Cannot check contained numbers in random builder");
        }

        @Override
        public int setsSize() {
            return count;
        }

        @Override
        public boolean completed() {
            return true;
        }

        @Override
        public BetNumbers build() {
            List<Collection<Integer>> numbers = IntStream.range(0, count).mapToObj(i -> ThreadLocalRandom.current().ints(minNumber, maxNumber + 1).distinct().limit(6).boxed().collect(Collectors.toList())).collect(Collectors.toList());
            return new BetNumbers(numbers, BetNumbersType.RANDOM);
        }

    }

}
