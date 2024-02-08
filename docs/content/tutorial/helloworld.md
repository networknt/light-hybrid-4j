---
date: 2017-06-10T09:33:02-04:00
title: Hello World Tutorial
---

This is a tutorial to show users how to generate a hybrid server and a hybrid service
and put them together to serve request from curl. It leverage the generic server and
generic service defined in model-config/hybrid. These server and service are used to
test the new version of the light-hybrid-4j and new version of light-codegen.

# Prepare Environment

Bofore start, we need to prepare the environment by clone several projects from networknt
and build them. Let's assume that you are using a workspace called networknt under your
user directory.

```
cd ~/networknt
git clone git@github.com:networknt/light-codegen.git
git clone git@github.com:networknt/model-config.git
git clone git@github.com:networknt/light-example-4j.git
cd light-codegen
mvn clean install
cd ..

```

As we are going to regenerate the generic-server and generic-service in light-example-4j,
let's rename these folder so that you can compare them if you want.

```
cd ~/networknt/light-example-4j/hybrid
mv generic-server generic-server.bak
mv generic-service generic-service.bak
```


# Generate Generic Server

In light-codegen light-hybrid-4j framework generator it needs a config.json as input
to generate a server project. This file can be found in model-config/hybrid/generic-server

Here is the content of config.json

```
{
  "rootPackage": "com.networknt.gserver",
  "handlerPackage":"com.networknt.gserver.handler",
  "modelPackage":"com.networknt.gserver.model",
  "artifactId": "gserver",
  "groupId": "com.networknt",
  "name": "gserver",
  "version": "1.0.0",
  "overwriteHandler": true,
  "overwriteHandlerTest": true
}

```

Here is the command line to generate server from light-codegen folder.

```
cd ~/networknt/light-codegen
java -jar codegen-cli/target/codegen-cli.jar -f light-hybrid-4j-server -o ../light-example-4j/hybrid/generic-server -c ../model-config/hybrid/generic-server/config.json
```

Build generic server

```
cd ~/networknt/light-example-4j/hybrid/generic-server
mvn clean install
```

# Generate Generic Service

Now let's generate a service. For hybrid service generator, it needs a config.json and also a
schema.json to define the interface/contract for the service.

Service config.json can be found in model-config/hybrid/generic-service and its content is

```
{
  "rootPackage": "com.networknt.gservice",
  "handlerPackage":"com.networknt.gservice.handler",
  "modelPackage":"com.networknt.gservice.model",
  "artifactId": "gservice",
  "groupId": "com.networknt",
  "name": "gservice",
  "version": "1.0.0",
  "overwriteHandler": true,
  "overwriteHandlerTest": true
}

```

Here is the command line to generate hybrid service

```
cd ~/networknt/light-codegen
java -jar codegen-cli/target/codegen-cli.jar -f light-hybrid-4j-service -o ../light-example-4j/hybrid/generic-service -m ../model-config/hybrid/generic-service/schema.json -c ../model-config/hybrid/generic-service/config.json
```

Build generic service

```
cd ~/networknt/light-example-4j/hybrid/generic-service
mvn clean install
```

# Start the server with Service

Now let's start the server with service in the classpath.

```
cd ~/networknt/light-example-4j/hybrid/generic-server
java -cp target/gserver-1.0.0.jar:../generic-service/target/gservice-1.0.0.jar com.networknt.server.Server
```
Now the server is up and running with 4 handlers.


# Test

```
curl -X POST \
  http://localhost:8080/api/json \
  -H 'cache-control: no-cache' \
  -H 'content-type: application/json' \
  -H 'postman-token: 58bb63eb-de70-b855-a633-5b043bb52c95' \
  -d '{
  "host": "lightapi.net",
  "service": "world",
  "action": "hello",
  "version": "0.1.1",
  "lastName": "Hu",
  "firstName": "Steve"
}'

```

You will have the reponse like this.

```
{"message":"Hello World!"}
```
