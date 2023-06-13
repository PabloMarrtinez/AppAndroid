package inf.um.pilotomimurcia.rest.models.chainmodels;


import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model for a scheme needed by an OLYMPUS vIdP, including public parameters and W3C's VC credential schema
 */
public final class SchemaModel {

    /**
     * docType property, used to distinguish types of elements on the chain
     */
    @JsonProperty("docType")
    private final String docType;

    /**
     * DID document of the schema
     */
    @JsonProperty("did")
    private final DIDDocument did;

    /**
     * Serialization of the public parameters for an IdP (i.e., p-ABC schema info, attribute definitions...). It is expected to
     * be a serialized JSON with two fields: type (e.g. OLYMPUS-pp) and data (e.g., public parameters serialized with OLYMPUS custom format)
     */
    @JsonProperty("pparameters")
    private final String pparameters;

    /**
     * Serialization of the credential schema as defined in W3C's Verifiable Credentials specification (JSON-LD).
     */
    @JsonProperty("credSchema")
    private final String credSchema;


    public SchemaModel(String docType, DIDDocument did, String pparameters, String credSchema) {
        this.docType = docType;
        this.did = did;
        this.pparameters = pparameters;
        this.credSchema = credSchema;
    }

    public DIDDocument getDid() {
        return did;
    }

    public String getPparameters() {
        return pparameters;
    }

    public String getCredSchema() {
        return credSchema;
    }

    public String getDocType() {
        return docType;
    }
}