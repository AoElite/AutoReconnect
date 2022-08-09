package me.aoelite.bungee.autoreconnect.net;

import me.aoelite.bungee.autoreconnect.AutoReconnect;
import net.md_5.bungee.ServerConnection;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.Util;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.Title;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerDisconnectEvent;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.connection.CancelSendSignal;
import net.md_5.bungee.connection.DownstreamBridge;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.protocol.packet.Kick;

public class ReconnectBridge extends DownstreamBridge {

	private static final TextComponent EMPTY = new TextComponent("");

	private final AutoReconnect instance;
	private final ProxyServer bungee;
	private final UserConnection user;
	private final ServerConnection server;

	public ReconnectBridge(AutoReconnect instance, ProxyServer bungee, UserConnection user, ServerConnection server) {
		super(bungee, user, server);
		this.instance = instance;
		this.bungee = bungee;
		this.user = user;
		this.server = server;
	}

	@Override
	public void exception(Throwable t) throws Exception {
		String errorMessage = bungee.getTranslation("server_went_down");
		// Usually, BungeeCord would reconnect the Player to the fallback server or kick
		// him if not
		// Fallback Server is available, when an Exception between the BungeeCord and
		// the Minecraft Server
		// occurs. We override this Method so that we can try to reconnect the client
		// instead.

		if (server.isObsolete()) {
			// do not perform any actions if the user has already moved
			return;
		}
		// setObsolete so that DownstreamBridge.disconnected(ChannelWrapper) won't be
		// called.
		server.setObsolete(true);

		// Fire ServerReconnectEvent and give plugins the possibility to cancel server
		// reconnecting.
		if (!instance.getReconnectHandler().fireServerReconnectEvent(user, server)) {
			// Invoke default behaviour if event has been cancelled.
			ServerInfo def = user.updateAndGetNextServer((ServerInfo) this.server.getInfo());
			if (def != null) {
				// Attempt connection to fallback server
				user.connectNow(def, ServerConnectEvent.Reason.SERVER_DOWN_REDIRECT);
				// Send error message to user to inform them why they were kicked
				if (!instance.getConfig().getRejectedChat().isEmpty())
					user.sendMessage(instance.getConfig().getRejectedChat().replace("{%reason%}", errorMessage).replace("{%server%}", server.getInfo().getName()));
				if (!instance.getConfig().getRejectedActionBar().isEmpty())
					user.sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(instance.getConfig().getRejectedActionBar().replace("{%reason%}", errorMessage).replace("{%server%}", server.getInfo().getName())));
				if (!instance.getConfig().getRejectedTitle().isEmpty())
					createTitle(instance.getConfig().getRejectedTitle().replace("{%reason%}", errorMessage).replace("{%server%}", server.getInfo().getName())).send(user);
			} else {
				user.disconnect(instance.getConfig().getKickText().isEmpty() ? Util.exception(t) : instance.getConfig().getKickText().replace("{%reason%}", Util.exception(t)).replace("{%server%}", server.getInfo().getName()));
			}
		} else {
			// Otherwise, reconnect the User if he is still online.
			instance.getReconnectHandler().reconnectIfOnline(user, server, errorMessage);
		}
	}

	@Override
	public void disconnected(ChannelWrapper channel) throws Exception {
		String errorMessage = bungee.getTranslation("lost_connection");
		// Usually, BungeeCord would reconnect the Player to the fallback server or kick
		// him if not
		// Fallback Server is available, when an Exception between the BungeeCord and
		// the Minecraft Server
		// occurs. We override this Method so that we can try to reconnect the client
		// instead.

		server.getInfo().removePlayer(user);
		
		if (server.isObsolete()) {
			// do not perform any actions if the user has already moved
			return;
		}
		// setObsolete so that DownstreamBridge.disconnected(ChannelWrapper) won't be
		// called.
		server.setObsolete(true);

		// Fire ServerReconnectEvent and give plugins the possibility to cancel server
		// reconnecting.
		if (user.isConnected()) {
			if (!instance.getReconnectHandler().fireServerReconnectEvent(user, server)) {
				// Invoke default behaviour if event has been cancelled.
				ServerInfo def = user.updateAndGetNextServer((ServerInfo) this.server.getInfo());
				if (def != null) {
					// Attempt connection to fallback server
					user.connectNow(def, ServerConnectEvent.Reason.SERVER_DOWN_REDIRECT);
					// Send error message to user to inform them why they were kicked
					if (!instance.getConfig().getRejectedChat().isEmpty())
						user.sendMessage(instance.getConfig().getRejectedChat().replace("{%reason%}", errorMessage).replace("{%server%}", server.getInfo().getName()));
					if (!instance.getConfig().getRejectedActionBar().isEmpty())
						user.sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(instance.getConfig().getRejectedActionBar().replace("{%reason%}", errorMessage).replace("{%server%}", server.getInfo().getName())));
					if (!instance.getConfig().getRejectedTitle().isEmpty())
						createTitle(instance.getConfig().getRejectedTitle().replace("{%reason%}", errorMessage).replace("{%server%}", server.getInfo().getName())).send(user);
				} else {
					user.disconnect(instance.getConfig().getKickText().isEmpty() ? errorMessage : instance.getConfig().getKickText().replace("{%reason%}", errorMessage).replace("{%server%}", server.getInfo().getName()));
				}
			} else {
				// Otherwise, reconnect the User if he is still online.
				instance.getReconnectHandler().reconnectIfOnline(user, server, errorMessage);
			}
		}
		ServerDisconnectEvent serverDisconnectEvent = new ServerDisconnectEvent(user, server.getInfo());
		bungee.getPluginManager().callEvent(serverDisconnectEvent);
	}

	@Override
	public void handle(Kick kick) throws Exception {
		// This method is called whenever a Kick-Packet is sent from the Minecraft
		// Server to the Minecraft Client.

		// .getFallbackServer()
		ServerInfo def = user.updateAndGetNextServer(server.getInfo());
		// Call ServerKickEvent
		ServerKickEvent event = bungee.getPluginManager().callEvent(new ServerKickEvent(user, server.getInfo(), ComponentSerializer.parse(kick.getMessage()), def, ServerKickEvent.State.CONNECTED, ServerKickEvent.Cause.SERVER));
		if (event.isCancelled() && event.getCancelServer() != null) {
			user.connectNow(event.getCancelServer(), ServerConnectEvent.Reason.KICK_REDIRECT);
		} else {
			String kickMessageColor = BaseComponent.toLegacyText(ComponentSerializer.parse(kick.getMessage()));
			String kickMessage = ChatColor.stripColor(kickMessageColor); // needs to be parsed like that...
			// doReconnect indicates whether the player should be reconnected or not after
			// he has been kicked. Only if the kick reason matches the one that has been
			// pre-defined on the config, we allow him to reconnect.
			boolean doReconnect = true;
			if (instance.getConfig().getShutdownMessage() != null) {
				doReconnect = instance.getConfig().getShutdownMessage().equals(kickMessage);
			} else if (instance.getConfig().getShutdownPattern() != null) {
				try {
					doReconnect = instance.getConfig().getShutdownPattern().matcher(kickMessage).matches();
				} catch (Exception e) {
					instance.getLogger().warning("Could not match shutdown-pattern " + instance.getConfig().getShutdownPattern().pattern());
				}
			}

			// As always, we fire a ServerReconnectEvent and give plugins the possibility to
			// cancel server reconnecting.
			if (!doReconnect || !instance.getReconnectHandler().fireServerReconnectEvent(user, server)) {
				// Invoke default behaviour if event has been cancelled and disconnect the
				// player.
				user.disconnect(instance.getConfig().getKickText().isEmpty() ? kickMessageColor : instance.getConfig().getKickText().replace("{%reason%}", kickMessageColor).replace("{%server%}", server.getInfo().getName()));
			} else {
				// Otherwise, reconnect the User if he is still online.
				instance.getReconnectHandler().reconnectIfOnline(user, server, kickMessageColor);
			}
		}
		server.setObsolete(true);

		// Throw Exception so that the Packet won't be send to the Minecraft Client.
		throw CancelSendSignal.INSTANCE;
	}

	/**
	 * Created a Title containing the given text.
	 *
	 * @return a Title that can be send to the player.
	 */
	private Title createTitle(String msg) {
		Title title = ProxyServer.getInstance().createTitle();
		title.title(EMPTY);
		title.subTitle(new TextComponent(msg));
		title.stay(80);
		title.fadeIn(10);
		title.fadeOut(10);
		return title;
	}
}
