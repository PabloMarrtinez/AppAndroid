package eu.olympus.credentialapp.rest;

import eu.olympus.model.Policy;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface ServiceAPI {

    @GET("/getPolicy") // Path may change to /policy/{servicePath}
    Call<String> getPolicy();

    @Headers("Content-Type: application/json")
    @POST("/present")
    Call<Boolean> presentToken(@Body PresentPostModel data);
}
