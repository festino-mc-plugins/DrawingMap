package com.festp.utils;

import org.bukkit.map.MapCursor.Type;

@SuppressWarnings("deprecation")
public class MapCursors {
	// MapCursor.Type was an enum before 1.21
	public static final Type WHITE_POINTER = UtilsVersion.USE_NEW_CURSORS ? Type.PLAYER : Type.valueOf("WHITE_POINTER");
	public static final Type WHITE_CIRCLE = UtilsVersion.USE_NEW_CURSORS ? Type.PLAYER_OFF_MAP : Type.valueOf("WHITE_CIRCLE");
	public static final Type SMALL_WHITE_CIRCLE = UtilsVersion.USE_NEW_CURSORS ? Type.PLAYER_OFF_LIMITS : Type.valueOf("SMALL_WHITE_CIRCLE");
	public static final Type RED_POINTER = UtilsVersion.USE_NEW_CURSORS ? Type.RED_MARKER : Type.valueOf("RED_POINTER");
}
