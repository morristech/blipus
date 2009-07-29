package pl.przemelek.android;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import pl.przemelek.android.blip.Blip;
import pl.przemelek.android.blip.Credentials;
import pl.przemelek.android.blip.Blip.BlipMsg;
import pl.przemelek.android.camera.CameraPreview;
import pl.przemelek.android.db.StatusesManager;
import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.Media;
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
	private static final int MENU_SHOW_USER_DASHBORD = 8;
	private static final int MENU_SHOW_IMAGE = 9;
	private static final int MENU_LINKS = 100;
	private static final int BLIP_LIMIT = 160;	
	private static String currentUserName;
	private boolean duringRefresh = false;
	private int lastId = -1;
	private LinkedHashSet<BlipMsg> allBlips = new LinkedHashSet<BlipMsg>();
	private ListView list;
	private Blip blip;
	private StatusesManager manager;
	private static Uri mUri;
	private static boolean withPhoto = false;
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
    	((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(1);
//    	Log.i
//    	requestWindowFeature(Window.FEATURE_NO_TITLE);
        SharedPreferences prefs = getSharedPreferences("CREDENTIALS", Context.MODE_PRIVATE);
        String userName = prefs.getString("userName", null);
        
        if (userName==null) {
        	startActivity(new Intent(this,SettingsActivity.class));
        } else {
        	startService(new Intent(this,BlipusService.class));        	
        }        
        currentUserName = userName;
        Button button = (Button)findViewById(R.id.Button01);
        Button button2 = (Button)findViewById(R.id.Button02);
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
    	  	  	MsgView view = new MsgView(Blipus.this, msg.getUsersString()+" "+msg.getCreatedAt(), msg.getBody(),msg.hasPicture(),msg.hasMovie(),msg.hasRecording());
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
        manager = new StatusesManager(this);
        new Thread(new Runnable() {
        	public void run() {
        		duringRefresh = false;
        		lastId=0;
        		while (1==1) {
	        		refreshListOfBlips(list);
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
            	  if (withPhoto) {
          	    	byte[] b = new byte[1024];
          	    	int rv = 0;
          	    	//super.onActivityResult(requestCode, resultCode, data);
          	    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
          	    	try {
          		    	InputStream is = getContentResolver().openInputStream(mUri);
          		    	while ((rv=is.read(b))>0) {
          		    		baos.write(b, 0, rv);
          		    	}		    	
          		    	is.close();
          		    	getContentResolver().delete(mUri, null, null);
          	    	} catch (Exception e) {
          	    		 try {
          	       		   FileWriter fw = new FileWriter("/sdcard/"+System.currentTimeMillis()+".txt");
          	       		   PrintWriter pw = new PrintWriter(fw);
          	       		   e.printStackTrace(pw);
           	        	  Dialog d = new Dialog(editor.getContext());
          	        	  d.setTitle(e.getMessage()+"\n"+e.getLocalizedMessage());
          	        	  d.show();
          	       		   fw.close();
          	       	   } catch (Exception ex) { }
          	        	  Dialog d = new Dialog(editor.getContext());
          	        	  d.setTitle(e.getMessage()+"\n"+e.getLocalizedMessage());
          	        	  d.show();
          	    	}
          	    	blip.sendBlip(text, baos.toByteArray());
            	  } else {            	  
            		blip.sendBlip(text);
            	  }
            	  withPhoto = false;
            	  editor.setText("");
            	  refreshListOfBlips(list);
              } catch (Exception e) {              
            	  Dialog d = new Dialog(editor.getContext());
            	  d.setTitle(e.getLocalizedMessage());
            	  d.show();
              }
        	}
        });
        button2.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
//        		startActivity(new Intent(Blipus.this,CameraPreview.class));
        		
//        		Intent pickPhotoFromCameraIntent = new Intent("android.media.action.IMAGE_CAPTURE");
//        		pickPhotoFromCameraIntent.putExtra("EXTRA_OUTPUT","output");
//        		startActivityForResult(pickPhotoFromCameraIntent, 2);
        		
//        		startActivity(new Intent("com.android.camera"));
        		//com.android.camera
        		
        		mUri = null;
        		try {
            		ContentValues values = new ContentValues();
            		values.put(Media.TITLE, "Image.jpg");
            		values.put(Images.Media.BUCKET_ID, "test");
            		values.put(Media.DESCRIPTION, "Image captured by camera");
            		mUri = getContentResolver().insert(Media.EXTERNAL_CONTENT_URI, values);
            		Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            		i.putExtra(MediaStore.EXTRA_OUTPUT, mUri);
            		startActivityForResult(i, 1);
        		} catch (Exception e) {
        			try {
	 	       		   FileWriter fw = new FileWriter("/sdcard/"+System.currentTimeMillis()+".txt");
		       		   PrintWriter pw = new PrintWriter(fw);
		       		   e.printStackTrace(pw);
		       		   fw.close();
        			} catch (Exception ex) {
        				
        			}
        		}
        	}
        });
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (resultCode==Activity.RESULT_OK) {
    		Button button = (Button)findViewById(R.id.Button01);
    		button.setText("Blip with photo");
    		withPhoto = true;
    		super.onActivityResult(requestCode, resultCode, data);
//	    	final EditText editor = (EditText)findViewById(R.id.Edit01);
//	    	byte[] b = new byte[1024];
//	    	int rv = 0;
//	    	super.onActivityResult(requestCode, resultCode, data);
//	    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
//	    	try {
//		    	InputStream is = getContentResolver().openInputStream(mUri);
//		    	while ((rv=is.read(b))>0) {
//		    		baos.write(b, 0, rv);
//		    	}		    	
//		    	is.close();
//		    	getContentResolver().delete(mUri, null, null);
//	    	} catch (Exception e) {
//	    		 try {
//	       		   FileWriter fw = new FileWriter("/sdcard/"+System.currentTimeMillis()+".txt");
//	       		   PrintWriter pw = new PrintWriter(fw);
//	       		   e.printStackTrace(pw);
//	       		   fw.close();
//	       	   } catch (Exception ex) { }
//	        	  Dialog d = new Dialog(editor.getContext());
//	        	  d.setTitle(e.getMessage()+"\n"+e.getLocalizedMessage());
//	        	  d.show();
//	    	}
//	 	   try {
//	 		   FileWriter fw = new FileWriter("/sdcard/"+System.currentTimeMillis()+".txt");
//	 		   PrintWriter pw = new PrintWriter(fw);
//	 		   for (String key:data.getExtras().keySet()) {
//	 			   pw.println(key);
//	 		   }
//	 		   fw.close();
//	 	   } catch (Exception ex) { }
//	                
//	        String text = editor.getText().toString();
//	        try {
//	      	  text = text.replaceAll("\n", " ").trim();
//	      	  blip.sendBlip(text,baos.toByteArray());
//	      	  baos.close();
//	      	  editor.setText("");
//	      	  refreshListOfBlips(list);
//	        } catch (Exception e) {
//	     	   try {
//	     		   FileWriter fw = new FileWriter("/sdcard/"+System.currentTimeMillis()+".txt");
//	     		   PrintWriter pw = new PrintWriter(fw);
//	     		   e.printStackTrace(pw);
//	     		   fw.close();
//	     	   } catch (Exception ex) { }
//	      	  Dialog d = new Dialog(editor.getContext());
//	      	  d.setTitle(e.getMessage()+"\n"+e.getLocalizedMessage());
//	      	  d.show();
//	        }
    	}
//       if ( data != null && data.hasExtra("data") ) {           
//    	   try {
//    		   FileWriter fw = new FileWriter("/sdcard/logPrzemelek.txt");
//    		   PrintWriter pw = new PrintWriter(fw);
//    		   for (String key:data.getExtras().keySet()) {
//    			   pw.println(key);
//    		   }
//    		   fw.close();
//    	   } catch (Exception ex) { }
//           
//    	   Bitmap photo = (Bitmap)data.getParcelableExtra("data");
//    	   
//           ByteArrayOutputStream baos = new ByteArrayOutputStream();
//           photo.compress(CompressFormat.JPEG, 80, baos);
//           final EditText editor = (EditText)findViewById(R.id.Edit01);
//           String text = editor.getText().toString();
//           try {
//         	  text = text.replaceAll("\n", " ").trim();
//         	  blip.sendBlip(text,baos.toByteArray());
//         	  baos.close();
//         	  editor.setText("");
//         	  refreshListOfBlips(list);
//           } catch (Exception e) {
//        	   try {
//        		   FileWriter fw = new FileWriter("/sdcard/logPrzemelek.txt");
//        		   PrintWriter pw = new PrintWriter(fw);
//        		   e.printStackTrace(pw);
//        		   fw.close();
//        	   } catch (Exception ex) { }
//         	  Dialog d = new Dialog(editor.getContext());
//         	  d.setTitle(e.getMessage()+"\n"+e.getLocalizedMessage());
//         	  d.show();
//           }
//           
//       }
    }
    
	private void refreshListOfBlips(final ListView list) {
		if (duringRefresh) return;
		new Thread(new Runnable() {
			public void run() {
				if (duringRefresh) return;
				duringRefresh = true;
				try {
					String condition = null;
					if (lastId!=-1) {
						condition = "id>"+lastId;
					}
		        	final List<BlipMsg> blips = manager.getList(condition, "100",false); 
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
    			refreshListOfBlips(list);
    			return true;
    		}
    		case MENU_EXIT : {
    			stopService(new Intent(this,BlipusService.class));
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
  		  	menu.add(0,MENU_SHOW_USER_DASHBORD,0,"Show "+user+" dashboard");
  		  	if (msg.hasPicture()) {
  		  		menu.add(0,MENU_SHOW_IMAGE,0,"Show image");
  		  	}
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
    	  final BlipMsg msg = msges[(int)info.id];
    	  final EditText editor = (EditText)findViewById(R.id.Edit01);
    	  try {
    	  switch (item.getItemId()) {
	    	  case MENU_DELETE: {
	    		  new Thread(new Runnable() {
	    			  public void run() {
		    			  try {
		    				  blip.deleteBlip(""+msg.getId());
		    			  } catch (IOException ex) { }
		    			  manager.delete(msg);
		    			  allBlips.remove(msg);
		    			  list.post(new Runnable() {
		    				 public void run() {
				    			  ArrayAdapter<BlipMsg> s = (ArrayAdapter<BlipMsg>)list.getAdapter();
				    			  s.remove(msg);
				    			  refreshListOfBlips(list);
				    			  bindService(new Intent("pl.przemelek.android.BlipusService.START"), null, 0);
		    				 }		    				 
		    			  });
	    			}
	    		  }).start(); 
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
	    	  case MENU_SHOW_USER_DASHBORD: {
	    		  String user = msg.getUserPath();
	    		  user = user.substring(user.lastIndexOf("/")+1);
	    		  Intent intent = new Intent(this,Dashboard.class);
	    		  intent.putExtra("pl.przemelek.android.userName", user);
	    		  startActivity(intent);
	    		  return true;
	    	  }
	    	  case MENU_SHOW_IMAGE: {
	    		  String picturePath = msg.getPicturesPath();
	    		  Intent intent = new Intent(this,ImageViewer.class);
	    		  intent.putExtra("pl.przemelek.android.imegInfoURL", picturePath);
	    		  startActivity(intent);
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