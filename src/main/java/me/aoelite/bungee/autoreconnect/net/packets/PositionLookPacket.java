package me.aoelite.bungee.autoreconnect.net.packets;

import dev.simplix.protocolize.api.PacketDirection;
import dev.simplix.protocolize.api.mapping.AbstractProtocolMapping;
import dev.simplix.protocolize.api.mapping.ProtocolIdMapping;
import dev.simplix.protocolize.api.packet.AbstractPacket;
import dev.simplix.protocolize.api.util.ProtocolUtil;
import io.netty.buffer.ByteBuf;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static dev.simplix.protocolize.api.util.ProtocolVersions.*;

public class PositionLookPacket extends AbstractPacket {

	public final static List<ProtocolIdMapping> MAPPINGS = Arrays.asList(
			AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_8, MINECRAFT_1_8, 0x08),
			AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_9, MINECRAFT_1_12, 0x2e),
			AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_12_1, MINECRAFT_1_12_2, 0x2f),
			AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_13, MINECRAFT_1_13_2, 0x32),
			AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_14, MINECRAFT_1_14_4, 0x35),
			AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_15, MINECRAFT_1_15_2, 0x36),
			AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_16, MINECRAFT_1_16_1, 0x35),
			AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_16_2, MINECRAFT_1_16_5, 0x34),
			AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_17, MINECRAFT_1_18_2, 0x38),
			AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_19, MINECRAFT_1_19, 0x36),
			AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_19_1, MINECRAFT_LATEST, 0x39)
	);

	private double x;
	private double y;
	private double z;
	private float yaw;
	private float pitch;
	private byte flags;
	private int teleportId;
	private boolean dismountVehicle;

	public void read(ByteBuf buf, PacketDirection packetDirection, int protocolVersion) {
		x = buf.readDouble();
		y = buf.readDouble();
		z = buf.readDouble();
		yaw = buf.readFloat();
		pitch = buf.readFloat();
		flags = buf.readByte();
		if (protocolVersion >= MINECRAFT_1_9)
			teleportId = ProtocolUtil.readVarInt(buf);
		if (protocolVersion >= MINECRAFT_1_17)
			dismountVehicle = buf.readBoolean();
	}

	public void write(ByteBuf buf, PacketDirection packetDirection, int protocolVersion) {
		buf.writeDouble(x);
		buf.writeDouble(y);
		buf.writeDouble(z);
		buf.writeFloat(yaw);
		buf.writeFloat(pitch);
		buf.writeByte(flags);
		if (protocolVersion >= MINECRAFT_1_9)
			ProtocolUtil.writeVarInt(buf, teleportId);
		if (protocolVersion >= MINECRAFT_1_17)
			buf.writeBoolean(dismountVehicle);
	}

	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
	}

	public double getZ() {
		return z;
	}

	public void setZ(double z) {
		this.z = z;
	}

	public float getYaw() {
		return yaw;
	}

	public void setYaw(float yaw) {
		this.yaw = yaw;
	}

	public float getPitch() {
		return pitch;
	}

	public void setPitch(float pitch) {
		this.pitch = pitch;
	}

	public byte getFlags() {
		return flags;
	}

	public void setFlags(byte flags) {
		this.flags = flags;
	}

	public int getTeleportId() {
		return teleportId;
	}

	public void setTeleportId(int teleportId) {
		this.teleportId = teleportId;
	}
	
	public boolean getDismountVehicle() {
		return dismountVehicle;
	}
	
	public void setDismountVehicle(boolean dismountVehicle) {
		this.dismountVehicle = dismountVehicle;
	}

	public String toString() {
		return "PositionLookPacket(x=" + getX() + ", y=" + getY() + ", z=" + getZ() + ", yaw=" + getYaw() + ", pitch=" + getPitch() + ", flags=" + getFlags() + ", teleportId=" + getTeleportId() + ", dismountVehicle=" + getDismountVehicle() + ")";
	}
	
	public PositionLookPacket() {
		this.x = 0;
		this.y = 0;
		this.z = 0;
		this.yaw = 0;
		this.pitch = 0;
		this.flags = (byte) 0;
		this.teleportId = 0;
		this.dismountVehicle = false;
	}

	public PositionLookPacket(double x, double y, double z, float yaw, float pitch, byte flags, int teleportId, boolean dismountVehicle) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.yaw = yaw;
		this.pitch = pitch;
		this.flags = flags;
		this.teleportId = teleportId;
		this.dismountVehicle = dismountVehicle;
	}

	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof PositionLookPacket)) {
			return false;
		}
		PositionLookPacket other = (PositionLookPacket) ((Object) o);
		if (!other.canEqual((Object) this)) {
			return false;
		}
		if (this.getX() != other.getX()) {
			return false;
		}
		if (this.getY() != other.getY()) {
			return false;
		}
		if (this.getZ() != other.getZ()) {
			return false;
		}
		if (this.getYaw() != other.getYaw()) {
			return false;
		}
		if (this.getPitch() != other.getPitch()) {
			return false;
		}
		if (this.getFlags() != other.getFlags()) {
			return false;
		}
		if (this.getTeleportId() != other.getTeleportId()) {
			return false;
		}
		if (this.getDismountVehicle() != other.getDismountVehicle()) {
			return false;
		}
		return true;
	}

	protected boolean canEqual(Object other) {
		return other instanceof PositionLookPacket;
	}

	public int hashCode() {
		return Objects.hash(new Object[]{Double.valueOf(x), Double.valueOf(y), Double.valueOf(z), Float.valueOf(yaw), Float.valueOf(pitch), Byte.valueOf(flags), Integer.valueOf(teleportId), Boolean.valueOf(dismountVehicle)});
	}
}