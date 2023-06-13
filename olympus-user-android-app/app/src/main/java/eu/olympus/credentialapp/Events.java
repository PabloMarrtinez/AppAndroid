package eu.olympus.credentialapp;

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

import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import eu.olympus.cfp.model.TestIdentityProof;
import eu.olympus.credentialapp.olympus.ClientSingleton;
import eu.olympus.credentialapp.utils.AsyncOperations;
import eu.olympus.model.Attribute;
import eu.olympus.model.Operation;
import eu.olympus.model.Policy;
import eu.olympus.model.Predicate;

import eu.olympus.credentialapp.address;
import eu.olympus.credentialapp.MySingleton;
public class Events extends Fragment {
    address a = new address();
    private static final String TAG = Events.class.getSimpleName();
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    public Events() {
        // Required empty public constructor
    }


    public static Events newInstance(String param1, String param2) {
        Events fragment = new Events();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    public void createLayout(View rootView, String name, String fecha) {
        LinearLayout mainLayout = rootView.findViewById(R.id.mainlayout);

        TextView textView2 = new TextView(getActivity());
        textView2.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                100
        ));

        textView2.setText(name);
        textView2.setTextSize(20);
        textView2.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        textView2.setTextColor(getResources().getColor(R.color.black));
        mainLayout.addView(textView2);

        LinearLayout customLayout = new LinearLayout(getActivity());
        customLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                300
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



        TextView textView3 = new TextView(getActivity());
        textView3.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1  // Peso de 1 para ocupar el espacio restante
        ));
        textView3.setGravity(Gravity.CENTER);
        textView3.setText(fecha);
        customLayout.addView(textView3);

        LinearLayout buttonLayout = new LinearLayout(getActivity());
        buttonLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));
        buttonLayout.setGravity(Gravity.CENTER_VERTICAL);

        Button button = new Button(getActivity());
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                300,
                200
        );
        buttonParams.gravity = Gravity.END;
        button.setLayoutParams(buttonParams);

            button.setText("Entrar");
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    getPolicy(name);

                }
            });
        buttonLayout.addView(button);

        customLayout.addView(buttonLayout);
        // Añadir el LinearLayout personalizado al LinearLayout principal

        mainLayout.addView(customLayout);


    }

    public void getPolicy(String name) {

        String url = "http:/"+a.getAddress()+":4000/Politica" + name;
        Log.d(TAG, url);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    List<Predicate> predicates = new ArrayList<>();

                    JSONArray predicatesArray = response.getJSONArray("predicates");
                    for (int i = 0; i < predicatesArray.length(); i++) {
                        JSONObject predicateObject = predicatesArray.getJSONObject(i);
                        String attributeName = predicateObject.getString("attributeName");
                        Operation operation = Operation.REVEAL;
                        // Parse and extract other properties as needed

                        Predicate predicate = new Predicate(attributeName, operation);
                        // Set other properties of the Predicate as needed

                        predicates.add(predicate);
                    }

                    String policyId = response.getString("policyId");
                    Log.d(TAG, "Policy: " + policyId.toString());
                    Policy policy = new Policy(predicates, policyId);

                    obtenerCredendial(policy);



                } catch (JSONException e) {
                    Log.e(TAG, "Error al procesar la respuesta JSON: " + e.getMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Error en la solicitud: " + error.toString());
            }
        });
        RequestQueue requestQueue = Volley.newRequestQueue(getActivity().getApplicationContext());
        requestQueue.add(jsonObjectRequest);
    }

    public void obtenerCredendial(Policy p){


        Log.d(TAG, "getCredential");
// user y pass

        AsyncOperations async= new AsyncOperations() {
            @Override
            public void handleProofIdentityResponse(Object response,int what) {
                Log.d(TAG,"Unreachable reached");
                throw new RuntimeException("Should never reach here");
            }

            @Override
            public void handleRegisterResponse(Object response) {
                Log.d(TAG,"Unreachable reached");
                throw new RuntimeException("Should never reach here");
            }

            @Override
            public void handleLoginResponse(Object response) {

                if(response instanceof Exception){
                    Log.d(TAG,"Something went wrong when getting a credential in login", (Throwable) response);

                }else if(response.equals("Failed")){
                    Log.d(TAG, "Unsuccessful authentication");
                    //runOnUiThread(()->failLogin.setVisibility(View.VISIBLE));
                }
                else{
                    Log.d(TAG,"Successfully retrieved a credential: "+
                            ClientSingleton.getCredentialManager().checkStoredCredential());

                }
            }
        };
        String user = MySingleton.getInstance().getUser();
        String pass = MySingleton.getInstance().getUser();

        Log.d(TAG,user);
        Log.d(TAG,pass);

        async.doAsyncLogin(user,pass,p);
    }


    public void consutarEventos(View rootView){
        String url =  "http:/"+a.getAddress()+":4000/ConsultarEventosPrueba";
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    for (int i = 0; i < response.length(); i++) {
                        JSONArray obj = response.getJSONArray(i);
                        String nombre = obj.getString(7);
                        String fecha = "09/11/2024";
                        createLayout(rootView, nombre,fecha);
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_events, container, false);
        consutarEventos(rootView);

        return rootView;


    }
}