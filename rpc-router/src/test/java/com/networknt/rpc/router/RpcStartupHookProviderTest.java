package com.networknt.rpc.router;

import com.networknt.rpc.HybridHandler;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

/**
 * Created by steve on 20/02/17.
 */
public class RpcStartupHookProviderTest {
    @Test
    public void testStartup() {
        RpcStartupHookProvider provider = new RpcStartupHookProvider();
        provider.onStartup();
        Map<String, HybridHandler> servicesMap = provider.serviceMap;
        Assert.assertTrue(servicesMap.size() > 0);
        System.out.println("serviceMap size = " + servicesMap.size());
    }
}
