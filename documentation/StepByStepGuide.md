# Use case 3
This document describes the steps of "use case 3" and how to perform them.
The OLYMPUS framework and the demonstrator does have some requirement:

* Java 1.8 (with crypto-extensions)
* Maven
* Docker
* NodeJs


The demonstrator consists of a couple of components interacting:

* a vIdP - docker containers exposing http on ports 9080, 9081 and 9082 and https on ports 9933, 9934 and 9935
* a user frontend - running http on port 8080
* a service provider - running http on port 3000
* optionally a KeyCloak IdP can be locally deployed on port 8082

## Flow
The flow of this use case is as follows:
The user wishes to create on online reservation at the Service Provider (Restaurant Pesto). To do this the user must
choose to authenticate with Olympus (or optionally KeyCloak).
If the user does not have an account at the Olympus vIdP, he/she must registers at Olympus, by entering name, email, and date of birth, as well as a username and password.
The user logs in, and is returned (with a OIDC Identity Token) to the Service Provider.
The Service Provider receives the information that is needed and, depending on the users age, the user is either allowed to make a reservation or not.

After a reservation has been made, the user arrives at the restaurant and uses the Android application to perform an authentication based on privacy-preserving Attribute-Based Credentials (p-ABCs). The user opens the app, and clicks the button for retrieving a new credential (if one was not previously obtained and stored). In the login screen, the user introduces username and password (if her account already has MFA, the user must select the corresponding checkbox and will be prompted to introduce a Time-based One Time Password to complete the login). Optionally, the user can select "addMFA" to start the process for adding two-factor authentication to her account based on Google Authenticator. Once the user has retrieved the credential, she can start a QR scan to get the URL of the service provider (for **testing** we give the option of building the app with alternative code where the URL is introduced by hand). The application will contact the service provider and show the user the requested policy. If she accepts, a p-ABC presentation will be generated and sent to the service provider, which will verify its validity and reply accordingly (the app will show a screen depending on the result).


## Deployment
To build the various components, first checkout the repository at https://bitbucket.alexandra.dk/projects/OL/repos/usecase-3

Next build the components:

from \olympus-identity
> mvn clean
> mvn install

*** NOTE that it is important to run 'mvn clean' as a single command before 'mvn install', as this will install AMCL (a require library)

*** NOTE that 'mvn install' builds a Docker test setup, so Docker must be running while running this command.

*** ALSO DO NOT skip the tests in 'mvn install'. The tests are necessary for constructing tests keys that is compiled into the Docker Images.

from \olympus-service-provider:
> npm install
> npm run build

*** NOTE that apart from building the different components, they may require some extra configuration steps to work. See the corresponding README files for more details.*

After the components have been built, the 3 services must be deployed:

### OLYMPUS vIdP
To deploy the OLYMPUS vIdP, enter the folder /olympus-identity/oidc-demo-idp and start the vIdP with the command:
> docker compose up

### OLYMPUS Front
To deploy the OLYMPUS Front enter the folder /olympus-identity/front and start it with the command:
> mvn spring-boot:run

*** NOTE that the vIdP must be running before starting the frontend *

### Service Provider
To deploy the Service Provider, enter the folder /olympus-service-provider and start it with the command:
>npm run start


## Step by step
After the services have been started, the actual use case can begin:

### Reserving a table
* The Service Provider is located at http://localhost:3000.
* Choose to log in.
* Choose 'Connect with Olympus'
* Choose 'Sign up' (if you do not already have an account at the vIdP)
* Enter name, birthdate, email, username and password.
* Click the 'Sign up'-button. Upon success, you are returned to the login screen
* Enter username and password. Click 'Log in'
* The Service Provider should now inform you of the username you chose
    If you are old enough to book a table, the service provider will tell you that you are successful in making your booking
    If you are not old enough, you are returned to the original log in screen

### Arriving at the restaurant
* Once you have finished the first flow of the use case (i.e. you have an OLYMPUS account), you can start with the second flow
* Open the OLYMPUS Android application and click on the 'New Credential' button
    * If you already have a stored credential from a previous login, you can skip these steps
* A login screen appears. Here, you can optionally decide to add multi-factor authentication to your OLYMPUS account
    * Press the 'Add MFA' button
    * In the new screen, introduce your OLYMPUS username and password
    * Copy the secret and introduce it in another application used for MFA using the GoogleAuthenticator algorithm (e.g., Google Authenticator app available on the PlayStore)
    * Introduce the TOTP code to confirm the new mechanism
* In the login screen, introduce your username and password and password and check the box depending on whether your account has multi-factor authentication activated or not.
    * If you have MFA activated, you will be prompted to input a TOTP code to login
* Once you have a stored credential, you can start a QR scan by clicking the icon
    * The QR must contain a URL (https://IP:PORT) pointing to the machine where the service provider is deployed (e.g. https://10.0.2.2:3001 from the emulator)
    * Alternatively (e.g., for testing in an emulator) you can click the 'Manual URL test' button to manually introduced the URL
* After a successful QR scan, the application will retrieve the policy requested by the service provider and show it to you asking for consent
* Click on ok to proceed. A zero-knowledge presentation revealing only that information will be generated
    * While the presentation is being generated, a loading bar will appears
* The application will automatically send the presentation to the verifier, and show a screen with the result returned
