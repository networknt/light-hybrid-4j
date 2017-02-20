package com.networknt.json.router;

import io.undertow.server.HttpHandler;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import java.util.Map;

/**
 * Created by steve on 19/02/17.
 */
public abstract class AbstractApiJsonHandler implements HttpHandler {
    static private final XLogger logger = XLoggerFactory.getXLogger(AbstractApiJsonHandler.class);

    public String getServiceId(Map<String, Object> jsonMap) {
        logger.entry(jsonMap);
        return  (jsonMap.get("host") == null? "" : jsonMap.get("host") + "#") +
                (jsonMap.get("service") == null? "" : jsonMap.get("service")) +
                (jsonMap.get("version") == null? "" : "-" + jsonMap.get("version"));
    }
}
