package eu.olympus.credentialapp.olympus;

import eu.olympus.client.interfaces.CredentialManagement;
import eu.olympus.client.interfaces.CredentialStorage;
import eu.olympus.client.interfaces.UserClient;
import eu.olympus.server.interfaces.Storage;
import eu.olympus.util.Pair;

public class ClientSingleton {

    private static UserClient client;
    private static CredentialManagement credentialManagement;
    private static CredentialStorage storage;

    public static void initialize(ClientConfiguration config) throws Exception {
        if(client!=null)
            throw new IllegalStateException("Method initialize must be called only once");
        Pair<UserClient,Pair<CredentialManagement, CredentialStorage>> res=config.createClient();
        client=res.getFirst();
        credentialManagement=res.getSecond().getFirst();
        storage=res.getSecond().getSecond();
    }

    public static UserClient getInstance(){
        if(client==null)
            throw new IllegalStateException("Method initialize must be successfully completed before getting an instance");
        return client;
    }

    //Necessary for checking whether a credential is stored
    public static CredentialManagement getCredentialManager(){
        if(client==null)
            throw new IllegalStateException("Method initialized must be successfully completed before getting an instance");
        return credentialManagement;
    }

    public static void deleteStoredCredential(){
        if(client==null)
            throw new IllegalStateException("Method initialized must be successfully completed before getting an instance");
        storage.deleteCredential();
    }

    public static boolean isInitialized() {
        return client!=null;
    }
}
