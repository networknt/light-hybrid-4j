package com.networknt.rpc.router;

import com.networknt.server.HandlerProvider;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;

/**
 * Created by steve on 19/02/17.
 */
public class RpcRouter implements HandlerProvider {
    @Override
    public HttpHandler getHandler() {
        return Handlers.path()
                .addPrefixPath("/api/colfer", new ColferHandler())
                .addPrefixPath("/api/json", new JsonHandler());
    }
}
