package eu.olympus.credentialapp.rest;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class PresentPostModel {

        @SerializedName("token")
        @Expose
        private String token;
        @SerializedName("policyId")
        @Expose
        private String policyId;

        public PresentPostModel() {
        }

        public PresentPostModel(String token, String policyId) {
            this.token = token;
            this.policyId = policyId;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getPolicy() {
            return policyId;
        }

        public void setPolicy(String policyId) {
            this.policyId = policyId;
        }


}
