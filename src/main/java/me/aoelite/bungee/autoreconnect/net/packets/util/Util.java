package me.aoelite.bungee.autoreconnect.net.packets.util;

import java.lang.reflect.Field;

import net.md_5.bungee.UserConnection;
import net.md_5.bungee.netty.ChannelWrapper;

public class Util {
	
	private static Field userChannelWrapperField = null;
	
	public static ChannelWrapper getUserChannelWrapper(UserConnection user) {
		if (user != null) {
			try {
				return (ChannelWrapper) userChannelWrapperField.get(user);
			} catch (ClassCastException | IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	static {
		for (Field f : UserConnection.class.getDeclaredFields()) {
			if (ChannelWrapper.class.isAssignableFrom(f.getType())) {
				userChannelWrapperField = f;
				userChannelWrapperField.setAccessible(true);
				break;
			}
		}
	}

}
