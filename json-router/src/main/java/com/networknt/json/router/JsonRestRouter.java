package com.networknt.json.router;

import com.networknt.server.HandlerProvider;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.util.Methods;

/**
 * Created by steve on 19/02/17.
 */
public class JsonRestRouter implements HandlerProvider {
    @Override
    public HttpHandler getHandler() {
        return Handlers.routing()
                .add(Methods.GET, "/api/json", new ApiJsonGetHandler())
                .add(Methods.POST, "/api/json", new ApiJsonPostHandler());
    }
}
