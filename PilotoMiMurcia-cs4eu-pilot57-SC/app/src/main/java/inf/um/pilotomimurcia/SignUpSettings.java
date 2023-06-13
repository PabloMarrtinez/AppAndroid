package inf.um.pilotomimurcia;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import eu.olympus.keyrock.model.EidasIdentityProof;
import eu.olympus.model.Policy;
import eu.olympus.model.server.rest.IdentityProof;
import inf.um.pilotomimurcia.eidas.MyJavaScriptInterface;
import inf.um.pilotomimurcia.eidas.MyWebViewClient;
import inf.um.pilotomimurcia.olympus.ClientSingleton;
import inf.um.pilotomimurcia.utils.Utils;

public class SignUpSettings extends AppCompatActivity {
    private static final String TAG = SignUpSettings.class.getSimpleName();
    private EditText country,province,city,postCode,address,birthDate;
    private Switch switchBD,switchAddress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        country=findViewById(R.id.editTextCountry);
        province=findViewById(R.id.editTextProvince);
        city=findViewById(R.id.editTextCity);
        postCode=findViewById(R.id.editTextPostalCode);
        address=findViewById(R.id.editTextAddress);
        birthDate=findViewById(R.id.editTextTextBirthDate);
        switchAddress=findViewById(R.id.switchAddress);
        switchBD=findViewById(R.id.switchDateOfBirth);
    }

    @Override
    protected void onResume() {
        try {
            SharedPreferences shPre=Utils.secureSharedPreferences(this);
            country.setText(shPre.getString("country",getString(R.string.defaultCountry)));
            province.setText(shPre.getString("province",getString(R.string.defaultProvince)));
            city.setText(shPre.getString("city",getString(R.string.defaultCity)));
            postCode.setText(shPre.getString("postCode",getString(R.string.defaultPostCode)));
            address.setText(shPre.getString("address",getString(R.string.defaultAddress)));
            birthDate.setText(shPre.getString("birthDate",getString(R.string.defaultBirthDate)));
            switchAddress.setChecked(shPre.getBoolean("useMockAddress",false));
            switchBD.setChecked(shPre.getBoolean("useMockDateOfBirth",false));
        } catch (GeneralSecurityException e) {
            Log.e(TAG,"Could not use shared preferences onResume",e);
        } catch (IOException e) {
            Log.e(TAG,"Could not use shared preferences onResume",e);
        }
        super.onResume();
    }

    public void onConfirm(View v){
        try {
            SharedPreferences shPre=Utils.secureSharedPreferences(this);
            SharedPreferences.Editor editor=shPre.edit();
            editor.putString("country",country.getText().toString().trim());
            editor.putString("province",province.getText().toString().trim());
            editor.putString("city",city.getText().toString().trim());
            editor.putString("postCode",postCode.getText().toString().trim());
            editor.putString("address",address.getText().toString().trim());
            editor.putString("birthDate",birthDate.getText().toString().trim());
            editor.putBoolean("useMockDateOfBirth",switchBD.isChecked());
            editor.putBoolean("useMockAddress",switchAddress.isChecked());
            editor.commit();
            Log.d(TAG,"Changed mock address and birthDate");
        } catch (GeneralSecurityException e) {
            Log.e(TAG,"Could not use shared preferences onConfirm",e);
        } catch (IOException e) {
            Log.e(TAG,"Could not use shared preferences onConfirm",e);

        }
        onBackPressed();
    }



}