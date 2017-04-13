package com.networknt.rpc.router;

import com.networknt.colfer.ColferRpc;
import io.undertow.server.HttpHandler;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import java.util.Map;

/**
 * Created by steve on 19/02/17.
 */
public abstract class AbstractRpcHandler implements HttpHandler {
    static private final XLogger logger = XLoggerFactory.getXLogger(AbstractRpcHandler.class);

    public String getServiceId(Map<String, Object> jsonMap) {
        logger.entry(jsonMap);
        return  (jsonMap.get("host") == null? "" : jsonMap.get("host") + "/") +
                (jsonMap.get("service") == null? "" : jsonMap.get("service") + "/") +
                (jsonMap.get("action") == null? "" : jsonMap.get("action") + "/") +
                (jsonMap.get("version") == null? "" : jsonMap.get("version"));
    }

    public String getServiceId(ColferRpc cf) {
        logger.entry(cf);
        return  (cf.host == null? "" : cf.host + "/") +
                (cf.service == null? "" : cf.service + "/") +
                (cf.action == null? "" : cf.action + "/") +
                (cf.version == null? "" : cf.version);
    }
}
