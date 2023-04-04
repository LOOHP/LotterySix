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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.loohp.lotterysix.LotterySixPlugin;
import com.loohp.lotterysix.discordsrv.DiscordInteraction;
import com.loohp.lotterysix.game.lottery.PlayableLotterySixGame;
import com.loohp.lotterysix.game.objects.AddBetResult;
import com.loohp.lotterysix.game.objects.BetUnitType;
import com.loohp.lotterysix.game.objects.PlayerPreferenceKey;
import com.loohp.lotterysix.game.objects.betnumbers.BetNumbers;
import com.loohp.lotterysix.game.objects.betnumbers.BetNumbersBuilder;
import com.loohp.lotterysix.game.objects.betnumbers.BetNumbersType;
import com.loohp.lotterysix.utils.LotteryUtils;
import com.loohp.lotterysix.utils.MathUtils;
import com.loohp.lotterysix.utils.StringUtils;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Emoji;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.GenericComponentInteractionCreateEvent;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SelectionMenuEvent;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Component;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.selections.SelectOption;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.selections.SelectionMenu;
import github.scarsz.discordsrv.dependencies.jda.api.requests.restaction.WebhookMessageUpdateAction;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PlaceBetInteraction extends DiscordInteraction {

    public static final String INTERACTION_LABEL = "ls_place_bet";

    public static final String SINGLE_ENTRY_LABEL = "ls_place_bet_single";
    public static final String MULTIPLE_ENTRY_LABEL = "ls_place_bet_multiple";
    public static final String BANKER_ENTRY_LABEL = "ls_place_bet_banker";
    public static final String RANDOM_ENTRY_LABEL = "ls_place_bet_random";

    public static final String SINGLE_RANDOM_ENTRY_LABEL = "ls_place_bet_single_random";
    public static final String MULTIPLE_RANDOM_ENTRY_LABEL = "ls_place_bet_multiple_random";
    public static final String BANKER_RANDOM_ENTRY_LABEL = "ls_place_bet_banker_random";

    public static final String SINGLE_ENTRY_SELECTION_LABEL = "ls_place_bet_single_selection_";
    public static final String SINGLE_ENTRY_SELECTION_OPTION_LABEL = "ls_place_bet_single_number_";
    public static final String SINGLE_ENTRY_CONFIRM_LABEL = "ls_place_bet_single_confirm_";

    public static final String MULTIPLE_ENTRY_SELECTION_LABEL = "ls_place_bet_multiple_selection_";
    public static final String MULTIPLE_ENTRY_SELECTION_OPTION_LABEL = "ls_place_bet_multiple_number_";
    public static final String MULTIPLE_ENTRY_CONFIRM_LABEL = "ls_place_bet_multiple_confirm_";

    public static final String BANKER_ENTRY_SELECTION_LABEL = "ls_place_bet_banker_selection_";
    public static final String BANKER_ENTRY_SELECTION_OPTION_LABEL = "ls_place_bet_banker_number_";
    public static final String BANKER_ENTRY_CONFIRM_LABEL = "ls_place_bet_banker_confirm_";

    public static final String SINGLE_RANDOM_ENTRY_CONFIRM_LABEL = "ls_place_bet_single_random_";

    public static final String MULTIPLE_RANDOM_ENTRY_SIZE_LABEL = "ls_place_bet_multiple_random_selection_";
    public static final String MULTIPLE_RANDOM_ENTRY_SIZE_OPTION_LABEL = "ls_place_bet_multiple_random_size_";
    public static final String MULTIPLE_RANDOM_ENTRY_CONFIRM_LABEL = "ls_place_bet_multiple_random_";

    public static final String BANKER_RANDOM_ENTRY_BANKER_LABEL = "ls_place_bet_banker_random_bankersel_";
    public static final String BANKER_RANDOM_ENTRY_BANKER_OPTION_LABEL = "ls_place_bet_banker_random_banker_";
    public static final String BANKER_RANDOM_ENTRY_SIZE_LABEL = "ls_place_bet_banker_random_selection_";
    public static final String BANKER_RANDOM_ENTRY_SIZE_OPTION_LABEL = "ls_place_bet_banker_random_size_";
    public static final String BANKER_RANDOM_ENTRY_CONFIRM_LABEL = "ls_place_bet_banker_random_";

    public static final String PLACE_BET_CONFIRM_LABEL = "ls_place_bet_{Number}_{Unit}";
    public static final Pattern PLACE_BET_CONFIRM_LABEL_PATTERN = Pattern.compile("^ls_place_bet_([^_]+)_(.+)$");

    public static final Color OFFSET_WHITE = new Color(0xFFFFFE);

    private final Map<UUID, Set<Integer>> chosenNumbers;
    private final Map<UUID, Set<Integer>> chosenBankers;
    private final Set<UUID> choosingBankers;
    private final Map<UUID, List<BetNumbers>> confirmNumbers;
    private final Map<UUID, int[]> randomSizeSelection;

    public PlaceBetInteraction() {
        super(INTERACTION_LABEL, true);
        Cache<UUID, Set<Integer>> chosenNumbers = CacheBuilder.newBuilder().expireAfterAccess(15, TimeUnit.MINUTES).build();
        this.chosenNumbers = chosenNumbers.asMap();
        Cache<UUID, Set<Integer>> chosenBankers = CacheBuilder.newBuilder().expireAfterAccess(15, TimeUnit.MINUTES).build();
        this.chosenBankers = chosenBankers.asMap();
        Cache<UUID, Boolean> choosingBankers = CacheBuilder.newBuilder().expireAfterAccess(15, TimeUnit.MINUTES).build();
        this.choosingBankers = Collections.newSetFromMap(choosingBankers.asMap());
        Cache<UUID, List<BetNumbers>> confirmNumbers = CacheBuilder.newBuilder().expireAfterAccess(15, TimeUnit.MINUTES).build();
        this.confirmNumbers = confirmNumbers.asMap();
        Cache<UUID, int[]> randomSizeSelection = CacheBuilder.newBuilder().expireAfterAccess(15, TimeUnit.MINUTES).build();
        this.randomSizeSelection = randomSizeSelection.asMap();
    }

    public List<SelectionMenu.Builder> createNumberSelectionMenu(String menuId, String optionsId) {
        return createNumberSelectionMenu(menuId, optionsId, Collections.emptySet());
    }

    public List<SelectionMenu.Builder> createNumberSelectionMenu(String menuId, String optionsId, Set<Integer> hidden) {
        List<SelectionMenu.Builder> list = new ArrayList<>(2);
        int u = 1;
        SelectionMenu.Builder current = SelectionMenu.create(menuId + u++);
        for (int i = 1; i <= instance.numberOfChoices; i++) {
            if (!hidden.contains(i)) {
                if (current.getOptions().size() >= 25) {
                    list.add(current);
                    current = SelectionMenu.create(menuId + u++);
                }
                current.addOption(String.valueOf(i), optionsId + i);
            }
        }
        list.add(current);
        return list;
    }

    public List<SelectionMenu.Builder> createSizeSelectionMenu(String menuId, String optionsId, int min, int max, String optionFormat) {
        List<SelectionMenu.Builder> list = new ArrayList<>(2);
        int u = 1;
        SelectionMenu.Builder current = SelectionMenu.create(menuId + u++);
        for (int i = min; i <= max; i++) {
            if (current.getOptions().size() >= 25) {
                list.add(current);
                current = SelectionMenu.create(menuId + u++);
            }
            current.addOption(optionFormat.replace("{Count}", String.valueOf(i)), optionsId + i);
        }
        list.add(current);
        return list;
    }

    @Override
    public boolean doOccupyEntireRow(UUID uuid) {
        return false;
    }

    @Override
    public List<ActionRow> getActionRows(UUID uuid) {
        Button button = Button.success(INTERACTION_LABEL, instance.discordSRVSlashCommandsPlaceBetTitle).withEmoji(Emoji.fromUnicode("\uD83D\uDCB0"));
        if (instance.getCurrentGame() == null) {
            button = button.asDisabled();
        }
        return Collections.singletonList(ActionRow.of(button));
    }

    @Override
    public void handle(GenericComponentInteractionCreateEvent event) {
        String discordUserId = event.getUser().getId();
        UUID uuid = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(discordUserId);
        if (uuid == null) {
            event.editMessage(instance.discordSRVSlashCommandsGlobalMessagesNotLinked).setActionRows().setEmbeds().queue();
            return;
        }
        PlayableLotterySixGame game = instance.getCurrentGame();
        if (game == null) {
            event.editMessage(instance.discordSRVSlashCommandsPlaceBetNoGame).setActionRows().setEmbeds().queue();
            return;
        }
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        String componentId = event.getComponent().getId();
        event.deferEdit().queue();
        if (componentId.equals(INTERACTION_LABEL)) {
            List<String> separator = Arrays.asList("", "");
            String description = ChatColor.stripColor(Stream.of(
                    Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiSelectNewBetTypeSingle, instance)),
                    separator.stream(),
                    Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiSelectNewBetTypeMultiple, instance)),
                    separator.stream(),
                    Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiSelectNewBetTypeBanker, instance)),
                    separator.stream(),
                    Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiSelectNewBetTypeRandom, instance))
            ).flatMap(Function.identity()).collect(Collectors.joining("\n")));

            Button single = Button.secondary(SINGLE_ENTRY_LABEL, ChatColor.stripColor(instance.betNumbersTypeNames.get(BetNumbersType.SINGLE)));
            Button multiple = Button.primary(MULTIPLE_ENTRY_LABEL, ChatColor.stripColor(instance.betNumbersTypeNames.get(BetNumbersType.MULTIPLE)));
            Button banker = Button.success(BANKER_ENTRY_LABEL, ChatColor.stripColor(instance.betNumbersTypeNames.get(BetNumbersType.BANKER)));
            Button random = Button.danger(RANDOM_ENTRY_LABEL, ChatColor.stripColor(instance.randomEntryName));

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle(instance.discordSRVSlashCommandsPlaceBetTitle)
                    .setColor(Color.ORANGE)
                    .setDescription(description);

            event.getHook().editOriginalEmbeds(embed.build()).setActionRows(ActionRow.of(single, multiple, banker, random)).queue();
        } else if (componentId.equals(SINGLE_ENTRY_LABEL)) {
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle(ChatColor.stripColor(instance.betNumbersTypeNames.get(BetNumbersType.SINGLE)))
                    .setColor(Color.ORANGE)
                    .setDescription("**" + ChatColor.stripColor(LotteryUtils.formatPlaceholders(player, instance.guiNewBetSingleTitle, instance)) + "**");

            UUID selectionId = UUID.randomUUID();
            String menuId = SINGLE_ENTRY_SELECTION_LABEL + selectionId + "_";
            List<ActionRow> actionRows = createNumberSelectionMenu(menuId, SINGLE_ENTRY_SELECTION_OPTION_LABEL).stream()
                    .map(b -> ActionRow.of(b.setMinValues(0).setMaxValues(b.getOptions().size()).build())).collect(Collectors.toList());

            Button confirmButton = Button.secondary(SINGLE_ENTRY_CONFIRM_LABEL + selectionId, instance.discordSRVSlashCommandsComponentsNotYetFinish).withEmoji(Emoji.fromUnicode("\u2B1C")).asDisabled();
            actionRows.add(ActionRow.of(confirmButton));

            event.getHook().editOriginalEmbeds(embed.build()).setActionRows(actionRows).queue();
        } else if (componentId.startsWith(SINGLE_ENTRY_SELECTION_LABEL)) {
            UUID selectionId = UUID.fromString(componentId.substring(SINGLE_ENTRY_SELECTION_LABEL.length(), componentId.lastIndexOf("_")));
            Set<Integer> selected = chosenNumbers.computeIfAbsent(selectionId, k -> new TreeSet<>());
            List<SelectOption> options = ((SelectionMenu) event.getComponent()).getOptions();
            options.stream().map(s -> Integer.parseInt(s.getValue().substring(SINGLE_ENTRY_SELECTION_OPTION_LABEL.length()))).forEach(i -> selected.remove(i));
            ((SelectionMenuEvent) event).getValues().stream().map(s -> Integer.parseInt(s.substring(SINGLE_ENTRY_SELECTION_OPTION_LABEL.length()))).forEach(i -> selected.add(i));

            List<ActionRow> oldActionRows = new ArrayList<>(event.getMessage().getActionRows());
            List<ActionRow> actionRows = new ArrayList<>(oldActionRows.size());
            oldActionRows.remove(oldActionRows.size() - 1);
            outer:
            for (ActionRow actionRow : oldActionRows) {
                for (Component component : actionRow) {
                    if (component instanceof SelectionMenu) {
                        SelectionMenu.Builder menu = ((SelectionMenu) component).createCopy();
                        List<String> defaultOptions = new ArrayList<>();
                        for (int i : selected) {
                            if (menu.getOptions().stream().anyMatch(s -> s.getValue().equals(SINGLE_ENTRY_SELECTION_OPTION_LABEL + i))) {
                                defaultOptions.add(SINGLE_ENTRY_SELECTION_OPTION_LABEL + i);
                            }
                        }
                        menu.setDefaultValues(defaultOptions);
                        actionRows.add(ActionRow.of(menu.build()));
                        continue outer;
                    }
                }
                actionRows.add(actionRow);
            }

            String description = "**" + ChatColor.stripColor(LotteryUtils.formatPlaceholders(player, instance.guiNewBetSingleTitle, instance)) + "**";

            if (selected.size() == 6) {
                Button confirmButton = Button.primary(SINGLE_ENTRY_CONFIRM_LABEL + selectionId, instance.discordSRVSlashCommandsComponentsFinish).withEmoji(Emoji.fromUnicode("\u2705"));
                actionRows.add(ActionRow.of(confirmButton));
                String confirm = ChatColor.stripColor(String.join("\n", LotteryUtils.formatPlaceholders(player, instance.guiNewBetFinishSimple, instance))
                        .replace("{BetUnits}", "1").replace("{Price}", StringUtils.formatComma(instance.pricePerBet)));
                description += "\n\n" + confirm;
            } else {
                Button confirmButton = Button.secondary(SINGLE_ENTRY_CONFIRM_LABEL + selectionId, instance.discordSRVSlashCommandsComponentsNotYetFinish).withEmoji(Emoji.fromUnicode("\u2B1C")).asDisabled();
                actionRows.add(ActionRow.of(confirmButton));
            }

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle(ChatColor.stripColor(instance.betNumbersTypeNames.get(BetNumbersType.SINGLE)))
                    .setColor(Color.ORANGE)
                    .setDescription(description);

            event.getHook().editOriginalComponents().setEmbeds(embed.build()).setActionRows(actionRows).queue();
        } else if (componentId.startsWith(SINGLE_ENTRY_CONFIRM_LABEL)) {
            UUID selectionId = UUID.fromString(componentId.substring(SINGLE_ENTRY_CONFIRM_LABEL.length()));
            Set<Integer> selected = chosenNumbers.remove(selectionId);
            if (selected == null) {
                event.editMessage(instance.discordSRVSlashCommandsGlobalMessagesTimeOut).setActionRows().setEmbeds().queue();
                return;
            }
            BetNumbersBuilder.SingleBuilder builder = BetNumbersBuilder.single(1, instance.numberOfChoices);
            for (int number : selected) {
                builder.addNumber(number);
            }
            handleConfirm(event, player, BetNumbersType.SINGLE, Collections.singletonList(builder.build()));
        } else if (componentId.equals(MULTIPLE_ENTRY_LABEL)) {
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle(ChatColor.stripColor(instance.betNumbersTypeNames.get(BetNumbersType.MULTIPLE)))
                    .setColor(OFFSET_WHITE)
                    .setDescription("**" + ChatColor.stripColor(LotteryUtils.formatPlaceholders(player, instance.guiNewBetMultipleTitle, instance)) + "**");

            UUID selectionId = UUID.randomUUID();
            String menuId = MULTIPLE_ENTRY_SELECTION_LABEL + selectionId + "_";
            List<ActionRow> actionRows = createNumberSelectionMenu(menuId, MULTIPLE_ENTRY_SELECTION_OPTION_LABEL).stream()
                    .map(b -> ActionRow.of(b.setMinValues(0).setMaxValues(b.getOptions().size()).build())).collect(Collectors.toList());

            Button confirmButton = Button.secondary(MULTIPLE_ENTRY_CONFIRM_LABEL + selectionId, instance.discordSRVSlashCommandsComponentsNotYetFinish).withEmoji(Emoji.fromUnicode("\u2B1C")).asDisabled();
            actionRows.add(ActionRow.of(confirmButton));

            event.getHook().editOriginalEmbeds(embed.build()).setActionRows(actionRows).queue();
        } else if (componentId.startsWith(MULTIPLE_ENTRY_SELECTION_LABEL)) {
            UUID selectionId = UUID.fromString(componentId.substring(MULTIPLE_ENTRY_SELECTION_LABEL.length(), componentId.lastIndexOf("_")));
            Set<Integer> selected = chosenNumbers.computeIfAbsent(selectionId, k -> new TreeSet<>());
            List<SelectOption> options = ((SelectionMenu) event.getComponent()).getOptions();
            options.stream().map(s -> Integer.parseInt(s.getValue().substring(MULTIPLE_ENTRY_SELECTION_OPTION_LABEL.length()))).forEach(i -> selected.remove(i));
            ((SelectionMenuEvent) event).getValues().stream().map(s -> Integer.parseInt(s.substring(MULTIPLE_ENTRY_SELECTION_OPTION_LABEL.length()))).forEach(i -> selected.add(i));

            List<ActionRow> oldActionRows = new ArrayList<>(event.getMessage().getActionRows());
            List<ActionRow> actionRows = new ArrayList<>(oldActionRows.size());
            oldActionRows.remove(oldActionRows.size() - 1);
            outer:
            for (ActionRow actionRow : oldActionRows) {
                for (Component component : actionRow) {
                    if (component instanceof SelectionMenu) {
                        SelectionMenu.Builder menu = ((SelectionMenu) component).createCopy();
                        List<String> defaultOptions = new ArrayList<>();
                        for (int i : selected) {
                            if (menu.getOptions().stream().anyMatch(s -> s.getValue().equals(MULTIPLE_ENTRY_SELECTION_OPTION_LABEL + i))) {
                                defaultOptions.add(MULTIPLE_ENTRY_SELECTION_OPTION_LABEL + i);
                            }
                        }
                        menu.setDefaultValues(defaultOptions);
                        actionRows.add(ActionRow.of(menu.build()));
                        continue outer;
                    }
                }
                actionRows.add(actionRow);
            }

            String description = "**" + ChatColor.stripColor(LotteryUtils.formatPlaceholders(player, instance.guiNewBetMultipleTitle, instance)) + "**";

            if (selected.size() >= 7) {
                long units = MathUtils.combinationsCount(selected.size(), 0);
                long price = units * instance.pricePerBet;
                long partial = price / BetUnitType.PARTIAL.getDivisor();
                Button confirmButton = Button.primary(MULTIPLE_ENTRY_CONFIRM_LABEL + selectionId, instance.discordSRVSlashCommandsComponentsFinish).withEmoji(Emoji.fromUnicode("\u2705"));
                actionRows.add(ActionRow.of(confirmButton));

                String confirm = ChatColor.stripColor(String.join("\n", LotteryUtils.formatPlaceholders(player, instance.guiNewBetFinishComplex, instance))
                        .replace("{BetUnits}", StringUtils.formatComma(units))
                        .replace("{Price}", StringUtils.formatComma(price))
                        .replace("{PricePartial}", StringUtils.formatComma(partial)));
                description += "\n\n" + confirm;
            } else {
                Button confirmButton = Button.secondary(MULTIPLE_ENTRY_CONFIRM_LABEL + selectionId, instance.discordSRVSlashCommandsComponentsNotYetFinish).withEmoji(Emoji.fromUnicode("\u2B1C")).asDisabled();
                actionRows.add(ActionRow.of(confirmButton));
            }

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle(ChatColor.stripColor(instance.betNumbersTypeNames.get(BetNumbersType.MULTIPLE)))
                    .setColor(OFFSET_WHITE)
                    .setDescription(description);

            event.getHook().editOriginalComponents().setEmbeds(embed.build()).setActionRows(actionRows).queue();
        } else if (componentId.startsWith(MULTIPLE_ENTRY_CONFIRM_LABEL)) {
            UUID selectionId = UUID.fromString(componentId.substring(MULTIPLE_ENTRY_CONFIRM_LABEL.length()));
            Set<Integer> selected = chosenNumbers.remove(selectionId);
            if (selected == null) {
                event.editMessage(instance.discordSRVSlashCommandsGlobalMessagesTimeOut).setActionRows().setEmbeds().queue();
                return;
            }
            BetNumbersBuilder.MultipleBuilder builder = BetNumbersBuilder.multiple(1, instance.numberOfChoices);
            for (int number : selected) {
                builder.addNumber(number);
            }
            handleConfirm(event, player, BetNumbersType.MULTIPLE, Collections.singletonList(builder.build()));
        } else if (componentId.equals(BANKER_ENTRY_LABEL)) {
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle(ChatColor.stripColor(instance.betNumbersTypeNames.get(BetNumbersType.BANKER)))
                    .setColor(Color.YELLOW)
                    .setDescription("**" + ChatColor.stripColor(LotteryUtils.formatPlaceholders(player, instance.guiNewBetBankerTitle, instance)) + "**");

            UUID selectionId = UUID.randomUUID();
            String menuId = BANKER_ENTRY_SELECTION_LABEL + selectionId + "_";
            List<ActionRow> actionRows = createNumberSelectionMenu(menuId, BANKER_ENTRY_SELECTION_OPTION_LABEL).stream()
                    .map(b -> ActionRow.of(b.setMinValues(0).setMaxValues(b.getOptions().size()).build())).collect(Collectors.toList());

            Button confirmButton = Button.secondary(BANKER_ENTRY_CONFIRM_LABEL + selectionId, instance.discordSRVSlashCommandsComponentsNotYetFinish).withEmoji(Emoji.fromUnicode("\u2B1C")).asDisabled();
            actionRows.add(ActionRow.of(confirmButton));

            choosingBankers.add(selectionId);

            event.getHook().editOriginalEmbeds(embed.build()).setActionRows(actionRows).queue();
        } else if (componentId.startsWith(BANKER_ENTRY_SELECTION_LABEL)) {
            UUID selectionId = UUID.fromString(componentId.substring(BANKER_ENTRY_SELECTION_LABEL.length(), componentId.lastIndexOf("_")));
            Set<Integer> bankers = chosenBankers.computeIfAbsent(selectionId, k -> new TreeSet<>());
            if (choosingBankers.contains(selectionId)) {
                List<SelectOption> options = ((SelectionMenu) event.getComponent()).getOptions();
                options.stream().map(s -> Integer.parseInt(s.getValue().substring(BANKER_ENTRY_SELECTION_OPTION_LABEL.length()))).forEach(i -> bankers.remove(i));
                ((SelectionMenuEvent) event).getValues().stream().map(s -> Integer.parseInt(s.substring(BANKER_ENTRY_SELECTION_OPTION_LABEL.length()))).forEach(i -> bankers.add(i));

                List<ActionRow> oldActionRows = new ArrayList<>(event.getMessage().getActionRows());
                List<ActionRow> actionRows = new ArrayList<>(oldActionRows.size());
                oldActionRows.remove(oldActionRows.size() - 1);
                outer:
                for (ActionRow actionRow : oldActionRows) {
                    for (Component component : actionRow) {
                        if (component instanceof SelectionMenu) {
                            SelectionMenu.Builder menu = ((SelectionMenu) component).createCopy();
                            List<String> defaultOptions = new ArrayList<>();
                            for (int i : bankers) {
                                if (menu.getOptions().stream().anyMatch(s -> s.getValue().equals(BANKER_ENTRY_SELECTION_OPTION_LABEL + i))) {
                                    defaultOptions.add(BANKER_ENTRY_SELECTION_OPTION_LABEL + i);
                                }
                            }
                            menu.setDefaultValues(defaultOptions);
                            actionRows.add(ActionRow.of(menu.build()));
                            continue outer;
                        }
                    }
                    actionRows.add(actionRow);
                }

                String description = "**" + ChatColor.stripColor(LotteryUtils.formatPlaceholders(player, instance.guiNewBetBankerTitle, instance)) + "**";

                if (bankers.size() > 0 && bankers.size() <= 5) {
                    Button confirmButton = Button.success(BANKER_ENTRY_CONFIRM_LABEL + selectionId, instance.discordSRVSlashCommandsComponentsFinishBankers).withEmoji(Emoji.fromUnicode("\u2705"));
                    actionRows.add(ActionRow.of(confirmButton));

                    String confirm = ChatColor.stripColor(String.join("\n", LotteryUtils.formatPlaceholders(player, instance.guiNewBetFinishBankers, instance)));
                    description += "\n\n" + confirm;
                } else {
                    Button confirmButton = Button.secondary(BANKER_ENTRY_CONFIRM_LABEL + selectionId, instance.discordSRVSlashCommandsComponentsNotYetFinish).withEmoji(Emoji.fromUnicode("\u2B1C")).asDisabled();
                    actionRows.add(ActionRow.of(confirmButton));
                }

                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle(ChatColor.stripColor(instance.betNumbersTypeNames.get(BetNumbersType.BANKER)))
                        .setColor(Color.YELLOW)
                        .setDescription(description);

                event.getHook().editOriginalComponents().setEmbeds(embed.build()).setActionRows(actionRows).queue();
            } else {
                Set<Integer> selected = chosenNumbers.computeIfAbsent(selectionId, k -> new TreeSet<>());
                List<SelectOption> options = ((SelectionMenu) event.getComponent()).getOptions();
                options.stream().map(s -> Integer.parseInt(s.getValue().substring(BANKER_ENTRY_SELECTION_OPTION_LABEL.length()))).forEach(i -> selected.remove(i));
                ((SelectionMenuEvent) event).getValues().stream().map(s -> Integer.parseInt(s.substring(BANKER_ENTRY_SELECTION_OPTION_LABEL.length()))).forEach(i -> selected.add(i));

                List<ActionRow> oldActionRows = new ArrayList<>(event.getMessage().getActionRows());
                List<ActionRow> actionRows = new ArrayList<>(oldActionRows.size());
                oldActionRows.remove(oldActionRows.size() - 1);
                outer:
                for (ActionRow actionRow : oldActionRows) {
                    for (Component component : actionRow) {
                        if (component instanceof SelectionMenu) {
                            SelectionMenu.Builder menu = ((SelectionMenu) component).createCopy();
                            List<String> defaultOptions = new ArrayList<>();
                            for (int i : selected) {
                                if (menu.getOptions().stream().anyMatch(s -> s.getValue().equals(BANKER_ENTRY_SELECTION_OPTION_LABEL + i))) {
                                    defaultOptions.add(BANKER_ENTRY_SELECTION_OPTION_LABEL + i);
                                }
                            }
                            menu.setDefaultValues(defaultOptions);
                            actionRows.add(ActionRow.of(menu.build()));
                            continue outer;
                        }
                    }
                    actionRows.add(actionRow);
                }

                String description = "**" + ChatColor.stripColor(LotteryUtils.formatPlaceholders(player, instance.guiNewBetBankerTitle, instance)) + "**";

                if (selected.size() >= 7 - bankers.size()) {
                    long units = MathUtils.combinationsCount(selected.size(), bankers.size());
                    long price = units * instance.pricePerBet;
                    long partial = price / BetUnitType.PARTIAL.getDivisor();
                    Button confirmButton = Button.primary(BANKER_ENTRY_CONFIRM_LABEL + selectionId, instance.discordSRVSlashCommandsComponentsFinish).withEmoji(Emoji.fromUnicode("\u2705"));
                    actionRows.add(ActionRow.of(confirmButton));

                    StringBuilder bankerStr = new StringBuilder();
                    for (int banker : bankers) {
                        bankerStr.append(banker).append(" ");
                    }
                    String confirm = ChatColor.stripColor(String.join("\n", LotteryUtils.formatPlaceholders(player, instance.guiNewBetFinishComplex, instance))
                            .replace("{BetUnits}", StringUtils.formatComma(units))
                            .replace("{Price}", StringUtils.formatComma(price))
                            .replace("{PricePartial}", StringUtils.formatComma(partial)));
                    description += "\n\n**" + bankerStr + "**\n\n" + confirm;
                } else {
                    Button confirmButton = Button.secondary(BANKER_ENTRY_CONFIRM_LABEL + selectionId, instance.discordSRVSlashCommandsComponentsNotYetFinish).withEmoji(Emoji.fromUnicode("\u2B1C")).asDisabled();
                    actionRows.add(ActionRow.of(confirmButton));

                    StringBuilder bankerStr = new StringBuilder();
                    for (int banker : bankers) {
                        bankerStr.append(banker).append(" ");
                    }
                    description += "\n\n**" + bankerStr + "**";
                }

                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle(ChatColor.stripColor(instance.betNumbersTypeNames.get(BetNumbersType.BANKER)))
                        .setColor(Color.YELLOW)
                        .setDescription(description);

                event.getHook().editOriginalComponents().setEmbeds(embed.build()).setActionRows(actionRows).queue();
            }
        } else if (componentId.startsWith(BANKER_ENTRY_CONFIRM_LABEL)) {
            UUID selectionId = UUID.fromString(componentId.substring(BANKER_ENTRY_CONFIRM_LABEL.length()));
            if (choosingBankers.remove(selectionId)) {
                Set<Integer> bankers = chosenBankers.get(selectionId);

                StringBuilder bankerStr = new StringBuilder();
                for (int banker : bankers) {
                    bankerStr.append(banker).append(" ");
                }

                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle(ChatColor.stripColor(instance.betNumbersTypeNames.get(BetNumbersType.BANKER)))
                        .setColor(Color.YELLOW)
                        .setDescription("**" + ChatColor.stripColor(LotteryUtils.formatPlaceholders(player, instance.guiNewBetBankerTitle, instance)) + "**\n\n**" + bankerStr + "**");

                String menuId = BANKER_ENTRY_SELECTION_LABEL + selectionId + "_";
                List<ActionRow> actionRows = createNumberSelectionMenu(menuId, BANKER_ENTRY_SELECTION_OPTION_LABEL, bankers).stream()
                        .map(b -> ActionRow.of(b.setMinValues(0).setMaxValues(b.getOptions().size()).build())).collect(Collectors.toList());

                Button confirmButton = Button.secondary(BANKER_ENTRY_CONFIRM_LABEL + selectionId, instance.discordSRVSlashCommandsComponentsNotYetFinish).withEmoji(Emoji.fromUnicode("\u2B1C")).asDisabled();
                actionRows.add(ActionRow.of(confirmButton));

                event.getHook().editOriginalEmbeds(embed.build()).setActionRows(actionRows).queue();
            } else {
                Set<Integer> bankers = chosenBankers.remove(selectionId);
                Set<Integer> selected = chosenNumbers.remove(selectionId);
                if (bankers == null || selected == null) {
                    event.editMessage(instance.discordSRVSlashCommandsGlobalMessagesTimeOut).setActionRows().setEmbeds().queue();
                    return;
                }
                BetNumbersBuilder.BankerBuilder builder = BetNumbersBuilder.banker(1, instance.numberOfChoices);
                for (int number : bankers) {
                    builder.addNumber(number);
                }
                builder.finishBankers();
                for (int number : selected) {
                    builder.addNumber(number);
                }
                handleConfirm(event, player, BetNumbersType.BANKER, Collections.singletonList(builder.build()));
            }
        } else if (componentId.equals(RANDOM_ENTRY_LABEL)) {
            Button single = Button.secondary(SINGLE_RANDOM_ENTRY_LABEL, ChatColor.stripColor(instance.betNumbersTypeNames.get(BetNumbersType.SINGLE)));
            Button multiple = Button.primary(MULTIPLE_RANDOM_ENTRY_LABEL, ChatColor.stripColor(instance.betNumbersTypeNames.get(BetNumbersType.MULTIPLE)));
            Button banker = Button.success(BANKER_RANDOM_ENTRY_LABEL, ChatColor.stripColor(instance.betNumbersTypeNames.get(BetNumbersType.BANKER)));

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle(instance.discordSRVSlashCommandsPlaceBetTitle)
                    .setColor(Color.RED)
                    .setDescription(ChatColor.stripColor(LotteryUtils.formatPlaceholders(player, instance.guiRandomEntryTitle, instance)));

            event.getHook().editOriginalEmbeds(embed.build()).setActionRows(ActionRow.of(single, multiple, banker)).queue();
        } else if (componentId.equals(SINGLE_RANDOM_ENTRY_LABEL)) {
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle(instance.discordSRVSlashCommandsPlaceBetTitle)
                    .setColor(Color.RED)
                    .setDescription("**" + ChatColor.stripColor(instance.betNumbersTypeNames.get(BetNumbersType.RANDOM)) + "**");

            Button b1 = Button.danger(SINGLE_RANDOM_ENTRY_CONFIRM_LABEL + "1", "1");
            Button b2 = Button.danger(SINGLE_RANDOM_ENTRY_CONFIRM_LABEL + "2", "2");
            Button b5 = Button.danger(SINGLE_RANDOM_ENTRY_CONFIRM_LABEL + "5", "5");
            Button b10 = Button.danger(SINGLE_RANDOM_ENTRY_CONFIRM_LABEL + "10", "10");
            Button b20 = Button.danger(SINGLE_RANDOM_ENTRY_CONFIRM_LABEL + "20", "20");
            Button b40 = Button.danger(SINGLE_RANDOM_ENTRY_CONFIRM_LABEL + "40", "40");
            Button b50 = Button.danger(SINGLE_RANDOM_ENTRY_CONFIRM_LABEL + "50", "50");
            Button b100 = Button.danger(SINGLE_RANDOM_ENTRY_CONFIRM_LABEL + "100", "100");

            event.getHook().editOriginalEmbeds(embed.build()).setActionRows(ActionRow.of(b1, b2, b5, b10), ActionRow.of(b20, b40, b50, b100)).queue();
        } else if (componentId.startsWith(SINGLE_RANDOM_ENTRY_CONFIRM_LABEL)) {
            int count = Integer.parseInt(componentId.substring(SINGLE_RANDOM_ENTRY_CONFIRM_LABEL.length()));
            handleConfirm(event, player, BetNumbersType.RANDOM, BetNumbersBuilder.random(1, instance.numberOfChoices, count).map(b -> b.build()).collect(Collectors.toList()));
        } else if (componentId.equals(MULTIPLE_RANDOM_ENTRY_LABEL)) {
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle(instance.discordSRVSlashCommandsPlaceBetTitle)
                    .setColor(Color.RED)
                    .setDescription("**" + ChatColor.stripColor(instance.betNumbersTypeNames.get(BetNumbersType.MULTIPLE_RANDOM)) + "**");

            UUID selectionId = UUID.randomUUID();
            String menuId = MULTIPLE_RANDOM_ENTRY_SIZE_LABEL + selectionId + "_";
            List<SelectionMenu.Builder> sizeMenu = createSizeSelectionMenu(menuId, MULTIPLE_RANDOM_ENTRY_SIZE_OPTION_LABEL, 7, instance.numberOfChoices, instance.discordSRVSlashCommandsComponentsMultipleRandomSize);
            sizeMenu.get(0).setDefaultValues(Collections.singleton(sizeMenu.get(0).getOptions().get(0).getValue()));
            List<ActionRow> actionRows = sizeMenu.stream().map(m -> ActionRow.of(m.setMinValues(1).setMaxValues(1).build())).collect(Collectors.toList());
            randomSizeSelection.put(selectionId, new int[] {7});

            Button b1 = Button.danger(MULTIPLE_RANDOM_ENTRY_CONFIRM_LABEL + "1_" + selectionId, "1");
            Button b2 = Button.danger(MULTIPLE_RANDOM_ENTRY_CONFIRM_LABEL + "2_" + selectionId, "2");
            Button b5 = Button.danger(MULTIPLE_RANDOM_ENTRY_CONFIRM_LABEL + "5_" + selectionId, "5");
            Button b10 = Button.danger(MULTIPLE_RANDOM_ENTRY_CONFIRM_LABEL + "10_" + selectionId, "10");

            actionRows.add(ActionRow.of(b1, b2, b5, b10));

            event.getHook().editOriginalEmbeds(embed.build()).setActionRows(actionRows).queue();
        } else if (componentId.startsWith(MULTIPLE_RANDOM_ENTRY_SIZE_LABEL)) {
            UUID selectionId = UUID.fromString(componentId.substring(MULTIPLE_RANDOM_ENTRY_SIZE_LABEL.length(), componentId.lastIndexOf("_")));
            int size = Integer.parseInt(((SelectionMenuEvent) event).getValues().get(0).substring(MULTIPLE_RANDOM_ENTRY_SIZE_OPTION_LABEL.length()));
            randomSizeSelection.get(selectionId)[0] = size;

            String menuId = MULTIPLE_RANDOM_ENTRY_SIZE_LABEL + selectionId + "_";
            List<SelectionMenu.Builder> sizeMenu = createSizeSelectionMenu(menuId, MULTIPLE_RANDOM_ENTRY_SIZE_OPTION_LABEL, 7, instance.numberOfChoices, instance.discordSRVSlashCommandsComponentsMultipleRandomSize);
            outer: for (SelectionMenu.Builder menu : sizeMenu) {
                for (SelectOption option : menu.getOptions()) {
                    if (option.getValue().equals(MULTIPLE_RANDOM_ENTRY_SIZE_OPTION_LABEL + size)) {
                        menu.setDefaultValues(Collections.singleton(option.getValue()));
                        break outer;
                    }
                }
            }
            List<ActionRow> actionRows = sizeMenu.stream().map(m -> ActionRow.of(m.setMinValues(1).setMaxValues(1).build())).collect(Collectors.toList());

            Button b1 = Button.danger(MULTIPLE_RANDOM_ENTRY_CONFIRM_LABEL + "1_" + selectionId, "1");
            Button b2 = Button.danger(MULTIPLE_RANDOM_ENTRY_CONFIRM_LABEL + "2_" + selectionId, "2");
            Button b5 = Button.danger(MULTIPLE_RANDOM_ENTRY_CONFIRM_LABEL + "5_" + selectionId, "5");
            Button b10 = Button.danger(MULTIPLE_RANDOM_ENTRY_CONFIRM_LABEL + "10_" + selectionId, "10");

            actionRows.add(ActionRow.of(b1, b2, b5, b10));

            event.getHook().editOriginalComponents().setActionRows(actionRows).queue();
        } else if (componentId.startsWith(MULTIPLE_RANDOM_ENTRY_CONFIRM_LABEL)) {
            int count = Integer.parseInt(componentId.substring(MULTIPLE_RANDOM_ENTRY_CONFIRM_LABEL.length(), componentId.lastIndexOf("_")));
            UUID selectionId = UUID.fromString(componentId.substring(componentId.lastIndexOf("_") + 1));
            int size = randomSizeSelection.remove(selectionId)[0];
            handleConfirm(event, player, BetNumbersType.MULTIPLE_RANDOM, BetNumbersBuilder.multipleRandom(1, instance.numberOfChoices, size, count).map(b -> b.build()).collect(Collectors.toList()));
        } else if (componentId.equals(BANKER_RANDOM_ENTRY_LABEL)) {
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle(instance.discordSRVSlashCommandsPlaceBetTitle)
                    .setColor(Color.RED)
                    .setDescription("**" + ChatColor.stripColor(instance.betNumbersTypeNames.get(BetNumbersType.BANKER_RANDOM)) + "**");

            UUID selectionId = UUID.randomUUID();
            String bankerMenuId = BANKER_RANDOM_ENTRY_BANKER_LABEL + selectionId + "_";
            List<SelectionMenu.Builder> bankerMenu = createSizeSelectionMenu(bankerMenuId, BANKER_RANDOM_ENTRY_BANKER_OPTION_LABEL, 1, 5, instance.discordSRVSlashCommandsComponentsBankerRandomBankerSize);
            bankerMenu.get(0).setDefaultValues(Collections.singleton(bankerMenu.get(0).getOptions().get(0).getValue()));

            String sizeMenuId = BANKER_RANDOM_ENTRY_SIZE_LABEL + selectionId + "_";
            List<SelectionMenu.Builder> sizeMenu = createSizeSelectionMenu(sizeMenuId, BANKER_RANDOM_ENTRY_SIZE_OPTION_LABEL, 6, instance.numberOfChoices, instance.discordSRVSlashCommandsComponentsBankerRandomSelectionSize);
            sizeMenu.get(0).setDefaultValues(Collections.singleton(sizeMenu.get(0).getOptions().get(0).getValue()));

            List<ActionRow> actionRows = Stream.concat(bankerMenu.stream(), sizeMenu.stream()).map(m -> ActionRow.of(m.setMinValues(1).setMaxValues(1).build())).collect(Collectors.toList());
            randomSizeSelection.put(selectionId, new int[]{1, 6});

            Button b1 = Button.danger(BANKER_RANDOM_ENTRY_CONFIRM_LABEL + "1_" + selectionId, "1");
            Button b2 = Button.danger(BANKER_RANDOM_ENTRY_CONFIRM_LABEL + "2_" + selectionId, "2");
            Button b5 = Button.danger(BANKER_RANDOM_ENTRY_CONFIRM_LABEL + "5_" + selectionId, "5");
            Button b10 = Button.danger(BANKER_RANDOM_ENTRY_CONFIRM_LABEL + "10_" + selectionId, "10");

            actionRows.add(ActionRow.of(b1, b2, b5, b10));

            event.getHook().editOriginalEmbeds(embed.build()).setActionRows(actionRows).queue();
        } else if (componentId.startsWith(BANKER_RANDOM_ENTRY_BANKER_LABEL)) {
            UUID selectionId = UUID.fromString(componentId.substring(BANKER_RANDOM_ENTRY_BANKER_LABEL.length(), componentId.lastIndexOf("_")));
            int[] data = randomSizeSelection.get(selectionId);
            int banker = Integer.parseInt(((SelectionMenuEvent) event).getValues().get(0).substring(BANKER_RANDOM_ENTRY_BANKER_OPTION_LABEL.length()));
            int size = data[1];
            if (banker + size < 7) {
                size = 7 - banker;
            } else if (banker + size > instance.numberOfChoices) {
                size = instance.numberOfChoices - banker;
            }
            data[0] = banker;
            data[1] = size;

            String bankerMenuId = BANKER_RANDOM_ENTRY_BANKER_LABEL + selectionId + "_";
            List<SelectionMenu.Builder> bankerMenu = createSizeSelectionMenu(bankerMenuId, BANKER_RANDOM_ENTRY_BANKER_OPTION_LABEL, 1, 5, instance.discordSRVSlashCommandsComponentsBankerRandomBankerSize);
            outer: for (SelectionMenu.Builder menu : bankerMenu) {
                for (SelectOption option : menu.getOptions()) {
                    if (option.getValue().equals(BANKER_RANDOM_ENTRY_BANKER_OPTION_LABEL + banker)) {
                        menu.setDefaultValues(Collections.singleton(option.getValue()));
                        break outer;
                    }
                }
            }

            String sizeMenuId = BANKER_RANDOM_ENTRY_SIZE_LABEL + selectionId + "_";
            List<SelectionMenu.Builder> sizeMenu = createSizeSelectionMenu(sizeMenuId, BANKER_RANDOM_ENTRY_SIZE_OPTION_LABEL, 7 - banker, instance.numberOfChoices - banker, instance.discordSRVSlashCommandsComponentsBankerRandomSelectionSize);
            outer: for (SelectionMenu.Builder menu : sizeMenu) {
                for (SelectOption option : menu.getOptions()) {
                    if (option.getValue().equals(BANKER_RANDOM_ENTRY_SIZE_OPTION_LABEL + size)) {
                        menu.setDefaultValues(Collections.singleton(option.getValue()));
                        break outer;
                    }
                }
            }

            List<ActionRow> actionRows = Stream.concat(bankerMenu.stream(), sizeMenu.stream()).map(m -> ActionRow.of(m.setMinValues(1).setMaxValues(1).build())).collect(Collectors.toList());

            Button b1 = Button.danger(BANKER_RANDOM_ENTRY_CONFIRM_LABEL + "1_" + selectionId, "1");
            Button b2 = Button.danger(BANKER_RANDOM_ENTRY_CONFIRM_LABEL + "2_" + selectionId, "2");
            Button b5 = Button.danger(BANKER_RANDOM_ENTRY_CONFIRM_LABEL + "5_" + selectionId, "5");
            Button b10 = Button.danger(BANKER_RANDOM_ENTRY_CONFIRM_LABEL + "10_" + selectionId, "10");

            actionRows.add(ActionRow.of(b1, b2, b5, b10));

            event.getHook().editOriginalComponents().setActionRows(actionRows).queue();
        } else if (componentId.startsWith(BANKER_RANDOM_ENTRY_SIZE_LABEL)) {
            UUID selectionId = UUID.fromString(componentId.substring(BANKER_RANDOM_ENTRY_SIZE_LABEL.length(), componentId.lastIndexOf("_")));
            int[] data = randomSizeSelection.get(selectionId);
            int size = Integer.parseInt(((SelectionMenuEvent) event).getValues().get(0).substring(BANKER_RANDOM_ENTRY_SIZE_OPTION_LABEL.length()));
            int banker = data[0];
            data[1] = size;

            String bankerMenuId = BANKER_RANDOM_ENTRY_BANKER_LABEL + selectionId + "_";
            List<SelectionMenu.Builder> bankerMenu = createSizeSelectionMenu(bankerMenuId, BANKER_RANDOM_ENTRY_BANKER_OPTION_LABEL, 1, 5, instance.discordSRVSlashCommandsComponentsBankerRandomBankerSize);
            outer: for (SelectionMenu.Builder menu : bankerMenu) {
                for (SelectOption option : menu.getOptions()) {
                    if (option.getValue().equals(BANKER_RANDOM_ENTRY_BANKER_OPTION_LABEL + banker)) {
                        menu.setDefaultValues(Collections.singleton(option.getValue()));
                        break outer;
                    }
                }
            }

            String sizeMenuId = BANKER_RANDOM_ENTRY_SIZE_LABEL + selectionId + "_";
            List<SelectionMenu.Builder> sizeMenu = createSizeSelectionMenu(sizeMenuId, BANKER_RANDOM_ENTRY_SIZE_OPTION_LABEL, 7 - banker, instance.numberOfChoices - banker, instance.discordSRVSlashCommandsComponentsBankerRandomSelectionSize);
            outer: for (SelectionMenu.Builder menu : sizeMenu) {
                for (SelectOption option : menu.getOptions()) {
                    if (option.getValue().equals(BANKER_RANDOM_ENTRY_SIZE_OPTION_LABEL + size)) {
                        menu.setDefaultValues(Collections.singleton(option.getValue()));
                        break outer;
                    }
                }
            }

            List<ActionRow> actionRows = Stream.concat(bankerMenu.stream(), sizeMenu.stream()).map(m -> ActionRow.of(m.setMinValues(1).setMaxValues(1).build())).collect(Collectors.toList());

            Button b1 = Button.danger(BANKER_RANDOM_ENTRY_CONFIRM_LABEL + "1_" + selectionId, "1");
            Button b2 = Button.danger(BANKER_RANDOM_ENTRY_CONFIRM_LABEL + "2_" + selectionId, "2");
            Button b5 = Button.danger(BANKER_RANDOM_ENTRY_CONFIRM_LABEL + "5_" + selectionId, "5");
            Button b10 = Button.danger(BANKER_RANDOM_ENTRY_CONFIRM_LABEL + "10_" + selectionId, "10");

            actionRows.add(ActionRow.of(b1, b2, b5, b10));

            event.getHook().editOriginalComponents().setActionRows(actionRows).queue();
        } else if (componentId.startsWith(BANKER_RANDOM_ENTRY_CONFIRM_LABEL)) {
            int count = Integer.parseInt(componentId.substring(BANKER_RANDOM_ENTRY_CONFIRM_LABEL.length(), componentId.lastIndexOf("_")));
            UUID selectionId = UUID.fromString(componentId.substring(componentId.lastIndexOf("_") + 1));
            int[] data = randomSizeSelection.remove(selectionId);
            int banker = data[0];
            int size = data[1];
            handleConfirm(event, player, BetNumbersType.BANKER_RANDOM, BetNumbersBuilder.bankerRandom(1, instance.numberOfChoices, banker, size, count).map(b -> b.build()).collect(Collectors.toList()));
        } else {
            Matcher matcher = PLACE_BET_CONFIRM_LABEL_PATTERN.matcher(componentId);
            if (matcher.find()) {
                event.getHook().editOriginalComponents(ActionRow.of(event.getMessage().getButtons().stream().map(b -> b.asDisabled()).collect(Collectors.toList()))).queue();
                if (instance.backendBungeecordMode && Bukkit.getOnlinePlayers().isEmpty()) {
                    event.getHook().editOriginal(instance.discordSRVSlashCommandsGlobalMessagesNoOneOnline).setActionRows().setEmbeds().queue();
                    return;
                }
                UUID betNumbersId = UUID.fromString(matcher.group(1));
                List<BetNumbers> betNumbers = confirmNumbers.remove(betNumbersId);
                if (betNumbers == null) {
                    throw new RuntimeException();
                }
                BetUnitType unitType = BetUnitType.valueOf(matcher.group(2).toUpperCase());
                if (betNumbers.isEmpty()) {
                    throw new RuntimeException();
                }
                long price = betNumbers.stream().mapToLong(each -> LotteryUtils.calculatePrice(each, instance)).sum() / unitType.getDivisor();
                if (instance.backendBungeecordMode) {
                    LotterySixPlugin.getPluginMessageHandler().requestAddBet(player.getName(), player.getUniqueId(), price / betNumbers.size(), unitType, betNumbers);
                    LotterySixPlugin.discordSRVHook.addBungeecordPendingBets(uuid, event.getHook());
                } else {
                    String message = "";
                    AddBetResult result = game.addBet(player.getName(), player.getUniqueId(), price / betNumbers.size(), unitType, betNumbers);
                    switch (result) {
                        case SUCCESS: {
                            message = instance.messageBetPlaced.replace("{Price}", StringUtils.formatComma(price));
                            break;
                        }
                        case GAME_LOCKED: {
                            message = instance.messageGameLocked.replace("{Price}", StringUtils.formatComma(price));
                            break;
                        }
                        case NOT_ENOUGH_MONEY: {
                            message = instance.messageNotEnoughMoney.replace("{Price}", StringUtils.formatComma(price));
                            break;
                        }
                        case LIMIT_SELF: {
                            message = instance.messageBetLimitReachedSelf.replace("{Price}", StringUtils.formatComma(price));
                            break;
                        }
                        case LIMIT_PERMISSION: {
                            message = instance.messageBetLimitReachedPermission.replace("{Price}", StringUtils.formatComma(price));
                            break;
                        }
                        case LIMIT_CHANCE_PER_SELECTION: {
                            message = instance.messageBetLimitMaximumChancePerSelection.replace("{Price}", StringUtils.formatComma(price));
                            break;
                        }
                        case ACCOUNT_SUSPENDED: {
                            long time = instance.getLotteryPlayerManager().getLotteryPlayer(player.getUniqueId()).getPreference(PlayerPreferenceKey.SUSPEND_ACCOUNT_UNTIL, long.class);
                            message = instance.messageBettingAccountSuspended.replace("{Date}", instance.dateFormat.format(new Date(time))).replace("{Price}", StringUtils.formatComma(price));
                            break;
                        }
                    }
                    event.getHook().editOriginal(ChatColor.stripColor(message)).setActionRows().setEmbeds().queue();
                }
            }
        }
    }
    
    private void handleConfirm(GenericComponentInteractionCreateEvent event, OfflinePlayer player, BetNumbersType type, List<BetNumbers> betNumbers) {
        PlayableLotterySixGame game = instance.getCurrentGame();
        long price = betNumbers.stream().mapToLong(each -> LotteryUtils.calculatePrice(each, instance)).sum();
        long partial = price / BetUnitType.PARTIAL.getDivisor();
        
        StringBuilder sb = new StringBuilder();
        for (String line : instance.discordSRVSlashCommandsPlaceBetSubTitle) {
            sb.append(ChatColor.stripColor(LotteryUtils.formatPlaceholders(player, line.replace("{BetNumbersType}", instance.betNumbersTypeNames.get(type)), instance, game))).append("\n");
        }
        if (sb.length() > 0) {
            sb.append("\n");
        }
        if (type.isRandom()) {
            int entriesTotal = betNumbers.stream().mapToInt(each -> each.getSetsSize()).sum();
            sb.append("**").append(ChatColor.stripColor(Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetBulkRandom, instance, game))
                    .map(each -> each.replace("{EntriesTotal}", entriesTotal + "")).collect(Collectors.joining("**\n**")))).append("**");
        } else {
            sb.append("**").append(betNumbers.iterator().next().toString()).append("**");
        }
        String description = sb.charAt(sb.length() - 1) == '\n' ? sb.substring(0, sb.length() - 1) : sb.toString();

        EmbedBuilder builder = new EmbedBuilder()
                .setColor(Color.GREEN)
                .setTitle(ChatColor.stripColor(LotteryUtils.formatPlaceholders(player, instance.discordSRVSlashCommandsPlaceBetTitle, instance, game)))
                .setDescription(description)
                .setThumbnail(instance.discordSRVSlashCommandsPlaceBetThumbnailURL);

        UUID betNumbersId = UUID.randomUUID();
        String id = PLACE_BET_CONFIRM_LABEL.replace("{Number}", betNumbersId.toString());
        confirmNumbers.put(betNumbersId, betNumbers);
        WebhookMessageUpdateAction<Message> action = event.getHook().editOriginalEmbeds(builder.build());
        List<ActionRow> actionRows = new ArrayList<>();
        if (type.isMultipleCombination()) {
            actionRows.add(ActionRow.of(
                    Button.secondary(id.replace("{Unit}", BetUnitType.PARTIAL.name()),
                            Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetPartialInvestmentConfirm, instance, game))
                                    .map(each -> ChatColor.stripColor(each.replace("{Price}", StringUtils.formatComma(price)).replace("{PricePartial}", StringUtils.formatComma(partial)))).collect(Collectors.joining("\n"))),
                    Button.primary(id.replace("{Unit}", BetUnitType.FULL.name()),
                            Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetUnitInvestmentConfirm, instance, game))
                                    .map(each -> ChatColor.stripColor(each.replace("{Price}", StringUtils.formatComma(price)).replace("{PricePartial}", StringUtils.formatComma(partial)))).collect(Collectors.joining("\n")))));
        } else {
            actionRows.add(ActionRow.of(
                    Button.primary(id.replace("{Unit}", BetUnitType.FULL.name()),
                            Arrays.stream(LotteryUtils.formatPlaceholders(player, instance.guiConfirmNewBetUnitInvestmentConfirm, instance, game))
                                    .map(each -> ChatColor.stripColor(each.replace("{Price}", StringUtils.formatComma(price)).replace("{PricePartial}", StringUtils.formatComma(partial)))).collect(Collectors.joining("\n")))));
        }
        action.setActionRows(actionRows).queue();
    }

}
