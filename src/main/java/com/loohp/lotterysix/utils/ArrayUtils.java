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

package com.loohp.lotterysix.utils;

import java.util.Arrays;
import java.util.Optional;

public class ArrayUtils {

    public static <T> T getOrNull(T[] array, int index) {
        if (index < 0 || index >= array.length) {
            return null;
        }
        return array[index];
    }

    public static <T> Optional<T> getOptional(T[] array, int index) {
        if (index < 0 || index >= array.length) {
            return Optional.empty();
        }
        return Optional.of(array[index]);
    }

    public static byte[][] divideArray(byte[] source, int chunksize) {
        int length = (int) Math.ceil(source.length / (double) chunksize);
        if (length <= 1) {
            return new byte[][] {source};
        }
        byte[][] ret = new byte[length][];
        int start = 0;
        for (int i = 0; i < ret.length; i++) {
            int end = start + chunksize;
            ret[i] = Arrays.copyOfRange(source, start, Math.min(end, source.length));
            start += chunksize;
        }
        return ret;
    }

}
