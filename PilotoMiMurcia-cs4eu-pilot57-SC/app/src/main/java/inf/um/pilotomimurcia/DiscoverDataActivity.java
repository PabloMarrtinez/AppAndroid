package inf.um.pilotomimurcia;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import inf.um.pilotomimurcia.miMurcia.CapabilityTokenStorage;
import inf.um.pilotomimurcia.miMurcia.model.CapabilityToken;
import inf.um.pilotomimurcia.rest.APIUtils;
import inf.um.pilotomimurcia.rest.PepAPIService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DiscoverDataActivity extends AppCompatActivity {

    private static final String TAG = DiscoverDataActivity.class.getSimpleName();
    private EditText textType,textFS;
    private TextView textViewResult;
    private PepAPIService pepAPIService;
    private CapabilityToken capToken;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discover_data);
        textViewResult=findViewById(R.id.textResultDiscovery);
        textViewResult.setMovementMethod(new ScrollingMovementMethod());
        textFS=findViewById(R.id.editTextFiwareService);
        textType=findViewById(R.id.editTextType);
        CapabilityTokenStorage capTokenStorage= CapabilityTokenStorage.getInstance();
        capToken=capTokenStorage.getToken("/data");
        if(capToken==null){
            Toast.makeText(this,R.string.capTokenIsNeededText,Toast.LENGTH_SHORT).show();
            finish();
        }
        pepAPIService= APIUtils.getPepAPIService();
    }

    public void onClick(View v){
        String type=textType.getText().toString();
        type=type.equals("")?null:type;
        String fs=textFS.getText().toString();
        fs=fs.equals("")?null:fs;
        discoverData(type,fs);
    }


    private void discoverData(String type, String fs) {
        pepAPIService.getDiscoverData(type,fs,capToken.toJsonString()).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                Log.d(TAG,"PEP OnResponse "+response);
                if(response.code()==200){
                    Log.d(TAG,"Response: "+ response.body());
                    textViewResult.setText(response.body());
                }else if(response.code()==401){
                    AlertDialog.Builder builder = new AlertDialog.Builder(DiscoverDataActivity.this);
                    builder.setTitle("Unauthorized").setMessage("Could not retrieve stats data: PEP denied access").setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
                    builder.create().show();
                } else{ //Other codes returned are not as relevant (internal server error/wrong request...)
                    AlertDialog.Builder builder = new AlertDialog.Builder(DiscoverDataActivity.this);
                    builder.setTitle("PEP error").setMessage("Could not retrieve stats data: PEP response with code "+response.code()).setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
                    builder.create().show();
                }
            }
            @Override
            public void onFailure(Call<String> call, Throwable t) {
                AlertDialog.Builder builder = new AlertDialog.Builder(DiscoverDataActivity.this);
                builder.setTitle("PEP error").setMessage("Could not retrieve stats data: Communication with PEP failed").setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
                builder.create().show();
                Log.d(TAG,"PEP OnFailure ",t);
            }
        });
    }

}