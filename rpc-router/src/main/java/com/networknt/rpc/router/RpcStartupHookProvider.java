package com.networknt.rpc.router;

import com.networknt.config.Config;
import com.networknt.rpc.Handler;
import com.networknt.server.StartupHookProvider;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.lukehutch.fastclasspathscanner.scanner.ClassInfo;

import java.util.ArrayList;
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
    static final String CONFIG_NAME = "rpc-router";

    static RpcRouterConfig config = (RpcRouterConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, RpcRouterConfig.class);

    static Map<String, ClassInfo> classNameToClassInfo =
            new FastClasspathScanner(config.getHandlerPackage()).scan().getClassNameToClassInfo();

    public static final Map<String, Handler> serviceMap = new HashMap<>();
    static String[] safeResourcePaths;

    @Override
    public void onStartup() {
        // lookup all ServiceHandler and register them to handle request
        List<String> handlers =
                classNameToClassInfo.values().stream()
                        .filter(ci -> ci.hasAnnotation(ServiceHandler.class.getName()))
                        .map(ClassInfo::getClassName)
                        .sorted()
                        .collect(Collectors.toList());
        System.out.println("RpcStartupHookProvider: handlers size " + handlers.size());
        // for each handler, create instance and register.
        for(String className: handlers) {
            try {
                Class handler = Class.forName(className);
                ServiceHandler a = (ServiceHandler)handler.getAnnotation(ServiceHandler.class);
                serviceMap.put(a.id(), (Handler)handler.getConstructor().newInstance());
                System.out.printf("RpcStartupHookProvider add %s to %s", a.id(), className);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            safeResourcePaths = getSafeResourcePaths().toArray(new String[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    List<String> getSafeResourcePaths() throws Exception {
        List<String> safeResourcePaths = new ArrayList<>();
        List<String> safeResourceProviders = classNameToClassInfo.values().stream()
                .filter(ci -> ci.directlyImplementsInterface(RpcResourcePathsProvider.class.getName()))
                .map(ClassInfo::getClassName)
                .sorted().collect(Collectors.toList());

        if (safeResourceProviders != null && safeResourceProviders.size() > 0) {
            for (String providerName : safeResourceProviders) {
                Class provider = Class.forName(providerName);
                safeResourcePaths.addAll(((RpcResourcePathsProvider)provider.getConstructor().newInstance()).getSafeResourcePaths());
            }
        }
        return safeResourcePaths;
    }
}
