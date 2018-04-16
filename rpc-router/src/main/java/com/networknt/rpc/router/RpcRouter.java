package com.networknt.rpc.router;

import com.networknt.server.HandlerProvider;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.resource.DefaultResourceSupplier;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;

/**
 * Created by steve on 19/02/17.
 */
public class RpcRouter implements HandlerProvider {
    @Override
    public HttpHandler getHandler() {
        return Handlers.path()
                .addPrefixPath("/api/colfer", new ColferHandler())
                .addPrefixPath("/api/json", new JsonHandler())
                .addPrefixPath("/api/form", new FormHandler())
                .addPrefixPath("/resources", new RpcResourceHandler());
    }
}
