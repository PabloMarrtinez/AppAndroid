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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

                                                                        /*
                                                                        import eu.olympus.keyrock.model.TestIdentityProof;
                                                                        duda con como importar este modelo
                                                                         */

import eu.olympus.cfp.model.TestIdentityProof;
import eu.olympus.model.Attribute;
import eu.olympus.model.Operation;
import eu.olympus.model.Predicate;
import eu.olympus.server.rest.*;

import eu.olympus.model.Policy;
import eu.olympus.credentialapp.olympus.ClientSingleton;
import eu.olympus.credentialapp.rest2.APIUtils;
import eu.olympus.credentialapp.rest2.CapManAPIService;
import eu.olympus.credentialapp.utils.AsyncOperations;





public class Register extends AppCompatActivity {

    private static final String TAG = Register.class.getSimpleName();
    private EditText username, password, publickey, birthday, privateKey;
    private TextView check;
    private Button confirmButton;
    private TextView notMatchText, failRegister;
    private ProgressBar loadingBar;
    private boolean isLoading;
    address a = new address();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);
        username = findViewById(R.id.UsernameRegister);
        password = findViewById(R.id.PasswdRegister);
        publickey = findViewById(R.id.publicKey);
        privateKey = findViewById(R.id.privateKey);
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

    public void checkKey(String publicK, String privateK) {

        String url = "http:/"+a.getAddress()+":4000/checkPrivateKey?publicKey=" + publicK + "&private=" + privateK;
        Log.d(TAG, url);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            Log.e(TAG, response);
                            boolean isMatchingPrivateKey = Boolean.parseBoolean(response);
                            if (isMatchingPrivateKey) {
                                regist();
                            }
                            else{
                                check.setVisibility(View.VISIBLE);
                                check.setText("public and private key don't match");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error al procesar la respuesta: " + e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error en la solicitud: " + error.toString());
                    }
                });

        RequestQueue requestQueue = Volley.newRequestQueue(Register.this);
        requestQueue.add(stringRequest);
    }


    public void regist(){
        Log.d(TAG, "Start registering in OLYMPUS");
        String usernameText = username.getText().toString().trim();
        String passwordText = password.getText().toString().trim();
        String publicKeyText = publickey.getText().toString().trim();
        String birthdayText = birthday.getText().toString().trim();
        String privateKeyText = privateKey.getText().toString().trim();



        if (usernameText.equals("") || passwordText.equals("") || publicKeyText.equals("") || birthdayText.equals("")){
            check.setVisibility(View.VISIBLE);
            check.setText("Complete all the fields");
        } else{



            check.setVisibility(View.INVISIBLE);
            TestIdentityProof testProof = new TestIdentityProof();
            testProof.setAttributes(new HashMap<>());
            testProof.getAttributes().put("https://olympus-project.eu/example/model/publicKey",  new Attribute(publicKeyText));
            testProof.getAttributes().put("https://olympus-project.eu/example/model/birthday",  new Attribute(birthdayText));
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
    public void onRegister(View v) {


        String publicKeyText = publickey.getText().toString().trim();

        String privateKeyText = privateKey.getText().toString().trim();

        String usernameText = username.getText().toString().trim();
        String passwordText = password.getText().toString().trim();

        String birthdayText = birthday.getText().toString().trim();


        if (publicKeyText.equals("") || privateKeyText.equals("") || usernameText.equals("") || passwordText.equals("") || publicKeyText.equals("") || birthdayText.equals("")){
            check.setVisibility(View.VISIBLE);
            check.setText("Complete all the fields");
        } else{
            checkKey(publicKeyText,privateKeyText);

        }

    }

    public void changeBack(View v){

        Intent  i = new Intent(this, Main.class);
        startActivity(i);

    }


}
