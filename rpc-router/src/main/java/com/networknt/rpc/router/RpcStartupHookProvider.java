package com.networknt.rpc.router;

import com.networknt.config.Config;
import com.networknt.resource.PathResourceProvider;
import com.networknt.resource.PredicatedHandlersProvider;
import com.networknt.rpc.Handler;
import com.networknt.server.Server;
import com.networknt.server.StartupHookProvider;
import com.networknt.service.SingletonServiceFactory;
import com.networknt.traceability.TraceabilityConfig;
import com.networknt.traceability.TraceabilityHandler;
import com.networknt.utility.ModuleRegistry;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    static final Logger logger = LoggerFactory.getLogger(RpcStartupHookProvider.class);
    private static final String CONFIG_NAME = "rpc-router";
    static RpcRouterConfig config;

    static final Map<String, Handler> serviceMap = new HashMap<>();
    static PathResourceProvider[] pathResourceProviders;
    static PredicatedHandlersProvider[] predicatedHandlersProviders;
    public RpcStartupHookProvider() {
        if(logger.isInfoEnabled()) logger.info("RpcStartupHookProvider is constructed");
        config = RpcRouterConfig.load();
        ModuleRegistry.registerModule(RpcRouterConfig.CONFIG_NAME, RpcStartupHookProvider.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(RpcRouterConfig.CONFIG_NAME), null);
    }

    @Override
    public void onStartup() {
        if(logger.isDebugEnabled()) logger.debug("Handler scanning package = " + config.getHandlerPackage());
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
        if(logger.isDebugEnabled()) logger.debug("RpcStartupHookProvider: handlers size " + handlers.size());
        // for each handler, create instance and register.
        for(String className: handlers) {
            try {
                Class handler = Class.forName(className);
                ServiceHandler a = (ServiceHandler)handler.getAnnotation(ServiceHandler.class);
                serviceMap.put(a.id(), (Handler)handler.getConstructor().newInstance());
                if(logger.isDebugEnabled()) logger.debug("RpcStartupHookProvider add id " + a.id() + " map to " + className);
                if(config.isRegisterService()) Server.serviceIds.add(a.id().replace('/', '.'));
            } catch (Exception e) {
                logger.error("Exception:", e);
            }
        }
        pathResourceProviders = SingletonServiceFactory.getBeans(PathResourceProvider.class);
        predicatedHandlersProviders = SingletonServiceFactory.getBeans(PredicatedHandlersProvider.class);
    }
}
