This file gathers some common issues that can make the use case scenario fail and how to fix them.

# SSL connection issues
In each component (front, application, service-provider) you may find issues related to SSL connections (unverified host, no root trusted CA for certificate...). Check the [CertificateTrust](./CertificateTrust.md) file and make sure you follow all the necessary steps detailed there.

# Docker issues in some systems
In some systems (mainly in Linux distributions) depending on the Docker installation you may run into a couple of issues.

In some cases, the commands 'docker compose <build|up>' are not available. Instead, you need to use 'docker-compose <build|up>'. This can make the installation process fail. To fix the issue, modify the *olympus-identity/oidc-demo-idp/pom.xlm* changing:
```maven
						<configuration>
							<executable>docker</executable>
							<workingDirectory>${project.basedir}</workingDirectory>
							<arguments>
                                <argument>compose</argument>
								<argument>build</argument>
							</arguments>
						</configuration>
```
To:
```maven
						<configuration>
							<executable>docker-compose</executable>
							<workingDirectory>${project.basedir}</workingDirectory>
							<arguments>
								<argument>build</argument>
							</arguments>
						</configuration>
```


In some Docker installations, the host 'host.docker.internal' is not included by default, leading to the exception (will make the Sign Up step fail):
> javax.servlet.ServletException: javax.ws.rs.ProcessingException: java.net.UnknownHostException: host.docker.internal

To fix this issue, modify the *docker-compose.yml* file adding the *extra_hosts* field (to *all* three servers) as in the example:

```
pesto1:
    build: .
    extra_hosts:
      - "host.docker.internal:host-gateway"
    environment:
      - CONFIG_FILE=/app/config/server1.json
    ports:
      - "9933:9933"
      - "9080:9080"
```

