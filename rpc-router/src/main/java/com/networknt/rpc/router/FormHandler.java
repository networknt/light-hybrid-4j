package com.networknt.rpc.router;

import com.networknt.rpc.Handler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormParserFactory;

import java.io.IOException;

/**
 * @author Nicholas Azar
 * Created on July 10, 2017
 */
public class FormHandler extends AbstractRpcHandler {

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) {

        FormParserFactory.Builder builder = FormParserFactory.builder();
        FormDataParser parser = builder.build().createParser(httpServerExchange);
        if (parser != null) {
            httpServerExchange.startBlocking();
            try {
                FormData data = parser.parseBlocking();
                String serviceId = getServiceId(data);
                Handler handler = getHandlerOrPopulateExchange(serviceId, httpServerExchange);
                if (handler == null) { // exchange has been populated
                    return;
                }

                // calling jwt scope verification here. token signature and expiration are done
                verifyJwt(serviceId, httpServerExchange);

                handleFormDataRequest(handler, data, httpServerExchange);

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
