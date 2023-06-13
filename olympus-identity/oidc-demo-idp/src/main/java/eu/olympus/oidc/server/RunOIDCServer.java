package eu.olympus.oidc.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.olympus.model.Authorization;
import eu.olympus.model.PABCConfigurationImpl;

import eu.olympus.oidc.server.identityprovers.DemoIdentityProver;
import eu.olympus.oidc.server.storage.InMemoryPestoDatabase;
import eu.olympus.server.AttributeIdentityProver;
import eu.olympus.server.CombinedPabcOIDCIdPImpl;
import eu.olympus.server.GoogleAuthenticator;
import eu.olympus.server.SoftwareServerCryptoModule;
import eu.olympus.server.interfaces.*;
import eu.olympus.server.rest.CombinedIdPServlet;
import eu.olympus.server.rest.PestoIdP2IdPRESTConnection;
import eu.olympus.server.rest.PestoIdPServlet;
import eu.olympus.server.rest.RESTIdPServer;
import java.io.File;
import java.security.SecureRandom;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunOIDCServer {

	private static Logger logger = LoggerFactory.getLogger(RunOIDCServer.class);
	
	/**
	 * Main method to start a server. Takes the path to a configuration file as a paramenter. If no paramenters
	 * are given, it looks for the location of the configuration file in the ENV variable CONFIG_FILE
	 */
	public static void main(String[] args) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		PABCConfigurationImpl configuration = null;
		if (args.length == 0) {
			String configFile = System.getenv("CONFIG_FILE");
			configuration = mapper.readValue(new File(configFile), PABCConfigurationImpl.class);
		}
		else {
			configuration = mapper.readValue(new File(args[0]), PABCConfigurationImpl.class);
		}

		logger.info("Starting OIDC server with configuration: "+configuration.toString());

		List<IdPRESTWrapper> others = new ArrayList<>();
		for (String s: configuration.getServers()) {
			others.add(new PestoIdP2IdPRESTConnection(s, configuration.getId(), configuration.getKeyStorePath(), configuration.getKeyStorePassword(),
					configuration.getTrustStorePath(), configuration.getTrustStorePassword(), configuration.getMyAuthorizationCookie()));
		}
		//Setup databases
		//Currently uses a simple in memory database
		PestoDatabase db = new InMemoryPestoDatabase();
		//Setup identity provers
		//Currently uses a Demo Identity Prover that stores a key-value mapping of attributes
		List<IdentityProver> identityProvers = new LinkedList<IdentityProver>();
		identityProvers.add(new AttributeIdentityProver(db));
		identityProvers.add(new DemoIdentityProver(db));

		ServerCryptoModule cryptoModule = new SoftwareServerCryptoModule(new SecureRandom());

		//Setup MFA authenticators
		Map<String, MFAAuthenticator> authenticatorMap=new HashMap<>();
		authenticatorMap.put(GoogleAuthenticator.TYPE,new GoogleAuthenticator(cryptoModule));

		List<String> types = new ArrayList<>(1);
		types.add(CombinedIdPServlet.class.getCanonicalName());

		//Setup the IdP.
		CombinedPabcOIDCIdPImpl idp = null;
		idp = new CombinedPabcOIDCIdPImpl(db, identityProvers, authenticatorMap, cryptoModule, configuration.getIssuerId(), 100000);
		idp.setup("ssid", configuration, others);
		//And also an in memory database for authorization of servers and admins
		for(String cookie: configuration.getAuthorizationCookies().keySet()) {
			Authorization authorization = configuration.getAuthorizationCookies().get(cookie);
			idp.addSession(cookie, authorization);
		}
		
		RESTIdPServer restServer = new RESTIdPServer();
		restServer.setIdP(idp);
		logger.info("Starting REST server on port: "+configuration.getPort());
		try {
			restServer.start(configuration.getPort(), types,
				configuration.getTlsPort(), 
				configuration.getKeyStorePath(), 
				configuration.getKeyStorePassword(), 
				configuration.getKeyStorePassword());
		}catch(Exception e) {
			logger.info("Failed starting REST server: "+e, e);
		}
	}
}
