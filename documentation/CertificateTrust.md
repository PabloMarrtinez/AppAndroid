# Generating self-signed certificates
For the demo IdPs, the certificates are automatically generated when running the *mvn install* command (i.e., when *TestConfigurationUtil.generateOIDCDockerConfiguration* is run). You can modify the Subject Alternative Names that will be included in the certificates by modifying the corresponding variable (SAN) in *TestConfigurationUtil.generateOIDCDockerConfiguration*.

For the service provider, you can generate the certificate with the command (you will be prompted to introduce some information like country, organization... That you can fill as you see fit):
> openssl req -nodes -new -x509 -keyout server.key -out server.cert -addext "subjectAltName = DNS:localhost,IP:10.0.2.2"

Note that you can add as many Subject Alternative Names as you see fit using the format shown in the previous command (be sure to put use "DNS:" or "IP:" accordingly).

# Trusting self-signed certificates 
## Android app
The app is already prepared to trust the certificates included in the files: *sample_provider_cert.pem*, *server0_cert.pem*, *server1_cert.pem*, and *server2_cert.pem*. Before running the app, you must ensure that they are up to date with the certificates that are being ised by the vIdP and the service provider. 

You can extract the certificates from each IdP {i} (note that they change if you each time you execute the automatic configuration) with the command:
> keytool -exportcert -keystore truststore.jks -storepass OLYMPUS -alias server-{i}-certificate -rfc -file server{i}_cert.pem

For the service provider cert, you can simple copy the content of *server.cert* in the *olympus-service-provider/keys* directory.

**NOTE: You need to make sure that the certificates include the correct DNS name/IP in the Subject Alternative Names field. E.g. if the endpoint is 10.0.2.2 the certificate must have a corresponding entry on its SAN field, otherwise, an unverified host exception will occur.**

## Service provider
You must copy the file *olympus-identity/oidc-demo-idp/src/test/resources/volatile/truststore.jks* to the *olympus-service-provider/server/ol-lib* directory (replacing the *truststore.jks* file in that location if there was any).

## Front end
You must copy the file *olympus-identity/oidc-demo-idp/src/test/resources/volatile/truststore.jks* to the *olympus-identity/front/src/test/resources* directory (replacing the *truststore.jks* file in that location if there was any).
