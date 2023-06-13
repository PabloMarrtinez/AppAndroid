package inf.um.pilotomimurcia.olympus;

import eu.olympus.client.interfaces.CredentialManagement;
import eu.olympus.client.interfaces.UserClient;
import eu.olympus.util.Pair;

public class ClientSingleton {
    private static UserClient client;
    private static CredentialManagement credentialManagement;
    
    public static void initialize(ClientConfiguration config) throws Exception {
        if(client!=null)
            throw new IllegalStateException("Method initializae must be called only once");
        Pair<UserClient,CredentialManagement> res=config.createClient();
        client=res.getFirst();
        credentialManagement=res.getSecond();
    }

    public static UserClient getInstance(){
        if(client==null)
            throw new IllegalStateException("Method initialize must be succesfully completed before getting an instance");
        return client;
    }

    //Necessary for checking whether a credential is stored (unless we change a bit the OL implementation)
    public static CredentialManagement getCredentialManager(){
        if(client==null)
            throw new IllegalStateException("Method initialized must be succesfully completed before getting an instance");
        return credentialManagement;
    }

    public static boolean isInitialized() {
        return client!=null;
    }
}
