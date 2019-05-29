package com.networknt.rpc.router;

import com.networknt.config.Config;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.lukehutch.fastclasspathscanner.scanner.ClassInfo;
import org.junit.Assert;
import org.junit.Test;

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
        Map<String, ClassInfo> classNameToClassInfo =
                new FastClasspathScanner(config.getHandlerPackage()).scan().getClassNameToClassInfo();
        Assert.assertTrue(classNameToClassInfo.size() > 0);
    }

    @Test
    public void testHandlerPackageSingle() {
        String configName = "rpc-router-false-package";
        RpcRouterConfig config = (RpcRouterConfig) Config.getInstance().getJsonObjectConfig(configName, RpcRouterConfig.class);
        Map<String, ClassInfo> classNameToClassInfo =
                new FastClasspathScanner(config.getHandlerPackage()).scan().getClassNameToClassInfo();
        Assert.assertTrue(classNameToClassInfo.size() > 0);
    }

}
