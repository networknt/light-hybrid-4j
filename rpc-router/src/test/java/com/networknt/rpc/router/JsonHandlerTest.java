package com.networknt.rpc.router;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by steve on 12/04/17.
 */
public class JsonHandlerTest {
    @Test
    public void testJsonServiceId() {
        Map<String, Object> map = new HashMap<>();
        map.put("host", "www.networknt.com");
        map.put("service", "account");
        map.put("action", "retrieve");
        map.put("version", "1.0.3");

        JsonHandler handler = new JsonHandler();
        String serviceId = handler.getServiceId(map);
        Assert.assertEquals("www.networknt.com/account/retrieve/1.0.3", serviceId);
    }
}
