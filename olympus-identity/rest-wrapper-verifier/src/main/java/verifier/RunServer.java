package verifier;

import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import verifier.rest.VerifierServer;

import javax.net.ssl.HostnameVerifier;
import java.util.Properties;

public class RunServer {


    public static void main(String[] args) throws Exception{
        if(args.length!=3) {
            System.out.println("Need port, trustStore path, and truststore password parameters");
            return;
        }
        int port;
        try{
            port = Integer.parseInt(args[0]);
            System.out.println("Running client-server on port: "+port);
        } catch(Exception e) {
            System.out.println("Failed to parse port");
            return;
        }
        Properties systemProps = System.getProperties();
        systemProps.put("javax.net.ssl.trustStore", args[1]);
        systemProps.put("javax.net.ssl.trustStorePassword", args[2]);
        // Ensure that there is a certificate in the trust store for the webserver connecting
        HostnameVerifier verifier = new DefaultHostnameVerifier();
        javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(verifier);
        VerifierServer server=VerifierServer.getInstance();
        server.start(port);
    }
}
