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

import com.loohp.lotterysix.game.objects.betnumbers.BetNumbers;
import com.loohp.lotterysix.utils.ChatColorUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class WinningNumbers {

    private final List<Integer> numbers;
    private final int specialNumber;

    private WinningNumbers(Collection<Integer> numbers, int specialNumber) {
        this.numbers = Collections.unmodifiableList(new ArrayList<>(numbers));
        this.specialNumber = specialNumber;
    }

    public WinningNumbers(int number1, int number2, int number3, int number4, int number5, int number6, int specialNumber) {
        this(Arrays.asList(number1, number2, number3, number4, number5, number6), specialNumber);
    }

    public List<Integer> getNumbers() {
        return numbers;
    }

    public List<Integer> getNumbersOrdered() {
        return Collections.unmodifiableList(numbers.stream().sorted().collect(Collectors.toList()));
    }

    public int getNumber(int index) {
        return numbers.get(index);
    }

    public int getNumberOrdered(int index) {
        return getNumbersOrdered().get(index);
    }

    public int getSpecialNumber() {
        return specialNumber;
    }

    public List<Pair<PrizeTier, WinningCombination>> checkWinning(BetNumbers betNumbers) {
        return betNumbers.combinations().map(numbers -> {
            int matches = 0;
            for (int num : numbers) {
                if (this.numbers.contains(num)) {
                    matches++;
                    if (matches >= 6) {
                        break;
                    }
                }
            }
            boolean matchSpecial = numbers.contains(specialNumber);
            switch (matches) {
                case 6: {
                    return Pair.of(PrizeTier.FIRST, new WinningCombination(numbers));
                }
                case 5: {
                    return Pair.of(matchSpecial ? PrizeTier.SECOND : PrizeTier.THIRD, new WinningCombination(numbers));
                }
                case 4: {
                    return Pair.of(matchSpecial ? PrizeTier.FOURTH : PrizeTier.FIFTH, new WinningCombination(numbers));
                }
                case 3: {
                    return Pair.of(matchSpecial ? PrizeTier.SIXTH : PrizeTier.SEVENTH, new WinningCombination(numbers));
                }
                default: {
                    return null;
                }
            }
        }).filter(each -> each != null).collect(Collectors.toList());
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

    public IntStream stream() {
        return numbers.stream().mapToInt(i -> i);
    }

    public Iterator<Integer> orderedIterator() {
        return new Iterator<Integer>() {
            private final Iterator<Integer> itr = getNumbersOrdered().iterator();

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

    @Override
    public String toString() {
        return numbers.stream().sorted().map(each -> each.toString()).collect(Collectors.joining(" ", "", " \"" + specialNumber + "\""));
    }

    public String toColoredString() {
        String numbersString = numbers.stream().sorted().map(each -> ChatColorUtils.getNumberColor(each) + each.toString()).collect(Collectors.joining(" "));
        return numbersString + " \u00a7e+ " + ChatColorUtils.getNumberColor(specialNumber) + specialNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WinningNumbers integers = (WinningNumbers) o;
        return specialNumber == integers.specialNumber && numbers.equals(integers.numbers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numbers, specialNumber);
    }
}
