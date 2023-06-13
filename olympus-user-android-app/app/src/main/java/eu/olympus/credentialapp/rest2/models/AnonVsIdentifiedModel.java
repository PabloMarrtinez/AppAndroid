package eu.olympus.credentialapp.rest2.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class AnonVsIdentifiedModel {

    @SerializedName("identified")
    @Expose
    private Integer identified;
    @SerializedName("anonymous")
    @Expose
    private Integer anonymous;

    public Integer getIdentified() {
        return identified;
    }

    public void setIdentified(Integer identified) {
        this.identified = identified;
    }

    public Integer getAnonymous() {
        return anonymous;
    }

    public void setAnonymous(Integer anonymous) {
        this.anonymous = anonymous;
    }
}
