package me.aoelite.bungee.autoreconnect;

import java.util.Collections;

import net.md_5.bungee.protocol.ProtocolConstants;
import se.llbit.nbt.ByteTag;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.DoubleTag;
import se.llbit.nbt.FloatTag;
import se.llbit.nbt.IntTag;
import se.llbit.nbt.ListTag;
import se.llbit.nbt.LongTag;
import se.llbit.nbt.NamedTag;
import se.llbit.nbt.StringTag;

public class LimboDimensionType {

	public static final String DIMENSION_NAME = "bungeecord:limbo";
	
	private static NamedTag LOGIN_REGISTRY_1_16 = null;
	
	private static NamedTag LOGIN_REGISTRY_1_16_2 = null;
	private static NamedTag CURRENT_DIMENSION_1_16_2 = null;
	
	private static NamedTag LOGIN_REGISTRY_1_17 = null;
	private static NamedTag CURRENT_DIMENSION_1_17 = null;
	
	private static NamedTag LOGIN_REGISTRY_1_18_2 = null;
	private static NamedTag CURRENT_DIMENSION_1_18_2 = null;
	
	private static NamedTag LOGIN_REGISTRY_1_19 = null;
	private static NamedTag CURRENT_DIMENSION_1_19 = null;
	
	public static NamedTag getLimboLoginRegistry(AutoReconnect plugin, int protocolVersion) {
		if (protocolVersion >= ProtocolConstants.MINECRAFT_1_19) {
			if (LOGIN_REGISTRY_1_19 == null) {
				CompoundTag ret = new CompoundTag();
				ret.add("minecraft:dimension_type", getLimboLoginRegistryDimensions(protocolVersion));
				ret.add("minecraft:worldgen/biome", getLimboLoginRegistryBiomes(plugin));
				ret.add("minecraft:chat_type", getLimboLoginChatType());
				LOGIN_REGISTRY_1_19 = new NamedTag("", ret);
			}
			return LOGIN_REGISTRY_1_19;
		} else if (protocolVersion >= ProtocolConstants.MINECRAFT_1_18_2) {
			if (LOGIN_REGISTRY_1_18_2 == null) {
				CompoundTag ret = new CompoundTag();
				ret.add("minecraft:dimension_type", getLimboLoginRegistryDimensions(protocolVersion));
				ret.add("minecraft:worldgen/biome", getLimboLoginRegistryBiomes(plugin));
				LOGIN_REGISTRY_1_18_2 = new NamedTag("", ret);
			}
			return LOGIN_REGISTRY_1_18_2;
		} else if (protocolVersion >= ProtocolConstants.MINECRAFT_1_17) {
			if (LOGIN_REGISTRY_1_17 == null) {
				CompoundTag ret = new CompoundTag();
				ret.add("minecraft:dimension_type", getLimboLoginRegistryDimensions(protocolVersion));
				ret.add("minecraft:worldgen/biome", getLimboLoginRegistryBiomes(plugin));
				LOGIN_REGISTRY_1_17 = new NamedTag("", ret);
			}
			return LOGIN_REGISTRY_1_17;
		} else if (protocolVersion >= ProtocolConstants.MINECRAFT_1_16_2) {
			if (LOGIN_REGISTRY_1_16_2 == null) {
				CompoundTag ret = new CompoundTag();
				ret.add("minecraft:dimension_type", getLimboLoginRegistryDimensions(protocolVersion));
				ret.add("minecraft:worldgen/biome", getLimboLoginRegistryBiomes(plugin));
				LOGIN_REGISTRY_1_16_2 = new NamedTag("", ret);
			}
			return LOGIN_REGISTRY_1_16_2;
		} else {
			if (LOGIN_REGISTRY_1_16 == null) {
				CompoundTag ret = new CompoundTag();
				ListTag list = new ListTag(CompoundTag.TAG_COMPOUND, Collections.emptyList());
				list.add(getLimboDimension_1_16());
				ret.add("dimension", list);
				LOGIN_REGISTRY_1_16 = new NamedTag("", ret);
			}
			return LOGIN_REGISTRY_1_16;
		}
	}

	public static Object getLimboCurrentDimension(AutoReconnect plugin, int protocolVersion) {
		if (protocolVersion >= ProtocolConstants.MINECRAFT_1_19) {
			if (CURRENT_DIMENSION_1_19 == null) {
				CURRENT_DIMENSION_1_19 = new NamedTag("", getLimboDimensionElement_1_19());
			}
			return CURRENT_DIMENSION_1_19;
		} else if (protocolVersion >= ProtocolConstants.MINECRAFT_1_18_2) {
			if (CURRENT_DIMENSION_1_18_2 == null) {
				CURRENT_DIMENSION_1_18_2 = new NamedTag("", getLimboDimensionElement_1_18_2());
			}
			return CURRENT_DIMENSION_1_18_2;
		} else if (protocolVersion >= ProtocolConstants.MINECRAFT_1_17) {
			if (CURRENT_DIMENSION_1_17 == null) {
				CURRENT_DIMENSION_1_17 = new NamedTag("", getLimboDimensionElement_1_17());
			}
			return CURRENT_DIMENSION_1_17;
		} else if (protocolVersion >= ProtocolConstants.MINECRAFT_1_16_2) {
			if (CURRENT_DIMENSION_1_16_2 == null) {
				CURRENT_DIMENSION_1_16_2 = new NamedTag("", getLimboDimensionElement_1_16_2());
			}
			return CURRENT_DIMENSION_1_16_2;
		} else {
			return DIMENSION_NAME;
		}
	}

	private static CompoundTag getLimboLoginRegistryDimensions(int protocolVersion) {
		CompoundTag ret = new CompoundTag();
		ListTag list = new ListTag(CompoundTag.TAG_COMPOUND, Collections.emptyList());
		list.add(getLimboDimension(protocolVersion));
		ret.add("type", new StringTag("minecraft:dimension_type"));
		ret.add("value", list);
		return ret;
	}

	private static CompoundTag getLimboDimension(int protocolVersion) {
		CompoundTag dimension = new CompoundTag();
		dimension.add("name", new StringTag(DIMENSION_NAME));
		dimension.add("id", new IntTag(0));
		if (protocolVersion >= ProtocolConstants.MINECRAFT_1_19) {
			dimension.add("element", getLimboDimensionElement_1_19());
		} else if (protocolVersion >= ProtocolConstants.MINECRAFT_1_18_2) {
			dimension.add("element", getLimboDimensionElement_1_18_2());
		} else if (protocolVersion >= ProtocolConstants.MINECRAFT_1_17) {
			dimension.add("element", getLimboDimensionElement_1_17());
		} else {
			dimension.add("element", getLimboDimensionElement_1_16_2());
		}
		return dimension;
	}
	
	private static CompoundTag getLimboDimensionElement_1_19() {
		CompoundTag element = new CompoundTag();
		element.add("piglin_safe", new ByteTag(0));
		element.add("has_raids", new ByteTag(0));
		element.add("monster_spawn_light_level", new IntTag(0));
		element.add("monster_spawn_block_light_limit", new IntTag(0));
		element.add("natural", new ByteTag(0));
		element.add("ambient_light", new FloatTag(0));
		element.add("fixed_time", new LongTag(18000));
		element.add("infiniburn", new StringTag("#minecraft:infiniburn_overworld"));
		element.add("respawn_anchor_works", new ByteTag(0));
		element.add("has_skylight", new ByteTag(0));
		element.add("bed_works", new ByteTag(0));
		element.add("effects", new StringTag("minecraft:the_end"));
		element.add("min_y", new IntTag(0));
		element.add("height", new IntTag(256));
		element.add("logical_height", new IntTag(256));
		element.add("coordinate_scale", new DoubleTag(1.0));
		element.add("ultrawarm", new ByteTag(0));
		element.add("has_ceiling", new ByteTag(0));
		return element;
	}
	
	private static CompoundTag getLimboDimensionElement_1_18_2() {
		CompoundTag element = new CompoundTag();
		element.add("natural", new ByteTag(0));
		element.add("ambient_light", new FloatTag(0));
		element.add("ultrawarm", new ByteTag(0));
		element.add("has_ceiling", new ByteTag(0));
		element.add("has_skylight", new ByteTag(0));
		element.add("piglin_safe", new ByteTag(0));
		element.add("bed_works", new ByteTag(0));
		element.add("respawn_anchor_works", new ByteTag(0));
		element.add("has_raids", new ByteTag(0));
		element.add("logical_height", new IntTag(256));
		element.add("infiniburn", new StringTag("#minecraft:infiniburn_overworld"));
		element.add("min_y", new IntTag(0));
		element.add("height", new IntTag(256));
		element.add("coordinate_scale", new DoubleTag(1.0));
		element.add("effects", new StringTag("minecraft:the_end"));
		return element;
	}
	
	private static CompoundTag getLimboDimensionElement_1_17() {
		CompoundTag element = new CompoundTag();
		element.add("natural", new ByteTag(0));
		element.add("ambient_light", new FloatTag(0));
		element.add("ultrawarm", new ByteTag(0));
		element.add("has_ceiling", new ByteTag(0));
		element.add("has_skylight", new ByteTag(0));
		element.add("piglin_safe", new ByteTag(0));
		element.add("bed_works", new ByteTag(0));
		element.add("respawn_anchor_works", new ByteTag(0));
		element.add("has_raids", new ByteTag(0));
		element.add("logical_height", new IntTag(256));
		element.add("infiniburn", new StringTag("minecraft:infiniburn_overworld"));
		element.add("min_y", new IntTag(0));
		element.add("height", new IntTag(256));
		element.add("coordinate_scale", new DoubleTag(1.0));
		element.add("effects", new StringTag("minecraft:the_end"));
		return element;
	}
	
	private static CompoundTag getLimboDimensionElement_1_16_2() {
		CompoundTag element = new CompoundTag();
		element.add("natural", new ByteTag(0));
		element.add("ambient_light", new FloatTag(0));
		element.add("shrunk", new ByteTag(0));
		element.add("ultrawarm", new ByteTag(0));
		element.add("has_ceiling", new ByteTag(0));
		element.add("has_skylight", new ByteTag(0));
		element.add("piglin_safe", new ByteTag(0));
		element.add("bed_works", new ByteTag(0));
		element.add("respawn_anchor_works", new ByteTag(0));
		element.add("has_raids", new ByteTag(0));
		element.add("logical_height", new IntTag(256));
		element.add("infiniburn", new StringTag("minecraft:infiniburn_overworld"));
		element.add("fixed_time", new LongTag(18000));
		element.add("coordinate_scale", new DoubleTag(1.0));
		element.add("effects", new StringTag("minecraft:the_end"));
		return element;
	}
	
	private static CompoundTag getLimboDimension_1_16() {
		CompoundTag element = new CompoundTag();
		element.add("natural", new ByteTag(0));
		element.add("ambient_light", new FloatTag(0));
		element.add("shrunk", new ByteTag(0));
		element.add("ultrawarm", new ByteTag(0));
		element.add("has_ceiling", new ByteTag(0));
		element.add("has_skylight", new ByteTag(0));
		element.add("piglin_safe", new ByteTag(0));
		element.add("bed_works", new ByteTag(0));
		element.add("respawn_anchor_works", new ByteTag(0));
		element.add("has_raids", new ByteTag(0));
		element.add("logical_height", new IntTag(256));
		element.add("infiniburn", new StringTag("minecraft:infiniburn_overworld"));
		element.add("fixed_time", new LongTag(18000));
		element.add("name", new StringTag(DIMENSION_NAME));
		return element;
	}

	private static CompoundTag getLimboLoginRegistryBiomes(AutoReconnect plugin) {
		CompoundTag ret = new CompoundTag();
		ListTag list = new ListTag(CompoundTag.TAG_COMPOUND, Collections.emptyList());
		list.add(getPlainsBiome(plugin));
		ret.add("type", new StringTag("minecraft:worldgen/biome"));
		ret.add("value", list);
		return ret;
	}

	private static CompoundTag getPlainsBiome(AutoReconnect plugin) {
		CompoundTag biome = new CompoundTag();
		biome.add("name", new StringTag("minecraft:plains"));
		biome.add("id", new IntTag(1));
		
		CompoundTag element = new CompoundTag();
		element.add("precipitation", new StringTag("rain"));
		
		CompoundTag effects = new CompoundTag();
		effects.add("sky_color", new IntTag(7907327));
		effects.add("water_fog_color", new IntTag(329011));
		effects.add("fog_color", new IntTag(12638463));
		effects.add("water_color", new IntTag(4159204));
		
		CompoundTag music = new CompoundTag();
		music.add("replace_current_music", new ByteTag(1));
		music.add("sound", new StringTag(plugin.getConfig().getReconnectingMusic()));
		music.add("max_delay", new IntTag(1));
		music.add("min_delay", new IntTag(0));
		effects.add("music", master);
		
		element.add("effects", effects);
		
		element.add("depth", new FloatTag(0.125f));
		element.add("temperature", new FloatTag(0.8f));
		element.add("scale", new FloatTag(0.05f));
		element.add("downfall", new FloatTag(0.4f));
		element.add("category", new StringTag("plains"));
		
		biome.add("element", element);
		return biome;
	}

	private static CompoundTag getLimboLoginChatType() {
		CompoundTag ret = new CompoundTag();
		ListTag list = new ListTag(CompoundTag.TAG_COMPOUND, Collections.emptyList());
		list.add(getChatSystem());
		list.add(getChatGameInfo());
		ret.add("type", new StringTag("minecraft:chat_type"));
		ret.add("value", list);
		return ret;
	}

	private static CompoundTag getChatSystem() {
		CompoundTag chat = new CompoundTag();
		chat.add("name", new StringTag("minecraft:system"));
		chat.add("id", new IntTag(0));
		
		CompoundTag element = new CompoundTag();
		element.add("chat", new CompoundTag());
		CompoundTag narration = new CompoundTag();
		narration.add("priority", new StringTag("system"));
		element.add("narration", narration);
		
		chat.add("element", element);
		return chat;
	}

	private static CompoundTag getChatGameInfo() {
		CompoundTag chat = new CompoundTag();
		chat.add("name", new StringTag("minecraft:game_info"));
		chat.add("id", new IntTag(1));
		
		CompoundTag element = new CompoundTag();
		element.add("overlay", new CompoundTag());
		
		chat.add("element", element);
		return chat;
	}

}
