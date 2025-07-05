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

package com.loohp.lotterysix.gui;

import com.cryptomorin.xseries.XMaterial;

import java.util.List;

public class GUIInfo {

    private final GUIType type;
    private final XMaterial itemType;
    private final String[] layout;
    private final int customModelDataOffset;

    public GUIInfo(GUIType type, XMaterial itemType, List<String> layout, int customModelDataOffset) {
        this.type = type;
        this.itemType = itemType;
        this.layout = layout == null ? null : layout.toArray(new String[0]);
        this.customModelDataOffset = customModelDataOffset;
    }

    public GUIType getType() {
        return type;
    }

    public boolean hasItemType() {
        return itemType != null;
    }

    public XMaterial getItemType() {
        return itemType;
    }

    public boolean hasLayout() {
        return layout != null;
    }

    public String[] getLayout() {
        return layout;
    }

    public int getCustomModelDataOffset() {
        return customModelDataOffset;
    }
}
