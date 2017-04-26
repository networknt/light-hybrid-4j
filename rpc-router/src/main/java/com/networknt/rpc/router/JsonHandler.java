package com.networknt.rpc.router;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.rpc.Handler;
import io.undertow.io.Receiver;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import java.nio.ByteBuffer;
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
    static private final XLogger logger = XLoggerFactory.getXLogger(JsonHandler.class);

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        System.out.println("JsonHandler is called");
        exchange.getRequestReceiver().receiveFullString(new Receiver.FullStringCallback() {
            @Override
            public void handle(HttpServerExchange exchange, String message) {
                logger.entry(message);

                Map<String, Object> map = null;
                try {
                    map = Config.getInstance().getMapper().readValue(message, new TypeReference<Map<String,Object>>(){});
                } catch (Exception e) {
                    e.printStackTrace();
                }
                String serviceId = getServiceId(map);
                System.out.println("serviceId = " + serviceId);
                Handler handler = RpcStartupHookProvider.serviceMap.get(serviceId);
                if(handler == null) {
                    // TODO handler doesn't exist.
                    // return a status
                }
                ByteBuffer result = handler.handle(map);
                logger.exit(result);
                if(result == null) {
                    // there is nothing returned from the handler.
                    exchange.endExchange();
                } else {
                    exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
                    exchange.getResponseSender().send(result);
                }
            }
        });
    }
}
