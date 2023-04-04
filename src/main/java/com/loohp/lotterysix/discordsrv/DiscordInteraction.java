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

package com.loohp.lotterysix.discordsrv;

import com.loohp.lotterysix.LotterySixPlugin;
import com.loohp.lotterysix.game.LotterySix;
import com.loohp.lotterysix.utils.LotteryUtils;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.GenericComponentInteractionCreateEvent;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.UUID;

public abstract class DiscordInteraction {

    protected static final LotterySix instance = LotterySixPlugin.getInstance();
    protected static final LotterySixPlugin plugin = LotterySixPlugin.plugin;
    protected static final DiscordSRV discordSRV = DiscordSRV.getPlugin();

    protected final String interactionLabelStartWith;
    protected final boolean requireAccountLinked;

    public DiscordInteraction(String interactionLabelStartWith, boolean requireAccountLinked) {
        this.interactionLabelStartWith = interactionLabelStartWith;
        this.requireAccountLinked = requireAccountLinked;
    }

    public String getInteractionLabelStartWith() {
        return interactionLabelStartWith;
    }

    public boolean requireAccountLinked() {
        return requireAccountLinked;
    }

    public abstract boolean doOccupyEntireRow(UUID uuid);

    public abstract List<ActionRow> getActionRows(UUID uuid);

    public abstract void handle(GenericComponentInteractionCreateEvent event);

}
