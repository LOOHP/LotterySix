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

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class IntObjectPair<S> {

    public static <S> IntObjectPair<S> of(int first, S second) {
        return new IntObjectPair<>(first, second);
    }

    private final int first;
    private final S second;

    private IntObjectPair(int first, S second) {
        this.first = first;
        this.second = second;
    }

    public int getFirstInt() {
        return first;
    }

    public S getSecond() {
        return second;
    }

    public <T> T product(BiFunction<Integer, S, T> function) {
        return function.apply(first, second);
    }

    public void consume(BiConsumer<Integer, S> consumer) {
        consumer.accept(first, second);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntObjectPair<?> that = (IntObjectPair<?>) o;
        return first == that.first && Objects.equals(second, that.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }
}
