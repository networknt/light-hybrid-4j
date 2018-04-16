package com.networknt.rpc.router;

/**
 * Created by steve on 19/02/17.
 */
public class RpcRouterConfig {
    private String description;
    private String handlerPackage;

    private String resourcesBasePath;

    public RpcRouterConfig() {

    }
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

    public String getResourcesBasePath() {
        return resourcesBasePath;
    }

    public void setResourcesBasePath(String resourcesBasePath) {
        this.resourcesBasePath = resourcesBasePath;
    }
}
