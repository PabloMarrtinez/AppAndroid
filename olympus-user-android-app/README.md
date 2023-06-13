# The Android user application
This Android application takes advantage of the OLYMPUS framework to offer privacy-preserving authentication based on p-ABCs. The application has the following functionalities:

- Users can retrieve a new credential, which will be securely stored, by login into their OLYMPUS account. To navigate to the login screen, click the New Credential button in the main activity.
- From the same login screen, users can optionally start the process to add a MFA method for their OLYMPUS account. The MFA addition screen will guide them through the process.
- With a stored credential, users can interact with service providers by a simple scan of a QR code. As an alternative for testing purposes (e.g, when using an emulator), we added the possibility of manually introducing an URL "mocking" the QR scan process. 
- Users will always be aware of the information that will be shared, as they have to approve the policy requested by the service provider.
- Once approved, a presentation for the requested policy will be generated and sent to the service provider. The user application will be notified of the result, and show a screen to the user accordingly.


# Application configuration
To be able to correctly run and test the Android user application, you need to do a couple changes to the application code detailed in this section.

## OLYMPUS vIdP endpoints
In *UseCasePilotConfiguration* (*olympus* package), you need to change the endpoints (URL:PORT) of the partial identity providers that form the vIdP in your deployment. A *TODO* marks the point where the code neeeds to be touched. If you are using an emulator and the IdPs are deployed in the same machine, you can leave the configuration as is (*10.0.2.2* as IP).

## Trusting self-signed certificates
The app is already prepared to trust the certificates included in the files: *sample_provider_cert.pem*, *server0_cert.pem*, *server1_cert.pem*, and *server2_cert.pem*. Before running the app, you must ensure that they are up to date with the certificates that are being ised by the vIdP and the service provider. 

You can extract the certificates from each IdP {i} (note that they change if you each time you execute the automatic configuration) with the command:
> keytool -exportcert -keystore truststore.jks -storepass OLYMPUS -alias server-{i}certificate -rfc -file server{i}_cert.pem

For the service provider cert, you can simple copy the content of *server.cert* in the *olympus-service-provider/keys* directory.

**NOTE: You need to make sure that the certificates include the correct DNS name/IP in the Subject Alternative Names field. E.g. if the endpoint is 10.0.2.2 the certificate must have a corresponding entry on its SAN field, otherwise, an unverified host exception will occur.**

# Build and install 
An easy way to build and deploy the Android application for testing is relying on Android Studio. If you want to build the APK from the command line, go to directory olympus-user-android-app and execute:
> gradlew assembleDebug

You can later install the APK in your phone or emulator directly, or you can do it from command line using the adb tool (if you have the phone connected and USB debugging activated):
> adb -d install app/build/outputs/apk/debug/app-debug.apk

For more information, you can go to [Android's developer guide]([olympus-identity/README.md](https://developer.android.com/studio/build/building-cmdline)).
