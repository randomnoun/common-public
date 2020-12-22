package com.randomnoun.common;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

/** Java hack to access windows registries. Relies on the sun implementation
 * of the Preferences class. 
 * 
 * <p>Technique stolen from
 * http://snipplr.com/view/6620/accessing-windows-registry-in-java/
 * 
 * <p>Probably better to JNI-wrap this, but hey, who has time for that.
 * 
 * 
 */

import java.lang.reflect.Method;
import java.util.prefs.Preferences;

public class Registry {
    
	// private static final int HKEY_CURRENT_USER = 0x80000001;
	// private static final int KEY_QUERY_VALUE = 1;
	// private static final int KEY_SET_VALUE = 2;
	private static final int KEY_READ = 0x20019;

	private static int initialisationState = 0;
	
	// java internals, bit nasty all of this being static
	private static final Preferences userRoot = Preferences.userRoot();
	private static final Preferences systemRoot = Preferences.systemRoot();
	private static final Class<?> clz = userRoot.getClass();
	private static Method winRegQueryValue;
	private static Method winRegEnumValue;
	private static Method winRegQueryInfo;	
	private static Method openKey;
	private static Method closeKey;
	
	
	// you'd better hope these methods are threadsafe
	public static String getSystemValue(String registryKey, String key) {
		initialise();
		
		byte[] valb = null;
		String vals = null;
		Integer handle = -1;

		// Query for IE version
		// key = "SOFTWARE\\Microsoft\\Internet Explorer#Version";
		try {
			handle = (Integer) openKey.invoke(systemRoot, toCstr(registryKey),
					KEY_READ, KEY_READ);
			valb = (byte[]) winRegQueryValue.invoke(systemRoot, handle,
					toCstr(key));
			vals = (valb != null ? new String(valb).trim() : null);
			closeKey.invoke(Preferences.systemRoot(), handle);
		} catch (Exception e) {
			if (handle!=-1) {
				try {
					closeKey.invoke(Preferences.systemRoot(), handle);
				} catch (Exception e2) {
					throw new IllegalArgumentException("Error reading registry '" + registryKey + "', key '" + key + "'; (and could not close handle " + handle + ": " + e2.getMessage() + ")", e);
				}
			}
			throw new IllegalArgumentException("Error reading registry key '" + registryKey + "', key '" + key + "'", e);
		}
		return vals;
	}
	
	public static String getUserValue(String registryKey, String key) {
		initialise();
		
		byte[] valb = null;
		String vals = null;
		Integer handle = -1;

		// Query Internet Settings for Proxy
		//key = "Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings#ProxyServer";
		try {
			handle = (Integer) openKey.invoke(userRoot, toCstr(registryKey), KEY_READ,
					KEY_READ);
			valb = (byte[]) winRegQueryValue.invoke(userRoot,
					handle.intValue(), toCstr(key));
			vals = (valb != null ? new String(valb).trim() : null);
			closeKey.invoke(Preferences.userRoot(), handle);
		} catch (Exception e) {
			if (handle!=-1) {
				try {
					closeKey.invoke(Preferences.systemRoot(), handle);
				} catch (Exception e2) {
					throw new IllegalArgumentException("Error reading registry '" + registryKey + "', key '" + key + "'; (and could not close handle " + handle + ": " + e2.getMessage() + ")", e);
				}
			}
			throw new IllegalArgumentException("Error reading registry key '" + registryKey + "', key '" + key + "'", e);
		}
		return vals;
	}
	
	private static synchronized void initialise() {
		if (initialisationState == 2) { 
			return; 
		} else if (initialisationState == 1) {
			throw new IllegalStateException("Registry could not be initialised");
		} else {
			try {
				openKey = clz.getDeclaredMethod("openKey", byte[].class, int.class, int.class);
				openKey.setAccessible(true);
	
				closeKey = clz.getDeclaredMethod("closeKey", int.class);
				closeKey.setAccessible(true);
	
				winRegQueryValue = clz.getDeclaredMethod("WindowsRegQueryValueEx", int.class, byte[].class);
				winRegQueryValue.setAccessible(true);
				winRegEnumValue = clz.getDeclaredMethod("WindowsRegEnumValue1", int.class, int.class, int.class);
				winRegEnumValue.setAccessible(true);
				winRegQueryInfo = clz.getDeclaredMethod("WindowsRegQueryInfoKey1", int.class);
				winRegQueryInfo.setAccessible(true);
			} catch (Exception e) {
				initialisationState = 1;
				throw new IllegalStateException("Cannot access registry", e);
			}
		}
	}
	
	
	private static byte[] toCstr(String str) {
		byte[] result = new byte[str.length() + 1];
		for (int i = 0; i < str.length(); i++) {
			result[i] = (byte) str.charAt(i);
		}
		result[str.length()] = 0;
		return result;
	}
}
