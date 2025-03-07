package com.festp.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.festp.Logger;

public class UtilsReflection {
	
	public static void printAllMethods(Class<?> clazz) {
		Logger.severe(clazz.getCanonicalName() + " has " + clazz.getDeclaredMethods().length + " methods:");
		for (Method m : clazz.getDeclaredMethods()) {
			String parametersStr = "";
			for (Class<?> p : m.getParameterTypes()) {
				if (parametersStr.length() > 0)
					parametersStr += ", ";

				parametersStr += p.getCanonicalName();
			}
			Logger.severe("  " + m.getReturnType().getCanonicalName() + " " + m.getName() + "(" + parametersStr + ")");
		}
		if (clazz.getSuperclass() != null) {
			printAllMethods(clazz.getSuperclass());
		}
	}

	public static void printAllFields(Class<?> clazz) {
		Logger.severe(clazz.getCanonicalName() + " has " + clazz.getDeclaredFields().length + " fields:");
    	for (Field f : clazz.getDeclaredFields())
    	{
    		Logger.severe("  " + f.getType().getCanonicalName() + " " + f.getName());
    	}
    	if (clazz.getSuperclass() != null) {
    		printAllFields(clazz.getSuperclass());
        }
	}
}
