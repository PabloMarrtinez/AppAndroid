package eu.olympus.credentialapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;

                                                                        /*
                                                                        import eu.olympus.keyrock.model.TestIdentityProof;
                                                                        duda con como importar este modelo
                                                                         */

import eu.olympus.cfp.model.TestIdentityProof;
import eu.olympus.model.Attribute;
import eu.olympus.server.rest.*;

import eu.olympus.model.Policy;
import eu.olympus.credentialapp.olympus.ClientSingleton;
import eu.olympus.credentialapp.rest2.APIUtils;
import eu.olympus.credentialapp.rest2.CapManAPIService;
import eu.olympus.credentialapp.utils.AsyncOperations;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import shaded.org.bouncycastle.util.test.Test;

public class Register extends AppCompatActivity {

    private static final String TAG = Register.class.getSimpleName();
    private EditText username, password, publickey, birthday;
    private TextView check;
    private Button confirmButton;
    private TextView notMatchText, failRegister;
    private ProgressBar loadingBar;
    private boolean isLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);
        username = findViewById(R.id.UsernameRegister);
        password = findViewById(R.id.PasswdRegister);
        publickey = findViewById(R.id.publicKey);
        birthday = findViewById(R.id.birthday);
        check = findViewById(R.id.check);
        check.setVisibility(View.INVISIBLE);
    }

    private void activarLoading() {
/*
        isLoading = true;
        loadingBar.setVisibility(View.VISIBLE);
        confirmButton.setEnabled(false);
        username.setEnabled(false);
        password.setEnabled(false);
        repeatPassword.setEnabled(false);

 */


    }

    private void desactivarLoading() {
/*
        isLoading = false;
        loadingBar.setVisibility(View.INVISIBLE);
        confirmButton.setEnabled(true);
        username.setEnabled(true);
        password.setEnabled(true);
        repeatPassword.setEnabled(true);
 */

    }


    public void onRegister(View v) {



        //failRegister.setVisibility(View.INVISIBLE);

        Log.d(TAG, "Start registering in OLYMPUS");
        String usernameText = username.getText().toString().trim();
        String passwordText = password.getText().toString().trim();
        String publicKeyText = publickey.getText().toString().trim();
        String birthdayText = birthday.getText().toString().trim();

        if (usernameText.equals("") || passwordText.equals("") || publicKeyText.equals("") || birthdayText.equals("")){
            check.setVisibility(View.VISIBLE);
        }
        else{
            check.setVisibility(View.INVISIBLE);
            TestIdentityProof testProof = new TestIdentityProof();
            testProof.setAttributes(new HashMap<>());
            testProof.getAttributes().put("publicKey",  new Attribute(publicKeyText));
            testProof.getAttributes().put("birthday",  new Attribute(birthdayText));
            AsyncOperations async = new AsyncOperations() {

                @Override
                public void handleProofIdentityResponse(Object response, int id) {

                }
                @Override
                public void handleRegisterResponse(Object response) {
                    if (response instanceof Exception) {
                        Log.d(TAG, "Something went wrong when registering and proving identity", (Throwable) response);
                        runOnUiThread(() -> {
                            desactivarLoading();
                            //failRegister.setVisibility(View.VISIBLE);
                        });
                    } else {
                        Log.d(TAG, "Successfully registered and proved identity in OLYMPUS");
                        doAsyncLogin(usernameText, passwordText, new Policy(new LinkedList<>(), "DummyPolicy"));
                    }
                }

                @Override
                public void handleLoginResponse(Object response) {
                    runOnUiThread(() -> desactivarLoading());
                    if (response instanceof Exception || response.equals("Failed")) {
                        Log.d(TAG, "Something went wrong when logging in after registration", (Throwable) response);
                        runOnUiThread(() -> {
                            Toast.makeText(Register.this, "Failed to login after registration", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(Register.this, Register.class);
                            startActivity(intent);
                            finish();
                        });
                    } else {
                        Log.d(TAG, "Successfully logged in after registration");
                        runOnUiThread(() -> {
                            Toast.makeText(Register.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }
                }
            };

            activarLoading();
            async.doAsyncRegister(username.getText().toString().trim(), password.getText().toString().trim(),testProof);

            //ClientSingleton.getInstance().doAsyncRegister(usernameText, passwordText, testProof, async);


        }

    }




}
