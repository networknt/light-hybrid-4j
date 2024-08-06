package com.networknt.rpc.router;

import com.networknt.rpc.HybridHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

/**
 * Created by steve on 20/02/17.
 */
@ServiceHandler(id="www.networknt.com/account/delete/0.1.1")
public class TestServiceHybridHandler implements HybridHandler {
    static private final Logger logger = LoggerFactory.getLogger(TestServiceHybridHandler.class);

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
