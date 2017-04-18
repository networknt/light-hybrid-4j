package com.networknt.rpc.router;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.rpc.Handler;
import io.undertow.io.Receiver;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * This is the entry handler for json api, it will parse the JSON request body and
 * construct service id and then calls the service handler to get the final response.
 *
 * Created by steve on 19/02/17.
 */
public class JsonHandler extends AbstractRpcHandler {
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        System.out.println("JsonHandler is called");
        exchange.getRequestReceiver().receiveFullString(new Receiver.FullStringCallback() {
            @Override
            public void handle(HttpServerExchange exchange, String message) {
                System.out.println("message = " + message);

                Map<String, Object> map = null;
                try {
                    map = Config.getInstance().getMapper().readValue(message, new TypeReference<Map<String,Object>>(){});
                } catch (Exception e) {
                    e.printStackTrace();
                }
                String serviceId = getServiceId(map);
                System.out.println("serviceId = " + serviceId);
                Handler handler = RpcStartupHookProvider.serviceMap.get(serviceId);
                Object result = handler.handle(map);
                System.out.println("result = " + result);
                exchange.getResponseSender().send("OK");
            }
        });

        /*
        exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");

        exchange.getRequestReceiver().receiveFullString((exchange1, message) -> {
            // parse the message to hashmap
            System.out.println(message);

            // Need to parse the body in json map

            // find the serviceId and call the service

            // send the response in json from the service

            exchange.getResponseSender().send("OK");

        });
        */
    }
}
