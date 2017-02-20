package com.networknt.json.router;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by steve on 19/02/17.
 */
public class ApiJsonGetHandler extends AbstractApiJsonHandler {
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        // find the right handler to handle the request.

    }

}
