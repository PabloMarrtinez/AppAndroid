package inf.um.pilotomimurcia;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
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

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Scanner;

import eu.olympus.keyrock.model.EidasIdentityProof;
import eu.olympus.keyrock.model.MiMurciaUserIdIdentityProof;
import eu.olympus.keyrock.model.TestIdentityProof;
import eu.olympus.model.Attribute;
import eu.olympus.model.Policy;
import inf.um.pilotomimurcia.olympus.ClientSingleton;
import inf.um.pilotomimurcia.rest.APIUtils;
import inf.um.pilotomimurcia.rest.CapManAPIService;
import inf.um.pilotomimurcia.utils.AsyncOperations;
import inf.um.pilotomimurcia.utils.Utils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignUp extends AppCompatActivity {

    private static final boolean useKeyrock = false;
    private static final String TAG = SignUp.class.getSimpleName();
    private static final int SCANDNI = 0;
    private EditText username, password, repeatPassword;
    private Button confirmButton;
    private ImageView settingsButton;
    private TextView notMatchText, failRegister;
    private ProgressBar loadingBar;
    private boolean isLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        username = (EditText) findViewById(R.id.fieldName);
        username.addTextChangedListener(signUpTextWatcher);
        password = (EditText) findViewById(R.id.fieldPassword);
        password.addTextChangedListener(signUpTextWatcher);
        repeatPassword = (EditText) findViewById(R.id.fieldRepeatPassword);
        repeatPassword.addTextChangedListener(signUpTextWatcher);
        confirmButton = (Button) findViewById(R.id.btnConfirm);
        confirmButton.setEnabled(false);
        settingsButton = findViewById(R.id.openSettingsButton);
        notMatchText = (TextView) findViewById(R.id.passwordNotMatch);
        notMatchText.setVisibility(View.INVISIBLE);
        failRegister = (TextView) findViewById(R.id.failRegister);
        failRegister.setVisibility(View.INVISIBLE);
        loadingBar = (ProgressBar) findViewById(R.id.loadingBarSignUp);
        loadingBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onBackPressed() {
        if(!isLoading)
            super.onBackPressed();
    }

    private void activarLoading() {
        isLoading=true;
        loadingBar.setVisibility(View.VISIBLE);
        confirmButton.setEnabled(false);
        username.setEnabled(false);
        password.setEnabled(false);
        repeatPassword.setEnabled(false);
        settingsButton.setEnabled(false);
    }

    private void desactivarLoading() {
        isLoading=false;
        loadingBar.setVisibility(View.INVISIBLE);
        confirmButton.setEnabled(true);
        username.setEnabled(true);
        password.setEnabled(true);
        repeatPassword.setEnabled(true);
        settingsButton.setEnabled(true);
    }

    public void onSettingsClick(View v) {
        Intent intent = new Intent(SignUp.this, SignUpSettings.class);
        startActivity(intent);
    }


    public void onRegister(View v) {
        failRegister.setVisibility(View.INVISIBLE);
        //Only clickable if username and passwords not empty and repeat password is correct

        Log.d(TAG,"Start requesting signed xml from Keyrock+eidas");
        if(useKeyrock)
            useKeyRockOpeningWebView(v);
        else {
            InputStream is = getResources().openRawResource(R.raw.response_keyrock4);
            Scanner s = new Scanner(is).useDelimiter("\\A");
            String xml = s.hasNext() ? s.next() : "";
            registerUsingXML(xml);
        }
    }

    public  void registerUsingXML(String xml) {
        AsyncOperations async= new AsyncOperations() {
            @Override
            public void handleProofIdentityResponse(Object response,int what) {
                if(response instanceof Exception){
                    Log.d(TAG, "Something went wrong when proving identity for idproof with code "+what, (Throwable) response);
                    runOnUiThread(()->desactivarLoading());
                    runOnUiThread(()->{
                        Toast.makeText(SignUp.this, "Something went wrong while proving identity", Toast.LENGTH_SHORT).show();
                    });
                }
                else{
                    Log.d(TAG, "Successfully proved identity in OLYMPUS for idproof with code "+what);
                    switch (what){
                        case TEST_PROOF_CODE:
                            Log.d(TAG, "Obtain credential for immediate use without need to login again and go to Menu Principal");
                            doAsyncLogin(username.getText().toString().trim(),password.getText().toString().trim(),new Policy(new LinkedList<>(),"DummyPolicy"));
                            break;
                        case UID_PROOF_CODE:
                            if(!extraMockIdProof(this)){
                                Log.d(TAG, "Not doing extra mock IdProof");
                                Log.d(TAG, "Obtain credential for immediate use without need to login again and go to Menu Principal");
                                doAsyncLogin(username.getText().toString().trim(),password.getText().toString().trim(),new Policy(new LinkedList<>(),"DummyPolicy"));
                            }
                            break;
                    }
                }
            }

            @Override
            public void handleRegisterResponse(Object response) {
                if(response instanceof Exception){
                    Log.d(TAG,"Something went wrong when registering and proving identity", (Throwable) response);
                    runOnUiThread(()->{desactivarLoading();
                        failRegister.setVisibility(View.VISIBLE);});
                }else{
                    Log.d(TAG,"Successfully registered and proved identity in OLYMPUS");
                    CapManAPIService capManAPIService=APIUtils.getCapManAPIService();
                    capManAPIService.requestUid().enqueue(new Callback<JsonElement>() {
                        @Override
                        public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
                            Log.d(TAG,"Registered on the platform. "+response.body());
                            JsonObject resp=response.body().getAsJsonObject();
                            String uid=resp.get("uid").getAsString();
                            String signature=resp.get("signature").getAsString();
                            MiMurciaUserIdIdentityProof proof=new MiMurciaUserIdIdentityProof(signature,uid);
                            doAsyncIdentityProof(username.getText().toString().trim(), password.getText().toString().trim(),proof);
                        }

                        @Override
                        public void onFailure(Call<JsonElement> call, Throwable t) {
                            Log.d(TAG,"Could not register on smart platform",t);
                            runOnUiThread(()->{desactivarLoading();
                                failRegister.setVisibility(View.VISIBLE);});
                            //Should change order of platform/OL registration or offer another try to register on platform somehow
                        }
                    });

                }
            }

            @Override
            public void handleLoginResponse(Object response) {
                runOnUiThread(()->desactivarLoading());
                if(response instanceof Exception || response.equals("Failed")){
                    Log.d(TAG,"Something went wrong when getting credential" + response.toString());
                    runOnUiThread(()->{
                        AlertDialog.Builder builder = new AlertDialog.Builder(SignUp.this);
                        builder.setTitle("Credential retrieval error").setMessage("Could not obtain a credential after registering. Please login to obtain a credential").setPositiveButton("Ok", (dialogInterface, i) -> {
                            Intent intent = new Intent(SignUp.this, LoginActivity.class);
                            startActivity(intent);
                        });
                        builder.create().show();
                    });
                }else{
                    Log.d(TAG, "Successfully retrieved a credential: " +
                            ClientSingleton.getCredentialManager().checkStoredCredential());
                    Intent intent = new Intent(SignUp.this, MenuPrincipal.class);
                    startActivity(intent);
                }
            }
        };
        activarLoading();
        EidasIdentityProof proof=new EidasIdentityProof(xml);
        Log.d(TAG,"Start registering in OLYMPUS IdProving with signed xml from Keyrock+eidas");
            async.doAsyncRegister(username.getText().toString().trim(), password.getText().toString().trim(),proof);
    }

    private boolean extraMockIdProof(AsyncOperations asyncOperations) {
        boolean useMockDateOfBirth=false;
        boolean useMockAddress=false;
        try {
            SharedPreferences shPre = Utils.secureSharedPreferences(this);
            useMockAddress=shPre.getBoolean("useMockAddress",false);
            useMockDateOfBirth=shPre.getBoolean("useMockDateOfBirth",false);
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Could not use shared preferences get useMock booleans", e);
        }

        if (useMockAddress || useMockDateOfBirth) {
            Map<String, Attribute> attrMock = new HashMap<>();
            attrMock.putAll(getAddressAttributes(useMockAddress));
            attrMock.putAll(getBirthDate(useMockDateOfBirth));
            TestIdentityProof proofMock = new TestIdentityProof();
            proofMock.setAttributes(attrMock);
            Log.d(TAG,"Start IdProving with test identity proof mock.");
            asyncOperations.doAsyncIdentityProof(username.getText().toString().trim(), password.getText().toString().trim(),proofMock);
            return true;
        } else
            return false;
    }

    private Map<String, Attribute> getBirthDate(boolean useMockDateOfBirth) {
        Map<String, Attribute> result = new HashMap<>();
        if (!useMockDateOfBirth)
            return result;
        try {
            SharedPreferences shPre = Utils.secureSharedPreferences(this);
            String dateString = shPre.getString("birthDate", getString(R.string.defaultBirthDate));
            Log.d(TAG,"RetrievedDate"+ dateString);
            SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
            Date date = format.parse(dateString);
            result.put("http://eidas.europa.eu/attributes/naturalperson/DateOfBirth", new Attribute(date));
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Could not use shared preferences getBirthDate", e);
        } catch (ParseException e) {
            Log.e(TAG, "Could not parse date", e);
        }
        return result;
    }

    private Map<String, Attribute> getAddressAttributes(boolean useMockAddress) {
        Map<String, Attribute> result = new HashMap<>();
        if (!useMockAddress)
            return result;
        try {
            SharedPreferences shPre = Utils.secureSharedPreferences(this);
            result.put("http://eidas.europa.eu/attributes/naturalperson/CurrentAddress/AdminUnitL1", new Attribute(shPre.getString("country", getString(R.string.defaultCountry))));
            result.put("http://eidas.europa.eu/attributes/naturalperson/CurrentAddress/AdminUnitL2", new Attribute(shPre.getString("province", getString(R.string.defaultProvince))));
            result.put("http://eidas.europa.eu/attributes/naturalperson/CurrentAddress/PostName", new Attribute(shPre.getString("city", getString(R.string.defaultCity))));
            result.put("http://eidas.europa.eu/attributes/naturalperson/CurrentAddress/PostCode", new Attribute(shPre.getString("postCode", getString(R.string.defaultPostCode))));
            result.put("http://eidas.europa.eu/attributes/naturalperson/CurrentAddress/ThoroughfareAndLocator", new Attribute(shPre.getString("address", getString(R.string.defaultAddress))));
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Could not use shared preferences getAddressAttributes", e);
        }
        return result;
    }

    @SuppressLint({"JavascriptInterface", "SetJavaScriptEnabled"})
    public void useKeyRockOpeningWebView(View view) {
        Intent intent = new Intent(SignUp.this, EIDASActivity.class);
        startActivityForResult(intent, SCANDNI);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case (SCANDNI): {
                if (resultCode == Activity.RESULT_OK) {
                    String xml = (String) data.getExtras().get("assertion");
                    registerUsingXML(xml);
                }
                break;
            }
        }
    }

    private TextWatcher signUpTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String us = username.getText().toString().trim();
            String pw = password.getText().toString().trim();
            String rpw = repeatPassword.getText().toString().trim();
            boolean match = pw.equals(rpw);
            confirmButton.setEnabled(!us.isEmpty() && !pw.isEmpty() && !rpw.isEmpty() && match);
            if (match)
                notMatchText.setVisibility(View.INVISIBLE);
            else
                notMatchText.setVisibility(View.VISIBLE);
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };
}