# kzen-launcher
UI for selecting projects


To auto-reload frontend:
1) Run `tech.kzen.launcher.server.dev.FrontendDevelopment` from IDE
2) Run `./gradlew -t :kzen-launcher-js:build -x test -PjsWatch` from CLI

To build self-contained jar and executable it from CLI:
1) Run `./gradlew jar`
2) Run `java -jar kzen-launcher-jvm/build/libs/kzen-launcher-jvm-*.jar`


Web:
> http://localhost:8080/


Notes:
- Archetype downloads validate TLS certificates with the JVM's default trust store. In
  environments with TLS-intercepting proxies (corporate MITM), supply your own trust store via
  `-Djavax.net.ssl.trustStore=<path>`.