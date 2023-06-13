package eu.olympus.credentialapp;


import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import eu.olympus.cfp.model.TestIdentityProof;
import eu.olympus.client.interfaces.CredentialStorage;
import eu.olympus.credentialapp.olympus.ClientConfiguration;
import eu.olympus.credentialapp.olympus.ClientSingleton;
import eu.olympus.credentialapp.olympus.EncryptedCredentialStorage;
import eu.olympus.credentialapp.olympus.UseCasePilotConfiguration;
import eu.olympus.credentialapp.utils.AsyncOperations;
import eu.olympus.model.PSCredential;
import eu.olympus.model.Policy;

public class Main extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private EditText username,password;
    private Button loginButton;
    private Button btnIni;

    public String user;
    public String pass;

    public  CredentialStorage storage;

    public void changeRegister(View v){

        Intent  i = new Intent(this, Register.class);
        startActivity(i);

    }
/*
    public void changeNextPage(View v){

        Intent  i = new Intent(this, Home.class);
        startActivity(i);

    }

 */

    @Override
    protected void onCreate(Bundle saveInstanceState){
        super.onCreate(saveInstanceState);
        setContentView(R.layout.main);
        btnIni = findViewById(R.id.Initialize);
        btnIni.setVisibility(View.INVISIBLE);
        loginButton=(Button)findViewById(R.id.Login);
        loginButton.setEnabled(true);
        username=(EditText)findViewById(R.id.Username);
        password=(EditText)findViewById(R.id.Passwd);

        if (!ClientSingleton.isInitialized()) {
            tryInitialize();
        }
    }

    public void onTryClick(View v) {
        tryInitialize();
    }
    private void tryInitialize() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CompletionService<Boolean> completionService = new ExecutorCompletionService<>(executor);
        storage = new EncryptedCredentialStorage("credentialStorage", this.getApplicationContext());
        Main.InitializeCallable callable = new Main.InitializeCallable(new UseCasePilotConfiguration(storage));
        completionService.submit(callable);
        try {
            Future<Boolean> future = completionService.poll(2, TimeUnit.SECONDS);
            if (future == null)
                handleErrorInitialization();
            else
                future.get();
        } catch (InterruptedException | ExecutionException e) {
            Log.d(TAG, "Could not initialize", e);
            handleErrorInitialization();
        }
    }

    private void handleErrorInitialization() {
        AlertDialog.Builder builder = new AlertDialog.Builder(Main.this);
        builder.setTitle("Initialization error").setMessage("Could not initialize OLYMPUS client: Perhaps no connectivity to IdPs. You can try again using the top right button.").setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
        builder.create().show();
        btnIni.setVisibility(View.VISIBLE);
    }

    public void onLoginClick(View v){
        user=username.getText().toString().trim();
        pass=password.getText().toString().trim();
        MySingleton.getInstance().setMyVariable(user,pass);

        TestIdentityProof testProof = new TestIdentityProof();
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
                //runOnUiThread(()->desactivarLoading());
                if(response instanceof Exception){
                    Log.d(TAG,"Something went wrong when getting a credential in login", (Throwable) response);
                    runOnUiThread(()->{
                        /*
                        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                        builder.setTitle("Login error").setMessage("Could not complete execution of the authentication process.").setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
                        builder.create().show();

                         */
                        Log.d(TAG, "Could not complete execution of the authentication process.");
                    });
                }else if(response.equals("Failed")){
                    Log.d(TAG, "Unsuccessful authentication");
                    //runOnUiThread(()->failLogin.setVisibility(View.VISIBLE));
                }
                else{
                    Log.d(TAG,"Successfully retrieved a credential: "+
                            ClientSingleton.getCredentialManager().checkStoredCredential());
                    Intent intent = new Intent(Main.this, Home.class);
/*
                    PSCredential credential = storage.getCredential();

                    Log.d(TAG, credential.toString());


 */

                    startActivity(intent);
                }
            }
        };
        activateLoading();

        async.doAsyncLogin(user,pass,new Policy(new LinkedList<>(),"DummyPolicy"));




    }

    private void activateLoading() {
        /*
        loginButton.setEnabled(false);
        username.setEnabled(false);
        password.setEnabled(false);

         */
    }

    private void deactivateLoading() {
        /*
        loginButton.setEnabled(true);
        username.setEnabled(true);
        password.setEnabled(true);

         */
    }

    private static class InitializeCallable implements Callable<Boolean> {

        private final ClientConfiguration config;

        public InitializeCallable(ClientConfiguration config) {
            this.config = config;
        }

        @Override
        public Boolean call() throws Exception {
            try {
                ClientSingleton.initialize(config);
                return true;
            } catch (Exception e) {
                Log.d(TAG, "Initialization exception", e);
                throw e;
            }
        }
    }
}
