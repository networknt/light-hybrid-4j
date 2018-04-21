package com.networknt.rpc.router;

import com.networknt.resources.ResourceHelpers;
import com.networknt.server.HandlerProvider;
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

        // Add all prefix or exact resources handlers that clients provide.
        ResourceHelpers.addProvidersToPathHandler(RpcStartupHookProvider.pathResourceProviders, httpHandler);

        httpHandler.addPrefixPath("/api/colfer", new ColferHandler())
                .addPrefixPath("/api/json", new JsonHandler())
                .addPrefixPath("/api/form", new FormHandler());

        // And if the client provides any predicated handlers, wrap the whole path handler in them.
        List<PredicatedHandler> predicatedHandlers = ResourceHelpers.getPredicatedHandlers(RpcStartupHookProvider.predicatedHandlersProviders);
        if (predicatedHandlers.size() > 0) {
            return Handlers.predicates(predicatedHandlers, httpHandler);
        }

        return httpHandler;
    }
}
