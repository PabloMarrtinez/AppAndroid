package inf.um.pilotomimurcia;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;

import inf.um.pilotomimurcia.miMurcia.CapabilityTokenStorage;
import inf.um.pilotomimurcia.miMurcia.model.CapabilityToken;
import inf.um.pilotomimurcia.rest.APIUtils;
import inf.um.pilotomimurcia.rest.PepAPIService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConsumeDataActivity extends AppCompatActivity {

    private static final String TAG = ConsumeDataActivity.class.getSimpleName();
    private EditText textType,textFS,textId;
    private TextView textViewResult;
    private CapabilityToken capToken;
    private PepAPIService pepAPIService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consume_data);
        textViewResult=findViewById(R.id.textResultConsume);
        textViewResult.setMovementMethod(new ScrollingMovementMethod());
        textFS=findViewById(R.id.editTextFSconsume);
        textType=findViewById(R.id.editTextTypeConsume);
        textId=findViewById(R.id.editTextIdConsume);
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
        if(type.equals("")){
            Toast.makeText(this,R.string.entityTypeIsNeededText,Toast.LENGTH_SHORT).show();
            return;
        }
        String fs=textFS.getText().toString();
        fs=fs.equals("")?null:fs;
        String id=textId.getText().toString();
        id=id.equals("")? ".*" : id;
        consumeData(type,fs,id);
    }


    private void consumeData(String type, String fs, String id) {
        pepAPIService.getConsumeData(type,fs,id,capToken.toJsonString()).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                Log.d(TAG,"PEP OnResponse "+response);
                if(response.code()==200){
                    Log.d(TAG,"Response: "+ response.body());
                    textViewResult.setText(response.body());
                }else if(response.code()==401){
                    AlertDialog.Builder builder = new AlertDialog.Builder(ConsumeDataActivity.this);
                    builder.setTitle("Unauthorized").setMessage("Could not retrieve stats data: PEP denied access").setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
                    builder.create().show();
                } else{ //Other codes returned are not as relevant (internal server error/wrong request...)
                    AlertDialog.Builder builder = new AlertDialog.Builder(ConsumeDataActivity.this);
                    builder.setTitle("PEP error").setMessage("Could not retrieve stats data: PEP response with code "+response.code()).setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
                    builder.create().show();
                }
            }
            @Override
            public void onFailure(Call<String> call, Throwable t) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ConsumeDataActivity.this);
                builder.setTitle("PEP error").setMessage("Could not retrieve stats data: Communication with PEP failed").setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
                builder.create().show();
                Log.d(TAG,"PEP OnFailure ",t);
            }
        });
    }
}