package eu.olympus.credentialapp;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import eu.olympus.credentialapp.olympus.ClientSingleton;


public class Admin extends Fragment {

    address a = new address();
    private boolean qrReader;
    private static final String TAG = Admin.class.getSimpleName();



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


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
        //consutarEntradas(rootView, address);
        Button button = rootView.findViewById(R.id.button4);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAdminClick(v);
            }
        });
        return rootView;
    }







public void onAdminClick(View v){
    Intent i = new Intent(getActivity(), Staff.class);
    startActivity(i);
}




}