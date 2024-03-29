package me.aoelite.bungee.autoreconnect;

import com.google.common.base.Strings;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.util.internal.PlatformDependent;
import me.aoelite.bungee.autoreconnect.net.BasicChannelInitializer;
import me.aoelite.bungee.autoreconnect.net.packets.util.Util;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.BungeeServerInfo;
import net.md_5.bungee.ServerConnection;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerConnectRequest;
import net.md_5.bungee.api.Title;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerConnectEvent.Reason;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.netty.PipelineUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ReconnectTask {

	private static final TextComponent EMPTY = new TextComponent("");

	protected static boolean oldPipelineUtils = false;
	private static Method getPipelineChannel = null;
	protected static boolean waterfallEventGroups = false;
	private static Field eventGroupsField = null;
	private static EventLoopGroup eventGroupsCache = null;

	private final AutoReconnect instance;
	private final ProxyServer bungee;
	private final UserConnection user;
	private final ServerConnection server;
	private final BungeeServerInfo target;
	private final long startTime = System.currentTimeMillis();
	private final String kickMessage;
	private final ScheduledTask reconnectMessageUpdate;
	private volatile ScheduledTask actionBarRefresh;
	private volatile long lastUpdate = 0;

	private volatile int numDots;
	private int tries;

	public ReconnectTask(AutoReconnect instance, ProxyServer bungee, UserConnection user, ServerConnection server, String kickMessage) {
		this.instance = instance;
		this.bungee = bungee;
		this.user = user;
		this.server = server;
		this.target = server.getInfo();
		this.kickMessage = kickMessage;

		// Schedule task to update the messages sent to the user to let them know they
		// are in the process of reconnecting
		if (instance.getConfig().getReconnectingSendInterval() > 0) {
			this.reconnectMessageUpdate = BungeeCord.getInstance().getScheduler().schedule(instance, new Runnable() {
				@Override
				public void run() {
					if (instance.getReconnectHandler().isUserOnline(user) && Objects.equals(user.getServer(), server)) {
						updateStatusMessages();
					} else {
						if (reconnectMessageUpdate != null)
							reconnectMessageUpdate.cancel();
					}
				}
			}, 0, instance.getConfig().getReconnectingSendInterval(), TimeUnit.MILLISECONDS);
		} else {
			reconnectMessageUpdate = null;
			updateStatusMessages();
		}
	}
	
	public void startReconnect() {
		instance.getReconnectHandler().keepAlive(user.getUniqueId(), user);
		if (!instance.getConfig().getReconnectingChat().isEmpty())
			user.sendMessage(instance.getConfig().getReconnectingChat().replace("{%server%}", server.getInfo().getName()));
		if (reconnectMessageUpdate == null)
			updateStatusMessages();
		BungeeCord.getInstance().getScheduler().schedule(instance, new Runnable() {
			@Override
			public void run() {
				tryReconnect();
			}
		}, instance.getConfig().getDelayBeforeTrying(), TimeUnit.MILLISECONDS);
	}

	/**
	 * Tries to reconnect the User to the specified Server. In case that fails, this
	 * method will be executed again after a short timeout.
	 */
	public void tryReconnect() {
		if (instance.getConfig().isDebugEnabled())
			instance.getLogger().info("Reconnect attempt " + (tries + 1) + " for " + user.getDisplayName() + " (" + user.getUUID() + ") to " + server.getInfo().getName());
		if (++tries > instance.getConfig().getMaxReconnectTries()) {
			if (instance.getConfig().isDebugEnabled())
				instance.getLogger().info("Max tries reached for " + user.getDisplayName() + " (" + user.getUUID() + ") to " + server.getInfo().getName());
			// If we have reached the maximum reconnect limit, proceed BungeeCord-like.
			instance.getReconnectHandler().cancelReconnectTask(user.getUniqueId());
			// getFallbackServer()
			ServerInfo def = user.updateAndGetNextServer(server.getInfo());
			if (def != null) {
				if (instance.getConfig().isDebugEnabled())
					instance.getLogger().info("Sending " + user.getDisplayName() + " (" + user.getUUID() + ") to fallback server " + def.getName());
				// If the fallback-server is not the same server we tried to reconnect to, send
				// the user to that one instead.
				server.setObsolete(true);
				connect((BungeeServerInfo) def, false, ServerConnectEvent.Reason.SERVER_DOWN_REDIRECT);

				// Send fancy title if it's enabled in config, otherwise reset the connecting
				// title.
				if (!instance.getConfig().getFailedChat().isEmpty())
					user.sendMessage(instance.getConfig().getFailedChat().replace("{%reason%}", kickMessage).replace("{%server%}", server.getInfo().getName()));
				if (!instance.getConfig().getFailedActionBar().isEmpty())
					user.sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(instance.getConfig().getFailedActionBar().replace("{%reason%}", kickMessage).replace("{%server%}", server.getInfo().getName())));
				else
					user.sendMessage(ChatMessageType.ACTION_BAR, EMPTY);
				if (!instance.getConfig().getFailedTitle().isEmpty())
					createTitle(instance.getConfig().getFailedTitle().replace("{%reason%}", kickMessage).replace("{%server%}", server.getInfo().getName())).send(user);
				else
					user.sendTitle(ProxyServer.getInstance().createTitle().reset());
			} else {
				if (instance.getConfig().isDebugEnabled())
					instance.getLogger().info("No fallback server for " + user.getDisplayName() + " (" + user.getUUID() + ")");
				// Otherwise, disconnect the user with a "Lost Connection"-message.
				// If do-not-disconnect is set to true in config, and the player can enter
				// limbo, they will be left in limbo instead
				if (instance.getConfig().getMoveToEmptyWorld() && instance.getPacketManager().isProtocolizeLoaded() && instance.getConfig().getDoNotDisconnect()) {
					if (!instance.getConfig().getFailedChat().isEmpty())
						user.sendMessage(instance.getConfig().getFailedChat().replace("{%reason%}", kickMessage).replace("{%server%}", server.getInfo().getName()));
					if (!instance.getConfig().getFailedActionBar().isEmpty())
						user.sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(instance.getConfig().getFailedActionBar().replace("{%reason%}", kickMessage).replace("{%server%}", server.getInfo().getName())));
					else
						user.sendMessage(ChatMessageType.ACTION_BAR, EMPTY);
					if (!instance.getConfig().getFailedTitle().isEmpty())
						createTitle(instance.getConfig().getFailedTitle().replace("{%reason%}", kickMessage).replace("{%server%}", server.getInfo().getName())).send(user);
					else
						user.sendTitle(ProxyServer.getInstance().createTitle().reset());
				} else {
					user.disconnect(instance.getConfig().getKickText().isEmpty() ? kickMessage : instance.getConfig().getKickText().replace("{%reason%}", kickMessage).replace("{%server%}", server.getInfo().getName()));
				}
			}
			return;
		}

		// If we are already connecting to a server, cancel the reconnect task.
		if (user.getPendingConnects().contains(target)) {
			instance.getLogger().warning("User already connecting to " + target);
			return;
		}

		// Add pending connection.
		user.getPendingConnects().add(target);

		// Add a try if the delay is not active
		if (startTime + instance.getConfig().getDelayBeforeTrying() <= System.currentTimeMillis()) {
			tries++;
		}

		// If status messages are not sent periodically, send on reconnection attempt
		if (reconnectMessageUpdate == null)
			updateStatusMessages();

		// Establish connection to the server.
		if (instance.getConfig().isDebugEnabled())
			instance.getLogger().info("Attempting to ping server for " + user.getDisplayName() + " (" + user.getUUID() + ") to connect to " + server.getInfo().getName());
		ping(target, new Callback<Boolean>() {
			@Override
			public void done(Boolean result, Throwable error) {
				if (instance.getConfig().isDebugEnabled())
					instance.getLogger().info("Pinged server for " + user.getDisplayName() + " (" + user.getUUID() + ") to connect to " + server.getInfo().getName() + " Result: " + (result ? "Available" : "Unavailable"));
				if (result) {
					if (instance.getConfig().isDebugEnabled())
						instance.getLogger().info("Will connect " + user.getDisplayName() + " (" + user.getUUID() + ") to " + server.getInfo().getName());
					// If pinged successfully, attempt reconnection.
					ChannelInitializer<Channel> initializer = new BasicChannelInitializer(bungee, user, target);
					ChannelFutureListener listener = future -> {
						if (future.isSuccess()) {
							if (instance.getConfig().isDebugEnabled())
								instance.getLogger().info("Connection successful for " + user.getDisplayName() + " (" + user.getUUID() + ") to " + server.getInfo().getName());
							// If reconnected successfully, remove from map and send another fancy title.
							instance.getReconnectHandler().cancelReconnectTask(user.getUniqueId());
							if (reconnectMessageUpdate != null)
								reconnectMessageUpdate.cancel();
							if (actionBarRefresh != null)
								actionBarRefresh.cancel();

							if (!instance.getConfig().getConnectingChat().isEmpty())
								user.sendMessage(instance.getConfig().getConnectingChat().replace("{%server%}", server.getInfo().getName()));
							if (!instance.getConfig().getConnectingActionBar().isEmpty())
								user.sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(instance.getConfig().getConnectingActionBar().replace("{%server%}", server.getInfo().getName())));
							else
								user.sendMessage(ChatMessageType.ACTION_BAR, EMPTY);
							if (!instance.getConfig().getConnectingTitle().isEmpty())
								createTitle(instance.getConfig().getConnectingTitle().replace("{%server%}", server.getInfo().getName())).send(user);
							else
								user.sendTitle(ProxyServer.getInstance().createTitle().reset());
						} else {
							if (instance.getConfig().isDebugEnabled())
								instance.getLogger().info("Connection failed for " + user.getDisplayName() + " (" + user.getUUID() + ") to " + server.getInfo().getName());
							future.channel().close();
							user.getPendingConnects().remove(target);

							// Schedule next reconnect.
							BungeeCord.getInstance().getScheduler().schedule(instance, new Runnable() {
								@Override
								public void run() {
									// Only retry to reconnect the user if he is still online and hasn't been moved
									// to another server.
									if (instance.getReconnectHandler().isUserOnline(user) && Objects.equals(user.getServer(), server)) {
										tryReconnect();
									} else {
										instance.getReconnectHandler().cancelReconnectTask(user.getUniqueId());
									}
								}
							}, instance.getConfig().getReconnectTime(), TimeUnit.MILLISECONDS);
						}
					};

					// Create a new Netty Bootstrap that contains the ChannelInitializer and the
					// ChannelFutureListener.
					Bootstrap b = new Bootstrap().channel(getChannel(target.getAddress())).group(server.getCh().getHandle().eventLoop()).handler(initializer).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) instance.getConfig().getReconnectTimeout()).remoteAddress(target.getAddress());

					// Windows is bugged, multi homed users will just have to live with random
					// connecting IPs
					if (user.getPendingConnection().getListener().isSetLocalAddress() && !PlatformDependent.isWindows()) {
						b.localAddress(((InetSocketAddress) user.getPendingConnection().getListener().getSocketAddress()).getHostString(), 0);
					}
					if (instance.getConfig().isDebugEnabled())
						instance.getLogger().info("Attempting to connect " + user.getDisplayName() + " (" + user.getUUID() + ") to " + server.getInfo().getName());
					b.connect().addListener(listener);
				} else {
					if (instance.getConfig().isDebugEnabled())
						instance.getLogger().info("Will not attempt to connect " + user.getDisplayName() + " (" + user.getUUID() + ") to " + server.getInfo().getName());
					user.getPendingConnects().remove(target);

					// Schedule next reconnect.
					BungeeCord.getInstance().getScheduler().schedule(instance, new Runnable() {
						@Override
						public void run() {
							// Only retry to reconnect the user if he is still online and hasn't been moved
							// to another server.
							if (instance.getReconnectHandler().isUserOnline(user) && Objects.equals(user.getServer(), server)) {
								tryReconnect();
							} else {
								instance.getReconnectHandler().cancelReconnectTask(user.getUniqueId());
							}
						}
					}, instance.getConfig().getReconnectTime(), TimeUnit.MILLISECONDS);
				}
			}
		});
	}
	
	private void ping(BungeeServerInfo target, Callback<Boolean> callback) {
		if (instance.getConfig().isDebugEnabled())
			instance.getLogger().info("Pinging for " + user.getDisplayName() + " (" + user.getUUID() + ") to " + target.getName());
		Bootstrap b = new Bootstrap().channel(getChannel(target.getAddress())).group(getEventLoopGroup()).handler((ChannelHandler) PipelineUtils.BASE).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) instance.getConfig().getReconnectTimeout()).remoteAddress(target.getAddress());
		b.connect().addListener(future -> callback.done(future.isSuccess(), future.cause()));
	}

	private void connect(BungeeServerInfo target, boolean retry, Reason reason) {
		if (instance.getConfig().isDebugEnabled())
			instance.getLogger().info("Connecting " + user.getDisplayName() + " (" + user.getUUID() + ") to " + target.getName());
		user.setDimensionChange(true);
		ServerConnectRequest.Builder builder = ServerConnectRequest.builder().retry(retry).reason(reason).target(target);
		ServerConnectRequest request = builder.build();
		user.getPendingConnects().add((ServerInfo) target);
		BasicChannelInitializer initializer = new BasicChannelInitializer(bungee, user, target);
		ChannelFutureListener listener = future -> {
			if (!future.isSuccess()) {
				future.channel().close();
				user.getPendingConnects().remove((Object) target);
				ServerInfo def = user.updateAndGetNextServer((ServerInfo) target);
				if (request.isRetry() && def != null && (Objects.equals(user.getServer(), server) || def != user.getServer().getInfo())) {
					if (instance.getConfig().isDebugEnabled())
						instance.getLogger().info("Connection failed for " + user.getDisplayName() + " (" + user.getUUID() + ") to " + target.getName() + " Will connect to fallback");
					user.sendMessage(bungee.getTranslation("fallback_lobby", new Object[0]));
					connect((BungeeServerInfo) def, true, ServerConnectEvent.Reason.LOBBY_FALLBACK);
				} else {
					if (instance.getConfig().isDebugEnabled())
						instance.getLogger().info("Connection failed for " + user.getDisplayName() + " (" + user.getUUID() + ") to " + target.getName() + " Will leave in limbo or disconnect if unavailable");
					if (instance.getConfig().getMoveToEmptyWorld() && instance.getPacketManager().isProtocolizeLoaded() && instance.getConfig().getDoNotDisconnect()) {
						user.sendMessage(instance.getConfig().getLimboText());
						user.setServer(new LimboServer(instance));
					} else {
						user.disconnect(instance.getConfig().getKickText().isEmpty() ? kickMessage : instance.getConfig().getKickText().replace("{%reason%}", kickMessage).replace("{%server%}", server.getInfo().getName()));
					}
				}
			}
		};

		// Create a new Netty Bootstrap that contains the ChannelInitializer and the
		// ChannelFutureListener.
		Bootstrap b = new Bootstrap().channel(getChannel(target.getAddress())).group(Util.getUserChannelWrapper(user).getHandle().eventLoop()).handler(initializer).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, request.getConnectTimeout()).remoteAddress(target.getAddress());

		// Windows is bugged, multi homed users will just have to live with random
		// connecting IPs
		if (user.getPendingConnection().getListener().isSetLocalAddress() && !PlatformDependent.isWindows()) {
			b.localAddress(((InetSocketAddress) user.getPendingConnection().getListener().getSocketAddress()).getHostString(), 0);
		}
		b.connect().addListener(listener);
	}

	/**
	 * Send the current reconnecting messages to the user
	 */
	private void updateStatusMessages() {
		// Increment number of dots to display
		numDots++;
		if (!instance.getConfig().getReconnectingActionBar().isEmpty()) {
			user.sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(instance.getConfig().getReconnectingActionBar().replace("{%server%}", server.getInfo().getName()).replace("{%dots%}", getDots())));
			if (instance.getConfig().getReconnectingSendInterval() > 1000) {
				// Schedule task to resend the action bar message approximately every second, if
				// necessary, so that it won't fade away
				if (actionBarRefresh != null)
					actionBarRefresh.cancel();
				actionBarRefresh = BungeeCord.getInstance().getScheduler().schedule(instance, new Runnable() {
					@Override
					public void run() {
						if (instance.getReconnectHandler().isUserOnline(user) && Objects.equals(user.getServer(), server)) {
							user.sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(instance.getConfig().getReconnectingActionBar().replace("{%server%}", server.getInfo().getName()).replace("{%dots%}", getDots())));
						} else if (actionBarRefresh != null)
							actionBarRefresh.cancel();
						if (instance.getConfig().getReconnectingSendInterval() > 0 && instance.getConfig().getReconnectingSendInterval() - (System.currentTimeMillis() - lastUpdate) <= 1500 && actionBarRefresh != null)
							actionBarRefresh.cancel();
					}
				}, 1, 1, TimeUnit.SECONDS);
			}
		}
		if (!instance.getConfig().getReconnectingTitle().isEmpty())
			createReconnectingTitle(instance.getConfig().getReconnectingTitle().replace("{%server%}", server.getInfo().getName()).replace("{%dots%}", getDots())).send(user);
		lastUpdate = System.currentTimeMillis();
	}

	/**
	 * Created a Title containing the given text to stay during the update duration.
	 *
	 * @return a Title that can be send to the player.
	 */
	private Title createReconnectingTitle(String msg) {
		Title title = ProxyServer.getInstance().createTitle();
		title.title(EMPTY);
		title.subTitle(new TextComponent(msg));
		title.stay((int) ((instance.getConfig().getReconnectingSendInterval() + 1000) / 1000 * 20));
		title.fadeIn(0);
		title.fadeOut(0);
		return title;
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

	/**
	 * @return a String that is made of dots for the "dots animation".
	 */
	private String getDots() {
		switch (numDots % 4) {
		case 0:
		default:
			return "";
		case 1:
			return ".";
		case 2:
			return "..";
		case 3:
			return "...";
		}
	}

	/**
	 * Resets the title and action bar message if the player is still online
	 */
	public void cancel() {
		if (reconnectMessageUpdate != null)
			reconnectMessageUpdate.cancel();
		if (actionBarRefresh != null)
			actionBarRefresh.cancel();
		if (instance.getReconnectHandler().isUserOnline(user)) {
			if (!Strings.isNullOrEmpty(instance.getConfig().getReconnectingTitle()) || !Strings.isNullOrEmpty(instance.getConfig().getConnectingTitle()) || !Strings.isNullOrEmpty(instance.getConfig().getRejectedTitle()) || !Strings.isNullOrEmpty(instance.getConfig().getFailedTitle())) {
				// For some reason, we have to reset and clear the title, so it completely
				// disappears -> BungeeCord bug?
				bungee.createTitle().reset().clear().send(user);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private Class<? extends Channel> getChannel(SocketAddress addr) {
		if (!oldPipelineUtils)
			return PipelineUtils.getChannel(addr);
		else
			try {
				return (Class<? extends Channel>) getPipelineChannel.invoke(null);
			} catch (ClassCastException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}
		return null;
	}
	
	private EventLoopGroup getEventLoopGroup() {
		if (eventGroupsCache != null && !eventGroupsCache.isShutdown()) {
			return eventGroupsCache;
		}
		if (!waterfallEventGroups) {
			try {
				eventGroupsCache = (EventLoopGroup) eventGroupsField.get(BungeeCord.getInstance());
				return eventGroupsCache;
			} catch (ClassCastException | IllegalAccessException | IllegalArgumentException e) {
				e.printStackTrace();
			}
		} else {
			eventGroupsCache = BungeeCord.getInstance().workerEventLoopGroup;
			return eventGroupsCache;
		}
		return null;
	}
	
	protected static void init() {
		try {
			PipelineUtils.class.getMethod("getChannel", SocketAddress.class);
		} catch (NoSuchMethodException | SecurityException e) {
			oldPipelineUtils = true;
		}
		if (oldPipelineUtils) {
			Logger.getLogger("AutoReconnect").info("Detected old BungeeCord build! Using compatibility mode for socket channel!");
			try {
				getPipelineChannel = PipelineUtils.class.getMethod("getChannel");
				getPipelineChannel.setAccessible(true);
			} catch (NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
			}
		}
		try {
			BungeeCord.class.getField("workerEventLoopGroup");
			waterfallEventGroups = true;
		} catch (NoSuchFieldException | SecurityException e) {
			waterfallEventGroups = false;
		}
		if (!waterfallEventGroups) {
			Logger.getLogger("AutoReconnect").info("Detected BungeeCord build! Using compatibility mode for event loop group!");
			try {
				eventGroupsField = BungeeCord.class.getField("eventLoops");
				eventGroupsField.setAccessible(true);
			} catch (NoSuchFieldException | SecurityException e) {
				e.printStackTrace();
			}
		}
	}

}