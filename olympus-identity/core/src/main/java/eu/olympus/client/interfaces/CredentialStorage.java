package eu.olympus.client.interfaces;

import VCModel.VerifiableCredential;
import eu.olympus.model.PSCredential;

public interface CredentialStorage {

    void storeCredential(VerifiableCredential credential);

    VerifiableCredential getCredential();

    boolean checkCredential();

    void deleteCredential();
}
