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
import com.loohp.lotterysix.events.LotterySixEvent;
import com.loohp.lotterysix.game.LotterySix;
import com.loohp.lotterysix.game.lottery.CompletedLotterySixGame;
import com.loohp.lotterysix.game.lottery.PlayableLotterySixGame;
import com.loohp.lotterysix.game.objects.LotterySixAction;
import com.loohp.lotterysix.game.objects.PlayerBets;
import com.loohp.lotterysix.game.objects.PlayerWinnings;
import com.loohp.lotterysix.utils.LotteryUtils;
import com.loohp.lotterysix.utils.SyncUtils;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.commands.PluginSlashCommand;
import github.scarsz.discordsrv.api.commands.SlashCommand;
import github.scarsz.discordsrv.api.commands.SlashCommandProvider;
import github.scarsz.discordsrv.api.events.DiscordReadyEvent;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.awt.Color;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class DiscordSRVHook implements Listener, SlashCommandProvider {

    public static final String PAST_DRAW_LABEL = "pastdraw";
    public static final String MY_BETS_LABEL = "mybets";

    private boolean init;

    public DiscordSRVHook() {
        DiscordSRV.api.subscribe(this);
        if (DiscordSRV.isReady) {
            this.init = true;
            DiscordSRV.api.addSlashCommandProvider(this);
            reload();
        } else {
            this.init = false;
        }
    }

    public void reload() {
        DiscordSRV.api.updateSlashCommands();
    }

    @Subscribe
    public void onDiscordReady(DiscordReadyEvent event) {
        if (!init) {
            DiscordSRV.api.addSlashCommandProvider(this);
            reload();
            this.init = true;
        }
    }

    @EventHandler
    public void onLotteryAction(LotterySixEvent event) {
        if (event.getAction().equals(LotterySixAction.RUN_LOTTERY_FINISH)) {
            LotterySix lotterySix = event.getLotterySix();
            CompletedLotterySixGame game = lotterySix.getCompletedGames().get(0);
            TextChannel channel = DiscordSRV.getPlugin().getOptionalTextChannel(lotterySix.discordSRVDrawResultAnnouncementChannel);
            if (channel != null) {
                EmbedBuilder builder = new EmbedBuilder()
                        .setColor(Color.YELLOW)
                        .setTitle(ChatColor.stripColor(LotteryUtils.formatPlaceholders(null, lotterySix.discordSRVDrawResultAnnouncementTitle, lotterySix, game)))
                        .setDescription(ChatColor.stripColor(LotteryUtils.formatPlaceholders(null, lotterySix.discordSRVDrawResultAnnouncementDescription, lotterySix, game)))
                        .setThumbnail(lotterySix.discordSRVDrawResultAnnouncementThumbnailURL);
                channel.sendMessageEmbeds(builder.build()).queue();
            }
        }
    }

    @Override
    public Set<PluginSlashCommand> getSlashCommands() {
        Set<PluginSlashCommand> commands = new HashSet<>();

        Guild guild = DiscordSRV.getPlugin().getMainGuild();

        if (LotterySixPlugin.getInstance().discordSRVSlashCommandsViewPastDrawEnabled) {
            commands.add(new PluginSlashCommand(LotterySixPlugin.plugin, new CommandData(PAST_DRAW_LABEL, LotterySixPlugin.getInstance().discordSRVSlashCommandsViewPastDrawDescription), guild.getId()));
        }
        if (LotterySixPlugin.getInstance().discordSRVSlashCommandsViewCurrentBetsEnabled) {
            commands.add(new PluginSlashCommand(LotterySixPlugin.plugin, new CommandData(MY_BETS_LABEL, LotterySixPlugin.getInstance().discordSRVSlashCommandsViewCurrentBetsDescription), guild.getId()));
        }

        return commands;
    }

    @SlashCommand(path = "*")
    public void onSlashCommand(SlashCommandEvent event) {
        Guild guild = DiscordSRV.getPlugin().getMainGuild();
        if (event.getGuild().getIdLong() != guild.getIdLong()) {
            return;
        }
        if (!(event.getChannel() instanceof TextChannel)) {
            return;
        }
        LotterySix lotterySix = LotterySixPlugin.getInstance();
        String label = event.getName();
        if (LotterySixPlugin.getInstance().discordSRVSlashCommandsViewPastDrawEnabled && label.equalsIgnoreCase(PAST_DRAW_LABEL)) {
            String discordUserId = event.getUser().getId();
            UUID uuid = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(discordUserId);
            if (uuid == null) {
                event.reply(lotterySix.discordSRVSlashCommandsGlobalMessagesNotLinked).setEphemeral(true).queue();
                return;
            }

            if (lotterySix.getCompletedGames().isEmpty()) {
                event.reply(lotterySix.discordSRVSlashCommandsViewPastDrawNoResults).setEphemeral(true).queue();
            } else {
                event.deferReply(true).queue();
                Bukkit.getScheduler().runTaskAsynchronously(LotterySixPlugin.plugin, () -> {
                    SyncUtils.blockUntilTrue(() -> !lotterySix.isGameLocked());

                    CompletedLotterySixGame game = lotterySix.getCompletedGames().get(0);
                    StringBuilder str = new StringBuilder(ChatColor.stripColor(LotteryUtils.formatPlaceholders(null, lotterySix.discordSRVDrawResultAnnouncementDescription, lotterySix, game)));

                    List<PlayerBets> playerBets = game.getPlayerBets(uuid);
                    if (!playerBets.isEmpty()) {
                        str.append("\n\n").append(lotterySix.discordSRVSlashCommandsViewPastDrawYourBets).append("\n");
                        List<PlayerWinnings> winningsList = game.getSortedPlayerWinnings(uuid);
                        for (PlayerWinnings winnings : winningsList.subList(0, Math.min(50, winningsList.size()))) {
                            str.append(winnings.getWinningBet(game).getChosenNumbers().toString()).append("\n");
                            if (winnings.isCombination(game)) {
                                str.append("(").append(winnings.getWinningCombination().toString()).append(")\n");
                            }
                            str.append(winnings.getTier().getShortHand()).append(" $").append(winnings.getWinnings()).append(" ($").append(game.getPricePerBet(winnings.getWinningBet(game).getType())).append(")").append("\n");
                        }
                        for (PlayerBets bets : playerBets) {
                            if (winningsList.stream().noneMatch(each -> each.getWinningBet(game).getBetId().equals(bets.getBetId()))) {
                                str.append(bets.getChosenNumbers().toString()).append("\n").append(lotterySix.discordSRVSlashCommandsViewPastDrawNoWinnings).append(" $0 ($").append(game.getPricePerBet(bets.getType())).append(")\n");
                            }
                        }
                    }

                    EmbedBuilder builder = new EmbedBuilder()
                            .setColor(Color.YELLOW)
                            .setTitle(ChatColor.stripColor(LotteryUtils.formatPlaceholders(null, lotterySix.discordSRVDrawResultAnnouncementTitle, lotterySix, game)))
                            .setDescription(str.substring(0, str.length() - 1))
                            .setThumbnail(lotterySix.discordSRVSlashCommandsViewPastDrawThumbnailURL);
                    event.getHook().setEphemeral(true).editOriginalEmbeds(builder.build()).queue();
                });
            }
            return;
        } else if (LotterySixPlugin.getInstance().discordSRVSlashCommandsViewCurrentBetsEnabled && label.equalsIgnoreCase(MY_BETS_LABEL)) {
            String discordUserId = event.getUser().getId();
            UUID uuid = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(discordUserId);
            if (uuid == null) {
                event.reply(lotterySix.discordSRVSlashCommandsGlobalMessagesNotLinked).setEphemeral(true).queue();
                return;
            }

            PlayableLotterySixGame game = lotterySix.getCurrentGame();
            if (game == null) {
                event.reply(lotterySix.discordSRVSlashCommandsViewCurrentBetsNoGame).setEphemeral(true).queue();
            } else {
                List<PlayerBets> bets = game.getPlayerBets(uuid);
                if (bets.isEmpty()) {
                    event.reply(lotterySix.discordSRVSlashCommandsViewCurrentBetsNoBets).setEphemeral(true).queue();
                } else {
                    StringBuilder str = new StringBuilder();

                    for (PlayerBets bet : bets) {
                        str.append("**").append(bet.getChosenNumbers().toString()).append("**\n");
                    }

                    EmbedBuilder builder = new EmbedBuilder()
                            .setColor(Color.YELLOW)
                            .setTitle(ChatColor.stripColor(LotteryUtils.formatPlaceholders(Bukkit.getOfflinePlayer(uuid), lotterySix.discordSRVSlashCommandsViewCurrentBetsTitle, lotterySix, game)))
                            .setDescription(str.substring(0, str.length() - 1))
                            .setThumbnail(lotterySix.discordSRVSlashCommandsViewCurrentBetsThumbnailURL);
                    event.replyEmbeds(builder.build()).setEphemeral(true).queue();
                }
            }
            return;
        }
    }


}
