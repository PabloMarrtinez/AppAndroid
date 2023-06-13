package inf.um.pilotomimurcia.rest.models.chainmodels;


import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Supporting model for the DID document
 */
public class Service {

    @JsonProperty("type")
    private final String type;

    @JsonProperty("type")
    private final String serviceEndpoint;

    public Service(String type, String serviceEndpoint) {
        this.type = type;
        this.serviceEndpoint = serviceEndpoint;
    }

    public String getType() {
        return type;
    }

    public String getServiceEndpoint() {
        return serviceEndpoint;
    }
}
