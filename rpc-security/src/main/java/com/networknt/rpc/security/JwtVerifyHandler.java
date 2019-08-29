package com.networknt.rpc.security;

import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.security.IJwtVerifyHandler;
import com.networknt.exception.ExpiredTokenException;
import com.networknt.security.JwtVerifier;
import com.networknt.utility.Constants;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * This is a middleware handler that handles security verification for light-hybrid-4j framework. It
 * verifies token signature and token expiration. Unlike the similar handler in light-rest-4j handles
 * scope verification, this one doesn't as it is placed before rpc-router and exact service is unknown.
 *
 * The scope verification will be done in the rpc-router once the service is identified and schema is
 * found. Both handlers share the same security.yml but just focus on different properties.
 *
 * @author Steve Hu
 */
public class JwtVerifyHandler implements MiddlewareHandler, IJwtVerifyHandler {
    static final Logger logger = LoggerFactory.getLogger(JwtVerifyHandler.class);
    static final String HYBRID_SECURITY_CONFIG = "hybrid-security";

    static final String STATUS_INVALID_AUTH_TOKEN = "ERR10000";
    static final String STATUS_AUTH_TOKEN_EXPIRED = "ERR10001";
    static final String STATUS_MISSING_AUTH_TOKEN = "ERR10002";

    // make it public so that AbstractRpcHandler can access it directly.
    public static Map<String, Object> config;
    public static JwtVerifier jwtVerifier;
    static {
        // check if hybrid-security.yml exist
        config = Config.getInstance().getJsonMapConfig(HYBRID_SECURITY_CONFIG);
        // fallback to generic security.yml
        if(config == null) config = Config.getInstance().getJsonMapConfig(JwtVerifier.SECURITY_CONFIG);
        jwtVerifier = new JwtVerifier(config);
    }

    private volatile HttpHandler next;

    public JwtVerifyHandler() {}

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        HeaderMap headerMap = exchange.getRequestHeaders();
        String authorization = headerMap.getFirst(Headers.AUTHORIZATION);
        String jwt = jwtVerifier.getJwtFromAuthorization(authorization);
        if(jwt != null) {
            try {
                JwtClaims claims = jwtVerifier.verifyJwt(jwt, false, true);
                Map<String, Object> auditInfo = new HashMap<>();
                auditInfo.put(Constants.ENDPOINT_STRING, exchange.getRequestURI());
                auditInfo.put(Constants.CLIENT_ID_STRING, claims.getStringClaimValue(Constants.CLIENT_ID_STRING));
                auditInfo.put(Constants.USER_ID_STRING, claims.getStringClaimValue(Constants.USER_ID_STRING));
                // This is the id-token scope, it is put into the header for audit and rpc-router for token verification
                // Need to remove the space in order for rpc-router to parse and verify scope
                auditInfo.put(Constants.SCOPE_STRING, claims.getStringListClaimValue(Constants.SCOPE_STRING).toString().replaceAll("\\s+",""));
                auditInfo.put(Constants.SUBJECT_CLAIMS, claims);
                exchange.putAttachment(AttachmentConstants.AUDIT_INFO, auditInfo);
                Handler.next(exchange, next);
            } catch (InvalidJwtException e) {
                // only log it and unauthorized is returned.
                logger.error("InvalidJwtException:", e);
                setExchangeStatus(exchange, STATUS_INVALID_AUTH_TOKEN);
            } catch (ExpiredTokenException e) {
                logger.error("ExpiredTokenException", e);
                setExchangeStatus(exchange, STATUS_AUTH_TOKEN_EXPIRED);
            }
        } else {
            setExchangeStatus(exchange, STATUS_MISSING_AUTH_TOKEN);
        }
    }

    @Override
    public HttpHandler getNext() {
        return next;
    }

    @Override
    public MiddlewareHandler setNext(final HttpHandler next) {
        Handlers.handlerNotNull(next);
        this.next = next;
        return this;
    }

    @Override
    public boolean isEnabled() {
        Object object = config.get(JwtVerifier.ENABLE_VERIFY_JWT);
        return object != null && (Boolean) object;
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(JwtVerifyHandler.class.getName(), config, null);
    }

    @Override
    public JwtVerifier getJwtVerifier() {
        return jwtVerifier;
    }

}
