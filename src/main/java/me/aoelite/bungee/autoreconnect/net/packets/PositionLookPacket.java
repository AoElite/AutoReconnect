package me.aoelite.bungee.autoreconnect.net.packets;

import java.util.HashMap;

import de.exceptionflug.protocolize.api.protocol.AbstractPacket;
import de.exceptionflug.protocolize.world.Location;
import io.netty.buffer.ByteBuf;
import java.util.Objects;
import net.md_5.bungee.protocol.ProtocolConstants;

public class PositionLookPacket extends AbstractPacket {
	
	public static final HashMap<Integer, Integer> MAPPING = new HashMap<Integer, Integer>();
	private double x;
	private double y;
	private double z;
	private float yaw;
	private float pitch;
	private byte flags;
	private int teleportId;

	public void read(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
		x = buf.readDouble();
		y = buf.readDouble();
		z = buf.readDouble();
		yaw = buf.readFloat();
		pitch = buf.readFloat();
		flags = buf.readByte();
		if (protocolVersion >= 79)
			teleportId = PositionLookPacket.readVarInt(buf);
	}

	public void write(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
		buf.writeDouble(x);
		buf.writeDouble(y);
		buf.writeDouble(z);
		buf.writeFloat(yaw);
		buf.writeFloat(pitch);
		buf.writeByte(flags);
		if (protocolVersion >= 79)
			PositionLookPacket.writeVarInt(teleportId, buf);
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

	public String toString() {
		return "PositionLookPacket(x=" + getX() + ", y=" + getY() + ", z=" + getZ() + ", yaw=" + getYaw() + ", pitch=" + getPitch() + ", flags=" + getFlags() + ", teleportId=" + getTeleportId() + ")";
	}
	
	public PositionLookPacket() {
		this.x = 0;
		this.y = 0;
		this.z = 0;
		this.yaw = 0;
		this.pitch = 0;
		this.flags = (byte) 0;
		this.teleportId = 0;
	}

	public PositionLookPacket(double x, double y, double z, float yaw, float pitch, byte flags, int teleportId) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.yaw = yaw;
		this.pitch = pitch;
		this.flags = flags;
		this.teleportId = teleportId;
	}

	public PositionLookPacket(Location loc, byte flags, int teleportId) {
		this.x = loc.getX();
		this.y = loc.getY();
		this.z = loc.getZ();
		this.yaw = loc.getYaw();
		this.pitch = loc.getPitch();
		this.flags = flags;
		this.teleportId = teleportId;
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
		return true;
	}

	protected boolean canEqual(Object other) {
		return other instanceof PositionLookPacket;
	}

	public int hashCode() {
		return Objects.hash(new Object[]{Double.valueOf(x), Double.valueOf(y), Double.valueOf(z), Float.valueOf(yaw), Float.valueOf(pitch), Byte.valueOf(flags), Integer.valueOf(teleportId)});
	}

	static {
		MAPPING.put(47, 0x08);
		MAPPING.put(107, 0x2e);
		MAPPING.put(108, 0x2e);
		MAPPING.put(109, 0x2e);
		MAPPING.put(110, 0x2e);
		MAPPING.put(210, 0x2e);
		MAPPING.put(315, 0x2e);
		MAPPING.put(316, 0x2e);
		MAPPING.put(335, 0x2e);
		MAPPING.put(338, 0x2f);
		MAPPING.put(340, 0x2f);
		MAPPING.put(393, 0x32);
		MAPPING.put(401, 0x32);
		MAPPING.put(404, 0x32);
		MAPPING.put(477, 0x35);
		MAPPING.put(480, 0x35);
		MAPPING.put(485, 0x35);
		MAPPING.put(490, 0x35);
		MAPPING.put(498, 0x35);
		MAPPING.put(573, 0x36);
		MAPPING.put(575, 0x36);
		MAPPING.put(578, 0x36);
		MAPPING.put(735, 0x35);
		MAPPING.put(736, 0x35);
		MAPPING.put(751, 0x34);
		MAPPING.put(753, 0x34);
		MAPPING.put(754, 0x34);
	}
}