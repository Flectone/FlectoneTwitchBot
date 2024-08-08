package net.flectone.twitch.manager;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.helix.domain.ChannelInformation;
import com.github.twitch4j.helix.domain.Game;
import kotlin.Pair;
import net.flectone.twitch.config.Config;
import net.flectone.twitch.utils.MessageUtil;
import net.flectone.twitch.utils.TwitchUtil;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CommandManager {

    private final OAuth2Credential oAuth2Credential;
    private final TwitchClient twitchClient;
    private final Logger logger;

    public CommandManager(OAuth2Credential oAuth2Credential, TwitchClient twitchClient, Logger logger) {
        this.oAuth2Credential = oAuth2Credential;
        this.twitchClient = twitchClient;
        this.logger = logger;
    }


    public void noneCommand(Config.Twitch.Command command, ChannelMessageEvent event) {
        String answer = MessageUtil.replaceUser(command.getAnswer(), event.getUser());
        twitchClient.getChat().sendMessage(event.getChannel().getName(), answer);
        logger.info("Command [{}], User [{}], Message [{}]", command.getTrigger(), event.getUser().getName(), answer);
    }

    public void titleCommand(Config.Twitch configTwitch, Config.Twitch.Command command, ChannelMessageEvent event) {
        moderatorCommand(configTwitch, command, event,"Changed title to {}", (chatterMessage, channelInformation) -> {
            String title = chatterMessage.substring(command.getTrigger().length()).trim();
            return new Pair<>(title, channelInformation.withTitle(title));
        });
    }

    public void tagsCommand(Config.Twitch configTwitch, Config.Twitch.Command command, ChannelMessageEvent event) {
        moderatorCommand(configTwitch, command, event,"Changed tags to {}", (chatterMessage, channelInformation) -> {
            List<String> tags = List.of(chatterMessage.substring(command.getTrigger().length()).trim().split(" "));
            return new Pair<>(String.join(", ", tags), channelInformation.withTags(tags));
        });
    }

    public void gameCommand(Config.Twitch configTwitch, Config.Twitch.Command command, ChannelMessageEvent event) {
        moderatorCommand(configTwitch, command, event,"Changed game to {}", (chatterMessage, channelInformation) -> {
            String gameName = chatterMessage.substring(command.getTrigger().length()).trim();
            List<Game> gameList = twitchClient.getHelix().searchCategories(oAuth2Credential.getAccessToken(), gameName, null, null).execute().getResults();
            if (gameList.isEmpty()) return null;

            Game game = Collections.max(gameList, Comparator.comparing(c -> TwitchUtil.getViewerCount(oAuth2Credential, twitchClient.getHelix(), c.getId())));
            return new Pair<>(game.getName(), channelInformation.withGameId(game.getId()));
        });
    }

    private void moderatorCommand(Config.Twitch configTwitch,
                                        Config.Twitch.Command command,
                                        ChannelMessageEvent event,
                                        String loggerMessage,
                                        ChannelMessageChangeInterface channelMessageChangeInterface) {
        String chatterMessage = event.getMessage();
        if (chatterMessage.length() == command.getTrigger().length()) return;

        Config.Twitch.Channel channel = configTwitch.getChannels().stream()
                .filter(account -> account.getName().equals(event.getChannel().getName()))
                .findAny()
                .orElse(null);

        if (channel == null) return;
        if (!TwitchUtil.isModerator(channel, event)) return;

        String channelID = event.getChannel().getId();

        ChannelInformation channelInformation = twitchClient.getHelix()
                .getChannelInformation(oAuth2Credential.getAccessToken(), List.of(channelID))
                .execute()
                .getChannels()
                .get(0)
                .withDelay(null);

        var changedInfo = channelMessageChangeInterface.change(chatterMessage, channelInformation);
        if (changedInfo == null) return;

        channelInformation = changedInfo.getSecond();
        String message = changedInfo.getFirst();

        twitchClient.getHelix()
                .updateChannelInformation(channel.getAccessToken(), channelID, channelInformation)
                .execute();

        twitchClient.getChat().sendMessage(event.getChannel().getName(),
                MessageUtil.replaceUser(command.getAnswer(), event.getUser()).replace("<message>", message));
        logger.info(loggerMessage, message);
    }

    @FunctionalInterface
    public interface ChannelMessageChangeInterface {
        Pair<String, ChannelInformation> change(String chatterMessage, ChannelInformation channelInformation);
    }
}
