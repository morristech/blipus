package pl.przemelek.android;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import pl.przemelek.android.blip.Blip;
import pl.przemelek.android.blip.Credentials;
import pl.przemelek.android.blip.Blip.BlipMsg;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class Blipus extends Activity {
    private static final int MENU_SETTINGS = 1;
    private static final int MENU_REFRESH = 2;
    private static final int MENU_EXIT = 3;
	private static final int MENU_DELETE = 1;
//	private static final int MENU_LINK = 2;
	private static final int MENU_QUOTE = 3;
	private static final int MENU_REPLY = 4;
	private static final int MENU_REPLY_QUOTE = 6;
	private static final int MENU_PRIV_REPLY = 5;
	private static final int MENU_PRIV_REPLY_QUOTE = 7;
	private static final int MENU_LINKS = 100;
	private static final int BLIP_LIMIT = 160;
	private static String currentUserName;
	private boolean duringRefresh = false;
	private int lastId = -1;
	private LinkedHashSet<BlipMsg> allBlips = new LinkedHashSet<BlipMsg>();
	private ListView list;
	private Blip blip;
	/** Called when the activity is first created. */ 
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
                
    }

    @Override
    protected void onResume() {
    	super.onResume();
//    	Log.i
//    	requestWindowFeature(Window.FEATURE_NO_TITLE);
        SharedPreferences prefs = getSharedPreferences("CREDENTIALS", Context.MODE_PRIVATE);
        String userName = prefs.getString("userName", null);
        
        if (userName==null) {
        	startActivity(new Intent(this,SettingsActivity.class));
        }
        currentUserName = userName;
        Button button = (Button)findViewById(R.id.Button01);
        final EditText editor = (EditText)findViewById(R.id.Edit01);
        final TextView textLengthInfo = (TextView)findViewById(R.id.textSize);
        list = (ListView)findViewById(R.id.list);
        list.setAdapter(new ArrayAdapter<BlipMsg>(this,
                         android.R.layout.simple_list_item_1) {
        	@Override
        	public View getView(int position, View convertView,
        			ViewGroup parent) {
    	  	  	BlipMsg[] msges = allBlips.toArray(new BlipMsg[allBlips.size()]);
    	  	  	BlipMsg msg = msges[(int)position];
    	  	  	MsgView view = new MsgView(Blipus.this, msg.getUsersString()+" "+msg.getCreatedAt(), msg.getBody());
//    	  	  	findViewById(R.id.image).setVisibility(View.INVISIBLE);
//    	  	  	findViewById(R.id.movie).setVisibility(View.INVISIBLE);
//    	  	  	findViewById(R.id.recording).setVisibility(View.INVISIBLE);
//    	  	  	if (msg.hasPicture()) {
//    	  	  		findViewById(R.id.image).setVisibility(View.VISIBLE);
//    	  	  	}
//    	  	  	if (msg.hasMovie()) {
//    	  	  		findViewById(R.id.movie).setVisibility(View.VISIBLE);
//    	  	  	}
//    	  	  	if (msg.hasRecording()) {
//    	  	  		findViewById(R.id.recording).setVisibility(View.VISIBLE);
//    	  	  	}    	  	  	
        		return view;
        	}
        });        
        editor.addTextChangedListener(new TextWatcher() {
        	public void afterTextChanged(Editable s) {
        		int len = s.length();
        		if (len>0) {
	        		if (len>BLIP_LIMIT) {
	        			s.delete(BLIP_LIMIT-1, len-1);
	        		}        		
	        		if (s.toString().indexOf("\n")==len-1) {
	        			s.replace(len-1,len, "");
	        		}
        		}
        		final String textLength = ""+len+"/"+(160-len);
        		textLengthInfo.post(new Runnable() {
        			public void run() {
        				textLengthInfo.setText(textLength);
        			}
        		});
        	}

			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) { }

			public void onTextChanged(CharSequence s, int start, int before,
					int count) { }
        });
        blip = new Blip(new Credentials(getSharedPreferences("CREDENTIALS", Context.MODE_PRIVATE)));
        new Thread(new Runnable() {
        	public void run() {
        		while (1==1) {
	        		refreshListOfBlips(list, blip);
	        		try {
	        			Thread.sleep(15*1000);
	        		} catch (InterruptedException ie) {
	        			        	
        		}
        		}
        	}
        }).start();        
        
        button.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {              
              String text = editor.getText().toString();
              try {
            	  text = text.replaceAll("\n", " ").trim();
            	  blip.sendBlip(text);
            	  editor.setText("");
            	  refreshListOfBlips(list, blip);
              } catch (Exception e) {              
            	  Dialog d = new Dialog(editor.getContext());
            	  d.setTitle(e.getLocalizedMessage());
            	  d.show();
              }
        	}
        });        
    }
    
	private void refreshListOfBlips(final ListView list,final Blip blip) {
		if (duringRefresh) return;
		new Thread(new Runnable() {
			public void run() {
				if (duringRefresh) return;      	  
				duringRefresh = true;
				try {
					String condition = null;
					if (lastId!=-1) {
						condition = ""+lastId;
					}
		        	final List<BlipMsg> blips = blip.getBlips(condition);
		        	final List<BlipMsg> newList = new ArrayList<BlipMsg>();
		        	boolean redraw = false;
		        	if (blips.size()>0) {
		        		blips.addAll(allBlips);
		        		allBlips.clear();
		        		allBlips.addAll(blips);
		        		redraw = true;
		        	}
		        	newList.addAll(allBlips);
		        	if (redraw) {
				        list.post(new Runnable() {
				        	public void run() {	        			        			
						       	ArrayAdapter<BlipMsg> s = (ArrayAdapter<BlipMsg>)list.getAdapter();
						       	s.clear();
						       	for (BlipMsg blipMsg:newList) {
						       		s.add(blipMsg);
						       		if (blipMsg.getId()>lastId) {
						       			lastId = blipMsg.getId();
						       		}
						       	}
				        	}
				        });
		        	}
//			        NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);			        
//			        Notification notification = new Notification(R.drawable.icon,"New blips!!!",System.currentTimeMillis());
//			        notification.flags=Notification.FLAG_SHOW_LIGHTS;
//			        notification.ledOffMS=100;
//			        notification.ledOnMS=250;
//			        nm.notify((int)System.currentTimeMillis(), notification);
//			        list.post(new Runnable() {
//			        	public void run() {
//			        		list.invalidate();			        		
//			        	}
//			        });			        
		        } catch (final Exception e) {		        	
		        	e.printStackTrace();
//		        	Blipus.this.runOnUiThread(new Runnable() {
//		        		public void run() {
//		        			Dialog d = new Dialog(Blipus.this);		        			
//			            	d.setTitle(e.getLocalizedMessage());
//			            	d.show();
//		        		}
//		        	});
	            	  
		        } finally {
		        	duringRefresh=false;
		        }
		        registerForContextMenu(list);
			}
		}).start();
	}
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	menu.add(0, MENU_REFRESH, 0, getString(R.string.refresh));
    	menu.add(0, MENU_SETTINGS, 0, getString(R.string.settings));
    	menu.add(0, MENU_EXIT, 0, getString(R.string.exit));
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    		case MENU_SETTINGS : {
    			startActivity(new Intent(this,SettingsActivity.class));
    			return true;
    		}
    		case MENU_REFRESH : {
    			refreshListOfBlips(list, blip);
    			return true;
    		}
    		case MENU_EXIT : {
    			finish();
    			return true;
    		}
    	}
    	return false;
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
    		ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, v, menuInfo);
  	  	BlipMsg[] msges = allBlips.toArray(new BlipMsg[allBlips.size()]); 
  	  	BlipMsg msg = msges[(int)((AdapterContextMenuInfo)menuInfo).id];
  	  	if (msg.isStatus() || msg.isPrivateMessage() || msg.isDirectMessage()) {
	    	menu.setHeaderTitle(getString(R.string.manageYourBlips));
	    	String user = msg.getUserPath();
  		  	user = user.substring(user.lastIndexOf("/")+1);
	    	if (currentUserName.equals(user)) {
	    		menu.add(0,MENU_DELETE,0,getString(R.string.delete));
	    	}
	    	menu.add(0,MENU_QUOTE,0,getString(R.string.Quote));
	    	menu.add(0,MENU_REPLY,0,getString(R.string.Reply));
	    	menu.add(0,MENU_REPLY_QUOTE,0,getString(R.string.ReplyAndQuote));
	    	menu.add(0,MENU_PRIV_REPLY,0,getString(R.string.PrivateReply));
	    	menu.add(0,MENU_PRIV_REPLY_QUOTE,0,getString(R.string.PrivateReplyAndQuote));	    	
  	  	}
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	  AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    	  BlipMsg[] msges = allBlips.toArray(new BlipMsg[allBlips.size()]); 
    	  BlipMsg msg = msges[(int)info.id];
    	  final EditText editor = (EditText)findViewById(R.id.Edit01);
    	  try {
    	  switch (item.getItemId()) {
	    	  case MENU_DELETE: {
	    		  try {
	    			  blip.deleteBlip(""+msg.getId());
	    			  allBlips.remove(msg);
	    			  ArrayAdapter<BlipMsg> s = (ArrayAdapter<BlipMsg>)list.getAdapter();
	    			  s.remove(msg);
	    			  refreshListOfBlips(list, blip);
	    		  } catch (IOException ioEx) {
	    			  
	    		  }	    		  
	    		  return true;
	    	  }
	    	  case MENU_QUOTE: {	    		 	    		 
	    		  editor.setText("http://blip.pl/s/"+msg.getId()); 		  
	    		  return true;
	    	  }
	    	  case MENU_REPLY: {
	    		  String user = msg.getUserPath();
	    		  user = user.substring(user.lastIndexOf("/")+1);
	    		  editor.setText(">"+user+":");
	    		  return true;
	    	  }
	    	  case MENU_PRIV_REPLY: {
	    		  String user = msg.getUserPath();
	    		  user = user.substring(user.lastIndexOf("/")+1);
	    		  editor.setText(">>"+user+":");
	    		  return true;
	    	  }
	    	  case MENU_REPLY_QUOTE: {
	    		  String user = msg.getUserPath();
	    		  user = user.substring(user.lastIndexOf("/")+1);
	    		  editor.setText(">"+user+":http://blip.pl/s/"+msg.getId());
	    		  return true;
	    	  }
	    	  case MENU_PRIV_REPLY_QUOTE: {
	    		  String user = msg.getUserPath();
	    		  user = user.substring(user.lastIndexOf("/")+1);
	    		  editor.setText(">>"+user+":http://blip.pl/s/"+msg.getId());
	    		  return true;
	    	  }
	    	  case MENU_LINKS: {
	    		  return true;
	    	  }
    	  }
    	  } catch (Exception e) {
			  Dialog d = new Dialog(this);
			  d.setTitle(e.getMessage()+"\n"+e.getLocalizedMessage()+"\n"+e.getClass().getName());
			  d.show();    		  
    	  }
    	return super.onContextItemSelected(item);
    }

}