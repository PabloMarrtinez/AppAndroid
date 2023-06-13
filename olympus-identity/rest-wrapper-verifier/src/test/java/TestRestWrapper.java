import eu.olympus.client.*;
import eu.olympus.client.interfaces.ClientCryptoModule;
import eu.olympus.client.interfaces.CredentialManagement;
import eu.olympus.client.interfaces.UserClient;
import eu.olympus.client.storage.InMemoryCredentialStorage;
import eu.olympus.model.*;
import eu.olympus.model.exceptions.AuthenticationFailedException;
import eu.olympus.model.exceptions.OperationFailedException;
import eu.olympus.model.exceptions.UserCreationFailedException;
import eu.olympus.server.*;
import eu.olympus.server.interfaces.*;
import eu.olympus.server.rest.*;
import eu.olympus.util.CommonCrypto;
import eu.olympus.util.ConfigurationUtil;
import eu.olympus.util.Util;
import eu.olympus.util.multisign.MSverfKey;
import eu.olympus.verifier.interfaces.Verifier;
import org.apache.commons.codec.binary.Base64;
import org.junit.BeforeClass;
import org.junit.Test;
import verifier.rest.SetupModel;
import verifier.rest.VerificationModel;
import verifier.rest.VerifierServer;
import java.math.BigInteger;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.util.*;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.junit.Assert.assertThat;

public class TestRestWrapper {

    private static final byte[] seed = "random value random value random value random value random".getBytes();
    private static final byte[] seed0 = "random value random value random value random value random0".getBytes();
    private static final byte[] seed1 = "random value random value random value random value random1".getBytes();
    private static final byte[] seed2 = "random value random value random value random value random2".getBytes();

    private static PABCConfigurationImpl[] configuration;
    private static Verifier verifier;
    private static long lifetime = 72000000;
    private static long allowedTimeDiff = 10000l;
    private static long sessionLength = 600000l;
    private static String adminCookie;
    private static final int serverCount = 3;
    private static final int verifierPort = 3000;
    private static Map<Integer, PestoDatabase> databases = new HashMap<Integer, PestoDatabase>();


    @BeforeClass
    public static void generatePestoConfigurations() {
        configuration = new PABCConfigurationImpl[3];
        byte[][] seeds=new byte[3][];
        seeds[0]=seed0;
        seeds[1]=seed1;
        seeds[2]=seed2;
        RSAPrivateCrtKey pk = (RSAPrivateCrtKey)TestParameters.getRSAPrivateKey2();
        BigInteger d = pk.getPrivateExponent();


        Random rnd = new Random(1);
        List<BigInteger> oprfKeys = new ArrayList<>(serverCount);
        List<BigInteger> rsaShares = new ArrayList<>(serverCount);
        BigInteger sum = BigInteger.ZERO;
        for(int i=0; i< serverCount-1; i++) {
            BigInteger currentRSAShare = new BigInteger(pk.getModulus().bitLength()+8* CommonCrypto.STATISTICAL_SEC_BYTES, rnd).mod(pk.getModulus());
            sum = sum.add(currentRSAShare);
            rsaShares.add(currentRSAShare);
            oprfKeys.add(new BigInteger(CommonCrypto.BITS_IN_GROUP+8*CommonCrypto.STATISTICAL_SEC_BYTES, rnd).mod(CommonCrypto.CURVE_ORDER));
        }
        rsaShares.add(d.subtract(sum));
        oprfKeys.add(new BigInteger(CommonCrypto.BITS_IN_GROUP+8*CommonCrypto.STATISTICAL_SEC_BYTES, rnd).mod(CommonCrypto.CURVE_ORDER));

        List<Map<Integer, BigInteger>> rsaBlindings = new ArrayList<Map<Integer, BigInteger>>(serverCount);
        List<Map<Integer, BigInteger>> oprfBlindings = new ArrayList<Map<Integer, BigInteger>>(serverCount);
        for(int i=0; i< serverCount; i++) {
            rsaBlindings.add(new HashMap<>(serverCount));
            oprfBlindings.add(new HashMap<>(serverCount));
        }
        for(int i=0; i< serverCount; i++) {
            for(int j = i; j<serverCount; j++) {
                if(i != j) {
                    BigInteger current = new BigInteger(CommonCrypto.COMPUTATION_SEC_BYTES * 8, rnd);
                    rsaBlindings.get(i).put(j, current);
                    rsaBlindings.get(j).put(i, current);
                    current = new BigInteger(CommonCrypto.COMPUTATION_SEC_BYTES * 8, rnd);
                    oprfBlindings.get(i).put(j, current);
                    oprfBlindings.get(j).put(i, current);
                }
            }
        }
        List<KeyShares> keyShares = new ArrayList<>();
        List<String> servers = new ArrayList<>();
        Map<String, Authorization> authorizationCookies = new HashMap<String, Authorization>();
        for(int i=0; i< serverCount; i++) {
            keyShares.add(new KeyShares(
                    new RSASharedKey(pk.getModulus(), rsaShares.get(i), pk.getPublicExponent()),
                    rsaBlindings.get(i), oprfKeys.get(i), oprfBlindings.get(i)));
            servers.add(Integer.toString(i));
            byte[] rawCookie = new byte[64];
            rnd.nextBytes(rawCookie);
            authorizationCookies.put(org.apache.commons.codec.binary.Base64.encodeBase64String(rawCookie), new Authorization("server"+i, Arrays.asList(new Role[] {Role.SERVER}), System.currentTimeMillis()+1000000l));
        }
        byte[] rawCookie = new byte[64];
        rnd.nextBytes(rawCookie);
        adminCookie = Base64.encodeBase64String(rawCookie);
        authorizationCookies.put(adminCookie, new Authorization("Administrator", Arrays.asList(new Role[] {Role.ADMIN}), System.currentTimeMillis()+1000000l));
        TestConfigurationUtil configGenerator = new TestConfigurationUtil();

        Certificate cert = configGenerator.splitKeys(
                (RSAPrivateCrtKey) TestParameters.getRSAPrivateKey2(),
                configuration.length);
        for(int i = 0; i< serverCount; i++) {
            configuration[i] = new PABCConfigurationImpl();
            List<String> otherServers=new LinkedList<>(servers);
            otherServers.remove(i);
            Map<String, Authorization> authorizedUsers = new HashMap<>();
            for(String cookie: authorizationCookies.keySet()) {
                if(("server"+i).equals(authorizationCookies.get(cookie).getId())) {
                    configuration[i].setMyAuthorizationCookie(cookie);
                } else {
                    authorizedUsers.put(cookie, authorizationCookies.get(cookie));
                }
            }
            configuration[i].setIssuerId("https://olympus-vidp.com/issuer1");
            configuration[i].setAuthorizationCookies(authorizedUsers);
            configuration[i].setSessionLength(sessionLength);
            configuration[i].setServers(otherServers);
            configuration[i].setKeyMaterial(configGenerator.rsaSharedKeys[i]);
            configuration[i].setRsaBlindings(configGenerator.rsaBlindings[i]);
            configuration[i].setOprfBlindings(configGenerator.oprfBlindings[i]);
            configuration[i].setOprfKey(configGenerator.oprfKeys[i]);
            configuration[i].setLocalKeyShare(configGenerator.localKeyShares[i]);
            configuration[i].setRemoteShares(configGenerator.remoteKeyShares[i]);
            configuration[i].setId(i);
            configuration[i].setAllowedTimeDifference(allowedTimeDiff);
            configuration[i].setWaitTime(10000);
            configuration[i].setLifetime(lifetime);;
            configuration[i].setAttrDefinitions(generateAttributeDefinitions());
            configuration[i].setSeed(seeds[i]);
            configuration[i].setPort(9080+i);
            configuration[i].setTlsPort(9090+i);
            configuration[i].setKeyStorePath(TestParameters.TEST_KEY_STORE_LOCATION);
            configuration[i].setTrustStorePath(TestParameters.TEST_TRUST_STORE_LOCATION);
            configuration[i].setKeyStorePassword(TestParameters.TEST_KEY_STORE_PWD);
            configuration[i].setTrustStorePassword(TestParameters.TEST_TRUST_STORE_PWD);
            configuration[i].setCert(cert);
        }
        for(int i = 0; i< serverCount; i++) {
            PestoRefresher refresher = new PestoRefresher(i, new SoftwareServerCryptoModule(new Random(i)));
            List<byte[]> shares = refresher.reshareMasterKeys(keyShares.get(i), serverCount);
            configuration[i].setLocalKeyShare(shares.remove(0));
            for(int j = 0; j < serverCount; j++) {
                if(i != j) {
                    configuration[j].getRemoteShares().put(i, shares.remove(0));
                }
            }
        }
    }

    private static Set<AttributeDefinition> generateAttributeDefinitions() {
        Set<AttributeDefinition> res=new HashSet<>();
        res.add(new AttributeDefinitionString("Name","name",0,16));
        res.add(new AttributeDefinitionInteger("Age","age",0,123));
        res.add(new AttributeDefinitionString("Nationality","nationality",0,16));
        res.add(new AttributeDefinitionDate("DateOfBirth","dateofbirth","1900-01-01T00:00:00","2020-09-01T00:00:00",DateGranularity.DAYS));
        return res;
    }


    private List<PabcIdP> setupPabcIdPs(int amount) {
        if (amount != 3) {
            throw new IllegalArgumentException("Configuration only supports " + 3 + " servers");
        }
        List<PabcIdP> idps = new ArrayList<>();
        databases = new HashMap<>();
        for(int i = 0; i< amount; i++) {
            databases.put(i,  new InMemoryPestoDatabase());
            PabcIdPImpl idp = null;
            List<IdentityProver> provers = new LinkedList<IdentityProver>();
            provers.add(new TestIdentityProver(databases.get(i)));
            SoftwareServerCryptoModule crypto = new SoftwareServerCryptoModule(new Random(i));
            try {
                Map<String, MFAAuthenticator> mfaAuthenticators = new HashMap<>();
                mfaAuthenticators.put(GoogleAuthenticator.TYPE, new GoogleAuthenticator(crypto));
                idp = new PabcIdPImpl(databases.get(i), provers, mfaAuthenticators, crypto, 10000);
            } catch(Exception e) {
                fail("Failed to start IdP");
            }
            idps.add(idp);
        }
        return idps;
    }

    @Test
    public void testPabcFlow() throws Exception {
        int serverCount = 3;
        List<PabcIdP> idps = setupPabcIdPs(serverCount);
        List<RESTIdPServer> restServers = new ArrayList<>();
        List<String> servlets=new LinkedList<>();
        servlets.add(PabcIdPServlet.class.getCanonicalName());

        for(int i = 0; i< serverCount; i++) {
            try {
                RESTIdPServer restServer = new RESTIdPServer();
                restServer.setIdP(idps.get(i));
                restServer.start(configuration[i].getPort(), servlets, 0, null, null, null);
                restServers.add(restServer);
            } catch (Exception e) {
                fail("Failed to start IdP");
            }
        }
        List<PabcIdP> restIdps = new ArrayList<>();
        List<String> urls=new ArrayList<>();
        for(int i = 0; i< serverCount; i++) {
            try {
                String url="http://127.0.0.1:"+(configuration[i].getPort());
                urls.add(url);
                PabcIdPRESTConnection restConnection = new PabcIdPRESTConnection(url,
                        adminCookie, i,1000);
                List<PestoIdP2IdPRESTConnection> others = new ArrayList<>();
                for(int j = 0; j< serverCount; j++) {
                    if (j != i) {
                        others.add(new PestoIdP2IdPRESTConnection("http://127.0.0.1:" + (configuration[j].getPort()), j,
                                configuration[i].getMyAuthorizationCookie()));
                    }
                }
                for(String cookie: configuration[i].getAuthorizationCookies().keySet()) {
                    (idps.get(i)).addSession(cookie, configuration[i].getAuthorizationCookies().get(cookie));
                }
                boolean res = ((PabcIdPImpl) idps.get(i)).setup("setup", configuration[i], others);
                assertTrue(res);
                restIdps.add(restConnection);
            } catch(Exception e) {
                fail("Failed to start IdP");
            }
        }

        Map<Integer, MSverfKey> publicKeys = new HashMap<>();
        for (Integer j = 0; j< serverCount; j++){
            publicKeys.put(j, restIdps.get(j).getPabcPublicKeyShare());
        }
        PabcPublicParameters publicParam= restIdps.get(0).getPabcPublicParam();

        CredentialManagement credentialManagement=new PSCredentialManagement(true, new InMemoryCredentialStorage(), 60);
        ((PSCredentialManagement)credentialManagement).setup(publicParam,publicKeys,seed);

        ClientCryptoModule cryptoModule = new SoftwareClientCryptoModule(new Random(1), configuration[0].getKeyMaterial().getModulus());

        UserClient client = new PabcClient(restIdps, credentialManagement, cryptoModule);

        VerifierServer server=VerifierServer.getInstance();
        server.start(verifierPort);

        //SETUP VERIFIER REST
        VerifierRESTConnection verifierConnection=new VerifierRESTConnection("http://127.0.0.1:" + (verifierPort));
        verifierConnection.setup(new SetupModel(urls));

        testSimpleFlowPabc(client,verifierConnection);
        server.stop();
    }

    private void testSimpleFlowPabc(UserClient client, VerifierRESTConnection verifier) throws AuthenticationFailedException {
        try{
            client.createUser("user_1", "password");
        } catch(UserCreationFailedException e) {
            fail("Failed to create user");
        }
        Map<String, Attribute> attributes = new HashMap<>();
        attributes.put("name", new Attribute("John Doe"));
        attributes.put("nationality", new Attribute("DK"));
        attributes.put("age",new Attribute(22));
        attributes.put("dateofbirth",new Attribute(Util.fromRFC3339UTC("1998-01-05T00:00:00")));

        try {

            // 	Prove identity with cached key
            client.addAttributes("user_1", "password", new TestIdentityProof("proof", attributes), null, "NONE");
        } catch(OperationFailedException e) {
            fail("Failed to prove identity: " + e);
        }
        client.clearSession();
        String signedMessage="SignedMessage";
        List<Predicate> predicates = new ArrayList<>();
        Predicate predicate = new Predicate();
        predicate.setAttributeName("Name");
        predicate.setOperation(Operation.REVEAL);
        predicates.add(predicate);
        predicate = new Predicate();
        predicate.setAttributeName("Age");
        predicate.setOperation(Operation.GREATERTHANOREQUAL);
        predicate.setValue(new Attribute(18));
        predicate = new Predicate();
        predicate.setAttributeName("DateOfBirth");
        predicate.setOperation(Operation.INRANGE);
        predicate.setValue(new Attribute(Util.fromRFC3339UTC("1990-01-05T00:00:00")));
        predicate.setExtraValue(new Attribute(Util.fromRFC3339UTC("2000-01-05T00:00:00")));
        predicates.add(predicate);
        Policy policy = new Policy(predicates, signedMessage);
        Policy verifierPolicy = new Policy(policy.getPredicates(), signedMessage);
        String token = client.authenticate("user_1", "password", policy, null, "NONE");
        client.clearSession();

        assertThat(verifier.verify(new VerificationModel(token,verifierPolicy)), is(true));
    }


    public static class TestConfigurationUtil extends ConfigurationUtil {

        public RSASharedKey[] rsaSharedKeys;
        public Map<Integer, BigInteger>[] rsaBlindings;
        public Map<Integer, BigInteger>[]  oprfBlindings;
        public Map<Integer, byte[]>[] remoteKeyShares;
        public BigInteger[] oprfKeys;
        public byte[][] localKeyShares;
        Certificate splitKeys(RSAPrivateCrtKey sk, int amount) {
            try {
                rng = new Random();
                RDN = "CN=olympus-vidp.com,O=Olympus,OU=www.olympus-project.eu,C=EU";

                rsaSharedKeys = new RSASharedKey[amount];
                rsaBlindings = new Map[amount];
                oprfBlindings = new Map[amount];
                remoteKeyShares = new Map[amount];
                oprfKeys = new BigInteger[amount];
                localKeyShares = new byte[amount][];

                Certificate certificate = doKeyShares(sk, amount, rng,
                        rsaSharedKeys, rsaBlindings, oprfBlindings, oprfKeys,
                        localKeyShares, remoteKeyShares, RDN);
                return certificate;

            }catch(Exception e ) {
                throw new RuntimeException(e);
            }
        }
    }

}
