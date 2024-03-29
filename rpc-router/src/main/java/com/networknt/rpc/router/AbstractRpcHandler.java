package com.networknt.rpc.router;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.handler.LightHttpHandler;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.httpstring.HttpStringConstants;
import com.networknt.rpc.Handler;
import com.networknt.security.JwtVerifier;
import com.networknt.exception.ExpiredTokenException;
import com.networknt.security.SecurityConfig;
import com.networknt.status.Status;
import com.networknt.utility.Constants;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.*;

import static com.networknt.rpc.router.JsonHandler.STATUS_HANDLER_NOT_FOUND;

/**
 * Created by steve on 19/02/17.
 */
public abstract class AbstractRpcHandler implements LightHttpHandler {
    static private final Logger logger = LoggerFactory.getLogger(AbstractRpcHandler.class);

    private static final String SCHEMA = "schema.json";
    private static final String HYBRID_SECURITY_CONFIG = "hybrid-security";
    private static final String SKIP_AUTH = "skipAuth"; // skip the JwtVerification for some endpoints.

    private static final String STATUS_INVALID_SCOPE_TOKEN = "ERR10003";
    private static final String STATUS_SCOPE_TOKEN_EXPIRED = "ERR10004";
    private static final String STATUS_AUTH_TOKEN_SCOPE_MISMATCH = "ERR10005";
    private static final String STATUS_SCOPE_TOKEN_SCOPE_MISMATCH = "ERR10006";
    private static final String STATUS_INVALID_AUTH_TOKEN = "ERR10000";
    private static final String STATUS_AUTH_TOKEN_EXPIRED = "ERR10001";
    private static final String STATUS_MISSING_AUTH_TOKEN = "ERR10002";

    private static SecurityConfig config;
    private static JwtVerifier jwtVerifier;
    static {
        // check if hybrid-security.yml exist
        config = SecurityConfig.load(HYBRID_SECURITY_CONFIG);
        jwtVerifier = new JwtVerifier(config);
    }

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

    public String getServiceId(Map<String, Object> jsonMap) {
        return  (jsonMap.get("host") == null? "" : jsonMap.get("host") + "/") +
                (jsonMap.get("service") == null? "" : jsonMap.get("service") + "/") +
                (jsonMap.get("action") == null? "" : jsonMap.get("action") + "/") +
                (jsonMap.get("version") == null? "" : jsonMap.get("version"));
    }

    /*
    public String getServiceId(ColferRpc cf) {
        return  (cf.host == null? "" : cf.host + "/") +
                (cf.service == null? "" : cf.service + "/") +
                (cf.action == null? "" : cf.action + "/") +
                (cf.version == null? "" : cf.version);
    }
    */

    String getServiceId(FormData formData) {
        return (formData.contains("host") ? formData.get("host").peek().getValue() + "/" : "") +
                (formData.contains("service") ? formData.get("service").peek().getValue() + "/" : "") +
                (formData.contains("action") ? formData.get("action").peek().getValue() + "/" : "") +
                (formData.contains("version") ? formData.get("version").peek().getValue() : "");
    }

    // if there are any error, return a status object to indicate the exact error. Otherwise, return null
    Status verifyJwt(String serviceId, HttpServerExchange exchange) {
        Map<String, Object> service = (Map<String, Object>)schema.get(serviceId);
        if(logger.isTraceEnabled()) logger.trace("serviceId = " + serviceId + " service = " + JsonMapper.toJson(service));
        if(isVerifyJwt(service.get(SKIP_AUTH))) {
            HeaderMap headerMap = exchange.getRequestHeaders();
            String reqPath = exchange.getRequestPath();
            String authorization = headerMap.getFirst(Headers.AUTHORIZATION);
            // output only the first 10 chars to see if the authorization header empty or miss the Bearer.
            if(logger.isTraceEnabled()) logger.trace("Need to verify JWT. authorization = " + (authorization == null ? null : authorization.substring(0, 10)));
            String jwt = jwtVerifier.getTokenFromAuthorization(authorization);
            if(jwt != null) {
                try {
                    JwtClaims claims = jwtVerifier.verifyJwt(jwt, false, true, null, reqPath, null);
                    // Unlike light-rest-4j, the auditInfo shouldn't be in the exchange for the hybrid framework.
                    Map<String, Object> auditInfo = new HashMap<>();
                    exchange.putAttachment(AttachmentConstants.AUDIT_INFO, auditInfo);
                    auditInfo.put(Constants.ENDPOINT_STRING, serviceId); // use serviceId as endpoint.
                    String clientId = claims.getStringClaimValue(Constants.CLIENT_ID_STRING);
                    // try to get the cid as some OAuth tokens name it as cid like Okta.
                    if(clientId == null) clientId = claims.getStringClaimValue(Constants.CID_STRING);
                    auditInfo.put(Constants.CLIENT_ID_STRING, clientId);
                    String userId = claims.getStringClaimValue(Constants.USER_ID_STRING);
                    // try to get the uid as some OAuth tokens name it as uid like Okta.
                    if(userId == null) userId = claims.getStringClaimValue(Constants.UID_STRING);
                    auditInfo.put(Constants.USER_ID_STRING, userId);
                    auditInfo.put(Constants.ROLES_STRING, claims.getStringClaimValue(Constants.ROLES_STRING));
                    auditInfo.put(Constants.SUBJECT_CLAIMS, claims);
                    String callerId = headerMap.getFirst(HttpStringConstants.CALLER_ID);
                    if(callerId != null) auditInfo.put(Constants.CALLER_ID_STRING, callerId);
                    if(config.isEnableVerifyScope()) {
                        // is there a scope token
                        String scopeHeader = headerMap.getFirst(HttpStringConstants.SCOPE_TOKEN);
                        String scopeJwt = jwtVerifier.getTokenFromAuthorization(scopeHeader);
                        List<String> secondaryScopes = null;
                        if(scopeJwt != null) {
                            try {
                                JwtClaims scopeClaims = jwtVerifier.verifyJwt(scopeJwt, false, true, null, reqPath, null);
                                Object scopeClaim = scopeClaims.getClaimValue(Constants.SCOPE_STRING);
                                if(scopeClaim instanceof String) {
                                    secondaryScopes = Arrays.asList(scopeClaims.getStringClaimValue(Constants.SCOPE_STRING).split(" "));
                                } else if(scopeClaim instanceof List) {
                                    secondaryScopes = scopeClaims.getStringListClaimValue(Constants.SCOPE_STRING);
                                }
                                if(secondaryScopes == null || secondaryScopes.isEmpty()) {
                                    // some IDPs like Okta and Microsoft call scope claim "scp" instead of "scope"
                                    Object scpClaim = scopeClaims.getClaimValue(Constants.SCP_STRING);
                                    if(scpClaim instanceof String) {
                                        secondaryScopes = Arrays.asList(scopeClaims.getStringClaimValue(Constants.SCP_STRING).split(" "));
                                    } else if(scpClaim instanceof List) {
                                        secondaryScopes = scopeClaims.getStringListClaimValue(Constants.SCP_STRING);
                                    }
                                }
                                auditInfo.put(Constants.SCOPE_CLIENT_ID_STRING, scopeClaims.getStringClaimValue(Constants.CLIENT_ID_STRING));
                                auditInfo.put(Constants.ACCESS_CLAIMS, scopeClaims);
                            } catch (InvalidJwtException | MalformedClaimException e) {
                                logger.error("InvalidJwtException", e);
                                return new Status(STATUS_INVALID_SCOPE_TOKEN);
                            } catch (ExpiredTokenException e) {
                                logger.error("ExpiredTokenException", e);
                                return new Status(STATUS_SCOPE_TOKEN_EXPIRED);
                            }
                        }

                        String schemaScope = (String)service.get(Constants.SCOPE_STRING);
                        // convert schemaScope to a list of String. scope in schema is space delimited.
                        List<String> specScopes = schemaScope == null? null : Arrays.asList(schemaScope.split("\\s+"));

                        // validate scope
                        if (scopeHeader != null) {
                            if (secondaryScopes == null || !matchedScopes(secondaryScopes, specScopes)) {
                                return new Status(STATUS_SCOPE_TOKEN_SCOPE_MISMATCH, secondaryScopes, specScopes);
                            }
                        } else {
                            // no scope token, verify scope from auth token.
                            List<String> primaryScopes = null;
                            try {
                                Object scopeClaim = claims.getClaimValue(Constants.SCOPE_STRING);
                                if(scopeClaim instanceof String) {
                                    primaryScopes = Arrays.asList(claims.getStringClaimValue(Constants.SCOPE_STRING).split(" "));
                                } else if(scopeClaim instanceof List) {
                                    primaryScopes = claims.getStringListClaimValue(Constants.SCOPE_STRING);
                                }
                                if(primaryScopes == null || primaryScopes.isEmpty()) {
                                    // some IDPs like Okta and Microsoft call scope claim "scp" instead of "scope"
                                    Object scpClaim = claims.getClaimValue(Constants.SCP_STRING);
                                    if(scpClaim instanceof String) {
                                        primaryScopes = Arrays.asList(claims.getStringClaimValue(Constants.SCP_STRING).split(" "));
                                    } else if(scpClaim instanceof List) {
                                        primaryScopes = claims.getStringListClaimValue(Constants.SCP_STRING);
                                    }
                                }
                            } catch (MalformedClaimException e) {
                                logger.error("MalformedClaimException", e);
                                return new Status(STATUS_INVALID_AUTH_TOKEN);
                            }
                            if (!matchedScopes(primaryScopes, specScopes)) {
                                return new Status(STATUS_AUTH_TOKEN_SCOPE_MISMATCH, primaryScopes, specScopes);
                            }
                        }
                    }
                } catch (InvalidJwtException | MalformedClaimException e) {
                    // only log it and unauthorized is returned.
                    logger.error("InvalidJwtException:", e);
                    return new Status(STATUS_INVALID_AUTH_TOKEN);
                } catch (ExpiredTokenException e) {
                    logger.error("ExpiredTokenException", e);
                    return new Status(STATUS_AUTH_TOKEN_EXPIRED);
                }
            } else {
                return new Status(STATUS_MISSING_AUTH_TOKEN);
            }
        }
        return null;
    }

    public boolean isVerifyJwt(Object skipAuth) {
        if(skipAuth != null && Boolean.valueOf(skipAuth.toString())) return false;
        return config.isEnableVerifyJwt();
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

    /**
     * Helper method to get handler from config and act accordingly in error scenarios.
     * @param serviceId The service id of the handler to receive.
     * @param httpServerExchange The exchange object with the client.
     * @return A handler if it is found. null otherwise.
     */
    Handler getHandlerOrPopulateExchange(String serviceId, HttpServerExchange httpServerExchange) {
        Handler handler = RpcStartupHookProvider.serviceMap.get(serviceId);
        if (handler == null) {
            setExchangeStatus(httpServerExchange, STATUS_HANDLER_NOT_FOUND, serviceId);
            return null;
        }
        return handler;
    }

    /**
     * Helper method to send requests containing FormData to the handler and process return status' appropriately.
     * @param handler The handler who will be running business logic.
     * @param formData The data that will be passed into the handler.
     * @param httpServerExchange The exchange object with the client.
     */
    void handleFormDataRequest(Handler handler, FormData formData, HttpServerExchange httpServerExchange) {
        ByteBuffer result = handler.handle(httpServerExchange, formData);
        this.completeExchange(result, httpServerExchange);
    }

}
