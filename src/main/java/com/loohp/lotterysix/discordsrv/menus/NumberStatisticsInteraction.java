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

package com.loohp.lotterysix.discordsrv.menus;

import com.loohp.lotterysix.discordsrv.DiscordInteraction;
import com.loohp.lotterysix.game.lottery.CompletedLotterySixGame;
import com.loohp.lotterysix.utils.LotteryUtils;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Emoji;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.GenericComponentInteractionCreateEvent;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import net.md_5.bungee.api.ChatColor;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class NumberStatisticsInteraction extends DiscordInteraction {

    public static final String INTERACTION_LABEL = "ls_number_statistics";

    public NumberStatisticsInteraction() {
        super(INTERACTION_LABEL, false);
    }

    @Override
    public boolean doOccupyEntireRow(UUID uuid) {
        return false;
    }

    @Override
    public List<ActionRow> getActionRows(UUID uuid) {
        return Collections.singletonList(ActionRow.of(Button.primary(INTERACTION_LABEL, instance.discordSRVSlashCommandsViewNumberStatisticsTitle).withEmoji(Emoji.fromUnicode("\uD83D\uDD22"))));
    }

    @Override
    public void handle(GenericComponentInteractionCreateEvent event) {
        CompletedLotterySixGame game = instance.getCompletedGames().isEmpty() ? null : instance.getCompletedGames().get(0);
        EmbedBuilder builder = new EmbedBuilder()
                .setColor(Color.CYAN)
                .setTitle(ChatColor.stripColor(LotteryUtils.formatPlaceholders(null, instance.guiNumberStatisticsTitle, instance, game)))
                .setFooter(ChatColor.stripColor(String.join("\n", LotteryUtils.formatPlaceholders(null, instance.guiNumberStatisticsNote, instance, game))))
                .setThumbnail(instance.discordSRVSlashCommandsViewNumberStatisticsThumbnailURL);

        List<String> numbers = new ArrayList<>(instance.numberOfChoices);

        for (int i = 1; i <= instance.numberOfChoices; i++) {
            numbers.add("**" + i + "**\n"
                    + ChatColor.stripColor(LotteryUtils.formatPlaceholders(null, instance.guiNumberStatisticsLastDrawn.replace("{Number}", String.valueOf(i)), instance, game)) + "\n"
                    + ChatColor.stripColor(LotteryUtils.formatPlaceholders(null, instance.guiNumberStatisticsTimesDrawn.replace("{Number}", String.valueOf(i)), instance, game)) + "\n");
        }

        event.getHook().editOriginalEmbeds(builder.setDescription(String.join("\n", numbers)).build()).setActionRows().queue();
    }

}
