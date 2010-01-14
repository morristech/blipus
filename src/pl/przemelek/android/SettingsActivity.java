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
        final EditText userNameField = (EditText)findViewById(R.id.loginTextField);
        final EditText passwordField = (EditText)findViewById(R.id.passwordTextField);
        userNameField.setText(loginText);
        passwordField.setText(passwordText);
        Button storeButton = (Button)findViewById(R.id.storeButton);
        storeButton.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		SharedPreferences.Editor editor = prefs.edit();
        		String userName = userNameField.getText().toString();
        		String password = passwordField.getText().toString();
				editor.putString("userName",userName);        		
				editor.putString("password",password);
        		editor.commit();
        		SettingsActivity.this.finish();
        	}
        });
        Button cancelButton = (Button)findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		SettingsActivity.this.finish();
        	}
        });
    }
}
