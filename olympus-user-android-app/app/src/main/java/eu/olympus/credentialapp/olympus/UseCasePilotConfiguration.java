package eu.olympus.credentialapp.olympus;

import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import eu.olympus.client.PSCredentialManagement;
import eu.olympus.client.PabcClient;
import eu.olympus.client.PabcIdPRESTConnection;
import eu.olympus.client.PestoIdPRESTConnection;
import eu.olympus.client.SoftwareClientCryptoModule;
import eu.olympus.client.interfaces.ClientCryptoModule;
import eu.olympus.client.interfaces.CredentialManagement;
import eu.olympus.client.interfaces.CredentialStorage;
import eu.olympus.client.interfaces.UserClient;
import eu.olympus.model.PabcPublicParameters;
import eu.olympus.server.interfaces.PabcIdP;
import eu.olympus.server.interfaces.PestoIdP;
import eu.olympus.util.Pair;
import eu.olympus.util.multisign.MSpublicParam;
import eu.olympus.util.multisign.MSverfKey;

/**
 * Basic class for configuring the client for the example use case scenario. The url must be modified
 * to point to addresses where the IdPs are deployed
 */
public class UseCasePilotConfiguration implements ClientConfiguration {
    private final static int rateLimit = 1000;
    private final long presentationLifetime = 60; //In seconds
    private final CredentialStorage storage;
    private HashMap<Integer, String> urlMap;

    public UseCasePilotConfiguration(CredentialStorage storage) {
        this.storage=storage;
        urlMap=new HashMap<>();
        //TODO These IPs must be changed depending on deployment
        urlMap.put(0,"http://10.0.2.2:9080");
        urlMap.put(1,"http://10.0.2.2:9081");
        urlMap.put(2,"http://10.0.2.2:9082");
    }

    @Override
    public Pair<UserClient, Pair<CredentialManagement,CredentialStorage>> createClient() throws Exception {
        List<PabcIdP> restIdps = new ArrayList<>();
        for (int serverId: this.urlMap.keySet()) {
            PabcIdPRESTConnection restConnection = new PabcIdPRESTConnection(this.urlMap.get(serverId), "", serverId, rateLimit);
            restIdps.add(restConnection);
        }
        Map<Integer, MSverfKey> publicKeys = new HashMap<>();
        ExecutorService executor = Executors.newFixedThreadPool(this.urlMap.size());
        Map<Integer, Future<MSverfKey>> futurePks = new HashMap<>();
        for (int serverId: this.urlMap.keySet()) {
            futurePks.put(serverId, executor.submit(() -> restIdps.get(serverId).getPabcPublicKeyShare()));
        }
        for (int serverId: this.urlMap.keySet()) {
            publicKeys.put(serverId, futurePks.get(serverId).get());
        }
        PabcPublicParameters publicParam = restIdps.get(0).getPabcPublicParam();
        SecureRandom random = new SecureRandom();
        CredentialManagement credentialManagement = new PSCredentialManagement(true, storage);
        ((PSCredentialManagement) credentialManagement).setup(publicParam, publicKeys, random.generateSeed(32));
        ClientCryptoModule cryptoModule = new SoftwareClientCryptoModule(new Random(1), ((RSAPublicKey) restIdps.get(0).getCertificate().getPublicKey()).getModulus());
        return new Pair(new PabcClient(restIdps, credentialManagement, cryptoModule), new Pair<>(credentialManagement,storage));
    }
}
