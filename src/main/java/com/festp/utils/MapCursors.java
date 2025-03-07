package com.festp.utils;

import org.bukkit.map.MapCursor.Type;

public class MapCursors {
	public static final Type WHITE_POINTER = UtilsVersion.USE_NEW_CURSORS ? Type.PLAYER : getEnum("WHITE_POINTER");
	public static final Type WHITE_CIRCLE = UtilsVersion.USE_NEW_CURSORS ? Type.PLAYER_OFF_MAP : getEnum("WHITE_CIRCLE");
	public static final Type SMALL_WHITE_CIRCLE = UtilsVersion.USE_NEW_CURSORS ? Type.PLAYER_OFF_LIMITS : getEnum("SMALL_WHITE_CIRCLE");
	public static final Type RED_POINTER = UtilsVersion.USE_NEW_CURSORS ? Type.RED_MARKER : getEnum("RED_POINTER");
	
	private static Type getEnum(String name) {
		// MapCursor.Type was an enum before 1.21
		try {
			// Type.valueOf and even Type.class.getEnumConstants()[0].toString() throws IncompatibleClassChangeError
			return (Type) Type.class.getDeclaredMethod("valueOf", String.class).invoke(null, name);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
