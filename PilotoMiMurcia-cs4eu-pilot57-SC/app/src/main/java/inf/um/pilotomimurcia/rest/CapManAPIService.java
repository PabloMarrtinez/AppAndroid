package inf.um.pilotomimurcia.rest;


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import inf.um.pilotomimurcia.miMurcia.model.CapabilityToken;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface CapManAPIService {

    @GET("/policy/{servicePath}")
    Call<JsonElement> getPolicy(@Path("servicePath") String servicePath);

    @GET("/newsigneduid")
    Call<JsonElement> requestUid();

    @Headers("Content-Type: application/json")
    @POST("/")
    Call<CapabilityToken> requestCapToken(@Body JsonObject data);

}