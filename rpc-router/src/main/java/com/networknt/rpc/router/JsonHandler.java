package com.networknt.rpc.router;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.rpc.Handler;
import com.networknt.security.JwtHelper;
import com.networknt.status.Status;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.Map;

/**
 * This is the entry handler for json api, it will parse the JSON request body and
 * construct service id and then calls the service handler to get the final response.
 *
 * Created by steve on 19/02/17.
 */
public class JsonHandler extends AbstractRpcHandler {
    static final String SCHEMA = "schema.json";
    static final String HYBRID_SECURITY_CONFIG = "hybrid-security";
    static final String ENABLE_VERIFY_JWT = "enableVerifyJwt";
    static final String ENABLE_VERIFY_SCOPE = "enableVerifyScope";
    static final String DATA = "data";
    static final String CMD = "cmd";

    static final String STATUS_HANDLER_NOT_FOUND = "ERR11200";
    static final String STATUS_REQUEST_BODY_EMPTY = "ERR11201";
    static final String STATUS_METHOD_NOT_ALLOWED = "ERR10008";
    static final String STATUS_REQUEST_CMD_EMPTY = "ERR11202";

    static private final Logger logger = LoggerFactory.getLogger(JsonHandler.class);

    static Map<String, Object> config;
    static {
        // check if hybrid-security.yml exist
        config = Config.getInstance().getJsonMapConfig(HYBRID_SECURITY_CONFIG);
        // fallback to generic security.yml
        if(config == null) config = Config.getInstance().getJsonMapConfig(JwtHelper.SECURITY_CONFIG);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if(Methods.POST.equals(exchange.getRequestMethod())) {
            exchange.getRequestReceiver().receiveFullString((exchange1, message) -> {
                if(message == null || message.trim().length() == 0) {
                    this.handleEmptyPostRequest(exchange1);
                    return;
                }
                if(logger.isDebugEnabled()) logger.debug("Post method with message = " + message);
                processRequest(exchange1, message);
            });
        } else if(Methods.GET.equals(exchange.getRequestMethod())) {
            Map params = exchange.getQueryParameters();
            String cmd = ((Deque<String>)params.get(CMD)).getFirst();
            if(cmd == null || cmd.trim().length() == 0) {
                this.handleMissingGetCommand(exchange);
                return;
            }
            String message = URLDecoder.decode(cmd, "UTF8");
            if(logger.isDebugEnabled()) logger.debug("Get method with message = " + message);
            processRequest(exchange, message);
        } else {
            this.handleUnsupportedMethod(exchange);
        }
    }

    private void processRequest(HttpServerExchange exchange, String message) {
        Map<String, Object> map = null;
        try {
            map = Config.getInstance().getMapper().readValue(message, new TypeReference<Map<String,Object>>(){});
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        String serviceId = getServiceId(map);
        logger.debug("serviceId = " + serviceId);
        Handler handler = RpcStartupHookProvider.serviceMap.get(serviceId);
        if(handler == null) {
            this.handleMissingHandler(exchange, serviceId);
            return;
        }

        // calling jwt scope verification here. token signature and expiration are done
        verifyJwt(config, serviceId, exchange);

        Object data = map.get(DATA);
        // calling schema validator here.
        ByteBuffer error = handler.validate(serviceId, data);
        if(error != null) {
            exchange.setStatusCode(StatusCodes.BAD_REQUEST);
            exchange.getResponseSender().send(error);
            return;
        }
        // if exchange is not ended, then do the processing.
        ByteBuffer result = handler.handle(exchange, data);
        if(logger.isDebugEnabled()) logger.debug(result.toString());
        this.completeExchange(result, exchange);
    }

    private void handleMissingHandler(HttpServerExchange exchange, String serviceId) {
        logger.error("Handler is not found for serviceId " + serviceId);
        Status status = new Status(STATUS_HANDLER_NOT_FOUND, serviceId);
        exchange.setStatusCode(status.getStatusCode());
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        exchange.getResponseSender().send(status.toString());
    }

    private void handleEmptyPostRequest(HttpServerExchange exchange) {
        // payload of request is missing
        logger.error("Post request without body");
        exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
        Status status = new Status(STATUS_REQUEST_BODY_EMPTY);
        exchange.setStatusCode(status.getStatusCode());
        exchange.getResponseSender().send(status.toString());
    }

    private void handleMissingGetCommand(HttpServerExchange exchange) {
        // payload of request is missing
        logger.error("Get param cmd is empty for light-hybrid-4j");
        exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
        Status status = new Status(STATUS_REQUEST_CMD_EMPTY);
        exchange.setStatusCode(status.getStatusCode());
        exchange.getResponseSender().send(status.toString());
    }

    private void handleUnsupportedMethod(HttpServerExchange exchange) {
        // options is handled in middleware handler so if reach here, invalid.
        exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
        Status status = new Status(STATUS_METHOD_NOT_ALLOWED);
        exchange.setStatusCode(status.getStatusCode());
        exchange.getResponseSender().send(status.toString());
    }
}
