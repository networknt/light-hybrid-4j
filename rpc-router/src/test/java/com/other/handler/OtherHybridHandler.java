package com.other.handler;

import com.networknt.rpc.HybridHandler;
import com.networknt.rpc.router.ServiceHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

/**
 * Created by steve on 20/02/17.
 */
@ServiceHandler(id="lightapi.net/rule/deleteRule/0.1.0")
public class OtherHybridHandler implements HybridHandler {
    static private final Logger logger = LoggerFactory.getLogger(OtherHybridHandler.class);

    @Override
    public ByteBuffer handle(HttpServerExchange exchange, Object input)  {
        System.out.println("TestServiceHandler is called with " + input);
        String message = "OK";
        ByteBuffer buffer = ByteBuffer.allocateDirect(message.length());
        try {
            buffer.put(message.getBytes("US-ASCII"));
        } catch (UnsupportedEncodingException e) {
            logger.error("Exception:" + e.getMessage(), e);
            throw new RuntimeException(e);
        }
        buffer.flip();
        return buffer;
    }

}
