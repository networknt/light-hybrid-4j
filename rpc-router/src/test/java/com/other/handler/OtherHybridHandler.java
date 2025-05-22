package com.other.handler;

import com.networknt.rpc.HybridHandler;
import com.networknt.rpc.router.ServiceHandler;
import io.undertow.server.HttpServerExchange;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * This test handler exists to test the multiple package handler configuration.
 */
@ServiceHandler(id="lightapi.net/rule/deleteRule/0.1.0")
public class OtherHybridHandler implements HybridHandler {

    @Override
    public ByteBuffer handle(HttpServerExchange exchange, Object input)  {
        System.out.println("OtherHybridHandler is called with " + input);
        String message = "OK";
        ByteBuffer buffer = ByteBuffer.allocateDirect(message.length());
        buffer.put(message.getBytes(StandardCharsets.US_ASCII));
        buffer.flip();
        return buffer;
    }

}
