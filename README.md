# kzen-launcher
UI for selecting projects

Dev mode (two processes for client refresh, and server from IDE):
> > run KzenLauncherApp from IDE
>
> > ./gradlew -t kzen-launcher-js:watch
>
> > cd kzen-launcher-js && yarn run start 

Dist:
> ./gradlew assemble
>
> java -jar kzen-launcher-jvm/build/libs/kzen-launcher-jvm-*.jar

Web:
> http://localhost:8080/