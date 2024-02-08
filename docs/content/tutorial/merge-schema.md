---
date: 2017-06-10T11:25:01-04:00
title: Merge Multiple Schemas Tutorial
---

In this tutorial, we are going to start a server with multiple services. Each service will have
its own schema.json and they need to be merge during server startup so that validation and scope
verification can be done on all request based on the merged schema.

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

As we are going to regenerate a server and several services in light-example-4j,
let's rename these folder so that you can compare them if you want.

```
cd ~/networknt/light-example-4j/hybrid
mv merge-schema merge-schema.bak
```


# Generate Merge Server

In light-codegen light-hybrid-4j framework generator it needs a config.json as input
to generate a server project. This file can be found in model-config/hybrid/merge-schema/server

Here is the content of config.json

```
{
  "rootPackage": "com.networknt.merger",
  "handlerPackage":"com.networknt.merger.handler",
  "modelPackage":"com.networknt.merger.model",
  "artifactId": "merger",
  "groupId": "com.networknt",
  "name": "merger",
  "version": "1.0.0"
}
```

Here is the command line to generate server from light-codegen folder.

```
cd ~/networknt/light-codegen
java -jar codegen-cli/target/codegen-cli.jar -f light-hybrid-4j-server -o ../light-example-4j/hybrid/merge-schema/server -c ../model-config/hybrid/merge-schema/server/config.json
```

Build generic server

```
cd ~/networknt/light-example-4j/hybrid/generic-server
mvn clean install
```

# Generate Two Services

Now let's generate two services. For hybrid service generator, it needs a config.json and also a
schema.json to define the interface/contract for the service.

These files can be found in model-config/hybrid/merge-schema/service1 and service2 folder.


Here is the list of command lines to generate hybrid services

```
cd ~/networknt/light-codegen
java -jar codegen-cli/target/codegen-cli.jar -f light-hybrid-4j-service -o ../light-example-4j/hybrid/merge-schema/service1 -m ../model-config/hybrid/merge-schema/service1/schema.json -c ../model-config/hybrid/merge-schema/service1/config.json
java -jar codegen-cli/target/codegen-cli.jar -f light-hybrid-4j-service -o ../light-example-4j/hybrid/merge-schema/service2 -m ../model-config/hybrid/merge-schema/service2/schema.json -c ../model-config/hybrid/merge-schema/service2/config.json

```

Build services

```
cd ~/networknt/light-example-4j/hybrid/merge-schema/service1
mvn clean install
cd ~/networknt/light-example-4j/hybrid/merge-schema/service2
mvn clean install

```

# Start the server with services

Now let's start the server with services in the classpath.

```
cd ~/networknt/light-example-4j/hybrid/merge-schema/server
java -cp target/merger-1.0.0.jar:../service1/target/merger-1.0.0.jar:../service2/target/merger-1.0.0.jar com.networknt.server.Server
```
Now the server is up and running with two services


# Test

Access the query service

```
curl -X POST \
  http://localhost:8080/api/json \
  -H 'cache-control: no-cache' \
  -H 'content-type: application/json' \
  -H 'postman-token: 58bb63eb-de70-b855-a633-5b043bb52c95' \
  -d '{
  "host": "lightapi.net",
  "service": "service1",
  "action": "query",
  "version": "0.1.0",
  "q1": "Hu",
  "q2": "Steve"
}'

```

You will have the response like this.

```
{"message":"Hello World!"}
```

Access the command service

```
curl -X POST \
  http://localhost:8080/api/json \
  -H 'cache-control: no-cache' \
  -H 'content-type: application/json' \
  -H 'postman-token: 58bb63eb-de70-b855-a633-5b043bb52c95' \
  -d '{
  "host": "lightapi.net",
  "service": "service2",
  "action": "command",
  "version": "0.1.0",
  "c1": "Hu",
  "c2": "Steve"
}'

```

You will have the response like this.

```
{"message":"Hello World!"}
```
