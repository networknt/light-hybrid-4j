package com.networknt.rpc.router;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.rpc.HybridHandler;
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
import java.nio.charset.StandardCharsets;
import java.util.*;

public class SchemaHandler implements MiddlewareHandler {
    static final Logger logger = LoggerFactory.getLogger(SchemaHandler.class);
    static final String REQUEST = "request";
    static final String SCHEMA = "schema";
    static final String DATA = "data";
    static final String CMD = "cmd";
    static final String STATUS_HANDLER_NOT_FOUND = "ERR11200";
    static final String STATUS_REQUEST_BODY_EMPTY = "ERR11201";
    static final String STATUS_METHOD_NOT_ALLOWED = "ERR10008";
    static final String STATUS_REQUEST_CMD_EMPTY = "ERR11202";

    private static final String SPEC_YAML = "spec.yaml";
    private volatile HttpHandler next;
    public static Map<String, Object> services = new HashMap<>();

    public SchemaHandler() {
        if(logger.isTraceEnabled()) logger.trace("SchemaHandler constructed");
        // load all spec.yaml from resources folder and merge them into one map.
        try {
            final Enumeration<URL> schemaResources = SchemaHandler.class.getClassLoader().getResources(SPEC_YAML);
            while(schemaResources.hasMoreElements()) {
                URL url = schemaResources.nextElement();
                if(logger.isDebugEnabled()) logger.debug("schema file = {}", url);
                try (InputStream is = url.openStream()) {
                    services.putAll(parseYaml(is));
                }
            }
            if(logger.isDebugEnabled()) logger.debug("services = {}", Config.getInstance().getMapper().writeValueAsString(services));
        } catch (IOException e) {
            logger.error("Error loading spec.yaml files from service jars", e);
            // throw exception to stop the service as this is a serious error.
            throw new RuntimeException("Error loading spec.yaml files from service jars");
        }
    }

    public static Map<String, Map<String, Object>> parseYaml(InputStream yamlStream) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Map<String, Object> rootNode = mapper.readValue(yamlStream, Map.class);
        Map<String, Map<String, Object>> resultMap = new LinkedHashMap<>();

        String host = (String) rootNode.get("host");
        String service = (String) rootNode.get("service");

        // Extract schemas
        Map<String, Map<String, Object>> schemas = new HashMap<>();
        Map<String, Object> schemasNode = (Map<String, Object>) rootNode.get("schemas");
        if (schemasNode != null) {
            for(Map.Entry<String, Object> entry: schemasNode.entrySet()){
                schemas.put(entry.getKey(), (Map<String, Object>) entry.getValue());
            }
        }


        // Process actions
        List<Map<String, Object>> actionsNode = (List<Map<String, Object>>) rootNode.get("action");

        if (actionsNode != null) {
            for (Map<String, Object> actionNode : actionsNode) {
                String actionName = (String) actionNode.get("name");
                String actionVersion = (String) actionNode.get("version");
                String key = host + "/" + service + "/" + actionName + "/" + actionVersion;

                Boolean skipAuth = (Boolean) actionNode.get("skipAuth") != null ? (Boolean) actionNode.get("skipAuth") : false;
                String scope = (String) actionNode.get("scope") ;

                Map<String, Object> actionMap = new LinkedHashMap<>();
                actionMap.put("skipAuth", skipAuth);
                if(scope != null) {
                    actionMap.put("scope", scope);
                }

                // resolve request schema
                Map<String, Object> requestNode = (Map<String, Object>) actionNode.get("request");
                if(requestNode != null) {
                    Map<String, Object> requestMap = new LinkedHashMap<>();
                    Map<String, Object> schemaRef = (Map<String, Object>) requestNode.get("schema");
                    if (schemaRef != null && schemaRef.containsKey("$ref")) {
                        String ref = (String) schemaRef.get("$ref");
                        String schemaName = ref.substring(ref.lastIndexOf('/') + 1);
                        Map<String, Object> resolvedSchema = schemas.get(schemaName);
                        if(resolvedSchema != null) {
                            requestMap.put("schema", resolvedSchema);
                        }
                    }
                    actionMap.put("request", requestMap);
                }

                // resolve response schema
                Map<String, Object> responseNode = (Map<String, Object>) actionNode.get("response");
                if(responseNode != null) {
                    Map<String, Object> responseMap = new LinkedHashMap<>();
                    Map<String, Object> schemaRef = (Map<String, Object>) responseNode.get("schema");
                    if (schemaRef != null && schemaRef.containsKey("$ref")) {
                        String ref = (String) schemaRef.get("$ref");
                        String schemaName = ref.substring(ref.lastIndexOf('/') + 1);
                        Map<String, Object> resolvedSchema = schemas.get(schemaName);
                        if(resolvedSchema != null) {
                            responseMap.put("schema", resolvedSchema);
                        }
                    }
                    actionMap.put("response", responseMap);
                }

                resultMap.put(key, actionMap);
            }
        }
        return resultMap;
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
                if(logger.isDebugEnabled()) logger.debug("Post method with message = {}", message);
                processRequest(exchange1, message);
            });
        } else if(Methods.GET.equals(exchange.getRequestMethod())) {
            Map<String, Deque<String>> params = exchange.getQueryParameters();
            String cmd = ((Deque<String>)params.get(CMD)).getFirst();
            if(cmd == null || cmd.trim().isEmpty()) {
                this.handleMissingGetCommand(exchange);
                return;
            }
            String message = URLDecoder.decode(cmd, StandardCharsets.UTF_8);
            if(logger.isDebugEnabled()) logger.debug("Get method with message = {}", message);
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
        HybridHandler handler = RpcStartupHookProvider.serviceMap.get(serviceId);
        if(handler == null) {
            this.handleMissingHandler(exchange, serviceId);
            return;
        }
        Map<String, Object> data = (Map<String, Object>)map.get(DATA);
        Map<String, Object> serviceMap = (Map<String, Object>)services.get(serviceId);
        Map<String, Object> requestMap = (Map<String, Object>)serviceMap.get(REQUEST);
        ByteBuffer error = handler.validate(serviceId, (Map<String, Object>)requestMap.get(SCHEMA), data);
        if(error != null) {
            exchange.setStatusCode(StatusCodes.BAD_REQUEST);
            exchange.getResponseSender().send(error);
            return;
        }

        if(logger.isDebugEnabled()) {
            logger.debug("serviceId = {} serviceMap = {}", serviceId, JsonMapper.toJson(serviceMap));
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

        // if exchange is not ended, then call the next handler in the chain.
        if(logger.isTraceEnabled()) logger.trace("SchemaHandler.handleRequest ends.");
        try {
            Handler.next(exchange, next);
        } catch (Exception e) {
            logger.error("Exception:", e);
            exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
            exchange.endExchange();
        }
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
        logger.error("Handler is not found for serviceId {}", serviceId);
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
