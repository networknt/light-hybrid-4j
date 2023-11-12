package com.networknt.rpc.router;

import com.networknt.config.Config;
import com.networknt.traceability.TraceabilityConfig;

import java.util.Map;

/**
 * Created by steve on 19/02/17.
 */
public class RpcRouterConfig {
    public static final String CONFIG_NAME = "rpc-router";
    public static final String HANDLER_PACKAGE = "handlerPackage";
    public static final String JSON_PATH = "jsonPath";
    public static final String FORM_PATH = "formPath";
    public static final String RESOURCES_BASE_PATH = "resourcesBasePath";
    public static final String REGISTER_SERVICE = "registerService";

    private String handlerPackage;
    private String jsonPath;
    private String formPath;
    private String resourcesBasePath;
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

    public String getHandlerPackage() {
        return handlerPackage;
    }

    public void setHandlerPackage(String handlerPackage) {
        this.handlerPackage = handlerPackage;
    }

    public String getJsonPath() {
        return jsonPath;
    }

    public void setJsonPath(String jsonPath) {
        this.jsonPath = jsonPath;
    }

    public String getFormPath() {
        return formPath;
    }

    public void setFormPath(String formPath) {
        this.formPath = formPath;
    }

    public String getResourcesBasePath() {
        return resourcesBasePath;
    }

    public void setResourcesBasePath(String resourcesBasePath) {
        this.resourcesBasePath = resourcesBasePath;
    }

    public boolean isRegisterService() {
        return registerService;
    }

    public void setRegisterService(boolean registerService) {
        this.registerService = registerService;
    }
    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    private void setConfigData() {
        if(getMappedConfig() != null) {
            Object object = getMappedConfig().get(HANDLER_PACKAGE);
            if(object != null) handlerPackage = (String)object;
            object = getMappedConfig().get(JSON_PATH);
            if(object != null) jsonPath = (String)object;
            object = getMappedConfig().get(FORM_PATH);
            if(object != null) formPath = (String)object;
            object = getMappedConfig().get(RESOURCES_BASE_PATH);
            if(object != null) resourcesBasePath = (String)object;
            object = getMappedConfig().get(REGISTER_SERVICE);
            if(object != null) registerService = Config.loadBooleanValue(REGISTER_SERVICE, object);
        }
    }

}