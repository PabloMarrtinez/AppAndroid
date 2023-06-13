package eu.olympus.credentialapp.olympus;

import eu.olympus.client.interfaces.CredentialManagement;
import eu.olympus.client.interfaces.CredentialStorage;
import eu.olympus.client.interfaces.UserClient;
import eu.olympus.util.Pair;

public interface ClientConfiguration {
    Pair<UserClient, Pair<CredentialManagement, CredentialStorage>> createClient() throws Exception;
}