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

package com.loohp.lotterysix.discordsrv;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.loohp.lotterysix.LotterySixPlugin;
import com.loohp.lotterysix.discordsrv.menus.BettingAccountInteraction;
import com.loohp.lotterysix.discordsrv.menus.NumberStatisticsInteraction;
import com.loohp.lotterysix.discordsrv.menus.PastDrawInteraction;
import com.loohp.lotterysix.discordsrv.menus.PlaceBetInteraction;
import com.loohp.lotterysix.discordsrv.menus.ViewBetsInteraction;
import com.loohp.lotterysix.events.LotterySixEvent;
import com.loohp.lotterysix.events.PlayerBetEvent;
import com.loohp.lotterysix.game.LotterySix;
import com.loohp.lotterysix.game.lottery.CompletedLotterySixGame;
import com.loohp.lotterysix.game.lottery.PlayableLotterySixGame;
import com.loohp.lotterysix.game.objects.LotterySixAction;
import com.loohp.lotterysix.game.objects.PlayerPreferenceKey;
import com.loohp.lotterysix.game.player.LotteryPlayer;
import com.loohp.lotterysix.utils.LotteryUtils;
import com.loohp.lotterysix.utils.StringUtils;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.commands.PluginSlashCommand;
import github.scarsz.discordsrv.api.commands.SlashCommand;
import github.scarsz.discordsrv.api.commands.SlashCommandProvider;
import github.scarsz.discordsrv.api.events.DiscordReadyEvent;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Emoji;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.GenericComponentInteractionCreateEvent;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.GenericInteractionCreateEvent;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent;
import github.scarsz.discordsrv.dependencies.jda.api.hooks.ListenerAdapter;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Component;
import github.scarsz.discordsrv.dependencies.jda.api.requests.RestAction;
import github.scarsz.discordsrv.dependencies.jda.api.requests.restaction.MessageAction;
import github.scarsz.discordsrv.dependencies.jda.api.requests.restaction.WebhookMessageUpdateAction;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class DiscordSRVHook extends ListenerAdapter implements Listener, SlashCommandProvider {

    public static final String SLASH_COMMAND_LABEL = "lottery";
    public static final String INTERACTION_LABEL_PREFIX = "ls_";
    public static final String MAIN_MENU_LABEL = INTERACTION_LABEL_PREFIX + "main_menu";
    public static final String NEW_MAIN_MENU_LABEL = INTERACTION_LABEL_PREFIX + "new_main_menu";

    public static List<ActionRow> buildActionRows(Collection<DiscordInteraction> interactions, String discordUserId) {
        UUID uuid = discordUserId == null ? null : DiscordSRV.getPlugin().getAccountLinkManager().getUuid(discordUserId);
        List<ActionRow> actionRows = new ArrayList<>(5);
        List<Component> currentRow = new ArrayList<>(5);
        for (DiscordInteraction interaction : interactions) {
            if (uuid != null || !interaction.requireAccountLinked()) {
                if (interaction.doOccupyEntireRow(uuid)) {
                    if (!currentRow.isEmpty()) {
                        actionRows.add(ActionRow.of(currentRow));
                        currentRow = new ArrayList<>(5);
                    }
                    actionRows.addAll(interaction.getActionRows(uuid));
                } else {
                    for (ActionRow row : interaction.getActionRows(uuid)) {
                        for (Component component : row) {
                            if (currentRow.size() >= component.getType().getMaxPerRow()) {
                                actionRows.add(ActionRow.of(currentRow));
                                currentRow = new ArrayList<>(5);
                            }
                            currentRow.add(component);
                        }
                    }
                }
            }
        }
        if (!currentRow.isEmpty()) {
            actionRows.add(ActionRow.of(currentRow));
        }
        return actionRows.subList(0, Math.min(5, actionRows.size()));
    }

    private final Map<UUID, InteractionHook> bungeecordPendingAddBet;
    private final Map<String, DiscordInteraction> interactionMap;
    private final Map<String, InteractionHookData> activeInteractionHooks;
    private byte[] advertisementImage;
    private boolean init;

    public DiscordSRVHook() {
        this.bungeecordPendingAddBet = new ConcurrentHashMap<>();
        this.interactionMap = new LinkedHashMap<>();
        Cache<String, InteractionHookData> activeInteractionHooks = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.HOURS).build();
        this.activeInteractionHooks = activeInteractionHooks.asMap();
        this.advertisementImage = null;

        registerInteraction(new BettingAccountInteraction());
        registerInteraction(new PastDrawInteraction());
        registerInteraction(new ViewBetsInteraction());
        registerInteraction(new PlaceBetInteraction());
        registerInteraction(new NumberStatisticsInteraction());

        DiscordSRV.api.subscribe(this);
        if (DiscordSRV.isReady) {
            this.init = true;
            DiscordSRV.api.addSlashCommandProvider(this);
            DiscordSRV.getPlugin().getJda().addEventListener(new JDAEvents());
            reload();
        } else {
            this.init = false;
        }
    }

    public void addBungeecordPendingBets(UUID uuid, InteractionHook hook) {
        bungeecordPendingAddBet.put(uuid, hook);
    }

    private void registerInteraction(DiscordInteraction interaction) {
        interactionMap.put(interaction.getInteractionLabelStartWith(), interaction);
    }

    public void reload() {
        DiscordSRV.api.updateSlashCommands();
    }

    public byte[] getAdvertisementImage() {
        return advertisementImage;
    }

    public void setAdvertisementImage(byte[] advertisementImage) {
        this.advertisementImage = advertisementImage;
    }

    public Button getMainMenuButton() {
        return Button.secondary(MAIN_MENU_LABEL, LotterySixPlugin.getInstance().discordSRVSlashCommandsGlobalComponentsBack).withEmoji(Emoji.fromUnicode("\u23EE\uFE0F"));
    }

    public MessageEmbed getGenericEmbed(String message, Color color) {
        return new EmbedBuilder().setAuthor(message).setColor(color).build();
    }

    @Subscribe
    public void onDiscordReady(DiscordReadyEvent event) {
        if (!init) {
            DiscordSRV.api.addSlashCommandProvider(this);
            DiscordSRV.getPlugin().getJda().addEventListener(new JDAEvents());
            reload();
            this.init = true;
        }
    }

    public void expireAllHooks(boolean immediately) {
        List<RestAction<?>> actions = new ArrayList<>();
        for (InteractionHookData data : activeInteractionHooks.values()) {
            InteractionHook interactionHook = data.getInteractionHook();
            if (!interactionHook.isExpired()) {
                actions.add(interactionHook.editOriginalEmbeds(getGenericEmbed(LotterySixPlugin.getInstance().discordSRVSlashCommandsGlobalMessagesTimeOut, Color.RED)).setActionRows().retainFiles(Collections.emptyList()).setCheck(() -> !interactionHook.isExpired()));
            }
            activeInteractionHooks.remove(data.getMessageId());
        }
        if (!actions.isEmpty()) {
            if (immediately) {
                RestAction.allOf(actions).complete();
            } else {
                RestAction.allOf(actions).queue();
            }
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
                if (advertisementImage != null) {
                    builder.setImage("attachment://image.png");
                }
                MessageAction action = channel.sendMessageEmbeds(builder.build());
                if (advertisementImage != null) {
                    action = action.addFile(advertisementImage, "image.png");
                }
                if (lotterySix.discordSRVSlashCommandsEnableLotteryCommand) {
                    Button button = Button.success(NEW_MAIN_MENU_LABEL, lotterySix.discordSRVSlashCommandsGlobalTitle).withEmoji(Emoji.fromUnicode("\u2139\uFE0F"));
                    action = action.setActionRows(ActionRow.of(button));
                }
                action.queue();
            }
        }
        expireAllHooks(false);
    }

    @EventHandler
    public void onPlaceBet(PlayerBetEvent event) {
        LotterySix lotterySix = LotterySixPlugin.getInstance();
        if (lotterySix.backendBungeecordMode) {
            LotteryPlayer lotteryPlayer = event.getPlayer();
            UUID uuid = lotteryPlayer.getPlayer();
            InteractionHook hook = bungeecordPendingAddBet.remove(uuid);
            if (hook != null) {
                PlayableLotterySixGame game = lotterySix.getCurrentGame();
                if (game == null) {
                    hook.editOriginalEmbeds(getGenericEmbed(lotterySix.discordSRVSlashCommandsPlaceBetNoGame, Color.RED)).setActionRows().retainFiles(Collections.emptyList()).queue();
                    return;
                }
                String message = "";
                Color color = Color.DARK_GRAY;
                long price = event.getPrice();
                switch (event.getResult()) {
                    case SUCCESS: {
                        message = lotterySix.messageBetPlaced.replace("{Price}", StringUtils.formatComma(price));
                        color = Color.GREEN;
                        break;
                    }
                    case GAME_LOCKED: {
                        message = lotterySix.messageGameLocked.replace("{Price}", StringUtils.formatComma(price));
                        color = Color.RED;
                        break;
                    }
                    case NOT_ENOUGH_MONEY: {
                        message = lotterySix.messageNotEnoughMoney.replace("{Price}", StringUtils.formatComma(price));
                        color = Color.RED;
                        break;
                    }
                    case LIMIT_SELF: {
                        message = lotterySix.messageBetLimitReachedSelf.replace("{Price}", StringUtils.formatComma(price));
                        color = Color.RED;
                        break;
                    }
                    case LIMIT_PERMISSION: {
                        message = lotterySix.messageBetLimitReachedPermission.replace("{Price}", StringUtils.formatComma(price));
                        color = Color.RED;
                        break;
                    }
                    case LIMIT_CHANCE_PER_SELECTION: {
                        message = lotterySix.messageBetLimitMaximumChancePerSelection.replace("{Price}", StringUtils.formatComma(price));
                        color = Color.RED;
                        break;
                    }
                    case ACCOUNT_SUSPENDED: {
                        long time = lotteryPlayer.getPreference(PlayerPreferenceKey.SUSPEND_ACCOUNT_UNTIL, long.class);
                        message = lotterySix.messageBettingAccountSuspended.replace("{Date}", lotterySix.dateFormat.format(new Date(time))).replace("{Price}", StringUtils.formatComma(price));
                        color = Color.RED;
                        break;
                    }
                }

                hook.editOriginalEmbeds(getGenericEmbed(ChatColor.stripColor(message), color)).setActionRows(ActionRow.of(getMainMenuButton())).retainFiles(Collections.emptyList()).queue();
            }
        }
    }

    @Override
    public Set<PluginSlashCommand> getSlashCommands() {
        if (!LotterySixPlugin.getInstance().discordSRVSlashCommandsEnableLotteryCommand) {
            return Collections.emptySet();
        }
        return Collections.singleton(new PluginSlashCommand(LotterySixPlugin.plugin, new CommandData(SLASH_COMMAND_LABEL, LotterySixPlugin.getInstance().discordSRVSlashCommandsGlobalDescription), DiscordSRV.getPlugin().getMainGuild().getId()));
    }

    @SlashCommand(path = "*")
    public void onSlashCommand(SlashCommandEvent event) {
        if (!LotterySixPlugin.getInstance().discordSRVSlashCommandsEnableLotteryCommand) {
            return;
        }
        Guild guild = DiscordSRV.getPlugin().getMainGuild();
        if (event.getGuild().getIdLong() != guild.getIdLong()) {
            return;
        }
        if (!(event.getChannel() instanceof TextChannel)) {
            return;
        }
        String label = event.getName();
        if (label.equals(SLASH_COMMAND_LABEL)) {
            event.deferReply(true).queue();
            if (LotterySixPlugin.getInstance().isGameLocked()) {
                event.getHook().editOriginalEmbeds(getGenericEmbed(ChatColor.stripColor(LotterySixPlugin.getInstance().messageGameLocked), Color.RED)).setActionRows().retainFiles(Collections.emptyList()).queue();
            } else {
                handle(event, true);
            }
        }
    }

    private void handle(GenericInteractionCreateEvent event, boolean entryPoint) {
        LotterySix lotterySix = LotterySixPlugin.getInstance();
        String discordUserId = event.getUser().getId();
        String description;
        PlayableLotterySixGame game = lotterySix.getCurrentGame();
        if (game == null) {
            description = ChatColor.stripColor(LotteryUtils.formatPlaceholders(null, lotterySix.discordSRVSlashCommandsGlobalSubTitleNoGame, lotterySix, lotterySix.getCurrentGame()));
        } else {
            description = ChatColor.stripColor(LotteryUtils.formatPlaceholders(null, lotterySix.discordSRVSlashCommandsGlobalSubTitleActiveGame, lotterySix, lotterySix.getCurrentGame()));
        }
        EmbedBuilder builder = new EmbedBuilder()
                .setTitle(lotterySix.discordSRVSlashCommandsGlobalTitle)
                .setDescription(description)
                .setColor(Color.YELLOW)
                .setThumbnail(lotterySix.discordSRVSlashCommandsGlobalThumbnailURL);
        if (advertisementImage != null && game != null) {
            builder.setImage("attachment://image.png");
        }
        WebhookMessageUpdateAction<Message> action = event.getHook().editOriginalEmbeds(builder.build()).setActionRows(buildActionRows(interactionMap.values(), discordUserId)).retainFiles(Collections.emptyList());
        if (advertisementImage != null && game != null) {
            action = action.addFile(advertisementImage, "image.png");
        }
        if (entryPoint) {
            action.queue(message -> activeInteractionHooks.put(message.getId(), new InteractionHookData(message.getId(), game, event.getHook())));
        } else {
            action.queue();
        }
    }

    public class JDAEvents extends ListenerAdapter {

        @Override
        public void onGenericComponentInteractionCreate(GenericComponentInteractionCreateEvent event) {
            if (!LotterySixPlugin.getInstance().discordSRVSlashCommandsEnableLotteryCommand) {
                return;
            }
            Component component = event.getComponent();
            if (component == null) {
                return;
            }
            String id = component.getId();
            if (id == null || !id.startsWith(INTERACTION_LABEL_PREFIX)) {
                return;
            }
            if (id.equals(NEW_MAIN_MENU_LABEL)) {
                event.deferReply(true).queue();
                if (LotterySixPlugin.getInstance().isGameLocked()) {
                    event.getHook().editOriginalEmbeds(getGenericEmbed(ChatColor.stripColor(LotterySixPlugin.getInstance().messageGameLocked), Color.RED)).setActionRows().retainFiles(Collections.emptyList()).queue();
                } else {
                    handle(event, true);
                }
                return;
            }
            event.deferEdit().queue();
            if (LotterySixPlugin.getInstance().isGameLocked()) {
                event.getHook().editOriginalEmbeds(getGenericEmbed(ChatColor.stripColor(LotterySixPlugin.getInstance().messageGameLocked), Color.RED)).setActionRows().retainFiles(Collections.emptyList()).queue();
            } else {
                PlayableLotterySixGame game = LotterySixPlugin.getInstance().getCurrentGame();
                InteractionHookData data = activeInteractionHooks.get(event.getMessageId());
                if (data == null || !data.compareCurrentGame(game)) {
                    event.getHook().editOriginalEmbeds(getGenericEmbed(LotterySixPlugin.getInstance().discordSRVSlashCommandsGlobalMessagesTimeOut, Color.RED)).setActionRows().retainFiles(Collections.emptyList()).queue();
                    return;
                }
                data.setInteractionHook(event.getHook());
                try {
                    if (id.startsWith(MAIN_MENU_LABEL)) {
                        handle(event, false);
                        return;
                    }
                    for (DiscordInteraction interaction : interactionMap.values()) {
                        if (id.startsWith(interaction.getInteractionLabelStartWith())) {
                            interaction.handle(event);
                            return;
                        }
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                event.getHook().editOriginalEmbeds(getGenericEmbed(LotterySixPlugin.getInstance().discordSRVSlashCommandsGlobalMessagesUnknownError, Color.RED)).setActionRows().retainFiles(Collections.emptyList()).queue();
            }
        }

    }

    public static class InteractionHookData {

        private final String messageId;
        private final UUID currentGameId;
        private InteractionHook interactionHook;

        public InteractionHookData(String messageId, PlayableLotterySixGame currentGame, InteractionHook interactionHook) {
            this.messageId = messageId;
            this.currentGameId = currentGame == null ? null : currentGame.getGameId();
            this.interactionHook = interactionHook;
        }

        public String getMessageId() {
            return messageId;
        }

        public UUID getCurrentGameId() {
            return currentGameId;
        }

        public boolean compareCurrentGame(PlayableLotterySixGame game) {
            return Objects.equals(game == null ? null : game.getGameId(), currentGameId);
        }

        public InteractionHook getInteractionHook() {
            return interactionHook;
        }

        public void setInteractionHook(InteractionHook interactionHook) {
            this.interactionHook = interactionHook;
        }
    }

}
