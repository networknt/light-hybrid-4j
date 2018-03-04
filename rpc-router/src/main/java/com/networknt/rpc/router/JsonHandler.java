package com.networknt.rpc.router;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.rpc.Handler;
import com.networknt.security.JwtHelper;
import com.networknt.status.Status;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * This is the entry handler for json api, it will parse the JSON request body and
 * construct service id and then calls the service handler to get the final response.
 *
 * Created by steve on 19/02/17.
 */
public class JsonHandler extends AbstractRpcHandler {
    static final String SCHEMA = "schema.json";
    static final String ENABLE_VERIFY_JWT = "enableVerifyJwt";
    static final String ENABLE_VERIFY_SCOPE = "enableVerifyScope";
    static final String DATA = "data";

    static final String STATUS_HANDLER_NOT_FOUND = "ERR11200";
    static final String STATUS_REQUEST_BODY_EMPTY = "ERR11201";

    static private final Logger logger = LoggerFactory.getLogger(JsonHandler.class);

    static final Map<String, Object> config = Config.getInstance().getJsonMapConfig(JwtHelper.SECURITY_CONFIG);


    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        System.out.println("JsonHandler is called");
        exchange.getRequestReceiver().receiveFullString((exchange1, message) -> {
            exchange1.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
            if(message == null || message.trim().length() == 0) {
                // payload of request is missing
                Status status = new Status(STATUS_REQUEST_BODY_EMPTY);
                exchange1.getResponseSender().send(status.toString());
                return;
            }

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
                Status status = new Status(STATUS_HANDLER_NOT_FOUND, serviceId);
                exchange1.getResponseSender().send(status.toString());
                return;
            }

            // calling jwt scope verification here. token signature and expiration are done
            verifyJwt(config, serviceId, exchange1);

            Object data = map.get(DATA);
            // calling schema validator here.
            ByteBuffer error = handler.validate(serviceId, data);
            if(error != null) {
                exchange1.getResponseSender().send(error);
                return;
            }
            // if exchange is not ended, then do the processing.
            ByteBuffer result = handler.handle(data);
            if(logger.isDebugEnabled()) logger.debug(result.toString());
            if(result == null) {
                // there is nothing returned from the handler.
                exchange1.endExchange();
            } else {
                exchange1.getResponseSender().send(result);
            }
        });
    }



}
