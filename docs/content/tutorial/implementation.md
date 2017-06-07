---
date: 2016-10-12T17:05:47-04:00
title: Unit Test
---

## Implementation service


1 change maven pom file for required dependency. For todo-list eventuate example project, following are required dependencies:

       <dependency>
            <groupId>com.networknt</groupId>
            <artifactId>eventuate-common</artifactId>
        </dependency>
        <dependency>
            <groupId>com.networknt</groupId>
            <artifactId>eventuate-client</artifactId>
        </dependency>
        <dependency>
            <groupId>com.networknt</groupId>
            <artifactId>eventuate-todo-common</artifactId>
        </dependency>
        <dependency>
            <groupId>com.networknt</groupId>
            <artifactId>eventuate-todo-command</artifactId>
        </dependency>


2. Implement service handler classes and test classes:

 To consist with Restful microservice, Hybrid service use RPC call with Json format input and output. In the handler class, we pass the input object as below:

 -------------------------------------------------------------------------------------------------------------
         JsonNode inputPara =Config.getInstance().getMapper().valueToTree(input);

         TodoInfo todo = new TodoInfo();
         todo.setTitle(inputPara.findPath("title").asText());
         todo.setCompleted(inputPara.findPath("completed").asBoolean());
         todo.setOrder(inputPara.findPath("order").asInt());
---------------------------------------------------------------------------------------------------------------

Please refer github source code for detail implementation:

 https://github.com/networknt/light-eventuate-example/tree/master/todo-list/command-hybridservice
 https://github.com/networknt/light-eventuate-example/tree/master/todo-list/query-hybridservice



## Setup service

1. Build project:
  cd light-eventuate-example/todo-list
  mvn clean install


2. Go to command server module in light-eventaute-4j project.

 change com.networknt.server.StartupHookProvider by adding following line:

 # registry all service handlers by from annotations
 com.networknt.rpc.router.RpcStartupHookProvider
 com.networknt.eventuate.cdcservice.CdcStartupHookProvider

 RpcStartupHookProvider is used for hybrid service to register and process RPC handlers
 CdcStartupHookProvider: eventuate sourcing run CDC service on command side to convert events in event store

3.  Go to query server module in light-eventaute-4j project.

 change com.networknt.server.StartupHookProvider by adding following line:

 # config event handle registration
 com.networknt.eventuate.client.EventuateClientStartupHookProvider

 EventuateClientStartupHookProvider:  eventatuate event handler startup provide. query side service need run defined event handler to process event.

 4. Prepare resource config in docker folder

 Add and modify required config files into light/eventuate-4j/docker folder for testing

 Please refer source in github:
 https://github.com/networknt/light-eventuate-4j/tree/master/docker


