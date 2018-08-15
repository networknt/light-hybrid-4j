package com.networknt.rpc.router;

import com.networknt.rpc.Handler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * Created by steve on 12/04/17.
 */
@ServiceHandler(id="www.networknt.com/account/credit/1.0.0")
public class AccountServiceHandler implements Handler {
    static private final Logger logger = LoggerFactory.getLogger(AccountServiceHandler.class);

    @Override
    public ByteBuffer handle(HttpServerExchange exchange, Object input) {
        System.out.println("AccountServiceHandler is called");
        return null;
    }
}
