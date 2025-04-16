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
import com.loohp.lotterysix.game.lottery.CompletedLotterySixGame;
import com.loohp.lotterysix.game.lottery.CompletedLotterySixGameIndex;
import com.loohp.lotterysix.game.lottery.GameNumber;
import com.loohp.lotterysix.game.objects.PlayerBets;
import com.loohp.lotterysix.game.objects.PlayerWinnings;
import com.loohp.lotterysix.game.objects.PrizeTier;
import com.loohp.lotterysix.game.objects.betnumbers.BetNumbers;
import com.loohp.lotterysix.utils.LotteryUtils;
import com.loohp.lotterysix.utils.StringUtils;
import com.loohp.lotterysix.utils.SyncUtils;
import com.loohp.platformscheduler.Scheduler;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Emoji;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.GenericComponentInteractionCreateEvent;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SelectionMenuEvent;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.selections.SelectionMenu;
import net.md_5.bungee.api.ChatColor;

import java.awt.Color;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.loohp.lotterysix.discordsrv.DiscordSRVHook.INTERACTION_LABEL_PREFIX;

public class PastDrawInteraction extends DiscordInteraction {

    public static final String INTERACTION_LABEL = INTERACTION_LABEL_PREFIX + "pastdraw";
    public static final String SELECTION_YEAR_MENU_LABEL = INTERACTION_LABEL_PREFIX + "pastdraw_year_menu_";
    public static final String SELECTION_YEAR_OPTION_LABEL = INTERACTION_LABEL_PREFIX + "pastdraw_year_menu_option_";
    public static final String SELECTION_MENU_LABEL = INTERACTION_LABEL_PREFIX + "pastdraw_menu_";
    public static final String SELECTION_OPTION_LABEL = INTERACTION_LABEL_PREFIX + "pastdraw_menu_option_";
    public static final String SELECTION_MENU_NEWER_LABEL = INTERACTION_LABEL_PREFIX + "pastdraw_menu_newer";
    public static final String SELECTION_MENU_OLDER_LABEL = INTERACTION_LABEL_PREFIX + "pastdraw_menu_older";

    public PastDrawInteraction() {
        super(INTERACTION_LABEL, false);
    }

    @Override
    public boolean doOccupyEntireRow(UUID uuid) {
        return false;
    }

    @Override
    public List<ActionRow> getActionRows(UUID uuid) {
        return Collections.singletonList(ActionRow.of(Button.danger(INTERACTION_LABEL, instance.discordSRVSlashCommandsViewPastDrawTitle).withEmoji(Emoji.fromUnicode("\uD83D\uDD52"))));
    }

    @Override
    public void handle(GenericComponentInteractionCreateEvent event) {
        String componentId = event.getComponent().getId();
        if (componentId.equals(SELECTION_MENU_NEWER_LABEL) || componentId.equals(SELECTION_MENU_OLDER_LABEL)) {
            GameNumber gameNumber = GameNumber.fromString(event.getMessage().getActionRows().get(1).getComponents().get(0).getId().substring(SELECTION_MENU_LABEL.length()));
            int currentPosition = instance.getCompletedGames().indexOf(instance.getCompletedGames().get(gameNumber));
            if (componentId.equals(SELECTION_MENU_OLDER_LABEL)) {
                currentPosition += 25;
            } else {
                currentPosition -= 25;
            }
            int startPosition = Math.max(0, currentPosition - currentPosition % 25);
            List<CompletedLotterySixGameIndex> list = new ArrayList<>();
            for (int i = startPosition; i < startPosition + 25 && i < instance.getCompletedGames().size(); i++) {
                CompletedLotterySixGameIndex gameIndex = instance.getCompletedGames().getIndex(i);
                list.add(gameIndex);
            }

            SelectionMenu.Builder yearBuilder = SelectionMenu.create(SELECTION_YEAR_MENU_LABEL + list.get(0).getGameNumber().getYear()).setMinValues(1).setMaxValues(1);
            list.stream().map(each -> each.getGameNumber().getYear()).distinct().forEach(year -> yearBuilder.addOption(year.toString(), SELECTION_YEAR_OPTION_LABEL + year));
            yearBuilder.setDefaultValues(Collections.singleton(SELECTION_YEAR_OPTION_LABEL + list.get(0).getGameNumber().getYear().toString()));

            SelectionMenu.Builder menuBuilder = SelectionMenu.create(SELECTION_MENU_LABEL + list.get(0).getGameNumber()).setMinValues(1).setMaxValues(1);
            for (CompletedLotterySixGameIndex index : list) {
                String gn = index.getGameNumber().toString();
                menuBuilder.addOption(ChatColor.stripColor(LotteryUtils.formatPlaceholders(null, "{GameNumber}", instance, index)), SELECTION_OPTION_LABEL + gn);
            }

            Button newer = Button.success(SELECTION_MENU_NEWER_LABEL, Emoji.fromUnicode("\u2B05\uFE0F"));
            if (startPosition == 0) {
                newer = newer.asDisabled();
            }
            Button older = Button.danger(SELECTION_MENU_OLDER_LABEL, Emoji.fromUnicode("\u27A1\uFE0F"));
            if (startPosition + 25 >= instance.getCompletedGames().size()) {
                older = older.asDisabled();
            }

            event.getHook().editOriginalComponents().setActionRows(ActionRow.of(yearBuilder.build()), ActionRow.of(menuBuilder.build()), ActionRow.of(newer, older), ActionRow.of(getMainMenuButton())).queue();
        } else if (componentId.startsWith(SELECTION_YEAR_MENU_LABEL)) {
            Year selectedYear = Year.of(Integer.parseInt(((SelectionMenuEvent) event).getValues().get(0).substring(SELECTION_YEAR_OPTION_LABEL.length())));

            int currentPosition = 0;
            for (int i = 0; i < instance.getCompletedGames().size(); i++) {
                CompletedLotterySixGameIndex gameIndex = instance.getCompletedGames().getIndex(i);
                if (gameIndex.getGameNumber().getYear().equals(selectedYear)) {
                    currentPosition = i;
                    break;
                }
            }
            int startPosition = Math.max(0, currentPosition - currentPosition % 25);
            List<CompletedLotterySixGameIndex> list = new ArrayList<>();
            for (int i = startPosition; i < startPosition + 25 && i < instance.getCompletedGames().size(); i++) {
                CompletedLotterySixGameIndex gameIndex = instance.getCompletedGames().getIndex(i);
                list.add(gameIndex);
            }

            SelectionMenu.Builder yearBuilder = SelectionMenu.create(SELECTION_YEAR_MENU_LABEL + list.get(0).getGameNumber().getYear()).setMinValues(1).setMaxValues(1);
            list.stream().map(each -> each.getGameNumber().getYear()).distinct().forEach(year -> yearBuilder.addOption(year.toString(), SELECTION_YEAR_OPTION_LABEL + year));
            yearBuilder.setDefaultValues(Collections.singleton(SELECTION_YEAR_OPTION_LABEL + selectedYear.toString()));

            SelectionMenu.Builder menuBuilder = SelectionMenu.create(SELECTION_MENU_LABEL + list.get(0).getGameNumber()).setMinValues(1).setMaxValues(1);
            for (CompletedLotterySixGameIndex index : list) {
                String gn = index.getGameNumber().toString();
                menuBuilder.addOption(ChatColor.stripColor(LotteryUtils.formatPlaceholders(null, "{GameNumber}", instance, index)), SELECTION_OPTION_LABEL + gn);
            }

            Button newer = Button.success(SELECTION_MENU_NEWER_LABEL, Emoji.fromUnicode("\u2B05\uFE0F"));
            if (startPosition == 0) {
                newer = newer.asDisabled();
            }
            Button older = Button.danger(SELECTION_MENU_OLDER_LABEL, Emoji.fromUnicode("\u27A1\uFE0F"));
            if (startPosition + 25 >= instance.getCompletedGames().size()) {
                older = older.asDisabled();
            }

            event.getHook().editOriginalComponents().setActionRows(ActionRow.of(yearBuilder.build()), ActionRow.of(menuBuilder.build()), ActionRow.of(newer, older), ActionRow.of(getMainMenuButton())).queue();
        } else {
            String discordUserId = event.getUser().getId();
            UUID uuid = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(discordUserId);
            String gameNumber;
            if (event instanceof SelectionMenuEvent) {
                gameNumber = ((SelectionMenuEvent) event).getValues().get(0).substring(SELECTION_OPTION_LABEL.length());
            } else {
                gameNumber = "";
            }
            CompletedLotterySixGame selectedGame;
            if (gameNumber.isEmpty()) {
                selectedGame = null;
            } else {
                try {
                    selectedGame = instance.getCompletedGames().get(GameNumber.fromString(gameNumber));
                } catch (Exception e) {
                    event.getHook().editOriginal(instance.discordSRVSlashCommandsViewPastDrawNoResults).setActionRows().setEmbeds().retainFiles(Collections.emptyList()).queue();
                    return;
                }
            }

            if (instance.getCompletedGames().isEmpty() && selectedGame == null) {
                event.getHook().editOriginal(instance.discordSRVSlashCommandsViewPastDrawNoResults).setActionRows().setEmbeds().retainFiles(Collections.emptyList()).queue();
            } else {
                Scheduler.runTaskAsynchronously(LotterySixPlugin.plugin, () -> {
                    SyncUtils.blockUntilTrue(() -> !instance.isGameLocked());

                    CompletedLotterySixGame game = selectedGame == null ? instance.getCompletedGames().get(0) : selectedGame;
                    StringBuilder str = new StringBuilder(ChatColor.stripColor(LotteryUtils.formatPlaceholders(null, instance.discordSRVDrawResultAnnouncementDescription, instance, game)));
                    boolean exceedLimit = false;

                    if (uuid != null) {
                        List<PlayerBets> playerBets = game.getPlayerBets(uuid);
                        if (!playerBets.isEmpty()) {
                            str.append("\n\n").append(instance.discordSRVSlashCommandsViewPastDrawYourBets).append("\n");
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
                                                sb.append("**").append(ChatColor.stripColor(instance.winningsDescription
                                                        .replace("{Tier}", instance.tierNames.get(localWinnings.getTier()))
                                                        .replace("{Winnings}", StringUtils.formatComma(localWinnings.getWinnings()))
                                                        .replace("{UnitPrice}", StringUtils.formatComma(game.getPricePerBet(localWinnings.getWinningBet(game).getType()))))).append("**");
                                            } else {
                                                sb.append(ChatColor.stripColor(numbers)).append("\n");
                                                sb.append(ChatColor.stripColor(instance.winningsDescription
                                                        .replace("{Tier}", LotteryUtils.formatPlaceholders(null, instance.guiLastResultsNoWinnings, instance, game))
                                                        .replace("{Winnings}", StringUtils.formatComma(0))
                                                        .replace("{UnitPrice}", StringUtils.formatComma(game.getPricePerBet(bets.getType())))));
                                            }
                                            sb.append("\n");
                                            i++;
                                        }
                                        sb.append("**").append(ChatColor.stripColor(instance.bulkWinningsDescription
                                                .replace("{HighestTier}", instance.tierNames.get(winnings.getTier()))
                                                .replace("{Winnings}", StringUtils.formatComma(winningsForBet.stream().mapToLong(each -> each.getWinnings()).sum()))
                                                .replace("{UnitPrice}", StringUtils.formatComma(game.getPricePerBet(winnings.getWinningBet(game).getType()))))).append("**\n\n");
                                    } else {
                                        sb.append("**").append(winnings.getWinningBet(game).getChosenNumbers().toString().replace("/ ", "/\n")).append("**\n");
                                        if (winnings.isCombination(game)) {
                                            Map<PrizeTier, List<PlayerWinnings>> winningsForBet = game.getPlayerWinningsByBet(betId);
                                            for (PrizeTier prizeTier : PrizeTier.values()) {
                                                List<PlayerWinnings> playerWinnings = winningsForBet.get(prizeTier);
                                                if (playerWinnings != null && !playerWinnings.isEmpty()) {
                                                    sb.append(ChatColor.stripColor(instance.multipleWinningsDescription
                                                            .replace("{Tier}", instance.tierNames.get(prizeTier))
                                                            .replace("{Times}", String.valueOf(playerWinnings.size()))
                                                            .replace("{Winnings}", StringUtils.formatComma(playerWinnings.get(0).getWinnings()))
                                                            .replace("{UnitPrice}", StringUtils.formatComma(game.getPricePerBet(winnings.getWinningBet(game).getType()))))).append("\n");
                                                }
                                            }
                                            sb.append("**").append(ChatColor.stripColor(instance.combinationWinningsDescription
                                                    .replace("{HighestTier}", instance.tierNames.get(winnings.getTier()))
                                                    .replace("{Winnings}", StringUtils.formatComma(winningsForBet.values().stream().flatMap(each -> each.stream()).mapToLong(each -> each.getWinnings()).sum()))
                                                    .replace("{UnitPrice}", StringUtils.formatComma(game.getPricePerBet(winnings.getWinningBet(game).getType()))))).append("**\n\n");
                                        } else {
                                            sb.append("**").append(ChatColor.stripColor(instance.winningsDescription
                                                    .replace("{Tier}", instance.tierNames.get(winnings.getTier()))
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
                                        sb.append(bets.getChosenNumbers().toString().replace("/ ", "/\n")).append("\n").append(ChatColor.stripColor(instance.winningsDescription
                                                .replace("{Tier}", instance.discordSRVSlashCommandsViewPastDrawNoWinnings)
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
                    }

                    String description = str.toString();
                    EmbedBuilder builder = new EmbedBuilder()
                            .setColor(Color.YELLOW)
                            .setTitle(ChatColor.stripColor(LotteryUtils.formatPlaceholders(null, instance.discordSRVDrawResultAnnouncementTitle, instance, game)))
                            .setDescription(description.endsWith("\n") ? description.substring(0, str.length() - 1) : description)
                            .setThumbnail(instance.discordSRVSlashCommandsViewPastDrawThumbnailURL);

                    int currentPosition = instance.getCompletedGames().indexOf(game);
                    int startPosition = Math.max(0, currentPosition - currentPosition % 25);
                    List<CompletedLotterySixGameIndex> list = new ArrayList<>();
                    for (int i = startPosition; i < startPosition + 25 && i < instance.getCompletedGames().size(); i++) {
                        CompletedLotterySixGameIndex gameIndex = instance.getCompletedGames().getIndex(i);
                        list.add(gameIndex);
                    }

                    SelectionMenu.Builder yearBuilder = SelectionMenu.create(SELECTION_YEAR_MENU_LABEL + list.get(0).getGameNumber().getYear()).setMinValues(1).setMaxValues(1);
                    list.stream().map(each -> each.getGameNumber().getYear()).distinct().forEach(year -> yearBuilder.addOption(year.toString(), SELECTION_YEAR_OPTION_LABEL + year));
                    yearBuilder.setDefaultValues(Collections.singleton(SELECTION_YEAR_OPTION_LABEL + game.getGameNumber().getYear().toString()));

                    SelectionMenu.Builder menuBuilder = SelectionMenu.create(SELECTION_MENU_LABEL + list.get(0).getGameNumber()).setMinValues(1).setMaxValues(1);
                    for (CompletedLotterySixGameIndex index : list) {
                        String gn = index.getGameNumber().toString();
                        menuBuilder.addOption(ChatColor.stripColor(LotteryUtils.formatPlaceholders(null, "{GameNumber}", instance, index)), SELECTION_OPTION_LABEL + gn);
                    }
                    menuBuilder.setDefaultValues(Collections.singleton(SELECTION_OPTION_LABEL + game.getGameNumber()));

                    Button newer = Button.success(SELECTION_MENU_NEWER_LABEL, Emoji.fromUnicode("\u2B05\uFE0F"));
                    if (startPosition == 0) {
                        newer = newer.asDisabled();
                    }
                    Button older = Button.danger(SELECTION_MENU_OLDER_LABEL, Emoji.fromUnicode("\u27A1\uFE0F"));
                    if (startPosition + 25 >= instance.getCompletedGames().size()) {
                        older = older.asDisabled();
                    }

                    event.getHook().editOriginalEmbeds(builder.build()).setActionRows(ActionRow.of(yearBuilder.build()), ActionRow.of(menuBuilder.build()), ActionRow.of(newer, older), ActionRow.of(getMainMenuButton())).retainFiles(Collections.emptyList()).queue();
                });
            }
        }
    }

}
