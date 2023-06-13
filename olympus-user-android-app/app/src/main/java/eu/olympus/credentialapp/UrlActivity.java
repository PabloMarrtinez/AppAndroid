package eu.olympus.credentialapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class UrlActivity extends AppCompatActivity {

    private EditText urlField;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_url);
        urlField=findViewById(R.id.editTextUrl);
        sharedPreferences= getSharedPreferences("storagePreferences", Context.MODE_PRIVATE);
        String lastUrl=sharedPreferences.getString("lastUrl","none");
        if(!lastUrl.equals("none"))
            urlField.setText(lastUrl);
    }

    public void onClick(View v){
        String url=urlField.getText().toString().trim();
        sharedPreferences.edit().putString("lastUrl",url).apply();
        Intent data = new Intent();
        data.putExtra("url",url);
        setResult(RESULT_OK, data);
        finish();
    }
}