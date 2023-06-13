package inf.um.pilotomimurcia.olympus;

import android.content.Context;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import eu.olympus.client.PSCredentialManagement;
import eu.olympus.client.PabcClient;
import eu.olympus.client.PabcIdPRESTConnection;
import eu.olympus.client.PestoIdPRESTConnection;
import eu.olympus.client.SoftwareClientCryptoModule;
import eu.olympus.client.interfaces.ClientCryptoModule;
import eu.olympus.client.interfaces.CredentialManagement;
import eu.olympus.client.interfaces.CredentialStorage;
import eu.olympus.client.interfaces.UserClient;
import eu.olympus.server.interfaces.PestoIdP;
import eu.olympus.util.Pair;
import eu.olympus.util.multisign.MSverfKey;
import eu.olympus.util.psmultisign.PSverfKey;
import inf.um.pilotomimurcia.R;

import static inf.um.pilotomimurcia.MenuPrincipal.PRESENTATION_LIFETIME;

public class BasicPilotOfflineIdpConfiguration implements ClientConfiguration {

    private final static String url="http://ppissuer.inf.um.es:";
    private final static String urlTls="https://ppissuer.inf.um.es:";
    private final static int serverCount=3;
    private final static int startingPort=9080;
    private final static int startingTlsPort=9933;
    private final static byte[] seed="istheseedrandomyesnomaybeidontknowcanyourepeatthequestion".getBytes();
    private CredentialStorage storage;
    private boolean useTls;
    private Context context;


    public BasicPilotOfflineIdpConfiguration(CredentialStorage storage, boolean useTls, Context context) {
        this.storage = storage;
        this.useTls=useTls;
        this.context=context;
    }

    @Override
    public Pair<UserClient, CredentialManagement> createClient() throws Exception {
        List<PabcIdPRESTConnection> restIdps = new ArrayList<>();
        for(int i = 0; i< serverCount; i++) {
            String target=useTls ?   (urlTls+(startingTlsPort+i)) : (url+(startingPort+i));
            PabcIdPRESTConnection restConnection = new PabcIdPRESTConnection(target,"useless",i,1000);
            restIdps.add(restConnection);
        }
        InputStream is = context.getResources().openRawResource(R.raw.vidp_publicparam);
        Scanner s = new Scanner(is).useDelimiter("\\A");
        String json = s.hasNext() ? s.next() : "";
        JsonObject ppJson=JsonParser.parseString(json).getAsJsonObject();
        JsonArray pabcPksJson=ppJson.getAsJsonArray("pabcPks");
        Map<Integer, MSverfKey> publicKeys = new HashMap<>();
        //PabcPublicParameters publicParam= new PabcPublicParameters(ppJsppJson.get("pp").getAsString());
        for (int j = 0; j< serverCount; j++){
            String encodedKey=pabcPksJson.get(j).getAsString();
            publicKeys.put(j,new PSverfKey(Base64.getDecoder().decode(encodedKey)));
        }
        CredentialManagement credentialManagement=new PSCredentialManagement(true, storage,PRESENTATION_LIFETIME);
        //((PSCredentialManagement)credentialManagement).setup(publicParam,publicKeys,seed);
        ClientCryptoModule cryptoModule = new SoftwareClientCryptoModule(new Random(1), ppJson.get("pestoModulus").getAsBigInteger());
        return new Pair(new PabcClient(restIdps,credentialManagement,cryptoModule),credentialManagement);
    }

}
