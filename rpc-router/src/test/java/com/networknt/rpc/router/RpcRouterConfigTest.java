package com.networknt.rpc.router;

import com.networknt.config.Config;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class RpcRouterConfigTest {
    @Test
    public void testRegisterServiceTrue() {
        String configName = "rpc-router-true";
        RpcRouterConfig config = (RpcRouterConfig) Config.getInstance().getJsonObjectConfig(configName, RpcRouterConfig.class);
        Assert.assertTrue(config.isRegisterService());
    }

    @Test
    public void testRegisterServiceFalse() {
        String configName = "rpc-router-false-package";
        RpcRouterConfig config = (RpcRouterConfig) Config.getInstance().getJsonObjectConfig(configName, RpcRouterConfig.class);
        Assert.assertFalse(config.isRegisterService());
    }

    @Test
    public void testRegisterServiceEmpty() {
        String configName = "rpc-router-empty";
        RpcRouterConfig config = (RpcRouterConfig) Config.getInstance().getJsonObjectConfig(configName, RpcRouterConfig.class);
        Assert.assertFalse(config.isRegisterService());
    }

    @Test
    public void testHandlerPackageEmpty() {
        String configName = "rpc-router-empty";
        RpcRouterConfig config = (RpcRouterConfig) Config.getInstance().getJsonObjectConfig(configName, RpcRouterConfig.class);
        List<String> handlers;
        try (ScanResult scanResult = new ClassGraph()
                .whitelistPackages(config.getHandlerPackage())
                .enableAllInfo()
                .scan()) {
            handlers = scanResult
                    .getClassesWithAnnotation(ServiceHandler.class.getName())
                    .getNames();
        }
        Assert.assertTrue(handlers.size() > 0);
    }

    @Test
    public void testHandlerPackageSingle() {
        String configName = "rpc-router-false-package";
        RpcRouterConfig config = (RpcRouterConfig) Config.getInstance().getJsonObjectConfig(configName, RpcRouterConfig.class);
        List<String> handlers;
        try (ScanResult scanResult = new ClassGraph()
                .whitelistPackages(config.getHandlerPackage())
                .enableAllInfo()
                .scan()) {
            handlers = scanResult
                    .getClassesWithAnnotation(ServiceHandler.class.getName())
                    .getNames();
        }
        Assert.assertTrue(handlers.size() > 0);
    }

}
