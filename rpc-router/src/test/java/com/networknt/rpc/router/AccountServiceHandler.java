package com.networknt.rpc.router;

import com.networknt.rpc.Handler;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import java.nio.ByteBuffer;

/**
 * Created by steve on 12/04/17.
 */
@ServiceHandler(id="www.networknt.com/account/credit/1.0.0")
public class AccountServiceHandler implements Handler {
    static private final XLogger logger = XLoggerFactory.getXLogger(AccountServiceHandler.class);

    @Override
    public ByteBuffer handle(HttpServerExchange exchange, Object input) {
        System.out.println("AccountServiceHandler is called");
        return null;
    }
}
