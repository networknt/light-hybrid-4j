package com.networknt.rpc.security;

import com.networknt.rpc.HybridHandler;
import com.networknt.rpc.router.ServiceHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Created by steve on 20/02/17.
 */
@ServiceHandler(id="lightapi.net/rule/deleteRule/0.1.0")
public class TestServiceHybridHandler implements HybridHandler {
    static private final Logger logger = LoggerFactory.getLogger(TestServiceHybridHandler.class);

    @Override
    public ByteBuffer handle(HttpServerExchange exchange, Object input)  {
        System.out.println("TestServiceHandler is called with " + input);
        String message = "OK";
        ByteBuffer buffer = ByteBuffer.allocateDirect(message.length());
        buffer.put(message.getBytes(StandardCharsets.US_ASCII));
        buffer.flip();
        return buffer;
    }

}
