package com.networknt.json.router;

/**
 * Created by steve on 19/02/17.
 */
public class JsonRouterConfig {
    String description;
    String handlerPackage;
    public JsonRouterConfig() {

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
}
