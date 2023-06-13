package eu.olympus.credentialapp;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.integration.android.IntentIntegrator;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import eu.olympus.client.interfaces.CredentialStorage;
import eu.olympus.credentialapp.olympus.ClientConfiguration;
import eu.olympus.credentialapp.olympus.ClientSingleton;
import eu.olympus.credentialapp.olympus.EncryptedCredentialStorage;
import eu.olympus.credentialapp.olympus.UseCasePilotConfiguration;
import eu.olympus.credentialapp.rest.PresentPostModel;
import eu.olympus.credentialapp.rest.RetrofitClient;
import eu.olympus.credentialapp.rest.ServiceAPI;
import eu.olympus.credentialapp.utils.asyncoperations.AsyncLogin;
import eu.olympus.credentialapp.utils.Utils;
import eu.olympus.credentialapp.utils.asyncoperations.AsyncPresentation;
import eu.olympus.model.Attribute;
import eu.olympus.model.AttributeType;
import eu.olympus.model.Operation;
import eu.olympus.model.Policy;
import eu.olympus.model.Predicate;
import eu.olympus.model.exceptions.TokenGenerationException;
import eu.olympus.util.Util;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final long initializationTimeout = 5;
    private Button btnIni,btnNew;
    private ImageButton btnScan;
    private ActivityResultLauncher<Intent> launcher;
    private ServiceAPI api;
    private ProgressBar progressBar;
    private boolean qrReader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnIni = findViewById(R.id.buttonTryInitialization);
        btnIni.setVisibility(View.INVISIBLE);
        btnNew = findViewById(R.id.buttonNew);
        btnScan = findViewById(R.id.imageButton);
        progressBar = findViewById(R.id.mainLoadingBar);
        progressBar.setVisibility(View.INVISIBLE);
        // You can do the assignment inside onAttach or onCreate, i.e, before the activity is displayed
        launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // There are no request codes
                        Intent data = result.getData();
                        String url = data.getStringExtra("url");
                        Log.d(TAG, data.getExtras().toString());
                        getPolicyFromUrl(url);
                    }
                });
        if (!ClientSingleton.isInitialized()) {
            tryInitialize();
        }
    }

    public void onCredClick(View v) {
        ClientSingleton.deleteStoredCredential();
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
    }

    public void onManualTestClick(View v) {
        if (ClientSingleton.isInitialized()) {
            if (ClientSingleton.getCredentialManager().checkStoredCredential()) {
                qrReader=false;
                Intent intent = new Intent(MainActivity.this, UrlActivity.class);
                launcher.launch(intent);
            }else {
                // No credential
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(R.string.noCredential).setMessage(R.string.noCredentialDesc).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                builder.create().show();
            }
        } else {
            // Client not initialized
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(R.string.initError).setMessage(R.string.initErrorDesc);
            builder.create().show();
        }
    }


        public void onScanClick(View v) {
            if (ClientSingleton.isInitialized()) {
                if (ClientSingleton.getCredentialManager().checkStoredCredential()) {
                    //QR scanner
                    try {
                        qrReader = true;
                        IntentIntegrator integrator = new IntentIntegrator(this);
                        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
                        integrator.setPrompt("Scan a QR");
                        integrator.setBeepEnabled(false);
                        integrator.initiateScan();
                    } catch (Exception e) {
                        Log.d(TAG,"Exception when launching QR scanner", e);
                        Toast.makeText(this,R.string.messageExceptionQR,Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // No credential
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle(R.string.noCredential).setMessage(R.string.noCredentialDesc).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
                    builder.create().show();
                }
            } else {
                // Client not initialized
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(R.string.initError).setMessage(R.string.initErrorDesc);
                builder.create().show();
            }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (qrReader){
            if(resultCode == RESULT_OK){
                Log.d(TAG,"QR SCAN: " + data.getStringExtra("SCAN_RESULT"));
                getPolicyFromUrl(data.getStringExtra("SCAN_RESULT"));
            } else {
                Toast.makeText(this,R.string.messageNoResultQR,Toast.LENGTH_LONG).show();
            }
            qrReader = false;

        }
    }


    public void onTryClick(View v) {
        tryInitialize();
    }


    private void getPolicyFromUrl(String url) {
        Toast.makeText(this, "Scanned url: " + url, Toast.LENGTH_LONG).show();
        if (Utils.checkURL(url)) {
            api = RetrofitClient.getClient(url).create(ServiceAPI.class);
            api.getPolicy().enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    Log.d(TAG, "Got Response. Successful: " + response.isSuccessful());
                    Log.d(TAG, "Response body "+response.body());
                    if (response.isSuccessful()) {
                        ObjectMapper mapper = new ObjectMapper();
                        Policy deserializedPolicy = null;
                        try {
                            deserializedPolicy = mapper.readValue(response.body(), Policy.class);
                            Log.d(TAG, "Policy id " + deserializedPolicy.getPolicyId());
                            Log.d(TAG, "Policy predicates " + deserializedPolicy.getPredicates());
                            Policy requestedPolicy = deserializedPolicy;
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            builder.setTitle(R.string.askPermissions).setMessage(generatePermissionText(deserializedPolicy)).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    activateLoading();
                                    AsyncPresentation async=new AsyncPresentation() {
                                        @Override
                                        public void handleGeneratePresentationResponse(Object response) {
                                            runOnUiThread(MainActivity.this::deactivateLoading);
                                            if(response instanceof TokenGenerationException){
                                                Log.d(TAG, "Failure token generation", (Exception)response);
                                                runOnUiThread(()-> {
                                                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                                    builder.setTitle("Presentation error").setMessage("Could not generate presentation. Is the policy fulfilled by the credential attributes?").setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
                                                    builder.create().show();
                                                });
                                            }else if(response instanceof IllegalStateException) {
                                                runOnUiThread(()-> {
                                                    Log.d(TAG, "Failure token generation", (Exception) response);
                                                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                                    builder.setTitle("Presentation error").setMessage("Credential not available").setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
                                                    builder.create().show();
                                                });
                                            } else if (response instanceof String){
                                                Log.d(TAG, "Token " + response);
                                                Toast.makeText(MainActivity.this, "Presentation generated", Toast.LENGTH_SHORT).show();
                                                presentToken((String)response, requestedPolicy);
                                            } else {
                                                runOnUiThread(()-> {
                                                    Log.d(TAG, "Failure token generation" + response);
                                                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                                    builder.setTitle("Presentation error").setMessage("Unexpected error generating presentation").setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
                                                    builder.create().show();
                                                });
                                            }
                                        }
                                    };
                                    async.doAsyncGeneratePresentation(requestedPolicy);
                                }
                            }).setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
                            builder.create().show();
                        } catch (IOException e) {
                            Log.d(TAG, "Mapper getPolicy ", e);
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            builder.setTitle("Communication error").setMessage("Unexpected policy format").setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
                            builder.create().show();
                        }
                    } else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle("Unsuccessful exchange").setMessage("Could not retrieve verification policy from URL").setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
                        builder.create().show();
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    Log.d(TAG, "OnFailure getPolicy ", t);
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Communication error").setMessage("Could not retrieve verification policy from URL").setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
                    builder.create().show();
                }
            });
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(R.string.error).setMessage(R.string.invalidURL);
            builder.create().show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(ClientSingleton.isInitialized())
            btnIni.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onBackPressed() {
    }

    private void presentToken(String token, Policy requestedPolicy) {
        Intent intent = new Intent(MainActivity.this, ResultActivity.class);
        api.presentToken(new PresentPostModel(token, requestedPolicy.getPolicyId())).enqueue(new Callback<Boolean>() {
            @Override
            public void onResponse(Call<Boolean> call, Response<Boolean> response) {
                if (response.isSuccessful() && response.body() == true) {
                    intent.putExtra("result", 0);
                } else {
                    intent.putExtra("result", 1);
                }
                startActivity(intent);
            }

            @Override
            public void onFailure(Call<Boolean> call, Throwable t) {
                intent.putExtra("result", 99);
                startActivity(intent);
            }
        });
    }


    private void tryInitialize() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CompletionService<Boolean> completionService = new ExecutorCompletionService<>(executor);
        CredentialStorage storage = new EncryptedCredentialStorage("credentialStorage", this.getApplicationContext());
        InitializeCallable callable = new InitializeCallable(new UseCasePilotConfiguration(storage));
        completionService.submit(callable);
        try {
            Future<Boolean> future = completionService.poll(initializationTimeout, TimeUnit.SECONDS);
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
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Initialization error").setMessage("Could not initialize OLYMPUS client: Perhaps no connectivity to IdPs. You can try again using the top right button.").setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
        builder.create().show();
        btnIni.setVisibility(View.VISIBLE);
    }

    private void activateLoading() {
        progressBar.setVisibility(View.VISIBLE);
        btnNew.setEnabled(false);
        btnScan.setEnabled(false);
    }

    private void deactivateLoading() {
        progressBar.setVisibility(View.INVISIBLE);
        btnNew.setEnabled(true);
        btnScan.setEnabled(true);
    }

    private String generatePermissionText(Policy requestedPolicy) {
        String message = "";
        for (Predicate p : requestedPolicy.getPredicates()) {
            switch (p.getOperation()) {
                case REVEAL:
                    message += "Reveal the value of " + p.getAttributeName() + "\n";
                    break;
                case LESSTHANOREQUAL:
                case GREATERTHANOREQUAL:
                    message += "Prove " + p.getAttributeName() + textFromValue(p.getOperation(), p.getValue(), null) + "\n";
                    break;
                case INRANGE:
                    message += "Prove " + p.getAttributeName() + textFromValue(p.getOperation(), p.getValue(), p.getExtraValue()) + "\n";
                    break;
                default:
                    message += p.getOperation() + " " + p.getValue() + "\n";
            }
        }
        return message;
    }

    private String textFromValue(Operation operation, Attribute value, Attribute extraValue) {
        String text = "";
        switch (operation) {
            case LESSTHANOREQUAL:
                if (value.getType() == AttributeType.DATE) {
                    text += " is before " + Util.toRFC3339UTC((Date) value.getAttr());
                } else {
                    text += " is less than " + value.getAttr();
                }
                break;
            case GREATERTHANOREQUAL:
                if (value.getType() == AttributeType.DATE) {
                    text += " is after " + Util.toRFC3339UTC((Date) value.getAttr());
                } else {
                    text += " is greater than " + value.getAttr();
                }
                break;
            case INRANGE:
                if (value.getType() == AttributeType.DATE) {
                    text += " is between " + Util.toRFC3339UTC((Date) value.getAttr()) + " and " + Util.toRFC3339UTC((Date) extraValue.getAttr());
                } else {
                    text += " is between " + value.getAttr() + " and " + extraValue.getAttr();
                }
                break;
            default:

        }
        return text;
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


