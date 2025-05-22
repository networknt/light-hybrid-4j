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
    public static final String HANDLER_PACKAGE = "handlerPackages";
    public static final String JSON_PATH = "jsonPath";
    public static final String FORM_PATH = "formPath";
    public static final String REGISTER_SERVICE = "registerService";

    @ArrayField(
            configFieldName = HANDLER_PACKAGE,
            externalizedKeyName = HANDLER_PACKAGE,
            externalized = true,
            description = "The hybrid handler package names that is used for scanner during server startup. The more specific of package names, the faster to start\n" +
                    "List the package prefixes for all handlers used. Leave an empty array to indicate wildcard (all packages)",
            items = String.class
    )
    @JsonDeserialize(using = HandlerPackageDeserializer.class)
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

            // Normally you can deserialize into itself by calling '...convertValue(getMappedConfig(), RpcRouterConfig.class)'
            // but this would lead to an infinite loop since Jackson uses the no-args constructor which calls this method.
            Object object = getMappedConfig().get(HANDLER_PACKAGE);
            if(object != null) {
                final var tempDeserializer = new HandlerPackageDeserializer();
                final var tempModule = new SimpleModule();
                tempModule.addDeserializer(List.class, tempDeserializer);
                final var tempMapper = new ObjectMapper();
                tempMapper.registerModule(tempModule);
                this.handlerPackages = tempMapper.convertValue(object, new TypeReference<>() {});
            }
            object = getMappedConfig().get(JSON_PATH);
            if(object != null) jsonPath = (String)object;
            object = getMappedConfig().get(FORM_PATH);
            if(object != null) formPath = (String)object;
            object = getMappedConfig().get(REGISTER_SERVICE);
            if(object != null) registerService = Config.loadBooleanValue(REGISTER_SERVICE, object);
        }
    }

    private static class HandlerPackageDeserializer extends JsonDeserializer<List<String>> {

        @Override
        public List<String> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
            final var mapper = (ObjectMapper) jsonParser.getCodec();
            final JsonNode root = mapper.readTree(jsonParser);
            if (root.isArray()) {
                return mapper.convertValue(root, new TypeReference<>(){});
            } else if (root.isTextual()) {
                final String packages = mapper.convertValue(root, String.class);
                return Arrays.stream(packages.split(",")).collect(Collectors.toList());
            } else {
                return List.of();
            }
        }
    }

}
