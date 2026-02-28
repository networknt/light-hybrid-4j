package com.networknt.rpc.router;

import com.networknt.rpc.HybridHandler;
import com.networknt.status.Status;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.StatusCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

@ServiceHandler(id="lightapi.net/rule/updateRule/0.1.0")
public class TestErrorServiceHybridHandler implements HybridHandler {
    static private final Logger logger = LoggerFactory.getLogger(TestErrorServiceHybridHandler.class);

    @Override
    public ByteBuffer handle(HttpServerExchange exchange, Object input)  {
        System.out.println("TestErrorServiceHybridHandler is called with " + input);

        exchange.setStatusCode(StatusCodes.BAD_REQUEST);
        Status status = new Status("ERR10001", "This is a test error");

        String message = com.networknt.config.JsonMapper.toJson(status);

        ByteBuffer buffer = ByteBuffer.allocateDirect(message.length());
        try {
            buffer.put(message.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            logger.error("Exception:" + e.getMessage(), e);
            throw new RuntimeException(e);
        }
        buffer.flip();
        return buffer;
    }
}
