package com.networknt.rpc.router;

import com.networknt.handler.MiddlewareHandler;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.rpc.HybridHandler;
import com.networknt.server.ServerConfig;
import com.networknt.utility.Constants;
import io.undertow.Handlers;
import io.undertow.server.DirectByteBufferDeallocator;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.StatusCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * This is the entry handler for json api, it will parse the JSON request body and
 * construct service id and then calls the service handler to get the final response.
 *
 * @author Steve Hu
 */
public class JsonHandler implements MiddlewareHandler {
    private volatile HttpHandler next;

    static final String STATUS_HANDLER_NOT_FOUND = "ERR11200";

    static private final Logger LOG = LoggerFactory.getLogger(JsonHandler.class);

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        LOG.trace("JsonHandler is called");

        // validation and security have been done already. The SchemaHandler should have already
        // parsed the body and set the auditInfo attachment.
        Map<String, Object> auditInfo = exchange.getAttachment(AttachmentConstants.AUDIT_INFO);
        final String serviceId;
        final ServerConfig serverConfig = ServerConfig.load();

        // get the serviceId from the auditInfo attachment.
        if (auditInfo != null) {
            serviceId = (String)auditInfo.get(Constants.ENDPOINT_STRING);
            if(serviceId == null) {
                setExchangeStatus(exchange, STATUS_HANDLER_NOT_FOUND);
                return;
            }

        // if the auditInfo is null, then get the serviceId from the serverConfig.
        } else if (serverConfig != null) {
            serviceId = serverConfig.getServiceId();

        // if the serverConfig is null, then set the exchange status to STATUS_HANDLER_NOT_FOUND.
        } else {
            setExchangeStatus(exchange, STATUS_HANDLER_NOT_FOUND);
            return;
        }

        LOG.trace("serviceId = {}", serviceId);

        HybridHandler handler = RpcStartupHookProvider.serviceMap.get(serviceId);
        Map<String, Object> data = (Map<String, Object>)auditInfo.get(Constants.HYBRID_SERVICE_DATA);
        ByteBuffer result = handler.handle(exchange, data);

        if(result == null) {

            // there is nothing returned from the handler.
            exchange.setStatusCode(StatusCodes.OK);
            exchange.endExchange();

        } else {

            // we are expecting the handler set the statusCode if there is an error.
            // if there is no status code, default 200 will be used.
            exchange.addExchangeCompleteListener((exchange1, nextListener) -> {
                try {
                    DirectByteBufferDeallocator.free(result);
                } finally {
                    nextListener.proceed();
                }
            });

            Object reqId = auditInfo != null ? auditInfo.get("jsonrpc_id") : null;
            if (reqId != null) {
                // Wrap the result in a JSON-RPC 2.0 response format
                byte[] bytes = new byte[result.remaining()];
                result.get(bytes);
                String resultString = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                String jsonRpcResponse;
                if (reqId instanceof String) {
                    jsonRpcResponse = String.format("{\"jsonrpc\":\"2.0\",\"result\":%s,\"id\":\"%s\"}", resultString, reqId);
                } else {
                    jsonRpcResponse = String.format("{\"jsonrpc\":\"2.0\",\"result\":%s,\"id\":%s}", resultString, reqId);
                }
                exchange.getResponseSender().send(jsonRpcResponse);
            } else {
                exchange.getResponseSender().send(result);
            }
        }
    }

    @Override
    public HttpHandler getNext() {
        return next;
    }

    @Override
    public MiddlewareHandler setNext(HttpHandler next) {
        Handlers.handlerNotNull(next);
        this.next = next;
        return this;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

}
