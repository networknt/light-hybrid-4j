package com.networknt.json.router;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by steve on 19/02/17.
 */
public class ApiJsonGetHandlerTest {
    @Test
    public void testServiceId() {
        Map<String, Object> map = new HashMap<>();
        map.put("host", "www.networknt.com");
        map.put("service", "get.account");
        map.put("version", "1.0.3");

        ApiJsonGetHandler handler = new ApiJsonGetHandler();
        String serviceId = handler.getServiceId(map);
        Assert.assertEquals("www.networknt.com#get.account-1.0.3", serviceId);
    }
}
