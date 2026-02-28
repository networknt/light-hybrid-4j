package com.networknt.rpc.router;

import com.networknt.client.Http2Client;
import com.networknt.client.simplepool.SimpleConnectionState;
import com.networknt.exception.ClientException;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;
import org.xnio.OptionMap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by steve on 12/04/17.
 */
public class RpcRouterTest {
    static TestServer server = TestServer.getInstance();

    static final Logger logger = LoggerFactory.getLogger(RpcRouterTest.class);
    static final ObjectMapper mapper = new ObjectMapper();
    static final boolean enableHttp2 = server.getServerConfig().isEnableHttp2();
    static final boolean enableHttps = server.getServerConfig().isEnableHttps();
    static final int httpPort = server.getServerConfig().getHttpPort();
    static final int httpsPort = server.getServerConfig().getHttpsPort();
    static final String url = enableHttp2 || enableHttps ? "https://localhost:" + httpsPort : "http://localhost:" + httpPort;

    static Undertow server2 = null;
    @BeforeAll
    public static void setUp() {
        server.start();
        if (server2 == null) {
            logger.info("starting server2");
            HttpHandler handler = getJwksHandler();
            server2 = Undertow.builder()
                    .addHttpListener(7082, "localhost")
                    .setHandler(handler)
                    .build();
            server2.start();
        }

    }

    @AfterAll
    public static void tearDown() throws Exception {
        if (server2 != null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {

            }
            server2.stop();
            logger.info("The server2 is stopped.");
        }
        server.stop();

    }

    static RoutingHandler getJwksHandler() {
        return Handlers.routing()
                .add(Methods.GET, "/oauth2/N2CMw0HGQXeLvC1wBfln2A/keys", exchange -> {
                    exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
                    exchange.getResponseSender().send("{\"keys\":[{\"kty\":\"RSA\",\"kid\":\"Tj_l_tIBTginOtQbL0Pv5w\",\"n\":\"0YRbWAb1FGDpPUUcrIpJC6BwlswlKMS-z2wMAobdo0BNxNa7hG_gIHVPkXu14Jfo1JhUhS4wES3DdY3a6olqPcRN1TCCUVHd-1TLd1BBS-yq9tdJ6HCewhe5fXonaRRKwutvoH7i_eR4m3fQ1GoVzVAA3IngpTr4ptnM3Ef3fj-5wZYmitzrRUyQtfARTl3qGaXP_g8pHFAP0zrNVvOnV-jcNMKm8YZNcgcs1SuLSFtUDXpf7Nr2_xOhiNM-biES6Dza1sMLrlxULFuctudO9lykB7yFh3LHMxtIZyIUHuy0RbjuOGC5PmDowLttZpPI_j4ynJHAaAWr8Ddz764WdQ\",\"e\":\"AQAB\"}]}");
                });

    }

    private static String auth = "Bearer eyJraWQiOiJUal9sX3RJQlRnaW5PdFFiTDBQdjV3IiwiYWxnIjoiUlMyNTYifQ.eyJpc3MiOiJ1cm46Y29tOm5ldHdvcmtudDpvYXV0aDI6djEiLCJhdWQiOiJ1cm46Y29tLm5ldHdvcmtudCIsImV4cCI6MjAxOTg3MzkyOSwianRpIjoiLWZaNzY5cWxFa05RRnFGQWNkLWlGQSIsImlhdCI6MTcwNDUxMzkyOSwibmJmIjoxNzA0NTEzODA5LCJ2ZXJzaW9uIjoiJzEuMCciLCJ1c2VyX2lkIjoic3RldmUiLCJ1c2VyX3R5cGUiOiJFTVBMT1lFRSIsImNsaWVudF9pZCI6ImY3ZDQyMzQ4LWM2NDctNGVmYi1hNTJkLTRjNTc4NzQyMWU3MiIsInJvbGVzIjoidXNlciIsInNjb3BlIjpbIndvcmxkLnIiLCJ3b3JsZC53Iiwic2VydmVyLmluZm8uciJdfQ.za1C5bh4QxI6McXeogmXtVYKS611VVPNn14PCcV5Q8NadVDIcgL7TCjT_KKbU35BiV0-xlu1BdyzMEZOr3E1xlhK-WB-eAHRF__IXydJXN-pWSo6wp-Jq4-rzZlW_e4kprlT_B0GHngzlJuru5Y-00Mh8bnhUuy1QTxF3JFHnz-62bJZmyeRx9iveNGvUTF5AJGPHhRoHMMCFJjSakiuy8El-wrTFN-Zi3e09n3xxPHj6qH_mDG_WyHMJ5r3bew9asMxU8QV_A0c7jkcwxJRg6UcVLSecmd-GxrCmjw3hiIJKInyqBBIa0uZkHmm4T1c9XiG1LbFHNhHwi0H_sykoQ";

    @Test
    public void testJsonRpcValidationError() throws Exception {
        Http2Client client = Http2Client.getInstance();
        String message = "{\"host\":\"lightapi.net\",\"service\":\"rule\",\"action\":\"deleteRule\",\"version\":\"0.1.0\",\"data\":{\"hostId\":\"1234567\"}}";

        final CountDownLatch latch = new CountDownLatch(1);
        final SimpleConnectionState.ConnectionToken token;

        try {

            token = client.borrow(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, enableHttp2 ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true): OptionMap.EMPTY);

        } catch (Exception e) {

            throw new ClientException(e);

        }

        final ClientConnection connection = (ClientConnection) token.getRawConnection();
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setPath("/api/json").setMethod(Methods.POST);
            request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/json");
            request.getRequestHeaders().put(Headers.AUTHORIZATION, auth);
            request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
            connection.sendRequest(request, client.createClientCallback(reference, latch, message));

            Assertions.assertTrue(latch.await(10, TimeUnit.SECONDS), "Timed out waiting for server response");
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {

            client.restore(token);

        }
        int statusCode = reference.get().getResponseCode();
        System.out.println("statusCode = " + statusCode);
        String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
        System.out.println("body = " + body);
        Assertions.assertTrue(body.contains("ERR11004"));
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
        final SimpleConnectionState.ConnectionToken token;

        try {

            token = client.borrow(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, enableHttp2 ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true): OptionMap.EMPTY);

        } catch (Exception e) {

            throw new ClientException(e);

        }

        final ClientConnection connection = (ClientConnection) token.getRawConnection();
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setPath("/api/json").setMethod(Methods.POST);
            request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/json");
            request.getRequestHeaders().put(Headers.AUTHORIZATION, auth);
            request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
            connection.sendRequest(request, client.createClientCallback(reference, latch, message));

            Assertions.assertTrue(latch.await(10, TimeUnit.SECONDS), "Timed out waiting for server response");
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {

            client.restore(token);

        }
        int statusCode = reference.get().getResponseCode();
        System.out.println("statusCode = " + statusCode);
        String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
        System.out.println("body = " + body);
        Assertions.assertTrue(body.contains("ERR11201"));
    }

    @Test
    public void testJsonRpc20Success() throws Exception {
        Http2Client client = Http2Client.getInstance();

        String message = "{\"jsonrpc\":\"2.0\",\"method\":\"lightapi.net/rule/deleteRule/0.1.0\",\"params\":{\"hostId\":\"1234567\",\"ruleId\":\"ruleId\"},\"id\":42}";
        final CountDownLatch latch = new CountDownLatch(1);
        final SimpleConnectionState.ConnectionToken token;

        try {

            token = client.borrow(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, enableHttp2 ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true): OptionMap.EMPTY);

        } catch (Exception e) {

            throw new ClientException(e);

        }

        final ClientConnection connection = (ClientConnection) token.getRawConnection();
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setPath("/api/json").setMethod(Methods.POST);
            request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/json");
            request.getRequestHeaders().put(Headers.AUTHORIZATION, auth);
            request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
            connection.sendRequest(request, client.createClientCallback(reference, latch, message));

            Assertions.assertTrue(latch.await(10, TimeUnit.SECONDS), "Timed out waiting for server response");
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {

            client.restore(token);

        }
        int statusCode = reference.get().getResponseCode();
        System.out.println("statusCode = " + statusCode);
        String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
        System.out.println("body = " + body);
        Assertions.assertEquals(200, statusCode);
        Map<String, Object> jsonResponse = mapper.readValue(body, new TypeReference<Map<String, Object>>() {});
        Assertions.assertEquals("2.0", jsonResponse.get("jsonrpc"));
        Assertions.assertEquals(42, jsonResponse.get("id"));
        Assertions.assertTrue(jsonResponse.containsKey("result"));
    }

    @Test
    public void testJsonRpc20Notification() throws Exception {
        Http2Client client = Http2Client.getInstance();

        // No "id" field: this is a notification
        String message = "{\"jsonrpc\":\"2.0\",\"method\":\"lightapi.net/rule/deleteRule/0.1.0\",\"params\":{\"hostId\":\"1234567\",\"ruleId\":\"ruleId\"}}";
        final CountDownLatch latch = new CountDownLatch(1);
        final SimpleConnectionState.ConnectionToken token;

        try {

            token = client.borrow(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, enableHttp2 ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true): OptionMap.EMPTY);

        } catch (Exception e) {

            throw new ClientException(e);

        }

        final ClientConnection connection = (ClientConnection) token.getRawConnection();
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setPath("/api/json").setMethod(Methods.POST);
            request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/json");
            request.getRequestHeaders().put(Headers.AUTHORIZATION, auth);
            request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
            connection.sendRequest(request, client.createClientCallback(reference, latch, message));

            Assertions.assertTrue(latch.await(10, TimeUnit.SECONDS), "Timed out waiting for server response");
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {

            client.restore(token);

        }
        int statusCode = reference.get().getResponseCode();
        System.out.println("statusCode = " + statusCode);
        String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
        System.out.println("body = " + body);
        Assertions.assertEquals(204, statusCode);
        Assertions.assertTrue(body == null || body.isEmpty(), "Notification response must have no body");
    }

    @Test
    public void testJsonRpc20Error() throws Exception {
        Http2Client client = Http2Client.getInstance();

        // Pass an id and valid params to bypass schema validation, but hit the handler that returns an error
        String message = "{\"jsonrpc\":\"2.0\",\"method\":\"lightapi.net/rule/updateRule/0.1.0\",\"params\":{\"ruleId\":\"rule456\",\"ruleName\":\"Updated Rule Name\",\"ruleVersion\":\"1.1\",\"ruleType\":\"Validation\",\"ruleOwner\":\"manager\",\"common\":\"N\",\"conditions\":[]},\"id\":99}";
        final CountDownLatch latch = new CountDownLatch(1);
        final SimpleConnectionState.ConnectionToken token;

        try {
            token = client.borrow(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, enableHttp2 ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true): OptionMap.EMPTY);
        } catch (Exception e) {
            throw new ClientException(e);
        }

        final ClientConnection connection = (ClientConnection) token.getRawConnection();
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setPath("/api/json").setMethod(Methods.POST);
            request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/json");
            request.getRequestHeaders().put(Headers.AUTHORIZATION, auth);
            request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
            connection.sendRequest(request, client.createClientCallback(reference, latch, message));

            Assertions.assertTrue(latch.await(10, TimeUnit.SECONDS), "Timed out waiting for server response");
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {
            client.restore(token);
        }
        
        int statusCode = reference.get().getResponseCode();
        System.out.println("statusCode = " + statusCode);
        String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
        System.out.println("body = " + body);
        
        Assertions.assertEquals(400, statusCode); // The HTTP status is preserved as 400 by JsonHandler
        
        Map<String, Object> jsonResponse = mapper.readValue(body, new TypeReference<Map<String, Object>>() {});
        Assertions.assertEquals("2.0", jsonResponse.get("jsonrpc"));
        Assertions.assertEquals(99, jsonResponse.get("id"));
        Assertions.assertFalse(jsonResponse.containsKey("result"), "Error response must not contain result");
        Assertions.assertTrue(jsonResponse.containsKey("error"), "Error response must contain error object");
        
        Map<String, Object> errorObject = (Map<String, Object>) jsonResponse.get("error");
        Assertions.assertEquals(401, errorObject.get("code")); // 401 status code should be coerced
    }


    // Ignore it as we cannot get the jwks and x509 certificate is not supported anymore.
    @Test
    public void testJsonRpcPostNoError() throws Exception {
        Http2Client client = Http2Client.getInstance();

        String message = "{\"host\":\"lightapi.net\",\"service\":\"rule\",\"action\":\"deleteRule\",\"version\":\"0.1.0\",\"data\":{\"hostId\":\"1234567\",\"ruleId\":\"ruleId\"}}";
        final CountDownLatch latch = new CountDownLatch(1);
        final SimpleConnectionState.ConnectionToken token;

        try {

            token = client.borrow(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, enableHttp2 ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true): OptionMap.EMPTY);

        } catch (Exception e) {

            throw new ClientException(e);

        }

        final ClientConnection connection = (ClientConnection) token.getRawConnection();
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setPath("/api/json").setMethod(Methods.POST);
            request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/json");
            request.getRequestHeaders().put(Headers.AUTHORIZATION, auth);
            request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
            connection.sendRequest(request, client.createClientCallback(reference, latch, message));

            Assertions.assertTrue(latch.await(10, TimeUnit.SECONDS), "Timed out waiting for server response");
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {

            client.restore(token);

        }
        int statusCode = reference.get().getResponseCode();
        System.out.println("statusCode = " + statusCode);
        String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
        System.out.println("body = " + body);
        Assertions.assertEquals("OK", body);
    }


    // Ignore it as we cannot get the jwks and x509 certificate is not supported anymore.
    @Test
    public void testJsonRpcGetNoError() throws Exception {
        Http2Client client = Http2Client.getInstance();
        String message = "/api/json?cmd=" + URLEncoder.encode("{\"host\":\"lightapi.net\",\"service\":\"rule\",\"action\":\"deleteRule\",\"version\":\"0.1.0\",\"data\":{\"hostId\":\"1234567\",\"ruleId\":\"ruleId\"}}", StandardCharsets.UTF_8);
        System.out.println("message = " + message);

        final CountDownLatch latch = new CountDownLatch(1);
        final SimpleConnectionState.ConnectionToken token;

        try {

            token = client.borrow(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true));

        } catch (Exception e) {

            throw new ClientException(e);

        }

        final ClientConnection connection = (ClientConnection) token.getRawConnection();
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath(message);
            request.getRequestHeaders().put(Headers.AUTHORIZATION, auth);
            connection.sendRequest(request, client.createClientCallback(reference, latch));
            Assertions.assertTrue(latch.await(10, TimeUnit.SECONDS), "Timed out waiting for server response");
            int statusCode = reference.get().getResponseCode();
            String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
            System.out.println("body = " + body);
            Assertions.assertEquals(200, statusCode);
            Assertions.assertEquals("OK", body);
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {

            client.restore(token);

        }
    }

    @Test
    public void testJsonRpcGetNoParamValue() throws Exception {
        Http2Client client = Http2Client.getInstance();
        String message = "/api/json?cmd=";
        System.out.println("message = " + message);

        final CountDownLatch latch = new CountDownLatch(1);
        final SimpleConnectionState.ConnectionToken token;

        try {

            token = client.borrow(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true));

        } catch (Exception e) {

            throw new ClientException(e);

        }

        final ClientConnection connection = (ClientConnection) token.getRawConnection();
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath(message);
            request.getRequestHeaders().put(Headers.AUTHORIZATION, auth);
            connection.sendRequest(request, client.createClientCallback(reference, latch));
            Assertions.assertTrue(latch.await(10, TimeUnit.SECONDS), "Timed out waiting for server response");
            int statusCode = reference.get().getResponseCode();
            String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
            System.out.println("statusCode = " + statusCode + " body = " + body);
            Assertions.assertEquals(400, statusCode);
            Assertions.assertTrue(body.contains("ERR11202"));
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {

            client.restore(token);

        }
    }
}
