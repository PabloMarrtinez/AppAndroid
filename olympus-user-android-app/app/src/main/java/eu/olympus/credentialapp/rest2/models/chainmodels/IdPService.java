package eu.olympus.credentialapp.rest2.models.chainmodels;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Supporting model used on the vIdPs model to reference (in the modified DID document) the IdPs that form it
 */
public class IdPService {

    /**
     * DID (id) of the IdP, same as indicated in the IdP's DID document
     */
    @JsonProperty("id")
    private final String id;

    /**
     * Endpoint for the IdP, same as indicated in the IdP's DID document
     */
    @JsonProperty("endpoint")
    private final String endpoint;

    /**
     * IdP's verification method for the p-ABC scheme, same as in the IdP element on the chain.
     */
    @JsonProperty("verificationMethod")
    private final VerificationMethod verificationMethod;


    public IdPService(@JsonProperty("id") String id,@JsonProperty("endpoint") String endpoint,
                      @JsonProperty("verificationMethod") VerificationMethod verificationMethod) {
        this.id = id;
        this.endpoint = endpoint;
        this.verificationMethod = verificationMethod;
    }

    public IdPService(IdPService idPService, VerificationMethod newVerificationMethod) {
        this.id = idPService.getId();
        this.endpoint = idPService.getEndpoint();
        this.verificationMethod = newVerificationMethod;
    }

    public String getId() {
        return id;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public VerificationMethod getVerificationMethod() {
        return verificationMethod;
    }
}
