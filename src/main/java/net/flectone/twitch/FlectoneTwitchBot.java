package net.flectone.twitch;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import net.flectone.twitch.config.Config;
import net.flectone.twitch.manager.CommandManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class FlectoneTwitchBot {

    private static final Logger logger = LoggerFactory.getLogger("FlectoneTwitchBot");
    private static final Config config = new Config();
    private static OAuth2Credential oAuth2Credential;
    private static TwitchClient twitchClient;
    private static CommandManager commandManager;

    public static void main(String[] args) {
        config.reload(Path.of("config.yml"));

        Config.Message message = config.getMessage();
        Config.Twitch configTwitch = config.getTwitch();

        if (configTwitch.getAccessToken().isEmpty() || configTwitch.getIdentityProvider().isEmpty()) {
            logger.error(message.getEmptyAuth());
            return;
        }

        oAuth2Credential = new OAuth2Credential(configTwitch.getIdentityProvider(), configTwitch.getAccessToken());
        twitchClient = TwitchClientBuilder.builder()
                .withEnableHelix(true)
                .withEnableChat(true)
                .withEnablePubSub(true)
                .withChatAccount(oAuth2Credential)
                .build();

        for (Config.Twitch.Channel channel : configTwitch.getChannels()) {
            logger.info(message.getJoinChannel().replace("<channel>", channel.getName()));
            twitchClient.getChat().joinChannel(channel.getName());
        }

        commandManager = new CommandManager(oAuth2Credential, twitchClient, logger);

        for (var command : configTwitch.getCommands()) {
            twitchClient.getEventManager().onEvent(ChannelMessageEvent.class, event -> {
                String eventMessage = event.getMessage();
                if (!eventMessage.startsWith(command.getTrigger())
                        && command.getAliases().stream().noneMatch(eventMessage::startsWith)) return;

                switch (command.getAction()) {
                    case NONE -> commandManager.noneCommand(command, event);
                    case GAME -> commandManager.gameCommand(configTwitch, command, event);
                    case TITLE -> commandManager.titleCommand(configTwitch, command, event);
                    case TAGS -> commandManager.tagsCommand(configTwitch, command, event);
                }
            });
        }

    }
}