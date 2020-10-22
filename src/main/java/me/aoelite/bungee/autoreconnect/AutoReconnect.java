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
	 * Whether or not the Protocolize plugin is loaded
	 */
	private static boolean isProtocolizeLoaded = false;
	
	/**
	 * bStats instance
	 */
	private Metrics metrics;

	@Override
	public void onEnable() {
		getLogger().info("AutoReconnect: A fork of Bungeecord-Reconnect updated by PseudoResonance and AoElite");
		// register Listener
		getProxy().getPluginManager().registerListener(this, new DisconnectListener(this));
		
		// load Configuration
		config = new Config(this);
		
		if (config.isDebugEnabled())
			getLogger().severe("Debug output is enabled!");

		// load dependency support
		try {
			isProtocolizeLoaded = Class.forName("de.exceptionflug.protocolize.api.protocol.AbstractPacket") != null;
		} catch (ClassNotFoundException e) {
			isProtocolizeLoaded = false;
		}
		if (isProtocolizeLoaded())
			PacketManager.register(this);
		
		// load reconnection handler
		reconnectHandler = new ReconnectHandler(this);

		// Initialize reflection if necessary
		ReconnectTask.init();
		
		// Initialize bStats
		metrics = new Metrics(this, 9174);
		// Add custom charts
		metrics.addCustomChart(new Metrics.SimplePie("old_pipeline_utils", () -> ReconnectTask.oldPipelineUtils ? "Enabled" : "Disabled"));
		metrics.addCustomChart(new Metrics.SimplePie("old_event_groups", () -> ReconnectTask.oldEventGroups ? "Enabled" : "Disabled"));
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
	 * @return true if Protocolize API is loaded
	 */
	public boolean isProtocolizeLoaded() {
		return isProtocolizeLoaded;
	}

}
