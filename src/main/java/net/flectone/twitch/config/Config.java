package net.flectone.twitch.config;

import lombok.Getter;
import net.elytrium.serializer.SerializerConfig;
import net.elytrium.serializer.custom.ClassSerializer;
import net.elytrium.serializer.language.object.YamlSerializable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
public class Config extends YamlSerializable {

    private static final SerializerConfig CONFIG = new SerializerConfig.Builder()
            .registerSerializer(new ClassSerializer<Twitch.Channel, Map<String, Object>>() {
                @Override
                public Map<String, Object> serialize(Twitch.Channel from) {
                    return Map.of("access-token", from.getAccessToken(), "name", from.getName(), "moderators", from.getModerators());
                }

                @Override
                public Twitch.Channel deserialize(Map<String, Object> from) {
                    return new Twitch.Channel((String) from.get("access-token"), (String) from.get("name"), (List<String>) from.get("moderators"));
                }

            })
            .registerSerializer(new ClassSerializer<Twitch.Command, Map<String, Object>>() {
                @Override
                public Map<String, Object> serialize(Twitch.Command from) {
                    return Map.of("enable", from.isEnable(), "trigger", from.getTrigger(), "answer", from.getAnswer(), "action", from.getAction(), "aliases", from.getAliases());
                }

                @Override
                public Twitch.Command deserialize(Map<String, Object> from) {
                    return new Twitch.Command((boolean) from.get("enable"), (String) from.get("trigger"), (String) from.get("answer"), Twitch.Command.Action.valueOf((String) from.get("action")), (List<String>) from.get("aliases"));
                }

            })
            .build();

    public Config() {
        super(Config.CONFIG);
    }

    private Twitch twitch = new Twitch();

    @Getter
    public static class Twitch {
        private String identityProvider;
        private String accessToken;

        public List<Channel> channels = List.of(new Channel("token", "twitch", new ArrayList<>()));

        @Getter
        public static class Channel {
            private String accessToken;
            private String name;
            private List<String> moderators;

            public Channel() {
                this.accessToken = "";
                this.name = "";
                this.moderators = new ArrayList<>();
            }

            public Channel(String accessToken, String name, List<String> moderators) {
                this.accessToken = accessToken;
                this.name = name;
                this.moderators = moderators;
            }
        }

        public List<Command> commands = List.of(
                new Command("!ping", "@<chatter> pong!", Command.Action.NONE),
                new Command("!game", "@<chatter> changed game to «<message>»", Command.Action.GAME),
                new Command("!title", "@<chatter> changed title to «<message>»", Command.Action.TITLE),
                new Command("!tags", "@<chatter> changed tags to «<message>»", Command.Action.TAGS)
        );

        @Getter
        public static class Command {
            private boolean enable = true;
            private String trigger;
            private String answer;
            private Action action;
            private List<String> aliases;

            public Command() {
                this.trigger = "";
                this.answer = "";
                this.action = Action.NONE;
            }

            public Command(String trigger, String answer, Action action, List<String> aliases) {
                this.trigger = trigger;
                this.answer = answer;
                this.action = action;
                this.aliases = aliases;
            }

            public Command(boolean enable, String trigger, String answer, Action action, List<String> aliases) {
                this(trigger, answer, action, aliases);
                this.enable = enable;
            }

            public Command(String trigger, String answer, Action action) {
                this(trigger, answer, action, new ArrayList<>());
            }

            public enum Action {
                NONE,
                GAME,
                TITLE,
                TAGS
            }
        }
    }

    public Message message = new Message();

    @Getter
    public static class Message {
        private String emptyAuth = "Fill in the “indentity-provider” and “access-token” fields from https://twitchtokengenerator.com/";
        private String joinChannel = "Join <channel> channel";
    }
}
