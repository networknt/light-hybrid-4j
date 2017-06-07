---
date: 2016-10-09T08:01:56-04:00
title: Microservices
---

## Introduction

Hybrid service is type of service between J2ee service and microservice. It can help some organizations to take step by step to fully adopt microservices.
User can split original large project to several Hybrid services to run at same JVM. From consumer side,  Hybrid services can be treated as microservices to be use.

Now on the tutorial, we are using light-eventuate-4j's todo-list example to generate and run hybrid service;



## Prepare workspace

All specifications and code of the services are on github.com but we are going to
redo it again by following the steps in the tutorial. Let's first create a
workspace. I have created a directory named networknt under user directory.

Checkout related projects.

```
cd ~/networknt
git clone git@github.com:networknt/light-4j.git
git clone git@github.com:networknt/light-hybrid-4j.git
git clone git@github.com:networknt/light-codegen.git
git clone git@github.com:networknt/light-eventuate-4j.git
git clone git@github.com:networknt/light-eventuate-example.git
git clone git@github.com:networknt/light-codegen.git



Go into the projects root folder above, and build the projects with maven

```
mvn clean install

```



## Use light-codegen to generate hybrid service project

Light-codegen use the schema JSON file as base to generate hybrid service project. We created a sample json config file under networknt/model-config project

you can checkout to you local for reference:

  ->  git clone https://github.com/networknt/model-config.git


under the folder model-config/hybrid, you can see we have two hybrid services for todo-list:
 1 for command side hybrid service:  todocommand-service
 2 for query side hybrid service: todoquery-service

 Inside each folder, there are three json config files:

   serverConfig.json    -- config file for generating hybrid service server.
   config.json          -- config file for generating hybrid service project
   schema.json          -- schema config to define the service handler, action, version, data type, and validation

Now let's generate light-hybrid-4j service server first. The server can be a host to run Multiple service modules:

1.  Generate command side server, run following command on your workspace:

```
java -jar light-codegen/codegen-cli/target/codegen-cli.jar -f light-hybrid-4j-server -o light-eventuate-4j/command -c model-config/hybrid/todocommand-service/serverConfig.json
```

It will generate command module under light-eventuate-4j project. Please refer existing code in GitHub:

https://github.com/networknt/light-eventuate-4j/tree/master/command


2. Generate query side server, run following command on your workspace:

```
java -jar light-codegen/codegen-cli/target/codegen-cli.jar -f light-hybrid-4j-server -o light-eventuate-4j/query -c model-config/hybrid/todoquery-service/serverConfig.json
```

It will generate command module under light-eventuate-4j project. Please refer existing code in GitHub:

https://github.com/networknt/light-eventuate-4j/tree/master/query


3. Generate command side hybrid service under light-eventuate-example/todo-list project

```
java -jar light-codegen/codegen-cli/target/codegen-cli.jar -f light-hybrid-4j-service -o light-eventuate-example/todo-list/command-hybridservice -m model-config/hybrid/todocommand-service/schema.json -c model-config/hybrid/todocommand-service/config.json
```

 Please refer existing code in GitHub:

 https://github.com/networknt/light-eventuate-example/tree/master/todo-list/command-hybridservice

 4. Generate query side hybrid service under light-eventuate-example/todo-list project

 ```
java -jar light-codegen/codegen-cli/target/codegen-cli.jar -f light-hybrid-4j-service -o light-eventuate-example/todo-list/query-hybridservice -m model-config/hybrid/todoquery-service/schema.json -c model-config/hybrid/todoquery-service/config.json
 ```

 Please refer existing code in GitHub:

  https://github.com/networknt/light-eventuate-example/tree/master/todo-list/query-hybridservice


# Ready

Now we generate required server and service for hybrid service. Let's go to next step to develop and setup service