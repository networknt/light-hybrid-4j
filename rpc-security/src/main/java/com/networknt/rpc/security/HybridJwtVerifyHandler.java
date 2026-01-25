package com.networknt.rpc.security;

import com.networknt.config.Config;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.security.AbstractJwtVerifyHandler;
import com.networknt.security.JwtVerifier;
import com.networknt.security.SecurityConfig;
import com.networknt.utility.Constants;
import com.networknt.server.ModuleRegistry;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class HybridJwtVerifyHandler extends AbstractJwtVerifyHandler {
    static final Logger logger = LoggerFactory.getLogger(HybridJwtVerifyHandler.class);

    public HybridJwtVerifyHandler() {
        // at this moment, we assume that the OpenApiHandler is fully loaded with a single spec or multiple specs.
        // And the basePath is the correct one from the OpenApiHandler helper or helperMap if multiple is used.
        config = SecurityConfig.load();
        jwtVerifier = new JwtVerifier(config);
    }

    @Override
    public boolean isSkipAuth(HttpServerExchange exchange) {
        // check if the request path is in the skipPathPrefixes list
        String reqPath = exchange.getRequestPath();
        if (config.getSkipPathPrefixes() != null && config.getSkipPathPrefixes().stream().anyMatch(reqPath::startsWith)) {
            if(logger.isTraceEnabled()) logger.trace("Skip auth base on skipPathPrefixes for {}", reqPath);
            return true;
        }
        // check if the service has skipAuth flag set to true in the schema.
        Map<String, Object> auditInfo = exchange.getAttachment(AttachmentConstants.AUDIT_INFO);
        Map<String, Object> serviceMap = (Map<String, Object>)auditInfo.get(Constants.HYBRID_SERVICE_MAP);
        Boolean skipAuth = (Boolean)serviceMap.get("skipAuth");
        if(skipAuth != null) {
            return skipAuth;
        }
        return false;
    }

    @Override
    public List<String> getSpecScopes(HttpServerExchange exchange, Map<String, Object> auditInfo) throws Exception {
        Map<String, Object> serviceMap = (Map<String, Object>)auditInfo.get(Constants.HYBRID_SERVICE_MAP);
        String scope = (String)serviceMap.get("scope");
        if(scope != null) {
            return List.of(scope);
        } else {
            return null;
        }
    }

    @Override
    public boolean isEnabled() {
        return config.isEnableVerifyJwt();
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(SecurityConfig.CONFIG_NAME, HybridJwtVerifyHandler.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(SecurityConfig.CONFIG_NAME), null);
    }
}
