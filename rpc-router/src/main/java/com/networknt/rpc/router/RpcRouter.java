package com.networknt.rpc.router;

import com.networknt.resource.ResourceHelpers;
import com.networknt.handler.HandlerProvider;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.builder.PredicatedHandler;

import java.util.List;

/**
 * Created by steve on 19/02/17.
 */
public class RpcRouter implements HandlerProvider {
    @Override
    public HttpHandler getHandler() {
        PathHandler httpHandler = Handlers.path();

        RpcRouterConfig config = RpcStartupHookProvider.config;

        // Add all prefix or exact resources handlers that clients provide.
        ResourceHelpers.addProvidersToPathHandler(RpcStartupHookProvider.pathResourceProviders, httpHandler);
        String jsonPath = config.getJsonPath() == null ? "/api/json" : config.getJsonPath();
        System.out.println("jsonPath = " + jsonPath);
        String formPath = config.getFormPath() == null ? "/api/form" : config.getFormPath();
        System.out.println("formPath = " + formPath);
        httpHandler.addPrefixPath(jsonPath, new JsonHandler());
        httpHandler.addPrefixPath(formPath, new FormHandler());

        // And if the client provides any predicated handlers, wrap the whole path handler in them.
        List<PredicatedHandler> predicatedHandlers = ResourceHelpers.getPredicatedHandlers(RpcStartupHookProvider.predicatedHandlersProviders);
        if (predicatedHandlers.size() > 0) {
            return Handlers.predicates(predicatedHandlers, httpHandler);
        }

        return httpHandler;
    }
}
