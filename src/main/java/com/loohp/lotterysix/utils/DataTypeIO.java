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

package com.loohp.lotterysix.utils;

import com.loohp.lotterysix.game.lottery.GameNumber;
import com.loohp.lotterysix.game.objects.NumberStatistics;
import com.loohp.lotterysix.game.objects.WinningNumbers;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Year;
import java.util.UUID;

public class DataTypeIO {

    public static UUID readUUID(DataInputStream in) throws IOException {
        return new UUID(in.readLong(), in.readLong());
    }

    public static void writeUUID(DataOutputStream out, UUID uuid) throws IOException {
        out.writeLong(uuid.getMostSignificantBits());
        out.writeLong(uuid.getLeastSignificantBits());
    }

    public static String readString(DataInputStream in, Charset charset) throws IOException {
        int length = in.readInt();

        if (length == -1) {
            throw new IOException("Premature end of stream.");
        }

        byte[] b = new byte[length];
        in.readFully(b);
        return new String(b, charset);
    }

    public static void writeString(DataOutputStream out, String string, Charset charset) throws IOException {
        byte[] bytes = string.getBytes(charset);
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    public static GameNumber readGameNumber(DataInputStream in) throws IOException {
        return new GameNumber(Year.of(in.readInt()), in.readInt());
    }

    public static void writeGameNumber(DataOutputStream out, GameNumber gameNumber) throws IOException {
        out.writeInt(gameNumber.getYear().getValue());
        out.writeInt(gameNumber.getNumber());
    }

    public static NumberStatistics readNumberStatistics(DataInputStream in) throws IOException {
        return new NumberStatistics(in.readInt(), in.readInt());
    }

    public static void writeNumberStatistics(DataOutputStream out, NumberStatistics statistics) throws IOException {
        out.writeInt(statistics.getLastDrawn());
        out.writeInt(statistics.getTimesDrawn());
    }

}
