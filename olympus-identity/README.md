
# Structure
The project is divided in three sub-projects or modules: 
- [core](core/README.md): Core functionality of OLYMPUS framework
- [front](front/README.md): Front-end for OIDC flow
- [oidc-demo-idp](oidc-demo-idp/README.md): Demonstrator for deploying a vIdP that supports OIDC and p-ABC for authentication
 
More information about the OLYMPUS architecture, functionalities and APIs can be found in the [documentation](https://olympus-idp.readthedocs.io/en/latest/).

To build the whole project, use the commands:
>mvn clean
> 
>mvn install

*Note that the 'mvn clean' command is needed to install the MIRACL jar dependency into the local m2 repository in order to build the project.*

*Note that 'mvn install' builds a Docker test setup, so Docker must be running while running this command.*

*DO NOT skip the tests in 'mvn install'. The tests are necessary for constructing tests keys that is compiled into the Docker Images.*
