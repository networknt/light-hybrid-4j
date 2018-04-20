package com.networknt.rpc.router;

/**
 * Created by steve on 19/02/17.
 */
public class RpcRouterConfig {
    String description;
    String handlerPackage;
    String jsonPath;
    String formPath;
    String colferPath;

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
}