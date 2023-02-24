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
import com.loohp.lotterysix.game.lottery.GameNumber;
import com.loohp.lotterysix.game.lottery.PlayableLotterySixGame;
import com.loohp.lotterysix.game.objects.LotterySixAction;
import com.loohp.lotterysix.game.objects.PlayerBets;
import com.loohp.lotterysix.game.objects.PlayerWinnings;
import com.loohp.lotterysix.game.objects.PrizeTier;
import com.loohp.lotterysix.game.objects.betnumbers.BetNumbers;
import com.loohp.lotterysix.utils.LotteryUtils;
import com.loohp.lotterysix.utils.StringUtils;
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
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionType;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class DiscordSRVHook implements Listener, SlashCommandProvider {

    public static final String PAST_DRAW_LABEL = "pastdraw";
    public static final String MY_BETS_LABEL = "mybets";
    public static final String NUMBER_STATS_LABEL = "numberstats";

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
            commands.add(new PluginSlashCommand(LotterySixPlugin.plugin, new CommandData(PAST_DRAW_LABEL, LotterySixPlugin.getInstance().discordSRVSlashCommandsViewPastDrawDescription)
                    .addOption(OptionType.STRING, LotterySixPlugin.getInstance().discordSRVSlashCommandsGlobalLabelsGameNumberName, LotterySixPlugin.getInstance().discordSRVSlashCommandsGlobalLabelsGameNumberDescription, false), guild.getId()));
        }
        if (LotterySixPlugin.getInstance().discordSRVSlashCommandsViewCurrentBetsEnabled) {
            commands.add(new PluginSlashCommand(LotterySixPlugin.plugin, new CommandData(MY_BETS_LABEL, LotterySixPlugin.getInstance().discordSRVSlashCommandsViewCurrentBetsDescription), guild.getId()));
        }
        if (LotterySixPlugin.getInstance().discordSRVSlashCommandsViewNumberStatisticsEnabled) {
            commands.add(new PluginSlashCommand(LotterySixPlugin.plugin, new CommandData(NUMBER_STATS_LABEL, LotterySixPlugin.getInstance().discordSRVSlashCommandsViewNumberStatisticsDescription), guild.getId()));
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
            CompletedLotterySixGame selectedGame;
            if (!event.getOptions().isEmpty()) {
                try {
                    selectedGame = lotterySix.getCompletedGames().get(GameNumber.fromString(event.getOptions().get(0).getAsString().trim()));
                } catch (Exception e) {
                    event.reply(lotterySix.discordSRVSlashCommandsViewPastDrawNoResults).setEphemeral(true).queue();
                    return;
                }
            } else {
                selectedGame = null;
            }

            if (lotterySix.getCompletedGames().isEmpty() && selectedGame == null) {
                event.reply(lotterySix.discordSRVSlashCommandsViewPastDrawNoResults).setEphemeral(true).queue();
            } else {
                event.deferReply(true).queue();
                Bukkit.getScheduler().runTaskAsynchronously(LotterySixPlugin.plugin, () -> {
                    SyncUtils.blockUntilTrue(() -> !lotterySix.isGameLocked());

                    CompletedLotterySixGame game = selectedGame == null ? lotterySix.getCompletedGames().get(0) : selectedGame;
                    StringBuilder str = new StringBuilder(ChatColor.stripColor(LotteryUtils.formatPlaceholders(null, lotterySix.discordSRVDrawResultAnnouncementDescription, lotterySix, game)));
                    boolean exceedLimit = false;

                    List<PlayerBets> playerBets = game.getPlayerBets(uuid);
                    if (!playerBets.isEmpty()) {
                        str.append("\n\n").append(lotterySix.discordSRVSlashCommandsViewPastDrawYourBets).append("\n");
                        List<PlayerWinnings> winningsList = game.getSortedPlayerWinnings(uuid);
                        Set<UUID> displayedBets = new HashSet<>();
                        for (PlayerWinnings winnings : winningsList) {
                            UUID betId = winnings.getWinningBetId();
                            if (!displayedBets.contains(betId)) {
                                displayedBets.add(betId);
                                StringBuilder sb = new StringBuilder();
                                if (winnings.isBulk(game)) {
                                    List<PlayerWinnings> winningsForBet = game.getPlayerWinningsByBet(betId).values().stream().flatMap(each -> each.stream()).collect(Collectors.toList());
                                    PlayerBets bets = winnings.getWinningBet(game);
                                    BetNumbers betNumbers = bets.getChosenNumbers();
                                    String[] numberStrings = betNumbers.toString().replace("/ ", "/\n").split("\n");
                                    int i = 0;
                                    for (String numbers : numberStrings) {
                                        int finalI = i;
                                        Optional<PlayerWinnings> optWinnings = winningsForBet.stream().filter(each -> each.getWinningCombination().getNumbers().equals(betNumbers.getSet(finalI))).findFirst();
                                        if (optWinnings.isPresent()) {
                                            sb.append("**").append(numbers).append("**\n");
                                            PlayerWinnings localWinnings = optWinnings.get();
                                            sb.append("**").append(ChatColor.stripColor(lotterySix.winningsDescription
                                                    .replace("{Tier}", lotterySix.tierNames.get(localWinnings.getTier()))
                                                    .replace("{Winnings}", StringUtils.formatComma(localWinnings.getWinnings()))
                                                    .replace("{UnitPrice}", StringUtils.formatComma(game.getPricePerBet(localWinnings.getWinningBet(game).getType()))))).append("**");
                                        } else {
                                            sb.append(ChatColor.stripColor(numbers)).append("\n");
                                            sb.append(ChatColor.stripColor(lotterySix.winningsDescription
                                                    .replace("{Tier}", LotteryUtils.formatPlaceholders(null, lotterySix.guiLastResultsNoWinnings, lotterySix, game))
                                                    .replace("{Winnings}", StringUtils.formatComma(0))
                                                    .replace("{UnitPrice}", StringUtils.formatComma(game.getPricePerBet(bets.getType())))));
                                        }
                                        sb.append("\n");
                                        i++;
                                    }
                                    sb.append("**").append(ChatColor.stripColor(lotterySix.bulkWinningsDescription
                                            .replace("{HighestTier}", lotterySix.tierNames.get(winnings.getTier()))
                                            .replace("{Winnings}", StringUtils.formatComma(winningsForBet.stream().mapToLong(each -> each.getWinnings()).sum()))
                                            .replace("{UnitPrice}", StringUtils.formatComma(game.getPricePerBet(winnings.getWinningBet(game).getType()))))).append("**\n\n");
                                } else {
                                    sb.append("**").append(winnings.getWinningBet(game).getChosenNumbers().toString().replace("/ ", "/\n")).append("**\n");
                                    if (winnings.isCombination(game)) {
                                        Map<PrizeTier, List<PlayerWinnings>> winningsForBet = game.getPlayerWinningsByBet(betId);
                                        for (PrizeTier prizeTier : PrizeTier.values()) {
                                            List<PlayerWinnings> playerWinnings = winningsForBet.get(prizeTier);
                                            if (playerWinnings != null && !playerWinnings.isEmpty()) {
                                                sb.append(ChatColor.stripColor(lotterySix.multipleWinningsDescription
                                                        .replace("{Tier}", lotterySix.tierNames.get(prizeTier))
                                                        .replace("{Times}", String.valueOf(playerWinnings.size()))
                                                        .replace("{Winnings}", StringUtils.formatComma(playerWinnings.get(0).getWinnings()))
                                                        .replace("{UnitPrice}", StringUtils.formatComma(game.getPricePerBet(winnings.getWinningBet(game).getType()))))).append("\n");
                                            }
                                        }
                                        sb.append("**").append(ChatColor.stripColor(lotterySix.combinationWinningsDescription
                                                .replace("{HighestTier}", lotterySix.tierNames.get(winnings.getTier()))
                                                .replace("{Winnings}", StringUtils.formatComma(winningsForBet.values().stream().flatMap(each -> each.stream()).mapToLong(each -> each.getWinnings()).sum()))
                                                .replace("{UnitPrice}", StringUtils.formatComma(game.getPricePerBet(winnings.getWinningBet(game).getType()))))).append("**\n\n");
                                    } else {
                                        sb.append("**").append(ChatColor.stripColor(lotterySix.winningsDescription
                                                .replace("{Tier}", lotterySix.tierNames.get(winnings.getTier()))
                                                .replace("{Winnings}", StringUtils.formatComma(winnings.getWinnings()))
                                                .replace("{UnitPrice}", StringUtils.formatComma(game.getPricePerBet(winnings.getWinningBet(game).getType()))))).append("**\n\n");
                                    }
                                }
                                if (str.length() + sb.length() < 4090) {
                                    str.append(sb);
                                } else {
                                    exceedLimit = true;
                                    break;
                                }
                            }
                        }
                        if (!exceedLimit) {
                            for (PlayerBets bets : playerBets) {
                                if (!displayedBets.contains(bets.getBetId())) {
                                    StringBuilder sb = new StringBuilder();
                                    sb.append(bets.getChosenNumbers().toString().replace("/ ", "/\n")).append("\n").append(ChatColor.stripColor(lotterySix.winningsDescription
                                                    .replace("{Tier}", lotterySix.discordSRVSlashCommandsViewPastDrawNoWinnings)
                                                    .replace("{Winnings}", StringUtils.formatComma(0))
                                                    .replace("{UnitPrice}", StringUtils.formatComma(game.getPricePerBet(bets.getType()))))).append("\n\n");
                                    if (str.length() + sb.length() < 4090) {
                                        str.append(sb);
                                    } else {
                                        exceedLimit = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    if (exceedLimit) {
                        str.append("...");
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
                OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                StringBuilder sb = new StringBuilder();
                for (String line : lotterySix.discordSRVSlashCommandsViewCurrentBetsSubTitle) {
                    sb.append(ChatColor.stripColor(LotteryUtils.formatPlaceholders(player, line, lotterySix, game))).append("\n");
                }
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                if (bets.isEmpty()) {
                    sb.append(lotterySix.discordSRVSlashCommandsViewCurrentBetsNoBets);
                } else {
                    for (PlayerBets bet : bets) {
                        StringBuilder str = new StringBuilder();
                        str.append("**").append(bet.getChosenNumbers().toString().replace("/ ", "/\n")).append("**\n").append(ChatColor.stripColor(lotterySix.ticketDescription
                                .replace("{Price}", StringUtils.formatComma(bet.getBet()))
                                .replace("{UnitPrice}", StringUtils.formatComma(lotterySix.pricePerBet / bet.getType().getDivisor())))).append("\n\n");
                        if (str.length() + sb.length() < 4090) {
                            sb.append(str);
                        } else {
                            sb.append("...");
                            break;
                        }
                    }
                }
                String description = sb.charAt(sb.length() - 1) == '\n' ? sb.substring(0, sb.length() - 1) : sb.toString();

                EmbedBuilder builder = new EmbedBuilder()
                        .setColor(Color.GREEN)
                        .setTitle(ChatColor.stripColor(LotteryUtils.formatPlaceholders(player, lotterySix.discordSRVSlashCommandsViewCurrentBetsTitle, lotterySix, game)))
                        .setDescription(description)
                        .setThumbnail(lotterySix.discordSRVSlashCommandsViewCurrentBetsThumbnailURL);
                event.replyEmbeds(builder.build()).setEphemeral(true).queue();
            }
            return;
        } else if (LotterySixPlugin.getInstance().discordSRVSlashCommandsViewNumberStatisticsEnabled && label.equalsIgnoreCase(NUMBER_STATS_LABEL)) {
            CompletedLotterySixGame game = lotterySix.getCompletedGames().isEmpty() ? null : lotterySix.getCompletedGames().get(0);
            EmbedBuilder builder = new EmbedBuilder()
                    .setColor(Color.CYAN)
                    .setTitle(ChatColor.stripColor(LotteryUtils.formatPlaceholders(null, lotterySix.guiNumberStatisticsTitle, lotterySix, game)))
                    .setDescription(ChatColor.stripColor(String.join("\n", LotteryUtils.formatPlaceholders(null, lotterySix.guiNumberStatisticsNote, lotterySix, game))))
                    .setThumbnail(lotterySix.discordSRVSlashCommandsViewNumberStatisticsThumbnailURL);

            List<String> numbers = new ArrayList<>(lotterySix.numberOfChoices);

            for (int i = 1; i <= lotterySix.numberOfChoices; i++) {
                numbers.add(String.format("%2s", i) + " / "
                    + ChatColor.stripColor(LotteryUtils.formatPlaceholders(null, "{" + i + "LastDrawn}", lotterySix, game)) + " / "
                    + ChatColor.stripColor(LotteryUtils.formatPlaceholders(null, "{" + i + "TimesDrawn}", lotterySix, game)));
            }

            String title = lotterySix.discordSRVSlashCommandsViewNumberStatisticsNumberField + " / "
                    + lotterySix.discordSRVSlashCommandsViewNumberStatisticsLastDrawnField + " / "
                    + lotterySix.discordSRVSlashCommandsViewNumberStatisticsTimesDrawnField;

            builder.addField(title, String.join("\n", numbers), false);

            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
            return;
        }
    }


}
