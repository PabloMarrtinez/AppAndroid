package eu.olympus.credentialapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import eu.olympus.credentialapp.olympus.ClientSingleton;
import eu.olympus.credentialapp.utils.asyncoperations.AsyncLogin;
import eu.olympus.server.GoogleAuthenticator;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = LoginActivity.class.getSimpleName();
    private ProgressBar progressBar;
    private EditText username,password;
    private Switch mfaSwitch;
    private Button loginButton;
    private TextView failLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        progressBar=findViewById(R.id.loadingBarLogin);
        progressBar.setVisibility(View.INVISIBLE);
        loginButton=(Button)findViewById(R.id.btnLogin);
        loginButton.setEnabled(false);
        mfaSwitch=findViewById(R.id.switchMFA);
        username=(EditText)findViewById(R.id.editTextPersonName);
        username.addTextChangedListener(loginTextWatcher);
        password=(EditText)findViewById(R.id.editTextTextPassword);
        password.addTextChangedListener(loginTextWatcher);
        failLogin=(TextView)findViewById(R.id.failLogin);
        failLogin.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = this.getSharedPreferences(
                "com.example.app", Context.MODE_PRIVATE);
        mfaSwitch.setChecked(prefs.getBoolean("mfaChecked",false));
    }

    public void onLoginClick(View v){
        String user=username.getText().toString().trim();
        String pass=password.getText().toString().trim();
        AsyncLogin async=new AsyncLogin() {
            @Override
            public void handleLoginResponse(Object response) {
                runOnUiThread(()->deactivateLoading());
                if(response instanceof Exception){
                    Log.d(TAG,"Something went wrong when getting a credential in login", (Exception) response);
                    runOnUiThread(()->{
                        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                        builder.setTitle("Login error").setMessage("Could not complete execution of the authentication process.").setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
                        builder.create().show();
                    });
                }else if(response.equals("Failed")){
                    Log.d(TAG, "Unsuccessful authentication");
                    runOnUiThread(()->failLogin.setVisibility(View.VISIBLE));
                }
                else{
                    Log.d(TAG,"Successfully retrieved a credential: "+
                            ClientSingleton.getCredentialManager().checkStoredCredential());
                    finish();
                }
            }
        };
        activateLoading();
        failLogin.setVisibility(View.INVISIBLE);
        SharedPreferences prefs = this.getSharedPreferences(
                "com.example.app", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("mfaChecked",mfaSwitch.isChecked()).apply();
        if(mfaSwitch.isChecked()){
            AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
            builder2.setTitle("MFA activation");
            final EditText input = new EditText(this);
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
            input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL);
            builder2.setView(input);
            builder2.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String token = input.getText().toString();
                    async.doAsyncLogin(user,pass,token,GoogleAuthenticator.TYPE);
                }
            });
            builder2.create().show();
        } else{
            async.doAsyncLogin(user,pass,"","NONE");
        }
    }

    private void activateLoading() {
        progressBar.setVisibility(View.VISIBLE);
        loginButton.setEnabled(false);
        username.setEnabled(false);
        password.setEnabled(false);
    }

    private void deactivateLoading() {
        progressBar.setVisibility(View.INVISIBLE);
        loginButton.setEnabled(true);
        username.setEnabled(true);
        password.setEnabled(true);
    }

    public void onMFAClick(View v) {
        Intent intent = new Intent(LoginActivity.this, MfaActivity.class);
        startActivity(intent);
    }

    private final TextWatcher loginTextWatcher =new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String us=username.getText().toString().trim();
            String pw=password.getText().toString().trim();
            loginButton.setEnabled(!us.isEmpty() && !pw.isEmpty());
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };
}