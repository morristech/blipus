package pl.przemelek.android;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.view.View;
import android.view.View.OnClickListener;

public class SettingsActivity extends Activity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        final SharedPreferences prefs = getSharedPreferences("CREDENTIALS", Context.MODE_PRIVATE);
        String loginText = prefs.getString("userName", "");
        String passwordText = prefs.getString("password", "");
        final EditText login = (EditText)findViewById(R.id.loginTextField);
        final EditText password = (EditText)findViewById(R.id.passwordTextField);
        login.setText(loginText);
        password.setText(passwordText);        
        Button storeButton = (Button)findViewById(R.id.storeButton);
        storeButton.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		SharedPreferences.Editor editor = prefs.edit();
        		editor.putString("userName",login.getText().toString());
        		editor.putString("password",password.getText().toString());
        		editor.commit();
        		SettingsActivity.this.finish();                		
        	}        	
        });        
    }
        
}
