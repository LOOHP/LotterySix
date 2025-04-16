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

package com.loohp.lotterysix.game.objects;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LazyReplaceString implements CharSequence {

    private final String value;

    public LazyReplaceString(String value) {
        this.value = value;
    }

    public LazyReplaceString replace(String target, Supplier<String> replacement) {
        if (value.contains(target)) {
            return new LazyReplaceString(value.replace(target, replacement.get()));
        }
        return this;
    }

    public LazyReplaceString replaceAll(String regex, Function<MatchResult, String> replacement) {
        Pattern pattern = Pattern.compile(regex);
        if (pattern.matcher(value).find()) {
            Matcher matcher = pattern.matcher(value);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                matcher.appendReplacement(sb, replacement.apply(matcher));
            }
            matcher.appendTail(sb);
            return new LazyReplaceString(sb.toString());
        }
        return this;
    }

    public LazyReplaceString replace(String target, String replacement) {
        if (value.contains(target)) {
            return new LazyReplaceString(value.replace(target, replacement));
        }
        return this;
    }

    public LazyReplaceString replaceAll(String regex, String replacement) {
        Pattern pattern = Pattern.compile(regex);
        if (pattern.matcher(value).find()) {
            return new LazyReplaceString(pattern.matcher(value).replaceAll(replacement));
        }
        return this;
    }

    @Override
    public int length() {
        return value.length();
    }

    @Override
    public char charAt(int index) {
        return value.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return value.subSequence(start, end);
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LazyReplaceString that = (LazyReplaceString) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
