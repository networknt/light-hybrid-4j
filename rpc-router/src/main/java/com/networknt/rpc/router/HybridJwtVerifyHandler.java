package com.networknt.rpc.router;

import com.networknt.config.Config;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.security.AbstractJwtVerifyHandler;
import com.networknt.security.JwtVerifier;
import com.networknt.security.SecurityConfig;
import com.networknt.utility.Constants;
import com.networknt.utility.ModuleRegistry;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class HybridJwtVerifyHandler extends AbstractJwtVerifyHandler {
    static final Logger logger = LoggerFactory.getLogger(HybridJwtVerifyHandler.class);
    static final String HYBRID_SECURITY_CONFIG = "hybrid-security";

    static SecurityConfig config;
    // make this static variable public so that it can be accessed from the server-info module
    public static JwtVerifier jwtVerifier;

    public HybridJwtVerifyHandler() {
        // at this moment, we assume that the OpenApiHandler is fully loaded with a single spec or multiple specs.
        // And the basePath is the correct one from the OpenApiHandler helper or helperMap if multiple is used.
        config = SecurityConfig.load(HYBRID_SECURITY_CONFIG);
        jwtVerifier = new JwtVerifier(config);
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
        ModuleRegistry.registerModule(HYBRID_SECURITY_CONFIG, HybridJwtVerifyHandler.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(HYBRID_SECURITY_CONFIG), null);
    }

    @Override
    public void reload() {
        config.reload(HYBRID_SECURITY_CONFIG);
        jwtVerifier = new JwtVerifier(config);
        ModuleRegistry.registerModule(HYBRID_SECURITY_CONFIG, HybridJwtVerifyHandler.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(HYBRID_SECURITY_CONFIG), null);
    }
}
