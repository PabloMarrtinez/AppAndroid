package inf.um.pilotomimurcia.rest.models.chainmodels;


import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model for a W3C's VC Verification Method, usually a public key associated to an id and a type (e.g. OlympusVerfKey-BLS461)
 */
public class VerificationMethod {

    /**
     * Type of the public key (e.g. OLYMPUS-BLS461). Determines information necessary to deserialize the key
     */
    @JsonProperty("type")
    private final String type;

    /**
     * Id associated to the key
     */
    @JsonProperty("id")
    private final String id;

    /**
     * Serialized public key
     */
    @JsonProperty("publicKeySerial")
    private final String publicKeySerial;

    public VerificationMethod(String type, String id, String publicKeySerial) {
        this.type = type;
        this.id = id;
        this.publicKeySerial = publicKeySerial;
    }

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public String getPublicKeySerial() {
        return publicKeySerial;
    }
}
