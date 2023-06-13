package inf.um.pilotomimurcia.eidas.utils.pki;

import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.jce.X509Principal;

import java.security.cert.X509Certificate;

/**
 * Clase auxiliar.
 */
public class Tool {
    public static final String EXAMPLE_TEXT = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. " +
            "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. " +
            "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. " +
            "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";
    public static String getCN(X509Certificate certificate) {
        X509Principal name = new X509Principal(certificate.getSubjectDN().toString());
        return name.getValues(BCStyle.CN).get(0).toString();
    }
    public static String getNIF(X509Certificate certificate) {
        X509Principal name = new X509Principal(certificate.getSubjectDN().toString());
        return name.getValues(BCStyle.SN).get(0).toString();
    }
}
