package inf.um.pilotomimurcia.olympus;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Base64;
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
import eu.olympus.client.storage.InMemoryCredentialStorage;
import eu.olympus.model.PabcPublicParameters;
import eu.olympus.server.interfaces.PestoIdP;
import eu.olympus.util.Pair;
import eu.olympus.util.multisign.MSpublicParam;
import eu.olympus.util.multisign.MSverfKey;
import eu.olympus.util.psmultisign.PSverfKey;

import static inf.um.pilotomimurcia.MenuPrincipal.PRESENTATION_LIFETIME;

public class BasicUmuIdpConfiguration implements ClientConfiguration {

    private final static String url="http://10.0.2.2:";
    private final static String urlTls="https://155.54.95.195:";
    private final static int serverCount=3;
    private final static int startingPort=9080;
    private final static int startingTlsPort=9933;
    private final static byte[] seed="istheseedrandomyesnomaybeidontknowcanyourepeatthequestion".getBytes();
    private CredentialStorage storage;
    private boolean useTls;

    public BasicUmuIdpConfiguration(CredentialStorage storage,boolean useTls) {
        this.storage = storage;
        this.useTls=useTls;
    }

    @Override
    public Pair<UserClient, CredentialManagement> createClient() throws Exception {
        List<PabcIdPRESTConnection> restIdps = new ArrayList<>();
        for(int i = 0; i< serverCount; i++) {
            String target=useTls ?   (urlTls+(startingTlsPort+i)) : (url+(startingPort+i));
            PabcIdPRESTConnection restConnection = new PabcIdPRESTConnection(target,"useless",i,1000);
            restIdps.add(restConnection);
        }
        Map<Integer, MSverfKey> publicKeys = new HashMap<>();
        ExecutorService executor= Executors.newFixedThreadPool(serverCount);
        Map<Integer, Future<MSverfKey>> futurePks=new HashMap<>();
        for (Integer j = 0; j< serverCount; j++){
            Integer finalJ = j;
            futurePks.put(j, executor.submit(() -> restIdps.get(finalJ).getPabcPublicKeyShare()));
        }
        for (Integer j = 0; j< serverCount; j++){
            publicKeys.put(j, futurePks.get(j).get());
        }
        PabcPublicParameters publicParam= restIdps.get(0).getPabcPublicParam();
        //JsonObject jsonObject = new JsonObject();
        //jsonObject.addProperty("pp",publicParam.toString());
        JsonArray pabcPks=new JsonArray();
        for (Integer j = 0; j< serverCount; j++){
            pabcPks.add(Base64.getEncoder().encodeToString(publicKeys.get(j).getEncoded()));
        }
        //jsonObject.add("pabcPks",pabcPks);
        CredentialManagement credentialManagement=new PSCredentialManagement(true, storage,PRESENTATION_LIFETIME);
        ((PSCredentialManagement)credentialManagement).setup(publicParam,publicKeys,seed);
        ClientCryptoModule cryptoModule = new SoftwareClientCryptoModule(new Random(1), ((RSAPublicKey)restIdps.get(0).getCertificate().getPublicKey()).getModulus());
        //jsonObject.addProperty("pestoModulus",((RSAPublicKey) restIdps.get(0).getCertificate().getPublicKey()).getModulus());
        //loguear("jsonObject",jsonObject.toString());
        return new Pair(new PabcClient(restIdps,credentialManagement,cryptoModule),credentialManagement);
    }

}
