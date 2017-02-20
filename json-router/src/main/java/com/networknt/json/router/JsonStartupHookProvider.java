package com.networknt.json.router;

import com.networknt.config.Config;
import com.networknt.server.StartupHookProvider;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.lukehutch.fastclasspathscanner.scanner.ClassInfo;
import io.undertow.server.HttpHandler;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Register all the service handlers by the annotation from jars in classpath.
 *
 * Created by steve on 19/02/17.
 */
public class JsonStartupHookProvider implements StartupHookProvider {
    static final String CONFIG_NAME = "json-router";

    static JsonRouterConfig config = (JsonRouterConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, JsonRouterConfig.class);

    static Map<String, ClassInfo> classNameToClassInfo =
            new FastClasspathScanner(config.getHandlerPackage()).scan().getClassNameToClassInfo();

    public static final Map<String, HttpHandler> serviceMap = new HashMap<>();

    @Override
    public void onStartup() {

        // lookup all ServiceHandler and register them to handle request
        List<String> handlers =
                classNameToClassInfo.values().stream()
                        .filter(ci -> ci.hasAnnotation(ServiceHandler.class.getName()))
                        .map(ClassInfo::getClassName)
                        .sorted()
                        .collect(Collectors.toList());

        // for each handler, create instance and register.
        for(String className: handlers) {
            try {
                Class handler = Class.forName(className);
                ServiceHandler a = (ServiceHandler)handler.getAnnotation(ServiceHandler.class);
                serviceMap.put(a.id(), (HttpHandler)handler.newInstance());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
