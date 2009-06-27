package pl.przemelek.android.db;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.OpenableColumns;

import pl.przemelek.android.blip.Blip.BlipMsg;

public class StatusesManager {	
	private static final String DATABASE_NAME = "blipus.db";
	private static final String DATABASE_TABLE = "statuses";
	private static final String DATABASE_CREATE = 
	  "create table " + DATABASE_TABLE + 
	  " ( _id integer primary key autoincrement," +
	  "id integer not null," +
	  "createdAt text not null, " +
	  "body text not null);";
	private Context context;
	private SQLiteDatabase myDatabase;
	
	public StatusesManager(Context context) {
		this.context = context;
		createDatabase();
	}
	
	private void createDatabase() {
	  
	  myDatabase = context.openOrCreateDatabase(DATABASE_NAME, 
	                                    Context.MODE_PRIVATE, null);
	  try {
		  myDatabase.execSQL(DATABASE_CREATE);
	  } catch (Exception e) {
		  e.printStackTrace();
	  }
	}
	
	public void create(BlipMsg msg) {
		ContentValues val = new ContentValues();
		val.put("body",msg.toJSONString());
		val.put("id",msg.getId());
		val.put("createdAt",msg.getCreatedAt());
		myDatabase.insert(DATABASE_TABLE, null, val);
	}
	
	public BlipMsg get(int idx) {
		Cursor cursor = myDatabase.query(DATABASE_TABLE, null, "id = "+idx,null,null,null,"id");		
		BlipMsg msg = null;
		if (cursor.moveToFirst()) {
			try {
				String body = cursor.getString(3);
				msg = new BlipMsg(cursor.getInt(0), body);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		cursor.deactivate();
		return msg;
	}
	
	public List<BlipMsg> getList(String condition,String limit) {
		return getList(condition, limit, true);
	}

	public List<BlipMsg> getList(String condition, String limit,
			boolean startingFromFirst) {
		Cursor cursor = myDatabase.query(DATABASE_TABLE, null, condition,null,null,null,"id");
		List<BlipMsg> list = new ArrayList<BlipMsg>();
		boolean dataAvailable = false;
		if (startingFromFirst) {
			dataAvailable = cursor.moveToFirst();
		} else {
			dataAvailable = cursor.moveToLast();
		}
		boolean hasNext = false;
		if (dataAvailable) {
			int idx = cursor.getPosition();
			do {
				BlipMsg msg = null;
				try {
					String body = cursor.getString(3);
					msg = new BlipMsg(cursor.getInt(0),body);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				if (msg!=null) {
					list.add(msg);
				}				
				if (startingFromFirst) {
					hasNext = cursor.moveToNext();
				} else {
					hasNext = cursor.moveToPrevious();
				}				
			} while (hasNext);
		}
		cursor.deactivate();
		return list;
	}
	
	public int getMaxID() {
		List<BlipMsg> list = getList("","1",false);
		if (list.size()<1) return -1;
		return list.get(0).getId();
	}
	
	public void delete(int idx) {
		 myDatabase.delete(DATABASE_TABLE, "_id=" + idx, null);
	}
	
	public void delete(BlipMsg msg) {
		delete(msg.getDbId());
	}
}
