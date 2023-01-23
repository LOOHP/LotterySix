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

package com.loohp.lotterysix.events;

import com.loohp.lotterysix.game.LotterySix;
import com.loohp.lotterysix.game.objects.BetNumbers;
import com.loohp.lotterysix.game.objects.LotterySixAction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class LotterySixEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    private final LotterySix lotterySix;
    private final LotterySixAction action;

    public LotterySixEvent(LotterySix lotterySix, LotterySixAction action) {
        super(!Bukkit.isPrimaryThread());
        this.lotterySix = lotterySix;
        this.action = action;
    }

    public LotterySix getLotterySix() {
        return lotterySix;
    }

    public LotterySixAction getAction() {
        return action;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

}
