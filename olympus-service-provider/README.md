# Olympus Service Provider
This project contains a service provider for the Olympus project.
The usecase is a restaurant, where one can book a table. To do so, one needs to authenticate using the Olympus Front, and vIDP.
This project contains the website of the restaurant. Furthermore it contains a verifier, in which one can verify a pABC token.

## Build
To build the project run 
    > npm run build

## Run
To run the project run
    > npm start

This will start the restaurant website on localhost:3000, running a supporting pABC verifier on localhost:3030 (can be accessed through the webserver endpoints on localhost:3000 or https://localhost:3001).

## Configuration
Note that the supporting pABC verifier connects to the deployed IdPs through https. Thus, you need to copy the file *olympus-identity/oidc-demo-idp/src/test/resources/volatile/truststore.jks* to the *olympus-service-provider/server/ol-lib* directory (replacing the *truststore.jks* file in that location if there was any). 

If the pABC verifier is correctly launched and setup, the message 'Verifier setup at port 3030' will appear (will take 5~7 seconds).
