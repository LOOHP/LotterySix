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

package com.loohp.lotterysix.events;

import com.loohp.lotterysix.game.objects.AddBetResult;
import com.loohp.lotterysix.game.objects.betnumbers.BetNumbers;
import com.loohp.lotterysix.game.player.LotteryPlayer;
import com.loohp.platformscheduler.Scheduler;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerBetEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    private final LotteryPlayer player;
    private final BetNumbers numbers;
    private final long price;
    private final AddBetResult result;

    public PlayerBetEvent(LotteryPlayer player, BetNumbers numbers, long price, AddBetResult result) {
        super(!Scheduler.isPrimaryThread());
        this.player = player;
        this.numbers = numbers;
        this.price = price;
        this.result = result;
    }

    public LotteryPlayer getPlayer() {
        return player;
    }

    public BetNumbers getNumbers() {
        return numbers;
    }

    public long getPrice() {
        return price;
    }

    public AddBetResult getResult() {
        return result;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

}
