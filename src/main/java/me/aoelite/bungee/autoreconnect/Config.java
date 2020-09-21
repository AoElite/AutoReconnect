package me.aoelite.bungee.autoreconnect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class Config {

	private AutoReconnect instance;

	private String reconnectingChat = "";
	private String reconnectingTitle = "§7Reconnecting{%dots%}";
	private String reconnectingActionBar = "§a§lPlease do not leave! §7Reconnecting to server{%dots%}";
	private long reconnectingSendInterval = 1000;
	private String connectingChat = "";
	private String connectingTitle = "§aConnecting...";
	private String connectingActionBar = "§7Connecting you to the server...";
	private String rejectedChat = "§cDiscononected from {%server%} with reason: {%reason%}";
	private String rejectedTitle = "";
	private String rejectedActionBar = "§eYou have been moved to the fallback server!";
	private String failedChat = "§cDiscononected from {%server%} with reason: {%reason%}";
	private String failedTitle = "§cReconnecting failed!";
	private String failedActionBar = "§eYou have been moved to the fallback server!";
	private String limboText = "§cYou are in limbo! Use /server to leave at any time.";
	private String kickText = "Disconnected from {%server%}\\n{%reason%}";
	private boolean moveToEmptyWorld = true;
	private boolean doNotDisconnect = true;
	private String limboServerName = "limbo";
	private long delayBeforeTrying = 15000;
	private int maxReconnectTries = 2;
	private long reconnectTime = 1000;
	private long reconnectTimeout = 5000;
	private List<String> ignoredServers = new ArrayList<>();
	private String shutdownMessage = "Server closed";
	private Pattern shutdownPattern = null;
	private boolean debug = false;

	/**
	 * Config instance that holds all config info
	 * 
	 * Config is automatically loaded upon instantiation
	 * 
	 * @param instance
	 *            Instance of AutoReconnect plugin
	 */
	protected Config(AutoReconnect instance) {
		this.instance = instance;
		loadConfig();
	}

	/**
	 * Tries to load the config from the config file or creates a default config if
	 * the file does not exist.
	 */
	private void loadConfig() {
		try {
			if (!instance.getDataFolder().exists() && !instance.getDataFolder().mkdir()) {
				throw new IOException("Could not create plugin directory!");
			}
			File configFile = new File(instance.getDataFolder(), "config.yml");
			if (configFile.exists()) {
				Configuration configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
				int pluginConfigVersion = ConfigurationProvider.getProvider(YamlConfiguration.class).load(instance.getResourceAsStream("config.yml")).getInt("version");
				if (configuration.getInt("version") < pluginConfigVersion) {
					instance.getLogger().info("Found an old config version! Replacing with new one...");
					File oldConfigFile = new File(instance.getDataFolder(), "config.old.yml");
					Files.move(configFile, oldConfigFile);
					instance.getLogger().info("A backup of your old config has been saved to " + oldConfigFile + "!");
					saveDefaultConfig(configFile);
					return;
				}

				reconnectingChat = ChatColor.translateAlternateColorCodes('&', configuration.getString("reconnecting-text.chat", reconnectingChat));
				reconnectingTitle = ChatColor.translateAlternateColorCodes('&', configuration.getString("reconnecting-text.title", reconnectingTitle));
				reconnectingActionBar = ChatColor.translateAlternateColorCodes('&', configuration.getString("reconnecting-text.actionbar", reconnectingActionBar));
				reconnectingSendInterval = Math.max(configuration.getLong("reconnecting-text.send-interval", reconnectingSendInterval), 0);
				connectingChat = ChatColor.translateAlternateColorCodes('&', configuration.getString("connecting-text.chat", connectingChat));
				connectingTitle = ChatColor.translateAlternateColorCodes('&', configuration.getString("connecting-text.title", connectingTitle));
				connectingActionBar = ChatColor.translateAlternateColorCodes('&', configuration.getString("connecting-text.actionbar", connectingActionBar));
				rejectedChat = ChatColor.translateAlternateColorCodes('&', configuration.getString("rejected-text.chat", rejectedChat));
				rejectedTitle = ChatColor.translateAlternateColorCodes('&', configuration.getString("rejected-text.title", rejectedTitle));
				rejectedActionBar = ChatColor.translateAlternateColorCodes('&', configuration.getString("rejected-text.actionbar", rejectedActionBar));
				failedChat = ChatColor.translateAlternateColorCodes('&', configuration.getString("failed-text.chat", failedChat));
				failedTitle = ChatColor.translateAlternateColorCodes('&', configuration.getString("failed-text.title", failedTitle));
				failedActionBar = ChatColor.translateAlternateColorCodes('&', configuration.getString("failed-text.actionbar", failedActionBar));
				limboText = ChatColor.translateAlternateColorCodes('&', configuration.getString("limbo-text", limboText));
				kickText = ChatColor.translateAlternateColorCodes('&', configuration.getString("kick-text", kickText));
				moveToEmptyWorld = configuration.getBoolean("move-to-empty-world", moveToEmptyWorld);
				doNotDisconnect = configuration.getBoolean("do-not-disconnect", doNotDisconnect);
				delayBeforeTrying = configuration.getLong("delay-before-trying", delayBeforeTrying);
				maxReconnectTries = Math.max(configuration.getInt("max-reconnect-tries", maxReconnectTries), 1);
				reconnectTime = Math.max(configuration.getLong("reconnect-time", reconnectTime), 0);
				reconnectTimeout = Math.max(configuration.getLong("reconnect-timeout", reconnectTimeout), 1000);
				ignoredServers = configuration.getStringList("ignored-servers");
				String shutdownText = configuration.getString("shutdown.text", shutdownMessage);
				if (Strings.isNullOrEmpty(shutdownText)) {
					shutdownMessage = null;
					shutdownPattern = null;
				} else if (!configuration.getBoolean("shutdown.regex")) {
					shutdownMessage = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', shutdownText)); // strip all color codes
				} else {
					try {
						shutdownPattern = Pattern.compile(shutdownText);
						shutdownMessage = null;
					} catch (Exception e) {
						instance.getLogger().warning("Could not compile shutdown regex! Please check your config! Using default shutdown message...");
					}
				}
				debug = configuration.getBoolean("debug", debug);
			} else {
				saveDefaultConfig(configFile);
			}
		} catch (IOException e) {
			instance.getLogger().warning("Could not load config, using default values...");
			e.printStackTrace();
		}
	}

	/**
	 * Saves the default config
	 * 
	 * @param configFile
	 *            {@link File} of config file location
	 * @throws IOException
	 */
	private void saveDefaultConfig(File configFile) throws IOException {
		if (!configFile.createNewFile()) {
			throw new IOException("Could not create default config!");
		}
		try (InputStream is = instance.getResourceAsStream("config.yml"); OutputStream os = new FileOutputStream(configFile)) {
			ByteStreams.copy(is, os);
		}
	}

	/**
	 * @return Chat message sent during reconnection
	 */
	public String getReconnectingChat() {
		return reconnectingChat;
	}

	/**
	 * @return Title message sent during reconnection
	 */
	public String getReconnectingTitle() {
		return reconnectingTitle;
	}

	/**
	 * @return Actionbar message sent during reconnection
	 */
	public String getReconnectingActionBar() {
		return reconnectingActionBar;
	}

	/**
	 * @return Interval between sending reconnecting messages
	 */
	public long getReconnectingSendInterval() {
		return reconnectingSendInterval;
	}

	/**
	 * @return Chat message sent when connecting
	 */
	public String getConnectingChat() {
		return connectingChat;
	}

	/**
	 * @return Title message sent when connecting
	 */
	public String getConnectingTitle() {
		return connectingTitle;
	}

	/**
	 * @return Actionbar message sent when connecting
	 */
	public String getConnectingActionBar() {
		return connectingActionBar;
	}

	/**
	 * @return Chat message sent when reconnection was rejected
	 */
	public String getRejectedChat() {
		return rejectedChat;
	}

	/**
	 * @return Title message sent when reconnection was rejected
	 */
	public String getRejectedTitle() {
		return rejectedTitle;
	}

	/**
	 * @return Actionbar message sent when reconnection was rejected
	 */
	public String getRejectedActionBar() {
		return rejectedActionBar;
	}

	/**
	 * @return Chat message sent when reconnection failed
	 */
	public String getFailedChat() {
		return failedChat;
	}

	/**
	 * @return Title message sent when reconnection failed
	 */
	public String getFailedTitle() {
		return failedTitle;
	}

	/**
	 * @return Actionbar message sent when reconnection failed
	 */
	public String getFailedActionBar() {
		return failedActionBar;
	}

	/**
	 * @return Message sent to player when they are unable to be reconnected and are
	 *         left in limbo
	 */
	public String getLimboText() {
		return limboText;
	}

	/**
	 * @return Kick message sent when unable to keep player connected
	 */
	public String getKickText() {
		return kickText;
	}

	/**
	 * @return Whether player should be moved to an empty limbo world
	 */
	public boolean getMoveToEmptyWorld() {
		return moveToEmptyWorld;
	}

	/**
	 * @return Whether player should not be disconnected if reconnection fails
	 */
	public boolean getDoNotDisconnect() {
		return doNotDisconnect;
	}
	
	/**
	 * @return Name of virtual limbo server
	 */
	public String getLimboServerName() {
		return limboServerName;
	}

	/**
	 * @return Delay before attempting to reconnect
	 */
	public long getDelayBeforeTrying() {
		return delayBeforeTrying;
	}

	/**
	 * @return Max number of reconnection attempts
	 */
	public int getMaxReconnectTries() {
		return maxReconnectTries;
	}

	/**
	 * @return Time between reconnection attempts
	 */
	public long getReconnectTime() {
		return reconnectTime;
	}

	/**
	 * @return Timeout until reconnection attempt fails
	 */
	public long getReconnectTimeout() {
		return reconnectTimeout;
	}

	/**
	 * @return List of servers to ignore reconnecting to
	 */
	public List<String> getIgnoredServers() {
		return ignoredServers;
	}

	/**
	 * @return Kick message that triggers reconnection attempts
	 */
	public String getShutdownMessage() {
		return shutdownMessage;
	}

	/**
	 * @return Kick message regex pattern that triggers reconnection attempts
	 */
	public Pattern getShutdownPattern() {
		return shutdownPattern;
	}
	
	/**
	 * @return True if debug messages should be outputted
	 */
	public boolean isDebugEnabled() {
		return debug;
	}

}
