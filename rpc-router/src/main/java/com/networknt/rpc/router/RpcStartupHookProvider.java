package com.networknt.rpc.router;

import com.networknt.config.Config;
import com.networknt.resource.PathResourceProvider;
import com.networknt.resource.PredicatedHandlersProvider;
import com.networknt.rpc.HybridHandler;
import com.networknt.server.Server;
import com.networknt.server.StartupHookProvider;
import com.networknt.service.SingletonServiceFactory;
import com.networknt.server.ModuleRegistry;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public static final Map<String, HybridHandler> serviceMap = new HashMap<>();
    public static PathResourceProvider[] pathResourceProviders;
    public static PredicatedHandlersProvider[] predicatedHandlersProviders;
    public RpcStartupHookProvider() {
        logger.info("RpcStartupHookProvider is constructed");
        RpcRouterConfig.load();
    }

    @Override
    public void onStartup() {
        RpcRouterConfig config = RpcRouterConfig.load();
        logger.debug("Handler scanning package = {}", config.getHandlerPackages());

        final var packages = config.getHandlerPackages().toArray(new String[0]);
        System.out.println("packages: " + Arrays.toString(packages));

        // lookup all ServiceHandler and register them to handle request
        List<String> handlers;
        try (ScanResult scanResult = new ClassGraph()
                .acceptPackages(packages)
                .enableAllInfo()
                .scan()) {

            handlers = scanResult
                    .getClassesWithAnnotation(ServiceHandler.class.getName())
                    .getNames();
        }
        logger.debug("RpcStartupHookProvider.onStartup handlers size = {}", handlers.size());

        // for each handler, create instance and register.
        for(String className: handlers) {

            try {
                Class handler = Class.forName(className);
                ServiceHandler a = (ServiceHandler)handler.getAnnotation(ServiceHandler.class);
                serviceMap.put(a.id(), (HybridHandler)handler.getConstructor().newInstance());

                logger.debug("RpcStartupHookProvider add id {} maps to {}",  a.id(), className);

                if(config.isRegisterService())
                    Server.serviceIds.add(a.id().replace('/', '.'));

            } catch (Exception e) {
                logger.error("Exception:", e);
            }
        }

        pathResourceProviders = SingletonServiceFactory.getBeans(PathResourceProvider.class);
        predicatedHandlersProviders = SingletonServiceFactory.getBeans(PredicatedHandlersProvider.class);
    }
}
