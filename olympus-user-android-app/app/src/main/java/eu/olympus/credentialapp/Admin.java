package eu.olympus.credentialapp;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.zxing.integration.android.IntentIntegrator;

import androidx.fragment.app.Fragment;


import org.json.JSONArray;
import org.json.JSONException;

import eu.olympus.credentialapp.olympus.ClientSingleton;


public class Admin extends Fragment {

    address a = new address();
    private boolean qrReader;
    private static final String TAG = Admin.class.getSimpleName();



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    public void createLayout(View rootView, String id, String name, String accion) {
        LinearLayout mainLayout = rootView.findViewById(R.id.main2);

        LinearLayout customLayout = new LinearLayout(getActivity());
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
        ImageView imageView = new ImageView(getActivity());
        imageView.setLayoutParams(new LinearLayout.LayoutParams(
                300,
                300
        ));

        if (name.equals("Duki")) imageView.setImageResource(R.drawable.concierto1);
        if (name.equals("Melendi")) imageView.setImageResource(R.drawable.concierto2);
        if (name.equals("Tini")) imageView.setImageResource(R.drawable.concierto3);
        if (name.equals("Pikeras")) imageView.setImageResource(R.drawable.concierto4);

        customLayout.addView(imageView);

        LinearLayout buttonLayout = new LinearLayout(getActivity());
        buttonLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));
        buttonLayout.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);

        Button button = new Button(getActivity());
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

        TextView textView2 = new TextView(getActivity());
        textView2.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1  // Peso de 1 para ocupar el espacio restante
        ));
        textView2.setGravity(Gravity.CENTER);
        textView2.setText("ID: " + id);
        customLayout.addView(textView2);

        // Añadir el LinearLayout personalizado al LinearLayout principal

        mainLayout.addView(customLayout);
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
        RequestQueue requestQueue = Volley.newRequestQueue(getActivity().getApplicationContext());
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
        RequestQueue requestQueue = Volley.newRequestQueue(getActivity().getApplicationContext());
        requestQueue.add(jsonArrayRequest);
    }


    public void consutarEntradas(View rootView, String address){
        //String url = "http:/"+a.getAddress()+":4000/ConsultarTickets?direccion=" + address;
        String url =  "http:/"+a.getAddress()+":4000/ConsultarTicketsPrueba";
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    for (int i = 0; i < response.length(); i++) {
                        JSONArray obj = response.getJSONArray(i);
                        String val1 = obj.getString(0);
                        String val2 = obj.getString(1);
                        String val3 = obj.getString(2);
                        createLayout(rootView, val1,val2,val3);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getActivity().getApplicationContext(), "Error", Toast.LENGTH_SHORT).show();
            }
        });
        RequestQueue requestQueue = Volley.newRequestQueue(getActivity().getApplicationContext());
        requestQueue.add(jsonArrayRequest);
    }


    public void consultarEventos(){

        String url =  "http:/"+a.getAddress()+":4000/ConsultarEventosPrueba";
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    for (int i = 0; i < response.length(); i++) {
                        JSONArray obj = response.getJSONArray(i);
                        String val1 = obj.getString(0);
                        String val2 = obj.getString(1);
                        boolean val3 = obj.getBoolean(2);
                        boolean val4 = obj.getBoolean(3);
                        String val5 = obj.getString(4);
                        String val6 = obj.getString(5);
                        String val7 = obj.getString(6);
                        String val8 = obj.getString(7);
                        String val9 = obj.getString(8);

                        // Aquí puedes hacer operaciones con los valores
                        // ...

                        // Imprimir los valores en la consola
                        Log.d("TAG", "Valor 1: " + val1);
                        Log.d("TAG", "Valor 2: " + val2);
                        Log.d("TAG", "Valor 3: " + val3);
                        Log.d("TAG", "Valor 4: " + val4);
                        Log.d("TAG", "Valor 5: " + val5);
                        Log.d("TAG", "Valor 6: " + val6);
                        Log.d("TAG", "Valor 7: " + val7);
                        Log.d("TAG", "Valor 8: " + val8);
                        Log.d("TAG", "Valor 9: " + val9);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getActivity().getApplicationContext(), "Error al realizar la consulta", Toast.LENGTH_SHORT).show();
            }
        });
        RequestQueue requestQueue = Volley.newRequestQueue(getActivity().getApplicationContext());
        requestQueue.add(jsonArrayRequest);




    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_admin, container, false);
        String address = "0xA9E38736227E96CE380B5061949f8c61D2015c5d";
        consutarEntradas(rootView, address);
        ImageView imageView = rootView.findViewById(R.id.imageView3);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onScanClick(v);
            }
        });
        return rootView;
    }

    public void updateLayout() {

        View rootView = getView(); // Obtener la vista raíz del fragmento
        LinearLayout mainLayout = rootView.findViewById(R.id.main2);
        mainLayout.removeAllViews();
        if (rootView != null) {
            String address = "0xA9E38736227E96CE380B5061949f8c61D2015c5d";
            consutarEntradas(rootView, address);
        }
    }

/*

    public void onScanClick(View v) {
        if (ClientSingleton.isInitialized()) {
            if (ClientSingleton.getCredentialManager().checkStoredCredential()) {
                //QR scanner
                try {
                    qrReader = true;
                    IntentIntegrator integrator = new IntentIntegrator(getActivity());
                    integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
                    integrator.setPrompt("Scan a QR");
                    integrator.setBeepEnabled(false);
                    integrator.initiateScan();
                } catch (Exception e) {
                    Log.d(TAG,"Exception when launching QR scanner", e);
                    Toast.makeText(getActivity().getApplicationContext(),R.string.messageExceptionQR,Toast.LENGTH_SHORT).show();
                }
            } else {
                // No credential
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity().getApplicationContext());
                builder.setTitle(R.string.noCredential).setMessage(R.string.noCredentialDesc).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                builder.create().show();
            }
        } else {
            // Client not initialized
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity().getApplicationContext());
            builder.setTitle(R.string.initError).setMessage(R.string.initErrorDesc);
            builder.create().show();
        }
    }

 */

    public void onScanClick(View v) {
        // Código para manejar el evento de clic en el elemento AppCompatImageView
        if (ClientSingleton.isInitialized()) {
            if (ClientSingleton.getCredentialManager().checkStoredCredential()) {
                //QR scanner
                try {
                    qrReader = true;
                    IntentIntegrator integrator = new IntentIntegrator(getActivity());
                    integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
                    integrator.setPrompt("Scan a QR");
                    integrator.setBeepEnabled(false);
                    integrator.initiateScan();
                } catch (Exception e) {
                    Log.d(TAG,"Exception when launching QR scanner", e);
                    Toast.makeText(getActivity().getApplicationContext(),R.string.messageExceptionQR,Toast.LENGTH_SHORT).show();
                }
            } else {
                // No credential
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity().getApplicationContext());
                builder.setTitle(R.string.noCredential).setMessage(R.string.noCredentialDesc).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                builder.create().show();
            }
        } else {
            // Client not initialized
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity().getApplicationContext());
            builder.setTitle(R.string.initError).setMessage(R.string.initErrorDesc);
            builder.create().show();
        }
    }






}