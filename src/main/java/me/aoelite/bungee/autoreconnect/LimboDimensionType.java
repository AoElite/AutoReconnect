package me.aoelite.bungee.autoreconnect;

import java.util.Collections;

import se.llbit.nbt.ByteTag;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.FloatTag;
import se.llbit.nbt.IntTag;
import se.llbit.nbt.ListTag;
import se.llbit.nbt.LongTag;
import se.llbit.nbt.NamedTag;
import se.llbit.nbt.StringTag;

public class LimboDimensionType {
	
	public static final String DIMENSION_NAME = "bungeecord:limbo";

	public static NamedTag getLimboDimensionList() {
		CompoundTag ret = new CompoundTag();
		ListTag list = new ListTag(CompoundTag.TAG_COMPOUND, Collections.emptyList());
		list.add(getLimbo());
		ret.add("dimension", list);
		return new NamedTag("", ret);
	}

	public static CompoundTag getLimbo() {
		CompoundTag ret = new CompoundTag();
		ret.add("name", new StringTag(DIMENSION_NAME));
		ret.add("natural", new ByteTag(0));
		ret.add("ambient_light", new FloatTag(0));
		ret.add("shrunk", new ByteTag(0));
		ret.add("ultrawarm", new ByteTag(0));
		ret.add("has_ceiling", new ByteTag(0));
		ret.add("has_skylight", new ByteTag(0));
		ret.add("piglin_safe", new ByteTag(0));
		ret.add("bed_works", new ByteTag(0));
		ret.add("respawn_anchor_works", new ByteTag(0));
		ret.add("has_raids", new ByteTag(0));
		ret.add("logical_height", new IntTag(256));
		ret.add("infiniburn", new StringTag("minecraft:infiniburn_overworld"));
		ret.add("fixed_time", new LongTag(6000));
		return ret;
	}

}
