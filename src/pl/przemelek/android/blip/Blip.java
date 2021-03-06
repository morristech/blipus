package pl.przemelek.android.blip;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Blip {
	
	public static class BlipMsg {
		private int id;
		private String createdAt;
		private int transportId;
		private String transportName;
		private String body;
		private String type;
		private String userPath;
		private String picturesPath;
		private String recordingPath;
		private String moviePath;
		private String recipentPath;
		private boolean isStatus;
		private boolean isDirectMessage;
		private boolean isPrivateMessage;
		private boolean withPicture;
		private boolean withMovie;
		private boolean withRecording;
		private int dbId;
		private String json;
		public BlipMsg(int dbId, String json) throws JSONException {
			this(dbId, new JSONObject(json));			
		}
		public BlipMsg(int dbId, JSONObject jsonObj) throws JSONException {
			json = jsonObj.toString();
			this.type = getString(jsonObj,"type");
			this.id = getInt(jsonObj,"id");
			this.body = getString(jsonObj,"body");			
			this.createdAt = getString(jsonObj,"created_at");
			this.userPath = getString(jsonObj,"user_path");
			this.dbId = dbId;
			isStatus = "Status".equals(type);
			isDirectMessage = "DirectedMessage".equals(type);
			isPrivateMessage = "PrivateMessage".equals(type);
			if (isStatus || isDirectMessage || isPrivateMessage) {
				JSONObject transport = jsonObj.getJSONObject("transport");
				this.transportId = getInt(transport,"id");
				this.transportName = getString(transport,"name");
				
				this.picturesPath = getString(jsonObj,"pictures_path");
				this.recordingPath = getString(jsonObj,"recording_path");
				this.moviePath = getString(jsonObj,"movie_path");
				if (!"".equals(picturesPath)) {
					withPicture=true;
				}
				if (!"".equals(moviePath)) {
					withMovie=true;
				}
				if (!"".equals(recordingPath)) {
					withRecording=true;
				}					
				if (!isStatus) {
					this.recipentPath = getString(jsonObj,"recipient_path");
				}
			}				  	  
		}
		private String getString(JSONObject obj, String key) throws JSONException {
			String str = "";
			if (obj.has(key)) {
				str = obj.getString(key);
			}
			return str;
		}
		private int getInt(JSONObject obj, String key) throws JSONException {
			int value = 0;
			if (obj.has(key)) {
				value = obj.getInt(key);
			}
			return value;
		}
		
		public int getId() {
			return id;
		}
		public String getCreatedAt() {
			return createdAt;
		}
		public int getTransportId() {
			return transportId;
		}
		public String getTransportName() {
			return transportName;
		}
		public String getBody() {
			return body;
		}
		public String getType() {
			return type;
		}
		public String getUserPath() {
			return userPath;
		}
		public String getPicturesPath() {
			return picturesPath;
		}
		public String getRecordingPath() {
			return recordingPath;
		}
		public String getMoviePath() {
			return moviePath;
		}

		public String getMsgBody() {
  		  	String body = this.body;
  		  	return body;
		}

		public String getUsersString() {
			String user = getUserPath();
  		  	user = getUserFromUserPath(user);
  		  	String toUser = "";
  		  	if (isDirectMessage) {
  		  		toUser=">"+getUserFromUserPath(getRecipentPath());
  		  	} else if (isPrivateMessage) {
  		  		toUser=">>"+getUserFromUserPath(getRecipentPath());
  		  	}
  		  	String usersString = user+toUser;
			return usersString;
		}
		private String getUserFromUserPath(String user) {
			user = user.substring(user.lastIndexOf("/")+1);
			return user;
		}
		public String getRecipentPath() {
			return recipentPath;
		}
		public boolean isStatus() {
			return isStatus;
		}
		public boolean isDirectMessage() {
			return isDirectMessage;
		}
		public boolean isPrivateMessage() {
			return isPrivateMessage;
		}
		public boolean hasPicture() {
			return withPicture;
		}
		public boolean hasMovie() {
			return withMovie;
		}
		public boolean hasRecording() {
			return withRecording;
		}
		public String toJSONString() {
			return this.json;
		}
		public int getDbId() {
			return dbId;
		}
	}
	private Credentials credentials;
	
	public Blip(Credentials credentials) {
		this.credentials = credentials;
	}
	
	public Blip() {
		this.credentials = null;
	}
	
	public String sendBlip(String text) throws IOException {
		byte[] bytes = ("update[body]="+text).getBytes();
		HttpURLConnection connection = getConnection("http://api.blip.pl/updates");
		connection.setRequestMethod("POST");		
		connection.setRequestProperty("Content-Length", ""+bytes.length);
		connection.setDoOutput(true);
		connection.connect();		
		connection.getOutputStream().write(bytes);
		BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String line = null;
		String str = "";
		while ((line=br.readLine())!=null) {
		  str+=line+"\n";
		}  	  
		br.close();
		return str;
	}
	
	public String sendBlip(String text,byte[] buffer) throws IOException {
		byte[] bytes = ("update[body]="+text).getBytes();
		HttpURLConnection connection = getConnection("http://api.blip.pl/updates");
//		connection.setRequestMethod("POST");
	    String boundary = "AaB03x";
	    connection.setRequestProperty("Content-Type", "multipart/form-data; boundary="+boundary);
	    connection.setRequestMethod("POST");
//		connection.setRequestProperty("Content-Length", ""+(bytes.length);

		connection.setDoOutput(true);
		connection.connect();

		connection.getOutputStream().write(("--"+boundary+"\r\n").getBytes());
		connection.getOutputStream().write(("Content-Disposition: form-data; name=\"update[body]\"\r\n").getBytes());
		connection.getOutputStream().write(("Content-Type: text/plain\r\n").getBytes());
		connection.getOutputStream().write(("\r\n").getBytes());
		connection.getOutputStream().write((text+"\r\n").getBytes());
		
	    connection.getOutputStream().write(("--"+boundary+"\r\n").getBytes());
	    connection.getOutputStream().write(("Content-Disposition: form-data; name=\"update[picture]\"; filename=\"pictur.jpg\"\r\n").getBytes());
	    connection.getOutputStream().write(("Content-Type: image/jpeg\r\n").getBytes());
	    connection.getOutputStream().write(("\r\n").getBytes());
	    connection.getOutputStream().write(buffer);
	    connection.getOutputStream().write(("\r\n--"+boundary+"--\r\n").getBytes());
	    connection.getOutputStream().flush();
	    connection.getOutputStream().close();
		BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String line = null;
		String str = "";
		while ((line=br.readLine())!=null) {
		  str+=line+"\n";
		}  	  
		br.close();
		return str;
	}	
	
	public String deleteBlip(String id) throws IOException {		
		HttpURLConnection connection = getConnection("http://api.blip.pl/updates/"+id);
		connection.setRequestMethod("DELETE");
		connection.setDoOutput(true);
		connection.connect();
		BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String line = null;
		String str = "";
		while ((line=br.readLine())!=null) {
		  str+=line+"\n";
		}  	  
		br.close();
		return str;
	}	
	
	public List<BlipMsg> getBlips() throws MalformedURLException, IOException {
		return getBlips("");
	}
	
	public List<BlipMsg> getBlips(String condition) throws MalformedURLException, IOException {
		return getBlips("",condition);
	}
	
	public List<BlipMsg> getBlips(String userString, String condition) throws MalformedURLException, IOException {
		List<BlipMsg> blips = new ArrayList<BlipMsg>();
		String url = "http://api.blip.pl/"+userString+"dashboard";
		if (condition!=null) {
			url = "http://api.blip.pl/"+userString+"dashboard/since/"+condition;
		}
		HttpURLConnection connection = getConnection(url);
		connection.setRequestMethod("GET");
		connection.setDoInput(true);
		connection.connect();
		  String str = getStringFromConnection(connection);		
		  try {
			JSONArray jsonArray = new JSONArray(str);
			for (int idx=0; idx<jsonArray.length(); idx++) {
				BlipMsg blipMsg = new BlipMsg(-1,jsonArray.getJSONObject(idx));
				blips.add(blipMsg);
			}
		} catch (JSONException jsonEx) {
			
		}
		return blips;
	}

	public String getStringFromConnection(HttpURLConnection connection)
			throws IOException {
		byte[] buffer = getBytesFromConnection(connection);
		  String str=new String(buffer);
		return str;
	}

	public byte[] getBytesFromConnection(HttpURLConnection connection)
			throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		  InputStream is = connection.getInputStream();
		  byte[] buffer = new byte[1024];
		  int readCount;
	      while((readCount=is.read(buffer))>0) {
	        baos.write(buffer, 0, readCount);        
	      }
	      buffer = baos.toByteArray();
		return buffer;
	}

	public HttpURLConnection getConnection(String urlStr) throws MalformedURLException,
			IOException {
		URL url = new URL(urlStr);
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		connection.setRequestProperty("User-Agent", "Blipus 0.4");
		if (credentials!=null) {
			connection.setRequestProperty("Authorization", credentials.getAuthorizationHeader());
		}
		connection.setRequestProperty("X-Blip-API","0.02");
		connection.setRequestProperty("Accept","application/json");
		return connection;
	}
	
}
