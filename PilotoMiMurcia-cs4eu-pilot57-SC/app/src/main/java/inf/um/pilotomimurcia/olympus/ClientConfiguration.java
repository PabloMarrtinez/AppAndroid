package inf.um.pilotomimurcia.olympus;

import eu.olympus.client.interfaces.CredentialManagement;
import eu.olympus.client.interfaces.UserClient;
import eu.olympus.util.Pair;

//TODO Create configurable configurations (i.e., number of IdPs, ports, TLS or not, storage...)
public interface ClientConfiguration {
    Pair<UserClient, CredentialManagement> createClient() throws Exception;
}
