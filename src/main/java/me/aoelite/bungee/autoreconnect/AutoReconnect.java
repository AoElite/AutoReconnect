package me.aoelite.bungee.autoreconnect;

import me.aoelite.bungee.autoreconnect.net.packets.PacketManager;
import net.md_5.bungee.api.plugin.Plugin;
import java.util.Random;

import org.bstats.bungeecord.Metrics;

public final class AutoReconnect extends Plugin {

	/**
	 * An instance of {@link Random}
	 */
	public static final Random RANDOM = new Random();

	/**
	 * Config instance
	 */
	private Config config;
	
	/**
	 * An instance of {@linkReconnectHandler}
	 */
	private ReconnectHandler reconnectHandler;
	
	/**
	 * An instance of {@PacketManager}
	 */
	private PacketManager packetManager;
	
	/**
	 * bStats instance
	 */
	@SuppressWarnings("unused")
	private Metrics metrics;

	@Override
	public void onEnable() {
		getLogger().info("AutoReconnect: A fork of Bungeecord-Reconnect updated by PseudoResonance and AoElite");
		// register Listener
		getProxy().getPluginManager().registerListener(this, new DisconnectListener(this));
		getProxy().getPluginManager().registerListener(this, new UserVersionCache());
		
		// load Configuration
		config = new Config(this);
		
		if (config.isDebugEnabled())
			getLogger().severe("Debug output is enabled!");

		// load dependency support
		packetManager = new PacketManager(this);
		
		// load reconnection handler
		reconnectHandler = new ReconnectHandler(this);

		// Initialize reflection if necessary
		ReconnectTask.init();
		
		// Initialize bStats
		metrics = new Metrics(this, 9174);
	}

	@Override
	public void onDisable() {
		reconnectHandler.stop();
	}

	/**
	 * @return Config instance
	 */
	public Config getConfig() {
		return config;
	}
	
	/**
	 * @return ReconnectHandler instance
	 */
	public ReconnectHandler getReconnectHandler() {
		return reconnectHandler;
	}

	/**
	 * @return PacketManager instance
	 */
	public PacketManager getPacketManager() {
		return packetManager;
	}

}
