package inf.um.pilotomimurcia;

import static inf.um.pilotomimurcia.rest.APIUtils.PEP_BASE_URL;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.function.Consumer;

import eu.olympus.model.Policy;
import inf.um.pilotomimurcia.exceptions.CapManCommunicationException;
import inf.um.pilotomimurcia.miMurcia.CapabilityTokenStorage;
import inf.um.pilotomimurcia.miMurcia.ServiceInfoModel;
import inf.um.pilotomimurcia.miMurcia.model.CapabilityToken;
import inf.um.pilotomimurcia.rest.APIUtils;
import inf.um.pilotomimurcia.rest.CapManAPIService;
import inf.um.pilotomimurcia.rest.ChainAPIService;
import inf.um.pilotomimurcia.rest.PepAPIService;
import inf.um.pilotomimurcia.utils.AsyncPresentation;
import inf.um.pilotomimurcia.utils.Utils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdvancedFuncSelection extends AppCompatActivity {

    private static final String statServiceName="stats";
    private static final String dataServicesName="data";
    private static final String manageServicesName="manageservices";
    private static final String TAG = AdvancedFuncSelection.class.getSimpleName();
    private CapabilityTokenStorage capTokenStorage;
    private CapManAPIService capManAPIService;
    private PepAPIService pepAPIService;
    private ChainAPIService chainAPIService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_func_selection);
        capTokenStorage= CapabilityTokenStorage.getInstance();
        capManAPIService=APIUtils.getCapManAPIService();
        pepAPIService=APIUtils.getPepAPIService();
        chainAPIService=APIUtils.getChainAPIService();
    }


    public void onManageServicesClicked(View v){
        CapabilityToken token=capTokenStorage.getToken("/"+manageServicesName);
        Log.d(TAG,"ms "+manageServicesName+(token==null));
        if(token!=null)
            goManage(token);
        else
            requestConsent(manageServicesName, "GET,POST", AdvancedFuncSelection.this::goManage);
    }

    public void onSeeStatsClicked(View v){
        CapabilityToken token=capTokenStorage.getToken("/"+statServiceName);
        if(token!=null)
            requestStats(token);
        else
            requestConsent(statServiceName, "GET", AdvancedFuncSelection.this::requestStats);
    }

    public void onDiscoverDataClicked(View v) {
        CapabilityToken token=capTokenStorage.getToken("/"+dataServicesName);
        if(token!=null)
            goDiscover(token);
        else
            requestConsent(dataServicesName, "GET", AdvancedFuncSelection.this::goDiscover);
    }

    public void onConsumeDataClicked(View v) {
        CapabilityToken token=capTokenStorage.getToken("/"+dataServicesName);
        if(token!=null)
            goConsume(token);
        else
            requestConsent(dataServicesName, "GET", AdvancedFuncSelection.this::goConsume);
    }

    private void requestConsent(String sname, String actions, Consumer<CapabilityToken> method) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        try {
            Policy requestedPolicy = Utils.getPolicy(statServiceName, capManAPIService,TAG);
            builder.setTitle(R.string.identifyYourself).setMessage(Utils.generatePermissionText(this,requestedPolicy)).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //activarLoading();
                    AsyncPresentation async=new AsyncPresentation() {
                        @Override
                        public void handleGeneratePresentationResponse(Object response) {
                            //runOnUiThread(AdvancedFuncSelection.this::desactivarLoading);
                            if(response instanceof Exception){
                                Log.d(TAG,"Something went wrong when generating presentation token.",(Exception)response);
                                runOnUiThread(()-> {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(AdvancedFuncSelection.this);
                                    builder.setTitle("Credential manager error").setMessage("Could not generate presentation token: can you fulfil the policy?").setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
                                    builder.create().show();
                                });
                            } else if (response instanceof String){
                                Utils.longLog(TAG, "Generated token " + response);
                                Toast.makeText(AdvancedFuncSelection.this, "Presentation generated", Toast.LENGTH_SHORT).show();
                                runOnUiThread(()->requestCapToken(sname,(String)response,requestedPolicy.getPolicyId(),method, actions));
                            } else {
                                runOnUiThread(()-> {
                                    Log.d(TAG, "Failure token generation" + response);
                                    AlertDialog.Builder builder = new AlertDialog.Builder(AdvancedFuncSelection.this);
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
            Log.e(TAG,"Could not get policy",e);
        }
        builder.create().show();
    }

    private void requestCapToken(String service, String token, String policyId, Consumer<CapabilityToken> method, String actions){
        Log.d(TAG,"Requesting capability token "+service+ " "+actions);
        JsonObject body=new JsonObject();
        body.addProperty("ac",actions);
        body.addProperty("re","/"+service);
        body.addProperty("de",PEP_BASE_URL);
        body.addProperty("ZKTOKEN",token);
        body.addProperty("policyID",policyId);
        capManAPIService.requestCapToken(body).enqueue(new Callback<CapabilityToken>() {
            @Override
            public void onResponse(Call<CapabilityToken> call, Response<CapabilityToken> response) {
                if(response.code()!=200){
                    if(response.code()==403){
                        AlertDialog.Builder builder = new AlertDialog.Builder(AdvancedFuncSelection.this);
                        builder.setTitle("Unauthorized").setMessage("Could not retrieve capability token: Access policy not met").setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
                        builder.create().show();
                    }else{
                        AlertDialog.Builder builder = new AlertDialog.Builder(AdvancedFuncSelection.this);
                        builder.setTitle("Capability manager error").setMessage("Could not obtain capability token: Wrong response from Capability Manager").setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
                        builder.create().show();
                        Log.d(TAG,"Something went wrong while requesting CapToken: Not OK code");
                    }
                }else {
                    CapabilityToken capabilityToken=response.body();
                    Log.d(TAG,"CapTokenRequest: Response body "+response.body().toString());
                    capTokenStorage.storeToken(capabilityToken);
                    method.accept(capabilityToken);
                }
            }

            @Override
            public void onFailure(Call<CapabilityToken> call, Throwable t) {
                AlertDialog.Builder builder = new AlertDialog.Builder(AdvancedFuncSelection.this);
                builder.setTitle("Capability manager error").setMessage("Could not obtain capability token: Error contacting capability manager").setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
                builder.create().show();
                Log.d(TAG,"Something went wrong while contacting CapManager.",t);
            }
        });

}

    private void requestStats(CapabilityToken capabilityToken) {
        pepAPIService.getUserTotals(capabilityToken.getSub(),capabilityToken.toJsonString()).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                Log.d(TAG,"PEP OnResponse "+response);
                if(response.code()==200){
                    Intent intent = new Intent(AdvancedFuncSelection.this, StatsActivity.class);
                    Log.d(TAG,"Response: "+ response.body().toString());
                    intent.putExtra("data",response.body().toString());
                    startActivity(intent);
                }else if(response.code()==401){
                    AlertDialog.Builder builder = new AlertDialog.Builder(AdvancedFuncSelection.this);
                    builder.setTitle("Unauthorized").setMessage("Could not retrieve stats data: PEP denied access").setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
                    builder.create().show();
                } else{ //Other codes returned are not as relevant (internal server error/wrong request...)
                    AlertDialog.Builder builder = new AlertDialog.Builder(AdvancedFuncSelection.this);
                    builder.setTitle("PEP error").setMessage("Could not retrieve stats data: PEP response with code "+response.code()).setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
                    builder.create().show();
                }
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                AlertDialog.Builder builder = new AlertDialog.Builder(AdvancedFuncSelection.this);
                builder.setTitle("PEP error").setMessage("Could not retrieve stats data: Communication with PEP failed").setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
                builder.create().show();
                Log.d(TAG,"PEP OnFailure ",t);
            }
        });
    }

    private void goDiscover(CapabilityToken capabilityToken) {
        Intent intent = new Intent(AdvancedFuncSelection.this, DiscoverDataActivity.class);
        startActivity(intent);
    }

    private void goConsume(CapabilityToken capabilityToken) {
        Intent intent = new Intent(AdvancedFuncSelection.this, ConsumeDataActivity.class);
        startActivity(intent);
    }

    private void goManage(CapabilityToken capabilityToken) {
        pepAPIService.getEnabledServices(capabilityToken.getSub(),capabilityToken.toJsonString()).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                Log.d(TAG,"PEP OnResponse "+response);
                if(response.code()==200){
                    String enabled=response.body();
                    Log.d(TAG,"Response enabled: "+enabled);
                    chainAPIService.getServicesExtraInfo().enqueue(new Callback<Map<String, ServiceInfoModel>>() {
                        @Override
                        public void onResponse(Call<Map<String, ServiceInfoModel>> call, Response<Map<String, ServiceInfoModel>> response) {
                            Intent intent = new Intent(AdvancedFuncSelection.this, ManageServicesActivity.class);
                            intent.putExtra("enabledList",enabled);
                            intent.putExtra("servicesInfo",new Gson().toJson(response.body()));
                            startActivity(intent);
                        }

                        @Override
                        public void onFailure(Call<Map<String, ServiceInfoModel>> call, Throwable t) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(AdvancedFuncSelection.this);
                            builder.setTitle("Error").setMessage("Could not retrieve services information"+response.code()).setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
                            builder.create().show();
                        }
                    });

                }else if(response.code()==401){
                    AlertDialog.Builder builder = new AlertDialog.Builder(AdvancedFuncSelection.this);
                    builder.setTitle("Unauthorized").setMessage("Could not retrieve services data: PEP denied access").setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
                    builder.create().show();
                } else{ //Other codes returned are not as relevant (internal server error/wrong request...)
                    AlertDialog.Builder builder = new AlertDialog.Builder(AdvancedFuncSelection.this);
                    builder.setTitle("PEP error").setMessage("Could not retrieve services data: PEP response with code "+response.code()).setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
                    builder.create().show();
                }
            }
            @Override
            public void onFailure(Call<String> call, Throwable t) {
                AlertDialog.Builder builder = new AlertDialog.Builder(AdvancedFuncSelection.this);
                builder.setTitle("PEP error").setMessage("Could not retrieve stats data: Communication with PEP failed").setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
                builder.create().show();
                Log.d(TAG,"PEP OnFailure ",t);
            }
        });

    }

}