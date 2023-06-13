package eu.olympus.credentialapp.rest2;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface PepAPIService {

    @GET("/{servicePath}")
    Call<String> getServicePage(@Path("servicePath") String servicePath, @Header("x-auth-token") String capabilityToken);

    @POST("/{servicePath}")
    Call<String> postServicePage(@Path("servicePath") String servicePath, @Header("x-auth-token") String capabilityToken, @Body String eventJSON);

    @GET("/stats/userTotals")
    Call<JsonObject> getUserTotals(@Query("uid") String uid, @Header("x-auth-token") String capabilityToken);

    @GET("/stats/serviceStats")
    Call<JsonObject> getServiceStats(@Query("uid") String uid,@Query("service") String service, @Header("x-auth-token") String capabilityToken);

    @GET("/data/discover")
    Call<String> getDiscoverData(@Query("type") String type,@Query("fs") String fs, @Header("x-auth-token") String capabilityToken);

    @GET("/data/consume")
    Call<String> getConsumeData(@Query("type") String type,@Query("fs") String fs, @Query("id") String id, @Header("x-auth-token") String capabilityToken);

    @GET("/manageservices")
    Call<String> getEnabledServices(@Query("uid") String uid, @Header("x-auth-token") String capabilityToken);


    @POST("/manageservices")
    Call<Void> manageEnabledServices(@Query("uid") String uid, @Header("x-auth-token") String capabilityToken, @Body JsonArray data);
}