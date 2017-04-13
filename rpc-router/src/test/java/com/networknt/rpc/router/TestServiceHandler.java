package com.networknt.rpc.router;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

/**
 * Created by steve on 20/02/17.
 */
@ServiceHandler(id="www.networknt.com/account/retrieve/1.0.0")
public class TestServiceHandler implements HttpHandler {
    static private final XLogger logger = XLoggerFactory.getXLogger(TestServiceHandler.class);

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        // find the right handler to handle the request.
        logger.entry(exchange);
    }

}
