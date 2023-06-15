package eu.olympus.credentialapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONArray;
import org.json.JSONException;

import eu.olympus.credentialapp.olympus.ClientSingleton;

public class Staff extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    address a = new address();
    private boolean qrReader;

    public LinearLayout rootView;

    public String publicKey;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.staff);
        rootView = findViewById(R.id.rootView);
    }

    public void onScanClick(View v) {


        // Código para manejar el evento de clic en el elemento AppCompatImageView
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
                    Toast.makeText(this.getApplicationContext(),R.string.messageExceptionQR,Toast.LENGTH_SHORT).show();
                }
            } else {
                // No credential
                AlertDialog.Builder builder = new AlertDialog.Builder(this.getApplicationContext());
                builder.setTitle(R.string.noCredential).setMessage(R.string.noCredentialDesc).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                builder.create().show();
            }
        } else {
            // Client not initialized
            AlertDialog.Builder builder = new AlertDialog.Builder(this.getApplicationContext());
            builder.setTitle(R.string.initError).setMessage(R.string.initErrorDesc);
            builder.create().show();
        }


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (qrReader) {
            IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
            if (result != null && result.getContents() != null) {
                String qrContent = result.getContents();
                Log.d(TAG, "QR SCAN: " + qrContent);
                String publicKeyValue = "";
                String[] parts = qrContent.split("publickey=");
                if (parts.length > 1) {
                    publicKeyValue = parts[1];
                }
                publicKey = publicKeyValue.substring(0, publicKeyValue.length() - 1);
                consutarEntradas(publicKey);


            } else {
                Toast.makeText(this, R.string.messageNoResultQR, Toast.LENGTH_LONG).show();
            }
            qrReader = false;
        }
    }


    public void consutarEntradas(String address){
        Log.d(TAG, "Public Key Value: " + address);
        String url = "http:/"+a.getAddress()+":4000/ConsultarTickets?direccion=" + address;
        //String url =  "http:/"+a.getAddress()+":4000/ConsultarTicketsPrueba";
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    for (int i = 0; i < response.length(); i++) {
                        JSONArray obj = response.getJSONArray(i);
                        String val1 = obj.getString(0);
                        String val2 = obj.getString(1);
                        String val3 = obj.getString(2);
                        createLayout(val1,val2,val3);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_SHORT).show();
            }
        });
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        requestQueue.add(jsonArrayRequest);
    }


    public void createLayout(String id, String name, String accion) {


        LinearLayout customLayout = new LinearLayout(this);
        customLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                400
        ));
        customLayout.setGravity(Gravity.CENTER);
        customLayout.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(50, 0, 0, 0);

        customLayout.setLayoutParams(params);

        LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(
                300,
                300
        );
        params2.setMargins(0, 0, 200, 0);

        ImageView imageView = new ImageView(this);



        if (name.equals("Duki")) imageView.setImageResource(R.drawable.concierto1);
        if (name.equals("Melendi")) imageView.setImageResource(R.drawable.concierto2);
        if (name.equals("Tini")) imageView.setImageResource(R.drawable.concierto3);
        if (name.equals("Pikeras")) imageView.setImageResource(R.drawable.concierto4);

        customLayout.addView(imageView,params2);

        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));


        buttonLayout.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);

        Button button = new Button(this);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                300,
                200
        );
        buttonParams.gravity = Gravity.END;
        button.setLayoutParams(buttonParams);
        if(accion.equals("false")){
            button.setText("Entrar");
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    entrarEvento(id, name);
                    updateLayout();

                }
            });
        }else{
            button.setText("Salir");
            button.setTextColor(getResources().getColor(R.color.red));

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    salirEvento(id, name);
                    updateLayout();

                }
            });
        }

        buttonLayout.addView(button);

        customLayout.addView(buttonLayout);

        TextView textView2 = new TextView(this);
        textView2.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1  // Peso de 1 para ocupar el espacio restante
        ));
        textView2.setGravity(Gravity.CENTER);
        textView2.setText("ID: " + id);
        customLayout.addView(textView2);

        // Añadir el LinearLayout personalizado al LinearLayout principal

        rootView.addView(customLayout);
    }


    public void entrarEvento(String id, String name){
        String url = "http:/"+a.getAddress()+":4000/entrar?id=" + id +"&event="+name;
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        RequestQueue requestQueue = Volley.newRequestQueue(this.getApplicationContext());
        requestQueue.add(jsonArrayRequest);
    }

    public void salirEvento(String id, String name){

        String url = "http:/"+a.getAddress()+":4000/salir?id=" + id +"&event="+name;
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        requestQueue.add(jsonArrayRequest);
    }

    public void updateLayout() {

        rootView.removeAllViews();
        if (rootView != null) {


            consutarEntradas(publicKey);
        }
    }
}
