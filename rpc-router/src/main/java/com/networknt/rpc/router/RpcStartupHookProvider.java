package com.networknt.rpc.router;

import com.networknt.config.Config;
import com.networknt.resource.PathResourceProvider;
import com.networknt.resource.PredicatedHandlersProvider;
import com.networknt.rpc.Handler;
import com.networknt.server.Server;
import com.networknt.server.StartupHookProvider;
import com.networknt.service.SingletonServiceFactory;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Register all the service handlers by the annotation from jars in classpath.
 * If you only want to scan services from a specific package, please update rpc-router.yml
 * Normally, you would put all the service into something com.yourcompany.xxx to speed up
 * the scanning.
 *
 * @author Steve Hu
 */
public class RpcStartupHookProvider implements StartupHookProvider {
    private static final String CONFIG_NAME = "rpc-router";
    static RpcRouterConfig config = (RpcRouterConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, RpcRouterConfig.class);

    static final Map<String, Handler> serviceMap = new HashMap<>();
    static PathResourceProvider[] pathResourceProviders;
    static PredicatedHandlersProvider[] predicatedHandlersProviders;

    @Override
    public void onStartup() {
        System.out.println("Handler scanning package = " + config.getHandlerPackage());
        // lookup all ServiceHandler and register them to handle request
        List<String> handlers;
        try (ScanResult scanResult = new ClassGraph()
                .whitelistPackages(config.getHandlerPackage())
                .enableAllInfo()
                .scan()) {
            handlers = scanResult
                    .getClassesWithAnnotation(ServiceHandler.class.getName())
                    .getNames();
        }
        System.out.println("RpcStartupHookProvider: handlers size " + handlers.size());
        // for each handler, create instance and register.
        for(String className: handlers) {
            try {
                Class handler = Class.forName(className);
                ServiceHandler a = (ServiceHandler)handler.getAnnotation(ServiceHandler.class);
                serviceMap.put(a.id(), (Handler)handler.getConstructor().newInstance());
                System.out.println("RpcStartupHookProvider add id " + a.id() + " map to " + className);
                if(config.isRegisterService()) Server.serviceIds.add(a.id().replace('/', '.'));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        pathResourceProviders = SingletonServiceFactory.getBeans(PathResourceProvider.class);
        predicatedHandlersProviders = SingletonServiceFactory.getBeans(PredicatedHandlersProvider.class);
    }
}
