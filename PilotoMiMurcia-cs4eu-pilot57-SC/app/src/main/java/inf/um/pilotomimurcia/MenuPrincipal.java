package inf.um.pilotomimurcia;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import eu.olympus.client.interfaces.CredentialStorage;
import eu.olympus.model.Operation;
import eu.olympus.model.Policy;
import eu.olympus.model.Predicate;
import inf.um.pilotomimurcia.exceptions.CapManCommunicationException;
import inf.um.pilotomimurcia.miMurcia.CapabilityTokenStorage;
import inf.um.pilotomimurcia.miMurcia.MiMurciaServicesAdapter;
import inf.um.pilotomimurcia.miMurcia.ServiceInfoModel;
import inf.um.pilotomimurcia.miMurcia.model.CapabilityToken;
import inf.um.pilotomimurcia.olympus.BasicLedgerConfiguration;
import inf.um.pilotomimurcia.olympus.ClientConfiguration;
import inf.um.pilotomimurcia.olympus.ClientSingleton;
import inf.um.pilotomimurcia.olympus.EncryptedCredentialStorage;
import inf.um.pilotomimurcia.rest.CapManAPIService;
import inf.um.pilotomimurcia.rest.APIUtils;
import inf.um.pilotomimurcia.rest.PepAPIService;
import inf.um.pilotomimurcia.utils.AsyncPresentation;
import inf.um.pilotomimurcia.utils.Utils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static inf.um.pilotomimurcia.rest.APIUtils.PEP_BASE_URL;

public class MenuPrincipal extends AppCompatActivity implements MiMurciaServicesAdapter.OnServiceListener {
    private CapManAPIService capManApiService;
    private PepAPIService pepAPIService;
    private static final String TAG = MenuPrincipal.class.getSimpleName();
    private static final boolean useTls = true;  //TODO Use some kind of configuration instead of these constants?
    public static final long PRESENTATION_LIFETIME = 60;
    private static final long initializationTimeout = 10000; //milliseconds
    private static final String UID_ATTRIBUTE = "http://cs4eu.eu/pilot57/attributes/UserId";
    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private MiMurciaServicesAdapter adapter;
    private List<ServiceInfoModel> lista;
    private CapabilityTokenStorage capTokenStorage;
    private SharedPreferences encPrefs = null;
    private boolean mapActive = false;
    private boolean isLoading;
    private ProgressBar loadingBar;
    private Button btnIssuance;
    private boolean revealUid;
    private static final int PICK_FILE = 2;
    private Uri uriForJsonEvent;
    private boolean ctiCase; // CTI case
    private Callback<String> accessServiceCallback;
    private String currentService;
    private CapabilityToken currentCapToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        capTokenStorage = CapabilityTokenStorage.getInstance();
        capManApiService = APIUtils.getCapManAPIService();
        pepAPIService = APIUtils.getPepAPIService();
        setContentView(R.layout.activity_menu_principal);
        // Get an API instance
        capManApiService = APIUtils.getCapManAPIService();
        Button btnIni = findViewById(R.id.buttonTryInitialization);
        btnIni.setVisibility(View.INVISIBLE);
        loadingBar = (ProgressBar) findViewById(R.id.progressBarMenuPrincipal);
        loadingBar.setVisibility(View.INVISIBLE);
        btnIssuance = findViewById(R.id.btnIssuance);
        if (!ClientSingleton.isInitialized()) {
            tryInitialize();
        }
        FloatingActionButton fab = findViewById(R.id.mainSearchBtn);
        /*try {
            if(!ClientSingleton.isInitialized() && !Utils.secureSharedPreferences(getApplicationContext()).getString("vidp", "").equals("")) {
                tryInit();
            }
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        fab.setOnClickListener(view -> {
            if (!ClientSingleton.isInitialized() || !ClientSingleton.getCredentialManager().checkStoredCredential()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MenuPrincipal.this);
                builder.setTitle("Credential will be needed for these functionalities").setMessage("Obtain a credential before continuing").setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
                builder.create().show();
            } else {
                Intent i = new Intent(MenuPrincipal.this, AdvancedFuncSelection.class);
                startActivity(i);
            }
        });
        accessServiceCallback = new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                Log.d(TAG, "PEP OnResponse " + response);
                desactivarLoading();
                if (response.code() == 200) {
                    useServiceWebView(response.body());
                } else if (response.code() == 401) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MenuPrincipal.this);
                    builder.setTitle("Unauthorized").setMessage("Could not access service: PEP denied access").setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
                    builder.create().show();
                } else { //Other codes returned are not as relevant (internal server error/wrong request...)
                    AlertDialog.Builder builder = new AlertDialog.Builder(MenuPrincipal.this);
                    builder.setTitle("PEP error").setMessage("Could not access service: PEP response with code " + response.code()).setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
                    builder.create().show();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MenuPrincipal.this);
                builder.setTitle("PEP error").setMessage("Could not access service: Communication with PEP failed").setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
                builder.create().show();
                Log.d(TAG, "PEP OnFailure ", t);
                desactivarLoading();
            }
        };
    }

    public void onTryClick(View v) {
        tryInitialize();
    }

    private void tryInitialize() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CompletionService<Void> completionService = new ExecutorCompletionService<>(executor);
        CredentialStorage storage = new EncryptedCredentialStorage("credential", this.getApplicationContext());
        //storage.deleteCredential();
        //InitializeCallable callable=new InitializeCallable(new BasicPilotOfflineIdpConfiguration(storage,useTls,this));
        //InitializeCallable callable=new InitializeCallable(new BasicUmuIdpConfiguration(storage,useTls));
        InitializeCallable callable = new InitializeCallable(new BasicLedgerConfiguration(storage, useTls));
        completionService.submit(callable);
        try {
            Future<Void> future = completionService.poll(initializationTimeout, TimeUnit.MILLISECONDS);
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
        AlertDialog.Builder builder = new AlertDialog.Builder(MenuPrincipal.this);
        builder.setTitle("Initialization error").setMessage("Could not initialize OLYMPUS client: Perhaps no connectivity to IdPs. You can try again using the top right button.").setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
        builder.create().show();
        Button btnIni = findViewById(R.id.buttonTryInitialization);
        btnIni.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        lista = Utils.retrieveServices(this);
        if (lista == null) {
            findViewById(R.id.noEnabledTextView).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.noEnabledTextView).setVisibility(View.GONE);
            recyclerView = findViewById(R.id.servicesListView);
            layoutManager = new LinearLayoutManager(this);
            recyclerView.setLayoutManager(layoutManager);
            adapter = new MiMurciaServicesAdapter(lista, this);
            recyclerView.setHasFixedSize(true);
            recyclerView.setAdapter(adapter);
            adapter.notifyDataSetChanged();
        }
        credentialAvailableCheck();
    }

    private void credentialAvailableCheck() {
        TextView textNoCred = findViewById(R.id.NoStoredCred);
        if (ClientSingleton.isInitialized() && ClientSingleton.getCredentialManager().checkStoredCredential())
            textNoCred.setVisibility(View.INVISIBLE);
        else
            textNoCred.setVisibility(View.VISIBLE);
    }

    @Override
    public void onBackPressed() {
        if (mapActive) {
            Intent intent = new Intent(MenuPrincipal.this, MenuPrincipal.class);
            startActivity(intent);
            finish();
        }
    }

    public void onIssuance(View v) {
        CredentialStorage storage = new EncryptedCredentialStorage("credential", this.getApplicationContext());
        storage.deleteCredential();
        capTokenStorage.deleteAll();
        Intent intent = new Intent(MenuPrincipal.this, LoginActivity.class);
        startActivity(intent);
    }

    @Override
    public void onClick(int index) {
        if (isLoading)
            return;
        ServiceInfoModel service = this.lista.get(index);
        ctiCase = service.getUrlPath().toUpperCase().contains("CTI"); // TODO to change for cti full path?
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        CapabilityToken token = capTokenStorage.getToken("/" + service.getUrlPath());
        if (token != null)
            accessService(service.getUrlPath(), token);
        else if (ClientSingleton.isInitialized() && ClientSingleton.getCredentialManager().checkStoredCredential()) {
            try {
                Policy requestedPolicy = Utils.getPolicy(service.getUrlPath(), capManApiService, TAG);
                boolean uidRequired = uidRequired(requestedPolicy);
                revealUid = false;
                if (!uidRequired) {
                    View view = getLayoutInflater().inflate(R.layout.dialog_with_checkboxes, null);
                    CheckBox rememberCheckbox = view.findViewById(R.id.checkBoxUid);
                    rememberCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            revealUid = isChecked;
                        }
                    });
                    builder.setView(view);
                }

                builder.setTitle(R.string.askPermissions).setMessage(Utils.generatePermissionText(this, requestedPolicy)).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        activarLoading();
                        Policy tokenPolicy = requestedPolicy;
                        if (!uidRequired && revealUid) {
                            List<Predicate> preds = tokenPolicy.getPredicates();
                            preds.add(new Predicate(UID_ATTRIBUTE, Operation.REVEAL, null));
                            tokenPolicy.setPredicates(preds);
                        }
                        AsyncPresentation async = new AsyncPresentation() {
                            @Override
                            public void handleGeneratePresentationResponse(Object response) {
                                runOnUiThread(MenuPrincipal.this::desactivarLoading);
                                if (response instanceof Exception) {
                                    Log.d(TAG, "Something went wrong when generating presentation token.", (Exception) response);
                                    runOnUiThread(() -> {
                                        credentialAvailableCheck();
                                        AlertDialog.Builder builder = new AlertDialog.Builder(MenuPrincipal.this);
                                        builder.setTitle("Credential manager error").setMessage("Could not generate presentation token: can you fulfil the policy?").setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
                                        builder.create().show();
                                    });
                                } else if (response instanceof String) {
                                    Utils.longLog(TAG, "Generated token " + response);
                                    Toast.makeText(MenuPrincipal.this, "Presentation generated", Toast.LENGTH_SHORT).show();
                                    runOnUiThread(() -> requestCapToken(service.getUrlPath(), (String) response, requestedPolicy.getPolicyId()));
                                } else {
                                    runOnUiThread(() -> {
                                        Log.d(TAG, "Failure token generation" + response);
                                        AlertDialog.Builder builder = new AlertDialog.Builder(MenuPrincipal.this);
                                        builder.setTitle("Presentation error").setMessage("Unexpected error generating presentation").setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
                                        builder.create().show();
                                    });
                                }
                            }
                        };
                        async.doAsyncGeneratePresentation(requestedPolicy);
                    }
                }).setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
            } catch (CapManCommunicationException e) {
                builder.setTitle("Capability manager error").setMessage("Could not obtain policy for service: Unreachable or wrong response from Capability Manager").setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
                builder.create().show();
                Log.e(TAG, "Could not get policy", e);
            }
        } else {
            builder.setTitle("Credential needed for this service").setMessage("Obtain a credential before continuing").setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
        }
        builder.create().show();
    }

    private boolean uidRequired(Policy requestedPolicy) {
        return requestedPolicy.getPredicates().stream().map(Predicate::getAttributeName).anyMatch(e -> e.equals(UID_ATTRIBUTE));
    }

    private void accessService(String service, CapabilityToken capToken) {
        Log.d(TAG, "Accessing service " + service);

        if (!ctiCase) { // CTI case condition
            pepAPIService.getServicePage(service, capToken.toJsonString()).enqueue(accessServiceCallback);
        } else {
            currentService=service;
            currentCapToken=capToken;
            openFile(Uri.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)));
        }
    }

    private void requestCapToken(String service, String token, String policyId) {
        Log.d(TAG, "Requesting capability token " + service);
        JsonObject body = new JsonObject();
        String method = "GET";

        if (ctiCase)
            method = "POST";
        body.addProperty("ac", method);
        body.addProperty("re", "/" + service);
        body.addProperty("de", PEP_BASE_URL);
        body.addProperty("ZKTOKEN", token);
        body.addProperty("policyID", policyId);
        capManApiService.requestCapToken(body).enqueue(new Callback<CapabilityToken>() {
            @Override
            public void onResponse(Call<CapabilityToken> call, Response<CapabilityToken> response) {
                if (response.code() != 200) {
                    desactivarLoading();
                    if (response.code() == 403) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(MenuPrincipal.this);
                        builder.setTitle("Unauthorized").setMessage("Could not retrieve capability token: Access policy not met").setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
                        builder.create().show();
                    } else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(MenuPrincipal.this);
                        builder.setTitle("Capability manager error").setMessage("Could not obtain capability token: Wrong response from Capability Manager").setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
                        builder.create().show();
                        Log.d(TAG, "Something went wrong while requesting CapToken: Not OK code");
                    }
                } else {
                    CapabilityToken capabilityToken = (CapabilityToken) response.body();
                    Log.d(TAG, "CapTokenRequest: Response body " + response.body().toString());
                    capTokenStorage.storeToken(capabilityToken);
                    accessService(service, capabilityToken);
                }
            }

            @Override
            public void onFailure(Call<CapabilityToken> call, Throwable t) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MenuPrincipal.this);
                builder.setTitle("Capability manager error").setMessage("Could not obtain capability token: Wrong response from Capability Manager").setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
                builder.create().show();
                Log.d(TAG, "Something went wrong while contacting CapManager.", t);
                desactivarLoading();
            }
        });
    }

    @SuppressLint("JavascriptInterface")
    public void useServiceWebView(String html) {
        //Log.d(TAG,"Using WebView for html "+html);
        WebView myWebView = new WebView(MenuPrincipal.this);
        myWebView.getSettings().setSupportZoom(true);
        myWebView.getSettings().setJavaScriptEnabled(true);
        myWebView.getSettings().setUseWideViewPort(true);
        myWebView.getSettings().setAllowContentAccess(true);
        myWebView.getSettings().setAllowFileAccess(true);
        myWebView.getSettings().setAllowFileAccessFromFileURLs(true);
        myWebView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        myWebView.loadDataWithBaseURL("http://mapamurcia.inf.um.es", html, "text/html", "UTF-8", "");
        setContentView(myWebView);
        /*
        myWebView.loadUrl(url);
        MyWebViewClient myWebViewClient = new MyWebViewClient(this, getApplicationContext());
        myWebView.setWebViewClient(myWebViewClient);*/
        mapActive = true;
    }

    private class InitializeCallable implements Callable<Void> {

        private ClientConfiguration config;

        public InitializeCallable(ClientConfiguration config) {
            this.config = config;
        }

        @Override
        public Void call() throws Exception {
            try {
                ClientSingleton.initialize(config);
            } catch (Exception e) {
                Log.d(TAG, "Initialization exception", e);
                throw e;
            }
            return null;
        }
    }

    private void activarLoading() {
        isLoading = true;
        loadingBar.setVisibility(View.VISIBLE);
        btnIssuance.setEnabled(false);
    }

    private void desactivarLoading() {
        isLoading = false;
        loadingBar.setVisibility(View.INVISIBLE);
        btnIssuance.setEnabled(true);
    }

    private void openFile(Uri pickerInitialUri) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);// comprobar que es android kitkat? antes de usar opendcument
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Optionally, specify a URI for the file that should appear in the
        // system file picker when it loads.
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);
        intent.setType("*/*");
        startActivityForResult(intent, PICK_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        switch (requestCode) {
            case PICK_FILE:
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        uriForJsonEvent = data.getData();
                        String content = Utils.readContentFromUri(uriForJsonEvent, this.getBaseContext());
                        if (content != null)
                            pepAPIService.postServicePage(currentService, currentCapToken.toJsonString(),
                                    content).enqueue(accessServiceCallback);
                        else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(MenuPrincipal.this);
                            builder.setTitle("Error reading file").setMessage("Something happened reading the " +
                                    "json file provided").setPositiveButton(R.string.ok, (dialogInterface, i) ->
                                    dialogInterface.dismiss());
                            builder.create().show();
                            Log.d(TAG, "Something happened while reading the file");
                        }
                        // Perform operations on the document using its URI.
                    }
                }

                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }
}