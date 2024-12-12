package com.networknt.rpc.router;

import com.networknt.handler.LightHttpHandler;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.rpc.HybridHandler;
import com.networknt.utility.Constants;
import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.DirectByteBufferDeallocator;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.StatusCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * This is the entry handler for json api, it will parse the JSON request body and
 * construct service id and then calls the service handler to get the final response.
 *
 * @author Steve Hu
 */
public class JsonHandler implements LightHttpHandler {

    static final String STATUS_HANDLER_NOT_FOUND = "ERR11200";

    static private final Logger logger = LoggerFactory.getLogger(JsonHandler.class);

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if(logger.isTraceEnabled()) logger.trace("JsonHandler is called");
        // validation and security have been done already. The SchemaHandler should have already
        // parsed the body and set the auditInfo attachment.
        Map<String, Object> auditInfo = exchange.getAttachment(AttachmentConstants.AUDIT_INFO);
        String serviceId = (String)auditInfo.get(Constants.ENDPOINT_STRING);
        if(logger.isTraceEnabled()) logger.trace("serviceId = {}", serviceId);
        if(serviceId == null) {
            setExchangeStatus(exchange, STATUS_HANDLER_NOT_FOUND);
            return;
        }
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
            exchange.getResponseSender().send(result, new IoCallback() {
                @Override
                public void onComplete(HttpServerExchange exchange, Sender sender) {
                    DirectByteBufferDeallocator.free(result);
                }

                @Override
                public void onException(HttpServerExchange exchange, Sender sender, IOException e) {
                    DirectByteBufferDeallocator.free(result);
                }
            });
        }
    }
}
