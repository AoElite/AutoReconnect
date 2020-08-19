package me.aoelite.bungee.autoreconnect;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import net.md_5.bungee.BungeeServerInfo;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class LimboServerInfo extends BungeeServerInfo {

	public LimboServerInfo(AutoReconnect instance) {
		super(instance.getConfig().getLimboServerName(), new InetSocketAddress("0.0.0.0", 0), "", false);
	}
	
	@Override
	public void addPlayer(ProxiedPlayer player) {
		return;
	}
	
	@Override
	public void removePlayer(ProxiedPlayer player) {
		return;
	}
	
	@Override
	public Collection<ProxiedPlayer> getPlayers() {
		return new ArrayList<ProxiedPlayer>();
	}
	
	@Override
	public boolean canAccess(CommandSender player) {
		return false;
	}
	
	@Override
	public int hashCode() {
		return 0;
	}
	
	@Override
	public void sendData(String channel, byte[] data) {
		return;
	}
	
	@Override
	public boolean sendData(String channel, byte[] data, boolean queue) {
		return true;
	}
	
	@Override
	public void cachePing(ServerPing serverPing) {
		return;
	}
	
	@Override
	public void ping(Callback<ServerPing> callback) {
		return;
	}
	
	@Override
	public void ping(final Callback<ServerPing> callback, final int protocolVersion) {
		return;
	}

}
