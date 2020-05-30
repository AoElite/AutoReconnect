package me.aoelite.bungee.autoreconnect;

import me.aoelite.bungee.autoreconnect.api.ServerReconnectEvent;
import me.aoelite.bungee.autoreconnect.net.ReconnectBridge;
import me.aoelite.bungee.autoreconnect.net.packets.PacketManager;
import me.aoelite.bungee.autoreconnect.net.packets.PositionLookPacket;
import net.md_5.bungee.PacketConstants;
import net.md_5.bungee.ServerConnection;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.netty.HandlerBoss;

import java.util.HashMap;
import java.util.UUID;

public final class AutoReconnect extends Plugin implements Listener {

	/**
	 * A HashMap containing all reconnect tasks.
	 */
	private HashMap<UUID, ReconnectTask> reconnectTasks = new HashMap<>();

	/**
	 * Config instance
	 */
	private Config config;
	
	private boolean isProtocolizeLoaded = false;

	@Override
	public void onEnable() {
		getLogger().info("AutoReconnect: A fork of Bungeecord-Reconnect updated by PseudoResonance and AoElite");
		// register Listener
		getProxy().getPluginManager().registerListener(this, this);
		
		try {
			isProtocolizeLoaded = Class.forName("de.exceptionflug.protocolize.api.event.PacketReceiveEvent") != null;
		} catch (ClassNotFoundException e) {
			isProtocolizeLoaded = false;
		}
		
		if (isProtocolizeLoaded())
			PacketManager.register(this);

		// load Configuration
		config = new Config(this);
		if (config.getMoveToEmptyWorld() && !isProtocolizeLoaded()) {
			this.getLogger().severe("Protocolize is not installed! Unable to send reconnecting players to an empty world!");
		}
	}

	@EventHandler
	public void onServerSwitch(ServerSwitchEvent event) {
		// We need to override the Downstream class of each user so that we can override
		// the disconnect methods of it.
		// ServerSwitchEvent is called just right after the Downstream Bridge has been
		// initialized, so we simply can
		// instantiate here our own implementation of the DownstreamBridge
		//
		// @see net.md_5.bungee.ServerConnector#L249

		ProxyServer bungee = getProxy();
		UserConnection user = (UserConnection) event.getPlayer();
		ServerConnection server = user.getServer();

		ChannelWrapper serverCh = server.getCh();

		ReconnectBridge downstreamBridge = new ReconnectBridge(this, bungee, user, server);
		serverCh.getHandle().pipeline().get(HandlerBoss.class).setHandler(downstreamBridge);

		// Cancel the reconnect task (if any exist) and clear title and action bar.
		if (isReconnecting(user.getUniqueId())) {
			cancelReconnectTask(user.getUniqueId());
		}
	}

	/**
	 * Checks whether the current server should be ignored and fires a
	 * ServerReconnectEvent afterwards.
	 *
	 * @param user
	 *            The User that should be reconnected.
	 * @param server
	 *            The Server the User should be reconnected to.
	 * @return true, if the ignore list does not contain the server and the event
	 *         hasn't been canceled.
	 */
	public boolean fireServerReconnectEvent(UserConnection user, ServerConnection server) {
		if (config.getIgnoredServers().contains(server.getInfo().getName())) {
			return false;
		}
		ServerReconnectEvent event = getProxy().getPluginManager().callEvent(new ServerReconnectEvent(user, server.getInfo()));
		return !event.isCancelled();
	}

	/**
	 * Checks if a UserConnection is still online.
	 *
	 * @param user
	 *            The User that should be checked.
	 * @return true, if the UserConnection is still online.
	 */
	public boolean isUserOnline(UserConnection user) {
		return getProxy().getPlayer(user.getUniqueId()) != null;
	}

	/**
	 * Reconnects a User to a Server, as long as the user is currently online. If he
	 * isn't, his reconnect task (if he had one) will be canceled.
	 *
	 * @param user
	 *            The User that should be reconnected.
	 * @param server
	 *            The Server the User should be connected to.
	 */
	public void reconnectIfOnline(UserConnection user, ServerConnection server, String kickMessage) {
		if (isUserOnline(user)) {
			if (!isReconnecting(user.getUniqueId())) {
				reconnect(user, server, kickMessage);
			}
		} else {
			cancelReconnectTask(user.getUniqueId());
		}
	}

	/**
	 * Reconnects the User without checking whether he's online or not.
	 *
	 * @param user
	 *            The User that should be reconnected.
	 * @param server
	 *            The Server the User should be connected to.
	 */
	private void reconnect(UserConnection user, ServerConnection server, String kickMessage) {
		if (getConfig().getMoveToEmptyWorld() && isProtocolizeLoaded()) {
			if (user.getDimension() <= 0) {
				user.unsafe().sendPacket(PacketConstants.DIM1_SWITCH);
				user.setDimension(1);
			} else if (user.getDimension() == 1) {
				user.unsafe().sendPacket(PacketConstants.DIM2_SWITCH);
				user.setDimension(-1);
			}
			user.unsafe().sendPacket(new PositionLookPacket(0, 64, 0, 0f, 0f, (byte) 0, 0));
		}
		ReconnectTask reconnectTask = reconnectTasks.get(user.getUniqueId());
		if (reconnectTask == null) {
			reconnectTasks.put(user.getUniqueId(), reconnectTask = new ReconnectTask(this, getProxy(), user, server, kickMessage));
		}
		reconnectTask.tryReconnect();
	}

	/**
	 * Removes a reconnect task from the main HashMap
	 *
	 * @param uuid
	 *            The UniqueId of the User.
	 */
	void cancelReconnectTask(UUID uuid) {
		ReconnectTask task = reconnectTasks.remove(uuid);
		if (task != null && getProxy().getPlayer(uuid) != null) {
			task.cancel();
		}
	}

	/**
	 * Checks whether a User has got a reconnect task.
	 *
	 * @param uuid
	 *            The UniqueId of the User.
	 * @return true, if there is a task that tries to reconnect the User to a
	 *         server.
	 */
	public boolean isReconnecting(UUID uuid) {
		return reconnectTasks.containsKey(uuid);
	}

	/**
	 * Returns the config instance
	 * 
	 * @return Config instance
	 */
	public Config getConfig() {
		return config;
	}

	/**
	 * @return true if Protocolize API is loaded
	 */
	private boolean isProtocolizeLoaded() {
		return isProtocolizeLoaded;
	}

}
