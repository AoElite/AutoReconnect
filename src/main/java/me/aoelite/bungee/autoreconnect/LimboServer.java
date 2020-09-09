package me.aoelite.bungee.autoreconnect;

import com.google.common.base.Preconditions;

import net.md_5.bungee.ServerConnection;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.protocol.DefinedPacket;

public class LimboServer extends ServerConnection {
	
	private final Connection.Unsafe unsafe = new Connection.Unsafe() {
		public void sendPacket(DefinedPacket packet) {
			return;
		}
	};

	public LimboServer(AutoReconnect instance) {
		super(null, new LimboServerInfo(instance));
	}

	public void disconnect(String reason) {
		this.disconnect(new BaseComponent[0]);
	}

	public void disconnect(BaseComponent... reason) {
		Preconditions.checkArgument((boolean) (reason.length == 0), (Object) "Server cannot have disconnect reason");
	}

	public void disconnect(BaseComponent reason) {
		this.disconnect(new BaseComponent[0]);
	}

	public boolean isConnected() {
		return true;
	}

	@Override
	public Connection.Unsafe unsafe() {
		return this.unsafe;
	}

}
