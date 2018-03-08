package com.networknt.rpc.router;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.audit.AuditHandler;
import com.networknt.colfer.ColferRpc;
import com.networknt.config.Config;
import com.networknt.exception.ExpiredTokenException;
import com.networknt.security.JwtHelper;
import com.networknt.status.Status;
import com.networknt.utility.Constants;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.util.HeaderMap;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import sun.text.resources.FormatData;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

/**
 * Created by steve on 19/02/17.
 */
public abstract class AbstractRpcHandler implements HttpHandler {
    static private final Logger logger = LoggerFactory.getLogger(AbstractRpcHandler.class);

    static final String SCHEMA = "schema.json";
    static final String ENABLE_VERIFY_JWT = "enableVerifyJwt";
    static final String ENABLE_VERIFY_SCOPE = "enableVerifyScope";

    static final String STATUS_INVALID_SCOPE_TOKEN = "ERR10003";
    static final String STATUS_SCOPE_TOKEN_EXPIRED = "ERR10004";
    static final String STATUS_AUTH_TOKEN_SCOPE_MISMATCH = "ERR10005";
    static final String STATUS_SCOPE_TOKEN_SCOPE_MISMATCH = "ERR10006";

    public static Map<String, Object> schema = new HashMap<>();

    static {
        // load all schema.json from resources folder and merge them into one map.
        try {
            final Enumeration<URL> schemaResources = JsonHandler.class.getClassLoader().getResources(SCHEMA);
            while(schemaResources.hasMoreElements()) {
                URL url = schemaResources.nextElement();
                if(logger.isDebugEnabled()) logger.debug("schema file = " + url);
                try (InputStream is = url.openStream()) {
                    schema.putAll(Config.getInstance().getMapper().readValue(is, new TypeReference<Map<String,Object>>(){}));
                }
            }
            if(logger.isDebugEnabled()) logger.debug("schema = " + Config.getInstance().getMapper().writeValueAsString(schema));
        } catch (IOException e) {
            logger.error("Error loading schema.json files from service jars", e);
        }
    }

    public String getServiceId(Map<String, Object> jsonMap) {
        return  (jsonMap.get("host") == null? "" : jsonMap.get("host") + "/") +
                (jsonMap.get("service") == null? "" : jsonMap.get("service") + "/") +
                (jsonMap.get("action") == null? "" : jsonMap.get("action") + "/") +
                (jsonMap.get("version") == null? "" : jsonMap.get("version"));
    }

    public String getServiceId(ColferRpc cf) {
        return  (cf.host == null? "" : cf.host + "/") +
                (cf.service == null? "" : cf.service + "/") +
                (cf.action == null? "" : cf.action + "/") +
                (cf.version == null? "" : cf.version);
    }

    public String getServiceId(FormData formData) {
        return (formData.contains("host") ? formData.get("host").peek().getValue() + "/" : "") +
                (formData.contains("service") ? formData.get("service").peek().getValue() + "/" : "") +
                (formData.contains("action") ? formData.get("action").peek().getValue() + "/" : "") +
                (formData.contains("version") ? formData.get("version").peek().getValue() : "");
    }

    void verifyJwt(Map<String, Object> config, String serviceId, HttpServerExchange exchange) {
        // calling jwt scope verification here. token signature and expiration are done
        if(config != null && (Boolean)config.get(ENABLE_VERIFY_JWT) && (Boolean)config.get(ENABLE_VERIFY_SCOPE)) {
            Map<String, Object> service = (Map<String, Object>)schema.get(serviceId);
            String scope = (String)service.get("scope");
            Status status = verifyScope(exchange, scope);
            if(status != null) {
                exchange.getResponseSender().send(status.toString());
            }
        }
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
        Map<String, Object> auditInfo = exchange.getAttachment(AuditHandler.AUDIT_INFO);
        // auditInfo cannot be null at this point as it is populated by rpc-security and scope verification
        // must not enabled if jwt verification is disabled.
        if (scopeJwt != null) {
            try {
                JwtClaims scopeClaims = JwtHelper.verifyJwt(scopeJwt);
                secondaryScopes = scopeClaims.getStringListClaimValue("scope");
                auditInfo.put(Constants.SCOPE_CLIENT_ID_STRING, scopeClaims.getStringClaimValue(Constants.CLIENT_ID_STRING));
                auditInfo.put(Constants.ACCESS_CLAIMS, scopeClaims);
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
            // no scope token, verify scope from auditInfo which is saved from id token.
            String idScope = (String)auditInfo.get(Constants.SCOPE_STRING);
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

    private boolean matchedScopes(List<String> jwtScopes, List<String> specScopes) {
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
