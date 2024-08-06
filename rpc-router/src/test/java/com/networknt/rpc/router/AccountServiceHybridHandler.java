package com.networknt.rpc.router;

import com.networknt.rpc.HybridHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * Created by steve on 12/04/17.
 */
@ServiceHandler(id="www.networknt.com/account/credit/1.0.0")
public class AccountServiceHybridHandler implements HybridHandler {
    static private final Logger logger = LoggerFactory.getLogger(AccountServiceHybridHandler.class);

    @Override
    public ByteBuffer handle(HttpServerExchange exchange, Object input) {
        System.out.println("AccountServiceHandler is called");
        return null;
    }
}
