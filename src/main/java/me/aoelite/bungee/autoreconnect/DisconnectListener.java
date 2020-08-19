package me.aoelite.bungee.autoreconnect;

import java.util.UUID;

import me.aoelite.bungee.autoreconnect.net.ReconnectBridge;
import net.md_5.bungee.ServerConnection;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.netty.HandlerBoss;

public class DisconnectListener implements Listener {
	
	private final AutoReconnect autoReconnect;
	
	protected DisconnectListener(AutoReconnect autoReconnect) {
		this.autoReconnect = autoReconnect;
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

		ProxyServer bungee = autoReconnect.getProxy();
		UserConnection user = (UserConnection) event.getPlayer();
		ServerConnection server = user.getServer();

		ChannelWrapper serverCh = server.getCh();

		ReconnectBridge downstreamBridge = new ReconnectBridge(autoReconnect, bungee, user, server);
		serverCh.getHandle().pipeline().get(HandlerBoss.class).setHandler(downstreamBridge);

		// Cancel the reconnect task (if any exist) and clear title and action bar.
		UUID uuid = user.getUniqueId();
		if (autoReconnect.getReconnectHandler().isReconnecting(uuid)) {
			autoReconnect.getReconnectHandler().cancelReconnectTask(uuid);
		}
		if (autoReconnect.getReconnectHandler().isKeptAlive(uuid)) {
			autoReconnect.getReconnectHandler().cancelKeepAlive(uuid);
		}
	}

	@EventHandler
	public void onPlayerDisconnect(PlayerDisconnectEvent event) {
		UUID uuid = event.getPlayer().getUniqueId();
		if (autoReconnect.getReconnectHandler().isReconnecting(uuid)) {
			autoReconnect.getReconnectHandler().cancelReconnectTask(uuid);
		}
		if (autoReconnect.getReconnectHandler().isKeptAlive(uuid)) {
			autoReconnect.getReconnectHandler().cancelKeepAlive(uuid);
		}
	}

}
