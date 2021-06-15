package me.aoelite.bungee.autoreconnect;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import me.aoelite.bungee.autoreconnect.api.ServerReconnectEvent;
import me.aoelite.bungee.autoreconnect.net.packets.PacketManager;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.ServerConnection;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.protocol.ProtocolConstants;
import net.md_5.bungee.protocol.packet.BossBar;
import net.md_5.bungee.protocol.packet.KeepAlive;
import net.md_5.bungee.protocol.packet.Login;
import net.md_5.bungee.protocol.packet.Respawn;

public class ReconnectHandler {

	private final AutoReconnect autoReconnect;

	/**
	 * A HashMap containing all reconnect tasks.
	 */
	private HashMap<UUID, ReconnectTask> reconnectTasks = new HashMap<>();

	/**
	 * A HashMap containing all users that should be kept alive.
	 */
	private ConcurrentHashMap<UUID, UserConnection> keepAliveUsers = new ConcurrentHashMap<>();

	protected ReconnectHandler(AutoReconnect autoReconnect) {
		this.autoReconnect = autoReconnect;
		// Schedule keep-alive packet every 5 seconds to keep players in limbo
		BungeeCord.getInstance().getScheduler().schedule(autoReconnect, () -> {
			keepAliveUsers.entrySet().removeIf(entry -> {
				if (isUserOnline(entry.getValue())) {
					entry.getValue().unsafe().sendPacket(new KeepAlive(AutoReconnect.RANDOM.nextInt()));
					return false;
				}
				return true;
			});
		}, 5, 5, TimeUnit.SECONDS);
	}

	/**
	 * Checks if a UserConnection is still online.
	 *
	 * @param user
	 *            The User that should be checked.
	 * @return true, if the UserConnection is still online.
	 */
	public boolean isUserOnline(UserConnection user) {
		return autoReconnect.getProxy().getPlayer(user.getUniqueId()) != null;
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
		if (autoReconnect.getConfig().getIgnoredServers().contains(server.getInfo().getName()))
			return false;
		ServerReconnectEvent event = autoReconnect.getProxy().getPluginManager().callEvent(new ServerReconnectEvent(user, server.getInfo()));
		return !event.isCancelled();
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
		if (autoReconnect.getConfig().isDebugEnabled())
			autoReconnect.getLogger().info("Attempting to reconnect user " + user.getDisplayName() + " (" + user.getUUID() + ") to " + server.getInfo().getName() + " because of kick: " + kickMessage);
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
		if (autoReconnect.getConfig().isDebugEnabled())
			autoReconnect.getLogger().info("Reconnecting user " + user.getDisplayName() + " (" + user.getUUID() + ") to " + server.getInfo().getName() + " because of kick: " + kickMessage);
		if (autoReconnect.isProtocolizeLoaded() && autoReconnect.getConfig().getMoveToEmptyWorld()) {
			if (autoReconnect.getConfig().isDebugEnabled())
				autoReconnect.getLogger().info("Attempting to move to limbo");
			int version = UserVersionCache.getPlayerProtocolVersion(user.getUniqueId());
			if (version >= ProtocolConstants.MINECRAFT_1_16) {
				if (autoReconnect.getConfig().isDebugEnabled())
					autoReconnect.getLogger().info("Version 1.16 or greater");
				Object newDimension = LimboDimensionType.getLimboCurrentDimension(version);
				if (version >= ProtocolConstants.MINECRAFT_1_17) {
					if (autoReconnect.getConfig().isDebugEnabled())
						autoReconnect.getLogger().info("Version 1.17 or greater");
				} else if (version >= ProtocolConstants.MINECRAFT_1_16_2) {
					if (autoReconnect.getConfig().isDebugEnabled())
						autoReconnect.getLogger().info("Version 1.16.2-.5");
				} else {
					if (autoReconnect.getConfig().isDebugEnabled())
						autoReconnect.getLogger().info("Version 1.16/1.16.1");
				}
				short previousGamemode = (short) user.getGamemode();
				user.unsafe().sendPacket(new Login(user.getClientEntityId(), false, (short) 2,
						previousGamemode, new HashSet<String>(Arrays.asList(LimboDimensionType.DIMENSION_NAME)), LimboDimensionType.getLimboLoginRegistry(version), newDimension,
						LimboDimensionType.DIMENSION_NAME, 0, (short) 0, (short) 0, "", 10, false, false, false, false));
				user.setGamemode(2);
				user.getServerSentScoreboard().clear();
				for (UUID bossbar : user.getSentBossBars()) {
					user.unsafe().sendPacket(new BossBar(bossbar, 1));
				}
				user.getSentBossBars().clear();
				user.unsafe().sendPacket(new Respawn(newDimension, LimboDimensionType.DIMENSION_NAME, 0, (short) 0, (short) 2, previousGamemode, "", false, false, false));
			} else {
				if (autoReconnect.getConfig().isDebugEnabled())
					autoReconnect.getLogger().info("Version pre-1.16");
				Object newDim = (Integer) user.getDimension() <= 0 ? 1 : 0;
				user.unsafe().sendPacket(new Respawn(newDim, "", 0L, (short) 0, (short) 2, (short) 2, "default", false, false, false));
				user.setGamemode(2);
				user.setDimension(newDim);
				user.getServerSentScoreboard().clear();
				for (UUID bossbar : user.getSentBossBars()) {
					user.unsafe().sendPacket(new BossBar(bossbar, 1));
				}
				user.getSentBossBars().clear();
			}
			user.unsafe().sendPacket(PacketManager.getPositionLookPacket());
		}
		ReconnectTask reconnectTask = reconnectTasks.get(user.getUniqueId());
		if (reconnectTask == null) {
			reconnectTasks.put(user.getUniqueId(), reconnectTask = new ReconnectTask(autoReconnect, autoReconnect.getProxy(), user, server, kickMessage));
		}
		reconnectTask.startReconnect();
	}

	/**
	 * Removes a reconnect task from the main HashMap
	 *
	 * @param uuid
	 *            The UniqueId of the User.
	 */
	void cancelReconnectTask(UUID uuid) {
		if (autoReconnect.getConfig().isDebugEnabled())
			autoReconnect.getLogger().info("Cancelling reconnect task for " + uuid);
		ReconnectTask task = reconnectTasks.remove(uuid);
		if (task != null && autoReconnect.getProxy().getPlayer(uuid) != null) {
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
	 * Removes a user from the keep alive list
	 *
	 * @param uuid
	 *            The UniqueId of the User.
	 * @param user
	 *            The user instance
	 */
	void keepAlive(UUID uuid, UserConnection user) {
		if (autoReconnect.getConfig().isDebugEnabled())
			autoReconnect.getLogger().info("Keeping user connection alive " + user.getDisplayName() + " (" + uuid + ")");
		keepAliveUsers.put(uuid, user);
	}

	/**
	 * Removes a user from the keep alive list
	 *
	 * @param uuid
	 *            The UniqueId of the User.
	 */
	void cancelKeepAlive(UUID uuid) {
		if (autoReconnect.getConfig().isDebugEnabled())
			autoReconnect.getLogger().info("No longer keeping user connection alive " + uuid);
		keepAliveUsers.remove(uuid);
	}

	/**
	 * Checks whether a User connection is being kept alive.
	 *
	 * @param uuid
	 *            The UniqueId of the User.
	 * @return true, if the user connection is being kept alive.
	 */
	public boolean isKeptAlive(UUID uuid) {
		return keepAliveUsers.containsKey(uuid);
	}
	
	/**
	 * Stops all reconnecting tasks
	 */
	public void stop() {
		for (ReconnectTask task : reconnectTasks.values()) {
			task.cancel();
		}
	}

}
