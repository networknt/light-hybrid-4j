package com.networknt.rpc.router;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.schema.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by steve on 19/02/17.
 */
@ConfigSchema(configKey = "rpc-router", configName = "rpc-router", configDescription = "rpc-router configuration", outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML})
public class RpcRouterConfig {
    public static final String CONFIG_NAME = "rpc-router";
    public static final String HANDLER_PACKAGES = "handlerPackages";
    public static final String JSON_PATH = "jsonPath";
    public static final String FORM_PATH = "formPath";
    public static final String REGISTER_SERVICE = "registerService";

    @ArrayField(
            configFieldName = HANDLER_PACKAGES,
            externalizedKeyName = HANDLER_PACKAGES,
            externalized = true,
            description = "The hybrid handler package names that is used for scanner during server startup. The more specific of package names, the faster to start\n" +
                    "List the package prefixes for all handlers used. Leave an empty array to indicate wildcard (all packages)",
            items = String.class
    )
    private List<String> handlerPackages;

    @StringField(
            configFieldName = JSON_PATH,
            externalizedKeyName = JSON_PATH,
            externalized = true,
            description = "The JSON RPC API path"
    )
    private String jsonPath;

    @StringField(
            configFieldName = FORM_PATH,
            externalizedKeyName = FORM_PATH,
            externalized = true,
            description = "The form RPC API path"
    )
    private String formPath;

    @BooleanField(
            configFieldName = REGISTER_SERVICE,
            externalizedKeyName = REGISTER_SERVICE,
            externalized = true,
            description = "if we want to register all handlers as services to the Consul for client to discover"
    )
    private boolean registerService;

    private Map<String, Object> mappedConfig;
    private final Config config;

    private RpcRouterConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
    }
    private RpcRouterConfig() {
        this(CONFIG_NAME);
    }

    public static RpcRouterConfig load(String configName) {
        return new RpcRouterConfig(configName);
    }

    public static RpcRouterConfig load() {
        return new RpcRouterConfig();
    }

    public void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
    }

    public void reload(String configName) {
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
    }

    public List<String> getHandlerPackages() {
        return handlerPackages;
    }

    public String getJsonPath() {
        return jsonPath;
    }

    public String getFormPath() {
        return formPath;
    }

    public boolean isRegisterService() {
        return registerService;
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    private void setConfigData() {
        if(getMappedConfig() != null) {
            Object object = getMappedConfig().get(HANDLER_PACKAGES);
            if(object != null) {
                if (object instanceof String) {
                    String s = (String) object;
                    s = s.trim();
                    if (s.startsWith("[")) {
                        // this is a JSON string, and we need to parse it.
                        try {
                            handlerPackages = Config.getInstance().getMapper().readValue(s, new TypeReference<List<String>>() {
                            });
                        } catch (Exception e) {
                            throw new ConfigException("could not parse the handlerPackages json with a list of strings.");
                        }
                    } else {
                        // this is a comma separated string.
                        handlerPackages = Arrays.asList(s.split("\\s*,\\s*"));
                    }
                } else if (object instanceof List) {
                    handlerPackages = (List<String>) getMappedConfig().get(HANDLER_PACKAGES);
                } else {
                    throw new ConfigException("handlerPackages list is missing or wrong type.");
                }
            }
            object = getMappedConfig().get(JSON_PATH);
            if(object != null) jsonPath = (String)object;
            object = getMappedConfig().get(FORM_PATH);
            if(object != null) formPath = (String)object;
            object = getMappedConfig().get(REGISTER_SERVICE);
            if(object != null) registerService = Config.loadBooleanValue(REGISTER_SERVICE, object);
        }
    }
}
