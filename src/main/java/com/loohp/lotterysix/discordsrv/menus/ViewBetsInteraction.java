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

package com.loohp.lotterysix.discordsrv.menus;

import com.loohp.lotterysix.LotterySixPlugin;
import com.loohp.lotterysix.discordsrv.DiscordInteraction;
import com.loohp.lotterysix.game.lottery.PlayableLotterySixGame;
import com.loohp.lotterysix.game.objects.PlayerBets;
import com.loohp.lotterysix.utils.LotteryUtils;
import com.loohp.lotterysix.utils.StringUtils;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Emoji;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.GenericComponentInteractionCreateEvent;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import github.scarsz.discordsrv.dependencies.jda.api.requests.restaction.WebhookMessageUpdateAction;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.awt.Color;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.loohp.lotterysix.discordsrv.DiscordSRVHook.INTERACTION_LABEL_PREFIX;

public class ViewBetsInteraction extends DiscordInteraction {

    public static final String INTERACTION_LABEL = INTERACTION_LABEL_PREFIX + "view_bets";

    public ViewBetsInteraction() {
        super(INTERACTION_LABEL, false);
    }

    @Override
    public boolean doOccupyEntireRow(UUID uuid) {
        return false;
    }

    @Override
    public List<ActionRow> getActionRows(UUID uuid) {
        Button button = Button.success(INTERACTION_LABEL, instance.discordSRVSlashCommandsViewCurrentBetsTitle).withEmoji(Emoji.fromUnicode("\uD83C\uDFAB"));
        if (instance.getCurrentGame() == null) {
            button = button.asDisabled();
        }
        return Collections.singletonList(ActionRow.of(button));
    }

    @Override
    public void handle(GenericComponentInteractionCreateEvent event) {
        String discordUserId = event.getUser().getId();
        UUID uuid = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(discordUserId);

        PlayableLotterySixGame game = instance.getCurrentGame();
        if (game == null) {
            event.getHook().editOriginalEmbeds(getGenericEmbed(instance.discordSRVSlashCommandsViewCurrentBetsNoGame, Color.RED)).setActionRows(ActionRow.of(getMainMenuButton())).retainFiles(Collections.emptyList()).queue();
        } else {
            OfflinePlayer player = uuid == null ? null : Bukkit.getOfflinePlayer(uuid);
            StringBuilder sb = new StringBuilder();
            for (String line : instance.discordSRVSlashCommandsViewCurrentBetsSubTitle) {
                sb.append(ChatColor.stripColor(LotteryUtils.formatPlaceholders(player, line, instance, game))).append("\n");
            }
            if (uuid != null) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                List<PlayerBets> bets = game.getPlayerBets(uuid);
                if (bets.isEmpty()) {
                    sb.append(instance.discordSRVSlashCommandsViewCurrentBetsNoBets);
                } else {
                    for (PlayerBets bet : bets) {
                        StringBuilder str = new StringBuilder();
                        if (bet.isMultipleDraw()) {
                            str.append("**").append(bet.getChosenNumbers().toString().replace("/ ", "/\n")).append("**\n").append(ChatColor.stripColor(instance.ticketDescriptionMultipleDraw
                                    .replace("{Price}", StringUtils.formatComma(bet.getBet()))
                                    .replace("{UnitPrice}", StringUtils.formatComma(instance.pricePerBet / bet.getType().getDivisor()))
                                    .replace("{DrawsRemaining}", StringUtils.formatComma(bet.getDrawsRemaining()))
                                    .replace("{MultipleDraw}", StringUtils.formatComma(bet.getMultipleDraw())))).append("\n\n");
                        } else {
                            str.append("**").append(bet.getChosenNumbers().toString().replace("/ ", "/\n")).append("**\n").append(ChatColor.stripColor(instance.ticketDescription
                                    .replace("{Price}", StringUtils.formatComma(bet.getBet()))
                                    .replace("{UnitPrice}", StringUtils.formatComma(instance.pricePerBet / bet.getType().getDivisor())))).append("\n\n");
                        }
                        if (str.length() + sb.length() < 4090) {
                            sb.append(str);
                        } else {
                            sb.append("...");
                            break;
                        }
                    }
                }
            }
            String description = sb.charAt(sb.length() - 1) == '\n' ? sb.substring(0, sb.length() - 1) : sb.toString();

            EmbedBuilder builder = new EmbedBuilder()
                    .setColor(Color.GREEN)
                    .setTitle(ChatColor.stripColor(LotteryUtils.formatPlaceholders(player, instance.discordSRVSlashCommandsViewCurrentBetsTitle, instance, game)))
                    .setDescription(description)
                    .setThumbnail(instance.discordSRVSlashCommandsViewCurrentBetsThumbnailURL);

            byte[] advertisementImage = LotterySixPlugin.discordSRVHook.getAdvertisementImage();
            if (advertisementImage != null) {
                builder.setImage("attachment://image.png");
            }
            WebhookMessageUpdateAction<Message> action = event.getHook().editOriginalEmbeds(builder.build()).setActionRows(ActionRow.of(getMainMenuButton())).retainFiles(Collections.emptyList());
            if (advertisementImage != null) {
                action = action.addFile(advertisementImage, "image.png");
            }
            action.queue();
        }
    }

}
