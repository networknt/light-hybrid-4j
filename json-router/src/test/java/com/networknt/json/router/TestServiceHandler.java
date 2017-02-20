package com.networknt.json.router;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

/**
 * Created by steve on 20/02/17.
 */
@ServiceHandler(id="www.networknt.com#get.account-1.0.0")
public class TestServiceHandler implements HttpHandler {

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        // find the right handler to handle the request.

    }

}
