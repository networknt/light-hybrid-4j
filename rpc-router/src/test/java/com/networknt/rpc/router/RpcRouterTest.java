package com.networknt.rpc.router;

import com.networknt.client.Http2Client;
import com.networknt.exception.ClientException;
import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;
import org.xnio.OptionMap;

import java.net.URI;
import java.net.URLEncoder;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by steve on 12/04/17.
 */
public class RpcRouterTest {
    @ClassRule
    public static TestServer server = TestServer.getInstance();

    static final Logger logger = LoggerFactory.getLogger(RpcRouterTest.class);
    static final boolean enableHttp2 = server.getServerConfig().isEnableHttp2();
    static final boolean enableHttps = server.getServerConfig().isEnableHttps();
    static final int httpPort = server.getServerConfig().getHttpPort();
    static final int httpsPort = server.getServerConfig().getHttpsPort();
    static final String url = enableHttp2 || enableHttps ? "https://localhost:" + httpsPort : "http://localhost:" + httpPort;

    private static String auth = "Bearer eyJraWQiOiIxMDAiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ1cm46Y29tOm5ldHdvcmtudDpvYXV0aDI6djEiLCJhdWQiOiJ1cm46Y29tLm5ldHdvcmtudCIsImV4cCI6MTgwODkxMzM2NCwianRpIjoicjZpNGstcEF2ekU4VTd4LTFya3JIQSIsImlhdCI6MTQ5MzU1MzM2NCwibmJmIjoxNDkzNTUzMjQ0LCJ2ZXJzaW9uIjoiMS4wIiwidXNlcl9pZCI6InN0ZXZlIiwidXNlcl90eXBlIjoiRU1QTE9ZRUUiLCJjbGllbnRfaWQiOiJmN2Q0MjM0OC1jNjQ3LTRlZmItYTUyZC00YzU3ODc0MjFlNzIiLCJzY29wZSI6WyJ3b3JsZC5yIiwid29ybGQudyIsInNlcnZlci5pbmZvLnIiXX0.VZCeU_M9xJKquSpGu0DgkX5aThUvqlChEcQOIG4aFlLkgfq76hf498GCdqLlAlk7RvkwnQUrwNa2kH8T-gNapgpWYnnwJ0cpWGE4LQ0urqFHetoJeiVyv6XVVp9khO4dsbcJLvVDzEr2Sgzwu3Bi7pkEg6BNwBQIEZRIwNxvQWIt9hnrdrvkId70C0mC9GkZC35_bEOWMkamw0TFUAimeStyZo3NJDwmH9EQmSN1523dF4Q2hFxhtfzOv-DQccIe8U2iG3tT3LJCSYjRJK0idt3NFq57WT0MA7vPSOFplTqCK_WfH5u-so_xKnltRKoKadXkBjHojznXO6nNhF38eQ";

    // Ignore it as we cannot get the jwks and x509 certificate is not supported anymore.
    @Test
    @Ignore
    public void testJsonRpcValidationError() throws Exception {
        Http2Client client = Http2Client.getInstance();
        String message = "{\"host\":\"www.networknt.com\",\"service\":\"account\",\"action\":\"delete\",\"version\":\"0.1.1\",\"data\":{\"accountNo\":\"1234567\"}}";

        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, enableHttp2 ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true): OptionMap.EMPTY).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setPath("/api/json").setMethod(Methods.POST);
            request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/json");
            request.getRequestHeaders().put(Headers.AUTHORIZATION, auth);
            request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
            connection.sendRequest(request, client.createClientCallback(reference, latch, message));

            latch.await();
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
        int statusCode = reference.get().getResponseCode();
        System.out.println("statusCode = " + statusCode);
        String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
        System.out.println("body = " + body);
        Assert.assertTrue(body.contains("ERR11004"));
    }

    /**
     * Test empty post request body and expect 400 error from the server.
     * 
     * @throws Exception
     */
    @Test
    public void testJsonRpcEmptyBody() throws Exception {
        Http2Client client = Http2Client.getInstance();

        String message = "";
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, enableHttp2 ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true): OptionMap.EMPTY).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setPath("/api/json").setMethod(Methods.POST);
            request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/json");
            request.getRequestHeaders().put(Headers.AUTHORIZATION, auth);
            request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
            connection.sendRequest(request, client.createClientCallback(reference, latch, message));

            latch.await();
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
        int statusCode = reference.get().getResponseCode();
        System.out.println("statusCode = " + statusCode);
        String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
        System.out.println("body = " + body);
        Assert.assertTrue(body.contains("ERR11201"));
    }


    // Ignore it as we cannot get the jwks and x509 certificate is not supported anymore.
    @Test
    @Ignore
    public void testJsonRpcPostNoError() throws Exception {
        Http2Client client = Http2Client.getInstance();

        String message = "{\"host\":\"www.networknt.com\",\"service\":\"account\",\"action\":\"delete\",\"version\":\"0.1.1\",\"data\":{\"accountNo\":\"1234567\",\"accountType\":\"P\"}}";
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, enableHttp2 ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true): OptionMap.EMPTY).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setPath("/api/json").setMethod(Methods.POST);
            request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/json");
            request.getRequestHeaders().put(Headers.AUTHORIZATION, auth);
            request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
            connection.sendRequest(request, client.createClientCallback(reference, latch, message));

            latch.await();
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
        int statusCode = reference.get().getResponseCode();
        System.out.println("statusCode = " + statusCode);
        String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
        System.out.println("body = " + body);
        Assert.assertEquals("OK", body);
    }


    // Ignore it as we cannot get the jwks and x509 certificate is not supported anymore.
    @Test
    @Ignore
    public void testJsonRpcGetNoError() throws Exception {
        Http2Client client = Http2Client.getInstance();
        String message = "/api/json?cmd=" + URLEncoder.encode("{\"host\":\"www.networknt.com\",\"service\":\"account\",\"action\":\"delete\",\"version\":\"0.1.1\",\"data\":{\"accountNo\":\"1234567\",\"accountType\":\"P\"}}", "UTF-8");
        System.out.println("message = " + message);

        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath(message);
            request.getRequestHeaders().put(Headers.AUTHORIZATION, auth);
            connection.sendRequest(request, client.createClientCallback(reference, latch));
            latch.await();
            int statusCode = reference.get().getResponseCode();
            String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
            System.out.println("body = " + body);
            Assert.assertEquals(200, statusCode);
            Assert.assertEquals("OK", body);
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
    }

    @Test
    public void testJsonRpcGetNoParamValue() throws Exception {
        Http2Client client = Http2Client.getInstance();
        String message = "/api/json?cmd=";
        System.out.println("message = " + message);

        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath(message);
            request.getRequestHeaders().put(Headers.AUTHORIZATION, auth);
            connection.sendRequest(request, client.createClientCallback(reference, latch));
            latch.await();
            int statusCode = reference.get().getResponseCode();
            String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
            System.out.println("statusCode = " + statusCode + " body = " + body);
            Assert.assertEquals(400, statusCode);
            Assert.assertTrue(body.contains("ERR11202"));
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
    }
}
