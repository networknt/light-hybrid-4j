package com.networknt.rpc.router;

import com.networknt.rpc.HybridHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * Created by steve on 12/04/17.
 */
@ServiceHandler(id="lightapi.net/rule/deleteRule/0.1.0")
public class DeleteRuleHybridHandler implements HybridHandler {
    static private final Logger logger = LoggerFactory.getLogger(DeleteRuleHybridHandler.class);

    @Override
    public ByteBuffer handle(HttpServerExchange exchange, Object input) {
        System.out.println("DeleteRuleHybridHandler is called");
        return null;
    }
}
