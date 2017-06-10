package com.networknt.rpc.router;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.exception.ExpiredTokenException;
import com.networknt.rpc.Handler;
import com.networknt.security.JwtHelper;
import com.networknt.status.Status;
import com.networknt.utility.Constants;
import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.undertow.io.Receiver;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.CompletableFuture;

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

    static final String STATUS_HANDLER_NOT_FOUND = "ERR11200";
    static final String STATUS_REQUEST_BODY_EMPTY = "ERR11201";
    static final String STATUS_INVALID_SCOPE_TOKEN = "ERR10003";
    static final String STATUS_SCOPE_TOKEN_EXPIRED = "ERR10004";
    static final String STATUS_AUTH_TOKEN_SCOPE_MISMATCH = "ERR10005";
    static final String STATUS_SCOPE_TOKEN_SCOPE_MISMATCH = "ERR10006";

    static private final XLogger logger = XLoggerFactory.getXLogger(JsonHandler.class);

    static final Map<String, Object> config = Config.getInstance().getJsonMapConfig(JwtHelper.SECURITY_CONFIG);
    public static Map<String, Object> schema = new HashMap<>();

    static {
        // load all schema.json from resources folder and merge them into one map.
        try {
            final Enumeration<URL> schemaResources = JsonHandler.class.getClassLoader().getResources(SCHEMA);
            while(schemaResources.hasMoreElements()) {
                try (InputStream is = schemaResources.nextElement().openStream()) {
                   schema.putAll(Config.getInstance().getMapper().readValue(is, new TypeReference<Map<String,Object>>(){}));
                }
            }
        } catch (IOException e) {
            logger.error("Error loading schema.json files from service jars", e);
        }
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        System.out.println("JsonHandler is called");
        exchange.getRequestReceiver().receiveFullString(new Receiver.FullStringCallback() {
            @Override
            public void handle(HttpServerExchange exchange, String message) {
                logger.entry(message);
                exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
                if(message == null || message.trim().length() == 0) {
                    // payload of request is missing
                    Status status = new Status(STATUS_REQUEST_BODY_EMPTY);
                    exchange.getResponseSender().send(status.toString());
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
                    exchange.getResponseSender().send(status.toString());
                    return;
                }
                // calling jwt scope verification here. token signature and expiration are done
                if(config != null && (Boolean)config.get(ENABLE_VERIFY_JWT) && (Boolean)config.get(ENABLE_VERIFY_SCOPE)) {
                    Map<String, Object> service = (Map<String, Object>)schema.get(serviceId);
                    String scope = (String)service.get("scope");
                    Status status = verifyScope(exchange, scope);
                    if(status != null) {
                        exchange.getResponseSender().send(status.toString());
                        return;
                    }
                }
                // calling schema validator here.
                ByteBuffer error = handler.validate(serviceId, map);
                if(error != null) {
                    exchange.getResponseSender().send(error);
                    return;
                }
                // if exchange is not ended, then do the processing.
                ByteBuffer result = handler.handle(map);
                logger.exit(result);
                if(result == null) {
                    // there is nothing returned from the handler.
                    exchange.endExchange();
                } else {
                    exchange.getResponseSender().send(result);
                }
            }
        });
    }

    /**
     * This is to verify if the scope of token from id token or access token matches to the schema
     * token passed in.
     *
     * @param exchange HttpServerExchange that contains information about the request/response.
     * @param schemaScope The scope defined in the schema for the particular service
     * @return Status object if there is an error or null if passed.
     */
    private Status verifyScope(HttpServerExchange exchange, String schemaScope) {
        // check if id token scope exist or not.
        HeaderMap headerMap = exchange.getRequestHeaders();
        String scopeHeader = headerMap.getFirst(Constants.SCOPE_TOKEN);
        String scopeJwt = JwtHelper.getJwtFromAuthorization(scopeHeader);
        List<String> secondaryScopes = null;
        if (scopeJwt != null) {
            try {
                JwtClaims scopeClaims = JwtHelper.verifyJwt(scopeJwt);
                secondaryScopes = scopeClaims.getStringListClaimValue("scope");
                headerMap.add(new HttpString(Constants.SCOPE_CLIENT_ID), scopeClaims.getStringClaimValue(Constants.CLIENT_ID));
            } catch (InvalidJwtException | MalformedClaimException e) {
                logger.error("InvalidJwtException", e);
                return new Status(STATUS_INVALID_SCOPE_TOKEN);
            } catch (ExpiredTokenException e) {
                return new Status(STATUS_SCOPE_TOKEN_EXPIRED);
            }
        }

        // convert schemaScope to a list of String. scope in schema is space delimited.
        List<String> specScopes = schemaScope == null? null : Arrays.asList(schemaScope.split("\\s+"));

        // validate scope
        if (scopeHeader != null) {
            if (secondaryScopes == null || !matchedScopes(secondaryScopes, specScopes)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Scopes " + secondaryScopes + " and specification token " + specScopes + " are not matched in scope token");
                }
                return new Status(STATUS_SCOPE_TOKEN_SCOPE_MISMATCH, secondaryScopes, specScopes);
            }
        } else {
            // no scope token, verify scope from header which is saved from id token.
            String idScope = headerMap.getFirst(Constants.SCOPE);
            List<String> primaryScopes = idScope == null? null : Arrays.asList(idScope.substring(1, idScope.length() - 1).split(","));
            if (!matchedScopes(primaryScopes, specScopes)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Authorization jwt token scope " + primaryScopes + " is not matched with " + specScopes);
                }
                return new Status(STATUS_AUTH_TOKEN_SCOPE_MISMATCH, primaryScopes, specScopes);
            }
        }
        return null;
    }

    protected boolean matchedScopes(List<String> jwtScopes, List<String> specScopes) {
        boolean matched = false;
        if(specScopes != null && specScopes.size() > 0) {
            if(jwtScopes != null && jwtScopes.size() > 0) {
                for(String scope: specScopes) {
                    if(jwtScopes.contains(scope)) {
                        matched = true;
                        break;
                    }
                }
            }
        } else {
            matched = true;
        }
        return matched;
    }

}
