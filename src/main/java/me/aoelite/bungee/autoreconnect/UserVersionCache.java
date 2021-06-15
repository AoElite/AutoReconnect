package me.aoelite.bungee.autoreconnect;

import java.util.HashMap;
import java.util.UUID;

import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class UserVersionCache implements Listener {

	private static HashMap<UUID, Integer> versionMap = new HashMap<>();
	
	@EventHandler
	public void onLogin(LoginEvent e) {
		versionMap.put(e.getConnection().getUniqueId(), e.getConnection().getVersion());
	}
	
	@EventHandler
	public void onPlayerDisconnect(PlayerDisconnectEvent e) {
		versionMap.remove(e.getPlayer().getUniqueId());
	}
	
	public static int getPlayerProtocolVersion(UUID uuid) {
		return versionMap.get(uuid);
	}
	
}
