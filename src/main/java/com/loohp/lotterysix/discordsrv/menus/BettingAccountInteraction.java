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
import com.loohp.lotterysix.utils.LotteryUtils;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Emoji;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.GenericComponentInteractionCreateEvent;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.awt.Color;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class BettingAccountInteraction extends DiscordInteraction {

    public static final String INTERACTION_LABEL = "ls_betting_account";

    public BettingAccountInteraction() {
        super(INTERACTION_LABEL, true);
    }

    @Override
    public boolean doOccupyEntireRow(UUID uuid) {
        return false;
    }

    @Override
    public List<ActionRow> getActionRows(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        String title = LotteryUtils.formatPlaceholders(player, instance.discordSRVSlashCommandsBetAccountTitle, instance);
        return Collections.singletonList(ActionRow.of(Button.secondary(INTERACTION_LABEL, title).withEmoji(Emoji.fromUnicode("\u2139\uFE0F"))));
    }

    @Override
    public void handle(GenericComponentInteractionCreateEvent event) {
        String discordUserId = event.getUser().getId();
        UUID uuid = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(discordUserId);
        if (uuid == null) {
            event.getHook().editOriginal(instance.discordSRVSlashCommandsGlobalMessagesNotLinked).setActionRows().setEmbeds().retainFiles(Collections.emptyList()).queue();
            return;
        }
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        StringBuilder sb = new StringBuilder();
        for (String line : instance.discordSRVSlashCommandsBetAccountSubTitle) {
            sb.append(ChatColor.stripColor(LotteryUtils.formatPlaceholders(player, line, instance))).append("\n");
        }
        if (sb.length() > 0) {
            sb.append("\n");
        }
        String description = sb.charAt(sb.length() - 1) == '\n' ? sb.substring(0, sb.length() - 1) : sb.toString();

        EmbedBuilder builder = new EmbedBuilder()
                .setColor(Color.BLUE)
                .setTitle(ChatColor.stripColor(LotteryUtils.formatPlaceholders(player, instance.discordSRVSlashCommandsBetAccountTitle, instance)))
                .setDescription(description)
                .setThumbnail(instance.discordSRVSlashCommandsBetAccountThumbnailURL);

        event.getHook().editOriginalEmbeds(builder.build()).setActionRows().retainFiles(Collections.emptyList()).queue();
    }

}
