package eu.olympus.credentialapp;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import androidx.fragment.app.Fragment;


import org.json.JSONArray;
import org.json.JSONException;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Ticket#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Ticket extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    address a = new address();
    public Ticket() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment Ticket.
     */
    // TODO: Rename and change types and number of parameters
    public static Ticket newInstance(String param1, String param2) {
        Ticket fragment = new Ticket();
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

    public void createLayout(View rootView, String id, String name){
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

        if(name.equals("Duki")) imageView.setImageResource(R.drawable.concierto1);
        if(name.equals("Melendi")) imageView.setImageResource(R.drawable.concierto2);
        if(name.equals("Tini")) imageView.setImageResource(R.drawable.concierto3);
        if(name.equals("Pikeras")) imageView.setImageResource(R.drawable.concierto4);



        customLayout.addView(imageView);

        TextView textView1 = new TextView(getActivity());
        textView1.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1  // Peso de 1 para ocupar el espacio restante
        ));
        textView1.setGravity(Gravity.CENTER);
        textView1.setText(name);
        customLayout.addView(textView1);

        TextView textView2 = new TextView(getActivity());
        textView2.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1  // Peso de 1 para ocupar el espacio restante
        ));
        textView2.setGravity(Gravity.CENTER);
        textView2.setText("ID: "+id);
        customLayout.addView(textView2);

        // Añadir el LinearLayout personalizado al LinearLayout principal
        LinearLayout mainLayout = rootView.findViewById(R.id.main);
        mainLayout.addView(customLayout);



    }

    public void consutarEntradas(View rootView, String address){
        String url = "http:/"+a.getAddress()+":4000/ConsultarTickets?direccion=" + address;
        //String url = "http:/"+a.getAddress()+":4000/ConsultarTicketsPrueba";
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    for (int i = 0; i < response.length(); i++) {
                        JSONArray obj = response.getJSONArray(i);
                        String val1 = obj.getString(0);
                        String val2 = obj.getString(1);
                        createLayout(rootView, val1,val2);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        });
        RequestQueue requestQueue = Volley.newRequestQueue(getActivity().getApplicationContext());
        requestQueue.add(jsonArrayRequest);
    }


    public void consultarEventos(){

        String url = "http:/"+a.getAddress()+":4000/ConsultarEventosPrueba";
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
        View rootView = inflater.inflate(R.layout.fragment_ticket, container, false);
        String address = MySingleton.getInstance().getPublicKey();
        consutarEntradas(rootView, address);
        return rootView;
    }
}