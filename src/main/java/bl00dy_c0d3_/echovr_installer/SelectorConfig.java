package bl00dy_c0d3_.echovr_installer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class SelectorConfig {

    private final Properties properties;

    public SelectorConfig() {
        this.properties = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("discord-selectors.properties")) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException e) {
            // Properties file missing or unreadable — fall back to defaults
        }
    }

    public String getServerInvite() {
        return properties.getProperty("discord.server.invite", "https://discord.gg/KqjqdNUaHR");
    }

    public String getChannelName() {
        return properties.getProperty("discord.channel.name", "quest-patch");
    }

    public String getChannelSelector() {
        return properties.getProperty("discord.channel.selector", "[data-list-item-id*=\"channels\"]");
    }

    public String getReactionEmoji() {
        return properties.getProperty("discord.reaction.emoji", "\uD83C\uDFAE");
    }

    public String getMessageSelector() {
        return properties.getProperty("discord.message.selector", "[class*=\"message\"]");
    }

    public String getThreadSelector() {
        return properties.getProperty("discord.thread.selector", "[class*=\"thread\"]");
    }

    public String getUrlPattern() {
        return properties.getProperty("discord.url.pattern", "https://cdn\\.discordapp\\.com/attachments/.*/pnsovr\\.dll.*");
    }

    public int getTimeoutSeconds() {
        String value = properties.getProperty("discord.timeout.seconds", "30");
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 30;
        }
    }
}
