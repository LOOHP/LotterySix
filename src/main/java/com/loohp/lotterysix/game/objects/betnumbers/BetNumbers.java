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

package com.loohp.lotterysix.game.objects.betnumbers;

import com.loohp.lotterysix.game.LotteryRegistry;
import com.loohp.lotterysix.game.objects.FormattedString;
import com.loohp.lotterysix.utils.ChatColorUtils;
import org.paukov.combinatorics3.Generator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BetNumbers implements FormattedString {

    private final Set<Integer> bankers;
    private final Set<Integer> numbers;
    private final List<Set<Integer>> additionalSets;
    private final BetNumbersType type;

    BetNumbers(Collection<Integer> numbers, BetNumbersType type) {
        if (type.isBanker()) {
            throw new IllegalArgumentException("type cannot be banker");
        }
        this.bankers = null;
        this.numbers = Collections.unmodifiableSet(new TreeSet<>(numbers));
        this.additionalSets = null;
        this.type = type;
    }

    BetNumbers(Collection<Integer> bankers, Collection<Integer> numbers, BetNumbersType type) {
        if (!type.isBanker()) {
            throw new IllegalArgumentException("type must be banker");
        }
        this.bankers = Collections.unmodifiableSet(new TreeSet<>(bankers));
        this.numbers = Collections.unmodifiableSet(new TreeSet<>(numbers));
        this.additionalSets = null;
        this.type = type;
    }

    BetNumbers(List<Collection<Integer>> numbers, BetNumbersType type) {
        if (type.isMultipleCombination()) {
            throw new IllegalArgumentException("type must not be multiple combinations");
        }
        if (numbers.isEmpty()) {
            throw new IllegalArgumentException("numbers cannot be empty");
        }
        this.bankers = null;
        this.numbers = Collections.unmodifiableSet(new TreeSet<>(numbers.get(0)));
        this.additionalSets = numbers.size() == 1 ? null : Collections.unmodifiableList(numbers.stream().skip(1).map(each -> Collections.unmodifiableSet(new TreeSet<>(each))).collect(Collectors.toList()));
        this.type = type;
    }

    public Iterator<Integer> iterator() {
        return numbers.stream().iterator();
    }

    public Stream<List<Integer>> combinations() {
        Stream<List<Integer>> stream;
        if (hasNoBankers()) {
            stream = Generator.combination(numbers).simple(LotteryRegistry.NUMBERS_PER_BET).stream();
        } else {
            stream = Generator.combination(numbers).simple(LotteryRegistry.NUMBERS_PER_BET - bankers.size()).stream().peek(list -> list.addAll(bankers));
        }
        if (hasNoAdditionalSets()) {
            return stream;
        } else {
            return Stream.concat(stream, additionalSets.stream().map(each -> new ArrayList<>(each)));
        }
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
        return bankers.stream().iterator();
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

    public boolean hasAdditionalSets() {
        return additionalSets != null && !additionalSets.isEmpty();
    }

    public boolean hasNoAdditionalSets() {
        return additionalSets == null || additionalSets.isEmpty();
    }

    public Iterator<Set<Integer>> additionalSetsIterator() {
        if (hasNoAdditionalSets()) {
            return Collections.emptyIterator();
        }
        return additionalSets.stream().map(each -> Collections.unmodifiableSet(each)).iterator();
    }

    public List<Set<Integer>> getAdditionalSets() {
        if (hasNoAdditionalSets()) {
            return Collections.emptyList();
        }
        return additionalSets.stream().map(each -> Collections.unmodifiableSet(each)).collect(Collectors.toList());
    }

    public int getSetsSize() {
        if (hasNoAdditionalSets()) {
            return 1;
        }
        return additionalSets.size() + 1;
    }

    public List<Integer> getAllNumbers() {
        List<Integer> combined = new ArrayList<>(numbers);
        if (hasBankers()) {
            combined.addAll(bankers);
        }
        if (hasAdditionalSets()) {
            for (Set<Integer> additional : additionalSets) {
                combined.addAll(additional);
            }
        }
        return combined;
    }

    public int setsIndexOf(Set<Integer> numberSet) {
        if (numbers.equals(numberSet)) {
            return 0;
        }
        if (hasNoAdditionalSets()) {
            return -1;
        }
        return additionalSets.indexOf(numberSet);
    }

    public Set<Integer> getSet(int index) {
        if (index == 0) {
            return numbers;
        }
        if (hasNoAdditionalSets()) {
            return null;
        }
        return additionalSets.get(index - 1);
    }

    public BetNumbersType getType() {
        return type;
    }

    public boolean isCombination() {
        return type.isMultipleCombination();
    }

    public boolean isBulk() {
        return getSetsSize() > 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BetNumbers that = (BetNumbers) o;
        return Objects.equals(bankers, that.bankers) && numbers.equals(that.numbers) && Objects.equals(additionalSets, that.additionalSets) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bankers, numbers, additionalSets, type);
    }

    @Override
    public String toString() {
        String numbersString = numbers.stream().map(each -> each.toString()).collect(Collectors.joining(" "));
        if (hasBankers()) {
            numbersString = bankers.stream().map(each -> each.toString()).collect(Collectors.joining(" ")) + " > " + numbersString;
        }
        if (hasNoAdditionalSets()) {
            return numbersString;
        }
        return numbersString + " / " + additionalSets.stream().map(each -> each.stream().map(e -> e.toString()).collect(Collectors.joining(" "))).collect(Collectors.joining(" / "));
    }

    @Override
    public String toFormattedString() {
        String numbersString = numbers.stream().map(each -> ChatColorUtils.getNumberColor(each) + each.toString()).collect(Collectors.joining(" "));
        if (hasBankers()) {
            numbersString = bankers.stream().map(each -> ChatColorUtils.getNumberColor(each) + each.toString()).collect(Collectors.joining(" ")) + " \u00a75> " + numbersString;;
        }
        if (hasNoAdditionalSets()) {
            return numbersString;
        }
        return numbersString + " \u00a75/ " + additionalSets.stream().map(each -> each.stream().map(e -> ChatColorUtils.getNumberColor(e) + e.toString()).collect(Collectors.joining(" "))).collect(Collectors.joining(" \u00a75/ "));
    }

}
