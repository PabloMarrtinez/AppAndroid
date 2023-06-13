package inf.um.pilotomimurcia.olympus;

import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import eu.olympus.client.PSCredentialManagement;
import eu.olympus.client.PabcClient;
import eu.olympus.client.PabcIdPRESTConnection;
import eu.olympus.client.SoftwareClientCryptoModule;
import eu.olympus.client.interfaces.ClientCryptoModule;
import eu.olympus.client.interfaces.CredentialManagement;
import eu.olympus.client.interfaces.CredentialStorage;
import eu.olympus.client.interfaces.UserClient;
import eu.olympus.model.PabcPublicParameters;
import eu.olympus.util.Pair;
import eu.olympus.util.multisign.MSverfKey;
import eu.olympus.util.psmultisign.PSverfKey;
import inf.um.pilotomimurcia.rest.APIUtils;
import inf.um.pilotomimurcia.rest.ChainAPIService;
import inf.um.pilotomimurcia.rest.models.chainmodels.VIdPModel;
import inf.um.pilotomimurcia.utils.Utils;
import shaded.org.bouncycastle.util.encoders.Base64;


import static inf.um.pilotomimurcia.MenuPrincipal.PRESENTATION_LIFETIME;


public class BasicLedgerConfiguration implements ClientConfiguration {

    private final static String TAG=BasicLedgerConfiguration.class.getSimpleName();
    private final static String didVIdp="did:OL-vIdP:cs4eu:task57pilot:bastion:finalversion";
    private final static byte[] seed="istheseedrandomyesnomaybeidontknowcanyourepeatthequestion".getBytes();
    private static String adminCookie="uselessCookie";

    private CredentialStorage storage;
    private boolean useTls;
    private ChainAPIService apiService = APIUtils.getChainAPIService();


    public BasicLedgerConfiguration(CredentialStorage storage, boolean useTls) {
        this.storage = storage;
        this.useTls=useTls;
    }

    @Override
    public Pair<UserClient, CredentialManagement> createClient() throws Exception {
        VIdPModel vidp=apiService.getLedgerVidp(didVIdp).execute().body();
        ObjectMapper mapper=new ObjectMapper();

        Utils.longLog(TAG,mapper.writeValueAsString(vidp));
        int serverCount=vidp.getDid().getServices().size();
        List<PabcIdPRESTConnection> idps = new ArrayList<>(serverCount);
        for (int i = 0; i < serverCount; i++) {
            PabcIdPRESTConnection rest = new PabcIdPRESTConnection(vidp.getDid().getServices().get(i).getEndpoint().contains("https://") ? vidp.getDid().getServices().get(i).getEndpoint() : "https://" + vidp.getDid().getServices().get(i).getEndpoint(), adminCookie, i,1000);
            Log.d("REST-IDP", rest.toString());
            idps.add(rest);
        }
        Map<Integer, MSverfKey> publicKeys = new HashMap<>();
        for (Integer j = 0; j < serverCount; j++) { //We use public key from vidp ledger record
            byte[] pkDecoded = Base64.decode(vidp.getDid().getServices().get(j).getVerificationMethod().getPublicKeySerial());
            publicKeys.put(j, new PSverfKey(pkDecoded));
            //publicKeys.put(j, idps.get(j).getPabcPublicKeyShare());
        }
        PabcPublicParameters publicParam = idps.get(0).getPabcPublicParam(); //We get public param from IdP until chain bug with attrDef is fixed
        PSCredentialManagement credentialManagement = new PSCredentialManagement(true, storage, PRESENTATION_LIFETIME);
        credentialManagement.setup(publicParam, publicKeys, seed);
        //Log.d("SETUP","RESULT: "+credentialManagement.getPublicParamsForOffline().getSecond().equals(new PSverfKey(Base64.decode(vidp.getVerificationMethod().getPublicKeySerial()))));
        ClientCryptoModule cryptoModule = new SoftwareClientCryptoModule(new Random(1),
                ((RSAPublicKey) idps.get(0).getCertificate().getPublicKey()).getModulus());
        return new Pair<>(new PabcClient(idps, credentialManagement, cryptoModule), credentialManagement);
    }
}
