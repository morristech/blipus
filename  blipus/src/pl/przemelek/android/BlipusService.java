package pl.przemelek.android;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import pl.przemelek.android.blip.Blip;
import pl.przemelek.android.blip.Credentials;
import pl.przemelek.android.blip.Blip.BlipMsg;
import pl.przemelek.android.db.StatusesManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class BlipusService extends Service {
	private Timer updateTimer;
	private Blip blip;
	private StatusesManager manager;
	private int lastId;
	private boolean duringRefresh;
	
	public void onStart(Intent intent, int startId) {		
		manager = new StatusesManager(getApplicationContext());
		if (blip==null) {
			blip = new Blip(new Credentials(getSharedPreferences("CREDENTIALS", Context.MODE_PRIVATE)));
		}
		Log.i("BlipusService", "Will cancel old timer");
		updateTimer.purge();
		updateTimer.cancel();
		updateTimer = new Timer("blipusTimer");
		TimerTask doRefresh = new TimerTask() {
			public void run() {
				refreshListOfBlips();
			}
		};		
		updateTimer.scheduleAtFixedRate(doRefresh, 0, 10*1000);
	}
	
	
	@Override
	public void onCreate() {
		updateTimer = new Timer("blipusTimer");
		Log.i("BlipusService", "new timer created in onCreate");
	}
	
	private void refreshListOfBlips() {
		if (duringRefresh) return;
		new Thread(new Runnable() {
			public void run() {
				if (duringRefresh) return;      	  
				duringRefresh = true;
				try {
					String condition = null;
					lastId = manager.getMaxID();
					if (lastId!=-1) {
						condition = ""+lastId;
					}
		        	final List<BlipMsg> blips = blip.getBlips(condition);
		        	final List<BlipMsg> newList = new ArrayList<BlipMsg>();
		        	boolean redraw = false;
		        	if (blips.size()>0) {
		        		for (BlipMsg msg:blips) {
		        			manager.create(msg);
		        		}		        	
				        NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);			        
				        Notification notification = new Notification(R.drawable.icon,"New blips!!!",System.currentTimeMillis());
	//			        notification.flags=Notification.FLAG_SHOW_LIGHTS;
	//			        notification.ledOffMS=100;
	//			        notification.ledOnMS=250;
				        
				        Context context = getApplicationContext();
				        String expandedText = "You have "+blips.size()+" new blips";
				        String expandedTitle = "New Blips are here! :-)";
				        Intent intent = new Intent(context, Blipus.class);
				        PendingIntent launchIntent = PendingIntent.getActivity(context, 0, intent, 0);
				        notification.setLatestEventInfo(context,
				    		 expandedTitle,
				    		 expandedText,
				    		 launchIntent);
				        
				        nm.notify(1, notification);
		        	}
		        } catch (final Exception e) {		        	
		        	e.printStackTrace();	            	  
		        } finally {
		        	duringRefresh=false;
		        }		        
			}
		}).start();
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}
