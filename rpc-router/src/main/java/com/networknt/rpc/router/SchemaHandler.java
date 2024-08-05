package com.networknt.rpc.router;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.rpc.Handler;
import com.networknt.status.Status;
import com.networknt.utility.Constants;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class SchemaHandler implements MiddlewareHandler {
    static final Logger logger = LoggerFactory.getLogger(SchemaHandler.class);
    static final String SCHEMA = "schema";
    static final String DATA = "data";
    static final String CMD = "cmd";
    static final String STATUS_HANDLER_NOT_FOUND = "ERR11200";
    static final String STATUS_REQUEST_BODY_EMPTY = "ERR11201";
    static final String STATUS_METHOD_NOT_ALLOWED = "ERR10008";
    static final String STATUS_REQUEST_CMD_EMPTY = "ERR11202";

    private static final String SCHEMA_JSON = "schema.json";
    private volatile HttpHandler next;
    public static Map<String, Object> schema = new HashMap<>();

    public SchemaHandler() {
        if(logger.isTraceEnabled()) logger.trace("SchemaHandler constructed");
        // load all schema.json from resources folder and merge them into one map.
        try {
            final Enumeration<URL> schemaResources = SchemaHandler.class.getClassLoader().getResources(SCHEMA_JSON);
            while(schemaResources.hasMoreElements()) {
                URL url = schemaResources.nextElement();
                if(logger.isDebugEnabled()) logger.debug("schema file = " + url);
                try (InputStream is = url.openStream()) {
                    schema.putAll(Config.getInstance().getMapper().readValue(is, new TypeReference<Map<String,Object>>(){}));
                }
            }
            if(logger.isDebugEnabled()) logger.debug("schema = {}", Config.getInstance().getMapper().writeValueAsString(schema));
        } catch (IOException e) {
            logger.error("Error loading schema.json files from service jars", e);
            // throw exception to stop the service as this is a serious error.
            throw new RuntimeException("Error loading schema.json files from service jars");
        }
    }

    @Override
    public HttpHandler getNext() {
        return this.next;
    }

    @Override
    public MiddlewareHandler setNext(HttpHandler next) {
        Handlers.handlerNotNull(next);
        this.next = next;
        return this;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(null, SchemaHandler.class.getName(), null, null);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (logger.isTraceEnabled())
            logger.trace("SchemaHandler.handleRequest starts.");
        if(Methods.POST.equals(exchange.getRequestMethod())) {
            exchange.getRequestReceiver().receiveFullString((exchange1, message) -> {
                if(message == null || message.trim().isEmpty()) {
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

    public String getServiceId(Map<String, Object> jsonMap) {
        return  (jsonMap.get("host") == null? "" : jsonMap.get("host") + "/") +
                (jsonMap.get("service") == null? "" : jsonMap.get("service") + "/") +
                (jsonMap.get("action") == null? "" : jsonMap.get("action") + "/") +
                (jsonMap.get("version") == null? "" : jsonMap.get("version"));
    }

    private void processRequest(HttpServerExchange exchange, String message) {
        Map<String, Object> map = JsonMapper.string2Map(message);
        String serviceId = getServiceId(map);
        logger.debug("serviceId = {}", serviceId);
        Handler handler = RpcStartupHookProvider.serviceMap.get(serviceId);
        if(handler == null) {
            this.handleMissingHandler(exchange, serviceId);
            return;
        }
        Map<String, Object> data = (Map<String, Object>)map.get(DATA);
        Map<String, Object> serviceMap = (Map<String, Object>)schema.get(serviceId);
        ByteBuffer error = handler.validate(serviceId, (Map<String, Object>)serviceMap.get(SCHEMA), data);
        if(error != null) {
            exchange.setStatusCode(StatusCodes.BAD_REQUEST);
            exchange.getResponseSender().send(error);
            return;
        }
        if(logger.isDebugEnabled()) {
            try {
                logger.debug("serviceId = " + serviceId  + " serviceMap = " + Config.getInstance().getMapper().writeValueAsString(serviceMap));
            } catch (Exception e) {
                logger.error("Exception:", e);
            }

        }
        // put the serviceId and data as well as the schema into the exchange for other handlers to use.
        Map<String, Object> auditInfo = exchange.getAttachment(AttachmentConstants.AUDIT_INFO) == null
                ? new HashMap<>()
                : exchange.getAttachment(AttachmentConstants.AUDIT_INFO);
        auditInfo.put(Constants.ENDPOINT_STRING, serviceId);
        auditInfo.put(Constants.HYBRID_SERVICE_ID, serviceId);
        auditInfo.put(Constants.HYBRID_SERVICE_MAP, serviceMap);
        auditInfo.put(Constants.HYBRID_SERVICE_DATA, data);
        exchange.putAttachment(AttachmentConstants.AUDIT_INFO, auditInfo);

        // if exchange is not ended, then do the processing.
        ByteBuffer result = handler.handle(exchange, data);
        if(logger.isDebugEnabled()) logger.debug(result.toString());
        this.completeExchange(result, exchange);
    }

    void completeExchange(ByteBuffer result, HttpServerExchange exchange) {
        if(result == null) {
            // there is nothing returned from the handler.
            exchange.setStatusCode(StatusCodes.OK);
            exchange.endExchange();
        } else {
            // we are expecting the handler set the statusCode if there is an error.
            // if there is no status code, default 200 will be used.
            exchange.getResponseSender().send(result);
        }
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
        String method = exchange.getRequestMethod().toString();
        String path = exchange.getRequestPath();
        Status status = new Status(STATUS_METHOD_NOT_ALLOWED, method, path);
        exchange.setStatusCode(status.getStatusCode());
        exchange.getResponseSender().send(status.toString());
    }

}
