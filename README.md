# License
The source code of the OLYMPUS open source modules is licensed under the Apache License, Version 2.0.

# OLYMPUS use case 3 demostrator
Welcome to the OLYMPUS demonstrator repository!

OLYMPUS addresses the challenges associated to the use of privacy-preserving identity management solutions by establishing an interoperable European identity management framework based on novel cryptographic approaches applied to currently deployed identity management technologies. In particular, OLYMPUS employs distributed cryptographic techniques to split up the role of the online IDP over multiple authorities, so that no single authority can impersonate or track its users.

# Structure
This repository is divided in 4 directories:
- **olympus-identity** has the source code for the OLYMPUS core functionality, including the vIdP for demonstration.
- **olympus-service-provider** has the source code that simulates a service provider that accepts OIDC for authentication (from OLYMPUS, and Keycloak), and has the verifying tools for the p-ABC authentication.
- **olympus-user-android-app** has the source code for the Android application prepared for using the OLYMPUS vIdP for privacy-preserving authentications using p-ABCs.
- **documentation** has extra documentation, like the use case description or step by step guide.

More information about them in the corresponding readmes: [olympus-identity](olympus-identity/README.md), [olympus-service-provide](olympus-service-provide/README.md), [olympus-user-android-app](olympus-user-android-app/README.md), and [documentation](documentation/README.md).

# Acknowledgements
The research leading to these results has received funding from the European Union’s Horizon 2020 Research and Innovation Programme, under Grant Agreement No. 786725 ([OLYMPUS](https://olympus-project.eu))
