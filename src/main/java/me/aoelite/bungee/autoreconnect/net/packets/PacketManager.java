package me.aoelite.bungee.autoreconnect.net.packets;

import me.aoelite.bungee.autoreconnect.AutoReconnect;
import net.md_5.bungee.UserConnection;

/**
 * AutoReconnect packet manager class. Handles checking for Protocolize API and registering/sending custom packets if possible.
 */
public class PacketManager {

	private final boolean protocolizeLoaded;

	/**
	 * Constructs a new PacketManager instance with the given AutoReconnect plugin instance.
	 *
	 * @param instance AutoReconnect plugin instance
	 */
	public PacketManager(AutoReconnect instance) {
		boolean protocolizeLoaded = false;
		try {
			Class.forName("dev.simplix.protocolize.api.Protocolize");
			protocolizeLoaded = true;
			instance.getLogger().info("Protocolize is loaded. Registering packets.");
			dev.simplix.protocolize.api.Protocolize.protocolRegistration().registerPacket(PositionLookPacket.MAPPINGS, dev.simplix.protocolize.api.Protocol.PLAY, dev.simplix.protocolize.api.PacketDirection.CLIENTBOUND, PositionLookPacket.class);
		} catch (ClassNotFoundException e) {
			instance.getLogger().info("Protocolize not found.");
		}
		this.protocolizeLoaded = protocolizeLoaded;
	}

	/**
	 * Returns true if the Protocolize API was loaded at startup.
	 *
	 * @return true if Protocolize is loaded
	 */
	public boolean isProtocolizeLoaded() {
		return protocolizeLoaded;
	}

	/**
	 * Sends an empty Position and Look packet to the specified player.
	 *
	 * Moves them to 0, 0, 0 and facing 0, 0.
	 *
	 * @param player player to send packet to
	 */
	public void sendPositionLookPacket(UserConnection player) {
		if (protocolizeLoaded) {
			dev.simplix.protocolize.api.player.ProtocolizePlayer protocolizePlayer = dev.simplix.protocolize.api.Protocolize.playerProvider().player(player.getUniqueId());
			if (protocolizePlayer != null) {
				protocolizePlayer.sendPacket(new PositionLookPacket());
			}
		}
	}
}
