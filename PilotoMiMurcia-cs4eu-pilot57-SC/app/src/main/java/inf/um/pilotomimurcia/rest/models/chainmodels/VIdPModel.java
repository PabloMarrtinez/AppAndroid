package inf.um.pilotomimurcia.rest.models.chainmodels;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This is the model for a virtual IdP element on the chain
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class VIdPModel {

    /**
     * docType property, used to distinguish types of elements on the chain
     */
    @JsonProperty("docType")
    private final String docType;

    /**
     * Did (document, modified) associated to this vIdP
     */
    @JsonProperty("did")
    private final VIDPDocument did;

    /**
     * Spawn date of the IdP (automatically computed, RFC 3339 format)
     */
    @JsonProperty("spawnDate")
    private final String spawnDate;

    /**
     * Last modification date of the vIdP (automatically computed, RFC 3339 format)
     */
    @JsonProperty("lastModificationDate")
    private String lastModificationDate;

    /**
     * Status of the IdP: Status of the IdP: ACTIVE or INACTIVE
     */
    @JsonProperty("status")
    private final String status;

    /**
     * Verification method for the vIdP's p-ABC scheme (aggregated public key, automatically computed)
     */
    @JsonProperty("verificationMethod")
    private final VerificationMethod verificationMethod;

    /**
     * Id (DID) of the schema (pp and credSchema) associated to this vIdP
     */
    @JsonProperty("schemaId")
    private final String schemaId;

    /**
     * Domain in which the vIdP is working. It can be used to separate vIdPs for different uses of the chain.
     * For instance, domain=usabilityPilot, domain=cs4eu57Pilot
     */
    @JsonProperty("domain")
    private final String domain;

    /**
     * Alias of the vIdP for easy naming
     */
    @JsonProperty("alias")
    private final String alias;

    public VIdPModel(String docType, VIDPDocument did, String spawnDate, String lastModificationDate, String status, VerificationMethod verificationMethod, String schemaId, String domain, String alias) {
        this.docType = docType;
        this.did = did;
        this.spawnDate = spawnDate;
        this.lastModificationDate = lastModificationDate;
        this.status = status;
        this.verificationMethod = verificationMethod;
        this.schemaId = schemaId;
        this.domain = domain;
        this.alias = alias;
    }

    public String getDocType() {
        return docType;
    }

    public VIDPDocument getDid() {
        return did;
    }

    public String getSpawnDate() {
        return spawnDate;
    }

    public String getStatus() {
        return status;
    }

    public VerificationMethod getVerificationMethod() {
        return verificationMethod;
    }

    public void addService(IdPService idPService) {
        did.addService(idPService);
    }

    public void updateLastModifcationTime(String newLastModificationDate){
        this.lastModificationDate=newLastModificationDate;
    }

    public String getSchemaId() {
        return schemaId;
    }

    public String getDomain() {
        return domain;
    }

    public String getAlias() {
        return alias;
    }

    public String getLastModificationDate() {
        return lastModificationDate;
    }
}

