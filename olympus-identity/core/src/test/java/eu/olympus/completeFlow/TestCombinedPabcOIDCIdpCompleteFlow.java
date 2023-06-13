package eu.olympus.completeFlow;

import eu.olympus.client.*;
import eu.olympus.client.interfaces.ClientCryptoModule;
import eu.olympus.client.interfaces.CredentialManagement;
import eu.olympus.client.interfaces.UserClient;
import eu.olympus.client.storage.InMemoryCredentialStorage;
import eu.olympus.model.*;
import eu.olympus.model.exceptions.AuthenticationFailedException;
import eu.olympus.model.exceptions.OperationFailedException;
import eu.olympus.model.exceptions.TokenGenerationException;
import eu.olympus.model.exceptions.UserCreationFailedException;
import eu.olympus.server.*;
import eu.olympus.server.interfaces.*;
import eu.olympus.server.rest.CombinedIdPServlet;
import eu.olympus.server.rest.PestoIdP2IdPRESTConnection;
import eu.olympus.server.rest.RESTIdPServer;
import eu.olympus.server.storage.InMemoryPestoDatabase;
import eu.olympus.unit.server.TestIdentityProof;
import eu.olympus.unit.server.TestIdentityProver;
import eu.olympus.util.Util;
import eu.olympus.util.multisign.MSverfKey;
import eu.olympus.verifier.JWTVerifier;
import eu.olympus.verifier.OLVerificationLibraryPS;
import eu.olympus.verifier.W3CPresentationVerifierOL;
import eu.olympus.verifier.W3CVerificationResult;
import eu.olympus.verifier.interfaces.Verifier;
import eu.olympus.verifier.interfaces.W3CPresentationVerifier;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.interfaces.RSAPublicKey;
import java.util.*;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.junit.Assert.assertThat;

public class TestCombinedPabcOIDCIdpCompleteFlow extends CommonCompleteTests{
    private static Logger logger = LoggerFactory.getLogger(TestCombinedPabcOIDCIdpCompleteFlow.class);

    private static Map<Integer, PestoDatabase> databases = new HashMap<Integer, PestoDatabase>();

    @Test
    public void testCombinedDirect() throws Exception {
        int serverAmount = 3;
        List<CombinedPabcOIDCIdPImpl> idps = new ArrayList<>();

        for(int i = 0; i< serverAmount; i++) {
            PestoDatabase db = new InMemoryPestoDatabase();
            CombinedPabcOIDCIdPImpl idp = null;
            List<IdentityProver> provers = new LinkedList<IdentityProver>();
            provers.add(new TestIdentityProver(db));
            try {
                SoftwareServerCryptoModule crypto = new SoftwareServerCryptoModule(new Random(i));
                Map<String, MFAAuthenticator> mfaAuthenticators = new HashMap<>();
                mfaAuthenticators.put(GoogleAuthenticator.TYPE, new GoogleAuthenticator(crypto));
                idp = new CombinedPabcOIDCIdPImpl(db, provers, mfaAuthenticators, crypto, configuration[i].getIssuerId(), 100);
            } catch(Exception e) {
                e.printStackTrace();

                fail("Failed to start IdP");
            }
            idps.add(idp);
        }
        for(int i = 0; i< serverAmount; i++) {
            try {
                CombinedPabcOIDCIdPImpl idp = idps.get(i);
                List<PestoIdP> others = new ArrayList<PestoIdP>();
                others.addAll(idps);
                others.remove(idp);
                boolean complete = idp.setup("setup", configuration[i], others);
                assertTrue(complete);
            } catch(Exception e) {
                fail("Failed to start IdP");
            }
        }

        ClientCryptoModule cryptoModule = new SoftwareClientCryptoModule(new Random(1), ((RSAPublicKey)idps.get(0).getCertificate().getPublicKey()).getModulus());
        UserClient client = new PestoClient(idps, cryptoModule);

        Verifier verifier = new JWTVerifier(idps.get(0).getCertificate().getPublicKey());
        simpleFlow(client, verifier);
        testPestoCreateAndAddAttributes(client);

        Map<Integer, MSverfKey> publicKeys = new HashMap<>();

        PSCredentialManagement credentialManagement=new PSCredentialManagement(false, null,60);
        credentialManagement.setup(idps,seed);

        UserClient pabcClient = new PabcClient(idps, credentialManagement, cryptoModule);
        OLVerificationLibraryPS verificationLibrary = new OLVerificationLibraryPS();
        verificationLibrary.setup(idps,seed);
        W3CPresentationVerifier w3cVerifier= new W3CPresentationVerifierOL(verificationLibrary);
        continuationFlowPabc(pabcClient,w3cVerifier);
    }

    //Policies for OIDC differ slightly, so we use a custom testflow
    public void simpleFlow(UserClient client, Verifier verifier) throws AuthenticationFailedException, TokenGenerationException {
        try{
            client.createUser("user_1", "password");
        } catch(UserCreationFailedException e) {
            e.printStackTrace();
            fail("Failed to create user");
        }
        Map<String, Attribute> attributes = new HashMap<>();
        attributes.put("Name", new Attribute("John Doe"));
        attributes.put("Nationality", new Attribute("DK"));
        attributes.put("Age",new Attribute(22));

        try {

            // Prove identity using the key cache
            client.addAttributes("user_1", "password",  new TestIdentityProof("proof",attributes), null, "NONE");
        } catch(OperationFailedException e) {
            fail("Failed to prove identity: " + e);
        }
        client.clearSession();

        Map<String, Attribute> attributes2 = new HashMap<>();
        attributes2.put("Name", new Attribute("Jane Doe"));
        attributes2.put("Nationality", new Attribute("Se"));
        attributes2.put("Age",new Attribute(30));
        try{
            client.createUserAndAddAttributes("user_2", "password2", new TestIdentityProof("proof", attributes2));
            client.clearSession();
        } catch(UserCreationFailedException e) {
            fail("Failed to create user");
        }

        List<Predicate> predicates = new ArrayList<>();
        Predicate predicate = new Predicate();
        predicate.setAttributeName("name");
        predicate.setOperation(Operation.REVEAL);
        predicates.add(predicate);
        predicate = new Predicate("audience", Operation.REVEAL, new Attribute("test-service-provider"));
        predicates.add(predicate);
        Policy policy = new Policy(predicates, "testPolicy");
        String token = client.authenticate("user_1", "password", policy, null, "NONE");
        assertThat(verifier.verify(token), is(true));
        client.clearSession();

        try{ //
            client.authenticate("user_1", "bad_password", policy, null, "NONE");
            fail("Could authenticate with a bad password");
        } catch(AuthenticationFailedException  e) {
        }
        client.clearSession();

        token = client.authenticate("user_2", "password2", policy, null, "NONE");
        assertThat(verifier.verify(token), is(true));
        client.clearSession();
    }

    //Test pabc after OIDC presentation for same user was tested
    public void continuationFlowPabc(UserClient client, W3CPresentationVerifier verifier) throws AuthenticationFailedException {
        long start = System.currentTimeMillis();
        String signedMessage="SignedMessage";
        List<Predicate> predicates = new ArrayList<>();
        Predicate predicate = new Predicate();
        predicate.setAttributeName("name");
        predicate.setOperation(Operation.REVEAL);
        predicates.add(predicate);
        predicate = new Predicate();
        predicate.setAttributeName("age");
        predicate.setOperation(Operation.GREATERTHANOREQUAL);
        predicate.setValue(new Attribute(18));
        Policy policy = new Policy(predicates, signedMessage);
        Policy verifierPolicy = new Policy(policy.getPredicates(), signedMessage);
        try {
            client.authenticate("user_1", "wrong password", policy, null, "NONE");
            fail();
        } catch (AuthenticationFailedException ignored){
        }
        client.clearSession();
        String token = client.authenticate("user_1", "password", policy, null, "NONE");
        assertThat(verifier.verifyPresentationToken(token, verifierPolicy), is(W3CVerificationResult.VALID));
        client.clearSession();
        long end = System.currentTimeMillis();
        logger.info("PABC total time: "+((end-start))+" ms");
    }
    

    private void testPestoCreateAndAddAttributes(UserClient client){
        Map<String, Attribute> attributes = new HashMap<>();
        attributes.put("name", new Attribute("John Doe"));
        attributes.put("email", new Attribute("John.Doe@example.com"));
        attributes.put("birthdate",new Attribute(Util.fromRFC3339UTC("1998-01-05T00:00:00")));

        try{
            client.createUserAndAddAttributes("user_1337", "password", new TestIdentityProof("proof2",attributes));
        } catch(UserCreationFailedException e) {
            fail("Failed to create user" + e);
        }
    }


}
