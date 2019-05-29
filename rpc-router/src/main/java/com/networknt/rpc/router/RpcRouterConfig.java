package com.networknt.rpc.router;

/**
 * Created by steve on 19/02/17.
 */
public class RpcRouterConfig {
    private String description;
    private String handlerPackage;
    private String jsonPath;
    private String formPath;
    private String colferPath;
    private String resourcesBasePath;
    private boolean registerService;

    public RpcRouterConfig() { }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getColferPath() {
        return colferPath;
    }

    public void setColferPath(String colferPath) {
        this.colferPath = colferPath;
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
}