package com.networknt.rpc.router;

import com.networknt.config.Config;
import com.networknt.rpc.Handler;
import com.networknt.security.JwtHelper;
import com.networknt.status.Status;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.util.HttpString;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

import static com.networknt.rpc.router.JsonHandler.STATUS_HANDLER_NOT_FOUND;

/**
 * Created by Nicholas Azar on July 10, 2017.
 */
public class MultipartHandler extends AbstractRpcHandler {

    static private final XLogger logger = XLoggerFactory.getXLogger(MultipartHandler.class);
    static final Map<String, Object> config = Config.getInstance().getJsonMapConfig(JwtHelper.SECURITY_CONFIG);

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {


        httpServerExchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");

        FormParserFactory.Builder builder = FormParserFactory.builder();
        FormDataParser parser = builder.build().createParser(httpServerExchange);
        if (parser != null) {
            httpServerExchange.startBlocking();
            try {
                FormData data = parser.parseBlocking();
                String serviceId = getServiceId(data);
                Handler handler = RpcStartupHookProvider.serviceMap.get(serviceId);
                if (handler == null) {
                    Status status = new Status(STATUS_HANDLER_NOT_FOUND, serviceId);
                    httpServerExchange.getResponseSender().send(status.toString());
                    return;
                }

                // calling jwt scope verification here. token signature and expiration are done
                verifyJwt(config, serviceId, httpServerExchange);

                ByteBuffer result = handler.handle(data);

                logger.exit(result);
                if (result == null) {
                    // there is nothing returned from the handler.
                    httpServerExchange.endExchange();
                } else {
                    httpServerExchange.getResponseSender().send(result);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
