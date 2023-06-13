package inf.um.pilotomimurcia.rest.models.chainmodels;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;

/**
 * Model for the "modified" DID document associated to a virtual IdP
 */
public final class VIDPDocument {

    /**
     * Context
     */
    @JsonProperty("@context")
    private final String context;

    /**
     * Id (DID)
     */
    @JsonProperty("id")
    private final String id;

    /**
     * List of "Services" associated, represents the list of partial IdPs that form the virtual IdP
     */
    @JsonProperty("services")
    private final ArrayList<IdPService> services;

    public VIDPDocument(String context, String id, ArrayList<IdPService> services) {
        this.context = context;
        this.id = id;
        this.services = services;
    }

    public String getContext() {
        return context;
    }

    public String getId() {
        return id;
    }

    public ArrayList<IdPService> getServices() {
        return services;
    }

    public void addService (IdPService idPService) {
        services.add(idPService);
    }

    public void updateIdP(IdPService idPService, VerificationMethod verificationMethod) {
        services.remove(idPService);
        services.add(new IdPService(idPService, verificationMethod));
    }
}
