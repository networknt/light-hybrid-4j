package com.networknt.json.router;

import io.undertow.server.HttpHandler;
import org.junit.Test;

import java.util.Map;

/**
 * Created by steve on 20/02/17.
 */
public class JsonStartupHookProviderTest {
    @Test
    public void testStartup() {
        JsonStartupHookProvider provider = new JsonStartupHookProvider();
        provider.onStartup();
        Map<String, HttpHandler> servicesMap = provider.serviceMap;
        System.out.println("serviceMap = " + servicesMap);
    }
}
