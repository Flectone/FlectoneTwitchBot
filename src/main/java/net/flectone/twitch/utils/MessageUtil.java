package net.flectone.twitch.utils;

import com.github.twitch4j.common.events.domain.EventUser;

public class MessageUtil {

    public static String replaceUser(String message, EventUser eventUser) {
        return message
                .replace("<chatter>", eventUser.getName())
                .replace("<user>", eventUser.getName());
    }

}
