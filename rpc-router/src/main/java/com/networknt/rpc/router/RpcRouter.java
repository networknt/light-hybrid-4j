package com.networknt.rpc.router;

import com.networknt.config.Config;
import com.networknt.server.HandlerProvider;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;

/**
 * Created by steve on 19/02/17.
 */
public class RpcRouter implements HandlerProvider {
    static final String CONFIG_NAME = "rpc-router";

    static RpcRouterConfig config = (RpcRouterConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, RpcRouterConfig.class);
    @Override
    public HttpHandler getHandler() {
        PathHandler paths =  Handlers.path()
                .addPrefixPath("/api/colfer", new ColferHandler())
                .addPrefixPath("/api/json", new JsonHandler())
                .addPrefixPath("/api/form", new FormHandler()
                );
        if (config.getJsonPath()!=null && !"/api/json".equals(config.getJsonPath())) {
            paths.addPrefixPath(config.getJsonPath(), new JsonHandler());
        }
        if (config.getFormPath()!=null && !"/api/form".equals(config.getFormPath())) {
            paths.addPrefixPath(config.getFormPath(), new FormHandler());
        }
        if (config.getColferPath()!=null && !"/api/colfer".equals(config.getColferPath())) {
            paths.addPrefixPath(config.getColferPath(), new ColferHandler());
        }
        return paths;
    }
}
