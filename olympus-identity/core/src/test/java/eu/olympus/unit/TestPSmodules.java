package eu.olympus.unit;


import VCModel.Proof;
import VCModel.Verifiable;
import VCModel.VerifiableCredential;
import VCModel.VerifiablePresentation;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import eu.olympus.client.PSCredentialManagement;
import eu.olympus.client.interfaces.CredentialManagement;
import eu.olympus.client.interfaces.CredentialStorage;
import eu.olympus.client.storage.InMemoryCredentialStorage;
import eu.olympus.model.exceptions.OperationFailedException;
import eu.olympus.model.Attribute;
import eu.olympus.model.AttributeDefinition;
import eu.olympus.model.AttributeDefinitionDate;
import eu.olympus.model.AttributeDefinitionInteger;
import eu.olympus.model.AttributeDefinitionString;
import eu.olympus.model.Operation;
import eu.olympus.model.PABCConfigurationImpl;
import eu.olympus.model.PabcPublicParameters;
import eu.olympus.model.Policy;
import eu.olympus.model.Predicate;
import eu.olympus.model.exceptions.SetupException;
import eu.olympus.model.exceptions.TokenGenerationException;
import eu.olympus.server.ThresholdPSSharesGenerator;
import eu.olympus.server.interfaces.CredentialGenerator;
import eu.olympus.server.interfaces.PabcIdP;
import eu.olympus.server.interfaces.PestoDatabase;
import eu.olympus.server.storage.InMemoryPestoDatabase;
import eu.olympus.unit.util.MockFactory;
import eu.olympus.util.Pair;
import eu.olympus.util.Util;
import eu.olympus.util.W3CSerializationUtil;
import eu.olympus.util.multisign.MSpublicParam;
import eu.olympus.util.multisign.MSverfKey;
import eu.olympus.util.psmultisign.PSauxArg;
import eu.olympus.util.psmultisign.PSms;
import eu.olympus.util.psmultisign.PSpublicParam;
import eu.olympus.util.rangeProof.RangePredicateToken;
import eu.olympus.verifier.OLVerificationLibraryPS;
import eu.olympus.verifier.W3CPresentationVerifierOL;
import eu.olympus.verifier.W3CVerificationResult;
import eu.olympus.verifier.interfaces.OLVerificationLibrary;
import eu.olympus.verifier.interfaces.W3CPresentationVerifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import java.util.*;
import static eu.olympus.util.Util.fromRFC3339UTC;
import static org.mockito.Mockito.*;

public class TestPSmodules {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private static final int nServers = 3;
    private final byte[] seed = "random value random value random value random value random".getBytes();
    private long lifetime = 72000000;
    private long allowedTimeDifference = 10000l;

    private Set<AttributeDefinition> generateAttributeDefinitions() {
        Set<AttributeDefinition> res = new HashSet<>();
        res.add(new AttributeDefinitionString("uri:Name", "name", 0, 16));
        res.add(new AttributeDefinitionInteger("uri:Age", "age", 0, 123));
        res.add(new AttributeDefinitionDate("uri:Now", "now", "1900-01-01T00:00:00", "2100-09-01T00:00:00"));
        return res;
    }



    @Test
    public void testCorrectFlowWithStorage() throws Exception {
        String username = "userJoe";
        PestoDatabase database = new InMemoryPestoDatabase();
        Map<String, Attribute> userAttr = new HashMap<>();
        userAttr.put("name", new Attribute("Joe"));
        userAttr.put("age", new Attribute(21));
        userAttr.put("now", new Attribute(new Date(System.currentTimeMillis())));
        database.addUser(username, null, 1);
        database.addAttributes(username, userAttr);
        //Create and credentialGenerator module for each server.
        Map<Integer, CredentialGenerator> mapServers = new HashMap<>();
        PabcPublicParameters publicParams = null;
        for (int i = 0; i < nServers; i++) {
            CredentialGenerator credentialServerModule = new ThresholdPSSharesGenerator(database, seed, i);
            PABCConfigurationImpl config = new PABCConfigurationImpl();
            config.setAttrDefinitions(generateAttributeDefinitions());
            config.setSeed((seed.toString()+i).getBytes());
            config.setLifetime(lifetime);
            config.setAllowedTimeDifference(allowedTimeDifference);
            config.setServers(Arrays.asList("1", "2"));
            credentialServerModule.setup(config);
            publicParams = credentialServerModule.getPublicParam();
            mapServers.put(i, credentialServerModule);
        }
        MSpublicParam schemePublicParam = new PSpublicParam(publicParams.getEncodedSchemePublicParam());
        //Obtain publicKeyShares and aggregatedKey
        Map<Integer, MSverfKey> verificationKeyShares = new HashMap<>();
        MSverfKey[] verificationKeys = new MSverfKey[nServers];
        int i = 0;
        for (Integer id : mapServers.keySet()) {
            MSverfKey key = (MSverfKey) mapServers.get(id).getVerificationKeyShare();
            verificationKeys[i] = key;
            verificationKeyShares.put(id, key);
            i++;
        }
        PSms auxSignScheme = new PSms();
        auxSignScheme.setup(schemePublicParam.getN(), schemePublicParam.getAuxArg(), seed);
        MSverfKey aggregatedVerificationKey = auxSignScheme.kAggreg(verificationKeys);
        //Setup client module
        CredentialManagement credentialClientModule = new PSCredentialManagement(true, new InMemoryCredentialStorage(), 60);
        ((PSCredentialManagement) credentialClientModule).setup(publicParams, verificationKeyShares, seed);
        //Setup verifier module
        OLVerificationLibrary credentialVerifierLibrary = new OLVerificationLibraryPS();
        ((OLVerificationLibraryPS) credentialVerifierLibrary).setup(publicParams, aggregatedVerificationKey, seed);
        W3CPresentationVerifier credentialVerifierModule = new W3CPresentationVerifierOL(credentialVerifierLibrary);
        //*********Credential creation, proof of policy and verification***********
        String signedMessage = "signedMessage";
        //Policy creation
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(new Predicate("uri:Now", Operation.REVEAL, null));
        predicates.add(new Predicate("uri:Age", Operation.REVEAL, null));
        Policy policy = new Policy(predicates, signedMessage);
        //Client receives a credential share from each IdP (simplified) and passes them to the credential manager
        Map<Integer, VerifiableCredential> credentialShares = new HashMap<>();
        long timestamp = System.currentTimeMillis();
        for (Integer id : mapServers.keySet()) {
            credentialShares.put(id, mapServers.get(id).createCredentialShare(username, timestamp));
        }
        VerifiablePresentation zkPT = credentialClientModule.combineAndGeneratePresentationToken(credentialShares, policy);
        //Verification of the presentation token
        W3CVerificationResult result = credentialVerifierModule.verifyPresentationToken(zkPT.toJSONString(), policy);
        assertEquals(W3CVerificationResult.VALID, result);
        String signedMessage2 = "signedMessage2";
        List<Predicate> predicates2 = new ArrayList<>();
        predicates2.add(new Predicate("uri:Age", Operation.REVEAL, null));
        Policy policy2 = new Policy(predicates2, signedMessage2);
        //Client has the credential and uses it without contacting servers
        assertThat(credentialClientModule.checkStoredCredential(), is(true));
        VerifiablePresentation zkPT2 = credentialClientModule.generatePresentationToken(policy2);
        W3CVerificationResult result2 = credentialVerifierModule.verifyPresentationToken(zkPT2.toJSONString(), policy2);
        assertEquals(W3CVerificationResult.VALID, result2);
    }

    @Test
    public void testCorrectFlowNoStorage() throws Exception {
        String username = "userJoe";
        PestoDatabase database = new InMemoryPestoDatabase();
        Map<String, Attribute> userAttr = new HashMap<>();
        userAttr.put("name", new Attribute("Joe"));
        userAttr.put("age", new Attribute(21));
        userAttr.put("now", new Attribute(new Date(System.currentTimeMillis())));
        database.addUser(username, null, 1);
        database.addAttributes(username, userAttr);
        //Create and credentialGenerator module for each server.
        Map<Integer, CredentialGenerator> mapServers = new HashMap<>();
        PabcPublicParameters publicParams = null;
        for (int i = 0; i < nServers; i++) {
            CredentialGenerator credentialServerModule = new ThresholdPSSharesGenerator(database, seed, i);
            PABCConfigurationImpl config = new PABCConfigurationImpl();
            config.setAttrDefinitions(generateAttributeDefinitions());
            config.setSeed((seed.toString()+i).getBytes());
            config.setLifetime(lifetime);
            config.setAllowedTimeDifference(allowedTimeDifference);
            config.setServers(Arrays.asList("1", "2"));
            credentialServerModule.setup(config);
            publicParams = credentialServerModule.getPublicParam();
            mapServers.put(i, credentialServerModule);
        }
        MSpublicParam schemePublicParam = new PSpublicParam(publicParams.getEncodedSchemePublicParam());
        //Obtain publicKeyShares and aggregatedKey
        Map<Integer, MSverfKey> verificationKeyShares = new HashMap<>();
        MSverfKey[] verificationKeys = new MSverfKey[nServers];
        int i = 0;
        for (Integer id : mapServers.keySet()) {
            MSverfKey key = (MSverfKey) mapServers.get(id).getVerificationKeyShare();
            verificationKeys[i] = key;
            verificationKeyShares.put(id, key);
            i++;
        }
        PSms auxSignScheme = new PSms();
        auxSignScheme.setup(schemePublicParam.getN(), schemePublicParam.getAuxArg(), seed);
        MSverfKey aggregatedVerificationKey = auxSignScheme.kAggreg(verificationKeys);
        //Setup client module
        CredentialManagement credentialClientModule = new PSCredentialManagement(false, null, 60);
        ((PSCredentialManagement) credentialClientModule).setup(publicParams, verificationKeyShares, seed);
        //Setup verifier module
        OLVerificationLibrary credentialVerifierLibrary = new OLVerificationLibraryPS();
        ((OLVerificationLibraryPS) credentialVerifierLibrary).setup(publicParams, aggregatedVerificationKey, seed);
        W3CPresentationVerifier credentialVerifierModule = new W3CPresentationVerifierOL(credentialVerifierLibrary);
        //*********Credential creation, proof of policy and verification***********
        String signedMessage = "signedMessage";
        //Policy creation
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(new Predicate("uri:Now", Operation.REVEAL, null));
        predicates.add(new Predicate("uri:Age", Operation.REVEAL, null));
        Policy policy = new Policy(predicates, signedMessage);
        //Client receives a credential share from each IdP (simplified) and passes them to the credential manager
        Map<Integer, VerifiableCredential> credentialShares = new HashMap<>();
        long timestamp = System.currentTimeMillis();
        for (Integer id : mapServers.keySet()) {
            credentialShares.put(id, mapServers.get(id).createCredentialShare(username, timestamp));
        }
        //Client uses module to generate presentation token for the policy
        VerifiablePresentation zkPT = credentialClientModule.combineAndGeneratePresentationToken(credentialShares, policy);
        //Verification of the presentation token
        W3CVerificationResult result = credentialVerifierModule.verifyPresentationToken(zkPT.toJSONString(), policy);
        assertEquals(W3CVerificationResult.VALID, result);
    }

    @Test
    public void testCompleteFlowWithRange() throws Exception {
        String username = "userJoe";
        PestoDatabase database = new InMemoryPestoDatabase();
        Map<String, Attribute> userAttr = new HashMap<>();
        userAttr.put("name", new Attribute("Joe"));
        userAttr.put("age", new Attribute(21));
        userAttr.put("now", new Attribute(new Date(System.currentTimeMillis())));
        database.addUser(username, null, 1);
        database.addAttributes(username, userAttr);
        //Create and credentialGenerator module for each server.
        Map<Integer, CredentialGenerator> mapServers = new HashMap<>();
        PabcPublicParameters publicParams = null;
        for (int i = 0; i < nServers; i++) {
            CredentialGenerator credentialServerModule = new ThresholdPSSharesGenerator(database, seed, i);
            PABCConfigurationImpl config = new PABCConfigurationImpl();
            config.setAttrDefinitions(generateAttributeDefinitions());
            config.setSeed((seed.toString()+i).getBytes());
            config.setLifetime(lifetime);
            config.setAllowedTimeDifference(allowedTimeDifference);
            config.setServers(Arrays.asList("1", "2"));
            config.setId(i);
            credentialServerModule.setup(config);
            publicParams = credentialServerModule.getPublicParam();
            mapServers.put(i, credentialServerModule);
        }
        MSpublicParam schemePublicParam = new PSpublicParam(publicParams.getEncodedSchemePublicParam());
        //Obtain publicKeyShares and aggregatedKey
        Map<Integer, MSverfKey> verificationKeyShares = new HashMap<>();
        MSverfKey[] verificationKeys = new MSverfKey[nServers];
        int i = 0;
        for (Integer id : mapServers.keySet()) {
            MSverfKey key = (MSverfKey) mapServers.get(id).getVerificationKeyShare();
            verificationKeys[i] = key;
            verificationKeyShares.put(id, key);
            i++;
        }
        PSms auxSignScheme = new PSms();
        auxSignScheme.setup(schemePublicParam.getN(), schemePublicParam.getAuxArg(), seed);
        MSverfKey aggregatedVerificationKey = auxSignScheme.kAggreg(verificationKeys);
        //Setup client module
        CredentialManagement credentialClientModule = new PSCredentialManagement(true, new InMemoryCredentialStorage(), 60);
        ((PSCredentialManagement) credentialClientModule).setup(publicParams, verificationKeyShares, seed);
        //Setup verifier module
        OLVerificationLibrary credentialVerifierLibrary = new OLVerificationLibraryPS();
        ((OLVerificationLibraryPS) credentialVerifierLibrary).setup(publicParams, aggregatedVerificationKey, seed);
        W3CPresentationVerifier credentialVerifierModule = new W3CPresentationVerifierOL(credentialVerifierLibrary);
        //*********Credential creation, proof of policy and verification***********
        String signedMessage = "signedMessage";
        //Policy creation
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(new Predicate("uri:Now", Operation.REVEAL, null));
        predicates.add(new Predicate("uri:Age", Operation.GREATERTHANOREQUAL, new Attribute(18)));
        Policy policy = new Policy(predicates, signedMessage);
        //Client receives a credential share from each IdP (simplified) and passes them to the credential manager
        Map<Integer, VerifiableCredential> credentialShares = new HashMap<>();
        long timestamp = System.currentTimeMillis();
        for (Integer id : mapServers.keySet()) {
            credentialShares.put(id, mapServers.get(id).createCredentialShare(username, timestamp));
        }
        VerifiablePresentation zkPT = credentialClientModule.combineAndGeneratePresentationToken(credentialShares, policy);
        //Client uses module to generate presentation token for the policy
        //Verification of the presentation token
        W3CVerificationResult result = credentialVerifierModule.verifyPresentationToken(zkPT.toJSONString(), policy);
        assertEquals(W3CVerificationResult.VALID, result);
        String signedMessage2 = "signedMessage2";
        List<Predicate> predicates2 = new ArrayList<>();
        predicates2.add(new Predicate("uri:Age", Operation.LESSTHANOREQUAL, new Attribute(30)));
        Policy policy2 = new Policy(predicates2, signedMessage2);
        //Client has the credential and uses it without contacting servers
        assertThat(credentialClientModule.checkStoredCredential(), is(true));
        VerifiablePresentation zkPT2 = credentialClientModule.generatePresentationToken(policy2);
        W3CVerificationResult result2 = credentialVerifierModule.verifyPresentationToken(zkPT2.toJSONString(), policy2);
        assertEquals(W3CVerificationResult.VALID, result2);
    }


    @Test
    public void testCompleteFlowWithMultipleRangeProofs() throws Exception {
        String username = "userJoe";
        PestoDatabase database = new InMemoryPestoDatabase();
        Map<String, Attribute> userAttr = new HashMap<>();
        userAttr.put("name", new Attribute("Joe"));
        userAttr.put("age", new Attribute(21));
        userAttr.put("now", new Attribute(new Date(System.currentTimeMillis())));
        database.addUser(username, null, 1);
        database.addAttributes(username, userAttr);
        //Create and credentialGenerator module for each server.
        Map<Integer, CredentialGenerator> mapServers = new HashMap<>();
        PabcPublicParameters publicParams = null;
        for (int i = 0; i < nServers; i++) {
            CredentialGenerator credentialServerModule = new ThresholdPSSharesGenerator(database, seed, i);
            PABCConfigurationImpl config = new PABCConfigurationImpl();
            config.setAttrDefinitions(generateAttributeDefinitions());
            config.setSeed((seed.toString()+i).getBytes());
            config.setLifetime(lifetime);
            config.setAllowedTimeDifference(allowedTimeDifference);
            config.setServers(Arrays.asList("1", "2"));
            config.setId(i);
            credentialServerModule.setup(config);
            publicParams = credentialServerModule.getPublicParam();
            mapServers.put(i, credentialServerModule);
        }
        MSpublicParam schemePublicParam = new PSpublicParam(publicParams.getEncodedSchemePublicParam());
        //Obtain publicKeyShares and aggregatedKey
        Map<Integer, MSverfKey> verificationKeyShares = new HashMap<>();
        MSverfKey[] verificationKeys = new MSverfKey[nServers];
        int i = 0;
        for (Integer id : mapServers.keySet()) {
            MSverfKey key = (MSverfKey) mapServers.get(id).getVerificationKeyShare();
            verificationKeys[i] = key;
            verificationKeyShares.put(id, key);
            i++;
        }
        PSms auxSignScheme = new PSms();
        auxSignScheme.setup(schemePublicParam.getN(), schemePublicParam.getAuxArg(), seed);
        MSverfKey aggregatedVerificationKey = auxSignScheme.kAggreg(verificationKeys);
        //Setup client module
        CredentialManagement credentialClientModule = new PSCredentialManagement(true, new InMemoryCredentialStorage(), 60);
        ((PSCredentialManagement) credentialClientModule).setup(publicParams, verificationKeyShares, seed);
        //Setup verifier module
        OLVerificationLibrary credentialVerifierLibrary = new OLVerificationLibraryPS();
        ((OLVerificationLibraryPS) credentialVerifierLibrary).setup(publicParams, aggregatedVerificationKey, seed);
        W3CPresentationVerifier credentialVerifierModule = new W3CPresentationVerifierOL(credentialVerifierLibrary);
        //*********Credential creation, proof of policy and verification***********
        String signedMessage = "signedMessage";
        //Policy creation
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(new Predicate("uri:Now", Operation.GREATERTHANOREQUAL, new Attribute(Util.fromRFC3339UTC("2000-01-01T00:00:00"))));
        predicates.add(new Predicate("uri:Age", Operation.GREATERTHANOREQUAL, new Attribute(18)));
        Policy policy = new Policy(predicates, signedMessage);
        //Client receives a credential share from each IdP (simplified) and passes them to the credential manager
        Map<Integer, VerifiableCredential> credentialShares = new HashMap<>();
        long timestamp = System.currentTimeMillis();
        for (Integer id : mapServers.keySet()) {
            credentialShares.put(id, mapServers.get(id).createCredentialShare(username, timestamp));
        }
        VerifiablePresentation zkPT = credentialClientModule.combineAndGeneratePresentationToken(credentialShares, policy);
        //Client uses module to generate presentation token for the policy
        //Verification of the presentation token
        W3CVerificationResult result = credentialVerifierModule.verifyPresentationToken(zkPT.toJSONString(), policy);
        assertEquals(W3CVerificationResult.VALID, result);
        String signedMessage2 = "signedMessage2";
        List<Predicate> predicates2 = new ArrayList<>();
        predicates2.add(new Predicate("uri:Age", Operation.LESSTHANOREQUAL, new Attribute(30)));
        Policy policy2 = new Policy(predicates2, signedMessage2);
        //Client has the credential and uses it without contacting servers
        assertThat(credentialClientModule.checkStoredCredential(), is(true));
        VerifiablePresentation zkPT2 = credentialClientModule.generatePresentationToken(policy2);
        W3CVerificationResult result2 = credentialVerifierModule.verifyPresentationToken(zkPT2.toJSONString(), policy2);
        assertEquals(W3CVerificationResult.VALID, result2);
    }

    @Test
    public void testExpiredCredential() throws Exception {
        String username = "userJoe";
        PestoDatabase database = new InMemoryPestoDatabase();
        Set<AttributeDefinition> definitions = new HashSet<>();
        definitions.add(new AttributeDefinitionString("uri:Name", "name", 0, 16));
        definitions.add(new AttributeDefinitionInteger("uri:Age", "age", 0, 123));
        definitions.add(new AttributeDefinitionInteger("uri:Height", "height", 10, 300));
        Map<String, Attribute> userAttr = new HashMap<>();
        userAttr.put("name", new Attribute("Joe"));
        userAttr.put("age", new Attribute(21));
        userAttr.put("height", new Attribute(170));
        database.addUser(username, null, 1);
        database.addAttributes(username, userAttr);
        //Create and credentialGenerator module for each server.
        Map<Integer, CredentialGenerator> mapServers = new HashMap<>();
        PabcPublicParameters publicParams = null;
        for (int i = 0; i < nServers; i++) {
            CredentialGenerator credentialServerModule = new ThresholdPSSharesGenerator(database, seed, i);
            PABCConfigurationImpl config = new PABCConfigurationImpl();
            config.setAttrDefinitions(definitions);
            config.setSeed((seed.toString()+i).getBytes());
            config.setLifetime(2000);
            config.setAllowedTimeDifference(allowedTimeDifference);
            config.setServers(Arrays.asList("1", "2"));
            credentialServerModule.setup(config);
            publicParams = credentialServerModule.getPublicParam();
            mapServers.put(i, credentialServerModule);
        }
        MSpublicParam schemePublicParam = new PSpublicParam(publicParams.getEncodedSchemePublicParam());
        //Obtain publicKeyShares and aggregatedKey
        Map<Integer, MSverfKey> verificationKeyShares = new HashMap<>();
        MSverfKey[] verificationKeys = new MSverfKey[nServers];
        int i = 0;
        for (Integer id : mapServers.keySet()) {
            MSverfKey key = (MSverfKey) mapServers.get(id).getVerificationKeyShare();
            verificationKeys[i] = key;
            verificationKeyShares.put(id, key);
            i++;
        }
        PSms auxSignScheme = new PSms();
        auxSignScheme.setup(schemePublicParam.getN(), schemePublicParam.getAuxArg(), seed);
        MSverfKey aggregatedVerificationKey = auxSignScheme.kAggreg(verificationKeys);
        //Setup client module
        CredentialManagement credentialClientModule = new PSCredentialManagement(true, new InMemoryCredentialStorage(), 60);
        ((PSCredentialManagement) credentialClientModule).setup(publicParams, verificationKeyShares, seed);
        //Setup verifier module
        OLVerificationLibrary credentialVerifierLibrary = new OLVerificationLibraryPS();
        ((OLVerificationLibraryPS) credentialVerifierLibrary).setup(publicParams, aggregatedVerificationKey, seed);
        W3CPresentationVerifier credentialVerifierModule = new W3CPresentationVerifierOL(credentialVerifierLibrary);
        //*********Credential creation, proof of policy and verification***********
        String signedMessage = "signedMessage";
        //Policy creation
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(new Predicate("uri:Name", Operation.REVEAL, null));
        Policy policy = new Policy(predicates, signedMessage);
        //Client receives a credential share from each IdP (simplified) and passes them to the credential manager
        Map<Integer, VerifiableCredential> credentialShares = new HashMap<>();
        long timestamp = System.currentTimeMillis();
        for (Integer id : mapServers.keySet()) {
            credentialShares.put(id, mapServers.get(id).createCredentialShare(username, timestamp));
        }
        VerifiablePresentation zkPT = credentialClientModule.combineAndGeneratePresentationToken(credentialShares, policy);
        //Client uses module to generate presentation token for the policy
        //Verification of the presentation token
        W3CVerificationResult result = credentialVerifierModule.verifyPresentationToken(zkPT.toJSONString(), policy);
        assertThat(result, is(W3CVerificationResult.VALID));
        TimeUnit.SECONDS.sleep(3);
        result = credentialVerifierModule.verifyPresentationToken(zkPT.toJSONString(), policy);
        assertThat(result, is(W3CVerificationResult.BAD_TIMESTAMP));
    }

    @Test
    public void testExpiredPresentation() throws Exception {
        String username = "userJoe";
        PestoDatabase database = new InMemoryPestoDatabase();
        Set<AttributeDefinition> definitions = new HashSet<>();
        definitions.add(new AttributeDefinitionString("uri:Name", "name", 0, 16));
        definitions.add(new AttributeDefinitionInteger("uri:Age", "age", 0, 123));
        definitions.add(new AttributeDefinitionInteger("uri:Height", "height", 10, 300));
        Map<String, Attribute> userAttr = new HashMap<>();
        userAttr.put("name", new Attribute("Joe"));
        userAttr.put("age", new Attribute(21));
        userAttr.put("height", new Attribute(170));
        database.addUser(username, null, 1);
        database.addAttributes(username, userAttr);
        //Create and credentialGenerator module for each server.
        Map<Integer, CredentialGenerator> mapServers = new HashMap<>();
        PabcPublicParameters publicParams = null;
        for (int i = 0; i < nServers; i++) {
            CredentialGenerator credentialServerModule = new ThresholdPSSharesGenerator(database, seed, i);
            PABCConfigurationImpl config = new PABCConfigurationImpl();
            config.setAttrDefinitions(definitions);
            config.setSeed((seed.toString()+i).getBytes());
            config.setLifetime(lifetime);
            config.setAllowedTimeDifference(allowedTimeDifference);
            config.setServers(Arrays.asList("1", "2"));
            credentialServerModule.setup(config);
            publicParams = credentialServerModule.getPublicParam();
            mapServers.put(i, credentialServerModule);
        }
        MSpublicParam schemePublicParam = new PSpublicParam(publicParams.getEncodedSchemePublicParam());
        //Obtain publicKeyShares and aggregatedKey
        Map<Integer, MSverfKey> verificationKeyShares = new HashMap<>();
        MSverfKey[] verificationKeys = new MSverfKey[nServers];
        int i = 0;
        for (Integer id : mapServers.keySet()) {
            MSverfKey key = (MSverfKey) mapServers.get(id).getVerificationKeyShare();
            verificationKeys[i] = key;
            verificationKeyShares.put(id, key);
            i++;
        }
        PSms auxSignScheme = new PSms();
        auxSignScheme.setup(schemePublicParam.getN(), schemePublicParam.getAuxArg(), seed);
        MSverfKey aggregatedVerificationKey = auxSignScheme.kAggreg(verificationKeys);
        //Setup client module
        CredentialManagement credentialClientModule = new PSCredentialManagement(true, new InMemoryCredentialStorage(), 1);
        ((PSCredentialManagement) credentialClientModule).setup(publicParams, verificationKeyShares, seed);
        //Setup verifier module
        OLVerificationLibrary credentialVerifierLibrary = new OLVerificationLibraryPS();
        ((OLVerificationLibraryPS) credentialVerifierLibrary).setup(publicParams, aggregatedVerificationKey, seed);
        W3CPresentationVerifier credentialVerifierModule = new W3CPresentationVerifierOL(credentialVerifierLibrary);
        //*********Credential creation, proof of policy and verification***********
        String signedMessage = "signedMessage";
        //Policy creation
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(new Predicate("uri:Name", Operation.REVEAL, null));
        Policy policy = new Policy(predicates, signedMessage);
        //Client receives a credential share from each IdP (simplified) and passes them to the credential manager
        Map<Integer, VerifiableCredential> credentialShares = new HashMap<>();
        long timestamp = System.currentTimeMillis();
        for (Integer id : mapServers.keySet()) {
            credentialShares.put(id, mapServers.get(id).createCredentialShare(username, timestamp));
        }
        VerifiablePresentation zkPT = credentialClientModule.combineAndGeneratePresentationToken(credentialShares, policy);
        //Client uses module to generate presentation token for the policy
        //Verification of the presentation token
        W3CVerificationResult result = credentialVerifierModule.verifyPresentationToken(zkPT.toJSONString(), policy);
        assertThat(result, is(W3CVerificationResult.VALID));
        TimeUnit.SECONDS.sleep(2);
        result = credentialVerifierModule.verifyPresentationToken(zkPT.toJSONString(), policy);
        assertThat(result, is(W3CVerificationResult.BAD_TIMESTAMP));
    }


    @Test(expected = IllegalArgumentException.class)
    public void testWrongTimestamp() throws Exception {
        String username = "userJoe";
        PestoDatabase database = new InMemoryPestoDatabase();
        Set<AttributeDefinition> definitions = new HashSet<>();
        definitions.add(new AttributeDefinitionString("uri:Name", "name", 0, 16));
        definitions.add(new AttributeDefinitionInteger("uri:Age", "age", 0, 123));
        definitions.add(new AttributeDefinitionInteger("uri:Height", "height", 10, 300));
        Map<String, Attribute> userAttr = new HashMap<>();
        userAttr.put("name", new Attribute("Joe"));
        userAttr.put("age", new Attribute(21));
        userAttr.put("height", new Attribute(170));
        database.addUser(username, null, 1);
        database.addAttributes(username, userAttr);
        //Create and credentialGenerator module for each server.
        Map<Integer, CredentialGenerator> mapServers = new HashMap<>();
        PabcPublicParameters publicParams = null;
        for (int i = 0; i < nServers; i++) {
            CredentialGenerator credentialServerModule = new ThresholdPSSharesGenerator(database, seed, i);
            PABCConfigurationImpl config = new PABCConfigurationImpl();
            config.setAttrDefinitions(definitions);
            config.setSeed((seed.toString()+i).getBytes());
            config.setLifetime(lifetime);
            config.setAllowedTimeDifference(allowedTimeDifference);
            config.setServers(Arrays.asList("1", "2"));
            credentialServerModule.setup(config);
            publicParams = credentialServerModule.getPublicParam();
            mapServers.put(i, credentialServerModule);
        }
        MSpublicParam schemePublicParam = new PSpublicParam(publicParams.getEncodedSchemePublicParam());
        //Obtain publicKeyShares and aggregatedKey
        Map<Integer, MSverfKey> verificationKeyShares = new HashMap<>();
        MSverfKey[] verificationKeys = new MSverfKey[nServers];
        int i = 0;
        for (Integer id : mapServers.keySet()) {
            MSverfKey key = (MSverfKey) mapServers.get(id).getVerificationKeyShare();
            verificationKeys[i] = key;
            verificationKeyShares.put(id, key);
            i++;
        }
        PSms auxSignScheme = new PSms();
        auxSignScheme.setup(schemePublicParam.getN(), schemePublicParam.getAuxArg(), seed);
        //Client receives a credential share from each IdP (simplified) and passes them to the credential manager
        Map<Integer, VerifiableCredential> credentialShares = new HashMap<>();
        long timestamp = System.currentTimeMillis() + 11000; // As in the user tries to say it was 11 seconds later
        for (Integer id : mapServers.keySet()) {
            credentialShares.put(id, mapServers.get(id).createCredentialShare(username, timestamp));
        }
    }

    @Test(expected = SetupException.class)
    public void testCredentialManagementBadSetupServers() throws Exception {
        List<PabcIdP> servers = new LinkedList<>();
        PabcIdP idp = mock(PabcIdP.class);
        doAnswer(invocationOnMock -> {
            MSpublicParam param = mock(MSpublicParam.class);
            doReturn(0).when(param).getN();
            doReturn(new PSauxArg("pairingName", new HashSet<String>())).when(param).getAuxArg();
            doReturn("WrongEncoded").when(param).getEncoded();
            return new PabcPublicParameters(new HashSet<>(),param.getEncoded());
        }).when(idp).getPabcPublicParam();
        servers.add(idp);
        PSCredentialManagement credentialManagement = new PSCredentialManagement(false, null, 60);
        credentialManagement.setup(servers, seed);
        fail("Should throw RuntimeException");
    }

    @Test
    public void testPolicyNotfulfilled() throws Exception {
        String username = "userJoe";
        PestoDatabase database = new InMemoryPestoDatabase();
        Set<AttributeDefinition> definitions = new HashSet<>();
        definitions.add(new AttributeDefinitionString("uri:Name", "name", 0, 16));
        definitions.add(new AttributeDefinitionInteger("uri:Age", "age", 0, 123));
        definitions.add(new AttributeDefinitionInteger("uri:Height", "height", 10, 300));
        Map<String, Attribute> userAttr = new HashMap<>();
        userAttr.put("name", new Attribute("Joe"));
        userAttr.put("age", new Attribute(21));
        userAttr.put("height", new Attribute(170));
        database.addUser(username, null, 1);
        database.addAttributes(username, userAttr);
        //Create and credentialGenerator module for each server.
        Map<Integer, CredentialGenerator> mapServers = new HashMap<>();
        PabcPublicParameters publicParams = null;
        for (int i = 0; i < nServers; i++) {
            CredentialGenerator credentialServerModule = new ThresholdPSSharesGenerator(database, seed, i);
            PABCConfigurationImpl config = new PABCConfigurationImpl();
            config.setAttrDefinitions(definitions);
            config.setSeed((seed.toString()+i).getBytes());
            config.setLifetime(lifetime);
            config.setAllowedTimeDifference(allowedTimeDifference);
            config.setServers(Arrays.asList("1", "2"));
            credentialServerModule.setup(config);
            publicParams = credentialServerModule.getPublicParam();
            mapServers.put(i, credentialServerModule);
        }
        MSpublicParam schemePublicParam = new PSpublicParam(publicParams.getEncodedSchemePublicParam());
        //Obtain publicKeyShares and aggregatedKey
        Map<Integer, MSverfKey> verificationKeyShares = new HashMap<>();
        MSverfKey[] verificationKeys = new MSverfKey[nServers];
        int i = 0;
        for (Integer id : mapServers.keySet()) {
            MSverfKey key = (MSverfKey) mapServers.get(id).getVerificationKeyShare();
            verificationKeys[i] = key;
            verificationKeyShares.put(id, key);
            i++;
        }
        PSms auxSignScheme = new PSms();
        auxSignScheme.setup(schemePublicParam.getN(), schemePublicParam.getAuxArg(), seed);
        MSverfKey aggregatedVerificationKey = auxSignScheme.kAggreg(verificationKeys);
        //Setup client module
        CredentialManagement credentialClientModule = new PSCredentialManagement(true, new InMemoryCredentialStorage(), 60);
        ((PSCredentialManagement) credentialClientModule).setup(publicParams, verificationKeyShares, seed);
        //Setup verifier module
        OLVerificationLibrary credentialVerifierLibrary = new OLVerificationLibraryPS();
        ((OLVerificationLibraryPS) credentialVerifierLibrary).setup(publicParams, aggregatedVerificationKey, seed);
        W3CPresentationVerifier credentialVerifierModule = new W3CPresentationVerifierOL(credentialVerifierLibrary);
        //*********Credential creation, proof of policy and verification***********
        String signedMessage = "signedMessage";
        //Policy creation
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(new Predicate("uri:Name", Operation.REVEAL, null));
        predicates.add(new Predicate("uri:Age", Operation.REVEAL, null));
        Policy policyRequested = new Policy(predicates, signedMessage);
        List<Predicate> predicatesRevealed = new ArrayList<>();
        predicatesRevealed.add(new Predicate("uri:Name", Operation.REVEAL, null));
        Policy policyRevealed = new Policy(predicatesRevealed, signedMessage);
        //Client receives a credential share from each IdP (simplified) and passes them to the credential manager
        Map<Integer, VerifiableCredential> credentialShares = new HashMap<>();
        long timestamp = System.currentTimeMillis();
        for (Integer id : mapServers.keySet()) {
            credentialShares.put(id, mapServers.get(id).createCredentialShare(username, timestamp));
        }
        VerifiablePresentation zkPT = credentialClientModule.combineAndGeneratePresentationToken(credentialShares, policyRevealed);
        //Client uses module to generate presentation token for the policy
        //Verification of the presentation token
        W3CVerificationResult result = credentialVerifierModule.verifyPresentationToken(zkPT.toJSONString(), policyRequested);
        assertThat(result, is(W3CVerificationResult.POLICY_NOT_FULFILLED));
    }

    @Test
    public void testWrongMessageSigned() throws Exception {
        // Does not make sense for new library because of how PSCredentialManagement includes signed nonce in proof. Can keep it or not depending on what we do with old library
        String username = "userJoe";
        PestoDatabase database = new InMemoryPestoDatabase();
        Set<AttributeDefinition> definitions = new HashSet<>();
        definitions.add(new AttributeDefinitionString("uri:Name", "name", 0, 16));
        definitions.add(new AttributeDefinitionInteger("uri:Age", "age", 0, 123));
        definitions.add(new AttributeDefinitionInteger("uri:Height", "height", 10, 300));
        Map<String, Attribute> userAttr = new HashMap<>();
        userAttr.put("name", new Attribute("Joe"));
        userAttr.put("age", new Attribute(21));
        userAttr.put("height", new Attribute(170));
        database.addUser(username, null, 1);
        database.addAttributes(username, userAttr);
        //Create and credentialGenerator module for each server.
        Map<Integer, CredentialGenerator> mapServers = new HashMap<>();
        PabcPublicParameters publicParams = null;
        for (int i = 0; i < nServers; i++) {
            CredentialGenerator credentialServerModule = new ThresholdPSSharesGenerator(database, seed, i);
            PABCConfigurationImpl config = new PABCConfigurationImpl();
            config.setAttrDefinitions(definitions);
            config.setSeed((seed.toString()+i).getBytes());
            config.setLifetime(lifetime);
            config.setAllowedTimeDifference(allowedTimeDifference);
            config.setServers(Arrays.asList("1", "2"));
            credentialServerModule.setup(config);
            publicParams = credentialServerModule.getPublicParam();
            mapServers.put(i, credentialServerModule);
        }
        MSpublicParam schemePublicParam = new PSpublicParam(publicParams.getEncodedSchemePublicParam());
        //Obtain publicKeyShares and aggregatedKey
        Map<Integer, MSverfKey> verificationKeyShares = new HashMap<>();
        MSverfKey[] verificationKeys = new MSverfKey[nServers];
        int i = 0;
        for (Integer id : mapServers.keySet()) {
            MSverfKey key = (MSverfKey) mapServers.get(id).getVerificationKeyShare();
            verificationKeys[i] = key;
            verificationKeyShares.put(id, key);
            i++;
        }
        PSms auxSignScheme = new PSms();
        auxSignScheme.setup(schemePublicParam.getN(), schemePublicParam.getAuxArg(), seed);
        MSverfKey aggregatedVerificationKey = auxSignScheme.kAggreg(verificationKeys);
        //Setup client module
        CredentialManagement credentialClientModule = new PSCredentialManagement(true, new InMemoryCredentialStorage(), 60);
        ((PSCredentialManagement) credentialClientModule).setup(publicParams, verificationKeyShares, seed);
        //Setup verifier module
        OLVerificationLibrary credentialVerifierLibrary = new OLVerificationLibraryPS();
        ((OLVerificationLibraryPS) credentialVerifierLibrary).setup(publicParams, aggregatedVerificationKey, seed);
        W3CPresentationVerifier credentialVerifierModule = new W3CPresentationVerifierOL(credentialVerifierLibrary);
        //*********Credential creation, proof of policy and verification***********
        String signedMessage = "signedMessage";
        String requestedSignedMessage = "message";
        //Policy creation
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(new Predicate("uri:Name", Operation.REVEAL, null));
        Policy policyRequested = new Policy(predicates, requestedSignedMessage);
        Policy policyCredential = new Policy(predicates, signedMessage);
        //Client receives a credential share from each IdP (simplified) and passes them to the credential manager
        Map<Integer, VerifiableCredential> credentialShares = new HashMap<>();
        long timestamp = System.currentTimeMillis();
        for (Integer id : mapServers.keySet()) {
            credentialShares.put(id, mapServers.get(id).createCredentialShare(username, timestamp));
        }
        VerifiablePresentation zkPT = credentialClientModule.combineAndGeneratePresentationToken(credentialShares, policyCredential);
        //Client uses module to generate presentation token for the policy
        //Verification of the presentation token
        W3CVerificationResult result = credentialVerifierModule.verifyPresentationToken(zkPT.toJSONString(), policyRequested);
        assertThat(result, is(W3CVerificationResult.POLICY_NOT_FULFILLED));
    }



    @Test
    public void testInvalidVerificationsW3C() throws Exception {
        String username = "userJoe";
        PestoDatabase database = new InMemoryPestoDatabase();
        Map<String, Attribute> userAttr = new HashMap<>();
        userAttr.put("name", new Attribute("Joe"));
        userAttr.put("age", new Attribute(21));
        userAttr.put("now", new Attribute(new Date(System.currentTimeMillis())));
        database.addUser(username, null, 1);
        database.addAttributes(username, userAttr);
        //Create and credentialGenerator module for each server.
        Map<Integer, CredentialGenerator> mapServers = new HashMap<>();
        PabcPublicParameters publicParams = null;
        for (int i = 0; i < nServers; i++) {
            CredentialGenerator credentialServerModule = new ThresholdPSSharesGenerator(database, seed, i);
            PABCConfigurationImpl config = new PABCConfigurationImpl();
            config.setAttrDefinitions(generateAttributeDefinitions());
            config.setSeed((seed.toString()+i).getBytes());
            config.setLifetime(lifetime);
            config.setAllowedTimeDifference(allowedTimeDifference);
            config.setServers(Arrays.asList("1", "2"));
            credentialServerModule.setup(config);
            publicParams = credentialServerModule.getPublicParam();
            mapServers.put(i, credentialServerModule);
        }
        MSpublicParam schemePublicParam = new PSpublicParam(publicParams.getEncodedSchemePublicParam());
        //Obtain publicKeyShares and aggregatedKey
        Map<Integer, MSverfKey> verificationKeyShares = new HashMap<>();
        MSverfKey[] verificationKeys = new MSverfKey[nServers];
        int i = 0;
        for (Integer id : mapServers.keySet()) {
            MSverfKey key = (MSverfKey) mapServers.get(id).getVerificationKeyShare();
            verificationKeys[i] = key;
            verificationKeyShares.put(id, key);
            i++;
        }
        PSms auxSignScheme = new PSms();
        auxSignScheme.setup(schemePublicParam.getN(), schemePublicParam.getAuxArg(), seed);
        MSverfKey aggregatedVerificationKey = auxSignScheme.kAggreg(verificationKeys);
        //Setup client module
        CredentialManagement credentialClientModule = new PSCredentialManagement(true, new InMemoryCredentialStorage(), 60);
        ((PSCredentialManagement) credentialClientModule).setup(publicParams, verificationKeyShares, seed);
        //Setup verifier module
        OLVerificationLibrary credentialVerifierLibrary = new OLVerificationLibraryPS();
        ((OLVerificationLibraryPS) credentialVerifierLibrary).setup(publicParams, aggregatedVerificationKey, seed);
        W3CPresentationVerifier credentialVerifierModule = new W3CPresentationVerifierOL(credentialVerifierLibrary);
        //*********Credential creation, proof of policy and verification***********
        String signedMessage = "signedMessage";
        //Policy creation
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(new Predicate("uri:Now", Operation.REVEAL, null));
        predicates.add(new Predicate("uri:Age", Operation.GREATERTHANOREQUAL, new Attribute(18)));
        Policy policyRange = new Policy(predicates, signedMessage);
        //Client receives a credential share from each IdP (simplified) and passes them to the credential manager
        Map<Integer, VerifiableCredential> credentialShares = new HashMap<>();
        long timestamp = System.currentTimeMillis();
        for (Integer id : mapServers.keySet()) {
            credentialShares.put(id, mapServers.get(id).createCredentialShare(username, timestamp));
        }
        VerifiablePresentation vPresentationRange = credentialClientModule.combineAndGeneratePresentationToken(credentialShares, policyRange);
        Proof rangeZkProof=vPresentationRange.getVCCredentials().get(0).obtainVCProof();
        List<Predicate> predicates2 = new ArrayList<>();
        predicates2.add(new Predicate("uri:Now", Operation.REVEAL, null));
        Policy policyNoRange = new Policy(predicates2, signedMessage);
        VerifiablePresentation vPnoRange = credentialClientModule.generatePresentationToken(policyNoRange);
        VerifiablePresentation vpModifiedExpirationDate= VerifiablePresentation.cloneVerifiablePresentation(vPnoRange, Arrays.asList(Verifiable.JSONLD_KEY_PROOF));
        vpModifiedExpirationDate.setExpirationDate(new Date(vPnoRange.getExpirationDate().getTime()+60000));
        vpModifiedExpirationDate.getVCCredentials().get(0).setProof(new Proof("OlPsDerivedProof", vPnoRange.getVCCredentials().get(0).obtainVCProof().getProofValue(), "did:vIdP", signedMessage,vPnoRange.getVCCredentials().get(0).getExpirationDate().getTime(),"AssertionMethod", null));
        VerifiablePresentation vpZkPTbadSignatureEpochModifiedNoRange = VerifiablePresentation.cloneVerifiablePresentation(vPnoRange, Arrays.asList(Verifiable.JSONLD_KEY_PROOF));
        Date fakeExpiration=fromRFC3339UTC("2030-01-05T01:12:31");
        vpZkPTbadSignatureEpochModifiedNoRange.getVCCredentials().get(0).setProof(new Proof("OlPsDerivedProof", vPnoRange.getVCCredentials().get(0).obtainVCProof().getProofValue(), "did:vIdP", signedMessage, fakeExpiration.getTime(), "AssertionMethod", null));
        vpZkPTbadSignatureEpochModifiedNoRange.getVCCredentials().get(0).setExpirationDate(fakeExpiration);
        // Range tokens keys different from range preds
        VerifiablePresentation vpZkPTbadRangeTokens = VerifiablePresentation.cloneVerifiablePresentation(vPresentationRange, Arrays.asList(Verifiable.JSONLD_KEY_PROOF));
        vpZkPTbadRangeTokens.getVCCredentials().get(0).setProof(new Proof("OlPsDerivedProofRange", rangeZkProof.getProofValue(), "did:vIdP", signedMessage, rangeZkProof.getEpoch(), "AssertionMethod", null));
        // Reconstructed token of different type for both (simply cross tokens/policies in two verifications)
        // PS Signature failed in range case
        VerifiablePresentation vpZkPTbadSignatureEpochModified = VerifiablePresentation.cloneVerifiablePresentation(vPresentationRange, Arrays.asList(Verifiable.JSONLD_KEY_PROOF));
        vpZkPTbadSignatureEpochModified.getVCCredentials().get(0).setProof(new Proof("OlPsDerivedProofRange", rangeZkProof.getProofValue(), "did:vIdP", signedMessage, fakeExpiration.getTime(), "AssertionMethod", rangeZkProof.getRangeProofs()));
        vpZkPTbadSignatureEpochModified.getVCCredentials().get(0).setExpirationDate(fakeExpiration);
        // RangePredicate proof failed
        Map<String, RangePredicateToken> badRangeTokens = W3CSerializationUtil.extractRangeTokens(rangeZkProof).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e ->
                        new RangePredicateToken(e.getValue().getProofLowerBound(),
                                e.getValue().getProofLowerBound(),
                                e.getValue().getCommitV())));
        VerifiablePresentation vpZkPTbadRangeToken = VerifiablePresentation.cloneVerifiablePresentation(vPresentationRange, Arrays.asList(Verifiable.JSONLD_KEY_PROOF));
        vpZkPTbadRangeToken.getVCCredentials().get(0).setProof(new Proof("OlPsDerivedProofRange", rangeZkProof.getProofValue(), "did:vIdP", signedMessage, rangeZkProof.getEpoch(), "assertionMethod", getProofRanges(badRangeTokens)));
        assertThat(credentialVerifierModule.verifyPresentationToken(vPnoRange.toJSONString(), policyNoRange), is(W3CVerificationResult.VALID));
        assertThat(credentialVerifierModule.verifyPresentationToken(vPresentationRange.toJSONString(), policyRange), is(W3CVerificationResult.VALID));
        assertThat(credentialVerifierModule.verifyPresentationToken(vPresentationRange.toJSONString(), policyNoRange), is(W3CVerificationResult.INVALID_SIGNATURE));
        assertThat(credentialVerifierModule.verifyPresentationToken(vPnoRange.toJSONString(), policyRange), is(W3CVerificationResult.POLICY_NOT_FULFILLED));
        assertThat(credentialVerifierModule.verifyPresentationToken(vpZkPTbadRangeTokens.toJSONString(), policyRange), is(W3CVerificationResult.INVALID_SIGNATURE));
        assertThat(credentialVerifierModule.verifyPresentationToken(vpZkPTbadSignatureEpochModified.toJSONString(), policyRange), is(W3CVerificationResult.INVALID_SIGNATURE));
        assertThat(credentialVerifierModule.verifyPresentationToken(vpZkPTbadSignatureEpochModifiedNoRange.toJSONString(), policyNoRange), is(W3CVerificationResult.INVALID_SIGNATURE));
        assertThat(credentialVerifierModule.verifyPresentationToken(vpZkPTbadRangeToken.toJSONString(), policyRange), is(W3CVerificationResult.INVALID_SIGNATURE));
    }

    @Test
    public void testPSCredManagerSetupExceptions() throws Exception {
        PSCredentialManagement manager = new PSCredentialManagement(true, new InMemoryCredentialStorage(), 60);
        PSms psScheme = new PSms();
        int n = 3;
        MSpublicParam pp = psScheme.setup(n, new PSauxArg("eu.olympus.util.pairingBLS461.PairingBuilderBLS461", new HashSet<>(Arrays.asList("uri:Age", "uri:Name", "uri:Now"))), seed);
        //Create wrong elements
        Map<Integer, MSverfKey> correctNumberVks = new HashMap<>();
        correctNumberVks.put(1, MockFactory.mockVerfKey());
        correctNumberVks.put(2, MockFactory.mockVerfKey());
        correctNumberVks.put(3, MockFactory.mockVerfKey());
        Map<Integer, MSverfKey> wrongNumberVks = new HashMap<>();
        wrongNumberVks.put(1, MockFactory.mockVerfKey());
        MSverfKey mockKey = MockFactory.mockVerfKey();
        MSpublicParam wrongPP = new PSpublicParam(n, new PSauxArg("wrongName", new HashSet<>(Arrays.asList("uri:Age", "uri:Name", "uri:Now"))));
        MSpublicParam differentAttrPublicParam = new PSpublicParam(n, new PSauxArg("eu.olympus.util.pairingBLS461.PairingBuilderBLS461", new HashSet<>(Arrays.asList("uri:Age", "uri:Name"))));
        PabcIdP mockIdpConflict = mockIdpForSetup(new PabcPublicParameters(generateAttributeDefinitions(), differentAttrPublicParam.getEncoded()));
        PabcIdP mockIdpWrongPP = mockIdpForSetup(new PabcPublicParameters(generateAttributeDefinitions(), wrongPP.getEncoded()));
        PabcIdP mockIdpWrongPPSerial = mockIdpForSetup(new PabcPublicParameters(generateAttributeDefinitions(), "wrongExtra" + pp.getEncoded()));
        try {
            manager.setup(Collections.singletonList(mockIdpConflict), seed);
            fail("Should throw SetupException, listIdp conflictingAttributeNames");
        } catch (SetupException e) {
        }
        try {
            manager.setup(Collections.singletonList(mockIdpWrongPP), seed);
            fail("Should throw SetupException, listIdp wrongPP");
        } catch (SetupException e) {
        }
        try {
            manager.setup(Collections.singletonList(mockIdpWrongPPSerial), seed);
            fail("Should throw SetupException, listIdp Could not retrieve scheme public param");
        } catch (SetupException e) {
        }
        try {
            manager.setup(new PabcPublicParameters(generateAttributeDefinitions(), differentAttrPublicParam.getEncoded()), correctNumberVks, seed);
            fail("Should throw SetupException, conflictingAttributeNames");
        } catch (SetupException e) {
        }
        try {
            manager.setup(new PabcPublicParameters(generateAttributeDefinitions(), wrongPP.getEncoded()), correctNumberVks, seed);
            fail("Should throw SetupException, wrongPP");
        } catch (SetupException e) {
        }
        try {
            manager.setup(new PabcPublicParameters(generateAttributeDefinitions(), "wrongExtra" + pp.getEncoded()), wrongNumberVks, seed);
            fail("Should throw SetupException, Could not retrieve scheme public param");
        } catch (SetupException e) {
        }
        try {
            manager.setup(new PabcPublicParameters(generateAttributeDefinitions(), pp.getEncoded()), wrongNumberVks, seed);
            fail("Should throw SetupException, wrong number of vks");
        } catch (SetupException e) {
        }
        try {
            manager.setup(new PabcPublicParameters(generateAttributeDefinitions(), pp.getEncoded()), correctNumberVks, seed);
            fail("Should throw IllegalArgumentException (from PSMS), wrong vk type");
        } catch (IllegalArgumentException e) {
        }
        try {
            manager.setupForOffline(new PabcPublicParameters(generateAttributeDefinitions(), differentAttrPublicParam.getEncoded()), mockKey, seed);
            fail("Should throw SetupException, setupOffline conflictingAttributeNames");
        } catch (SetupException e) {
        }
        try {
            manager.setupForOffline(new PabcPublicParameters(generateAttributeDefinitions(), wrongPP.getEncoded()), mockKey, seed);
            fail("Should throw SetupException, setupOffline wrongPP");
        } catch (SetupException e) {
        }
        try {
            manager.setupForOffline(new PabcPublicParameters(generateAttributeDefinitions(), "wrongExtra" + pp.getEncoded()), mockKey, seed);
            fail("Should throw SetupException, setupOffline Could not retrieve scheme public param");
        } catch (SetupException e) {
        }

    }

    @Test
    public void testPSCredManagerCheckStoredCredential() throws Exception {
        String username = "userJoe";
        PestoDatabase database = new InMemoryPestoDatabase();
        Set<String> attrNames = new HashSet<>(Arrays.asList("uri:Name", "uri:Age", "uri:Now"));
        Map<String, Attribute> userAttr = new HashMap<>();
        userAttr.put("name", new Attribute("Joe"));
        userAttr.put("age", new Attribute(21));
        userAttr.put("now", new Attribute(new Date(System.currentTimeMillis())));
        database.addUser(username, null, 1);
        database.addAttributes(username, userAttr);
        //Create and credentialGenerator module for each server.
        Map<Integer, CredentialGenerator> mapServers = new HashMap<>();
        PabcPublicParameters publicParams = null;
        for (int i = 0; i < nServers; i++) {
            CredentialGenerator credentialServerModule = new ThresholdPSSharesGenerator(database, seed, i);
            PABCConfigurationImpl config = new PABCConfigurationImpl();
            config.setAttrDefinitions(generateAttributeDefinitions());
            config.setSeed((seed.toString()+i).getBytes());
            config.setLifetime(2000);
            config.setAllowedTimeDifference(allowedTimeDifference);
            config.setServers(Arrays.asList("1", "2"));
            credentialServerModule.setup(config);
            publicParams = credentialServerModule.getPublicParam();
            mapServers.put(i, credentialServerModule);
        }
        MSpublicParam schemePublicParam = new PSpublicParam(publicParams.getEncodedSchemePublicParam());
        //Obtain publicKeyShares and aggregatedKey
        Map<Integer, MSverfKey> verificationKeyShares = new HashMap<>();
        MSverfKey[] verificationKeys = new MSverfKey[nServers];
        int i = 0;
        for (Integer id : mapServers.keySet()) {
            MSverfKey key = (MSverfKey) mapServers.get(id).getVerificationKeyShare();
            verificationKeys[i] = key;
            verificationKeyShares.put(id, key);
            i++;
        }
        PSms auxSignScheme = new PSms();
        auxSignScheme.setup(schemePublicParam.getN(), schemePublicParam.getAuxArg(), seed);
        //Setup client module
        CredentialManagement credentialClientModule = new PSCredentialManagement(true, new InMemoryCredentialStorage(), 60);
        ((PSCredentialManagement) credentialClientModule).setup(publicParams, verificationKeyShares, seed);

        //*********Credential creation, proof of policy and verification***********
        String signedMessage = "signedMessage";
        //Policy creation
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(new Predicate("uri:Now", Operation.REVEAL, null));
        predicates.add(new Predicate("uri:Age", Operation.REVEAL, null));
        Policy policy = new Policy(predicates, signedMessage);
        //Client receives a credential share from each IdP (simplified) and passes them to the credential manager
        Map<Integer, VerifiableCredential> credentialShares = new HashMap<>();
        long timestamp = System.currentTimeMillis();
        for (Integer id : mapServers.keySet()) {
            credentialShares.put(id, mapServers.get(id).createCredentialShare(username, timestamp));
        }
        assertThat(credentialClientModule.checkStoredCredential(), is(false));
        credentialClientModule.combineAndGeneratePresentationToken(credentialShares, policy);
        assertThat(credentialClientModule.checkStoredCredential(), is(true));
        TimeUnit.SECONDS.sleep(3);
        assertThat(credentialClientModule.checkStoredCredential(), is(false));
        PSCredentialManagement credentialClientModule2 = new PSCredentialManagement(false, null, 60);
        credentialClientModule2.setup(publicParams, verificationKeyShares, seed);
        assertThat(credentialClientModule2.checkStoredCredential(), is(false));
    }

    @Test
    public void testPSCredManagerGeneratePresentationTokenExceptions() throws Exception {
        String username = "userJoe";
        PestoDatabase database = new InMemoryPestoDatabase();
        Set<String> attrNames = new HashSet<>(Arrays.asList("uri:Name", "uri:Age", "uri:Now"));
        Map<String, Attribute> userAttr = new HashMap<>();
        userAttr.put("name", new Attribute("John"));
        userAttr.put("now", new Attribute(new Date(System.currentTimeMillis())));
        database.addUser(username, null, 1);
        database.addAttributes(username, userAttr);
        //Create and credentialGenerator module for each server.
        Map<Integer, CredentialGenerator> mapServers = new HashMap<>();
        PabcPublicParameters publicParams = null;
        for (int i = 0; i < nServers; i++) {
            CredentialGenerator credentialServerModule = new ThresholdPSSharesGenerator(database, seed, i);
            PABCConfigurationImpl config = new PABCConfigurationImpl();
            config.setAttrDefinitions(generateAttributeDefinitions());
            config.setSeed((seed.toString()+i).getBytes());
            config.setLifetime(lifetime);
            config.setAllowedTimeDifference(allowedTimeDifference);
            config.setServers(Arrays.asList("1", "2"));
            credentialServerModule.setup(config);
            publicParams = credentialServerModule.getPublicParam();
            mapServers.put(i, credentialServerModule);
        }
        MSpublicParam schemePublicParam = new PSpublicParam(publicParams.getEncodedSchemePublicParam());
        //Obtain publicKeyShares and aggregatedKey
        Map<Integer, MSverfKey> verificationKeyShares = new HashMap<>();
        MSverfKey[] verificationKeys = new MSverfKey[nServers];
        int i = 0;
        for (Integer id : mapServers.keySet()) {
            MSverfKey key = (MSverfKey) mapServers.get(id).getVerificationKeyShare();
            verificationKeys[i] = key;
            verificationKeyShares.put(id, key);
            i++;
        }
        PSms auxSignScheme = new PSms();
        auxSignScheme.setup(schemePublicParam.getN(), schemePublicParam.getAuxArg(), seed);
        MSverfKey aggregatedVerificationKey = auxSignScheme.kAggreg(verificationKeys);
        //Setup client module
        CredentialManagement credentialClientModule = new PSCredentialManagement(true, new InMemoryCredentialStorage(), 60);
        String signedMessage = "signedMessage";
        //Policy creation
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(new Predicate("uri:Now", Operation.REVEAL, null));
        predicates.add(new Predicate("uri:Name", Operation.REVEAL, null));
        Policy policy = new Policy(predicates, signedMessage);
        List<Predicate> predicates2 = new ArrayList<>();
        predicates2.add(new Predicate("uri:Name", Operation.REVEAL, null));
        predicates2.add(new Predicate("uri:Age", Operation.REVEAL, null));
        Policy policy2 = new Policy(predicates2, signedMessage);
        List<Predicate> predicates3 = new ArrayList<>();
        predicates3.add(new Predicate("uri:Name", Operation.EQ, null));
        Policy policy3 = new Policy(predicates3, signedMessage);
        List<Predicate> predicates4 = new ArrayList<>();
        predicates4.add(new Predicate("uri:Age", Operation.GREATERTHANOREQUAL, new Attribute(10)));
        Policy policy4 = new Policy(predicates4, signedMessage);
        List<Predicate> predicates5 = new ArrayList<>();
        predicates5.add(new Predicate("uri:Now", Operation.GREATERTHANOREQUAL, new Attribute(10)));
        predicates5.add(new Predicate("uri:Now", Operation.LESSTHANOREQUAL, new Attribute(20)));
        Policy policy5 = new Policy(predicates5, signedMessage);
        try {
            credentialClientModule.generatePresentationToken(policy);
            fail("Should throw IllegalStateException, no setup");
        } catch (IllegalStateException e) {
        }
        ((PSCredentialManagement) credentialClientModule).setup(publicParams, verificationKeyShares, seed);
        //Client uses module to generate presentation token for the policy
        try {
            credentialClientModule.generatePresentationToken(policy);
            fail("Should throw IllegalStateException, no credential");
        } catch (IllegalStateException e) {
        }
        Map<Integer, VerifiableCredential> goodCredentialShares = new HashMap<>();
        long timestamp = System.currentTimeMillis();
        for (int id = 0; id < nServers; id++) {
            goodCredentialShares.put(id, mapServers.get(id).createCredentialShare(username, timestamp));
        }
        Assert.assertNotNull(credentialClientModule.combineAndGeneratePresentationToken(goodCredentialShares, policy));
        Assert.assertNotNull(credentialClientModule.generatePresentationToken(policy));
        try {
            credentialClientModule.generatePresentationToken(policy2);
            fail("Should throw TokenGenerationException, unfulfilled policy reveal");
        } catch (TokenGenerationException e) {
        }
        try {
            credentialClientModule.generatePresentationToken(policy3);
            fail("Should throw TokenGenerationException, unsupported policy");
        } catch (TokenGenerationException e) {
        }
        try {
            credentialClientModule.generatePresentationToken(policy4);
            fail("Should throw TokenGenerationException, unfulfilled policy range");
        } catch (TokenGenerationException ignored) {
        }
        try {
            credentialClientModule.generatePresentationToken(policy5);
            fail("Should throw TokenGenerationException, illegal policy repeated");
        } catch (TokenGenerationException e) {
        }
    }

    @Test
    public void testPSCredManagerCombineAndGeneratePresentationTokenExceptions() throws Exception {
        String username = "userJoe";
        PestoDatabase database = new InMemoryPestoDatabase();
        Map<String, Attribute> userAttr = new HashMap<>();
        userAttr.put("age", new Attribute(21));
        userAttr.put("now", new Attribute(new Date(System.currentTimeMillis())));
        database.addUser(username, null, 1);
        database.addAttributes(username, userAttr);
        //Create and credentialGenerator module for each server.
        Map<Integer, CredentialGenerator> mapServers = new HashMap<>();
        PabcPublicParameters publicParams = null;
        for (int i = 0; i < nServers; i++) {
            CredentialGenerator credentialServerModule = new ThresholdPSSharesGenerator(database, seed, i);
            PABCConfigurationImpl config = new PABCConfigurationImpl();
            config.setAttrDefinitions(generateAttributeDefinitions());
            config.setSeed((seed.toString()+i).getBytes());
            config.setLifetime(lifetime);
            config.setAllowedTimeDifference(allowedTimeDifference);
            config.setServers(Arrays.asList("1", "2"));
            credentialServerModule.setup(config);
            publicParams = credentialServerModule.getPublicParam();
            mapServers.put(i, credentialServerModule);
        }
        MSpublicParam schemePublicParam = new PSpublicParam(publicParams.getEncodedSchemePublicParam());
        //Obtain publicKeyShares and aggregatedKey
        Map<Integer, MSverfKey> verificationKeyShares = new HashMap<>();
        MSverfKey[] verificationKeys = new MSverfKey[nServers];
        int i = 0;
        for (Integer id : mapServers.keySet()) {
            MSverfKey key = (MSverfKey) mapServers.get(id).getVerificationKeyShare();
            verificationKeys[i] = key;
            verificationKeyShares.put(id, key);
            i++;
        }
        PSms auxSignScheme = new PSms();
        auxSignScheme.setup(schemePublicParam.getN(), schemePublicParam.getAuxArg(), seed);
        //Setup client module
        CredentialManagement credentialClientModule = new PSCredentialManagement(false, null, 60);
        String signedMessage = "signedMessage";
        //Policy creation
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(new Predicate("uri:Now", Operation.REVEAL, null));
        predicates.add(new Predicate("uri:Age", Operation.REVEAL, null));
        Policy policy = new Policy(predicates, signedMessage);
        List<Predicate> predicates2 = new ArrayList<>();
        predicates2.add(new Predicate("uri:Name", Operation.REVEAL, null));
        predicates2.add(new Predicate("uri:Age", Operation.REVEAL, null));
        Policy policy2 = new Policy(predicates2, signedMessage);
        try {
            credentialClientModule.combineAndGeneratePresentationToken(null, policy);
            fail("Should throw IllegalStateException, no setup");
        } catch (IllegalStateException e) {
        }
        ((PSCredentialManagement) credentialClientModule).setup(publicParams, verificationKeyShares, seed);
        //Create wrong shares
        Map<Integer, VerifiableCredential> wrongNumberCredentialShares = new HashMap<>();
        long timestamp = System.currentTimeMillis();
        for (int id = 0; id < nServers - 1; id++)
            wrongNumberCredentialShares.put(id, mapServers.get(id).createCredentialShare(username, timestamp));
        Map<Integer, VerifiableCredential> wrongIdsCredentialShares = new HashMap<>();
        for (int id = 0; id < nServers; id++)
            wrongIdsCredentialShares.put(id + 1, mapServers.get(id).createCredentialShare(username, timestamp));
        Map<Integer, VerifiableCredential> wrongCombinationShares = new HashMap<>();
        for (int id = 0; id < nServers; id++)
            wrongCombinationShares.put(id, mapServers.get(id).createCredentialShare(username, timestamp + id * 1000));
        Map<Integer, VerifiableCredential> goodCredentialShares = new HashMap<>();
        for (int id = 0; id < nServers; id++)
            goodCredentialShares.put(id, mapServers.get(id).createCredentialShare(username, timestamp));
        try {
            credentialClientModule.combineAndGeneratePresentationToken(wrongNumberCredentialShares, policy);
            fail("Should throw TokenGenerationException, wrong number credential shares");
        } catch (TokenGenerationException e) {
        }
        try {
            credentialClientModule.combineAndGeneratePresentationToken(wrongIdsCredentialShares, policy);
            fail("Should throw TokenGenerationException, wrong ids shares");
        } catch (TokenGenerationException e) {
        }
        try {
            credentialClientModule.combineAndGeneratePresentationToken(goodCredentialShares, policy2);
            fail("Should throw TokenGenerationException, unfulfilled policy");
        } catch (TokenGenerationException e) {
        }
        try {
            credentialClientModule.combineAndGeneratePresentationToken(wrongCombinationShares, policy);
            fail("Should throw TokenGenerationException, unfulfilled policy");
        } catch (TokenGenerationException e) {
        }
        try {
            credentialClientModule.combineAndGeneratePresentationToken(wrongCombinationShares, policy);
            fail("Should throw TokenGenerationException, combined credential verification failed");
        } catch (TokenGenerationException e) {
        }
        Assert.assertNotNull(credentialClientModule.combineAndGeneratePresentationToken(goodCredentialShares, policy));
    }


    @Test
    public void credentialManagerGetPublicParam() throws Exception {
        String username = "userJoe";
        PestoDatabase database = new InMemoryPestoDatabase();
        Map<String, Attribute> userAttr = new HashMap<>();
        userAttr.put("name", new Attribute("Joe"));
        userAttr.put("age", new Attribute(21));
        userAttr.put("now", new Attribute(new Date(System.currentTimeMillis())));
        database.addUser(username, null, 1);
        database.addAttributes(username, userAttr);
        //Create and credentialGenerator module for each server.
        Map<Integer, CredentialGenerator> mapServers = new HashMap<>();
        PabcPublicParameters publicParams = null;
        for (int i = 0; i < nServers; i++) {
            CredentialGenerator credentialServerModule = new ThresholdPSSharesGenerator(database, seed, i);
            PABCConfigurationImpl config = new PABCConfigurationImpl();
            config.setAttrDefinitions(generateAttributeDefinitions());
            config.setSeed((seed.toString()+i).getBytes());
            config.setLifetime(lifetime);
            config.setAllowedTimeDifference(allowedTimeDifference);
            config.setServers(Arrays.asList("1", "2"));
            credentialServerModule.setup(config);
            publicParams = credentialServerModule.getPublicParam();
            mapServers.put(i, credentialServerModule);
        }
        MSpublicParam schemePublicParam = new PSpublicParam(publicParams.getEncodedSchemePublicParam());
        //Obtain publicKeyShares and aggregatedKey
        Map<Integer, MSverfKey> verificationKeyShares = new HashMap<>();
        MSverfKey[] verificationKeys = new MSverfKey[nServers];
        int i = 0;
        for (Integer id : mapServers.keySet()) {
            MSverfKey key = (MSverfKey) mapServers.get(id).getVerificationKeyShare();
            verificationKeys[i] = key;
            verificationKeyShares.put(id, key);
            i++;
        }
        PSms auxSignScheme = new PSms();
        auxSignScheme.setup(schemePublicParam.getN(), schemePublicParam.getAuxArg(), seed);
        MSverfKey aggregatedVerificationKey = auxSignScheme.kAggreg(verificationKeys);
        //Setup client module
        CredentialManagement credentialClientModule1 = new PSCredentialManagement(true, new InMemoryCredentialStorage(), 60);
        ((PSCredentialManagement) credentialClientModule1).setup(publicParams, verificationKeyShares, seed);
        Pair<PabcPublicParameters, Map<Integer, MSverfKey>> pp = ((PSCredentialManagement) credentialClientModule1).getPublicParams();
        CredentialManagement credentialClientModule = new PSCredentialManagement(true, new InMemoryCredentialStorage(), 60);
        ((PSCredentialManagement) credentialClientModule).setup(pp.getFirst(), pp.getSecond(), seed);
        //Setup verifier module
        OLVerificationLibrary credentialVerifierLibrary1 = new OLVerificationLibraryPS();
        ((OLVerificationLibraryPS) credentialVerifierLibrary1).setup(publicParams, aggregatedVerificationKey, seed);
        Pair<PabcPublicParameters, MSverfKey> ppV = ((OLVerificationLibraryPS) credentialVerifierLibrary1).getPublicParams();
        OLVerificationLibrary credentialVerifierLibrary = new OLVerificationLibraryPS();
        ((OLVerificationLibraryPS) credentialVerifierLibrary).setup(ppV.getFirst(), ppV.getSecond(), seed);
        W3CPresentationVerifier credentialVerifierModule = new W3CPresentationVerifierOL(credentialVerifierLibrary);
        //*********Credential creation, proof of policy and verification***********
        String signedMessage = "signedMessage";
        //Policy creation
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(new Predicate("uri:Now", Operation.REVEAL, null));
        predicates.add(new Predicate("uri:Age", Operation.REVEAL, null));
        Policy policy = new Policy(predicates, signedMessage);
        //Client receives a credential share from each IdP (simplified) and passes them to the credential manager
        Map<Integer, VerifiableCredential> credentialShares = new HashMap<>();
        long timestamp = System.currentTimeMillis();
        for (Integer id : mapServers.keySet()) {
            credentialShares.put(id, mapServers.get(id).createCredentialShare(username, timestamp));
        }
        VerifiablePresentation zkPT = credentialClientModule.combineAndGeneratePresentationToken(credentialShares, policy);
        //Client uses module to generate presentation token for the policy
        //Verification of the presentation token
        W3CVerificationResult result = credentialVerifierModule.verifyPresentationToken(zkPT.toJSONString(), policy);
        assertThat(result, is(W3CVerificationResult.VALID));
        List<Predicate> predicates2 = new ArrayList<>();
        predicates2.add(new Predicate("uri:Age", Operation.REVEAL, null));
        Policy policy2 = new Policy(predicates2, signedMessage);
        //Client has the credential and uses it without contacting servers
        assertThat(credentialClientModule.checkStoredCredential(), is(true));
        VerifiablePresentation zkPT2 = credentialClientModule.generatePresentationToken(policy2);
        W3CVerificationResult result2 = credentialVerifierModule.verifyPresentationToken(zkPT2.toJSONString(), policy2);
        assertThat(result2, is(W3CVerificationResult.VALID));
    }


    @Test(expected = IllegalStateException.class)
    public void pabcVerifierGetPublicParamException() {
        OLVerificationLibraryPS pabcVerifier = new OLVerificationLibraryPS();
        pabcVerifier.getPublicParams();
    }

    @Test
    public void testFlowOfflineSetup() throws Exception {
        String username = "userJoe";
        PestoDatabase database = new InMemoryPestoDatabase();
        Map<String, Attribute> userAttr = new HashMap<>();
        userAttr.put("name", new Attribute("Joe"));
        userAttr.put("age", new Attribute(21));
        userAttr.put("now", new Attribute(new Date(System.currentTimeMillis())));
        database.addUser(username, null, 1);
        database.addAttributes(username, userAttr);
        //Create and credentialGenerator module for each server.
        Map<Integer, CredentialGenerator> mapServers = new HashMap<>();
        PabcPublicParameters publicParams = null;
        for (int i = 0; i < nServers; i++) {
            CredentialGenerator credentialServerModule = new ThresholdPSSharesGenerator(database, seed, i);
            PABCConfigurationImpl config = new PABCConfigurationImpl();
            config.setAttrDefinitions(generateAttributeDefinitions());
            config.setSeed((seed.toString()+i).getBytes());
            config.setLifetime(lifetime);
            config.setAllowedTimeDifference(allowedTimeDifference);
            config.setServers(Arrays.asList("1", "2"));
            credentialServerModule.setup(config);
            publicParams = credentialServerModule.getPublicParam();
            mapServers.put(i, credentialServerModule);
        }
        MSpublicParam schemePublicParam = new PSpublicParam(publicParams.getEncodedSchemePublicParam());
        //Obtain publicKeyShares and aggregatedKey
        Map<Integer, MSverfKey> verificationKeyShares = new HashMap<>();
        MSverfKey[] verificationKeys = new MSverfKey[nServers];
        int i = 0;
        for (Integer id : mapServers.keySet()) {
            MSverfKey key = (MSverfKey) mapServers.get(id).getVerificationKeyShare();
            verificationKeys[i] = key;
            verificationKeyShares.put(id, key);
            i++;
        }
        PSms auxSignScheme = new PSms();
        auxSignScheme.setup(schemePublicParam.getN(), schemePublicParam.getAuxArg(), seed);
        MSverfKey aggregatedVerificationKey = auxSignScheme.kAggreg(verificationKeys);
        //Setup client module
        CredentialStorage storage = new InMemoryCredentialStorage();
        CredentialManagement credentialClientModule = new PSCredentialManagement(true, storage, 60);
        ((PSCredentialManagement) credentialClientModule).setup(publicParams, verificationKeyShares, seed);
        //Setup verifier module
        OLVerificationLibrary credentialVerifierLibrary = new OLVerificationLibraryPS();
        ((OLVerificationLibraryPS) credentialVerifierLibrary).setup(publicParams, aggregatedVerificationKey, seed);
        W3CPresentationVerifier credentialVerifierModule = new W3CPresentationVerifierOL(credentialVerifierLibrary);
        //*********Credential creation, proof of policy and verification***********
        String signedMessage = "signedMessage";
        //Policy creation
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(new Predicate("uri:Now", Operation.REVEAL, null));
        predicates.add(new Predicate("uri:Age", Operation.REVEAL, null));
        Policy policy = new Policy(predicates, signedMessage);
        //Client receives a credential share from each IdP (simplified) and passes them to the credential manager
        Map<Integer, VerifiableCredential> credentialShares = new HashMap<>();
        long timestamp = System.currentTimeMillis();
        for (Integer id : mapServers.keySet()) {
            credentialShares.put(id, mapServers.get(id).createCredentialShare(username, timestamp));
        }
        VerifiablePresentation zkPT = credentialClientModule.combineAndGeneratePresentationToken(credentialShares, policy);
        //Client uses module to generate presentation token for the policy
        //Verification of the presentation token
        W3CVerificationResult result = credentialVerifierModule.verifyPresentationToken(zkPT.toJSONString(), policy);
        assertThat(result, is(W3CVerificationResult.VALID));
        String signedMessage2 = "signedMessage2";
        List<Predicate> predicates2 = new ArrayList<>();
        predicates2.add(new Predicate("uri:Age", Operation.REVEAL, null));
        Policy policy2 = new Policy(predicates2, signedMessage2);
        //Client has the credential and uses it without contacting servers
        assertThat(credentialClientModule.checkStoredCredential(), is(true));
        VerifiablePresentation zkPT2 = credentialClientModule.generatePresentationToken(policy2);
        W3CVerificationResult result2 = credentialVerifierModule.verifyPresentationToken(zkPT2.toJSONString(), policy2);
        assertThat(result2, is(W3CVerificationResult.VALID));
        //Get public parameters for offline and setup two new modules
        Pair<PabcPublicParameters, MSverfKey> pp = ((PSCredentialManagement) credentialClientModule).getPublicParamsForOffline();
        CredentialManagement credentialClientModule2 = new PSCredentialManagement(true, storage, 60);
        ((PSCredentialManagement) credentialClientModule2).setupForOffline(pp.getFirst(), pp.getSecond(), seed);
        Pair<PabcPublicParameters, MSverfKey> ppV = ((OLVerificationLibraryPS) credentialVerifierLibrary).getPublicParams();
        OLVerificationLibrary credentialVerifierLibrary2 = new OLVerificationLibraryPS();
        ((OLVerificationLibraryPS) credentialVerifierLibrary2).setup(ppV.getFirst(), ppV.getSecond(), seed);
        W3CPresentationVerifier credentialVerifierModule2 = new W3CPresentationVerifierOL(credentialVerifierLibrary2);
        //Presentation
        String signedMessage3 = "signedMessage3";
        List<Predicate> predicates3 = new ArrayList<>();
        predicates3.add(new Predicate("uri:Now", Operation.REVEAL, null));
        Policy policy3 = new Policy(predicates3, signedMessage3);
        //Client has the credential and uses it without contacting servers
        assertThat(credentialClientModule2.checkStoredCredential(), is(true));
        VerifiablePresentation zkPT3 = credentialClientModule2.generatePresentationToken(policy3);
        W3CVerificationResult result3 = credentialVerifierModule2.verifyPresentationToken(zkPT3.toJSONString(), policy3);
        assertThat(result3, is(W3CVerificationResult.VALID));

    }

    private List<LinkedHashMap<String, Object>> getProofRanges(Map<String, RangePredicateToken> rangePredicateTokenMap) {
        List<LinkedHashMap<String, Object>> ranges = new LinkedList<>();
        LinkedHashMap<String, Object> range = new LinkedHashMap<>();
        for(String p: rangePredicateTokenMap.keySet()) {
            range.put("attr", p);
            range.put("commitment", rangePredicateTokenMap.get(p).getEncodedCommitV());
            range.put("lowerBoundProofValue", rangePredicateTokenMap.get(p).getProofLowerBound().getEncoded());
            range.put("upperBoundProofValue", rangePredicateTokenMap.get(p).getProofUpperBound().getEncoded());
            ranges.add(range);
        }
        return ranges;
    }

    private PabcIdP mockIdpForSetup(PabcPublicParameters pp) throws OperationFailedException {
        PabcIdP idp = mock(PabcIdP.class);
        doReturn(pp).when(idp).getPabcPublicParam();
        return idp;
    }
}
