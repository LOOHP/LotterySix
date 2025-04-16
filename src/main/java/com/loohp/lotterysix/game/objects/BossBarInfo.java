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

public class BossBarInfo {

    public static final BossBarInfo CLEAR = new BossBarInfo(null, "", "", 0.0);

    private final String message;
    private final String color;
    private final String style;
    private final double progress;

    public BossBarInfo(String message, String color, String style, double progress) {
        this.message = message;
        this.color = color;
        this.style = style;
        this.progress = progress;
    }

    public String getMessage() {
        return message;
    }

    public String getColor() {
        return color;
    }

    public String getStyle() {
        return style;
    }

    public double getProgress() {
        return progress;
    }

    public boolean isClearBossBar() {
        return message == null;
    }
}
