package com.festp;

public class Logger {
	private static java.util.logging.Logger wrapped = null;
	
	public static void setLogger(java.util.logging.Logger logger) {
		if (wrapped == null)
			wrapped = logger;
	}
	
	public static void info(String msg) {
		wrapped.info(msg);
	}
	
	public static void warning(String msg) {
		wrapped.warning(msg);
	}
	
	public static void severe(String msg) {
		wrapped.severe(msg);
	}

	public static void printStackTracePeak(Exception e, int n) {
		String error = "";
		StackTraceElement[] elems = e.getStackTrace();
		for (int i = 0; i < elems.length && i < n; i++) {
			error += elems[i].toString() + "\n";
		}
		severe(error);
	}
}
