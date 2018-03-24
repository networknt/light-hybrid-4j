package com.networknt.rpc.router;

import com.networknt.rpc.Handler;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Created by steve on 20/02/17.
 */
@ServiceHandler(id="www.networknt.com/account/delete/0.1.1")
public class TestServiceHandler implements Handler {
    static private final XLogger logger = XLoggerFactory.getXLogger(TestServiceHandler.class);

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
