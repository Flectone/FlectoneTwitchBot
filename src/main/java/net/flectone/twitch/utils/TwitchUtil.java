package net.flectone.twitch.utils;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.chat.events.AbstractChannelMessageEvent;
import com.github.twitch4j.helix.TwitchHelix;
import com.github.twitch4j.helix.domain.Stream;
import com.github.twitch4j.helix.domain.StreamList;
import com.github.twitch4j.util.PaginationUtil;
import net.flectone.twitch.config.Config;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeSet;

public class TwitchUtil {

    public static boolean isModerator(Config.Twitch.Channel channel, AbstractChannelMessageEvent event) {
        if (event.getChannel().getId().equals(event.getUser().getId())) return true;
        return channel != null && channel.getModerators().contains(event.getUser().getName());
    }

    public static int getViewerCount(OAuth2Credential oAuth2Credential, TwitchHelix helix, String gameId) {
        int views = 0;
        Collection<Stream> streams = PaginationUtil.getPaginated(cursor -> {
                    try {
                        return helix.getStreams(oAuth2Credential.getAccessToken(), cursor, null, 100, Collections.singletonList(gameId), null, null, null).execute();
                    } catch (Exception e) {
                        return null;
                    }
                },
                StreamList::getStreams,
                result -> result.getPagination() != null ? result.getPagination().getCursor() : null,
                2,
                10_000,
                () -> new TreeSet<>(Comparator.comparing(Stream::getUserId)),
                true
        );
        for (Stream stream : streams) {
            views += stream.getViewerCount();
        }
        System.out.println(views);
        return views;
    }
}
