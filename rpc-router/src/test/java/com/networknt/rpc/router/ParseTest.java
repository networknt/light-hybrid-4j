package com.networknt.rpc.router;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

public class ParseTest {
    @Test
    public void testParseMethod() {
        String method = "lightapi.net/service/createApiVersion/0.1.0";
        String[] parts = method.split("/");
        Assertions.assertEquals(4, parts.length);
        Assertions.assertEquals("lightapi.net", parts[0]);
        Assertions.assertEquals("service", parts[1]);
        Assertions.assertEquals("createApiVersion", parts[2]);
        Assertions.assertEquals("0.1.0", parts[3]);
    }
}
