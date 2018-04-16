package com.networknt.rpc.router;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.resource.*;

import java.io.File;


/**
 * @author Nicholas Azar
 * Created on April 15, 2018
 */
public class RpcResourceHandler extends AbstractRpcHandler {

    private static ResourceManager resourceManager = new PathResourceManager(
            new File(RpcResourceHandler.class.getResource(RpcStartupHookProvider.config.getResourcesBasePath()).getFile()).toPath(),
            0L, false, RpcStartupHookProvider.safeResourcePaths);
    private static ResourceHandler resourceHandler = new ResourceHandler(resourceManager);

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
        resourceHandler.handleRequest(httpServerExchange);
    }
}
