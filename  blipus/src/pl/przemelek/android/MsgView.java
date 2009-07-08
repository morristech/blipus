package pl.przemelek.android;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.text.util.Linkify;
import android.text.util.Linkify.MatchFilter;
import android.text.util.Linkify.TransformFilter;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MsgView extends LinearLayout {
	private static final TransformFilter filter = new TransformFilter() {
		public String transformUrl(Matcher match, String url) {
			if (url.startsWith("#")) {
				return "tags/"+url.substring(1);
			} else if (url.startsWith("^")) {
				return "users/"+url.substring(1)+"/dashboard";
			} else return url;
		}
	};
	private static final MatchFilter matcher = new MatchFilter() {
		public boolean acceptMatch(CharSequence arg0, int arg1, int arg2) {
			return true;
		};
	};
	public MsgView(Context context, String users, String messageBody) {
		super(context);
		LayoutInflater li = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		li.inflate(R.layout.msg_view, this, true);
		TextView msgUser = (TextView)findViewById(R.id.msgUser);
		TextView msgBody = (TextView)findViewById(R.id.msgBody);		
		msgUser.setText(users);
		msgBody.setText(messageBody+"\n");
		Linkify.addLinks(msgBody, Linkify.ALL);		
		Linkify.addLinks(msgBody, Pattern.compile("[\\#\\^]\\w+"),  "http://blip.pl/", matcher, filter);		
	}
	

}
