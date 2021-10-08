# kzen-launcher
UI for selecting projects

Dev mode (one process for client refresh, and one server process from IDE):

1) Run KzenAutoApp from IDE: --server.port=8081
    to start https://localhost:8081
    
2) Run from terminal: `./gradlew -t :kzen-launcher-js:run`
    to run client proxy at https://localhost:8080 with live reload
    - Web UI JavaScript will be provided by webpack          
    - Everything expect `*.js` files is served by port 8081


Dist:
> ./gradlew build
>
> java -jar kzen-launcher-jvm/build/libs/kzen-launcher-jvm-*.jar

Web:
> http://localhost:8080/