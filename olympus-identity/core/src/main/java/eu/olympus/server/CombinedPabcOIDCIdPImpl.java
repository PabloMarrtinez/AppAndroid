package eu.olympus.server;

import eu.olympus.model.Policy;
import eu.olympus.model.exceptions.AuthenticationFailedException;
import eu.olympus.model.exceptions.SetupException;
import eu.olympus.server.interfaces.*;
import eu.olympus.server.rest.CommonRESTEndpoints;
import java.util.List;
import java.util.Map;

public class CombinedPabcOIDCIdPImpl extends PabcIdPImpl implements PestoIdP {

  public CombinedPabcOIDCIdPImpl(PestoDatabase database, List<IdentityProver> identityProvers, Map<String, MFAAuthenticator> authenticators, ServerCryptoModule cryptoModule, String issuerId, int rateLimit) throws SetupException {
    super(database, identityProvers, authenticators, cryptoModule, rateLimit);
    tokenGenerator = new ThresholdOIDCTokenGenerator(database, cryptoModule, issuerId);
  }

  @Override
  public String authenticate(String username, byte[] cookie, long salt, byte[] signature, Policy policy) throws AuthenticationFailedException {
    boolean authenticated = authenticationHandler.validateUsernameAndSignature(username, cookie, salt, signature, CommonRESTEndpoints.AUTHENTICATE);
    if(authenticated) {
      try{
        return ((ThresholdOIDCTokenGenerator)tokenGenerator).generateOIDCToken(username, policy, salt);
      } catch(Exception e) {
        throw new AuthenticationFailedException("Failed : Could not produce a token", e);
      }
    }
    throw new AuthenticationFailedException("Failed : User failed authentication");
  }
}
