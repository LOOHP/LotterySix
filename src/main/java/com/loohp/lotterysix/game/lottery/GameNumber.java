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

package com.loohp.lotterysix.game.lottery;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Objects;

public class GameNumber implements Comparable<GameNumber> {

    public static final DateTimeFormatter YEAR_FORMAT = DateTimeFormatter.ofPattern("yy");
    public static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("000");
    public static final Comparator<GameNumber> COMPARATOR = Comparator.comparing(GameNumber::getYear).thenComparing(GameNumber::getNumber);
    public static final GameNumber UNKNOWN = new GameNumber(Year.of(1970), 0);

    public static GameNumber fromString(String input) {
        String[] sections = input.split("/");
        if (sections.length != 2) {
            throw new IllegalArgumentException("\"" + input + "\" is not a valid game number format");
        }
        return new GameNumber(Year.from(YEAR_FORMAT.parse(sections[0])), Integer.parseInt(sections[1]));
    }

    @JsonAdapter(YearJsonAdapter.class)
    private final Year year;
    private final int number;

    public GameNumber(Year year, int number) {
        this.year = year;
        this.number = number;
    }

    public Year getYear() {
        return year;
    }

    public int getNumber() {
        return number;
    }

    @Override
    public String toString() {
        return YEAR_FORMAT.format(year) + "/" + NUMBER_FORMAT.format(number);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameNumber that = (GameNumber) o;
        return number == that.number && Objects.equals(year, that.year);
    }

    @Override
    public int hashCode() {
        return Objects.hash(year, number);
    }

    @Override
    public int compareTo(GameNumber o) {
        return COMPARATOR.compare(this, o);
    }

    public static class YearJsonAdapter extends TypeAdapter<Year> {

        @Override
        public void write(JsonWriter out, Year year) throws IOException {
            out.value(year.getValue());
        }

        @Override
        public Year read(JsonReader in) throws IOException {
            return Year.of((int) in.nextLong());
        }
    }

}
