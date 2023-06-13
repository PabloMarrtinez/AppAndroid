package inf.um.pilotomimurcia;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import eu.olympus.model.PSCredential;
import inf.um.pilotomimurcia.olympus.EncryptedCredentialStorage;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    private final String psCred="CK3o283LLhITCgNub3cSDAoIAAABdLVsULkQAhINCgRuYW1lEgUKA0pvZRIPCgNhZ2USCAoEAAAAFRABGrYCCjwKOgAAAAAAAAAAAAAAAAAAAAAAAAAPG5XiQ1FoMIekt7uTLU3eFhClurkqDVnZw4OfY4QVPGxvn9HR5rYSegp4CjoKhW6GaZfBpb6LZsNRGSyBnZ+HM01AqAQGAu9PZ1+SYz1OZZYi0tl4q1A6rKdxUWhrV6DZlw4fSOw5EjoTp9emUgt9ZYGGzIP1zcPWVOD1aTY3zlRv+3Pu4loR76hfryyuz3N/dqGAzBRdTKgV8vIOQh5jOdJLGnoKeAo6BCH8RwSfXqls70GkuwtSPwi1MZWnYbZtLNWd/puL0vFgFd5HkvyAXa601ZX3x2O9WR/E9IrxMJB73RI6A4Z/FGRIF8mfY1Iy0XdOXTFstCGK7bYtPs8WzKEGWLn4SLJ388kSRlYYxTMmdQN8ddj10wg+LH56NA==";

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        assertEquals("inf.um.pilotomimurcia", appContext.getPackageName());
    }

    @Ignore
    @Test
    public void encryptedCredentialStorage() throws Exception {
        PSCredential credential=new PSCredential(psCred);
        EncryptedCredentialStorage encryptedCredentialStorage=new EncryptedCredentialStorage("credentialTest",new MenuPrincipal());
        encryptedCredentialStorage.storeCredential(credential);
        //NOTE: checkCredential also checks that it is not expired, so it will return false after some time and break the test
        assertTrue(encryptedCredentialStorage.checkCredential());
        assertEquals(encryptedCredentialStorage.getCredential().toString(),psCred);
        encryptedCredentialStorage.deleteCredential();
        assertTrue(!encryptedCredentialStorage.checkCredential());
    }
}
