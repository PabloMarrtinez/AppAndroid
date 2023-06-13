package eu.olympus.credentialapp.rest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class RetrofitClient {

    private static Map<String,Retrofit> clients=new HashMap<>();

    public static Retrofit getClient(String baseUrl) {
        Retrofit retrofit=clients.get(baseUrl);
        if (retrofit==null) {
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(1, TimeUnit.MINUTES)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .build();
            JsonSerializer<Date> ser = (src, typeOfSrc, context) -> src == null ? null
                    : new JsonPrimitive(src.getTime());
            JsonDeserializer<Date> deser = (jSon, typeOfT, context) -> jSon == null ? null : new Date(jSon.getAsLong());
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Date.class, ser)
                    .registerTypeAdapter(Date.class, deser).create();

            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
            clients.put(baseUrl,retrofit);
        }
        return retrofit;
    }
}
