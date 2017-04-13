package com.networknt.rpc.router;

import io.undertow.io.Receiver;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Created by steve on 19/02/17.
 */
public class JsonHandler extends AbstractRpcHandler {
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {

        exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");

        exchange.getRequestReceiver().receiveFullString((exchange1, message) -> {
            // parse the message to hashmap
            System.out.println(message);

            // Need to parse the body in json map

            // find the serviceId and call the service

            // send the response in json from the service

            exchange.getResponseSender().send("OK");

        });
    }
}
