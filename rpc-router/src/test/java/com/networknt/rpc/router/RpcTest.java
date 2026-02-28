package com.networknt.rpc.router;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

public class RpcTest {
    @Test
    public void testRpcResponseFormat() {
        String resultStr = "{\"host\":\"lightapi.net\"}";
        Object reqId = 123;
        
        String jsonRpcResponse = String.format("{\"jsonrpc\":\"2.0\",\"result\":%s,\"id\":%s}", resultStr, reqId);
        Assertions.assertEquals("{\"jsonrpc\":\"2.0\",\"result\":{\"host\":\"lightapi.net\"},\"id\":123}", jsonRpcResponse);
    }
}
