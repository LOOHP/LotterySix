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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public abstract class BetNumbersBuilder {

    public static BetNumbersBuilder.SingleBuilder single(int minNumber, int maxNumber) {
        return new BetNumbersBuilder.SingleBuilder(minNumber, maxNumber);
    }

    public static BetNumbersBuilder.MultipleBuilder multiple(int minNumber, int maxNumber) {
        return new BetNumbersBuilder.MultipleBuilder(minNumber, maxNumber);
    }

    public static BetNumbersBuilder.BankerBuilder banker(int minNumber, int maxNumber) {
        return new BetNumbersBuilder.BankerBuilder(minNumber, maxNumber);
    }

    public static BetNumbersBuilder.RandomBuilder random(int minNumber, int maxNumber) {
        return new BetNumbersBuilder.RandomBuilder(minNumber, maxNumber);
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

    public abstract BetNumbersBuilder removeNumber(int number);

    public abstract int size();

    public abstract boolean contains(int i);

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

        private final List<Integer> numbers;

        private RandomBuilder(int minNumber, int maxNumber) {
            super(minNumber, maxNumber, BetNumbersType.RANDOM);
            this.numbers = ThreadLocalRandom.current().ints(minNumber, maxNumber + 1).distinct().limit(6).boxed().collect(Collectors.toList());
        }

        @Override
        public synchronized RandomBuilder addNumber(int number) {
            throw new IllegalStateException("Lottery Number builder already completed!");
        }

        @Override
        public synchronized RandomBuilder removeNumber(int number) {
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
            return true;
        }

        @Override
        public BetNumbers build() {
            if (!completed()) {
                throw new IllegalStateException("Lottery Number builder not yet completed!");
            }
            return new BetNumbers(numbers, BetNumbersType.RANDOM);
        }

    }

}
