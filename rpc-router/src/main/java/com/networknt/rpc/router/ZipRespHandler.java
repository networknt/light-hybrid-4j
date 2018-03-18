package com.networknt.rpc.router;

import com.networknt.config.Config;
import com.networknt.rpc.Handler;
import com.networknt.security.JwtHelper;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.util.HttpString;
import io.undertow.util.StatusCodes;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import java.util.Map;

/**
 * @author Nicholas Azar
 * Created on March 17, 2018
 */
public class ZipRespHandler extends AbstractRpcHandler {

    static private final XLogger logger = XLoggerFactory.getXLogger(MultipartHandler.class);
    static final Map<String, Object> config = Config.getInstance().getJsonMapConfig(JwtHelper.SECURITY_CONFIG);

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {

        // Not providing a filename. In this case the browser should show something like file.zip and let the user name it.
        httpServerExchange.getResponseHeaders()
                .add(new HttpString("Content-Type"), "application/zip")
                .add(new HttpString("Content-Disposition"), "attachment");

        // A form submission is how the request for the file should come in. Downloads from ajax is not simple.
        FormParserFactory.Builder builder = FormParserFactory.builder();
        FormDataParser parser = builder.build().createParser(httpServerExchange);

        if (parser != null) {
            httpServerExchange.startBlocking();

            FormData formData = parser.parseBlocking();
            String serviceId = getServiceId(formData);
            Handler handler = getHandlerOrPopulateExchange(serviceId, httpServerExchange);
            if (handler == null) { // exchange has been populated.
                return;
            }
            verifyJwt(config, serviceId, httpServerExchange);

            // Calculate the response and return it to the client.
            handleFormDataRequest(handler, formData, httpServerExchange);
        } else {
            logger.error("Form could not be retrieved from request.");
            httpServerExchange.setStatusCode(StatusCodes.BAD_REQUEST);
            httpServerExchange.getResponseSender().send(StatusCodes.BAD_REQUEST_STRING);
        }
    }
}
