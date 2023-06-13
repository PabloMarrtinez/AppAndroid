package inf.um.pilotomimurcia;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import inf.um.pilotomimurcia.miMurcia.CapabilityTokenStorage;
import inf.um.pilotomimurcia.miMurcia.MiMurciaServiceManagementAdapter;
import inf.um.pilotomimurcia.miMurcia.ServiceInfoModel;
import inf.um.pilotomimurcia.miMurcia.model.CapabilityToken;
import inf.um.pilotomimurcia.rest.APIUtils;
import inf.um.pilotomimurcia.rest.PepAPIService;
import inf.um.pilotomimurcia.rest.models.chainmodels.Service;
import inf.um.pilotomimurcia.utils.Utils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageServicesActivity extends AppCompatActivity {

    private static final String TAG=ManageServicesActivity.class.getSimpleName();

    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private MiMurciaServiceManagementAdapter adapter;
    private Map<String,ServiceInfoModel> services;
    private List<String> serviceIds;
    private PepAPIService pepAPIService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_services);
        //TODO Get from intent (also will probably need the list of services that the user has)
        if(!getIntent().hasExtra("servicesInfo")) {
            Log.e(TAG,"Error, no service info data");
        }
        if(!getIntent().hasExtra("enabledList")) {
            Log.e(TAG,"Error, no enabled services data");
        }


        services  = new Gson().fromJson(getIntent().getExtras().getString("servicesInfo"), new TypeToken<Map<String,ServiceInfoModel>>(){}.getType());
        List<String> enabledList=new Gson().fromJson(getIntent().getExtras().getString("enabledList"), new TypeToken<List<String>>(){}.getType());
        Log.d(TAG,"IntentExtra enabledList "+ getIntent().getExtras().getString("enabledList"));

        recyclerView = findViewById(R.id.manageServicesView);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        List<ServiceInfoModel> values=new ArrayList<>(services.size());
        List<Boolean> enabled=new ArrayList<>(services.size());
        serviceIds=new ArrayList<>(services.size());
        int i=0;
        for(Map.Entry<String,ServiceInfoModel> e:services.entrySet()){
            serviceIds.add(i,e.getKey());
            values.add(i,e.getValue());
            enabled.add(i,enabledList.contains(e.getKey()));
            i++;
        }
        adapter =new MiMurciaServiceManagementAdapter(values,enabled);
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(false);
        adapter.notifyDataSetChanged();
        pepAPIService= APIUtils.getPepAPIService();

    }

    public void onCLickConfirm(View v){
        JsonArray array=new JsonArray();
        for(int i =0;i<serviceIds.size();i++)
            if(adapter.isCheckBoxChecked(i))
                array.add(serviceIds.get(i));
        CapabilityToken token=CapabilityTokenStorage.getInstance().getToken("/manageservices");
        pepAPIService.manageEnabledServices(token.getSub(),token.toJsonString(),array).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                try {
                    Log.d(TAG,"Response code"+ response.code());
                    if(response.isSuccessful()){
                        Gson gson = new Gson();
                        SharedPreferences shPre = Utils.secureSharedPreferences(ManageServicesActivity.this);
                        SharedPreferences.Editor editor=shPre.edit();
                        editor.putString("enabledServices",gson.toJson(adapter.enabledServices()));
                        editor.apply();
                        Toast.makeText(ManageServicesActivity.this,"Enabled services updated",Toast.LENGTH_SHORT).show();
                        finish();
                    }else{
                        Toast.makeText(ManageServicesActivity.this,"Could not update enabled services",Toast.LENGTH_SHORT).show();
                        Log.e(TAG,"Could not update, PEP response not OK");
                    }

                } catch (GeneralSecurityException | IOException e) {
                    Toast.makeText(ManageServicesActivity.this,"Failed to store enabled services",Toast.LENGTH_SHORT).show();
                    Log.e(TAG,"Failed to store",e);
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(ManageServicesActivity.this,"Failed to modify enabled services",Toast.LENGTH_SHORT).show();
                Log.e(TAG,"Failed to contact PEP",t);
            }
        });

    }
}