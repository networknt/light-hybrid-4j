package com.networknt.rpc.router;

import com.networknt.config.Config;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class RpcRouterConfigTest {
    @Test
    public void testHandlerPackage() {
        RpcRouterConfig config = RpcRouterConfig.load();
        Assertions.assertTrue(config.getHandlerPackages().size() == 2);
    }

    @Test
    public void testRegisterServiceTrue() {
        String configName = "rpc-router-true";
        RpcRouterConfig config = RpcRouterConfig.load(configName);
        Assertions.assertTrue(config.isRegisterService());
    }

    @Test
    public void testRegisterServiceFalse() {
        String configName = "rpc-router-false-package";
        RpcRouterConfig config = RpcRouterConfig.load(configName);
        Assertions.assertFalse(config.isRegisterService());
    }

    @Test
    public void testRegisterServiceEmpty() {
        String configName = "rpc-router-empty";
        RpcRouterConfig config = RpcRouterConfig.load(configName);
        Assertions.assertFalse(config.isRegisterService());
    }

    @Test
    public void testHandlerPackageEmpty() {
        String configName = "rpc-router-empty";
        RpcRouterConfig config = RpcRouterConfig.load(configName);
        List<String> handlers;
        final var handler_packages = config.getHandlerPackages().toArray(new String[0]);
        try (ScanResult scanResult = new ClassGraph()
                .acceptPackages(handler_packages)
                .enableAllInfo()
                .scan()) {
            handlers = scanResult
                    .getClassesWithAnnotation(ServiceHandler.class.getName())
                    .getNames();
        }
        Assertions.assertFalse(handlers.isEmpty());
    }

    @Test
    public void testHandlerPackageSingle() {
        String configName = "rpc-router-false-package";
        RpcRouterConfig config = RpcRouterConfig.load(configName);
        List<String> handlers;
        final var handler_packages = config.getHandlerPackages().toArray(new String[0]);
        try (ScanResult scanResult = new ClassGraph()
                .acceptPackages(handler_packages)
                .enableAllInfo()
                .scan()) {
            handlers = scanResult
                    .getClassesWithAnnotation(ServiceHandler.class.getName())
                    .getNames();
        }
        Assertions.assertFalse(handlers.isEmpty());
    }

    @Test
    public void testMultiplePackageHandlers() {
        String configName = "rpc-router-multi-package";
        RpcRouterConfig config = RpcRouterConfig.load(configName);
        List<String> handlers;
        final var handler_packages = config.getHandlerPackages().toArray(new String[0]);
        try (ScanResult scanResult = new ClassGraph()
                .acceptPackages(handler_packages)
                .enableAllInfo()
                .scan()) {
            handlers = scanResult
                    .getClassesWithAnnotation(ServiceHandler.class.getName())
                    .getNames();
        }
        // 5 handlers are expected.
        // - DeleteRuleHybridHandler (com.networknt package)
        // - TestServiceHybridHandler (com.networknt package)
        // - TestErrorServiceHybridHandler (com.networknt package)
        // - ToolsListHandler (com.networknt package)
        // - OtherHybridHandler (com.other.handler package)
        Assertions.assertEquals(5, handlers.size());
    }

}
