package inf.um.pilotomimurcia.rest.models.chainmodels;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model for a DID document
 */

public final class DIDDocument {

    @JsonProperty("@context")
    private final String context;

    @JsonProperty("id")
    private final String id;

    @JsonProperty("service")
    private final Service service;

    public DIDDocument(String context, String id, Service service) {
        this.context = context;
        this.id = id;
        this.service = service;
    }

    public String getContext() {
        return context;
    }

    public String getId() {
        return id;
    }

    public Service getService() {
        return service;
    }
}
