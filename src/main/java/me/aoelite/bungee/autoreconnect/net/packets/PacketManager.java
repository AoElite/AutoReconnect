package me.aoelite.bungee.autoreconnect.net.packets;

import de.exceptionflug.protocolize.api.protocol.ProtocolAPI;
import me.aoelite.bungee.autoreconnect.AutoReconnect;
import net.md_5.bungee.protocol.DefinedPacket;

public class PacketManager {
	
	public static void register(AutoReconnect instance) {
		ProtocolAPI.getPacketRegistration().registerPlayClientPacket(PositionLookPacket.class, PositionLookPacket.MAPPING);
	}
	
	public static DefinedPacket getPositionLookPacket() {
		return new PositionLookPacket();
	}

}
