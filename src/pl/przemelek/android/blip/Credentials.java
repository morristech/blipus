package pl.przemelek.android.blip;

import android.content.SharedPreferences;
import pl.przemelek.android.util.Util;

public class Credentials {

	private SharedPreferences prefs;
	public Credentials(SharedPreferences prefs) {
		this.prefs = prefs;
	}
	
	public String getAuthorizationHeader() {		
		String user = prefs.getString("userName", "");
		String password = prefs.getString("password", "");		
		return "Basic "+Util.encodeToBase64(user+":"+password);
	}
	
}
