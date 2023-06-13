package inf.um.pilotomimurcia;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.concurrent.Callable;

import eu.olympus.model.Policy;
import inf.um.pilotomimurcia.olympus.ClientConfiguration;
import inf.um.pilotomimurcia.olympus.ClientSingleton;
import inf.um.pilotomimurcia.utils.AsyncOperations;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = LoginActivity.class.getSimpleName();
    private EditText username,password;
    private Button loginButton,registerButton;
    private TextView failLogin;
    private ProgressBar loadingBar;
    private boolean isLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        username=(EditText)findViewById(R.id.txtUser);
        username.addTextChangedListener(loginTextWatcher);
        password=(EditText)findViewById(R.id.txtPassword);
        password.addTextChangedListener(loginTextWatcher);
        loginButton=(Button)findViewById(R.id.btnLogin);
        loginButton.setEnabled(false);
        registerButton=findViewById(R.id.btnSignup);
        failLogin=(TextView)findViewById(R.id.failLogin);
        failLogin.setVisibility(View.INVISIBLE);
        loadingBar = (ProgressBar) findViewById(R.id.loadingBarLogin);
        loadingBar.setVisibility(View.INVISIBLE);
    }

    public void onLogin(View v) {
        AsyncOperations async= new AsyncOperations() {
            @Override
            public void handleProofIdentityResponse(Object response,int what) {
                Log.d(TAG,"Unreachable reached");
                throw new RuntimeException("Should never reach here");
            }

            @Override
            public void handleRegisterResponse(Object response) {
                Log.d(TAG,"Unreachable reached");
                throw new RuntimeException("Should never reach here");
            }

            @Override
            public void handleLoginResponse(Object response) {
                runOnUiThread(()->desactivarLoading());
                if(response instanceof Exception){
                    Log.d(TAG,"Something went wrong when getting a credential in login", (Throwable) response);
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
                    Intent intent = new Intent(LoginActivity.this, MenuPrincipal.class);
                    startActivity(intent);
                }
            }
        };
        activarLoading();
        failLogin.setVisibility(View.INVISIBLE);
        async.doAsyncLogin(username.getText().toString().trim(),password.getText().toString().trim(),new Policy(new LinkedList<>(),"DummyPolicy"));
    }

    private void activarLoading() {
        isLoading=true;
        loadingBar.setVisibility(View.VISIBLE);
        loginButton.setEnabled(false);
        registerButton.setEnabled(false);
        username.setEnabled(false);
        password.setEnabled(false);
    }

    private void desactivarLoading() {
        isLoading=false;
        loadingBar.setVisibility(View.INVISIBLE);
        loginButton.setEnabled(true);
        registerButton.setEnabled(true);
        username.setEnabled(true);
        password.setEnabled(true);
    }


    public void onRegister(View v) {
        Intent intent = new Intent(LoginActivity.this, SignUp.class);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        if(!isLoading)
            super.onBackPressed();
    }

    private TextWatcher loginTextWatcher =new TextWatcher() {
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
