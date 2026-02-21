package com.networknt.rpc.router;

import com.networknt.utility.Util;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by steve on 12/04/17.
 */
public class JsonHybridHandlerTest {
    @Test
    public void testJsonServiceId() {
        Map<String, Object> map = new HashMap<>();
        map.put("host", "www.networknt.com");
        map.put("service", "account");
        map.put("action", "retrieve");
        map.put("version", "1.0.3");

        SchemaHandler handler = new SchemaHandler();
        String serviceId = Util.getServiceId(map);
        Assertions.assertEquals("www.networknt.com/account/retrieve/1.0.3", serviceId);
    }
}
