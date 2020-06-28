package me.aoelite.bungee.autoreconnect.net.packets;

import de.exceptionflug.protocolize.api.protocol.ProtocolAPI;
import me.aoelite.bungee.autoreconnect.AutoReconnect;

public class PacketManager {
	
	public static void register(AutoReconnect instance) {
		ProtocolAPI.getPacketRegistration().registerPlayClientPacket(PositionLookPacket.class, PositionLookPacket.MAPPING);
	}

}
