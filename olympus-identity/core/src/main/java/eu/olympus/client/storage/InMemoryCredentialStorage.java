package eu.olympus.client.storage;

import VCModel.VerifiableCredential;
import eu.olympus.client.interfaces.CredentialStorage;
import eu.olympus.model.PSCredential;

public class InMemoryCredentialStorage implements CredentialStorage {


    private VerifiableCredential currentVCredential;


    @Override
    public void storeCredential(VerifiableCredential credential) {
        currentVCredential = credential;
    }


    @Override
    public VerifiableCredential getCredential() {
        return currentVCredential;
    }

    @Override
    public boolean checkCredential() {
        if (currentVCredential == null)
            return false;
        if (currentVCredential.getExpirationDate().getTime() < System.currentTimeMillis()) {
            deleteCredential();
            return false;
        }
        return true;
    }

    @Override
    public void deleteCredential() {
        currentVCredential = null;
    }
}
