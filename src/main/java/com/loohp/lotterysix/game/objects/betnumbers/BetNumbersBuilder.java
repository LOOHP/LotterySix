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

import com.loohp.lotterysix.game.LotteryRegistry;
import com.loohp.lotterysix.game.objects.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public abstract class BetNumbersBuilder implements Iterable<Integer> {

    public static final Pattern RANDOM_REP_PATTERN = Pattern.compile("^RAN/([0-9]+)(?:/([0-9]+))?(?:/([0-9]+))?$");

    public static Pair<Stream<? extends BetNumbersBuilder>, BetNumbersType> fromString(int minNumber, int maxNumber, String input) {
        Matcher matcher = RANDOM_REP_PATTERN.matcher(input);
        if (matcher.find()) {
            String group1 = matcher.group(1);
            if (group1 == null) {
                return null;
            }
            int count;
            try {
                count = Integer.parseInt(group1);
            } catch (NumberFormatException e) {
                return null;
            }
            if (count <= 0) {
                return null;
            }
            String group2 = matcher.group(2);
            if (group2 == null) {
                try {
                    return Pair.of(random(minNumber, maxNumber, count), BetNumbersType.RANDOM);
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
            int size;
            try {
                size = Integer.parseInt(group2);
            } catch (NumberFormatException e) {
                return null;
            }
            String group3 = matcher.group(3);
            if (group3 == null) {
                if (size < LotteryRegistry.NUMBERS_PER_BET + 1) {
                    return null;
                }
                try {
                    return Pair.of(multipleRandom(minNumber, maxNumber, size, count), BetNumbersType.MULTIPLE_RANDOM);
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
            int selection;
            try {
                selection = Integer.parseInt(group3);
            } catch (NumberFormatException e) {
                return null;
            }
            if (size > LotteryRegistry.NUMBERS_PER_BET - 1) {
                return null;
            }
            if (selection + size < LotteryRegistry.NUMBERS_PER_BET + 1) {
                return null;
            }
            try {
                return Pair.of(bankerRandom(minNumber, maxNumber, size, selection, count), BetNumbersType.BANKER_RANDOM);
            } catch (IllegalArgumentException e) {
                return null;
            }
        } else {
            String[] sections = input.split(" ");
            Set<Integer> bankers = null;
            Set<Integer> numbers = new LinkedHashSet<>();
            for (String section : sections) {
                if (!section.isEmpty()) {
                    if (section.equals(">")) {
                        if (numbers.isEmpty() || numbers.size() > LotteryRegistry.NUMBERS_PER_BET - 1) {
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
                if (numbers.size() < LotteryRegistry.NUMBERS_PER_BET) {
                    return null;
                } else if (numbers.size() == LotteryRegistry.NUMBERS_PER_BET) {
                    builder = new SingleBuilder(minNumber, maxNumber);
                } else {
                    builder = new MultipleBuilder(minNumber, maxNumber);
                }
                for (int number : numbers) {
                    builder.addNumber(number);
                }
                return Pair.of(Stream.of(builder), builder.getType());
            } else {
                if (numbers.isEmpty() || bankers.size() + numbers.size() < LotteryRegistry.NUMBERS_PER_BET + 1) {
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
                return Pair.of(Stream.of(builder), builder.getType());
            }
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
        } else {
            int perTicket = IntStream.iterate(10, i -> i - 1).limit(10).filter(i -> count % i == 0 && count / i > 0).findFirst().orElse(1);
            int tickets = count / perTicket;
            if (tickets > 10) {
                throw new IllegalArgumentException("invalid count value, as it would result in more than 10 tickets");
            }
            return IntStream.range(0, tickets).mapToObj(i -> new BetNumbersBuilder.RandomBuilder(minNumber, maxNumber, perTicket));
        }
    }

    public static MultipleRandomBuilder multipleRandom(int minNumber, int maxNumber, int size) {
        return new BetNumbersBuilder.MultipleRandomBuilder(minNumber, maxNumber, size);
    }

    public static Stream<MultipleRandomBuilder> multipleRandom(int minNumber, int maxNumber, int size, int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("count cannot be 0 or negative");
        } else {
            if (count > 10) {
                throw new IllegalArgumentException("invalid count value, as it would result in more than 10 tickets");
            }
            return IntStream.range(0, count).mapToObj(i -> new BetNumbersBuilder.MultipleRandomBuilder(minNumber, maxNumber, size));
        }
    }

    public static BankerRandomBuilder bankerRandom(int minNumber, int maxNumber, int bankerSize, int selectionSize) {
        return new BetNumbersBuilder.BankerRandomBuilder(minNumber, maxNumber, bankerSize, selectionSize);
    }

    public static Stream<BankerRandomBuilder> bankerRandom(int minNumber, int maxNumber, int bankerSize, int selectionSize, int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("count cannot be 0 or negative");
        } else {
            if (count > 10) {
                throw new IllegalArgumentException("invalid count value, as it would result in more than 10 tickets");
            }
            return IntStream.range(0, count).mapToObj(i -> new BetNumbersBuilder.BankerRandomBuilder(minNumber, maxNumber, bankerSize, selectionSize));
        }
    }

    protected final int minNumber;
    protected final int maxNumber;
    protected final BetNumbersType type;
    protected boolean validateCompleteOnAdd;

    protected BetNumbersBuilder(int minNumber, int maxNumber, BetNumbersType type) {
        this.minNumber = minNumber;
        this.maxNumber = maxNumber;
        this.type = type;
        this.validateCompleteOnAdd = true;
    }

    protected void checkBound(int number) {
        if (number < minNumber || number > maxNumber) {
            throw new IllegalArgumentException("number out of bound");
        }
    }

    public BetNumbersType getType() {
        return type;
    }

    public BetNumbersBuilder setValidateCompleteOnAdd(boolean value) {
        this.validateCompleteOnAdd = value;
        return this;
    }

    public abstract BetNumbersBuilder addNumber(int number);

    public abstract IntObjectPair<BetNumbersBuilder> addRandomNumber();

    public abstract BetNumbersBuilder removeNumber(int number);

    public int bankerSize() {
        return 0;
    }

    public abstract int size();

    public abstract boolean contains(int i);

    public abstract boolean canAdd();

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
            this.max = LotteryRegistry.NUMBERS_PER_BET;
            this.numbers = new ArrayList<>(max);
        }

        @Override
        public synchronized SingleBuilder addNumber(int number) {
            checkBound(number);
            if (validateCompleteOnAdd && completed()) {
                throw new IllegalStateException("Lottery Number builder already completed!");
            }
            numbers.add(number);
            return this;
        }

        @Override
        public IntObjectPair<BetNumbersBuilder> addRandomNumber() {
            if (validateCompleteOnAdd && completed()) {
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
        public boolean canAdd() {
            return !completed();
        }

        @Override
        public BetNumbers build() {
            if (!completed()) {
                throw new IllegalStateException("Lottery Number builder not yet completed!");
            }
            return new BetNumbers(numbers, BetNumbersType.SINGLE);
        }

        @Override
        public Iterator<Integer> iterator() {
            return numbers.iterator();
        }

    }

    public static class MultipleBuilder extends BetNumbersBuilder {

        private final int min;
        private final List<Integer> numbers;

        private MultipleBuilder(int minNumber, int maxNumber) {
            super(minNumber, maxNumber, BetNumbersType.MULTIPLE);
            this.min = LotteryRegistry.NUMBERS_PER_BET + 1;
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
        public boolean canAdd() {
            return size() < maxNumber - minNumber + 1;
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

        @Override
        public Iterator<Integer> iterator() {
            return numbers.iterator();
        }

    }

    public static class BankerBuilder extends BetNumbersBuilder {

        private final int maxBankers;
        private final List<Integer> bankers;
        private final List<Integer> selections;
        private boolean bankersComplete;

        private BankerBuilder(int minNumber, int maxNumber) {
            super(minNumber, maxNumber, BetNumbersType.BANKER);
            this.maxBankers = LotteryRegistry.NUMBERS_PER_BET - 1;
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
                if (validateCompleteOnAdd && bankerCompleted()) {
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
                if (validateCompleteOnAdd && bankerCompleted()) {
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

        @Override
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
            return Math.max(1, LotteryRegistry.NUMBERS_PER_BET - bankers.size());
        }

        public List<Integer> getBankers() {
            return Collections.unmodifiableList(bankers);
        }

        public boolean inSelectionPhase() {
            return bankersComplete;
        }

        public boolean selectionContains(int i) {
            return selections.contains(i);
        }

        public boolean bankerContains(int i) {
            return bankers.contains(i);
        }

        @Override
        public boolean contains(int i) {
            return selectionContains(i) || bankerContains(i);
        }

        public boolean bankerCompleted() {
            return bankersComplete || bankers.size() >= maxBankers;
        }

        @Override
        public boolean completed() {
            return (bankers.size() + selections.size()) > LotteryRegistry.NUMBERS_PER_BET;
        }

        @Override
        public BetNumbers build() {
            if (!completed()) {
                throw new IllegalStateException("Lottery Number builder not yet completed!");
            }
            return new BetNumbers(bankers, selections, BetNumbersType.BANKER);
        }

        @Override
        public Iterator<Integer> iterator() {
            return Stream.concat(bankers.stream(), selections.stream()).iterator();
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
            return LotteryRegistry.NUMBERS_PER_BET;
        }

        @Override
        public boolean canAdd() {
            return false;
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
            List<Collection<Integer>> numbers = IntStream.range(0, count).mapToObj(i -> ThreadLocalRandom.current().ints(minNumber, maxNumber + 1).distinct().limit(LotteryRegistry.NUMBERS_PER_BET).boxed().collect(Collectors.toList())).collect(Collectors.toList());
            return new BetNumbers(numbers, BetNumbersType.RANDOM);
        }

        @Override
        public Iterator<Integer> iterator() {
            return Collections.emptyIterator();
        }

    }

    public static class MultipleRandomBuilder extends BetNumbersBuilder {

        private final int size;

        private MultipleRandomBuilder(int minNumber, int maxNumber, int size) {
            super(minNumber, maxNumber, BetNumbersType.MULTIPLE_RANDOM);
            if (maxNumber + 1 - minNumber < size) {
                throw new IllegalArgumentException("not enough numbers to satisfy size");
            }
            this.size = size;
        }

        @Override
        public synchronized MultipleBuilder addNumber(int number) {
            throw new UnsupportedOperationException("Cannot check contained numbers in random builder");
        }

        @Override
        public IntObjectPair<BetNumbersBuilder> addRandomNumber() {
            throw new UnsupportedOperationException("Cannot check contained numbers in random builder");
        }

        @Override
        public synchronized MultipleBuilder removeNumber(int number) {
            throw new UnsupportedOperationException("Cannot check contained numbers in random builder");
        }

        @Override
        public boolean canAdd() {
            return false;
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public boolean contains(int i) {
            throw new UnsupportedOperationException("Cannot check contained numbers in random builder");
        }

        @Override
        public boolean completed() {
            return true;
        }

        @Override
        public BetNumbers build() {
            List<Integer> numbers = ThreadLocalRandom.current().ints(minNumber, maxNumber + 1).distinct().limit(size).boxed().collect(Collectors.toList());
            return new BetNumbers(numbers, BetNumbersType.MULTIPLE_RANDOM);
        }

        @Override
        public Iterator<Integer> iterator() {
            return Collections.emptyIterator();
        }

    }

    public static class BankerRandomBuilder extends BetNumbersBuilder {

        private final int bankersSize;
        private final int selectionSize;

        private BankerRandomBuilder(int minNumber, int maxNumber, int bankersSize, int selectionSize) {
            super(minNumber, maxNumber, BetNumbersType.BANKER_RANDOM);
            if (maxNumber + 1 - minNumber < bankersSize + selectionSize) {
                throw new IllegalArgumentException("not enough numbers to satisfy size");
            }
            this.bankersSize = bankersSize;
            this.selectionSize = selectionSize;
        }

        @Override
        public synchronized BankerBuilder addNumber(int number) {
            throw new UnsupportedOperationException("Cannot check contained numbers in random builder");
        }

        @Override
        public IntObjectPair<BetNumbersBuilder> addRandomNumber() {
            throw new UnsupportedOperationException("Cannot check contained numbers in random builder");
        }

        @Override
        public synchronized BankerBuilder removeNumber(int number) {
            throw new UnsupportedOperationException("Cannot check contained numbers in random builder");
        }

        @Override
        public int size() {
            return selectionSize;
        }

        @Override
        public int bankerSize() {
            return bankersSize;
        }

        @Override
        public boolean canAdd() {
            return false;
        }

        public synchronized BankerBuilder finishBankers() {
            throw new UnsupportedOperationException("Cannot check contained numbers in random builder");
        }

        public int getMinSelectionsNeeded() {
            throw new UnsupportedOperationException("Cannot check contained numbers in random builder");
        }

        public List<Integer> getBankers() {
            throw new UnsupportedOperationException("Cannot check contained numbers in random builder");
        }

        public boolean inSelectionPhase() {
            throw new UnsupportedOperationException("Cannot check contained numbers in random builder");
        }

        @Override
        public boolean contains(int i) {
            throw new UnsupportedOperationException("Cannot check contained numbers in random builder");
        }

        public boolean bankerCompleted() {
            return true;
        }

        @Override
        public boolean completed() {
            return true;
        }

        @Override
        public BetNumbers build() {
            List<Integer> bankers = ThreadLocalRandom.current().ints(minNumber, maxNumber + 1).distinct().limit(bankersSize).boxed().collect(Collectors.toList());
            List<Integer> selections = ThreadLocalRandom.current().ints(minNumber, maxNumber + 1).distinct().filter(i -> !bankers.contains(i)).limit(selectionSize).boxed().collect(Collectors.toList());
            return new BetNumbers(bankers, selections, BetNumbersType.BANKER_RANDOM);
        }

        @Override
        public Iterator<Integer> iterator() {
            return Collections.emptyIterator();
        }

    }

}
